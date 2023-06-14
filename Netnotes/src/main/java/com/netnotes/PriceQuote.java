package com.netnotes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.google.gson.JsonObject;
import com.utils.Utils;

public class PriceQuote {

    private String m_transactionCurrency;
    private String m_quoteCurrency;
    private long m_epochMillis = 0;
    private double m_amount;

    public PriceQuote(double amount, String transactionCurrency, String quoteCurrency) {

        m_epochMillis = Utils.getNowEpochMillis();

        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;
        m_amount = amount;
    }

    public double getAmount() {
        return m_amount;
    }

    public String getTransactionCurrency() {
        return m_transactionCurrency;
    }

    public String getQuoteCurrency() {
        return m_quoteCurrency;
    }

    public long howOldMillis() {
        return (Utils.getNowEpochMillis() - m_epochMillis);
    }

    public long getTimeStamp() {
        return m_epochMillis;
    }

    public JsonObject getJsonObject() {
        JsonObject priceQuoteObject = new JsonObject();
        priceQuoteObject.addProperty("transactionCurrency", m_transactionCurrency);
        priceQuoteObject.addProperty("quoteCurrency", m_quoteCurrency);
        priceQuoteObject.addProperty("amount", m_amount);
        priceQuoteObject.addProperty("timeStamp", m_epochMillis);

        return priceQuoteObject;
    }

}
