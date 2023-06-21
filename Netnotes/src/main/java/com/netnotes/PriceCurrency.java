package com.netnotes;

import java.util.Map;

import org.ergoplatform.appkit.ErgoToken;

import com.netnotes.Network.NetworkID;

import javafx.scene.image.Image;

public class PriceCurrency {

    private String m_tokenId;
    private String m_symbol;
    private String m_name;
    private String m_networkId;
    private String m_unitImageString;
    private int m_fractionalPrecision = 2;

    public PriceCurrency(String token_id, String name, String symbol, int fractionalPrecision, String networkId, String unitImageString) {
        m_tokenId = token_id;
        m_name = name;
        m_symbol = symbol;
        m_networkId = networkId;
        m_unitImageString = unitImageString;
        m_fractionalPrecision = fractionalPrecision;

    }

    public String getTokenId() {
        return m_tokenId;
    }

    public Image getUnitImage() {
        if (m_symbol != null && m_name != null && m_networkId != null && m_unitImageString != null) {
            return new Image(m_unitImageString);
        } else {
            return getUnknownUnitImage();
        }
    }

    public static Image getUnknownUnitImage() {
        return new Image("/assets/unknown-unit.png");
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

    public String getNetworkName() {
        if (m_networkId == null) {
            return "";
        }

        return Network.getNetworkName(m_networkId);
    }

    public int getFractionalPrecision() {
        return m_fractionalPrecision;
    }

    @Override
    public String toString() {
        return m_symbol;
    }
}
