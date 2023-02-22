package com.netnotes;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.satergo.extra.AESEncryption;
import com.utils.Utils;

public class AppData {

    //AppKey
    private String keyHash;
    private Network[] networks = null;

    public AppData(boolean exists, Path filePath, String password) throws Exception {

        if (exists) {
            open(filePath, password);
        } else {
            keyHash = Utils.getBcryptHashString(password);
            save(filePath, password);
        }

    }

    public boolean verifyKey(String key) {

        return Utils.verifyBCryptPassword(key, keyHash);

    }

    private String getJson() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("keyHash", keyHash);

        return jsonObj.toString();
    }

    public void save(Path filePath, String password) throws Exception {
        String json = getJson();

        byte[] encryptedBytes = AESEncryption.encryptData(password.toCharArray(), json.getBytes());

        Files.write(filePath, encryptedBytes);
    }

    private void open(Path filePath, String password) throws Exception {

        byte[] bytes = Files.readAllBytes(filePath);
        char[] passChars = password.toCharArray();

        ByteBuffer encryptedData = ByteBuffer.wrap(bytes);

        byte[] decryptedData = AESEncryption.decryptData(passChars, encryptedData);

        String json = new String(decryptedData);

        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

        keyHash = jsonObject.get("keyHash").getAsString();

    }

    public Network[] getNetworks() {
        return networks;
    }

}
