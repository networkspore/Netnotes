package com.netnotes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
//import java.io.File;
import java.sql.Time;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class SpectrumChartView {

   private File logFile = new File("netnotes-log.txt");

   public final static int DECIMAL_PRECISION = 6;

   private TimeSpan m_timeSpan = new TimeSpan("30min");

    private SpectrumNumbers m_numberClass = new SpectrumNumbers();

    private ArrayList<SpectrumPriceData> m_priceList = new ArrayList<>();

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();
    private ChangeListener<LocalDateTime> m_changeListener = null;

    private SimpleDoubleProperty m_topVvalue = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty m_bottomVvalue = new SimpleDoubleProperty(0);
    private SimpleBooleanProperty m_settingRange = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty m_active = new SimpleBooleanProperty(false);

    private int m_valid = 0;
    private Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);

    private Color m_backgroundColor = new Color(1f, 1f, 1f, 0f);
    private SimpleObjectProperty<Font> m_labelFont = new SimpleObjectProperty< Font>(new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 12));

    private double m_scale = 0;
    private int m_defaultCellWidth = 20;
    private int m_cellWidth = m_defaultCellWidth;
    private SimpleDoubleProperty m_chartHeight;
    private SimpleDoubleProperty m_chartWidth;

    private boolean m_direction = false;

    private String m_msg = "Loading";

    private BigDecimal m_currentPrice = BigDecimal.ZERO;
    // private BufferedImage m_img = null;
    private int m_cellPadding = 3;
    private HBox m_chartHbox = new HBox();
    private int m_scaleColWidth = 0;
    private int m_labelHeight = 0;
    private int m_amStringWidth = 0;
    private int m_labelAscent = 0;
    private FontMetrics m_fm = null;

    private long m_lastTimeStamp = 0;

    public SpectrumChartView(SimpleDoubleProperty width, SimpleDoubleProperty height, TimeSpan timeSpan) {
        HBox.setHgrow(m_chartHbox, Priority.ALWAYS);
        m_chartHbox.setAlignment(Pos.CENTER);
        m_chartWidth = width;
        m_chartHeight = height;
        m_timeSpan = timeSpan;
        updateLabelFont();
        updateBufferedImage();
    }

    public long getLastTimestamp(){
        return m_lastTimeStamp;
    }

    public void setLastTimeStamp(long timestamp){
        m_lastTimeStamp = timestamp;
    }

    public TimeSpan getTimeSpan(){
        return m_timeSpan;
    }

    public void setTimeSpan(TimeSpan timeSpan){
        m_timeSpan = timeSpan;
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        return jsonObject;
    }

    public SimpleBooleanProperty rangeActiveProperty() {
        return m_active;
    }

    public SimpleDoubleProperty rangeTopVvalueProperty() {
        return m_topVvalue;
    }

    public SimpleDoubleProperty rangeBottomVvalueProperty() {
        return m_bottomVvalue;
    }

    public SimpleBooleanProperty isSettingRangeProperty() {
        return m_settingRange;
    }

    public void updateLabelFont() {

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(m_labelFont.get());
        g2d.setColor(m_labelColor);
        m_fm = g2d.getFontMetrics();
        String measureString = "0.00000000";
        int stringWidth = m_fm.stringWidth(measureString);
        m_amStringWidth = m_fm.stringWidth(" a.m. ");
        m_labelAscent = m_fm.getAscent();
        m_labelHeight = m_fm.getHeight();
        m_scaleColWidth = stringWidth + 25;

    }

    private SimpleObjectProperty<Image> m_imageBuffer = new SimpleObjectProperty<>(null);

    public HBox getChartBox() {

        m_labelFont.addListener((obs, oldVal, newVal) -> {
            updateLabelFont();
            m_lastUpdated.set(LocalDateTime.now());
        });
        
        updateBufferedImage();
        ImageView imgView = new ImageView();
        imgView.setPreserveRatio(true);

        m_imageBuffer.addListener((obs,oldval,newval)->{
            if(newval != null){
                imgView.setImage(newval);
                imgView.setFitWidth(newval.getWidth());
            }
        });



        m_chartHeight.addListener((obs, oldVal, newVal) -> updateBufferedImage());

        //  m_chartWidth.addListener((obs, oldVal, newVal) -> updateImage.run());
        m_lastUpdated.addListener((obs, oldVal, newVal) -> updateBufferedImage());

        m_chartHbox.getChildren().clear();
        m_chartHbox.getChildren().add(imgView);

        rangeBottomVvalueProperty().addListener((obs, oldVal, newVal) -> updateBufferedImage());
        rangeTopVvalueProperty().addListener((obs, oldVal, newVal) -> updateBufferedImage());
        isSettingRangeProperty().addListener((obs, oldVal, newVal) -> updateBufferedImage());

        rangeActiveProperty().addListener((obs, oldVal, newVal) -> updateBufferedImage());

        return m_chartHbox;
    }

    public void reset() {
        m_valid = 0;
        m_priceList.clear();
        m_msg = "Loading";
        m_lastUpdated.set(LocalDateTime.now());
    }

    /*
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

    } */
    public static int getDecimals(String string) {
         int indexOfDecimal = string.indexOf(".");

        int numZeros = 0;
        
        while(indexOfDecimal != -1 && indexOfDecimal < string.length() && string.charAt(indexOfDecimal + 1) == '0'){
            indexOfDecimal ++;
            numZeros++;
        }
                        
        int decimals = numZeros == 0 ? 0 : numZeros + DECIMAL_PRECISION;
                        
        return decimals;
    }

    private static SpectrumPriceData getEpochElement(JsonArray array, long epochStart, long longEpochEnd, BigDecimal prevClose) {
        SimpleObjectProperty<SpectrumPriceData> priceData = new SimpleObjectProperty<>(null);
       // SimpleObjectProperty<BigDecimal> prevPrice = new SimpleObjectProperty<>(BigDecimal.ZERO);

        for (JsonElement jsonElement : array) {
            if (jsonElement != null && jsonElement.isJsonObject()) {

                JsonObject priceObject = jsonElement.getAsJsonObject();
                JsonElement timeStampElement = priceObject.get("timestamp");
                JsonElement priceElement = priceObject.get("price");

                if(timeStampElement != null && timeStampElement.isJsonPrimitive() &&
                    priceElement != null && priceElement.isJsonPrimitive()
                ){
                    long timestamp = timeStampElement.getAsLong();
                    BigDecimal price = priceElement.getAsBigDecimal();

                    if (timestamp > epochStart && timestamp <= longEpochEnd) {
                        if(priceData.get() == null){
                            priceData.set(new SpectrumPriceData(longEpochEnd, price));
                        }
                        priceData.get().addPrice(price);
                    }else{
                        if(timestamp > longEpochEnd){
                            if(priceData.get() != null){
                                return priceData.get();
                            }else{
                                if(prevClose != null){
                                    return new SpectrumPriceData(longEpochEnd, prevClose );
                                }else{
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        }
        if(prevClose != null){
            return new SpectrumPriceData(longEpochEnd, prevClose );
        }else{
            return null;
        }
    }
    public final static int MAX_BARS = 250;

    public void setPriceDataList(JsonArray jsonArray) {
        long timeSpanMillis = m_timeSpan.getMillis();

        if (jsonArray != null && jsonArray.size() > 0) {
            m_valid = 1;
            m_msg = "Loading";
            SpectrumNumbers numberClass = new SpectrumNumbers();

            ArrayList<SpectrumPriceData> tmpPriceList = new ArrayList<>();

            JsonElement oldestElement = jsonArray.get(0);
            JsonElement newestElement = jsonArray.get(jsonArray.size() - 1);

            

            if (oldestElement != null && oldestElement.isJsonObject() && newestElement != null && newestElement.isJsonObject()) {
              
                JsonObject oldestObject = oldestElement.getAsJsonObject();
                JsonObject newestObject = newestElement.getAsJsonObject();
                m_currentPrice = newestObject.get("price").getAsBigDecimal();
                long oldestTimeStamp = oldestObject.get("timestamp").getAsLong();
                long newestTimeStamp = newestObject.get("timestamp").getAsLong();

                int elements = (int) Math.ceil(((newestTimeStamp + timeSpanMillis) - oldestTimeStamp) / timeSpanMillis);
             

                int i = elements > MAX_BARS ? elements - MAX_BARS : 0;


                try {
                    Files.writeString(logFile.toPath(), "oldestObject: " + oldestObject.toString() + "\nnewest Element:" + newestObject.toString() + "\ntimespan: " + timeSpanMillis  + "\nelements: " + elements, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
             
                }

                while (i < elements) {
                    long epochStart = (i * timeSpanMillis) + oldestTimeStamp - timeSpanMillis;
                    long epochEnd = (i * timeSpanMillis) + oldestTimeStamp;

                    SimpleObjectProperty<SpectrumPriceData> prevPriceData = new SimpleObjectProperty<>(numberClass.getCount() == 0 ? null : tmpPriceList.get(numberClass.getCount() - 1));

                    if(i != 0){
                        int j = i;
                        while( j >= 0 && prevPriceData.get() == null){
                            long prevEpochStart = (j * timeSpanMillis) + oldestTimeStamp - timeSpanMillis;
                            long prevEpochEnd = (j * timeSpanMillis) + oldestTimeStamp;
                            prevPriceData.set(getEpochElement(jsonArray, prevEpochStart, prevEpochEnd, null));
                            j--;
                        }
                    }

                    SpectrumPriceData priceData = getEpochElement(jsonArray, epochStart, epochEnd, prevPriceData.get() != null ? prevPriceData.get().getClose() : BigDecimal.ZERO);
       

                    if (numberClass.getLow().equals(BigDecimal.ZERO)) {
                        numberClass.setLow(priceData.getLow());
                    }

                    numberClass.setSum(numberClass.getSum().add(priceData.getClose()));
                

                    numberClass.setHigh(priceData.getHigh().max(numberClass.getHigh()));

                    numberClass.setLow(priceData.getLow().min(numberClass.getLow()));
                    
                    String closeString = priceData.getCloseString();
                    int indexOfDecimal = closeString.indexOf(".");

                    int numZeros = 0;
                    
                    while(indexOfDecimal != -1 && indexOfDecimal < closeString.length() && closeString.charAt(indexOfDecimal + 1) == '0'){
                        indexOfDecimal ++;
                        numZeros++;
                    }
                    
                    int decimals = numZeros == 0 ? 0 : numZeros + DECIMAL_PRECISION;
                    
                    numberClass.setDecimals(Integer.max(decimals, numberClass.getDecimals()));
                    
                    numberClass.setCount(numberClass.getCount() + 1);
                    tmpPriceList.add(priceData);
                    i++;
                }
                
                
                // Collections.reverse(tmpPriceList);
            } else {
                m_valid = 2;
                m_msg = "Received no data.";
            }
            m_priceList = tmpPriceList;
            m_numberClass = numberClass;
            try {
                
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyString = gson.toJson(getPriceDataJson());
                Files.writeString(new File("priceList.json").toPath(), prettyString);
            } catch (IOException e1) {
            
            }
        } else {
            m_valid = 2;
            m_msg = "Received no data.";
        }
        m_lastUpdated.set(LocalDateTime.now());
    }

    public JsonObject getPriceDataJson(){
        JsonObject json = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for(SpectrumPriceData dataItem : m_priceList){
            jsonArray.add(dataItem.getJsonObject());
        }

        json.add("priceData", jsonArray);

        return json;
    }

    public void addPrices(JsonArray jsonArray) {
      /*   int priceListSize = m_priceList.size();

        if (priceListSize > 0) {
            long lastTimeStamp = m_priceList.

            int lastIndex = m_priceList.size() - 1;
            SpectrumPriceData lastData = m_priceList.get(lastIndex);

            long lastTimeStamp = lastData.getTimestamp();

            if (lastTimeStamp > timeStamp) {
                //out of sync
            } else {

                if (lastTimeStamp <= timeStamp) {
                    m_priceList.get(lastIndex).addPrice(price);
                } else {

                    long timeSinceLastUpdate = priceData.getTimestamp() - lastTimeStamp;

                    if (timeSinceLastUpdate == timeSpanMillis) {
                        m_priceList.add(priceData);
                    } else {

                        int intervalsSinceLastUpdate = (int) Math.ceil(timeSinceLastUpdate / timeSpanMillis);
                        double lastClose = lastData.getClose();

                        for (int i = 0; i < (intervalsSinceLastUpdate - 1); i++) {
                            m_priceList.add(new PriceData(lastTimeStamp + (i * timeSpanMillis), lastClose, lastClose, lastClose, lastClose, 0, 0));
                        }
                        m_priceList.add(priceData);
                    }

                }
            }

        } else {
            setPriceDataList(jsonArray);

        }
        m_currentPrice = priceData.getClose();*/
        m_lastUpdated.set(LocalDateTime.now());
    }

    public BigDecimal getCurrentPrice() {
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
        m_labelFont.set(font);
    }

    public Font getLabelFont() {
        return m_labelFont.get();
    }

    public SimpleObjectProperty<Font> labelFontProperty() {
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

    public SpectrumNumbers getNumbers() {
        return m_numberClass;
    }

    /*1min, 3min, 15min, 30min, 1hour, 2hour, 4hour, 6hour, 8hour, 12hour, 1day, 1week */
    public int getPriceListSize() {
        return m_priceList.size();
    }

    public int getChartTopY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.getHigh().doubleValue());
    }

    public int getChartBottomY(BufferedImage img) {
        return img.getHeight() - (int) (m_scale * m_numberClass.getLow().doubleValue());
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

    private double m_lastClose = 0;
    private int m_labelSpacingSize = 150;
    private Color m_labelColor = new Color(0xc0ffffff);

    private double m_topRangePrice = 0;
    private double m_botRangePrice = 0;

    private int getRowHeight() {
        return m_labelHeight + 7;
    }

    public void updateBufferedImage() {
        LocalDateTime now = LocalDateTime.now();
        int greenHighlightRGB = 0x504bbd94;
      //  int greenHighlightRGB2 = 0x80028a0f;
        int redRGBhighlight = 0x50e96d71;
      //  int redRGBhighlight2 = 0x809a2a2a;

        int priceListSize = getPriceListSize();
        int totalCellWidth = getTotalCellWidth();

        double bottomVvalue = rangeBottomVvalueProperty().get();
        double topVvalue = rangeTopVvalueProperty().get();
        boolean isRangeSetting = isSettingRangeProperty().get();

        boolean rangeActive = rangeActiveProperty().get() && !isRangeSetting;

        int cellWidth = m_cellWidth;
        int width = m_priceList.size() == 0 ? (int) m_chartWidth.get() : m_scaleColWidth + (m_priceList.size() * totalCellWidth);
        width = width < m_chartWidth.get() ? (int) m_chartWidth.get() : width;

        int height = (int) Math.ceil(m_chartHeight.get());

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        g2d.setColor(new Color(0f, 0f, 0f, 0.01f));
        g2d.fillRect(0, 0, width, height);

        g2d.setFont(getLabelFont());
        g2d.setColor(m_labelColor);
        if (m_valid == 1 && m_priceList.size() > 0) {

            int chartWidth = width - m_scaleColWidth;
            int chartHeight = height - (2 * (m_labelHeight + 5)) - 10;

            int numCells = (int) Math.floor((width - m_scaleColWidth) / totalCellWidth);

            int i = numCells > priceListSize ? 0 : priceListSize - numCells;

            SpectrumNumbers nc = new SpectrumNumbers();

            for (int j = i; j < priceListSize; j++) {
                SpectrumPriceData priceData = m_priceList.get(j);

                if (nc.getLow().equals(BigDecimal.ZERO)) {
                    nc.setLow(priceData.getLow());
                }

                nc.setSum(nc.getSum().add(priceData.getClose()));
                nc.setCount(nc.getCount() + 1);
                nc.setHigh(priceData.getHigh().max(nc.getHigh()));
                
               
                nc.setLow(priceData.getLow().min(nc.getLow()));
                
                int decimals = getDecimals(priceData.getCloseString());

                if (decimals > nc.getDecimals()) {
                    nc.setDecimals(decimals);
                }
            }

            double scale = (.6d * ((double) chartHeight)) / nc.getHigh().doubleValue();

            double topRangePrice = (topVvalue * (Math.floor(chartHeight / getRowHeight()) * getRowHeight())) / scale;
            double botRangePrice = (bottomVvalue * (Math.floor(chartHeight / getRowHeight()) * getRowHeight())) / scale;

            if (isRangeSetting) {
                m_topRangePrice = topRangePrice;
                m_botRangePrice = botRangePrice;
            } else {
                topRangePrice = m_topRangePrice;
                botRangePrice = m_botRangePrice;
            }

            double scale2 = (double) chartHeight / (topRangePrice - botRangePrice);

            Drawing.fillArea(img, 0xff111111, 0, 0, chartWidth, chartHeight);

            int j = 0;

            //    Color green = KucoinExchange.POSITIVE_COLOR;
            Color highlightGreen = KucoinExchange.POSITIVE_HIGHLIGHT_COLOR;
            Color garnetRed = KucoinExchange.NEGATIVE_COLOR;
            Color highlightRed = KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR;

            Color overlayRed = new Color(garnetRed.getRed(), garnetRed.getGreen(), garnetRed.getBlue(), 0x70);
            Color overlayRedHighlight = new Color(highlightRed.getRed(), highlightRed.getGreen(), highlightRed.getBlue(), 0x70);

            int priceListWidth = priceListSize * totalCellWidth;

            int halfCellWidth = cellWidth / 2;

            int items = m_priceList.size() - i;
            int colLabelSpacing = (int) Math.floor(items / ((items * cellWidth) / m_labelSpacingSize));

            int rowHeight = getRowHeight();

            int rows = (int) Math.floor(chartHeight / rowHeight);

            int rowLabelSpacing = (int) (rows / ((rows * rowHeight) / m_labelSpacingSize));

            for (j = 0; j < rows; j++) {

                int y = chartHeight - (j * rowHeight);
                if (j % rowLabelSpacing == 0) {
                    Drawing.fillAreaDotted(2, img, 0x10ffffff, 0, y, chartWidth, y + 1);
                    if (j != 0) {
                        Drawing.fillArea(img, 0xc0ffffff, chartWidth, y, chartWidth + 6, y + 2);
                    }
                }
                Drawing.fillArea(img, 0xff000000, chartWidth, y, chartWidth + 6, y + 1);

                double scaleLabeldbl = rangeActive ? (double) ((j * rowHeight) / scale2) + botRangePrice : (double) (j * rowHeight) / scale;

                String scaleAmount = String.format("%." + m_numberClass.getDecimals() + "f", scaleLabeldbl);
                int amountWidth = m_fm.stringWidth(scaleAmount);

                int x1 = (chartWidth + (m_scaleColWidth / 2)) - (amountWidth / 2);
                int y1 = (y - (m_labelHeight / 2)) + m_labelAscent;

                g2d.drawString(scaleAmount, x1, y1);

            }
            j = 0;

            while (i < m_priceList.size()) {
                SpectrumPriceData priceData = m_priceList.get(i);

                int x = ((priceListWidth < chartWidth) ? (chartWidth - priceListWidth) : 0) + (j * (cellWidth + m_cellPadding));
                j++;

                double low = priceData.getOpen().doubleValue() < priceData.getLow().doubleValue() ? priceData.getOpen().doubleValue() : priceData.getLow().doubleValue();
                double high = priceData.getHigh().doubleValue();

                double nextOpen = (i < m_priceList.size() - 2 ? m_priceList.get(i + 1).getOpen().doubleValue() : priceData.getClose().doubleValue());
                double prevClose = i > 0 ? m_priceList.get(i - 1).getClose().doubleValue() : priceData.getOpen().doubleValue();
                double close = priceData.getClose().doubleValue();

                double open = prevClose;

                int lowY = (int) (low * scale);
                int highY = (int) (high * scale);
                int openY = (int) (open * scale);
                int closeY = (int) (close * scale);

                if (rangeActive) {

                    lowY = (int) ((low - botRangePrice) * scale2);
                    highY = (int) ((high - botRangePrice) * scale2);
                    openY = (int) ((open - botRangePrice) * scale2);
                    closeY = (int) ((close - botRangePrice) * scale2);

                    lowY = lowY < 0 ? 0 : lowY;
                    lowY = lowY > chartHeight ? chartHeight : lowY;

                    highY = highY < 0 ? 0 : highY;
                    highY = highY > chartHeight ? chartHeight : highY;

                    openY = openY < 0 ? 0 : openY;
                    openY = openY > chartHeight ? chartHeight : openY;

                    closeY = closeY < 0 ? 0 : closeY;
                    closeY = closeY > chartHeight ? chartHeight : closeY;

                }

                boolean positive = !((height - closeY) > (height - openY));
                boolean neutral = open == nextOpen && open == close;

                LocalDateTime localTimestamp = priceData.getLocalDateTime();

                if (localTimestamp != null) {
                    if (i % colLabelSpacing == 0) {

                        Drawing.fillAreaDotted(2, img, 0x10ffffff, x + halfCellWidth - 1, 0, x + halfCellWidth, chartHeight);
                        Drawing.fillArea(img, 0x80000000, x + halfCellWidth - 1, chartHeight, x + halfCellWidth + 1, chartHeight + 4);

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

                        String timeString = formatter.format(localTimestamp);
                        int timeStringWidth = m_fm.stringWidth(timeString);

                        int timeStringX = x - ((timeStringWidth - m_amStringWidth) / 2);
                        g2d.drawString(timeString, timeStringX, chartHeight + 4 + (m_labelHeight / 2) + m_labelAscent);

                        if (!(localTimestamp.getDayOfYear() == now.getDayOfYear() && localTimestamp.getYear() == now.getYear())) {

                            formatter = DateTimeFormatter.ofPattern("MM/dd/YYYY");

                            timeString = formatter.format(localTimestamp);

                            timeStringWidth = m_fm.stringWidth(timeString);

                            timeStringX = x - ((timeStringWidth - m_amStringWidth) / 2);

                            g2d.drawString(timeString, timeStringX, chartHeight + 4 + m_labelHeight + 4 + (m_labelHeight / 2) + m_labelAscent);

                        }
                    }

                }

                if (highY != lowY) {
                    Drawing.fillArea(img, 0xffffffff, x + halfCellWidth - 1, chartHeight - highY, x + halfCellWidth, chartHeight - lowY);
                }

                if (neutral) {

                    Drawing.drawBar(1, Color.lightGray, Color.gray, img, x, chartHeight - openY - 1, x + cellWidth, chartHeight - closeY + 1);

                } else {
                    if (positive) {

                        int y1 = chartHeight - closeY;
                        int y2 = (chartHeight - openY) + 1;

                        int x2 = x + cellWidth;
                        Drawing.drawBar(1, 0xff000000, 0xff111111, img, x, y1, x2, y2);
                        Drawing.drawBar(1, greenHighlightRGB, 0xff000000, img, x, y1, x2, y2);
                        Drawing.drawBar(0, 0x104bbd94, 0x004bbd94, img, x, y1, x2, y2);

                        int RGBhighlight = highlightGreen.getRGB();

                        Drawing.fillArea(img, 0x804bbd94, x + 1, y1, x2, y1 + 1);

                        Drawing.drawBar(0x80555555, RGBhighlight, img, x2, y1 + 3, x2 - 2, y2);

                        Drawing.fillArea(img, 0x30000000, x, y2 - 1, x2, y2);

                    } else {

                        int y1 = chartHeight - openY;
                        int y2 = chartHeight - closeY;

                        Drawing.drawBar(1, garnetRed, highlightRed, img, x, y1, x + cellWidth, y2);
                        Drawing.drawBar(0, overlayRed, overlayRedHighlight, img, x, y1, x + cellWidth, y2);

                        int RGBhighlight = overlayRedHighlight.getRGB();

                        Drawing.fillArea(img, RGBhighlight, x, y1, x + 1, y2);
                        Drawing.fillArea(img, RGBhighlight, x, y1, x + cellWidth, y1 + 1);
                        Drawing.fillArea(img, garnetRed.getRGB(), x + cellWidth - 1, y1, x + cellWidth, y2);

                        Drawing.fillArea(img, garnetRed.getRGB(), x + 1, y2 - 1, x + cellWidth - 1, y2);
                    }
                }

                i++;
            }

            int halfLabelHeight = (m_labelHeight / 2);
            int currentCloseY = (int) (getCurrentPrice().doubleValue() * scale);

            int y = chartHeight - currentCloseY;
            int minY = (halfLabelHeight + 8);
            int maxY = chartHeight;

            boolean outOfBounds = false;

            if (rangeActive) {
                y = (int) (chartHeight - ((getCurrentPrice().doubleValue() - botRangePrice) * scale2));

                if (y < minY) {
                    outOfBounds = true;
                    y = minY;
                } else {
                    if (y > maxY) {
                        outOfBounds = true;
                        y = maxY;
                    }
                }

            }

            y = y < 0 ? 0 : y > chartHeight ? chartHeight : y;

            m_direction = getCurrentPrice().doubleValue() > m_lastClose;

            m_lastClose = getCurrentPrice().doubleValue();
            int y1 = y - (halfLabelHeight + 7);
            int y2 = y + (halfLabelHeight + 5);
            int halfScaleColWidth = (m_scaleColWidth / 2);

            int RGBhighlight;

            int x1 = chartWidth + 1;
            int x2 = chartWidth + m_scaleColWidth;

            if (m_direction) {
                RGBhighlight = greenHighlightRGB;
            } else {
                RGBhighlight = redRGBhighlight;
            }

            Drawing.drawBar(1, 0xff000000, 0xff111111, img, x1, y1, width, y2);
            Drawing.drawBar(1, RGBhighlight, 0xff000000, img, x1, y1, width, y2);

            y2 = y2 + 1;

            Drawing.drawBar(1, 0x90000000, RGBhighlight, img, x1, y1 - 1, x2, y1 + 3);
            Drawing.drawBar(0x80555555, RGBhighlight, img, x1 - 2, y1 + 3, x1 + 1, y2 - 1);
            Drawing.drawBar(0x80555555, RGBhighlight, img, x2, y1 + 4, x2 - 2, y2);
            Drawing.drawBar(1, 0x50ffffff, RGBhighlight, img, x1, y2 - 1, x2, y2);

            Color stringColor = new java.awt.Color(0xffffffff, true);

            String closeString = String.format("%." + m_numberClass.getDecimals() + "f", getCurrentPrice());
            int stringWidth = m_fm.stringWidth(closeString);

            int stringY = (y - halfLabelHeight) + m_labelAscent;
            int stringX = (x1 + halfScaleColWidth) - (stringWidth / 2);

            g2d.setColor(stringColor);
            g2d.drawString(closeString, stringX, stringY);
            if (!outOfBounds) {
                g2d.setColor(m_direction ? KucoinExchange.POSITIVE_HIGHLIGHT_COLOR : KucoinExchange.NEGATIVE_HIGHLIGHT_COLOR);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("◄", chartWidth - 9, stringY);
            }
            stringX = chartWidth - 9;
            if (m_direction) {
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffffffff, img, stringX, y - (m_labelHeight / 2) - 1, chartWidth + m_scaleColWidth, y + 4 - (m_labelHeight / 2));
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xff4bbd94, img, stringX, y + 4 - (m_labelHeight / 2), chartWidth + m_scaleColWidth, y + (m_labelHeight / 2));
            } else {
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffffffff, img, stringX, y - (m_labelHeight / 2) - 1, chartWidth + m_scaleColWidth, y + 4 - (m_labelHeight / 2));
                Drawing.drawBarFillColor(1, false, stringColor.getRGB(), 0xffffffff, 0xffe96d71, img, stringX, y + 4 - (m_labelHeight / 2), chartWidth + m_scaleColWidth, y + (m_labelHeight / 2));
            }
            int borderColor = 0xFF000000;

            Drawing.fillArea(img, borderColor, 0, chartHeight, chartWidth, chartHeight + 1);

            Drawing.fillArea(img, borderColor, chartWidth, 0, chartWidth + 1, chartHeight);//(width - scaleColWidth, 0, width - 1, chartHeight - 1);
            if (!outOfBounds) {
                for (int x = 0; x < width - m_scaleColWidth - 7; x++) {
                    int p = img.getRGB(x, y);

                    p = (0xFFFFFF - p) | 0xFF000000;

                    img.setRGB(x, y, p);
                }
            }
            if (isRangeSetting) {

                x1 = x1 + 1;
                Color blackColor = new java.awt.Color(0xff000000, true);

                g2d.setFont(getLabelFont());

                int topRangeY = (int) Math.ceil(chartHeight - (topVvalue * (rows * rowHeight)));
                int botRangeY = (int) Math.ceil(chartHeight - (bottomVvalue * ((rows * rowHeight))));

                Drawing.fillArea(img, 0x40ffffff, 0, 0, width, topRangeY);
                Drawing.fillArea(img, 0x40ffffff, 0, botRangeY, width, chartHeight);

                String topRangeString = String.format("%." + m_numberClass.getDecimals() + "f", topRangePrice);
                int topRangeStringWidth = m_fm.stringWidth(topRangeString);

                int topRangeStringX = (chartWidth + (m_scaleColWidth / 2)) - (topRangeStringWidth / 2);
                int topRangeStringY = (topRangeY - (m_labelHeight / 2)) + m_labelAscent;

                Drawing.fillArea(img, 0xffffffff, x1, topRangeY - (m_labelHeight / 2) - 3, x2, topRangeY + (m_labelHeight / 2) + 3);
                g2d.setColor(blackColor);
                g2d.drawString(topRangeString, topRangeStringX, topRangeStringY);

                String botRangeString = String.format("%." + m_numberClass.getDecimals() + "f", botRangePrice);
                int botRangeStringWidth = m_fm.stringWidth(botRangeString);

                int botRangeStringX = (chartWidth + (m_scaleColWidth / 2)) - (botRangeStringWidth / 2);
                int botRangeStringY = (botRangeY - (m_labelHeight / 2)) + m_labelAscent;

                Drawing.fillArea(img, 0xffffffff, x1, botRangeY - (m_labelHeight / 2) - 3, x2, botRangeY + (m_labelHeight / 2) + 3);
                g2d.drawString(botRangeString, botRangeStringX, botRangeStringY);

            }

            g2d.dispose();

        } else {

            g2d.setFont(m_headingFont);
            FontMetrics fm = g2d.getFontMetrics();
            String text = m_msg;

            int stringWidth = fm.stringWidth(text);
            int fmAscent = fm.getAscent();
            int x = (width / 2) - (stringWidth/2);
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
        m_imageBuffer.set( SwingFXUtils.toFXImage(img,null));
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
