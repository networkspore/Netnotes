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

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class KucoinChart {

    private File logFile = new File("pricechart-log.txt");

    private NumberClass m_numberClass = new NumberClass();

    private ArrayList<PriceData> m_priceList = new ArrayList<>();

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();
    private ChangeListener<LocalDateTime> m_changeListener = null;
    private Stage m_stage = null;

    private boolean m_valid = false;
    private Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);

    Color m_backgroundColor = new Color(1f, 1f, 1f, 0f);
    Font m_labelFont = new Font("OCR A Extended", java.awt.Font.PLAIN, 10);
    private double m_scale = 0;
    private int m_cellWidth = 20;
    private int m_chartHeight = 800;
    private String m_msg = "Getting price information";
    private String m_timeSpan = "30min";
    private double m_currentPrice = 0;
    private BufferedImage m_img = null;

    private KucoinExchange m_kucoinExchange;

    public KucoinChart(KucoinExchange kucoinExchange, JsonObject jsonObject) {
        m_kucoinExchange = kucoinExchange;

        if (jsonObject != null) {
            JsonElement timeSpanElement = jsonObject.get("timeSpan");

            m_timeSpan = timeSpanElement == null ? "30min" : timeSpanElement.getAsString();
        }

    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("timeSpan", m_timeSpan);

        return jsonObject;
    }

    public void updatePriceDataList(JsonArray jsonArray) {

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

        updateBufferedImage();

    }

    private void updateCandleData(PriceData priceData) {
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
        updateBufferedImage();
    }

    public double getCurrentPrice() {
        return m_currentPrice;
    }

    public BufferedImage getChartBufferedImage() {
        return m_img;
    }

    public int getChartHeight() {
        return m_chartHeight;
    }

    public void setChartHeight(int chartHeight) {
        m_chartHeight = chartHeight;
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

    public String getTimespan() {
        return m_timeSpan;
    }

    public void setTimespan(String timespan) {
        m_timeSpan = timespan;
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

    public boolean getValid() {
        return m_valid;
    }

    public NumberClass getNumbers() {
        return m_numberClass;
    }

    /*1min, 3min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week */
    public int getPriceListSize() {
        return m_priceList.size();
    }

    public javafx.scene.image.Image getChartFxImage() {
        BufferedImage img = getChartBufferedImage();
        return SwingFXUtils.toFXImage(img, null);
    }

    public int getChartTopY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.high.get());
    }

    public int getChartBottomY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.low.get());
    }

    public BufferedImage zoomLatest(int cells) {
        if (m_valid) {
            int height = m_img.getHeight();
            int width = m_img.getWidth();

            int padding = 5;

            int priceListSize = m_priceList.size();

            NumberClass numberTracker = new NumberClass();
            numberTracker.low.set(Double.MAX_VALUE);
            for (int i = priceListSize - cells; i < priceListSize; i++) {
                double high = m_priceList.get(i).getHigh();
                double low = m_priceList.get(i).getLow();
                if (high > numberTracker.high.get()) {
                    numberTracker.high.set(high);
                }
                if (low < numberTracker.low.get()) {
                    numberTracker.low.set(low);
                }
            }

            int topY = (m_img.getHeight() - (int) (m_scale * numberTracker.high.get())) - padding;
            int bottomY = (m_img.getHeight() - (int) (m_scale * numberTracker.low.get())) + padding;

            int newHeight = bottomY - topY;
            int newWidth = (m_cellWidth + 2) * cells;
            int newX = width - newWidth;

            BufferedImage newImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p = m_img.getRGB(x, y);

                    /*  int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;*/
                    if ((y >= topY && y < bottomY) && (x >= newX)) {
                        newImg.setRGB(x - newX, y - topY, p);
                    }

                }
            }

//.getSubimage(imgWidth - width, 200, width, imgHeight - 200)
            return newImg;
        } else {
            return m_img;
        }
    }

    public String getTimeStampString() {
        LocalDateTime time = m_lastUpdated.get();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(time);
    }

    public void updateBufferedImage() {

        try {
            Files.writeString(logFile.toPath(), "--creating image--", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        int priceListSize = getPriceListSize();

        int width = m_valid ? (priceListSize * m_cellWidth + (2 * priceListSize)) : 600;
        int height = m_chartHeight;

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
        if (m_valid) {

            // g2d.setColor(new Color(.8f, .8f, .8f, 1.0f));
            //    g2d.drawRect(chartRect.x, chartRect.y, chartRect.width, chartRect.height);
            int i = 0;

            double totalHigh = m_numberClass.high.get();
            // double low = m_numberClass.low.get();
            //   double average = m_numberClass.getAverage();
            m_scale = (0.95 * height) / totalHigh;

            while (i < priceListSize) {
                int x = i * (m_cellWidth + 2);
                double low = m_priceList.get(i).getLow();
                double high = m_priceList.get(i).getHigh();
                double open = m_priceList.get(i).getOpen();
                double close = m_priceList.get(i).getClose();

                int lowY = (int) (low * m_scale);
                int highY = (int) (high * m_scale);
                int openY = (int) (open * m_scale);

                boolean positive = close > open;
                boolean neutral = close == open;

                int closeY = (int) (close * m_scale);

                Color emeraldGreen = new Color(0x04, 0x63, 0x07, 0xff);
                Color garnetRed = new Color(0x7A, 0x20, 0x21, 0xff);

                if (positive || neutral) {
                    g2d.setStroke(new BasicStroke(2));
                    double barHeight = (close - open) * m_scale;
                    g2d.setColor(Color.white);
                    g2d.drawLine(x + (m_cellWidth / 2), height - highY, x + (m_cellWidth / 2), height - closeY - 1);
                    g2d.drawLine(x + (m_cellWidth / 2), height - openY, x + (m_cellWidth / 2), height - lowY);

                    g2d.setStroke(new BasicStroke(1));

                    g2d.setColor(emeraldGreen);
                    g2d.fillRect(x, height - closeY, m_cellWidth, (int) barHeight);

                    g2d.drawRect(x, height - closeY, m_cellWidth, (int) barHeight);

                } else {

                    double barHeight = (open - close) * m_scale;
                    g2d.setStroke(new BasicStroke(2));
                    g2d.setColor(Color.white);
                    g2d.drawLine(x + (m_cellWidth / 2), height - highY, x + (m_cellWidth / 2), height - openY);
                    g2d.drawLine(x + (m_cellWidth / 2), height - closeY, x + (m_cellWidth / 2), height - lowY);

                    g2d.setStroke(new BasicStroke(1));
                    g2d.setColor(garnetRed);
                    g2d.fillRect(x, height - openY, m_cellWidth, (int) barHeight);

                    g2d.setColor(garnetRed);

                    g2d.drawRect(x, height - openY, m_cellWidth, (int) barHeight);

                }

                i = i + 1;
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
        }

        g2d.dispose();

        /*   File outputfile = new File("image.png");
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {

        } */
        m_img = img;
        m_lastUpdated.set(LocalDateTime.now());
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
