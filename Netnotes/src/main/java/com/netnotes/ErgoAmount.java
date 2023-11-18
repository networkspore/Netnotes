package com.netnotes;

public class ErgoAmount extends PriceAmount {

    public ErgoAmount(double amount) {
        super(amount, new ErgoCurrency());
    }

    public ErgoAmount(long nanoErg) {
        super(nanoErg, new ErgoCurrency());
    }

    public ErgoAmount(long nanoErg, boolean amountValid){
        super(nanoErg, new ErgoCurrency(), amountValid);
        
    }
}
