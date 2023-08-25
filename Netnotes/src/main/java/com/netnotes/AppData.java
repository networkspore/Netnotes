package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.beans.property.SimpleBooleanProperty;

public class AppData {

    private File m_dataFile;
    private String m_appKey;

    private SimpleBooleanProperty m_updatesProperty = new SimpleBooleanProperty(true);

    public AppData(File dataFile) throws Exception {
        m_dataFile = dataFile;
        String jsonString = Files.readString(m_dataFile.toPath());

        JsonObject dataObject = new JsonParser().parse(jsonString).getAsJsonObject();
        if (dataObject != null) {
            JsonElement updatesElement = dataObject.get("updates");
            JsonElement appkeyElement = dataObject.get("appKey");

            boolean updates = updatesElement != null && updatesElement.isJsonPrimitive() ? updatesElement.getAsBoolean() : true;
            if (appkeyElement != null && appkeyElement.isJsonPrimitive()) {
                m_appKey = appkeyElement.getAsString();
                m_updatesProperty.set(updates);
            } else {
                throw new Exception("Null appKey");
            }
        } else {
            throw new Exception("Null Json");
        }

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

    public SimpleBooleanProperty updatesProperty() {
        return m_updatesProperty;
    }

    public void setUpdates(boolean updates) throws IOException {
        m_updatesProperty.set(updates);
        save();
    }

    public JsonObject getJson() {
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("appKey", m_appKey);
        dataObject.addProperty("updates", m_updatesProperty.get());
        return dataObject;
    }

    public void save() throws IOException {
        String jsonString = getJson().toString();
        Files.writeString(m_dataFile.toPath(), jsonString);
    }
}
