package com.netnotes;


public class USCurrency extends PriceCurrency {

    public final static String TOKEN_ID = "USD";
    public final static String NAME = "US Dollar";
    public final static String SYMBOL = "USD";
    public final static String IMAGE_STRING = "/assets/unitUSD.png";
    public final static int FRACTIONAL_PRECISION = 2;
    public final static String NETWORK_ID = null;
    public final static String FONT_SYMBOL = "$";

    public USCurrency() {
        super(TOKEN_ID, NAME, SYMBOL, FRACTIONAL_PRECISION, NETWORK_ID, IMAGE_STRING, FONT_SYMBOL);
    }


}
