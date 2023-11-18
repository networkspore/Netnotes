package com.netnotes;

import java.text.DecimalFormat;
import java.time.LocalDateTime;


public class PriceAmount {

    private long m_amount;
    private PriceCurrency m_currency;
    private LocalDateTime m_created;
    private boolean m_valid = true;

    public PriceAmount(long amount, PriceCurrency currency) {
        m_amount = amount;
        m_currency = currency;

        m_created = LocalDateTime.now();
    }



    public PriceAmount(double amount, PriceCurrency currency) {
        
        m_currency = currency;
        setDoubleAmount(amount);
        m_created = LocalDateTime.now();
    }

    public PriceAmount(long amount, PriceCurrency currency, boolean amountValid){
        m_amount = amount;
        m_currency = currency;
        m_valid = amountValid;
        m_created = LocalDateTime.now();
    }



    public boolean getAmountValid(){
        return m_valid;
    }

    public void setAmoutValid(boolean valid){
        m_valid = valid;
    }

    public void setLongAmount(long amount) {
        m_amount = amount;
    }

    public long getLongAmount() {
        return m_amount;
    }

    public void setDoubleAmount(double amount) {
        if(m_currency.getFractionalPrecision() == 0){
            m_amount = (long) amount;
        }else{
            double precision = Math.pow(10, m_currency.getFractionalPrecision());
            m_amount = (long) (precision * amount);
        }
    }

    public double getDoubleAmount() {
        double precision = Math.pow(10, m_currency.getFractionalPrecision());
        
        return (double) m_amount * (precision != 0 ? ((long) 1 / precision) : 1.0 );
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
        String amount = m_valid ? formatedDecimals : "-";

        switch (getCurrency().getSymbol()) {
      
            case "USD":
                amount = "$" + amount;
                break;
            case "EUR":
                amount = "€‎" + amount;
                break;
            default:
                amount = amount + " " + getCurrency().getSymbol();
        }

        return amount;
    }


}
