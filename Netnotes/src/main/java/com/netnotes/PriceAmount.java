package com.netnotes;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

import com.utils.Utils;

import javafx.beans.property.SimpleStringProperty;

public class PriceAmount {

    private long m_amount;
    private PriceCurrency m_currency;
    private LocalDateTime m_created;

    public PriceAmount(long amount, PriceCurrency currency) {
        m_amount = amount;
        m_currency = currency;

        m_created = LocalDateTime.now();
    }

    public PriceAmount(double amount, PriceCurrency currency) {
        
        m_currency = currency;
        setDoubleAmount(amount);
    }


    public void setLongAmount(long amount) {
        m_amount = amount;
    }

    public long getLongAmount() {
        return m_amount;
    }

    public void setDoubleAmount(double amount) {
        double precision = Math.pow(10, m_currency.getFractionalPrecision());
        m_amount = (long) (precision * amount);
    }

    public double getDoubleAmount() {
        double precision = Math.pow(10, m_currency.getFractionalPrecision());
        
        return (double) m_amount * ((long) 1 / precision);
    }

    public PriceCurrency getCurrency() {
        return m_currency;
    }

    public LocalDateTime getCreatedTime() {
        return m_created;
    }

    @Override
    public String toString() {
        
        int precision = getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

        String formatedDecimals = df.format(getDoubleAmount());
        String priceTotal = getCurrency().getPriceValid() ? formatedDecimals : "-";

        switch (getCurrency().getSymbol()) {
      
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            default:
                priceTotal = priceTotal + " " + getCurrency().getSymbol();
        }

        return priceTotal;
    }

}
