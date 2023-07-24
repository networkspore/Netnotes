package com.netnotes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MarketUpdates {

    public final static String TICKER = "ticker";

    private String m_networkId = KucoinExchange.NETWORK_ID;
    private String m_updateType = TICKER;
    private QuoteListener m_quoteListener;
    private NoteInterface m_noteInterface;

    public MarketUpdates(NoteInterface noteInterface, JsonObject json) {
        noteInterface = m_noteInterface;
        if (json != null) {

            JsonElement idElement = json.get("id");
            JsonElement typeElement = json.get("type");

            m_networkId = idElement == null ? null : idElement.getAsString();
            m_updateType = typeElement == null ? null : typeElement.getAsString();
        }

    }

    public MarketUpdates(NoteInterface noteInterface, String networkId, String updateType) {
        m_networkId = networkId;
        m_updateType = updateType;
        m_noteInterface = noteInterface;
    }

    public String getNetworkId() {
        return m_networkId;
    }

    public void setQuoteListener(QuoteListener quoteListener) {
        m_quoteListener = quoteListener;
    }

    public QuoteListener getQuoteListener() {
        return m_quoteListener;
    }

    public void setNetworkid(String networkId) {
        m_networkId = networkId;
    }

    public String getUpdateType() {
        return m_updateType;
    }

    public void setUpdateType(String updateType) {
        m_updateType = updateType;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_networkId);
        json.addProperty("type", m_updateType);
        return json;
    }

    public void start() {
        if (m_networkId != null) {

            switch (m_updateType) {
                case TICKER:

                    break;
            }
        }
    }
}
