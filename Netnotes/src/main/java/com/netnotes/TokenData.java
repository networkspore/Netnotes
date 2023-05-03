package com.netnotes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TokenData {

    private boolean m_valid = false;
    private String m_tokenID = "";
    private long m_amount = 0;
    private int m_decimals = 0;
    private String m_name = "";

    public TokenData(JsonObject jsonObj) {
        JsonElement tokenElement = jsonObj.get("tokenID");
        JsonElement amountElement = jsonObj.get("amount");
        JsonElement decimalsElement = jsonObj.get("decimals");
        JsonElement nameElement = jsonObj.get("name");

        if (tokenElement != null && amountElement != null && decimalsElement != null && nameElement != null) {
            m_tokenID = tokenElement.getAsString();
            m_amount = amountElement.getAsLong();
            m_decimals = decimalsElement.getAsInt();
            m_name = nameElement.getAsString();
            m_valid = true;
        } else {
            m_valid = false;
        }
    }

    public boolean compare(TokenData tokenData) {
        if (m_tokenID.equals(tokenData.getTokenID()) && m_amount == tokenData.getAmount() && m_decimals == tokenData.getDecimals() && m_name.equals(tokenData.getName())) {
            return true;
        }
        return false;
    }

    public String getTokenID() {
        return m_tokenID;
    }

    public String getName() {
        return m_name;
    }

    public long getAmount() {
        return m_amount;
    }

    public int getDecimals() {
        return m_decimals;
    }

    public boolean getValid() {
        return m_valid;
    }

    public JsonObject getJsonObject() {
        return new JsonObject();
    }
}
