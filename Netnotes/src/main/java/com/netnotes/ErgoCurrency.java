package com.netnotes;

import com.netnotes.Network.NetworkID;

import javafx.scene.image.Image;

public class ErgoCurrency extends PriceCurrency {

    public final static String NAME = "Ergo";
    public final static String SYMBOL = "ERG";
    public final static String NETWORK_ID = NetworkID.ERGO_NETWORK;
    public final static String IMAGE_STRING = "/assets/unitErgo.png";
    public final static int FRACTIONAL_PRECISION = 9;

    public ErgoCurrency() {
        super(NAME, SYMBOL, FRACTIONAL_PRECISION, NETWORK_ID, IMAGE_STRING);

    }

    public static Image getImage() {
        return new Image(IMAGE_STRING);
    }
}
