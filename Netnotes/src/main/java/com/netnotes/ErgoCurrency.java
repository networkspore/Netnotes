package com.netnotes;



public class ErgoCurrency extends PriceCurrency {

    public final static String TOKEN_ID = "ERG";
    public final static String NAME = "Ergo";
    public final static String SYMBOL = "ERG";
    public final static String IMAGE_STRING = "/assets/unitErgo.png";
    public final static int FRACTIONAL_PRECISION = 9;
    public final static String NETWORK_ID = ErgoNetwork.NETWORK_ID;
    public final static String FONT_SYMBOL  = "Σ";
    public ErgoCurrency() { 
        super(TOKEN_ID, NAME, SYMBOL, FRACTIONAL_PRECISION, NETWORK_ID, IMAGE_STRING, FONT_SYMBOL);
    }

}
