package com.netnotes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PriceData {

    private LocalDateTime m_timestamp_UTC;
    private double m_open;
    private double m_close;
    private double m_high;
    private double m_low;
    private double m_volume;
    private double m_turnover;

    public PriceData(LocalDateTime timestamp_UTC, double open, double close, double high, double low, double volume, double turnover) {
        m_timestamp_UTC = timestamp_UTC;
        m_open = open;
        m_close = close;
        m_high = high;
        m_low = low;
        m_volume = volume;
        m_turnover = turnover;
    }

    public PriceData(JsonArray jsonArray) {

        long timestamp = jsonArray.get(0).getAsLong();

        Instant timeInstant = Instant.ofEpochMilli(timestamp / 1_000).plusNanos(timestamp % 1_000);

        m_timestamp_UTC = LocalDateTime.ofInstant(timeInstant, ZoneId.of("UTC"));

        m_open = jsonArray.get(1).getAsDouble();
        m_close = jsonArray.get(2).getAsDouble();
        m_high = jsonArray.get(3).getAsDouble();
        m_low = jsonArray.get(4).getAsDouble();
        m_volume = jsonArray.get(5).getAsDouble();
        m_turnover = jsonArray.get(6).getAsDouble();

    }

    public LocalDateTime getTimestamp_UTC() {
        return m_timestamp_UTC;
    }

    public void setTimestamp_UTC(LocalDateTime timeStamp) {
        m_timestamp_UTC = timeStamp;
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
