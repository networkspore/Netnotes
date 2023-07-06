package com.netnotes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.WalletKey.Local;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class ChartView {

    private File logFile = new File("pricechart-log.txt");

    private NumberClass m_numberClass = new NumberClass();

    private ArrayList<PriceData> m_priceList = new ArrayList<>();

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();
    private ChangeListener<LocalDateTime> m_changeListener = null;

    private int m_valid = 0;
    private Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);

    private Color m_backgroundColor = new Color(1f, 1f, 1f, 0f);
    private Font m_labelFont = new Font("Arial", java.awt.Font.BOLD, 12);

    private double m_scale = 0;
    private int m_defaultCellWidth = 20;
    private int m_cellWidth = m_defaultCellWidth;
    private SimpleDoubleProperty m_chartHeight;
    private SimpleDoubleProperty m_chartWidth;

    private boolean m_direction = false;

    private String m_msg = "Loading";

    private double m_currentPrice = 0;
    // private BufferedImage m_img = null;
    private int m_cellPadding = 3;
    private HBox m_chartHbox = new HBox();

    public ChartView(SimpleDoubleProperty width, SimpleDoubleProperty height) {
        HBox.setHgrow(m_chartHbox, Priority.ALWAYS);
        m_chartHbox.setAlignment(Pos.CENTER);
        m_chartWidth = width;
        m_chartHeight = height;
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        //   jsonObject.addProperty("timeSpan", m_timeSpan);
        return jsonObject;
    }

    public HBox getChartBox() {

        Image image = SwingFXUtils.toFXImage(getBufferedImage(), null);
        ImageView imgView = IconButton.getIconView(image, image.getWidth());

        Runnable updateImage = () -> {
            Image latestImage = SwingFXUtils.toFXImage(getBufferedImage(), null);
            imgView.setImage(latestImage);
            imgView.setFitWidth(latestImage.getWidth());
        };

        m_chartHeight.addListener((obs, oldVal, newVal) -> updateImage.run());

        m_chartWidth.addListener((obs, oldVal, newVal) -> updateImage.run());

        m_lastUpdated.addListener((obs, oldVal, newVal) -> updateImage.run());

        m_chartHbox.getChildren().clear();
        m_chartHbox.getChildren().add(imgView);

        return m_chartHbox;
    }

    private void resizeChart() {
        int fullChartSize = (m_cellWidth + (m_cellPadding * 2)) * m_priceList.size();
        if (fullChartSize < m_chartWidth.get()) {
            while (fullChartSize < m_chartWidth.get()) {
                m_cellWidth += 1;
                fullChartSize = m_cellWidth + (m_cellPadding * 2);
            }
        } else {
            if ((m_chartWidth.get() / (m_cellWidth + (m_cellPadding * 2))) < 30) {
                while (m_cellWidth > 2 && (m_chartWidth.get() / (m_cellWidth + (m_cellPadding * 2))) < 30) {
                    m_cellWidth -= 1;
                }
            }
        }

    }

    public void setPriceDataList(JsonArray jsonArray) {

        if (jsonArray != null && jsonArray.size() > 0) {
            m_valid = 1;
            m_msg = "Loading";
            NumberClass numberClass = new NumberClass();

            ArrayList<PriceData> tmpPriceList = new ArrayList<>();

            jsonArray.forEach(dataElement -> {

                JsonArray dataArray = dataElement.getAsJsonArray();

                PriceData priceData = new PriceData(dataArray);
                if (numberClass.low.get() == 0) {
                    numberClass.low.set(priceData.getLow());
                }

                numberClass.sum.set(numberClass.sum.get() + priceData.getClose());
                numberClass.count.set(numberClass.count.get() + 1);
                tmpPriceList.add(priceData);
                if (priceData.getHigh() > numberClass.high.get()) {
                    numberClass.high.set(priceData.getHigh());
                }
                if (priceData.getLow() < numberClass.low.get()) {
                    numberClass.low.set(priceData.getLow());
                }

            });

            Collections.reverse(tmpPriceList);
            m_priceList = tmpPriceList;
            m_numberClass = numberClass;
        } else {
            m_valid = 2;
            m_msg = "Received corrupt data.";
        }
        m_lastUpdated.set(LocalDateTime.now());
    }

    public void updateCandleData(PriceData priceData) {
        int priceListSize = m_priceList.size();

        if (priceListSize > 0) {
            int lastIndex = m_priceList.size() - 1;
            PriceData lastData = m_priceList.get(lastIndex);

            if (lastData.getTimestamp() == priceData.getTimestamp()) {
                m_priceList.set(lastIndex, priceData);
            } else {
                m_priceList.add(priceData);
            }
        } else {
            m_priceList.add(priceData);

        }
        m_currentPrice = priceData.getClose();
        m_lastUpdated.set(LocalDateTime.now());
    }

    public double getCurrentPrice() {
        return m_currentPrice;
    }

    public SimpleDoubleProperty chartHeightProperty() {
        return m_chartHeight;
    }

    public double getChartHeight() {
        return m_chartHeight.get();
    }

    public void setChartHeight(int chartHeight) {
        m_chartHeight.set(chartHeight);
    }

    public int getCellWidth() {
        return m_cellWidth;
    }

    public void setCellWidth(int cellWidth) {
        m_cellWidth = cellWidth;
    }

    public double getScale() {
        return m_scale;
    }

    public void setScale(double scale) {
        m_scale = scale;
    }

    public void setLabelFont(Font font) {
        m_labelFont = font;
    }

    public Font getLabelFont() {
        return m_labelFont;
    }

    public void setBackgroundColor(Color color) {
        m_backgroundColor = color;
    }

    public Color getBackgroundColor() {
        return m_backgroundColor;
    }

    public void setHeadingFont(Font font) {
        m_headingFont = font;
    }

    public Font getHeadingFont() {
        return m_headingFont;
    }

    public int getValid() {
        return m_valid;
    }

    public NumberClass getNumbers() {
        return m_numberClass;
    }

    /*1min, 3min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week */
    public int getPriceListSize() {
        return m_priceList.size();
    }

    public int getChartTopY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.high.get());
    }

    public int getChartBottomY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.low.get());
    }

    public String getTimeStampString() {
        LocalDateTime time = m_lastUpdated.get();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(time);
    }

    public void setMsg(String msg) {
        m_msg = msg;
        m_lastUpdated.set(LocalDateTime.now());
    }

    public String getMsg() {
        return m_msg;
    }

    public int getTotalCellWidth() {
        return m_cellPadding + m_cellWidth;
    }

    private java.awt.Color m_scaleBgColor = new Color(0x33333350, true);

    public java.awt.Color getScaleBgColor() {
        return m_scaleBgColor;
    }

    public void setScaleBgColor(Color scaleBgColor) {
        m_scaleBgColor = scaleBgColor;
        m_lastUpdated.set(LocalDateTime.now());
    }

    private double m_lastClose = 0;

    public BufferedImage getBufferedImage() {

        int priceListSize = getPriceListSize();
        int totalCellWidth = getTotalCellWidth();

        int width = (int) m_chartWidth.get();
        int height = (int) m_chartHeight.get();

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        /*  g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);*/

        g2d.setColor(new Color(0f, 0f, 0f, 0.01f));
        g2d.fillRect(0, 0, width, height);
        if (m_valid == 1 && m_priceList.size() > 0) {

            // g2d.setColor(new Color(.8f, .8f, .8f, 1.0f));
            //    g2d.drawRect(chartRect.x, chartRect.y, chartRect.width, chartRect.height);
            double totalHigh = m_numberClass.high.get();
            double totalLow = m_numberClass.low.get();

            // double low = m_numberClass.low.get();
            //   double average = m_numberClass.getAverage();
            double scale = (0.6d * (double) height) / totalHigh;
            double totalRange = (totalHigh - totalLow) * scale;

            double rangePercent = totalRange / height;

            g2d.setFont(m_labelFont);
            g2d.setColor(Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            String measureString = "0.00000000";
            int stringWidth = fm.stringWidth(measureString);

            int labelAscent = fm.getAscent();
            int labelHeight = fm.getHeight();

            int scaleColWidth = stringWidth + 5;
            int scaleRowHeight = 30;

            int chartWidth = width - scaleColWidth;
            int chartHeight = height - scaleRowHeight;

            int cellWidth = m_cellWidth;
            int numCells = (int) Math.floor((width - scaleColWidth) / totalCellWidth);
            int j = 0;
            int i = numCells > priceListSize ? 0 : priceListSize - numCells;

            Color green = KucoinExchange.POSITIVE_COLOR;
            Color highlightGreen = KucoinExchange.POSITIVE_HIGHLIGHT_COLOR;
            Color garnetRed = KucoinExchange.NEGATIVE_COLOR;
            Color highlightRed = KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR;

            Color overlayRed = new Color(garnetRed.getRed(), garnetRed.getGreen(), garnetRed.getBlue(), 0x70);
            Color overlayRedHighlight = new Color(highlightRed.getRed(), highlightRed.getGreen(), highlightRed.getBlue(), 0x70);

            Color overlayGreen = new Color(green.getRed(), green.getGreen(), green.getBlue(), 0x70);
            Color overlayGreenHighlight = new Color(highlightGreen.getRed(), highlightGreen.getGreen(), highlightGreen.getBlue(), 0x70);

            try {
                Files.writeString(logFile.toPath(), "\ntotalHigh: " + totalHigh + "scale: " + scale + " cellWidth: " + cellWidth + " numCells: " + numCells + " width: " + width + " height: " + height);
            } catch (IOException e) {

            }

            boolean positive = false;
            boolean neutral = false;

            double open = m_priceList.get(0).getOpen();
            double close = getCurrentPrice();
            int closeY = (int) (close * scale);
            int openY = (int) (open * scale);

            int priceListWidth = priceListSize * totalCellWidth;

            int halfCellWidth = cellWidth / 2;

            while (i < m_priceList.size()) {
                PriceData priceData = m_priceList.get(i);

                int x = ((priceListWidth < chartWidth) ? (chartWidth - priceListWidth) : 0) + (j * (cellWidth + m_cellPadding));
                j++;
                open = m_lastClose;
                double low = priceData.getLow();
                double high = priceData.getHigh();
                open = priceData.getOpen();
                close = priceData.getClose();

                int lowY = (int) (low * scale);
                int highY = (int) (high * scale);
                openY = (int) (open * scale);

                positive = close > open;
                neutral = close == open;

                closeY = (int) (close * scale);
                Drawing.fillArea(img, 0xffffffff, x + halfCellWidth - 1, height - highY, x + halfCellWidth, height - lowY);

                if (neutral) {
                    //  g2d.setStroke(new BasicStroke(2));
                    //  double barHeight = (close - open) * scale;
                    //  g2d.setColor(Color.white);
                    //    g2d.drawLine(x + halfCellWidth, height - highY, x + halfCellWidth, height - closeY - 1);
                    //    g2d.drawLine(x + halfCellWidth, height - openY, x + halfCellWidth, height - lowY);
                    // g2d.setColor(Color.gray);
                    //  g2d.drawRect(x, height - closeY, cellWidth, (int) barHeight);
                    //  open = m_lastClose != 0 ? open = m_lastClose : open;

                    Drawing.drawBar(1, Color.lightGray, Color.gray, img, x, height - openY - 1, x + cellWidth, height - closeY);
                } else {
                    if (positive) {
                        //      g2d.setStroke(new BasicStroke(2));
                        // double barHeight = (close - open) * scale;
                        //     g2d.setColor(Color.white);

                        //  g2d.drawLine(x + halfCellWidth, height - highY, x + halfCellWidth, height - closeY - 1);
                        //  g2d.drawLine(x + halfCellWidth, height - openY, x + halfCellWidth, height - lowY);
                        //  g2d.setStroke(new BasicStroke(1));
                        // g2d.setColor(green);
                        int y1 = height - closeY;
                        int y2 = height - openY;

                        Drawing.drawBar(1, highlightGreen, green, img, x, y1, x + cellWidth, y2);
                        // drawBar(0, overlayGreenHighlight, overlayGreen, img, x, y1, x + halfCellWidth, y2);
                        Drawing.drawBar(0, overlayGreen, overlayGreenHighlight, img, x + halfCellWidth, y1, x + cellWidth, y2);

                        int RGBhighlight = highlightGreen.getRGB();

                        Drawing.fillArea(img, RGBhighlight, x, y1, x + 1, y2);
                        Drawing.fillArea(img, RGBhighlight, x, y1, x + cellWidth, y1 + 1);
                        Drawing.fillArea(img, KucoinExchange.POSITIVE_COLOR.getRGB(), x + cellWidth - 1, y1, x + cellWidth, y2);

                        // g2d.fillRect(x, height - closeY, cellWidth, (int) barHeight);
                        //g2d.drawRect(x, height - closeY, cellWidth, (int) barHeight);
                    } else {

                        // double barHeight = (open - close) * scale;
                        //    g2d.setStroke(new BasicStroke(2));
                        // g2d.setColor(Color.white);
                        //g2d.drawLine(,, x + halfCellWidth, height - openY);
                        // g2d.drawLine(x + halfCellWidth, height - closeY, x + halfCellWidth, height - lowY);
                        //  g2d.setStroke(new BasicStroke(1));
                        int y1 = height - openY;
                        int y2 = height - closeY;

                        Drawing.drawBar(1, garnetRed, highlightRed, img, x, y1, x + cellWidth, y2);
                        //  drawBar(overlayRed, overlayRedHighlight, img, x, y1, x + halfCellWidth, y2);
                        Drawing.drawBar(0, overlayRed, overlayRedHighlight, img, x, y1, x + cellWidth, y2);

                        int RGBhighlight = overlayRedHighlight.getRGB();

                        Drawing.fillArea(img, RGBhighlight, x, y1, x + 1, y2);
                        Drawing.fillArea(img, RGBhighlight, x, y1, x + cellWidth, y1 + 1);
                        Drawing.fillArea(img, garnetRed.getRGB(), x + cellWidth - 1, y1, x + cellWidth, y2);

                        // g2d.setColor(garnetRed);
                        // g2d.fillRect(x, height - openY, cellWidth, (int) barHeight);
                        //  g2d.drawRect(x, height - openY, cellWidth, (int) barHeight);
                    }
                }
                i++;
            }

            g2d.setFont(m_labelFont);
            fm = g2d.getFontMetrics();

            int y = height - closeY;
            String closeString = close + "";

            /*  if (closeString.length() > measureString.length()) {
                closeString = String.format("%e", close);
            }*/
            stringWidth = fm.stringWidth(closeString);
            int halfLabelHeight = (labelHeight / 2);

            int stringY = (y - halfLabelHeight) + labelAscent;

            if (m_lastClose != 0 && close != m_lastClose) {
                m_direction = close > m_lastClose;
            } else {
                if (m_lastClose == 0) {
                    m_direction = positive;
                }
            }

            m_lastClose = close;
            int y1 = y - (halfLabelHeight + 5);
            int y2 = y + (halfLabelHeight + 5);
            int halfScaleColWidth = (scaleColWidth / 2);
            g2d.setColor(Color.WHITE);
            g2d.drawString("◄", chartWidth - 9, stringY);
            int stringX = width - scaleColWidth + (halfScaleColWidth - (stringWidth / 2));
            Color color1;
            Color color2;
            int RGBhighlight;
            int x1 = width - scaleColWidth + 5;

            if (m_direction) {
                RGBhighlight = highlightGreen.getRGB();
                // Drawing.drawBar(1,, img, width - scaleColWidth, y1, width, y2);
                color1 = green;
                color2 = new Color(0xff4bbd94, true);
                // g2d.setColor(Color.black);
                // g2d.drawString(closeString, width - scaleColWidth + (halfScaleColWidth - (stringWidth / 2)) + 2, stringY + 1);
            } else {
                RGBhighlight = highlightRed.getRGB();
                color1 = garnetRed;
                color2 = highlightRed;
            }

            Drawing.drawBar(1, color1, color2, img, x1, y1, width, y2);

            Drawing.fillArea(img, RGBhighlight, x1, y1, x1 + 1, y2);
            Drawing.fillArea(img, RGBhighlight, x1, y1, width - 1, y1 + 1);
            Drawing.fillArea(img, color1.getRGB(), width - 1, y1, width - 1, y2);

            Color stringColor = new java.awt.Color(0xffffffff, true);

            g2d.setColor(stringColor);
            g2d.drawString(closeString, stringX, stringY);
            g2d.dispose();
            if (m_direction) {
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xff333333, img, stringX, y - (labelHeight / 2) - 1, stringX + stringWidth, y + 4 - (labelHeight / 2));
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xff333333, 0xff000000, img, stringX, y + 4 - (labelHeight / 2), stringX + stringWidth, y + (labelHeight / 2));
            }
            int borderColor = 0xFF000000;

            Drawing.fillArea(img, borderColor, chartWidth, 0, chartWidth + 1, chartHeight);//(width - scaleColWidth, 0, width - 1, height - 1);

            for (int x = 0; x < width - scaleColWidth - 6; x++) {
                int p = img.getRGB(x, y);

                p = (0xFFFFFF - p) | 0xFF000000;

                img.setRGB(x, y, p);
            }

        } else {

            g2d.setFont(m_headingFont);
            FontMetrics fm = g2d.getFontMetrics();
            String text = m_msg;

            int stringWidth = fm.stringWidth(text);
            int fmAscent = fm.getAscent();
            int x = (width - stringWidth) / 2;
            int y = ((height - fm.getHeight()) / 2) + fmAscent;
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, x, y);
            g2d.dispose();
        }

        /*   File outputfile = new File("image.png");
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {

        } */
        return img;
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_lastUpdated;
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_changeListener = changeListener;
        if (m_changeListener != null) {
            m_lastUpdated.addListener(changeListener);

        } else {
            removeUpdateListener();
        }
        // m_lastUpdated.addListener();

    }

    public void removeUpdateListener() {
        if (m_changeListener != null) {
            m_lastUpdated.removeListener(m_changeListener);
        }
    }

    public void remove() {
        removeUpdateListener();
    }
}

/*           for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p = img.getRGB(x, y);

                      int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;
                    if ((y >= topY && y < bottomY) && (x >= newX)) {
                        newImg.setRGB(x - newX, y - topY, p);
                    }

                }
            }
 */
