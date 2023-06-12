package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.bouncycastle.math.ec.rfc7748.X25519.Friend;
import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class WalletData extends Network implements NoteInterface {

    private File logFile;
    private File m_walletFile = null;
    private File m_walletDirectory;

    private NetworkType m_networkType = NetworkType.MAINNET;
    private Stage m_walletStage = null;
    private Stage m_passwordStage = null;
    // private String m_name;

    private String m_nodeId;
    private String m_explorerId;
    private String m_marketId;
    private String m_timerId;

    private ArrayList<JsonObject> m_timerList = new ArrayList<>();
    private TimersList m_availableTimers = new TimersList();

    // private ErgoWallet m_ergoWallet;
    public WalletData(String id, String name, File walletFile, String nodeId, String explorerId, String marketId, JsonObject timersObject, NetworkType networkType, ErgoWallet ergoWallet) {
        super(null, name, id, ergoWallet);
        //   m_name = name;
        //  m_ergoWallet = ergoWallet;

        logFile = new File("WalletData" + name + "-log.txt");

        try {
            Files.writeString(logFile.toPath(), "ExchangeId: " + marketId + " nodeId: " + nodeId + " explorerId: " + explorerId + " networkType: " + networkType + " timers:" + (timersObject != null ? "\n" + timersObject.toString() : "null"));
        } catch (IOException e) {

        }
        m_walletFile = walletFile;
        m_networkType = networkType;

        if (timersObject == null) {
            m_timerId = null;
        } else {
            JsonElement timerNetworkIdElement = timersObject.get("networkId");
            m_timerId = timerNetworkIdElement == null ? null : timerNetworkIdElement.getAsString();
            if (m_timerId != null) {
                JsonElement timersElement = timersObject.get("timersData");
                if (timersElement != null && timersElement.isJsonArray()) {
                    JsonArray timersArr = timersElement.getAsJsonArray();

                    for (int i = 0; i < timersArr.size(); i++) {
                        JsonElement arrayElement = timersArr.get(i);
                        if (arrayElement.isJsonObject()) {
                            JsonObject timerObject = arrayElement.getAsJsonObject();

                            JsonElement timerIdElement = timerObject.get("timerId");
                            JsonElement timerNameElement = timerObject.get("name");
                            JsonElement timerIntervalElement = timerObject.get("interval");
                            JsonElement timerTimeUnitElement = timerObject.get("timeUnit");

                            if (timerIdElement != null && timerIdElement.isJsonPrimitive() && timerNameElement != null && timerNameElement.isJsonPrimitive() && timerIntervalElement != null && timerIntervalElement.isJsonPrimitive() && timerTimeUnitElement != null && timerTimeUnitElement.isJsonPrimitive()) {
                                try {
                                    timerIdElement.getAsString();
                                    timerIntervalElement.getAsLong();
                                    timerTimeUnitElement.getAsString();

                                    m_timerList.add(timerObject);
                                } catch (ClassCastException e) {

                                }

                            }
                        }
                    }
                }
                NoteInterface timerInterface = getNetworksData().getNoteInterface(m_timerId);
                if (timerInterface != null) {
                    timerInterface.sendNote(getTimers(), null, null);
                }
            }

        }

        m_nodeId = nodeId == null ? null : nodeId;
        m_explorerId = explorerId == null ? null : explorerId;
        m_marketId = marketId == null ? null : marketId;

        setIconStyle(IconStyle.ROW);

    }

    private void subscribeTimers() {
        if (m_timerId != null && m_timerList.size() > 0) {
            NoteInterface timerInterface = getNetworksData().getNoteInterface(m_timerId);

            if (timerInterface != null) {
                for (JsonObject timer : m_timerList) {
                    JsonObject subscribeJson = new JsonObject();

                    JsonElement timerIdElement = timer.get("timerId");

                    if (timerIdElement != null) {
                        String timerId = timerIdElement.getAsString();

                        subscribeJson.addProperty("subject", "SUBSCRIBE");
                        subscribeJson.addProperty("fullNetworkId", getFullNetworkId());
                        subscribeJson.addProperty("timerId", timerId);

                        if (!timerInterface.sendNote(subscribeJson, null, null)) {

                            try {
                                Files.writeString(logFile.toPath(), "\nFailed to subscribe:\n" + subscribeJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {

                            }

                        }
                    } else {
                        try {
                            Files.writeString(logFile.toPath(), "\nFailed to subscribe:\n" + "timerId null", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }
                    }
                }
            }
        }
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("id", getNetworkId());
        jsonObject.addProperty("file", m_walletFile.getAbsolutePath());
        jsonObject.addProperty("networkType", m_networkType.toString());

        if (m_nodeId != null) {
            jsonObject.addProperty("nodeId", m_nodeId);
        }
        if (m_explorerId != null) {
            jsonObject.addProperty("explorerId", m_explorerId);
        }
        if (m_marketId != null) {
            jsonObject.addProperty("marketId", m_marketId);
        }

        if (m_timerId != null) {
            JsonObject timersObject = new JsonObject();
            timersObject.addProperty("networkId", m_timerId);

            if (m_timerList.size() > 0) {
                timersObject.add("timersData", getTimersArray());
            }
            jsonObject.add("timers", timersObject);
        }

        /*jsonObject.set("name");
        jsonObject.get("id");
        jsonObject.get("file");
        jsonObject.get("networkType");
        jsonObject.get("nodeId");
        jsonObject.get("explorerId");
        jsonObject.get("exchangeId");*/
        return jsonObject;
    }

    private JsonArray getTimersArray() {
        JsonArray timersArray = new JsonArray();

        for (int i = 0; i < m_timerList.size(); i++) {
            timersArray.add(m_timerList.get(i));
        }

        return timersArray;
    }

    @Override
    public void open() {
        try {
            Files.writeString(logFile.toPath(), "\nwalletsData opening.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        openWallet();
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjectElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");

        String subject = subjectElement.getAsString();
        String networkId = networkIdElement.getAsString();

        switch (subject) {
            case "TIMERS":
                if (networkId.equals(m_timerId)) {
                    JsonElement availableTimersElement = note.get("availableTimers");
                    if (availableTimersElement != null && availableTimersElement.isJsonArray()) {
                        JsonArray timersArray = availableTimersElement.getAsJsonArray();
                        m_availableTimers.setAvailableTimers(timersArray);
                        return true;
                    }
                }

                break;
        }

        return false;
    }

    public void openWallet() {

        try {
            Files.writeString(logFile.toPath(), "\nConfirming wallet password.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        if (m_passwordStage == null) {
            m_passwordStage = new Stage();

            m_passwordStage.setResizable(false);
            m_passwordStage.initStyle(StageStyle.UNDECORATED);
            m_passwordStage.setTitle("Wallet file: Enter password");

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), getName() + " - Enter password", closeBtn, m_passwordStage);
            closeBtn.setOnAction(event -> {
                m_passwordStage.close();

            });
            Button imageButton = App.createImageButton(ErgoWallet.getAppIcon(), "Wallet");
            imageButton.setGraphicTextGap(10);
            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);

            Text passwordTxt = new Text("> Enter password:");
            passwordTxt.setFill(App.txtColor);
            passwordTxt.setFont(App.txtFont);

            PasswordField passwordField = new PasswordField();
            passwordField.setFont(App.txtFont);
            passwordField.setId("passField");
            HBox.setHgrow(passwordField, Priority.ALWAYS);

            Platform.runLater(() -> passwordField.requestFocus());

            HBox passwordBox = new HBox(passwordTxt, passwordField);
            passwordBox.setAlignment(Pos.CENTER_LEFT);

            Button clickRegion = new Button();
            clickRegion.setPrefWidth(Double.MAX_VALUE);
            clickRegion.setId("transparentColor");
            clickRegion.setPrefHeight(40);

            clickRegion.setOnAction(e -> {
                passwordField.requestFocus();

            });

            VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

            VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene passwordScene = new Scene(layoutVBox, 650, 375);

            passwordScene.getStylesheets().add("/css/startWindow.css");
            m_passwordStage.setScene(passwordScene);

            passwordField.setOnKeyPressed(e -> {

                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.ENTER) {

                    try {

                        Wallet wallet = Wallet.load(m_walletFile.toPath(), passwordField.getText());
                        showWalletStage(wallet);

                        m_passwordStage.close();
                        m_passwordStage = null;
                    } catch (Exception e1) {

                        passwordField.setText("");
                        try {
                            Files.writeString(logFile.toPath(), e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e2) {

                        }
                    }

                }
            });

            m_passwordStage.show();
        } else {
            m_passwordStage.show();
        }

    }

    private void showWalletStage(Wallet wallet) {
        if (m_walletStage == null) {
            AddressesData addressesData = new AddressesData(FriendlyId.createFriendlyId(), wallet, this, m_networkType);
            addTunnelNoteInterface(addressesData);
            subscribeTimers();
            try {
                Files.writeString(logFile.toPath(), "\nshowing wallet stage", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            String title = getName() + " - (" + m_networkType.toString() + ")";

            double sceneWidth = 450;
            double sceneHeight = 525;
            double imageWidth = 25;
            double alertImageWidth = 75;

            //  PriceChart priceChart = null;
            m_walletStage = new Stage();
            m_walletStage.setTitle(title);
            m_walletStage.getIcons().add(ErgoWallet.getAppIcon());
            m_walletStage.setResizable(false);
            m_walletStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {

                m_walletStage.close();
                m_walletStage = null;
            });

            HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), title, closeBtn, m_walletStage);

            /*Button imageButton = createImageButton(walletImg240, title + "\n" + wallet.name.get());
        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);*/
 /*  ImageView addImage = App.highlightedImageView(App.addImg);
            addImage.setFitHeight(10);
            addImage.setPreserveRatio(true);*/
 /*   Tooltip nameTip = new Tooltip("Wallet: " + getText());
            nameTip.setShowDelay(new javafx.util.Duration(100));
            nameTip.setFont(App.txtFont);
           
            Button walletButton = new Button();
            walletButton.setGraphic(IconButton.getIconView(new Image("/assets/ergo-wallet-30.png"), 30));
            walletButton.setId("menuBtn");
            walletButton.setTooltip(nameTip); */
            Tooltip addTip = new Tooltip("Add address");
            addTip.setShowDelay(new javafx.util.Duration(100));
            addTip.setFont(App.txtFont);

            Button addButton = new Button();
            addButton.setGraphic(IconButton.getIconView(new Image("/assets/git-branch-outline-white-30.png"), 30));
            addButton.setId("menuBtn");
            addButton.setTooltip(addTip);

            Tooltip networkTip = new Tooltip("Select network");
            networkTip.setShowDelay(new javafx.util.Duration(100));
            networkTip.setFont(App.txtFont);

            ImageView nodeView = IconButton.getIconView(new Image("/assets/node-30.png"), imageWidth);

            MenuButton networkMenuBtn = new MenuButton();
            networkMenuBtn.setGraphic(m_nodeId == null ? nodeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_nodeId, true).getIcon(), imageWidth));
            networkMenuBtn.setPadding(new Insets(2, 0, 0, 0));
            networkMenuBtn.setTooltip(networkTip);

            MenuItem nodeNullMenuItem = new MenuItem("(none)");
            nodeNullMenuItem.setOnAction(e -> {
                removeNodeId();
                networkMenuBtn.setGraphic(nodeView);

            });

            MenuItem nodeMenuItem = new MenuItem(ErgoNetwork.NAME);
            nodeMenuItem.setOnAction(e -> {
                setNodeId(NetworkID.ERGO_NETWORK);

                networkMenuBtn.setGraphic(IconButton.getIconView(ErgoNetwork.getSmallAppIcon(), imageWidth));
                if (getNodeInterface() == null) {
                    Alert nodeAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + ErgoNetwork.NAME + "' to use this feature.", ButtonType.OK);
                    nodeAlert.setGraphic(IconButton.getIconView(ErgoNetwork.getAppIcon(), alertImageWidth));
                    nodeAlert.initOwner(m_walletStage);
                    nodeAlert.show();
                }
            });

            networkMenuBtn.getItems().addAll(nodeNullMenuItem, nodeMenuItem);

            Tooltip explorerUrlTip = new Tooltip("Select explorer");
            explorerUrlTip.setShowDelay(new javafx.util.Duration(100));
            explorerUrlTip.setFont(App.txtFont);

            ImageView searchView = IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), imageWidth);

            MenuButton explorerBtn = new MenuButton();
            explorerBtn.setGraphic(m_explorerId == null ? searchView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_explorerId, true).getIcon(), imageWidth));;
            explorerBtn.setPadding(new Insets(2, 0, 0, 0));
            explorerBtn.setTooltip(explorerUrlTip);

            MenuItem explorerNullMenuItem = new MenuItem("(none)");
            explorerNullMenuItem.setOnAction(e -> {
                removeExplorerId();

                explorerBtn.setGraphic(searchView);
            });

            MenuItem ergoExplorerMenuItem = new MenuItem(ErgoExplorer.NAME);
            ergoExplorerMenuItem.setOnAction(e -> {
                setExplorerId(NetworkID.ERGO_EXPLORER);

                explorerBtn.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), imageWidth));
                if (getNodeInterface() == null) {
                    Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + ErgoExplorer.NAME + " to use this feature.", ButtonType.OK);
                    explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                    explorerAlert.initOwner(m_walletStage);
                    explorerAlert.show();
                }
            });

            explorerBtn.getItems().addAll(explorerNullMenuItem, ergoExplorerMenuItem);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip marketTip = new Tooltip("Select market");
            marketTip.setShowDelay(new javafx.util.Duration(100));
            marketTip.setFont(App.txtFont);

            ImageView exchangeView = IconButton.getIconView(new Image("/assets/bar-chart-30.png"), imageWidth);

            MenuButton marketBtn = new MenuButton();
            marketBtn.setGraphic(m_marketId == null ? exchangeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_marketId, true).getIcon(), imageWidth));
            marketBtn.setTooltip(marketTip);
            marketBtn.setPadding(new Insets(2, 0, 0, 0));
            MenuItem marketNullMenuItem = new MenuItem("(none)");
            marketNullMenuItem.setOnAction(e -> {
                removeMarketId();
                marketBtn.setGraphic(exchangeView);
            });

            MenuItem kucoinMenuItem = new MenuItem(KucoinExchange.NAME);
            kucoinMenuItem.setOnAction(e -> {

                setMarketId(NetworkID.KUKOIN_EXCHANGE);

                marketBtn.setGraphic(IconButton.getIconView(KucoinExchange.getSmallAppIcon(), imageWidth));
                if (getMarketInterface() == null) {
                    Alert marketAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + KucoinExchange.NAME + "'' to use this feature.", ButtonType.OK);
                    marketAlert.setGraphic(IconButton.getIconView(KucoinExchange.getAppIcon(), alertImageWidth));
                    marketAlert.initOwner(m_walletStage);
                    marketAlert.show();
                }
            });

            marketBtn.getItems().addAll(marketNullMenuItem, kucoinMenuItem);

            HBox rightSideMenu = new HBox(networkMenuBtn, explorerBtn, marketBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

            HBox menuBar = new HBox(addButton, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(5, 0, 5, 5));

            HBox paddingBox = new HBox(menuBar);
            paddingBox.setPadding(new Insets(2, 5, 2, 5));

            VBox layoutBox = addressesData.getAddressBox();
            layoutBox.setPadding(SMALL_INSETS);

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text updatedTxt = new Text("Updated:");
            updatedTxt.setFill(App.altColor);
            updatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setPrefWidth(160);
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setId("formField");

            HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
            updateBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacerRegion = new Region();
            VBox.setVgrow(spacerRegion, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane(layoutBox);

            VBox bodyVBox = new VBox(titleBox, paddingBox, scrollPane, updateBox);

            Scene openWalletScene = new Scene(bodyVBox, sceneWidth, sceneHeight);
            openWalletScene.getStylesheets().add("/css/startWindow.css");
            m_walletStage.setScene(openWalletScene);
            m_walletStage.show();

            layoutBox.prefWidthProperty().bind(openWalletScene.widthProperty().subtract(5));

            addButton.setOnAction(e -> {
                addressesData.addAddress();
            });

            HBox.setHgrow(layoutBox, Priority.ALWAYS);

            scrollPane.prefViewportWidthProperty().bind(openWalletScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(openWalletScene.heightProperty().subtract(titleBox.heightProperty().get()).subtract(menuBar.heightProperty().get()).subtract(updateBox.heightProperty().get()));

            m_walletStage.setOnCloseRequest(event -> {
                removeTunnelNoteInterface(addressesData.getNetworkId());
            });

        } else {
            m_walletStage.show();
        }

    }

    public NoteInterface getNodeInterface() {
        return m_nodeId == null ? null : getNetworksData().getNoteInterface(m_nodeId);
    }

    public NoteInterface getExplorerInterface() {
        return m_explorerId == null ? null : getNetworksData().getNoteInterface(m_explorerId);
    }

    public NoteInterface getMarketInterface() {

        return m_marketId == null ? null : getNetworksData().getNoteInterface(m_marketId);
    }

    private void setNodeId(String nodeId) {
        m_nodeId = nodeId;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setExplorerId(String explorerId) {
        m_explorerId = explorerId;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setMarketId(String marketId) {
        m_marketId = marketId;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void removeNodeId() {
        m_nodeId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void removeExplorerId() {
        m_explorerId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void removeMarketId() {
        m_marketId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    @Override
    public boolean sendNoteToFullNetworkId(JsonObject note, String fullNetworkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int indexOfNetworkID = fullNetworkId.indexOf(getNetworkId());

        int indexOfperiod = fullNetworkId.indexOf(".", indexOfNetworkID);

        if (indexOfperiod == -1) {
            return sendNote(note, onSucceeded, onFailed);
        } else {
            int indexOfSecondPeriod = fullNetworkId.indexOf(".", indexOfperiod + 1);
            String tunnelId;

            if (indexOfSecondPeriod == -1) {
                tunnelId = fullNetworkId.substring(indexOfperiod);
            } else {
                tunnelId = fullNetworkId.substring(indexOfperiod, indexOfSecondPeriod);
            }

            NoteInterface addressInterface = getTunnelNoteInterface(tunnelId);
            if (addressInterface != null) {
                return addressInterface.sendNoteToFullNetworkId(note, fullNetworkId, onSucceeded, onFailed);

            }
        }

        return false;

    }
    /* private void updateAddressBtn(double width, Button rowBtn, AddressData addressData) {

        //   BufferedImage imageBuffer = addressData.getBufferedImage();
        double remainingSpace = width;// - imageBuffer.getWidth();

        String addressMinimal = addressData.getAddressMinimal((int) (remainingSpace / 24));
        
        //ImageView btnImageView = new ImageView();
       // if (imageBuffer != null) {
       //     btnImageView.setImage(SwingFXUtils.toFXImage(imageBuffer, null));
      //  }
        String text = "> " + addressData.getName() + ": \n  " + addressMinimal;
        Tooltip addressTip = new Tooltip(addressData.getName());

        //  rowBtn.setGraphic(btnImageView);
        rowBtn.setText(text);
        rowBtn.setTooltip(addressTip);

    }*/
}
