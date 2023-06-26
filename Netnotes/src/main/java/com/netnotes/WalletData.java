package com.netnotes;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
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
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class WalletData extends Network implements NoteInterface {

    public final static double MIN_WIDTH = 400;
    public final static double MIN_HEIGHT = 275;
    public final static double NORMAL_WIDTH = 720;
    public final static double NORMAL_HEIGHT = 480;
    private int m_maxWidth = 800;
    private int m_maxHeight = 600;

    private double m_sceneWidth = NORMAL_WIDTH;
    private double m_sceneHeight = NORMAL_HEIGHT;

    private File logFile;
    private File m_walletFile = null;
    // private File m_walletDirectory;

    private NetworkType m_networkType = NetworkType.MAINNET;

    // private String m_name;
    private String m_networkNetworkeId;
    private String m_explorerId;
    private String m_marketId;

    private String m_quoteTransactionCurrency = "USD";
    private SimpleObjectProperty<PriceQuote> m_lastQuote = new SimpleObjectProperty<PriceQuote>(null);

    private TimersList m_availableTimers;

    // private ErgoWallet m_ergoWallet;
    public WalletData(String id, String name, File walletFile, double sceneWidth, double sceneHeight, String networkId, String explorerId, String marketId, String timerId, JsonArray timersArray, NetworkType networkType, NoteInterface ergoWallet) {
        super(null, name, id, ergoWallet);

        m_sceneWidth = sceneWidth;
        m_sceneHeight = sceneHeight;

        logFile = new File("WalletData" + name + "-log.txt");

        try {
            Files.writeString(logFile.toPath(), "ExchangeId: " + marketId + " networkId: " + networkId + " explorerId: " + explorerId + " networkType: " + networkType + " timers:" + (timersArray != null ? "\n" + timersArray.toString() : "null"));
        } catch (IOException e) {

        }
        m_walletFile = walletFile;
        m_networkType = networkType;

        m_networkNetworkeId = networkId == null ? null : networkId;
        m_explorerId = explorerId == null ? null : explorerId;
        m_marketId = marketId == null ? null : marketId;

        m_availableTimers = new TimersList(timerId, getFullNetworkId(), timersArray, getNetworksData());
        m_availableTimers.lastUpdatedProperty.addListener(e -> {
            getLastUpdated().set(LocalDateTime.now());
        });
        setIconStyle(IconStyle.ROW);

    }

    public int getWindowMaxWidth() {
        return m_maxWidth;
    }

    public int getWindowMaxHeight() {
        return m_maxHeight;
    }

    /*
    private void subscribePriceTimer() {
        if (m_timerNetworkId != null && m_priceTimerId != null && m_marketId != null) {
            NoteInterface timerInterface = getNetworksData().getNoteInterface(m_timerNetworkId);

            if (timerInterface != null) {

                timers

                JsonObject subscribeJson = new JsonObject();
                subscribeJson.addProperty("subject", "SUBSCRIBE");
                subscribeJson.addProperty("fullNetworkId", getFullNetworkId());
                subscribeJson.addProperty("timerId", m_priceTimerId);

                if (!timerInterface.sendNote(subscribeJson, null, null)) {

                    try {
                        Files.writeString(logFile.toPath(), "\nFailed to subscribe:\n" + subscribeJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }

                }

            }
        }
    } */
 /*
    private void subscribeQuantityTimer() {
        if (m_timerNetworkId != null && m_quantityTimerId != null) {
            NoteInterface timerInterface = getNetworksData().getNoteInterface(m_timerNetworkId);

            if (timerInterface != null) {

                JsonObject subscribeJson = new JsonObject();
                subscribeJson.addProperty("subject", "SUBSCRIBE");
                subscribeJson.addProperty("fullNetworkId", getFullNetworkId());
                subscribeJson.addProperty("timerId", m_priceTimerId.get());

                if (!timerInterface.sendNote(subscribeJson, null, null)) {

                    try {
                        Files.writeString(logFile.toPath(), "\nQuantity did not subscribe (may already be subscribed):\n" + subscribeJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }

                }

            }
        }
    } */
    private void unsubscribeTimer(String networkId, String timerId, String fullNetworkId) {
        if (networkId != null && timerId != null) {
            NoteInterface timerInterface = getNetworksData().getNoteInterface(networkId);

            if (timerInterface != null) {

                JsonObject unsubscribeJson = new JsonObject();
                unsubscribeJson.addProperty("subject", "UNSUBSCRIBE");
                unsubscribeJson.addProperty("fullNetworkId", fullNetworkId);
                unsubscribeJson.addProperty("timerId", timerId);

                if (!timerInterface.sendNote(unsubscribeJson, null, null)) {

                    try {
                        Files.writeString(logFile.toPath(), "\nFailed to unsubscribe:\n" + unsubscribeJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

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

        JsonObject windowSize = new JsonObject();
        windowSize.addProperty("width", m_sceneWidth);
        windowSize.addProperty("height", m_sceneHeight);

        jsonObject.add("windowSize", windowSize);

        if (m_networkNetworkeId != null) {
            jsonObject.addProperty("networkNetworkId", m_networkNetworkeId);
        }
        if (m_explorerId != null) {
            jsonObject.addProperty("explorerId", m_explorerId);
        }
        if (m_marketId != null) {
            jsonObject.addProperty("marketId", m_marketId);
        }
        if (m_availableTimers.getTimerNetworkId() != null) {
            jsonObject.addProperty("timerId", m_availableTimers.getTimerNetworkId());
        }
        JsonArray subscribedTimers = m_availableTimers.getSubscribedTimers();
        if (subscribedTimers.size() > 0) {
            jsonObject.add("timers", subscribedTimers);
        }

        return jsonObject;
    }

    @Override
    public void open() {
        try {
            Files.writeString(logFile.toPath(), "\nwalletsData opening.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        openWallet(m_sceneWidth, m_sceneHeight);

    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjectElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");

        String subject = subjectElement.getAsString();
        String networkId = networkIdElement.getAsString();

        switch (subject) {
            case "TIME":
                if (networkId.equals(m_availableTimers.getTimerNetworkId())) {
                    JsonElement localDateTimeElement = note.get("localDateTime");
                    JsonElement timerIdElement = note.get("timerId");

                    if (localDateTimeElement != null && timerIdElement != null) {
                        return m_availableTimers.time(note, timerIdElement.getAsString(), localDateTimeElement.getAsString());
                    }
                }
                break;
            case "TIMERS":
                if (networkId.equals(m_availableTimers.getTimerNetworkId())) {
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
    private GraphicsDevice m_gd;

    private void setDevice() {

        Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        m_maxWidth = rect.width; //gd.getDisplayMode().getWidth();
        m_maxHeight = rect.height; //gd.getDisplayMode().getHeight();
    }

    public void openWallet(double walletSceneWidth, double walletSceneHeight) {

        setDevice();
        double maxWidth = walletSceneWidth > m_maxWidth ? m_maxWidth : walletSceneWidth;
        double maxHeight = walletSceneHeight > m_maxHeight ? m_maxHeight : walletSceneHeight;

        double sceneWidth = maxWidth < MIN_WIDTH ? MIN_WIDTH : maxWidth;
        double sceneHeight = maxHeight < MIN_HEIGHT ? MIN_HEIGHT : maxHeight;

        try {
            Files.writeString(logFile.toPath(), "\nConfirming wallet password.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        Stage walletStage = new Stage();
        walletStage.getIcons().add(ErgoWallet.getSmallAppIcon());
        walletStage.initStyle(StageStyle.UNDECORATED);
        walletStage.setTitle("Wallet file: Enter password");

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), getName() + " - Enter password", closeBtn, walletStage);
        closeBtn.setOnAction(event -> {
            walletStage.close();

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

        Scene passwordScene = new Scene(layoutVBox, sceneWidth, 300);

        passwordScene.getStylesheets().add("/css/startWindow.css");
        walletStage.setScene(passwordScene);

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                try {

                    Wallet wallet = Wallet.load(m_walletFile.toPath(), passwordField.getText());
                    passwordField.setText("");

                    walletStage.setScene(getWalletScene(wallet, sceneWidth, sceneHeight, passwordScene, walletStage));
                    ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, m_maxWidth, m_maxHeight);

                } catch (Exception e1) {

                    passwordField.setText("");
                    try {
                        Files.writeString(logFile.toPath(), e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e2) {

                    }
                }

            }
        });
        ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, m_maxWidth, m_maxHeight);

        walletStage.show();

    }

    public JsonObject getMarketQuoteObject() {
        JsonObject marketQuoteObject = new JsonObject();
        marketQuoteObject.addProperty("subject", "GET_QUOTE");
        marketQuoteObject.addProperty("transactionCurrency", m_quoteTransactionCurrency);
        marketQuoteObject.addProperty("quoteCurrency", ErgoNetwork.NATIVE_CURRENCY.getSymbol());

        return marketQuoteObject;
    }

    private void getMarketQuote(NoteInterface marketInterface) {
        try {
            Files.writeString(logFile.toPath(), "\nGetting " + (marketInterface == null ? "null" : marketInterface.getName()) + " quote:", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        if (marketInterface != null) {
            marketInterface.sendNote(getMarketQuoteObject(), success -> {
                Object sourceValue = success.getSource().getValue();
                if (sourceValue != null) {
                    PriceQuote quote = (PriceQuote) sourceValue;
                    m_lastQuote.set(quote);

                    try {
                        Files.writeString(logFile.toPath(), "\nGot price quote:\n" + quote.getJsonObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }

                }
            }, failed -> {

            });
        }
    }

    private Scene getWalletScene(Wallet wallet, double sceneWidth, double sceneHeight, Scene passwordScene, Stage walletStage) {

        AddressesData addressesData = new AddressesData(FriendlyId.createFriendlyId(), wallet, this, m_networkType);
        PriceQuote lastQuote = m_lastQuote.get();
        if (lastQuote != null) {
            long howOld = lastQuote.howOldMillis();
            if (howOld < 30000) {
                addressesData.setQuote(m_lastQuote.get());
            }
        }
        getMarketQuote(getMarketInterface());

        try {
            Files.writeString(logFile.toPath(), "\nshowing wallet stage", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        String title = getName() + " - (" + m_networkType.toString() + ")";

        double imageWidth = 25;
        double alertImageWidth = 75;

        //  PriceChart priceChart = null;
        walletStage.setTitle(title);

        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), title, maximizeBtn, closeBtn, walletStage);

        Tooltip sendTip = new Tooltip("Send");
        sendTip.setShowDelay(new javafx.util.Duration(100));
        sendTip.setFont(App.txtFont);

        Button sendButton = new Button();
        sendButton.setGraphic(IconButton.getIconView(new Image("/assets/arrow-send-white-30.png"), 30));
        sendButton.setId("menuBtn");
        sendButton.setTooltip(sendTip);
        sendButton.setDisable(true);
        sendButton.setUserData("sendButton");

        //   addressesData.currentAddressProperty
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
        networkMenuBtn.setGraphic(m_networkNetworkeId == null ? nodeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_networkNetworkeId, true).getIcon(), imageWidth));
        networkMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        networkMenuBtn.setTooltip(networkTip);
        networkMenuBtn.setUserData(m_networkNetworkeId);

        MenuItem nodeNullMenuItem = new MenuItem("(none)");

        nodeNullMenuItem.setOnAction(e -> {
            removeNodeId();
            networkMenuBtn.setGraphic(nodeView);
            networkMenuBtn.setUserData(null);
        });

        MenuItem nodeMenuItem = new MenuItem(ErgoNetwork.NAME);
        nodeMenuItem.setGraphic(IconButton.getIconView(ErgoNetwork.getSmallAppIcon(), imageWidth));
        nodeMenuItem.setOnAction(e -> {
            setNodeId(NetworkID.ERGO_NETWORK);

            networkMenuBtn.setGraphic(IconButton.getIconView(ErgoNetwork.getSmallAppIcon(), imageWidth));
            networkMenuBtn.setUserData(NetworkID.ERGO_NETWORK);
            if (getNodeInterface() == null) {
                Alert nodeAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + ErgoNetwork.NAME + "' to use this feature.", ButtonType.OK);
                nodeAlert.setGraphic(IconButton.getIconView(ErgoNetwork.getAppIcon(), alertImageWidth));
                nodeAlert.initOwner(walletStage);
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
        explorerBtn.setUserData(m_explorerId);

        MenuItem explorerNullMenuItem = new MenuItem("(none)");

        explorerNullMenuItem.setOnAction(e -> {
            removeExplorerId();
            explorerBtn.setUserData(null);
            explorerBtn.setGraphic(searchView);
        });

        MenuItem ergoExplorerMenuItem = new MenuItem(ErgoExplorer.NAME);
        ergoExplorerMenuItem.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), imageWidth));

        ergoExplorerMenuItem.setOnAction(e -> {
            setExplorerId(NetworkID.ERGO_EXPLORER);
            explorerBtn.setUserData(NetworkID.ERGO_EXPLORER);
            explorerBtn.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), imageWidth));

            addressesData.updateBalance();
            if (getExplorerInterface() == null) {
                Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + ErgoExplorer.NAME + " to use this feature.", ButtonType.OK);
                explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                explorerAlert.initOwner(walletStage);
                explorerAlert.show();
            } else {

            }

        });

        explorerBtn.getItems().addAll(explorerNullMenuItem, ergoExplorerMenuItem);

        /*
            explorerBtn.getItems().addAll(explorerNullMenuItem, ergoExplorerMenuItem);
              
         */
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Tooltip marketTip = new Tooltip("Select market");
        marketTip.setShowDelay(new javafx.util.Duration(100));
        marketTip.setFont(App.txtFont);

        ImageView exchangeView = IconButton.getIconView(new Image("/assets/bar-chart-30.png"), imageWidth);

        MenuButton marketBtn = new MenuButton();
        marketBtn.setUserData(m_marketId);
        marketBtn.setGraphic(m_marketId == null ? exchangeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_marketId, true).getIcon(), imageWidth));
        marketBtn.setTooltip(marketTip);
        marketBtn.setPadding(new Insets(2, 0, 0, 0));

        MenuItem marketNullMenuItem = new MenuItem("(none)");

        marketNullMenuItem.setOnAction(e -> {
            removeMarketId();
            marketBtn.setUserData(null);
            marketBtn.setGraphic(exchangeView);
        });

        MenuItem kucoinMenuItem = new MenuItem(KucoinExchange.NAME);
        kucoinMenuItem.setGraphic(IconButton.getIconView(KucoinExchange.getSmallAppIcon(), imageWidth));
        kucoinMenuItem.setOnAction(e -> {

            setMarketId(NetworkID.KUKOIN_EXCHANGE);
            marketBtn.setUserData(NetworkID.KUKOIN_EXCHANGE);
            marketBtn.setGraphic(IconButton.getIconView(KucoinExchange.getSmallAppIcon(), imageWidth));

            getMarketQuote(getMarketInterface());

            if (getMarketInterface() == null) {
                Alert marketAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + KucoinExchange.NAME + "'' to use this feature.", ButtonType.OK);
                marketAlert.setGraphic(IconButton.getIconView(KucoinExchange.getAppIcon(), alertImageWidth));
                marketAlert.initOwner(walletStage);
                marketAlert.show();
            }
        });

        marketBtn.getItems().addAll(marketNullMenuItem, kucoinMenuItem);

        Tooltip timerTip = new Tooltip("Select timer");
        timerTip.setShowDelay(new javafx.util.Duration(100));
        timerTip.setFont(App.txtFont);

        ImageView timerView = IconButton.getIconView(new Image("/assets/timer-30.png"), imageWidth);

        MenuButton timerBtn = new MenuButton();
        timerBtn.setGraphic(m_availableTimers.getTimerNetworkId() == null ? timerView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_availableTimers.getTimerNetworkId(), true).getIcon(), imageWidth));;
        timerBtn.setPadding(new Insets(2, 0, 0, 0));
        timerBtn.setTooltip(timerTip);

        MenuItem timerNullMenuItem = new MenuItem("(none)");

        timerNullMenuItem.setOnAction(e -> {
            m_availableTimers.setTimerNetworkId(null);
            timerBtn.setGraphic(timerView);
        });

        MenuItem timerMenuItem = new MenuItem(NetworkTimer.NAME);
        timerMenuItem.setGraphic(IconButton.getIconView(NetworkTimer.getSmallAppIcon(), imageWidth));
        timerMenuItem.setOnAction(e -> {

            m_availableTimers.setTimerNetworkId(NetworkID.NETWORK_TIMER);
            timerBtn.setGraphic(IconButton.getIconView(NetworkTimer.getSmallAppIcon(), imageWidth));

            if (getNetworksData().getNoteInterface(m_availableTimers.getTimerNetworkId()) == null) {
                Alert timerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + NetworkTimer.NAME + " to use this feature.", ButtonType.OK);
                timerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                timerAlert.initOwner(walletStage);
                timerAlert.show();
            }
        });

        timerBtn.getItems().addAll(timerNullMenuItem, timerMenuItem);

        HBox rightSideMenu = new HBox(networkMenuBtn, explorerBtn, marketBtn, timerBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

        HBox menuBar = new HBox(sendButton, addButton, spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        HBox timerMenu = m_availableTimers.createTimerMenu(explorerBtn, marketBtn);

        VBox menuVBox = new VBox(paddingBox, timerMenu);

        VBox layoutBox = addressesData.getAddressBox();
        //layoutBox.setPadding(SMALL_INSETS);

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
        scrollPane.setId("bodyBox");
        TextField totalField = new TextField(Utils.formatCryptoString(0, m_quoteTransactionCurrency, false));
        totalField.setId("priceField");
        totalField.setEditable(false);
        HBox.setHgrow(totalField, Priority.ALWAYS);
        HBox summaryBox = new HBox(totalField);
        HBox.setHgrow(summaryBox, Priority.ALWAYS);
        summaryBox.setPadding(new Insets(5, 0, 0, 5));

        HBox scrollBox = new HBox(scrollPane);
        HBox.setHgrow(scrollBox, Priority.ALWAYS);
        scrollBox.setPadding(new Insets(5, 5, 0, 5));
        VBox bodyVBox = new VBox(titleBox, menuVBox, scrollBox, summaryBox, updateBox);

        Scene openWalletScene = new Scene(bodyVBox, sceneWidth, sceneHeight);

        layoutBox.prefWidthProperty().bind(openWalletScene.widthProperty().subtract(5));

        addButton.setOnAction(e -> {
            addressesData.addAddress();
        });

        HBox.setHgrow(layoutBox, Priority.ALWAYS);

        scrollPane.prefViewportWidthProperty().bind(openWalletScene.widthProperty().subtract(10));
        scrollPane.prefViewportHeightProperty().bind(openWalletScene.heightProperty().subtract(titleBox.heightProperty()).subtract(menuBar.heightProperty()).subtract(updateBox.heightProperty()).subtract(summaryBox.heightProperty()));

        /*
             * Listeners
         */
        sendButton.setOnAction((actionEvent) -> {
            if (getNodeInterface() == null) {
                Alert nodeAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall/Enable '" + ErgoNetwork.NAME + "' to use this feature.", ButtonType.OK);
                nodeAlert.setGraphic(IconButton.getIconView(ErgoNetwork.getAppIcon(), alertImageWidth));
                nodeAlert.initOwner(walletStage);
                nodeAlert.show();
            } else {
                Scene sendScene = addressesData.getSendScene(openWalletScene, walletStage);
                if (sendScene != null) {
                    walletStage.setScene(sendScene);
                    ResizeHelper.addResizeListener(walletStage, 400, 440, m_maxWidth, m_maxHeight);
                }
            }

        });
        openWalletScene.focusOwnerProperty().addListener((e) -> {
            if (openWalletScene.focusOwnerProperty().get() instanceof AddressData) {
                AddressData addressData = (AddressData) openWalletScene.focusOwnerProperty().get();

                addressesData.getSelectedAddressDataProperty().set(addressData);
                if (getNodeInterface() != null) {
                    sendButton.setId("menuBtn");
                    sendButton.setDisable(false);
                }
            } else {
                if (openWalletScene.focusOwnerProperty().get() instanceof Button) {
                    Button focusedButton = (Button) openWalletScene.focusOwnerProperty().get();

                    if (focusedButton.getUserData() != null) {
                        String buttonData = (String) focusedButton.getUserData();
                        if (!(buttonData.equals("sendButton"))) {
                            addressesData.getSelectedAddressDataProperty().set(null);
                            sendButton.setId("menuBtnDisabled");
                            sendButton.setDisable(true);

                        }
                    } else {
                        addressesData.getSelectedAddressDataProperty().set(null);
                        sendButton.setId("menuBtnDisabled");
                        sendButton.setDisable(true);
                    }
                } else {
                    addressesData.getSelectedAddressDataProperty().set(null);
                    sendButton.setId("menuBtnDisabled");
                    sendButton.setDisable(true);
                }
            }
        });
        ChangeListener<PriceQuote> quoteListener = (obs, oldValue, newQuote) -> {
            addressesData.setQuote(newQuote);
        };

        ChangeListener<Number> totalListener = (obs, oldValue, newValue) -> {

            double updatedValue = newValue.doubleValue();

            String formatedAmount = Utils.formatCryptoString(updatedValue, m_quoteTransactionCurrency, true);
            PriceQuote quote = m_lastQuote.get();
            Platform.runLater(() -> totalField.setText("Σ(" + (quote == null ? Utils.formatCryptoString(0, m_quoteTransactionCurrency, false) : Utils.formatCryptoString(quote.getAmount(), m_quoteTransactionCurrency, true)) + ") " + formatedAmount));

            Platform.runLater(() -> lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now())));
        };

        m_lastQuote.addListener(quoteListener);
        addressesData.getTotalDoubleProperty().addListener(totalListener);

        walletStage.setOnCloseRequest(event -> {

            m_lastQuote.removeListener(quoteListener);
            addressesData.getTotalDoubleProperty().removeListener(totalListener);
        });

        openWalletScene.getStylesheets().add("/css/startWindow.css");
        closeBtn.setOnAction(closeEvent -> {

            walletStage.close();

        });

        return openWalletScene;

    }

    public String getNetworkNetworkId() {
        return m_networkNetworkeId;
    }

    public NoteInterface getNodeInterface() {
        return m_networkNetworkeId == null ? null : getNetworksData().getNoteInterface(m_networkNetworkeId);
    }

    public String getExplorerId() {
        return m_explorerId;
    }

    public String getMarketId() {
        return m_marketId;
    }

    public NoteInterface getExplorerInterface() {
        return m_explorerId == null ? null : getNetworksData().getNoteInterface(m_explorerId);
    }

    public NoteInterface getMarketInterface() {

        return m_marketId == null ? null : getNetworksData().getNoteInterface(m_marketId);
    }

    private void setNodeId(String nodeId) {
        m_networkNetworkeId = nodeId;
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
        m_networkNetworkeId = null;
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