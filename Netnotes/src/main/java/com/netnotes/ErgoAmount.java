package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

public class ErgoAmount extends PriceAmount {

    public ErgoAmount(double amount, NetworkType networkType) {
        super(amount, new ErgoCurrency(networkType));
    }

    public ErgoAmount(long nanoErg, NetworkType networkType) {
        super(nanoErg, new ErgoCurrency(networkType));
    }


}
