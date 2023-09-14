package com.netnotes;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rfksystems.blake2b.Blake2b;

public class HashData {

    public static String DEFAULT_HASH = Blake2b.BLAKE2_B_256;

    private String m_id = DEFAULT_HASH;
    private String m_name = "Blake2b 256";
    private byte[] m_hashBytes = null;

    public HashData(byte[] bytes) {
        m_hashBytes = bytes;
    }

    public HashData(JsonObject json) throws Exception {
        openJson(json);
    }

    public HashData(String hashId, String name, String hashString) {

        m_id = hashId;
        m_name = name;
        setHash(hashString);

    }

    public void openJson(JsonObject json) throws Exception {
        /*json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        if (m_hashBytes != null) {
            json.addProperty("hash", getHashString());
        }*/
        JsonElement idElement = json.get("id");
        JsonElement nameElement = json.get("name");
        JsonElement hashStringElement = json.get("hash");

        if (idElement != null && idElement.isJsonPrimitive()) {
            m_id = idElement.getAsString();
            if (nameElement != null && nameElement.isJsonPrimitive()) {
                m_name = nameElement.getAsString();
                if (hashStringElement != null && hashStringElement.isJsonPrimitive()) {
                    setHashHex(hashStringElement.getAsString());
                    return;
                }
            }
        }

        throw new Exception("Json data missing");
    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getHashString() {
        return new String(m_hashBytes);
    }

    public String getHashStringHex() {
        return Hex.encodeHexString(m_hashBytes);
    }

    public byte[] getHashBytes() {
        return m_hashBytes;
    }

    public void setHash(String hashString) {
        m_hashBytes = hashString.getBytes();
    }

    public void setHashHex(String hashHexString) {
        try {
            m_hashBytes = Hex.decodeHex(hashHexString);
        } catch (DecoderException e) {
            m_hashBytes = null;
        }
    }

    public void setHash(byte[] hashBytes) {
        m_hashBytes = hashBytes;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        if (m_hashBytes != null) {
            json.addProperty("hash", getHashStringHex());
        }
        return json;
    }
}
