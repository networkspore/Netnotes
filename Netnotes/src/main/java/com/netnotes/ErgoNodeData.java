package com.netnotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ErgoNodeData {

    File logFile = new File("ergoNodeData-log.txt");

    public final static String PUBLIC = "PUBLIC";
    public final static String PRIVATE = "PRIVATE";

    public final static String LIGHT_CLIENT = "LIGHT_CLIENT";
    public final static String FULL_NODE = "FULL_NODE";

    private String m_id;
    private NamedNodeUrl m_namedNodeUrl;

    private String m_imgUrl = "/assets/ergoNodes-30.png";

    private String m_radioOffUrl = "/assets/radio-button-off-30.png";
    private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 30);
    private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
    private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    private Color m_secondaryColor = new Color(.4, .4, .4, .9); //base
    private Color m_primaryColor = new Color(.7, .7, .7, .9); //quote

    private String m_startImgUrl = "/assets/play-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

    private SimpleStringProperty m_statusProperty = new SimpleStringProperty(MarketsData.STOPPED);
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
    private ChangeListener<LocalDateTime> m_updateListener = null;

    //  public SimpleStringProperty nodeApiAddress;
    private ErgoNodesList m_ergoNodesList;

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);
    private String m_clientType = LIGHT_CLIENT;

    public ErgoNodeData(ErgoNodesList nodesList, JsonObject jsonObj) {
        m_ergoNodesList = nodesList;

        openJson(jsonObj);

    }

    public ErgoNodeData(ErgoNodesList ergoNodesList, String clientType, NamedNodeUrl namedNodeUrl) {
        m_ergoNodesList = ergoNodesList;
        m_clientType = clientType;
        m_id = FriendlyId.createFriendlyId();
        m_namedNodeUrl = namedNodeUrl == null ? new NamedNodeUrl() : namedNodeUrl;

    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_namedNodeUrl == null ? "INVALID NODE" : m_namedNodeUrl.getName();
    }

    public NamedNodeUrl getNamedNodeUrl() {
        return m_namedNodeUrl;
    }

    private void openJson(JsonObject jsonObj) {

        JsonElement idElement = jsonObj == null ? null : jsonObj.get("id");
        JsonElement namedNodeElement = jsonObj == null ? null : jsonObj.get("namedNode");

        m_id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
        m_namedNodeUrl = namedNodeElement != null && namedNodeElement.isJsonObject() ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : new NamedNodeUrl();

    }

    public Image getIcon() {
        return new Image(m_imgUrl == null ? "/assets/ergoNodes-30.png" : m_imgUrl);
    }

    public JsonObject getJsonObject() {

        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        json.add("namedNode", m_namedNodeUrl.getJsonObject());

        return json;

    }

    public HBox getRowItem() {
        String initialDefaultId = m_ergoNodesList.defaultIdProperty().get();
        BufferedButton defaultBtn = new BufferedButton(initialDefaultId != null && m_id.equals(initialDefaultId) ? m_radioOnUrl : m_radioOffUrl, 15);

        m_ergoNodesList.defaultIdProperty().addListener((obs, oldval, newVal) -> {
            defaultBtn.getBufferedImageView().setDefaultImage(new Image(newVal != null && m_id.equals(newVal) ? m_radioOnUrl : m_radioOffUrl));
        });

        String topInfoString = "Pinging...";
        String amountString = m_namedNodeUrl.getIP();
        String baseCurrencyString = "";
        String quoteCurrencyString = "";

        Text topInfoStringText = new Text(topInfoString);
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_secondaryColor);

        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_secondaryColor);

        TextField amountField = new TextField(amountString);
        amountField.setFont(m_largeFont);
        amountField.setId("textField");
        amountField.setEditable(false);
        amountField.setAlignment(Pos.CENTER_RIGHT);
        amountField.setPadding(new Insets(0, 10, 0, 0));

        Text baseCurrencyText = new Text(baseCurrencyString);
        baseCurrencyText.setFont(m_font);
        baseCurrencyText.setFill(m_secondaryColor);

        Text quoteCurrencyText = new Text(quoteCurrencyString);
        quoteCurrencyText.setFont(m_font);
        quoteCurrencyText.setFill(m_primaryColor);

        VBox currencyBox = new VBox(baseCurrencyText, quoteCurrencyText);
        currencyBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(currencyBox, Priority.ALWAYS);

        BufferedButton statusBtn = new BufferedButton(m_statusProperty.get().equals(MarketsData.STOPPED) ? m_startImgUrl : m_stopImgUrl, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setOnAction(action -> {
            if (m_statusProperty.get().equals(MarketsData.STOPPED)) {
                start();
            } else {
                m_shutdownNow.set(LocalDateTime.now());
            }
        });

        m_statusProperty.addListener((obs, oldVal, newVal) -> {
            switch (m_statusProperty.get()) {
                case MarketsData.STOPPED:
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_startImgUrl), 15);
                    break;
                default:
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_stopImgUrl), 15);
                    break;
            }
        });

        HBox leftBox = new HBox(defaultBtn);
        HBox rightBox = new HBox(statusBtn);

        leftBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        leftBox.setId("bodyBox");
        rightBox.setId("bodyBox");

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerBox = new HBox(amountField, currencyBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        centerBox.setId("darkBox");

        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        topSpacer.setId("bodyBox");
        bottomSpacer.setId("bodyBox");

        HBox topBox = new HBox(topInfoStringText);
        topBox.setId("darkBox");

        HBox bottomBox = new HBox(botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox rowBox = new HBox(leftBox, bodyBox, rightBox);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setId("rowBox");

        // centerBox.prefWidthProperty().bind(rowBox.widthProperty().subtract(leftBox.widthProperty()).subtract(rightBox.widthProperty()));
        start();
        return rowBox;
    }

    private void start() {
        pingIP(m_namedNodeUrl.getIP(), m_namedNodeUrl.getPort());
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_lastUpdated;
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_updateListener = changeListener;
        if (m_updateListener != null) {
            m_lastUpdated.addListener(m_updateListener);

        }
        // m_lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_updateListener != null) {
            m_lastUpdated.removeListener(m_updateListener);
            m_updateListener = null;
        }
    }

    public boolean pingIP(String ip, int port) {

        String[] cmd = {"cmd", "/c", "ping", ip + ":" + port};
//
        try {
            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            List<String> javaOutputList = new ArrayList<String>();

            String s = null;
            while ((s = stdInput.readLine()) != null) {
                javaOutputList.add(s);
                try {
                    Utils.writeString(logFile.toPath(), s, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            }

            String[] splitStr = javaOutputList.get(0).trim().split("\\s+");
            //Version jV = new Version(splitStr[1].replaceAll("/[^0-9.]/g", ""));

            return javaOutputList.size() > 0;

        } catch (Exception e) {
            return false;
        }

    }

}
