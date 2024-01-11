package com.netnotes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.google.gson.JsonObject;
import com.utils.Utils;
import com.netnotes.SpectrumChartView.SpectrumPrice;

public class SpectrumPriceData {

    private long m_timestamp = 0;
    private BigDecimal m_open = BigDecimal.ZERO;
    private BigDecimal m_close = BigDecimal.ZERO;
    private BigDecimal m_high = BigDecimal.ZERO;
    private BigDecimal m_low = BigDecimal.ZERO;



    public SpectrumPriceData(SpectrumPrice spectrumPrice, long epochEnd){

        BigDecimal price = spectrumPrice.getPrice();

        m_timestamp = epochEnd;
        m_open = price;
        m_low = price;
        m_high = price;
        m_close = price;


    }

    public SpectrumPriceData(long timestamp, BigDecimal price, long epochEnd) {
        m_timestamp = timestamp;
        m_open = price;
        m_low = price;
        m_high = price;
        m_close = price;
 
    }
    public SpectrumPriceData(long timestamp, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low) {
        m_timestamp = timestamp;
        m_open = open;
        m_close = close;
        m_high = high;
        m_low = low;

    }



    public String getCloseString() {
        return m_close.toString();
    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public void addPrice(long timestamp, BigDecimal price){
  
    
        m_close = price;
       
        m_low = m_low.min(price);
        m_high = m_high.max(price);
        
    }

    public LocalDateTime getLocalDateTime() {
       
        return Utils.milliToLocalTime(m_timestamp);
        
    }

    public void setTimestamp(long timeStamp) {
        m_timestamp = timeStamp;
    }

    public BigDecimal getOpen() {
        return m_open;
    }

    public void setOpen(BigDecimal open) {
        m_open = open;
    }

    public BigDecimal getClose() {
        return m_close;
    }

    public void setClose(BigDecimal close) {
        m_close = close;
    }

    public BigDecimal getHigh() {
        return m_high;
    }

    public void setHigh(BigDecimal high) {
        m_high = high;
    }

    public BigDecimal getLow() {
        return m_low;
    }

    public void setLow(BigDecimal low) {
        m_low = low;
    }


    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("open", getOpen());
        jsonObject.addProperty("close",getClose());
        jsonObject.addProperty("high", getHigh());
        jsonObject.addProperty("low", getLow());
        jsonObject.addProperty("timeStamp", getTimestamp());
        return jsonObject;
    }
}
