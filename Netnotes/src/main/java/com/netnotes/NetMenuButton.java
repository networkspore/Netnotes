package com.netnotes;

import javafx.scene.control.MenuButton;

public class NetMenuButton extends MenuButton {

    private String m_networkId;

    public NetMenuButton(String networkId, String text, Object userData) {
        super(text);
        m_networkId = networkId;
        setUserData(userData);
    }

    public String getNetworkId() {
        return m_networkId;
    }
}
