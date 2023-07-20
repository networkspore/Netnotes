package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AppData {

    private File m_dataFile;
    private String m_appKey;
    private boolean m_updates;

    public AppData(File dataFile) throws IOException {

        String jsonString = Files.readString(dataFile.toPath());
        JsonObject dataObject = new JsonParser().parse(jsonString).getAsJsonObject();

        m_dataFile = dataFile;
        m_appKey = dataObject.get("appKey").getAsString();
        m_updates = dataObject.get("updates").getAsBoolean();
    }

    public String getAppKey() {
        return m_appKey;
    }

    public byte[] getAppKeyBytes() {
        return m_appKey.getBytes();
    }

    public void setAppKey(String keyHash) throws IOException {
        m_appKey = keyHash;
        save();
    }

    public boolean getUpdates() {
        return m_updates;
    }

    public void setUpdates(boolean updates) throws IOException {
        m_updates = updates;
        save();
    }

    public JsonObject getJson() {
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("appKey", m_appKey);
        dataObject.addProperty("updates", m_updates);
        return dataObject;
    }

    public void save() throws IOException {
        String jsonString = getJson().toString();
        Files.writeString(m_dataFile.toPath(), jsonString);
    }
}
