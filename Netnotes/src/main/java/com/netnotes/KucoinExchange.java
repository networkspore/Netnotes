package com.netnotes;

import java.awt.Rectangle;
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
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class KucoinExchange extends Network implements NoteInterface {

    public static String DESCRIPTION = "KuCoin Exchange provides access to real time crypto market information for trading pairs.";
    public static String SUMMARY = "";
    public static String NAME = "KuCoin Exchange";

    public static String API_URL = "https://api.kucoin.com";

    private File logFile = new File("kucoinExchange-log.txt");
    private KucoinWebClient m_webClient = null;

    private File m_appDir = null;
    private File m_dataFile = null;
    private File m_testnetDataFile = null;
    private Stage m_appStage = null;

    private static long MIN_QUOTE_MILLIS = 5000;
    private ArrayList<PriceQuote> m_quotes = new ArrayList<PriceQuote>();

    public KucoinExchange(NetworksData networksData) {
        this(null, networksData);

    }

    public KucoinExchange(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.KUKOIN_EXCHANGE, networksData);

        setup(jsonObject);
    }

    public File getAppDir() {
        return m_appDir;
    }

    public static Image getAppIcon() {
        return App.kucoinImg;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/kucoin-30.png");
    }

    public File getDataFile() {
        return m_dataFile;
    }

    public File getTestnetDataFile() {
        return m_testnetDataFile;
    }

    private void setup(JsonObject jsonObject) {

        String testnetFileString = null;
        String mainnetFileString = null;
        String appDirFileString = null;
        if (jsonObject != null) {
            JsonElement appDirElement = jsonObject.get("appDir");
            JsonElement testnetFileElement = jsonObject.get("testnetFile");
            JsonElement mainnetFileElement = jsonObject.get("mainnetFile");

            testnetFileString = testnetFileElement == null ? null : testnetFileElement.toString();
            mainnetFileString = mainnetFileElement == null ? null : mainnetFileElement.toString();

            appDirFileString = appDirElement == null ? null : appDirElement.getAsString();

        }

        m_appDir = appDirFileString == null ? new File(getNetworksData().getAppDir().getAbsolutePath() + "/" + NAME) : new File(appDirFileString);

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

        }

        String location = getNetworksData().getAppDir().getAbsolutePath() + "/" + "/";

        m_testnetDataFile = new File(testnetFileString == null ? m_appDir.getAbsolutePath() + "/testnet" + NAME + ".dat" : testnetFileString);
        m_dataFile = new File(mainnetFileString == null ? m_appDir.getAbsolutePath() + "/" + NAME + ".dat" : mainnetFileString);

    }

    public void open() {
        super.open();
        showAppStage();
    }

    public Stage getAppStage() {
        return m_appStage;
    }

    private void showAppStage() {
        if (m_appStage == null) {

            KuCoinDataList kucoinData = new KuCoinDataList(this);

            double appStageWidth = 450;
            double appStageHeight = 480;

            m_appStage = new Stage();
            m_appStage.getIcons().add(getIcon());
            m_appStage.initStyle(StageStyle.UNDECORATED);
            m_appStage.setTitle(NAME);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_appStage.close();
                m_appStage = null;
                kucoinData.closeAll();
                kucoinData.removeUpdateListener();
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getIcon(), maxBtn, closeBtn, m_appStage);

            m_appStage.titleProperty().bind(Bindings.concat(NAME, " - ", kucoinData.sortOrderProperty()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip refreshTip = new Tooltip("Refresh");
            refreshTip.setShowDelay(new javafx.util.Duration(100));
            refreshTip.setFont(App.txtFont);

            Button refreshButton = new Button();
            refreshButton.setGraphic(IconButton.getIconView(new Image("/assets/refresh-white-30.png"), 30));
            refreshButton.setId("menuBtn");

            HBox menuBar = new HBox(refreshButton);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            VBox favoritesVBox = kucoinData.getFavoriteGridBox();

            ScrollPane favoriteScroll = new ScrollPane(favoritesVBox);
            favoriteScroll.setPadding(SMALL_INSETS);
            favoriteScroll.setId("bodyBox");

            VBox chartList = kucoinData.getGridBox();

            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(SMALL_INSETS);
            scrollPane.setId("bodyBox");

            VBox bodyPaddingBox = new VBox(scrollPane);
            bodyPaddingBox.setPadding(SMALL_INSETS);

            Button paddingBtn = new Button();
            paddingBtn.prefHeight(5);
            paddingBtn.setId("transparentColor");
            paddingBtn.setDisable(true);
            paddingBtn.setPadding(new Insets(0));
            HBox paddingRegion = new HBox(paddingBtn);

            favoritesVBox.getChildren().addListener((Change<? extends Node> changeEvent) -> {
                int numFavorites = favoritesVBox.getChildren().size();
                if (numFavorites > 0) {
                    if (!bodyPaddingBox.getChildren().contains(favoriteScroll)) {

                        bodyPaddingBox.getChildren().clear();
                        bodyPaddingBox.getChildren().addAll(favoriteScroll, paddingRegion, scrollPane);
                    }
                    int favoritesHeight = numFavorites * 40 + 40;
                    favoriteScroll.setPrefViewportHeight(favoritesHeight > 175 ? 175 : favoritesHeight);
                } else {
                    if (bodyPaddingBox.getChildren().contains(favoriteScroll)) {
                        bodyPaddingBox.getChildren().clear();
                        bodyPaddingBox.getChildren().add(scrollPane);
                    }
                }
            });

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setPrefWidth(150);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(SMALL_INSETS);

            VBox layoutBox = new VBox(titleBox, menuBar, bodyPaddingBox, footerVBox);

            Scene appScene = new Scene(layoutBox, appStageWidth, appStageHeight);
            appScene.getStylesheets().add("/css/startWindow.css");
            m_appStage.setScene(appScene);
            m_appStage.show();

            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            bodyPaddingBox.prefWidthProperty().bind(m_appStage.widthProperty());
            scrollPane.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            favoriteScroll.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(favoriteScroll.heightProperty()).subtract(titleBox.heightProperty()).subtract(menuBar.heightProperty()).subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            favoritesVBox.prefWidthProperty().bind(favoriteScroll.prefViewportWidthProperty().subtract(40));
            lastUpdatedField.textProperty().bind(kucoinData.getLastUpdated().asString());

            ResizeHelper.addResizeListener(m_appStage, 250, 300, rect.getWidth() / 3, rect.getHeight());

        } else {
            m_appStage.show();
        }
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

                            break;
                        case "pong":
                            m_webClient.setPong(System.currentTimeMillis());
                            break;
                        case "message":
                            JsonElement tunnelIdElement = messageObject.get("tunnelId");

                            if (tunnelIdElement != null) {

                                String tunnelId = tunnelIdElement.getAsString();

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

                        m_webClient.subscribeToCandles(tunnelId, symbolElement.getAsString(), timeSpanElement.getAsString());
                    } else {
                        return false;
                    }

                    break;
                case "GET_ALL_TICKERS":
                    int page = 1;
                    int pageSize = 50;
                    return getAllTickers(page, pageSize, onSucceeded, onFailed);

            }
        } else {
            return false;
        }
        return true;
    }

    public boolean isClientReady() {
        return (m_webClient != null && m_webClient.isReady());
    }

    public void getCandlesDataset(String symbol, String timespan, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        String urlString = API_URL + "/api/v1/market/candles?type=" + timespan + "&symbol=" + symbol;
        try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Utils.getUrlJson(urlString, onSuccess, onFailed, null);

    }

    public boolean getAllTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v1/market/allTickers";
        try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Utils.getUrlJson(urlString, onSucceeded, onFailed, null);

        return false;
    }

    public boolean getAllTickers(int page, int pageSize, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v1/market/allTickers?currentPage=" + page + "&pageSize=" + pageSize;
        try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Utils.getUrlJson(urlString, onSucceeded, onFailed, null);

        return false;
    }

    public boolean getSymbols(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/api/v2/symbols";
        try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Utils.getUrlJson(urlString, onSucceeded, onFailed, null);

        return false;
    }
}
