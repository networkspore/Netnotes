package com.netnotes;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

import com.utils.Utils;

public class PriceAmount {

    private double m_amount;
    private PriceCurrency m_currency;
    private LocalDateTime m_created;

    public PriceAmount(double amount, PriceCurrency currency) {
        m_amount = amount;
        m_currency = currency;

        m_created = LocalDateTime.now();
    }

    public double getAmount() {
        return m_amount;
    }

    public PriceCurrency getCurrency() {
        return m_currency;
    }

    public LocalDateTime getCreatedTime() {
        return m_created;
    }

    public String getNetworkName() {
        return m_currency == null ? "" : m_currency.getNetworkName();
    }

    @Override
    public String toString() {
        int precision = m_currency.getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

        return df.format(getAmount()) + " " + m_currency;
    }

}
