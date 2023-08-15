package com.netnotes;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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

    public final static String PUBLIC = "PUBLIC";
    public final static String PRIVATE = "PRIVATE";

    public final static String LIGHT_CLIENT = "LIGHT_CLIENT";
    public final static String FULL_NODE = "FULL_NODE";

    private String m_id;
    private String m_name;
    private int m_exPort = ErgoNodes.EXTERNAL_PORT;
    private int m_port = ErgoNodes.MAINNET_PORT;
    private String m_apiKey = "";
    private String m_url = null;
    private NetworkType m_networkType;

    private String m_imgUrl = "/assets/ergoNodes-30.png";

    private String m_radioOffUrl = "/assets/radio-button-off-30.png";
    private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 30);
    private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
    private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    private Color m_priceColor = Color.WHITE;
    private Color m_secondaryColor = new Color(.4, .4, .4, .9); //base
    private Color m_primaryColor = new Color(.7, .7, .7, .9); //quote

    private String m_startImgUrl = "/assets/play-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

    private SimpleStringProperty m_statusProperty = new SimpleStringProperty(MarketsData.STOPPED);
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());

    //  public SimpleStringProperty nodeApiAddress;
    private ErgoNodesList m_ergoNodesList;

    public ErgoNodeData(ErgoNodesList nodesList, JsonObject jsonObj) {
        m_ergoNodesList = nodesList;

        openJson(jsonObj);

    }

    public ErgoNodeData(ErgoNodesList ergoNodesList, NetworkType networkType) {
        m_id = FriendlyId.createFriendlyId();
        m_name = "Node #" + m_id;
        m_networkType = networkType;
        m_ergoNodesList = ergoNodesList;

    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public String getId() {
        return m_id;
    }

    private void openJson(JsonObject jsonObj) {

        JsonElement networkIdElement = jsonObj == null ? null : jsonObj.get("id");
        JsonElement apiKeyElement = jsonObj == null ? null : jsonObj.get("apiKey");
        JsonElement urlElement = jsonObj == null ? null : jsonObj.get("url");
        JsonElement nameElement = jsonObj == null ? null : jsonObj.get("name");
        JsonElement networkTypeElement = jsonObj == null ? null : jsonObj.get("networkType");
        JsonElement portElement = jsonObj == null ? null : jsonObj.get("port");
        JsonElement exPortElement = jsonObj == null ? null : jsonObj.get("exPort");

        m_apiKey = apiKeyElement == null ? "" : apiKeyElement.getAsString();
        m_networkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.getAsString().equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : NetworkType.TESTNET;

        m_id = networkIdElement == null ? FriendlyId.createFriendlyId() : networkIdElement.getAsString();
        m_name = nameElement == null ? "Node #" + m_id : nameElement.getAsString();
        m_url = urlElement == null ? null : urlElement.getAsString();
        m_port = portElement == null ? ErgoNodes.MAINNET_PORT : portElement.getAsInt();
        m_exPort = exPortElement == null ? ErgoNodes.EXTERNAL_PORT : exPortElement.getAsInt();
    }

    /*
    public JsonObject getExplorerUrlObject() {

        JsonObject getExplorerUrlObject = new JsonObject();
        getExplorerUrlObject.addProperty("subject", "GET_EXPLORER_URL");
        getExplorerUrlObject.addProperty("networkId", m_networkId);
        getExplorerUrlObject.addProperty("networkType", m_networkType.toString());
        return getExplorerUrlObject;
    }

     public boolean getClient(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        NoteInterface explorerInterface = m_ergoNodes.getExplorerInterface();
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

    }*/
    public Image getIcon() {
        return new Image(m_imgUrl == null ? "/assets/ergoNodes-30.png" : m_imgUrl);
    }

    public String getName() {
        return m_name;
    }

    public String getUrl() {
        return m_url;
    }

    public int getPort() {
        return m_port;
    }

    public int getExternalPort() {
        return m_port;
    }

    public String getNetworkTypeString() {
        return m_networkType == null ? NetworkType.MAINNET.toString() : NetworkType.TESTNET.toString();
    }

    public JsonObject getJsonObject() {

        JsonObject json = new JsonObject();

        json.addProperty("port", m_port);
        json.addProperty("externalPort", m_exPort);
        json.addProperty("networkType", getNetworkTypeString());
        json.addProperty("name", m_name);
        json.addProperty("apiKey", m_apiKey);
        json.addProperty("id", m_id);
        return json;

    }

    public HBox getRowItem() {
        String initialDefaultId = m_ergoNodesList.defaultIdProperty().get();
        BufferedButton defaultBtn = new BufferedButton(initialDefaultId != null && m_id.equals(initialDefaultId) ? m_radioOnUrl : m_radioOffUrl, 15);

        m_ergoNodesList.defaultIdProperty().addListener((obs, oldval, newVal) -> {
            defaultBtn.getBufferedImageView().setDefaultImage(new Image(newVal != null && m_id.equals(newVal) ? m_radioOnUrl : m_radioOffUrl));
        });

        String valueString = "";
        String amountString = "";
        String baseCurrencyString = "";
        String quoteCurrencyString = "";

        Text topValueText = new Text(valueString);
        topValueText.setFont(m_font);
        topValueText.setFill(m_secondaryColor);

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

        HBox topBox = new HBox(topValueText);
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

    }

}
