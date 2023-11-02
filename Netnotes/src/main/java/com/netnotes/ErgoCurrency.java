package com.netnotes;

import javafx.scene.image.Image;

public class ErgoCurrency extends PriceCurrency {

    public final static String TOKEN_ID = "ERG";
    public final static String NAME = "Ergo";
    public final static String SYMBOL = "Σ";
    public final static String IMAGE_STRING = "/assets/unitErgo.png";
    public final static int FRACTIONAL_PRECISION = 9;
    public final static String NETWORK_ID = ErgoNetwork.NETWORK_ID;

    public ErgoCurrency() {
        super(TOKEN_ID, NAME, SYMBOL, FRACTIONAL_PRECISION, NETWORK_ID, IMAGE_STRING);
    }

    public static Image getImage() {
        return new Image(IMAGE_STRING);
    }
}
