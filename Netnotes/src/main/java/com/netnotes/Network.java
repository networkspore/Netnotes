package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.satergo.Wallet;

public class Network {

    private final String m_uuID;
    private String m_wallet = null;
    private String m_type = "Ergo";
    private String m_name = "";

    public Network() {
        m_uuID = UUID.randomUUID().toString();
        m_name = m_uuID;
    }

    public Network(JsonObject networkJson) throws Exception {
        m_name = networkJson.get("name").getAsString();
        m_uuID = networkJson.get("UUID").getAsString();
        m_wallet = networkJson.get("wallet").getAsString();
        m_type = networkJson.get("type").getAsString();
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public void setWallet(Wallet wallet) throws Exception {
        byte[] bytes = wallet.serializeEncrypted();
        String hexString = Hex.encodeHexString(bytes);
        m_wallet = hexString;
    }

    public Wallet getWallet(String password) throws Exception {
        byte[] bytes = Hex.decodeHex(m_wallet);

        return Wallet.deserializeEncrypted(bytes, password.toCharArray());
    }

    private JsonObject getJsonObject() throws Exception {
        if (m_wallet != null) {
            JsonObject networkObj = new JsonObject();
            networkObj.addProperty("type", m_type);
            networkObj.addProperty("UUID", m_uuID);
            networkObj.addProperty("wallet", m_wallet);
            networkObj.addProperty("name", m_name);

            return networkObj;
        } else {
            return null;
        }
    }

    public void save(File networksFile) throws Exception {
        boolean isFile = !networksFile.isFile();

        String fileHexString = isFile ? Files.readString(networksFile.toPath()) : "";

        String jsonArrayString = "";
        JsonObject jsonObj = getJsonObject();

        if (jsonObj != null) {
            if (fileHexString.equals("")) {
                JsonArray jsonArray = new JsonArray();

                jsonArray.add(jsonObj);
                jsonArrayString = jsonArray.toString();

            } else {

                byte[] bytes = Hex.decodeHex(fileHexString);

                jsonArrayString = new String(bytes, StandardCharsets.UTF_8);

                JsonArray jsonArray = new JsonParser().parse(jsonArrayString).getAsJsonArray();

                int elementIndex = -1;
                int i = 0;

                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();

                    String uuid = jsonObject.get("UUID").getAsString();
                    if (uuid.equals(m_uuID)) {
                        elementIndex = i;
                        break;
                    }
                    i++;
                }

                if (elementIndex == -1) {

                    jsonArray.add(jsonObj);

                } else {
                    jsonArray.set(elementIndex, jsonObj);
                }

                jsonArrayString = jsonArray.toString();

            }
            byte[] bytes = jsonArrayString.getBytes(StandardCharsets.UTF_8);
            fileHexString = Hex.encodeHexString(bytes);

            Files.writeString(networksFile.toPath(), fileHexString);
        } else {
            throw new NullPointerException("Network has not been initialized.");
        }
    }

}
