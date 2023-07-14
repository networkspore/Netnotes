package com.netnotes;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

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

    public HashData(String hashId, String name, String hashString) throws DecoderException {

        m_id = hashId;
        m_name = name;
        setHash(hashString);

    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getHashString() {
        return Hex.encodeHexString(m_hashBytes);
    }

    public byte[] getHashBytes() {
        return m_hashBytes;
    }

    public void setHash(String hashString) throws DecoderException {
        m_hashBytes = Hex.decodeHex(hashString);
    }

    public void setHash(byte[] hashBytes) {
        m_hashBytes = hashBytes;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        if (m_hashBytes != null) {
            json.addProperty("hash", getHashString());
        }
        return json;
    }
}
