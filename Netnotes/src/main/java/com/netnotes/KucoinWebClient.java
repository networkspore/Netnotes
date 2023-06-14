package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.utils.Utils;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class KucoinWebClient implements WebClient {

    private String m_clientID;
    private long m_pong;
    private File logFile = new File("kucoinWebClient-log.txt");

    private Timer m_pingTimer;
    private int m_pingInterval;
    private WebSocketClient m_websocketClient = null;

    public static String PUBLIC_TOKEN_URL = "https://api.kucoin.com/api/v1/bullet-public";

    public static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    public KucoinWebClient() {

        m_clientID = FriendlyId.createFriendlyId();

    }

    public String getClientId() {
        return m_clientID;
    }

    public void setClientId(String clientId) {
        m_clientID = clientId;
    }

    public void setWebSocketClient(WebSocketClient webSocketClient) {
        m_websocketClient = webSocketClient;
    }

    public long getPong() {
        return m_pong;
    }

    public void setPong(long pong) {
        m_pong = pong;
    }

    public void requestSocket(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    URL url = new URL(PUBLIC_TOKEN_URL);

                    //  String urlParameters = "param1=a&param2=b&param3=c";
                    //  byte[] postData = new byte[]{};
                    //  int postDataLength = postData.length;
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    //  conn.setDoOutput(false);
                    //   conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    //  conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    //   conn.setRequestProperty("charset", "utf-8");
                    //  conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    //   conn.setUseCaches(false);

                    conn.setRequestProperty("Content-Type", "application/json");

                    conn.setRequestProperty("User-Agent", USER_AGENT);

                    // long contentLength = con.getContentLengthLong();
                    inputStream = conn.getInputStream();

                    byte[] buffer = new byte[2048];

                    int length;
                    // long downloaded = 0;

                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);
                        //   downloaded += (long) length;

                    }
                    String jsonString = outputStream.toString();

                    try {
                        Files.writeString(logFile.toPath(), "\nPriceChart - json string1:\n" + jsonString + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }

                    JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
                    return jsonObject;
                } catch (JsonParseException | IOException e) {
                    return null;
                }

            }

        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.start();
    }
    private boolean m_ready = false;

    public boolean isReady() {
        return m_ready;
    }

    public void setReady(boolean ready) {
        m_ready = ready;
    }

    public void startPinging() {

        if (m_websocketClient != null) {
            JsonObject pingMessageObj = new JsonObject();
            pingMessageObj.addProperty("id", getClientId());
            pingMessageObj.addProperty("type", "ping");

            String pingString = pingMessageObj.toString();

            m_pingTimer = new Timer("pingTimer:" + getClientId(), true);

            TimerTask pingTask = new TimerTask() {

                @Override
                public void run() {
                    long sinceLastPong = System.currentTimeMillis() - m_pong;

                    if (sinceLastPong < m_pingInterval * 3) {
                        m_websocketClient.send(pingString);
                    } else {
                        if (m_websocketClient != null) {
                            m_websocketClient.close();
                            m_pingTimer.cancel();
                            m_pingTimer.purge();
                        }
                    }
                }
            };
            m_pingTimer.schedule(pingTask, 0, m_pingInterval);
        }
    }

    public String createMessageString(String type, String topic, boolean response, String id) {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("id", id);
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public String createMessageString(String type, String topic, boolean response) {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public String createMessageString(String tunnelId, String type, String topic, boolean response, String id) {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("id", id);
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("tunnelId", tunnelId);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public String createMessageString(String tunnelId, String type, String topic, boolean response) {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("type", type);
        messageObj.addProperty("topic", topic);
        messageObj.addProperty("tunnelId", tunnelId);
        messageObj.addProperty("response", response);

        return messageObj.toString();
    }

    public void subscribeToCandles(String tunnelId, String symbol, String timespan) {
        String topic = "/market/candles:" + symbol + "_" + timespan;

        m_websocketClient.send(createMessageString(tunnelId, "subscribe", topic, true));
    }

    public void unsubscribeToCandles(String tunnelId, String symbol, String timespan) {

        m_websocketClient.send(createMessageString(tunnelId, "unsubscribe", "/market/candles:" + symbol + "_" + timespan, false));
    }

    public void subescribeToTicker(String tunnelId, String symbol) {

        m_websocketClient.send(createMessageString(tunnelId, "subscribe", "/market/ticker:" + symbol, false));
    }

    public void unsubscribeToTicker(String tunnelId, String clientID, String symbol) {
        m_websocketClient.send(createMessageString(tunnelId, "unsubscribe", "/market/ticker:" + symbol, false));
    }

    public Exception terminate() {
        m_pingTimer.cancel();
        Exception ex = null;
        try {
            m_websocketClient.closeBlocking();
        } catch (InterruptedException e) {
            ex = e;
        }

        return ex;
    }

}
