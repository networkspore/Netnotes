package com.netnotes;

import com.satergo.Wallet;

public class WalletContainer {

    private Wallet m_wallet = null;

    public WalletContainer() {
    }

    public Wallet getWallet() {
        return m_wallet;
    }

    public void setWallet(Wallet wallet) {
        m_wallet = wallet;
    }
}
