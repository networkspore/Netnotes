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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class KuCoinDataList extends Network implements NoteInterface {

    private File logFile = new File("kucoinChartsList-log.txt");
    private KucoinExchange m_kucoinExchange;

    private VBox m_gridBox = new VBox();

    private ArrayList<KucoinMarketItem> m_marketsList = new ArrayList<KucoinMarketItem>();
    private SimpleStringProperty m_statusMsg = new SimpleStringProperty("Loading...");

    public KuCoinDataList(KucoinExchange kuCoinCharts) {
        super(null, "Ergo Charts List", "KUCOIN_CHARTS_LIST", kuCoinCharts);
        m_kucoinExchange = kuCoinCharts;

        //  getFile();
        m_kucoinExchange.getAllTickers(success -> {
            Object sourceObject = success.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                readTickers(getDataJson((JsonObject) sourceObject));
                sortByChangeRate(false);
                updateGridBox();
                getLastUpdated().set(LocalDateTime.now());
            } else {
                m_statusMsg.set("Not connected");
            }
        }, failed -> {

            m_statusMsg.set("Not connected");
        });
    }

    public void closeAll() {

    }

    public SimpleStringProperty sortOrderProperty() {
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
                if (item.getId().equals(symbol)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void readTickers(JsonObject tickersJson) {

        //  JsonElement timeStampElement = tickersJson.get("time");
        JsonElement tickerElement = tickersJson.get("ticker");

        if (tickerElement != null && tickerElement.isJsonArray()) {

            JsonArray jsonArray = tickerElement.getAsJsonArray();

            int i = 0;

            for (i = 0; i < jsonArray.size(); i++) {
                try {
                    JsonElement tickerObjectElement = jsonArray.get(i);
                    if (tickerObjectElement != null && tickerObjectElement.isJsonObject()) {

                        JsonObject tickerJson = tickerObjectElement.getAsJsonObject();
                        JsonElement symbolElement = tickerJson.get("symbol");
                        if (symbolElement != null && symbolElement.isJsonPrimitive()) {
                            try {
                                Files.writeString(logFile.toPath(), "\n" + i + ": item " + symbolElement.getAsString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {

                            }
                            String symbolString = symbolElement.getAsString();

                            KucoinTickerData tickerData = new KucoinTickerData(symbolString, tickerJson);

                            KucoinMarketItem oldMarketItem = getMarketItem(symbolString);

                            if (oldMarketItem == null) {
                                m_marketsList.add(new KucoinMarketItem(symbolString, symbolString, tickerData, m_kucoinExchange));
                            } else {
                                oldMarketItem.tickerDataProperty().set(tickerData);
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

    }

    private void getFile() {
        JsonObject json = null;

        File dataFile = m_kucoinExchange.getDataFile();
        if (dataFile != null && dataFile.isFile()) {
            try {
                json = Utils.readJsonFile(getNetworksData().getAppKey(), dataFile.toPath());
                //  openJson(json);
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

        updateGridBox();

        return m_gridBox;
    }

    public void sortByChangeRate(boolean direction) {

        if (direction) {
            Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getChangeRate));

        } else {
            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getChangeRate)));

        }
        String sortOrder = "Top 100 - 24h Change Rate ";
        m_statusMsg.set(sortOrder + (direction ? "(Low to High)" : "(High to Low)"));
    }

    public void sortByChangePrice(boolean direction) {
        if (direction) {
            Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getChangePrice));
        } else {
            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getChangePrice)));
        }
        String sortOrder = "Top 100 - 24h Price Change ";
        m_statusMsg.set(sortOrder + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByHigh(boolean direction) {
        if (direction) {
            Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getHigh));
        } else {
            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getHigh)));
        }
        String sortOrder = "Top 100 - 24h High Price ";
        m_statusMsg.set(sortOrder + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByLow(boolean direction) {
        if (direction) {
            Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getLow));
        } else {
            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getLow)));
        }
        String sortOrder = "Top 100 - 24h Low Price ";
        m_statusMsg.set(sortOrder + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void sortByVolValue(boolean direction) {
        if (direction) {
            Collections.sort(m_marketsList, Comparator.comparing(KucoinMarketItem::getVolValue));
        } else {
            Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(KucoinMarketItem::getVolValue)));
        }
        String sortOrder = "Top 100 - 24h Volume ";
        m_statusMsg.set(sortOrder + (direction ? "(Low to High)" : ("High to Low")));
    }

    public void updateGridBox() {
        if (m_gridBox != null) {
            int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();

            //  m_marketsList.sort();
            m_gridBox.getChildren().clear();

            for (int i = 0; i < numCells; i++) {
                KucoinMarketItem marketItem = m_marketsList.get(i);

                HBox rowBox = marketItem.getRowBox();

                m_gridBox.getChildren().add(rowBox);

            }
        }
    }

}
