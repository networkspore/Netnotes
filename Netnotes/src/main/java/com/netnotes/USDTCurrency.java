package com.netnotes;


public class USDTCurrency extends PriceCurrency {

    public final static String TOKEN_ID = "USDT";
    public final static String NAME = "Tether";
    public final static String SYMBOL = "USDT";
    public final static String IMAGE_STRING = "/assets/unitTether.png";
    public final static int FRACTIONAL_PRECISION = 2;
    public final static String NETWORK_ID = null;
    public final static String FONT_SYMBOL = null;

    public USDTCurrency() {
        super(TOKEN_ID, NAME, SYMBOL, FRACTIONAL_PRECISION, NETWORK_ID, IMAGE_STRING, FONT_SYMBOL);
    }


}
