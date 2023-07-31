package com.netnotes;

import com.google.gson.JsonObject;
import com.utils.Utils;

public class PriceQuote {

    private String m_amountString;

    private String m_transactionCurrency;
    private String m_quoteCurrency;
    private long m_timestamp = 0;
    private double m_amount;

    public PriceQuote(double amount, String transactionCurrency, String quoteCurrency) {

        m_timestamp = System.currentTimeMillis();
        m_amountString = amount + "";
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;
        m_amount = amount;
    }

    public PriceQuote(String amountString, String transactionCurrency, String quoteCurrency) {
        m_amount = Double.parseDouble(amountString);
        m_timestamp = System.currentTimeMillis();
        m_amountString = amountString;
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;

    }

    public PriceQuote(String amountString, String transactionCurrency, String quoteCurrency, long timestamp) {
        m_amount = Double.parseDouble(amountString);
        m_timestamp = timestamp;
        m_amountString = amountString;
        m_transactionCurrency = transactionCurrency;
        m_quoteCurrency = quoteCurrency;

    }

    public double getAmount() {
        return m_amount;
    }

    public String getAmountString() {
        return m_amountString;
    }

    public String getTransactionCurrency() {
        return m_transactionCurrency;
    }

    public String getQuoteCurrency() {
        return m_quoteCurrency;
    }

    public long howOldMillis() {
        return (Utils.getNowEpochMillis() - m_timestamp);
    }

    public long getTimeStamp() {
        return m_timestamp;
    }

    public JsonObject getJsonObject() {
        JsonObject priceQuoteObject = new JsonObject();
        priceQuoteObject.addProperty("transactionCurrency", m_transactionCurrency);
        priceQuoteObject.addProperty("quoteCurrency", m_quoteCurrency);
        priceQuoteObject.addProperty("amount", m_amount);
        priceQuoteObject.addProperty("timeStamp", m_timestamp);

        return priceQuoteObject;
    }

    @Override
    public String toString() {
        return m_amount + " " + m_quoteCurrency + "-" + m_transactionCurrency;
    }
}
