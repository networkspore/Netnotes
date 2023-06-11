package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class KucoinExchange extends Network implements NoteInterface {

    public static String DESCRIPTION = "KuCoin Exchange provides access to real time crypto market information for trading pairs.";
    public static String SUMMARY = "KuCoin is a cryptocurrency exchange built with the mission to “facilitate the global free flow of digital value.” It claims to have an emphasis on intuitive design, simple registration process and high level of security.";
    public static String NAME = "KuCoin Exchange";

    private File logFile = new File("kucoinExchange-log.txt");
    private KucoinWebClient m_webClient = new KucoinWebClient();

    private ArrayList<String> m_listeningIds = new ArrayList<String>();

    public KucoinExchange(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.KUKOIN_EXCHANGE, networksData);

    }

    public KucoinExchange(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.KUKOIN_EXCHANGE, networksData);

    }

    public static Image getAppIcon() {
        return App.kucoinImg;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/kucoin-30.png");
    }

    private void connectToExchange() {
        m_webClient.requestSocket(e -> {

            JsonObject jsonObject = (JsonObject) e.getSource().getValue();

            try {
                Files.writeString(logFile.toPath(), "\nKucoinExchange - connectToExchange:\n" + jsonObject.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }

            if (jsonObject != null) {
                JsonElement dataElement = jsonObject.get("data");
                if (dataElement != null) {
                    JsonObject dataObject = dataElement.getAsJsonObject();
                    String tokenString = dataObject.get("token").getAsString();
                    JsonArray serversArray = dataObject.get("instanceServers").getAsJsonArray();
                    int i = 0;
                    int serverArraySize = serversArray.size();
                    String endpoint = "";
                    int pingInterval = 18000;

                    while (endpoint.equals("") && i < serverArraySize) {
                        JsonElement arrayElement = serversArray.get(i);
                        if (arrayElement != null) {

                            JsonObject serverObj = arrayElement.getAsJsonObject();
                            endpoint = serverObj.get("endpoint").getAsString();
                            pingInterval = serverObj.get("pingInterval").getAsInt();

                        }
                        i++;
                    }

                    openKucoinSocket(tokenString, endpoint, pingInterval);
                }
            }
        }, onFailed -> {
            //m_webClient.setReady(false);
        });

    }

    public JsonObject getReadyJson() {
        JsonObject json = new JsonObject();
        json.addProperty("subject", "READY");
        json.addProperty("networkId", getNetworkId());
        return json;
    }

    private void openKucoinSocket(String tokenString, String endpointURL, int pingInterval) {

        URI uri;
        try {
            uri = new URI(endpointURL + "?token=" + tokenString + "&[connectId=" + m_webClient.getClientId() + "]");
        } catch (URISyntaxException e) {

            return;
        }

        WebSocketClient websocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {

            }

            @Override
            public void onMessage(String s) {

                try {
                    Files.writeString(logFile.toPath(), "\nPriceChart - websocket message:\n" + s + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

                JsonObject messageObject = new JsonParser().parse(s).getAsJsonObject();
                JsonElement typeElement = messageObject.get("type");

                if (typeElement != null) {
                    String type = typeElement.getAsString();

                    switch (type) {
                        case "welcome":
                            m_webClient.setPong(System.currentTimeMillis());
                            m_webClient.setReady(true);

                            m_webClient.startPinging();
                            if (m_listeningIds.size() > 0) {
                                NetworksData netData = getNetworksData();
                                ArrayList<String> listeningIds = getListeningIds();
                                netData.broadcastNoteToNetworkIds(getReadyJson(), listeningIds);
                            }
                            break;
                        case "pong":
                            m_webClient.setPong(System.currentTimeMillis());
                            break;
                        case "message":
                            JsonElement tunnelIdElement = messageObject.get("tunnelId");

                            if (tunnelIdElement != null) {

                                String tunnelId = tunnelIdElement.getAsString();

                                int indexOfNetworkId = tunnelId.indexOf(".");

                                if (indexOfNetworkId == -1) {
                                    getNetworksData().sendNoteToNetworkId(messageObject, tunnelId, null, null);
                                } else {
                                    getNetworksData().sendNoteToFullNetworkId(messageObject, tunnelId, null, null);
                                }

                            }

                            break;

                    }
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                m_webClient.setReady(false);

                /*ArrayList<WebClientListener> listeners = getMessageListeners();
                for (WebClientListener messagelistener : listeners) {

                    messagelistener.close(i, s, b);
                }*/
            }

            @Override
            public void onError(Exception e) {
                /*ArrayList<WebClientListener> listeners = getMessageListeners();
                for (WebClientListener messagelistener : listeners) {
                    messagelistener.error(e);
                }*/
                try {
                    Files.writeString(logFile.toPath(), "\nPriceChart - websocket error:\n" + e.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }
        };
        websocketClient.connect();
    }

    public ArrayList<String> getListeningIds() {
        return m_listeningIds;
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        if (!m_webClient.isReady()) {
            connectToExchange();
        }
        JsonElement subjecElement = note.get("subject");
        JsonElement tunnelIdElement = note.get("tunnelId");
        JsonElement networkId = note.get("networkId");
        JsonElement symbolElement = note.get("symbol");
        JsonElement timeSpanElement = note.get("timeSpan");

        if (subjecElement != null) {
            String subject = subjecElement.getAsString();
            switch (subject) {
                case "GET_CANDLES_DATASET":

                    if (symbolElement != null && timeSpanElement != null) {
                        m_webClient.getCandlesDataset(symbolElement.getAsString(), timeSpanElement.getAsString(), onSucceeded, onFailed);
                    } else {
                        return false;
                    }
                    break;
                case "SUBSCRIBE_TO_CANDLES":
                    if (tunnelIdElement != null && symbolElement != null && timeSpanElement != null) {
                        String tunnelId = tunnelIdElement.getAsString();
                        int tunnelIdIndex = m_listeningIds.indexOf(tunnelId);

                        if (tunnelIdIndex == -1) {
                            m_listeningIds.add(tunnelId);
                        }
                        m_webClient.subscribeToCandles(tunnelId, symbolElement.getAsString(), timeSpanElement.getAsString());
                    } else {
                        return false;
                    }

                    break;
            }
        } else {
            return false;
        }
        return true;
    }
}
