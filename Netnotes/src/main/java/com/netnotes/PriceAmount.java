package com.netnotes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;


public class PriceAmount {

    private BigDecimal m_amount;
    private PriceCurrency m_currency;
    private LocalDateTime m_created;
    private boolean m_valid = true;

    public PriceAmount(long amount, PriceCurrency currency) {
        m_currency = currency;
        setLongAmount(amount);
        m_created = LocalDateTime.now();
    }

    public PriceAmount(BigDecimal amount, PriceCurrency currency){
        m_currency = currency;
        setBigDecimalAmount(amount);
        m_created = LocalDateTime.now();

    }

    public PriceAmount(double amount, PriceCurrency currency) {
        
        m_currency = currency;
        setDoubleAmount(amount);
        m_created = LocalDateTime.now();
    }

    public PriceAmount(long amount, PriceCurrency currency, boolean amountValid){
        
        m_currency = currency;
        setLongAmount(amount);
        m_valid = amountValid;
        m_created = LocalDateTime.now();
    }

    public void setBigDecimalAmount(BigDecimal amount) {
        m_amount = amount;
    }

    public String getTokenId(){
        return m_currency.getTokenId();
    }
    
    public BigDecimal getBigDecimalAmount(){
        return m_amount;
    }

    public double getDoubleAmount() {
        return m_amount.doubleValue();
    }


    public boolean getAmountValid(){
        return m_valid;
    }

    public void setAmoutValid(boolean valid){
        m_valid = valid;
    }

    public void setLongAmount(long amount) {
        int decimals = m_currency.getFractionalPrecision();
        BigDecimal bigAmount = BigDecimal.valueOf(amount);

        if(decimals != 0){
            BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);
            m_amount = bigAmount.divide(pow);
        }else{
            m_amount = bigAmount;
        }
    }

    public long getLongAmount() {
        int decimals = m_currency.getFractionalPrecision();
        BigDecimal pow = BigDecimal.valueOf(10).pow(decimals);

        return m_amount.multiply(pow).longValue();
    }

    public void setDoubleAmount(double amount) {
        m_amount = BigDecimal.valueOf(amount);
    }

    public PriceCurrency getCurrency() {
        return m_currency;
    }

    public LocalDateTime getCreatedTime() {
        return m_created;
    }

    public String getAmountString(){
        int precision = getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);
        
        String formatedDecimals = df.format(m_amount);
        return formatedDecimals;
    }

    @Override
    public String toString() {
        String formatedDecimals = getAmountString();
        
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
