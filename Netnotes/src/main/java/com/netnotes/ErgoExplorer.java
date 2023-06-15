package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.satergo.ergo.ErgoNodeAccess;
import com.utils.Utils;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class ErgoExplorer extends Network implements NoteInterface {

    public static String DESCRIPTION = "Ergo Explorer allows you to explore and search the Ergo blockchain.";
    public static String SUMMARY = "Installing the Ergo Explorer allows balance and transaction information to be looked up for wallet addresses.";
    public static String NAME = "Ergo Explorer";
    public static String ErgoMainnet_EXPLORER_URL = "https://api.ergoplatform.com";
    public static String ErgoTestnet_EXPLORER_URL = "https://api-testnet.ergoplatform.com";

    private String m_mainnetExplorerUrlString = ErgoMainnet_EXPLORER_URL;
    private String m_testnetExplorerUrlString = ErgoTestnet_EXPLORER_URL;

    public ErgoExplorer(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_EXPLORER, networksData);

    }

    public ErgoExplorer(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_EXPLORER, networksData);

        JsonElement explorerURLElement = jsonObject.get("mainnetExplorerURL");
        JsonElement testnetExplorerUrlElement = jsonObject.get("testnetExplorerURL");

        m_mainnetExplorerUrlString = explorerURLElement != null ? explorerURLElement.getAsString() : ErgoMainnet_EXPLORER_URL;
        m_testnetExplorerUrlString = testnetExplorerUrlElement != null ? explorerURLElement.getAsString() : ErgoMainnet_EXPLORER_URL;

    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergo-explorer-30.png");
    }

    public static Image getAppIcon() {
        return App.ergoExplorerImg;
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjecElement = note.get("subject");
        if (subjecElement != null) {
            switch (subjecElement.getAsString()) {
                case "GET_BALANCE":

                    JsonElement addressElement = note.get("address");
                    if (addressElement != null) {
                        JsonElement networkTypeElement = note.get("networkType");
                        String networkType = networkTypeElement != null ? networkTypeElement.getAsString() : NetworkType.MAINNET.toString();
                        String address = addressElement != null ? addressElement.getAsString() : null;

                        getBalance(networkType, address, onSucceeded, onFailed);
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    public String getMainnetExplorerURL() {
        return m_mainnetExplorerUrlString;
    }

    public void getBalance(String networkType, String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        String urlString = networkType == NetworkType.MAINNET.toString() ? m_mainnetExplorerUrlString : m_testnetExplorerUrlString;
        urlString += "/api/v1/addresses/" + address + "/balance/total";

        Utils.getUrlJson(urlString, onSucceeded, onFailed, null);
    }
    /*
        public static int getNetworkBlockHeight(NetworkType networkType) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = ErgoNodeAccess.httpRequestBuilder().uri(URI.create(getExplorerUrl(networkType) + "/blocks?limit=1&sortBy=height&sortDirection=desc")).build();
        try {
            JsonObject body = JsonParser.object().from(httpClient.send(request, ofString()).body());
            return body.getArray("items").getObject(0).getInt("height");
        } catch (JsonParserException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
     */

}
