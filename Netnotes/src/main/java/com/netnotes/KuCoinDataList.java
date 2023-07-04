package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class KuCoinDataList extends Network implements NoteInterface {

    private File logFile = new File("kucoinChartsList-log.txt");
    private KucoinExchange m_kucoinExchange;

    private VBox m_favoriteGridBox = new VBox();
    private VBox m_gridBox = new VBox();

    private ArrayList<KucoinMarketItem> m_marketsList = new ArrayList<KucoinMarketItem>();
    private ArrayList<HBox> m_favoritesList = new ArrayList<HBox>();

    private boolean m_notConnected = false;
    private SimpleStringProperty m_statusMsg = new SimpleStringProperty("Loading...");

    private int m_sortMethod = 0;
    private boolean m_sortDirection = false;
    private String m_searchText = null;

    public KuCoinDataList(KucoinExchange kuCoinCharts) {
        super(null, "Ergo Charts List", "KUCOIN_CHARTS_LIST", kuCoinCharts);
        m_kucoinExchange = kuCoinCharts;

        setup();

    }

    private void setup() {
        updateGridBox();

        m_kucoinExchange.getAllTickers(success -> {
            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {

                readTickers(m_favoritesList, getDataJson((JsonObject) sourceObject), onSuccess -> {
                    getFile();
                    sortByChangeRate(false);
                    sort();
                    updateGridBox();

                    getLastUpdated().set(LocalDateTime.now());
                }, failed -> {
                    try {
                        Files.writeString(logFile.toPath(), "setup failed 1 " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    getFile();
                    m_notConnected = true;
                    m_statusMsg.set("Not connected");
                    updateGridBox();

                });

            } else {
                try {
                    Files.writeString(logFile.toPath(), "setup failed 2  instance of object is not json", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
                getFile();
                m_notConnected = true;
                m_statusMsg.set("Not connected");
                updateGridBox();
            }
        }, failed -> {
            try {
                Files.writeString(logFile.toPath(), "setup failed 3 " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            getFile();
            m_notConnected = true;
            m_statusMsg.set("Not connected");
            updateGridBox();
        });
    }

    public void closeAll() {

    }

    public HBox getFavoriteBox(String symbol) {
        for (int i = 0; i < m_favoritesList.size(); i++) {
            HBox favBox = m_favoritesList.get(i);
            Object userObject = favBox.getUserData();

            if (userObject != null && userObject instanceof String) {
                String boxSymbol = (String) userObject;

                if (boxSymbol.equals(symbol)) {
                    return favBox;
                }
            } else {
                m_favoritesList.remove(favBox);
            }
        }
        return null;
    }

    public void addFavorite(String symbol, boolean doSave) {
        if (getFavoriteBox(symbol) == null) {

            KucoinMarketItem item = getMarketItem(symbol);
            HBox favoriteBox = item.getRowBox();

            favoriteBox.setUserData(symbol);
            m_favoritesList.add(favoriteBox);

            updateGridBox();

            if (doSave) {
                save();
            }
        }
    }

    public void removeFavorite(String symbol, boolean doSave) {
        HBox favBox = getFavoriteBox(symbol);
        if (favBox != null) {
            m_favoritesList.remove(favBox);

            updateGridBox();

            if (doSave) {
                save();
            }
        }
    }

    public SimpleStringProperty statusProperty() {
        return m_statusMsg;
    }

    private JsonObject getDataJson(JsonObject urlJson) {
        if (urlJson != null) {
            JsonElement dataElement = urlJson.get("data");
            if (dataElement != null && dataElement.isJsonObject()) {
                return dataElement.getAsJsonObject();
            }
        }
        return null;
    }

    public KucoinMarketItem getMarketItem(String symbol) {
        if (symbol != null) {

            for (KucoinMarketItem item : m_marketsList) {
                if (item.getSymbol().equals(symbol)) {
                    return item;
                }
            }
        }
        return null;
    }

    public void updateTickers() {
        m_kucoinExchange.getAllTickers(success -> {
            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                readTickers(m_favoritesList, getDataJson((JsonObject) sourceObject), succeeded -> {
                    m_notConnected = false;
                    updateGridBox();
                    getLastUpdated().set(LocalDateTime.now());
                }, secondfail -> {

                    m_notConnected = true;
                    m_statusMsg.set("Not connected");
                    updateGridBox();
                    getLastUpdated().set(LocalDateTime.now());
                }
                );

            } else {
                m_notConnected = true;
                m_statusMsg.set("Not connected");
                updateGridBox();
                getLastUpdated().set(LocalDateTime.now());
            }
        }, failed -> {
            m_notConnected = true;
            m_statusMsg.set("Not connected");
            updateGridBox();
            getLastUpdated().set(LocalDateTime.now());
        });
    }

    public KuCoinDataList getKuCoinDataList() {
        return this;
    }

    private void readTickers(ArrayList<HBox> favorites, JsonObject tickersJson, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                JsonElement tickerElement = tickersJson.get("ticker");

                if (tickerElement != null && tickerElement.isJsonArray()) {

                    JsonArray jsonArray = tickerElement.getAsJsonArray();

                    int i = 0;
                    boolean init = false;
                    if (m_marketsList.size() == 0) {
                        init = true;
                    }

                    for (i = 0; i < jsonArray.size(); i++) {
                        try {
                            JsonElement tickerObjectElement = jsonArray.get(i);
                            if (tickerObjectElement != null && tickerObjectElement.isJsonObject()) {

                                JsonObject tickerJson = tickerObjectElement.getAsJsonObject();
                                JsonElement symbolElement = tickerJson.get("symbol");
                                if (symbolElement != null && symbolElement.isJsonPrimitive()) {

                                    String symbolString = symbolElement.getAsString();

                                    KucoinTickerData tickerData = new KucoinTickerData(symbolString, tickerJson);

                                    boolean isFavorite = false;

                                    for (HBox favorite : m_favoritesList) {
                                        Object favoriteUserData = favorite.getUserData();
                                        if (favoriteUserData instanceof String) {
                                            if (symbolString.equals((String) favoriteUserData)) {
                                                isFavorite = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (init) {
                                        String id = FriendlyId.createFriendlyId();
                                        m_marketsList.add(new KucoinMarketItem(m_kucoinExchange, id, symbolString, symbolString, isFavorite, tickerData, getKuCoinDataList()));
                                    } else {
                                        KucoinMarketItem item = getMarketItem(symbolString);
                                        item.tickerDataProperty().set(tickerData);
                                    }
                                }
                            }

                        } catch (Exception jsonException) {
                            try {
                                Files.writeString(logFile.toPath(), "\njson error" + jsonException.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e2) {

                            }
                        }

                    }

                } else {
                    try {
                        Files.writeString(logFile.toPath(), "\nticker json bad format: \n" + tickersJson.toString());
                    } catch (IOException e) {

                    }
                }

                return null;

            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSuccess);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

    }

    private void getFile() {
        JsonObject json = null;

        File dataFile = m_kucoinExchange.getDataFile();
        if (dataFile != null && dataFile.isFile()) {
            try {
                json = Utils.readJsonFile(getNetworksData().getAppKey(), dataFile.toPath());
                openJson(json);
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

                try {
                    Files.writeString(logFile.toPath(), "\ngetfile error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

            }

        }

    }

    /*
    private void openJson(JsonObject json) {
        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray()) {

            JsonArray dataArray = dataElement.getAsJsonArray();

            //  if (m_ergoTokens.getNetworkType().toString().equals(networkType)) {
            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement idElement = objJson.get("id");

                    if (nameElement != null && nameElement.isJsonPrimitive() && idElement != null && idElement.isJsonPrimitive()) {

                        try {
                            Files.writeString(logFile.toPath(), "\ndataElement: " + objJson.toString());
                        } catch (IOException e) {

                        }
                    }

                }
            }
            //   }
        }

        updateGridBox();
    } */
    public VBox getGridBox() {

        return m_gridBox;
    }

    public VBox getFavoriteGridBox() {
        return m_favoriteGridBox;
    }

    private void sort() {
        switch (m_sortMethod) {
            case 0:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getChangeRate));

                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getChangeRate)));

                }
                break;
            case 1:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getChangePrice));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getChangePrice)));
                }
                break;
            case 2:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getHigh));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getHigh)));
                }
                break;
            case 3:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getLow));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getLow)));
                }
                break;
            case 4:
                if (m_sortDirection) {
                    Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getVolValue));
                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getVolValue)));
                }
                break;

        }

    }

    public void sortByChangeRate(boolean direction) {
        m_sortMethod = 0;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h Change Rate ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : "(High to Low)"));
    }

    public void sortByChangePrice(boolean direction) {
        m_sortMethod = 1;
        m_sortDirection = direction;

        String msg = "Top 100 - 24h Price Change ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByHigh(boolean direction) {
        m_sortMethod = 2;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h High Price ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByLow(boolean direction) {
        m_sortMethod = 3;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h Low Price ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByVolValue(boolean direction) {
        m_sortMethod = 4;
        m_sortDirection = direction;
        String msg = "Top 100 - 24h Volume ";
        m_statusMsg.set(msg + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void setSearchText(String text) {
        m_searchText = text.equals("") ? null : text;

        updateGridBox();

    }

    public String getSearchText() {
        return m_searchText;
    }

    private void doSearch(ArrayList<KucoinMarketItem> marketItems, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<KucoinMarketItem[]> task = new Task<KucoinMarketItem[]>() {
            @Override
            public KucoinMarketItem[] call() {
                List<KucoinMarketItem> searchResultsList = marketItems.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());

                KucoinMarketItem[] results = new KucoinMarketItem[searchResultsList.size()];

                searchResultsList.toArray(results);

                return results;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void updateGridBox() {
        m_favoriteGridBox.getChildren().clear();
        m_gridBox.getChildren().clear();

        int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();

        if (numCells > 0) {
            int numFavorites = m_favoritesList.size();

            for (int i = 0; i < numFavorites; i++) {

                m_favoriteGridBox.getChildren().add(m_favoritesList.get(i));
            }
            if (m_searchText == null) {
                for (int i = 0; i < numCells; i++) {
                    KucoinMarketItem marketItem = m_marketsList.get(i);

                    HBox rowBox = marketItem.getRowBox();

                    m_gridBox.getChildren().add(rowBox);

                }
            } else {

                // List<KucoinMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());
                doSearch(m_marketsList, onSuccess -> {
                    WorkerStateEvent event = onSuccess;
                    Object sourceObject = event.getSource().getValue();

                    if (sourceObject instanceof KucoinMarketItem[]) {
                        KucoinMarketItem[] searchResults = (KucoinMarketItem[]) sourceObject;
                        int numResults = searchResults.length > 100 ? 100 : searchResults.length;

                        for (int i = 0; i < numResults; i++) {
                            KucoinMarketItem marketItem = searchResults[i];

                            HBox rowBox = marketItem.getRowBox();

                            m_gridBox.getChildren().add(rowBox);
                        }
                    }
                }, onFailed -> {
                });

            }
        } else {
            HBox imageBox = new HBox();
            imageBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(imageBox, Priority.ALWAYS);

            if (m_notConnected) {
                Button notConnectedBtn = new Button("No Connection");
                notConnectedBtn.setFont(App.txtFont);
                notConnectedBtn.setTextFill(App.txtColor);
                notConnectedBtn.setId("menuBtn");
                notConnectedBtn.setGraphicTextGap(15);
                notConnectedBtn.setGraphic(IconButton.getIconView(new Image("/assets/cloud-offline-150.png"), 150));
                notConnectedBtn.setContentDisplay(ContentDisplay.TOP);
                notConnectedBtn.setOnAction(e -> {
                    updateTickers();
                });

            } else {
                Button loadingBtn = new Button("Loading...");
                loadingBtn.setFont(App.txtFont);
                loadingBtn.setTextFill(Color.WHITE);
                loadingBtn.setId("transparentColor");
                loadingBtn.setGraphicTextGap(15);
                loadingBtn.setGraphic(IconButton.getIconView(new Image("/assets/kucoin-100.png"), 150));
                loadingBtn.setContentDisplay(ContentDisplay.TOP);

                imageBox.getChildren().add(loadingBtn);

            }
            m_gridBox.getChildren().add(imageBox);
        }
    }

    public JsonArray getFavoritesJsonArray() {
        JsonArray jsonArray = new JsonArray();
        for (HBox rowBox : m_favoritesList) {
            Object rowBoxObject = rowBox.getUserData();

            if (rowBoxObject != null && rowBoxObject instanceof String) {
                jsonArray.add((String) rowBoxObject);
            }
        }
        return jsonArray;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();

        jsonObject.add("favorites", getFavoritesJsonArray());
        jsonObject.addProperty("sortMethod", m_sortMethod);
        jsonObject.addProperty("sortDirection", m_sortDirection);

        return jsonObject;
    }

    private void openJson(JsonObject json) {
        if (json != null) {
            JsonElement favoritesElement = json.get("favorites");
            JsonElement sortMethodElement = json.get("sortMethod");
            JsonElement sortDirectionElement = json.get("sortDirection");

            if (favoritesElement != null && favoritesElement.isJsonArray()) {
                JsonArray favoriteJsonArray = favoritesElement.getAsJsonArray();

                for (int i = 0; i < favoriteJsonArray.size(); i++) {
                    JsonElement favoriteElement = favoriteJsonArray.get(i);
                    String symbol = favoriteElement.getAsString();
                    addFavorite(symbol, false);
                    KucoinMarketItem item = getMarketItem(symbol);
                    item.isFavoriteProperty().set(true);
                }
            }

            m_sortDirection = sortDirectionElement != null && sortDirectionElement.isJsonPrimitive() ? sortDirectionElement.getAsBoolean() : m_sortDirection;
            m_sortMethod = sortMethodElement != null && sortMethodElement.isJsonPrimitive() ? sortMethodElement.getAsInt() : m_sortMethod;

        }
    }

    public KucoinExchange getKucoinExchange() {
        return m_kucoinExchange;
    }

    public void save() {

        try {
            Utils.saveJson(m_kucoinExchange.getNetworksData().getAppKey(), getJsonObject(), m_kucoinExchange.getDataFile());
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nsave failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

}
