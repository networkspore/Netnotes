package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.Utils;

import io.circe.Json;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class KucoinExchange extends Network implements NoteInterface {

    public static String DESCRIPTION = "KuCoin Exchange provides access to real time crypto market information for trading pairs.";
    public static String SUMMARY = "";
    public static String NAME = "KuCoin Exchange";

    public static String API_URL = "https://api.kucoin.com/api/v1/";

    private File logFile = new File("kucoinExchange-log.txt");
    private KucoinWebClient m_webClient = null;

    private ArrayList<String> m_listeningIds = new ArrayList<String>();

    private static long MIN_QUOTE_MILLIS = 5000;
    private ArrayList<PriceQuote> m_quotes = new ArrayList<PriceQuote>();

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

    private void removeStaleQuotes(ArrayList<PriceQuote> stalePriceQuotes) {
        for (PriceQuote quote : stalePriceQuotes) {
            m_quotes.remove(quote);
        }
    }

    public PriceQuote findCurrentQuote(String transactionCurrency, String quoteCurrency) {
        ArrayList<PriceQuote> staleQuotes = new ArrayList<>();

        for (int i = 01; i < m_quotes.size(); i++) {
            PriceQuote quote = m_quotes.get(i);

            if (quote.howOldMillis() > MIN_QUOTE_MILLIS) {
                staleQuotes.add(quote);
            } else {
                if (quote.getTransactionCurrency().equals(transactionCurrency) && quote.getQuoteCurrency().equals(quoteCurrency)) {
                    removeStaleQuotes(staleQuotes);
                    return quote;
                }
            }
        }
        if (staleQuotes.size() > 0) {
            removeStaleQuotes(staleQuotes);
        }
        return null;
    }

    private void returnQuote(PriceQuote quote, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<PriceQuote> task = new Task<PriceQuote>() {
            @Override
            public PriceQuote call() {

                return quote;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void getQuote(String transactionCurrency, String quoteCurrency, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        // prices?base=" + baseCurrency + "&currencies=ERG";
        String urlString = API_URL + "prices?base=" + transactionCurrency + "&currencies=" + quoteCurrency;
        Utils.getUrlJson(urlString, success -> {
            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null) {
                JsonObject jsonObject = (JsonObject) sourceObject;

                JsonElement dataElement = jsonObject.get("data");
                try {
                    Files.writeString(logFile.toPath(), "\n" + API_URL + " returned: " + jsonObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
                if (dataElement != null && dataElement.isJsonObject()) {

                    JsonElement currentAmountElement = dataElement.getAsJsonObject().get(quoteCurrency);

                    if (currentAmountElement != null) {
                        double amount = currentAmountElement.getAsDouble();
                        PriceQuote quote = new PriceQuote(amount, transactionCurrency, quoteCurrency);
                        m_quotes.add(quote);
                        returnQuote(quote, onSucceeded, onFailed);
                    } else {
                        returnQuote(new PriceQuote(-1, null, null), onSucceeded, onFailed);
                    }
                } else {
                    returnQuote(new PriceQuote(-1, null, null), onSucceeded, onFailed);
                }
            } else {
                returnQuote(new PriceQuote(-1, null, null), onSucceeded, onFailed);
            }

        }, onFailed, null);

    }

    public void checkQuote(String transactionCurrency, String quoteCurrency, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        PriceQuote currentQuote = findCurrentQuote(transactionCurrency, quoteCurrency);

        if (currentQuote != null) {
            returnQuote(currentQuote, onSucceeded, onFailed);
        } else {
            getQuote(transactionCurrency, quoteCurrency, onSucceeded, onFailed);
        }
    }

    public void getCandlesDataset(String symbol, String timespan, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        String urlString = API_URL + "market/candles?type=" + timespan + "&symbol=" + symbol;
        try {
            Files.writeString(logFile.toPath(), "getting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Utils.getUrlJson(urlString, onSuccess, onFailed, null);

    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");
        JsonElement tunnelIdElement = note.get("tunnelId");
        JsonElement networkId = note.get("networkId");
        JsonElement symbolElement = note.get("symbol");
        JsonElement timeSpanElement = note.get("timeSpan");
        JsonElement transactionCurrencyElement = note.get("transactionCurrency");
        JsonElement quoteCurrencyElement = note.get("quoteCurrency");

        if (subjecElement != null) {
            String subject = subjecElement.getAsString();
            switch (subject) {
                case "GET_QUOTE":
                    if (transactionCurrencyElement != null && quoteCurrencyElement != null) {
                        String transactionCurrency = transactionCurrencyElement.getAsString();
                        String quoteCurrency = quoteCurrencyElement.getAsString();

                        checkQuote(transactionCurrency, quoteCurrency, onSucceeded, onFailed);

                        return true;
                    } else {
                        return false;
                    }
                //break;
                case "GET_CANDLES_DATASET":

                    if (symbolElement != null && timeSpanElement != null) {
                        getCandlesDataset(symbolElement.getAsString(), timeSpanElement.getAsString(), onSucceeded, onFailed);
                    } else {
                        return false;
                    }
                    break;
                case "IS_CLIENT_READY":
                    return isClientReady();

                case "SUBSCRIBE_TO_CANDLES":
                    if (isClientReady() && tunnelIdElement != null && symbolElement != null && timeSpanElement != null) {
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

    public boolean isClientReady() {
        return (m_webClient != null && m_webClient.isReady());
    }
}
