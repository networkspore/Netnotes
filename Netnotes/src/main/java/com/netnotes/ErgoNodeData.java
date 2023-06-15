package com.netnotes;

import java.net.MalformedURLException;
import java.net.URL;

import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.ergo.ErgoInterface;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoNodeData extends IconButton {

    private String m_networkId;

    private int m_exPort = ErgoNetwork.EXTERNAL_PORT;
    private int m_port = ErgoNetwork.MAINNET_PORT;
    private String m_apiKey = "";
    private String m_url = null;

    public SimpleStringProperty nodeApiAddress;

    private NetworkType m_networkType;
    private ErgoNetwork m_ergoNetwork;

    public ErgoNodeData(JsonObject jsonObj, ErgoNetwork ergoNetwork) {
        super();
        m_ergoNetwork = ergoNetwork;

        JsonElement networkIdElement = jsonObj == null ? null : jsonObj.get("networkId");
        JsonElement apiKeyElement = jsonObj == null ? null : jsonObj.get("apiKey");
        JsonElement urlElement = jsonObj == null ? null : jsonObj.get("url");
        JsonElement nameElement = jsonObj == null ? null : jsonObj.get("name");
        JsonElement networkTypeElement = jsonObj == null ? null : jsonObj.get("networkType");

        m_apiKey = apiKeyElement == null ? "" : apiKeyElement.getAsString();
        m_networkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.getAsString().equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : NetworkType.TESTNET;

        m_networkId = networkIdElement == null ? FriendlyId.createFriendlyId() : networkIdElement.getAsString();
        setName(nameElement == null ? "Node #" + m_networkId : nameElement.getAsString());
        m_url = urlElement == null ? null : urlElement.getAsString();

        setIconStyle(IconStyle.ROW);
    }

    public String getNetworkTypeString() {
        return m_networkType.toString();
    }

    public String getNetworkId() {
        return m_networkId;
    }

    public JsonObject getExplorerUrlObject() {

        JsonObject getExplorerUrlObject = new JsonObject();
        getExplorerUrlObject.addProperty("subject", "GET_EXPLORER_URL");
        getExplorerUrlObject.addProperty("fullNetworkId", m_ergoNetwork.getNetworkId());
        getExplorerUrlObject.addProperty("networkType", m_networkType.toString());
        return getExplorerUrlObject;
    }

    public boolean getClient(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        NoteInterface explorerInterface = m_ergoNetwork.getExplorerInterface();
        if (explorerInterface != null) {
            return explorerInterface.sendNote(getExplorerUrlObject(), success -> {
                Object successObject = success.getSource().getValue();

                JsonObject successJson = successObject == null ? null : (JsonObject) successObject;
                JsonElement urlElement = successJson == null ? null : successJson.get("url");
                String explorerUrl = urlElement == null ? null : urlElement.getAsString();

                ErgoClient ergoClient = explorerUrl == null ? null : RestApiErgoClient.create(m_url, m_networkType, m_apiKey, explorerUrl);

                returnClient(ergoClient, onSucceeded, onFailed);

            }, onFailed);

        } else {
            return false;
        }

    }

    private void returnClient(ErgoClient ergoClient, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<ErgoClient> task = new Task<ErgoClient>() {
            @Override
            public ErgoClient call() {

                return ergoClient;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

    }

    public String getUrl() {
        return m_url;
    }

    public void setHost(String host) {
        m_url = host;
    }

    public int getPort() {
        return m_port;
    }

    public void setPort(int port) {
        m_port = port;

    }

    public void setExternalPort(int port) {
        m_port = port;
    }

    public int getExternalPort() {
        return m_port;
    }

    public JsonObject getJsonObject() {

        JsonObject networkObj = new JsonObject();

        //String hostValue = m_host == null ? "" : m_host.toString();
        //  networkObj.addProperty("host", hostValue);
        networkObj.addProperty("port", m_port);
        networkObj.addProperty("externalPort", m_exPort);

        return networkObj;

    }

    /*
    public void setUrl(String url) throws MalformedURLException {
        if (url == null) {
            m_host = null;
        } else {
            if (url.equals("")) {
                m_host = null;
            } else {

                char c0 = url.charAt(0);

                URL testURL = new URL(c0 != 'h' ? "http://" + url : url);

                m_host = testURL.getHost();

                int port = testURL.getPort();

                if (port != 80 && port != -1) {

                    setPort(port);

                }

            }

        }

    } */
}
