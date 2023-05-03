package com.netnotes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.satergo.WalletKey.Local;
import com.utils.Utils;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

public class PriceChart {

    private ArrayList<PriceData> m_priceList = new ArrayList<>();

    public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<>();

    private boolean m_valid = false;
    private Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);

    Color m_backgroundColor = new Color(1f, 1f, 1f, 0f);
    Font m_labelFont = new Font("OCR A Extended", java.awt.Font.PLAIN, 10);
    private double m_scale = 0;
    public int m_cellWidth = 20;
    public int m_chartHeight = 800;
    private String m_msg = "Getting price information";
    private String m_timeSpan = "30min";
    private String m_symbol = "ERG-USDT";
    private double m_currentPrice = 0;

    public BufferedImage m_img = null;

    File logFile = new File("log.txt");

    public PriceChart() {
        updateKucoinPriceData(null);
    }

    public PriceChart(String timeSpan, String symbol, ProgressIndicator progressIndicator) {
        m_timeSpan = timeSpan;
        m_symbol = symbol;
        updateKucoinPriceData(progressIndicator);
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

    public String getSymbol() {
        return m_symbol;
    }

    public void setSymbol(String symbol) {
        m_symbol = symbol;
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

    public Font getFont() {
        return m_headingFont;
    }

    public boolean getValid() {
        return m_valid;
    }

    public class NumberClass {

        private SimpleDoubleProperty high = new SimpleDoubleProperty();
        private SimpleDoubleProperty low = new SimpleDoubleProperty();
        public SimpleDoubleProperty sum = new SimpleDoubleProperty();
        public SimpleIntegerProperty count = new SimpleIntegerProperty();

        public NumberClass() {
            count.set(0);
            sum.set(0);
            high.set(0);
            low.set(0);
        }

        public double getAverage() {
            return count.get() == 0 ? 0 : sum.get() / count.get();
        }
    }
    private NumberClass m_numberClass = new NumberClass();

    public NumberClass getNumbers() {
        return m_numberClass;
    }

    public void getKucoinPriceStream() {

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    URL url = new URL("https://api.kucoin.com/api/v1/bullet-public");

                    String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
                    //  String urlParameters = "param1=a&param2=b&param3=c";

                    //  byte[] postData = new byte[]{};
                    //  int postDataLength = postData.length;
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    //  conn.setDoOutput(false);
                    //   conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    //  conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    //   conn.setRequestProperty("charset", "utf-8");
                    //  conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    //   conn.setUseCaches(false);

                    conn.setRequestProperty("Content-Type", "application/json");

                    conn.setRequestProperty("User-Agent", USER_AGENT);

                    // long contentLength = con.getContentLengthLong();
                    inputStream = conn.getInputStream();

                    byte[] buffer = new byte[2048];

                    int length;
                    // long downloaded = 0;

                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);
                        //   downloaded += (long) length;

                    }
                    String jsonString = outputStream.toString();

                    JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
                    return jsonObject;
                } catch (JsonParseException | IOException e) {
                    return null;
                }

            }

        };
        task.setOnFailed(e -> {
            try {
                Files.writeString(logFile.toPath(), e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        });

        task.setOnSucceeded(e -> {

            JsonObject jsonObject = task.getValue();

            if (jsonObject != null) {
                JsonElement dataElement = jsonObject.get("data");
                if (dataElement != null) {
                    JsonObject dataObject = dataElement.getAsJsonObject();
                    String tokenString = dataObject.get("token").getAsString();

                }
            }
        });

        Thread t = new Thread(task);
        t.start();
    }

    /*1min, 3min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week */
    public void updateKucoinPriceData(ProgressIndicator progressIndicator) {

        String urlString = "https://api.kucoin.com/api/v1/market/candles?type=" + m_timeSpan + "&symbol=" + m_symbol;
        try {
            Files.writeString(logFile.toPath(), "url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Utils.getUrlData(urlString, success -> {
            ByteArrayOutputStream output = (ByteArrayOutputStream) success.getSource().getValue();

            if (output != null) {
                String outputString = output.toString();
                JsonObject json = null;
                try {
                    json = new JsonParser().parse(outputString).getAsJsonObject();
                } catch (JsonParseException e) {
                    m_valid = false;
                    m_priceList.clear();

                }

                if (json != null) {

                    ArrayList<PriceData> tmpPriceList = new ArrayList<>();
                    JsonArray jsonArray = json.get("data").getAsJsonArray();

                    jsonArray.forEach(dataElement -> {

                        JsonArray dataArray = dataElement.getAsJsonArray();

                        PriceData priceData = new PriceData(dataArray);
                        if (m_numberClass.low.get() == 0) {
                            m_numberClass.low.set(priceData.getLow());
                        }

                        m_numberClass.sum.set(m_numberClass.sum.get() + priceData.getClose());
                        m_numberClass.count.set(m_numberClass.count.get() + 1);
                        tmpPriceList.add(priceData);
                        if (priceData.getHigh() > m_numberClass.high.get()) {
                            m_numberClass.high.set(priceData.getHigh());
                        }
                        if (priceData.getLow() < m_numberClass.low.get()) {
                            m_numberClass.low.set(priceData.getLow());
                        }
                    });

                    Collections.reverse(tmpPriceList);
                    m_valid = true;

                    m_msg = "Retrieved Kucoin price data.";
                    m_priceList = tmpPriceList;
                    m_currentPrice = m_priceList.get(m_priceList.size() - 1).getClose();

                } else {
                    m_valid = false;
                    m_msg = "Unable to retrieve price information.";
                    m_priceList.clear();

                }

            } else {
                /*  try {
                    Files.writeString(testFile.toPath(), "nullstring");
                } catch (IOException e1) {

                }*/
                m_valid = false;
                m_msg = "Unable to retrieve price information.";
                m_priceList.clear();

            }
            updateBufferedImage();
        }, failed -> {
            /* try {
                Files.writeString(testFile.toPath(), failed.toString());
            } catch (IOException e1) {

            } */
            m_valid = false;
            m_msg = "Unable to retrieve price information.";
            updateBufferedImage();
        }, progressIndicator);
    }

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
        LocalDateTime time = lastUpdated.get();

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
        lastUpdated.set(LocalDateTime.now());
    }
}
