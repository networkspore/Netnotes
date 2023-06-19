package com.netnotes;

public class ErgoAmount extends PriceAmount {
    public ErgoAmount(double amount){
        super(amount, new ErgoCurrency());
    }

    
}
