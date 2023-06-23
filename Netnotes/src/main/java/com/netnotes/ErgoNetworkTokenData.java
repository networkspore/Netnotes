package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.Network.NetworkID;

public class ErgoNetworkTokenData {

    private String m_id = null;
    private String m_boxId = null;
    private long m_emissionAmount = 0;
    private String m_name = null;
    private String m_description = null;
    private String m_type = null;
    private int m_decimals = 0;

    public ErgoNetworkTokenData(JsonObject sourceJson, String explorerId, NetworkType networkType) {

        //if (NetworkID.ERGO_EXPLORER.equals(explorerId)) {
        JsonElement idElement = sourceJson.get("id");
        JsonElement boxIdElement = sourceJson.get("boxId");
        JsonElement emissionAmountElement = sourceJson.get("emissionAmount");
        JsonElement nameElement = sourceJson.get("name");
        JsonElement descriptionElement = sourceJson.get("description");
        JsonElement typeElement = sourceJson.get("type");
        JsonElement decimalsElement = sourceJson.get("decimals");

        m_id = idElement == null ? null : idElement.getAsString();
        m_boxId = boxIdElement == null ? null : boxIdElement.getAsString();
        m_emissionAmount = emissionAmountElement == null ? 0 : emissionAmountElement.getAsLong();
        m_name = nameElement == null ? null : nameElement.getAsString();
        m_description = descriptionElement == null ? null : descriptionElement.getAsString();
        m_type = typeElement == null ? null : typeElement.getAsString();
        m_decimals = decimalsElement == null ? 0 : decimalsElement.getAsInt();
        //  }
    }

    public String getTokenId() {
        return m_id;
    }

    public String getBoxId() {
        return m_boxId;
    }

    public long getEmissionAmount() {
        return m_emissionAmount;
    }

    public String getName() {
        return m_name;
    }

    public String getDescription() {
        return m_description;
    }

    public String getType() {
        return m_type;
    }

    public int getDecimals() {
        return m_decimals;
    }

    @Override
    public String toString() {
        return m_name != null ? (m_name + " [ " + getType() + " ]") : "INVALID";
    }
}
