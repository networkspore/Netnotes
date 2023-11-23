package com.netnotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import org.ergoplatform.appkit.NetworkType;

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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
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

    File logFile = new File("netnotes-log.txt");

    public final static String PUBLIC = "PUBLIC";
    public final static String PRIVATE = "PRIVATE";

    public final static String LIGHT_CLIENT = "Remote Node";
    public final static String NODE_INSTALLER = "Node Installer";
    public final static String LOCAL_NODE = "Local Node";

    public final SimpleObjectProperty< NamedNodeUrl> namedNodeUrlProperty = new SimpleObjectProperty<>();

    private String m_imgUrl = "/assets/ergoNodes-30.png";

    private String m_radioOffUrl = "/assets/radio-button-off-30.png";
    private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
    private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

    private Color m_secondaryColor = new Color(.4, .4, .4, .9);
    private Color m_primaryColor = new Color(.7, .7, .7, .9); 

    private String m_startImgUrl = "/assets/play-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

    public final SimpleBooleanProperty isSetupProperty = new SimpleBooleanProperty(false);
    public final SimpleStringProperty statusProperty = new SimpleStringProperty(ErgoMarketsData.STOPPED);
    public final SimpleStringProperty statusString = new SimpleStringProperty("");
    public final SimpleObjectProperty<LocalDateTime> shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
    public final SimpleStringProperty cmdProperty = new SimpleStringProperty("");
    public final SimpleStringProperty cmdStatusUpdated = new SimpleStringProperty(String.format("%29s", Utils.formatDateTimeString(LocalDateTime.now())));
    public final SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);

    private ChangeListener<LocalDateTime> m_updateListener = null;

    //  public SimpleStringProperty nodeApiAddress;
    private ErgoNodesList m_ergoNodesList;

    private String m_clientType = LIGHT_CLIENT;

    public final SimpleBooleanProperty availableProperty = new SimpleBooleanProperty(false);

    public ErgoNodeData(ErgoNodesList nodesList, JsonObject jsonObj) {
        m_ergoNodesList = nodesList;

        openJson(jsonObj);

    }

    public ErgoNodeData(ErgoNodesList ergoNodesList, String clientType, NamedNodeUrl namedNodeUrl) {
        m_ergoNodesList = ergoNodesList;
        m_clientType = clientType;

        namedNodeUrlProperty.set(namedNodeUrl == null ? new NamedNodeUrl() : namedNodeUrl);

    }

    public String getId() {
        return namedNodeUrlProperty.get().getId();
    }

    public String getClientType() {
        return m_clientType;
    }

    public String getName() {
        return namedNodeUrlProperty.get() == null ? "INVALID NODE" : namedNodeUrlProperty.get().getName();
    }

    public NetworkType getNetworkType() {
        return namedNodeUrlProperty.get() == null ? null : namedNodeUrlProperty.get().getNetworkType();
    }

    public void openJson(JsonObject jsonObj) {

        JsonElement namedNodeElement = jsonObj == null ? null : jsonObj.get("namedNode");

        namedNodeUrlProperty.set(namedNodeElement != null && namedNodeElement.isJsonObject() ? new NamedNodeUrl(namedNodeElement.getAsJsonObject()) : new NamedNodeUrl());

    }

    public Image getIcon() {
        return new Image(m_imgUrl == null ? "/assets/ergoNodes-30.png" : m_imgUrl);
    }

    public JsonObject getJsonObject() {
        NamedNodeUrl namedNodeUrl = namedNodeUrlProperty.get();

        JsonObject json = new JsonObject();

        if (namedNodeUrl != null) {
            json.add("namedNode", namedNodeUrl.getJsonObject());
        }

        return json;

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

    public void stop() {

    }

    public HBox getStatusBox(){
      
    
        String centerString = "";

        Text topInfoStringText = new Text((namedNodeUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_primaryColor);

        Text topRightText = new Text(getClientType());
        topRightText.setFont(m_smallFont);
        topRightText.setFill(m_secondaryColor);

        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_secondaryColor);
        botTimeText.textProperty().bind(cmdStatusUpdated);
        
        Text centerField = new Text(centerString);
        centerField.setFont(App.txtFont);
        centerField.setFill(App.txtColor);
       // centerField.setEditable(false);

       // centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(statusString);

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(m_font);
        middleTopRightText.setFill(m_secondaryColor);

        middleTopRightText.textProperty().bind(cmdProperty);

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(m_font);
        middleBottomRightText.setFill(m_primaryColor);

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerFieldBox = new HBox(centerField);
        centerFieldBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(centerFieldBox, Priority.ALWAYS);

        HBox centerBox = new HBox(centerFieldBox, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_LEFT);
      //  centerBox.setId("darkBox");

       // centerField.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerRightBox.widthProperty()).subtract(20));

        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
      //  topSpacer.setId("bodyBox");
      //  bottomSpacer.setId("bodyBox");

        Region topMiddleRegion = new Region();
        HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

        HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
     //   topBox.setId("darkBox");

        Text ipText = new Text(namedNodeUrlProperty.get() != null ? (namedNodeUrlProperty.get().getUrlString() == null ? "IP INVALID" : namedNodeUrlProperty.get().getUrlString()) : "Configure node");
        ipText.setFill(m_primaryColor);
        ipText.setFont(m_smallFont);

        Region bottomMiddleRegion = new Region();
        HBox.setHgrow(bottomMiddleRegion, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, bottomMiddleRegion, botTimeText);
      //  bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox contentsBox = new HBox( bodyBox);
       // contentsBox.setId("rowBox");
        HBox.setHgrow(contentsBox, Priority.ALWAYS);

        contentsBox.setMouseTransparent(true);
        start();
        contentsBox.setId("blackBox");
        return contentsBox;
    }

    public HBox getRowItem() {

       
         Tooltip defaultIdTip = new Tooltip(getErgoNodesList().defaultNodeIdProperty().get() != null && getErgoNodesList().defaultNodeIdProperty().get().equals(getId()) ? "Default Node" : "Set default");
        defaultIdTip.setShowDelay(new Duration(100));
        BufferedButton defaultIdBtn = new BufferedButton(m_ergoNodesList.defaultNodeIdProperty().get() != null && m_ergoNodesList.defaultNodeIdProperty().get().equals(getId()) ? m_radioOnUrl : m_radioOffUrl, 15);
        defaultIdBtn.setTooltip(defaultIdTip);
        defaultIdBtn.setOnAction(e->{
            String currentDefaultId = m_ergoNodesList.defaultNodeIdProperty().get();
            if(currentDefaultId != null && currentDefaultId.equals(getId())){
                m_ergoNodesList.defaultNodeIdProperty().set(null);
            }else{
                m_ergoNodesList.defaultNodeIdProperty().set(getId());
            }
        });

        m_ergoNodesList.defaultNodeIdProperty().addListener((obs, oldval, newval)->{
            defaultIdBtn.setImage(new Image(newval != null && newval.equals(getId()) ? m_radioOnUrl : m_radioOffUrl));
             defaultIdTip.setText(newval != null && newval.equals(getId()) ? "Default Node" : "Set default");
        });

        String centerString = "";

        Text topInfoStringText = new Text((namedNodeUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
        topInfoStringText.setFont(m_font);
        topInfoStringText.setFill(m_primaryColor);

        Text topRightText = new Text(getClientType());
        topRightText.setFont(m_smallFont);
        topRightText.setFill(m_secondaryColor);

        Text botTimeText = new Text();
        botTimeText.setFont(m_smallFont);
        botTimeText.setFill(m_secondaryColor);
        botTimeText.textProperty().bind(cmdStatusUpdated);
        
        TextField centerField = new TextField(centerString);
        centerField.setFont(m_largeFont);
        centerField.setId("formField");
        centerField.setEditable(false);
        centerField.setAlignment(Pos.CENTER);
        centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(statusString);

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(m_font);
        middleTopRightText.setFill(m_secondaryColor);

        middleTopRightText.textProperty().bind(cmdProperty);

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(m_font);
        middleBottomRightText.setFill(m_primaryColor);

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip(statusProperty.get().equals(ErgoMarketsData.STOPPED) ? "Ping" : "Stop");
        statusBtnTip.setShowDelay(new Duration(100));
        BufferedButton statusBtn = new BufferedButton(statusProperty.get().equals(ErgoMarketsData.STOPPED) ? m_startImgUrl : m_stopImgUrl, 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (statusProperty.get().equals(ErgoMarketsData.STOPPED)) {
                start();
            } else {
                shutdownNow.set(LocalDateTime.now());
            }
        });

        statusProperty.addListener((obs, oldVal, newVal) -> {
            switch (statusProperty.get()) {
                case ErgoMarketsData.STOPPED:

                    statusBtnTip.setText("Ping");
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(m_startImgUrl), 15);
                    /*if (!availableProperty.get()) {
                        defaultIdBtn.setGraphic(IconButton.getIconView(new Image(getPowerOffUrl()), 15));
                    } else {
                        defaultIdBtn.setGraphic(IconButton.getIconView(new Image(getPowerOnUrl()), 15));
                    }*/

                    break;
                default:
                    if (!statusBtnTip.getText().equals("Stop")) {
                        statusBtnTip.setText("Stop");
                        statusBtn.getBufferedImageView().setDefaultImage(new Image(m_stopImgUrl), 15);
                        /*if (!availableProperty.get()) {
                            defaultIdBtn.setGraphic(IconButton.getIconView(new Image(getPowerInitUrl()), 15));
                        }*/
                    }
                    break;
            }
        });

        HBox leftBox = new HBox(defaultIdBtn);
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

        Text ipText = new Text(namedNodeUrlProperty.get() != null ? (namedNodeUrlProperty.get().getUrlString() == null ? "IP INVALID" : namedNodeUrlProperty.get().getUrlString()) : "Configure node");
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

        HBox contentsBox = new HBox(leftBox, bodyBox, rightBox);
     
        HBox.setHgrow(contentsBox, Priority.ALWAYS);

        HBox rowBox = new HBox(contentsBox);
        rowBox.setPadding(new Insets(0, 0, 5, 0));
        rowBox.setAlignment(Pos.CENTER_RIGHT);
       
        HBox.setHgrow(rowBox, Priority.ALWAYS);

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            Platform.runLater(() -> {
                getErgoNodesList().selectedIdProperty().set(getId());
                e.consume();
            });
        });

        Runnable updateSelected = () -> {
            String selectedId = m_ergoNodesList.selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            centerField.setId(isSelected ? "selectedField" : "formField");

            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

        m_ergoNodesList.selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());
        updateSelected.run();

        start();
        return rowBox;
    }

    public void start() {
        NamedNodeUrl namedNodeUrl = namedNodeUrlProperty.get();
        if (namedNodeUrl != null && namedNodeUrl.getIP() != null) {
            Runnable r = () -> {
                Platform.runLater(() -> statusProperty.set(ErgoMarketsData.STARTED));
                statusString.set("Pinging...");
                pingIP(namedNodeUrl.getIP());
                Platform.runLater(() -> statusProperty.set(ErgoMarketsData.STOPPED));
            };
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.start();
        }

    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_updateListener = changeListener;
        if (m_updateListener != null) {
            lastUpdated.addListener(m_updateListener);

        }
        // lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_updateListener != null) {
            lastUpdated.removeListener(m_updateListener);
            m_updateListener = null;
        }
    }

    public void openSettings() {

    }

    public HBox getMenuBar() {
        Tooltip settingsTip = new Tooltip("Settings");
        settingsTip.setShowDelay(new Duration(100));
        BufferedButton settingsBtn = new BufferedButton("/assets/settings-outline-white-120.png", 20);
        settingsBtn.setTooltip(settingsTip);

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        HBox menuBar = new HBox(menuSpacer, settingsBtn);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        settingsBtn.setOnAction(e -> openSettings());
        return menuBar;
    }

    public void pingIP(String ip) {
        cmdProperty.set("PING");
        String[] cmd = {"cmd", "/c", "ping", ip};

        try {

            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            List<String> javaOutputList = new ArrayList<String>();

            String s = null;

            while ((s = stdInput.readLine()) != null) {
                javaOutputList.add(s);

                String timeString = "time=";
                int indexOftimeString = s.indexOf(timeString);

                if (s.indexOf("timed out") > 0) {
                    availableProperty.set(false);
                    statusString.set("Timed out");
                    cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                }

                if (indexOftimeString > 0) {
                    int lengthOftime = timeString.length();

                    int indexOfms = s.indexOf("ms");

                    availableProperty.set(true);

                    String time = s.substring(indexOftimeString + lengthOftime, indexOfms + 2);

                    statusString.set("Ping: " + time);
                    cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                }

                String avgString = "Average = ";
                int indexOfAvgString = s.indexOf(avgString);

                if (indexOfAvgString > 0) {
                    int lengthOfAvg = avgString.length();

                    String avg = s.substring(indexOfAvgString + lengthOfAvg);

                    statusString.set("Average: " + avg);

                    cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));

                }

            }
            if (!availableProperty.get()) {
                statusString.set("Offline");
                cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));
                cmdProperty.set("");
            } else {
                Thread.sleep(1000);
                cmdProperty.set("");
                statusString.set("Online");

            }
            // String[] splitStr = javaOutputList.get(0).trim().split("\\s+");
            //Version jV = new Version(splitStr[1].replaceAll("/[^0-9.]/g", ""));
        } catch (Exception e) {
            cmdProperty.set("");
            cmdStatusUpdated.set(Utils.formatDateTimeString(LocalDateTime.now()));

        }

    }


  


}
