package com.netnotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
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

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class ErgoNodeData {

    File logFile = new File("ergoNodeData-log.txt");

    public final static String PUBLIC = "PUBLIC";
    public final static String PRIVATE = "PRIVATE";

    public final static String LIGHT_CLIENT = "LIGHT_CLIENT";
    public final static String FULL_NODE = "FULL_NODE";

    private String m_id;
    private SimpleObjectProperty< NamedNodeUrl> m_namedNodeUrlProperty = new SimpleObjectProperty<>();

    private String m_imgUrl = "/assets/ergoNodes-30.png";

    private String m_radioOffUrl = "/assets/radio-button-off-30.png";
    private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
    private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    private Color m_secondaryColor = new Color(.4, .4, .4, .9); //base
    private Color m_primaryColor = new Color(.7, .7, .7, .9); //quote

    private String m_startImgUrl = "/assets/play-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

    private SimpleStringProperty m_statusProperty = new SimpleStringProperty(MarketsData.STOPPED);
    private SimpleStringProperty m_statusString = new SimpleStringProperty("");
    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
    private ChangeListener<LocalDateTime> m_updateListener = null;

    //  public SimpleStringProperty nodeApiAddress;
    private ErgoNodesList m_ergoNodesList;

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);
    private String m_clientType = LIGHT_CLIENT;

    private SimpleStringProperty m_cmdProperty = new SimpleStringProperty("");
    private SimpleStringProperty m_cmdStatusUpdated = new SimpleStringProperty("");

    public ErgoNodeData(ErgoNodesList nodesList, JsonObject jsonObj) {
        m_ergoNodesList = nodesList;

        openJson(jsonObj);

    }

    public ErgoNodeData(ErgoNodesList ergoNodesList, String clientType, NamedNodeUrl namedNodeUrl) {
        m_ergoNodesList = ergoNodesList;
        m_clientType = clientType;
        m_id = FriendlyId.createFriendlyId();
        m_namedNodeUrlProperty.set(namedNodeUrl == null ? new NamedNodeUrl() : namedNodeUrl);

    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
    }

    public String getName() {
        return m_namedNodeUrlProperty.get() == null ? "INVALID NODE" : m_namedNodeUrlProperty.get().getName();
    }

    public NetworkType getNetworkType() {
        return m_namedNodeUrlProperty.get() == null ? null : m_namedNodeUrlProperty.get().getNetworkType();
    }

    public SimpleObjectProperty< NamedNodeUrl> namedNodeUrlProperty() {
        return m_namedNodeUrlProperty;
    }

    public void openJson(JsonObject jsonObj) {

        JsonElement idElement = jsonObj == null ? null : jsonObj.get("id");
        JsonElement namedNodeElement = jsonObj == null ? null : jsonObj.get("namedNode");

        m_id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
        m_namedNodeUrlProperty.set(namedNodeElement != null && namedNodeElement.isJsonObject() ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : new NamedNodeUrl());

    }

    public Image getIcon() {
        return new Image(m_imgUrl == null ? "/assets/ergoNodes-30.png" : m_imgUrl);
    }

    public JsonObject getJsonObject() {
        NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();

        JsonObject json = new JsonObject();
        json.addProperty("id", m_id);
        if (namedNodeUrl != null) {
            json.add("namedNode", namedNodeUrl.getJsonObject());
        }

        return json;

    }

    public String getClientTypeName() {
        switch (m_clientType) {
            case LIGHT_CLIENT:
                return "Light client";

            case FULL_NODE:
                return "Full Node";

            default:
                return m_clientType;

        }
    }

    public String getNetworkTypeString() {
        return getNetworkType() != null ? getNetworkType().toString() : "NONE";
    }

    public ErgoNodesList getErgoNodesList() {
        return m_ergoNodesList;
    }

    public String getRadioOnUrl() {
        return m_radioOnUrl;
    }

    public String getRadioOffUrl() {
        return m_radioOffUrl;
    }

    public String getStopImgUrl() {
        return m_stopImgUrl;
    }

    public String getStartImgUrl() {
        return m_startImgUrl;
    }

    public Font getFont() {
        return m_font;
    }

    public Font getSmallFont() {
        return m_smallFont;
    }

    public Font getLargeFont() {
        return m_largeFont;
    }

    public Color getPrimaryColor() {
        return m_primaryColor;
    }

    public Color getSecondaryColor() {
        return m_secondaryColor;
    }

    public SimpleStringProperty cmdStatusUpdatedProperty() {
        return m_cmdStatusUpdated;
    }

    public SimpleStringProperty statusStringProperty() {
        return m_statusString;
    }

    public SimpleStringProperty statusProperty() {
        return m_statusProperty;
    }

    public SimpleStringProperty cmdProperty() {
        return m_cmdProperty;
    }

    public SimpleObjectProperty<LocalDateTime> shutdownNowProperty() {
        return m_shutdownNow;
    }

    public HBox getRowItem() {
        String initialDefaultId = m_ergoNodesList.defaultIdProperty().get();
        boolean isDefault = initialDefaultId != null && getId().equals(initialDefaultId);

        BufferedButton defaultBtn = new BufferedButton(isDefault ? m_radioOnUrl : m_radioOffUrl, 15);
        defaultBtn.setOnAction(e -> {
            getErgoNodesList().setDefaultId(getId());
        });

        String centerString = "";

        Text topInfoStringText = new Text((m_namedNodeUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_primaryColor);

        Text topRightText = new Text(getClientTypeName());
        topRightText.setFont(m_smallFont);
        topRightText.setFill(m_secondaryColor);

        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_secondaryColor);
        botTimeText.textProperty().bind(m_cmdStatusUpdated);
        TextField centerField = new TextField(centerString);
        centerField.setFont(m_largeFont);
        centerField.setId(isDefault ? "textField" : "formField");
        centerField.setEditable(false);
        centerField.setAlignment(Pos.CENTER);
        centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(m_statusString);

        m_ergoNodesList.defaultIdProperty().addListener((obs, oldval, newVal) -> {
            defaultBtn.getBufferedImageView().setDefaultImage(new Image(newVal != null && getId().equals(newVal) ? m_radioOnUrl : m_radioOffUrl));
            centerField.setId(newVal != null && getId().equals(newVal) ? "textField" : "formField");
        });

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(m_font);
        middleTopRightText.setFill(m_secondaryColor);

        middleTopRightText.textProperty().bind(m_cmdProperty);

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(m_font);
        middleBottomRightText.setFill(m_primaryColor);

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip(m_statusProperty.get().equals(MarketsData.STOPPED) ? "Ping" : "Stop");
        statusBtnTip.setShowDelay(new Duration(100));
        BufferedButton statusBtn = new BufferedButton(m_statusProperty.get().equals(MarketsData.STOPPED) ? m_startImgUrl : m_stopImgUrl, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
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
                    statusBtnTip.setText("Ping");
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_startImgUrl), 15);
                    break;
                default:
                    statusBtnTip.setText("Stop");
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

        HBox centerBox = new HBox(centerField, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        centerBox.setId("darkBox");

        centerField.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerRightBox.widthProperty()).subtract(20));

        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        topSpacer.setId("bodyBox");
        bottomSpacer.setId("bodyBox");

        Region topMiddleRegion = new Region();
        HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

        HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
        topBox.setId("darkBox");

        Text ipText = new Text(m_namedNodeUrlProperty.get() != null ? (m_namedNodeUrlProperty.get().getIP() == null ? "IP INVALID" : m_namedNodeUrlProperty.get().getIP()) : "Configure node");
        ipText.setFill(m_primaryColor);
        ipText.setFont(m_smallFont);

        Region bottomMiddleRegion = new Region();
        HBox.setHgrow(bottomMiddleRegion, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, bottomMiddleRegion, botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox rowBox = new HBox(leftBox, bodyBox, rightBox);
        rowBox.setPadding(new Insets(0, 0, 5, 0));
        rowBox.setAlignment(Pos.CENTER_RIGHT);
        rowBox.setId("rowBox");

        start();
        return rowBox;
    }

    public void start() {
        NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();
        if (namedNodeUrl != null && namedNodeUrl.getIP() != null) {
            Runnable r = () -> {
                Platform.runLater(() -> m_statusProperty.set(MarketsData.STARTED));
                m_statusString.set("Pinging...");
                pingIP(namedNodeUrl.getIP());
                Platform.runLater(() -> m_statusProperty.set(MarketsData.STOPPED));
            };
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.start();
        }

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

    public void pingIP(String ip) {
        m_cmdProperty.set("PING");
        String[] cmd = {"cmd", "/c", "ping", ip};

        try {

            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            List<String> javaOutputList = new ArrayList<String>();

            String s = null;
            SimpleBooleanProperty replyProperty = new SimpleBooleanProperty(false);

            while ((s = stdInput.readLine()) != null) {
                javaOutputList.add(s);

                String timeString = "time=";
                int indexOftimeString = s.indexOf(timeString);

                if (s.indexOf("timed out") > 0) {

                    m_statusString.set("Timed out");
                    m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                }

                if (indexOftimeString > 0) {
                    int lengthOftime = timeString.length();

                    int indexOfms = s.indexOf("ms");
                    replyProperty.set(true);

                    String time = s.substring(indexOftimeString + lengthOftime, indexOfms + 2);

                    m_statusString.set("Ping: " + time);
                    m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                }

                String avgString = "Average = ";
                int indexOfAvgString = s.indexOf(avgString);

                if (indexOfAvgString > 0) {
                    int lengthOfAvg = avgString.length();

                    String avg = s.substring(indexOfAvgString + lengthOfAvg);

                    m_statusString.set("Average: " + avg);

                    m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));

                }

            }
            if (!replyProperty.get()) {
                m_statusString.set("Offline");
                m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                m_cmdProperty.set("");
            } else {
                Thread.sleep(1000);
                m_cmdProperty.set("");
                m_statusString.set("Online");
            }
            // String[] splitStr = javaOutputList.get(0).trim().split("\\s+");
            //Version jV = new Version(splitStr[1].replaceAll("/[^0-9.]/g", ""));
        } catch (Exception e) {
            m_cmdProperty.set("");
            m_cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));

        }

    }

}
