package com.netnotes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PriceData {

    private long m_timestamp = 0;
    private double m_open = 0;
    private double m_close = 0;
    private double m_high = 0;
    private double m_low = 0;
    private double m_volume = 0;
    private double m_turnover = 0;

    public PriceData(long timestamp, double open, double close, double high, double low, double volume, double turnover) {
        m_timestamp = timestamp;
        m_open = open;
        m_close = close;
        m_high = high;
        m_low = low;
        m_volume = volume;
        m_turnover = turnover;
    }

    public PriceData(JsonArray jsonArray) {
        if (jsonArray != null) {
            JsonElement timestampElement = jsonArray.size() > 0 ? jsonArray.get(0) : null;
            JsonElement openElement = jsonArray.size() > 1 ? jsonArray.get(1) : null;
            JsonElement closeElement = jsonArray.size() > 2 ? jsonArray.get(2) : null;
            JsonElement highElement = jsonArray.size() > 3 ? jsonArray.get(3) : null;
            JsonElement lowElement = jsonArray.size() > 4 ? jsonArray.get(4) : null;
            JsonElement volumeElement = jsonArray.size() > 5 ? jsonArray.get(5) : null;
            JsonElement turnOverElement = jsonArray.size() > 6 ? jsonArray.get(6) : null;

            m_timestamp = timestampElement != null && timestampElement.isJsonPrimitive() ? timestampElement.getAsLong() : 0;
            m_open = openElement != null && openElement.isJsonPrimitive() ? openElement.getAsDouble() : 0;
            m_close = closeElement != null && closeElement.isJsonPrimitive() ? closeElement.getAsDouble() : 0;
            m_high = highElement != null && highElement.isJsonPrimitive() ? highElement.getAsDouble() : 0;
            m_low = lowElement != null && lowElement.isJsonPrimitive() ? lowElement.getAsDouble() : 0;
            m_volume = volumeElement != null && volumeElement.isJsonPrimitive() ? volumeElement.getAsDouble() : 0;
            m_turnover = turnOverElement != null && turnOverElement.isJsonPrimitive() ? turnOverElement.getAsDouble() : 0;
        }

    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public static LocalDateTime nanosToTimeUTC(long timestamp) {
        Instant timeInstant = Instant.ofEpochMilli(timestamp / 1_000).plusNanos(timestamp % 1_000);

        return LocalDateTime.ofInstant(timeInstant, ZoneId.of("UTC"));
    }

    public LocalDateTime getTimestamp_UTC() {
        return nanosToTimeUTC(m_timestamp);
    }

    public void setTimestamp(long timeStamp) {
        m_timestamp = timeStamp;
    }

    public double getOpen() {
        return m_open;
    }

    public void setOpen(double open) {
        m_open = open;
    }

    public double getClose() {
        return m_close;
    }

    public void setClose(double close) {
        m_close = close;
    }

    public double getHigh() {
        return m_high;
    }

    public void setHigh(double high) {
        m_high = high;
    }

    public double getLow() {
        return m_low;
    }

    public void setLow(double low) {
        m_low = low;
    }

    public double getVolume() {
        return m_volume;
    }

    public void setVolume(double volume) {
        m_volume = volume;
    }

    public double getTurnover() {
        return m_turnover;
    }

    public void setTurnover(double turnover) {
        m_turnover = turnover;
    }
}
