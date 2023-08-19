package com.netnotes;

import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;

public class NamedNodeUrl {

    //networkType
    public final static String TESTNET_STRING = NetworkType.TESTNET.toString();
    public final static String MAINNET_STRING = NetworkType.MAINNET.toString();

    //type
    public final static String LIGHT_CLIENT = "LIGHT_CLIENT";
    public final static String FULL_NODE = "FULL_NODE";

    private int m_port = 9053;
    private String m_id = "DEFAULT_SERVER_#1";
    private String m_name = "Public node #1";
    private String m_protocol = "http";
    private String m_ip = "213.239.193.208";
    private NetworkType m_networkType = NetworkType.MAINNET;
    private String m_apiKey = null;

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>(null);

    public NamedNodeUrl() {

    }

    public NamedNodeUrl(JsonObject json) {
        if (json != null) {

            JsonElement idElement = json.get("id");
            JsonElement nameElement = json.get("name");
            JsonElement ipElement = json.get("ip");
            JsonElement portElement = json.get("port");
            JsonElement networkTypeElement = json.get("networkType");
            JsonElement nodeTypeElement = json.get("nodeType");
            JsonElement apiKeyElement = json.get("apiKey");

            m_id = idElement != null ? idElement.getAsString() : FriendlyId.createFriendlyId();

            if (networkTypeElement != null && networkTypeElement.isJsonPrimitive()) {
                String networkTypeString = networkTypeElement.getAsString();
                m_networkType = networkTypeString.equals(TESTNET_STRING) ? NetworkType.TESTNET : NetworkType.MAINNET;
            }

            m_name = nameElement != null && idElement != null ? nameElement.getAsString() : m_networkType.toString() + " #" + m_id;
            m_ip = ipElement != null ? ipElement.getAsString() : m_ip;
            m_port = portElement != null ? portElement.getAsInt() : m_port;
            m_apiKey = apiKeyElement != null ? apiKeyElement.getAsString() : m_apiKey;

        }
    }

    public NamedNodeUrl(String id, String name, String ip, int port, String apiKey, NetworkType networkType) {
        m_id = id;
        m_name = name;
        m_ip = ip;
        m_port = port;
        m_networkType = networkType;
        m_apiKey = apiKey;
    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getIP() {
        return m_ip;
    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public int getPort() {
        return m_port;
    }

    public String getProtocol() {
        return m_protocol;
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.addProperty("name", m_name);
        json.addProperty("protocol", m_protocol);
        json.addProperty("ip", m_ip);
        json.addProperty("port", m_port);
        if (m_apiKey != null) {
            json.addProperty("apiKey", m_apiKey);
        }
        json.addProperty("networkType", m_networkType == null ? MAINNET_STRING : m_networkType.toString());
        return json;
    }

    public IconButton getButton() {

        IconButton btn = new IconButton(null, toString(), IconStyle.ROW);
        btn.setButtonId(m_id);
        return btn;

    }

    public SimpleObjectProperty<LocalDateTime> lastUpdatedProperty() {
        return m_lastUpdated;
    }

    @Override
    public String toString() {
        String formattedName = String.format("%-28s", m_name);
        String formattedUrl = String.format("%-30s", "(" + m_protocol + "://" + m_ip + ":" + m_port + ")");

        return formattedName + " " + formattedUrl;
    }
}
