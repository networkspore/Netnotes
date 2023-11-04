package com.netnotes;

import java.util.Map;

import org.ergoplatform.appkit.ErgoToken;

import javafx.scene.image.Image;

public class PriceCurrency {

    private boolean m_priceValid = true;
    private double m_price;
    private String m_tokenId;
    private String m_symbol;
    private String m_name;
    private String m_networkId;
    private String m_unitImageString;

    private int m_fractionalPrecision = 2;
    private String m_fontSymbol = "";

    public PriceCurrency(String token_id, String name, String symbol, int fractionalPrecision, String networkId, String unitImageString, String fontSymbol) {
        this(token_id, name, symbol, 0, true, fractionalPrecision, networkId, unitImageString, fontSymbol);
    }

    public PriceCurrency(String token_id, String name, String symbol, double price, boolean priceValid, int fractionalPrecision, String networkId, String unitImageString, String fontSymbol) {
        m_priceValid = priceValid;
        m_tokenId = token_id;
        m_price = price;
        m_name = name;
        m_symbol = symbol;
        m_networkId = networkId;
        m_unitImageString = unitImageString;
        m_fractionalPrecision = fractionalPrecision;
        m_fontSymbol = fontSymbol;

    }

    public void setPriceValid(boolean priceValid) {
        m_priceValid = priceValid;
    }

    public String getFontSymbol(){
        return m_fontSymbol;
    }

    public boolean getPriceValid() {
        return m_priceValid;
    }

    public double getPrice() {
        return m_price;
    }

    public void setPrice(double price) {
        m_price = price;
    }

    public String getTokenId() {
        return m_tokenId;
    }

    public Image getUnitImage() {
        if (m_symbol != null && m_name != null && m_unitImageString != null) {
            return new Image(m_unitImageString);
        } else {
            return getUnknownUnitImage();
        }
    }

    public static Image getUnknownUnitImage() {
        return new Image("/assets/unknown-unit.png");
    }

    public String getImageString(){
        return m_unitImageString;
    }

    public String getName() {
        return m_name;
    }

    public String getSymbol() {
        return m_symbol;
    }

    public String networkId() {
        return m_networkId;
    }

    public int getFractionalPrecision() {
        return m_fractionalPrecision;
    }

    @Override
    public String toString() {
        return m_symbol;
    }
}
