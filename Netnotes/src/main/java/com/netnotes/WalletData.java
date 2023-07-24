package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.satergo.Wallet;

import com.utils.Utils;

import javafx.application.Platform;

import javafx.beans.property.SimpleObjectProperty;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WalletData extends Network implements NoteInterface {

    public final static double MIN_WIDTH = 400;
    public final static double MIN_HEIGHT = 275;

    private double m_sceneWidth = 400;
    private double m_sceneHeight = 720;

    private File logFile = new File("WalletData-log.txt");
    private File m_walletFile = null;
    // private File m_walletDirectory;

    private NetworkType m_networkType = NetworkType.MAINNET;

    // private String m_name;
    private String m_nodesId;
    private String m_selectedNodeId;
    private String m_explorerId;
    private String m_explorerUpdates;
    private MarketUpdates m_marketUpdates;

    private String m_quoteTransactionCurrency = "USD";
    private SimpleObjectProperty<PriceQuote> m_lastQuote = new SimpleObjectProperty<PriceQuote>(null);
    private ErgoWallet m_ergoWallet;

    // private ErgoWallet m_ergoWallet;
    public WalletData(String id, String name, File walletFile, double sceneWidth, double sceneHeight, String nodesId, String selectedNodeId, String explorerId, String explorerUpdates, MarketUpdates marketUpdates, NetworkType networkType, ErgoWallet ergoWallet) {
        super(null, name, id, ergoWallet);

        m_sceneWidth = sceneWidth;
        m_sceneHeight = sceneHeight;

        m_walletFile = walletFile;
        m_networkType = networkType;

        m_nodesId = nodesId;
        m_selectedNodeId = selectedNodeId;
        m_explorerId = explorerId;
        m_explorerUpdates = explorerUpdates;
        m_marketUpdates = marketUpdates;

        m_ergoWallet = ergoWallet;

        setIconStyle(IconStyle.ROW);

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

        if (m_nodesId != null) {
            jsonObject.addProperty("nodesId", m_nodesId);
        }
        if (m_selectedNodeId != null) {
            jsonObject.addProperty("selectedNodeId", m_selectedNodeId);
        }
        if (m_explorerId != null) {
            jsonObject.addProperty("explorerId", m_explorerId);
        }

        if (m_explorerUpdates != null) {
            jsonObject.addProperty("explorerUpdates", m_explorerUpdates);
        }
        if (m_marketUpdates != null) {
            jsonObject.add("marketUpdates", m_marketUpdates.getJsonObject());
        }

        return jsonObject;
    }

    @Override
    public void open() {

        openWallet();

    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjectElement = note.get("subject");
        JsonElement networkIdElement = note.get("networkId");

        String subject = subjectElement.getAsString();
        String networkId = networkIdElement.getAsString();

        return false;
    }

    public void open(String password) {
        try {

            Wallet wallet = Wallet.load(m_walletFile.toPath(), password);

            Stage walletStage = new Stage();
            walletStage.getIcons().add(ErgoWallet.getSmallAppIcon());
            walletStage.initStyle(StageStyle.UNDECORATED);
            walletStage.setTitle(getName() + " - " + ErgoWallet.NAME);

            Scene walletScene = getWalletScene(wallet, m_sceneWidth, m_sceneHeight, walletStage);
            walletStage.setScene(walletScene);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());
        } catch (Exception e1) {

            //    passwordField.setText("");
            try {
                Files.writeString(logFile.toPath(), e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e2) {

            }
        }
    }

    public void openWallet() {

        double sceneWidth = 720;
        double sceneHeight = 480;

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
        Rectangle rect = getNetworksData().getMaximumWindowBounds();

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                try {

                    Wallet wallet = Wallet.load(m_walletFile.toPath(), passwordField.getText());
                    passwordField.setText("");

                    walletStage.setScene(getWalletScene(wallet, sceneWidth, sceneHeight, walletStage));
                    ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());

                } catch (Exception e1) {

                    passwordField.setText("");
                    try {
                        Files.writeString(logFile.toPath(), e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e2) {

                    }
                }

            }
        });

        ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());
        walletStage.show();

    }

    private Scene getWalletScene(Wallet wallet, double sceneWidth, double sceneHeight, Stage walletStage) {

        AddressesData addressesData = new AddressesData(FriendlyId.createFriendlyId(), wallet, this, m_networkType, walletStage);

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
        networkMenuBtn.setGraphic(m_nodesId == null ? nodeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_nodesId, true).getIcon(), imageWidth));
        networkMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        networkMenuBtn.setTooltip(networkTip);
        networkMenuBtn.setUserData(m_nodesId);

        MenuItem nodeNullMenuItem = new MenuItem("(none)");

        nodeNullMenuItem.setOnAction(e -> {
            removeNodesId();
            networkMenuBtn.setGraphic(nodeView);
            networkMenuBtn.setUserData(null);
        });

        MenuItem nodeMenuItem = new MenuItem(ErgoNetwork.NAME);
        nodeMenuItem.setGraphic(IconButton.getIconView(ErgoNetwork.getSmallAppIcon(), imageWidth));
        nodeMenuItem.setOnAction(e -> {

            networkMenuBtn.setGraphic(IconButton.getIconView(ErgoNetwork.getSmallAppIcon(), imageWidth));

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

        Tooltip marketTip = new Tooltip("Price: " + m_marketUpdates.getNetworkId() == null ? "Disabled" : "Enabled");
        marketTip.setShowDelay(new javafx.util.Duration(100));
        marketTip.setFont(App.txtFont);

        ImageView exchangeView = IconButton.getIconView(new Image("/assets/bar-chart-30.png"), imageWidth);

        MenuButton marketBtn = new MenuButton();
        marketBtn.setUserData(m_marketUpdates.getNetworkId());
        /// marketBtn.setGraphic()
        marketBtn.setPadding(new Insets(2, 0, 0, 0));

        MenuItem chartsNullMenuItem = new MenuItem("(none)");

        chartsNullMenuItem.setOnAction(e -> {
            m_marketUpdates.setNetworkid(null);
            marketBtn.setGraphic(exchangeView);
        });

        MenuItem kuCoinExchangeMenuItem = new MenuItem(KucoinExchange.NAME);
        kuCoinExchangeMenuItem.setGraphic(IconButton.getIconView(KucoinExchange.getSmallAppIcon(), imageWidth));
        kuCoinExchangeMenuItem.setOnAction(e -> {

            marketBtn.setGraphic(IconButton.getIconView(KucoinExchange.getSmallAppIcon(), imageWidth));

            m_marketUpdates.setNetworkid(KucoinExchange.NETWORK_ID);

            if (getNetworksData().getNoteInterface(KucoinExchange.NETWORK_ID) == null) {
                Alert marketAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + KucoinExchange.NAME + "'' to use this feature.", ButtonType.OK);
                marketAlert.setGraphic(IconButton.getIconView(KucoinExchange.getAppIcon(), alertImageWidth));
                marketAlert.initOwner(walletStage);
                marketAlert.show();
            } else {

            }
        });

        marketBtn.getItems().addAll(chartsNullMenuItem, kuCoinExchangeMenuItem);

        HBox rightSideMenu = new HBox(networkMenuBtn, explorerBtn, marketBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

        HBox menuBar = new HBox(sendButton, addButton, spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        VBox menuVBox = new VBox(paddingBox);

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
                    Rectangle rect = getNetworksData().getMaximumWindowBounds();
                    ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());
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

        ChangeListener<Number> totalListener = (obs, oldValue, newValue) -> {

            double updatedValue = newValue.doubleValue();

            String formatedAmount = Utils.formatCryptoString(updatedValue, m_quoteTransactionCurrency, true);
            PriceQuote quote = m_lastQuote.get();
            Platform.runLater(() -> totalField.setText("Σ(" + (quote == null ? Utils.formatCryptoString(0, m_quoteTransactionCurrency, false) : Utils.formatCryptoString(quote.getAmount(), m_quoteTransactionCurrency, true)) + ") " + formatedAmount));

            Platform.runLater(() -> lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now())));
        };

        addressesData.getTotalDoubleProperty().addListener(totalListener);

        walletStage.setOnCloseRequest(event -> {

            addressesData.getTotalDoubleProperty().removeListener(totalListener);
        });

        openWalletScene.getStylesheets().add("/css/startWindow.css");
        closeBtn.setOnAction(closeEvent -> {

            walletStage.close();

        });

        return openWalletScene;

    }

    public String getNodesId() {
        return m_nodesId;
    }

    public NoteInterface getNodeInterface() {
        return m_nodesId == null ? null : getNetworksData().getNoteInterface(m_nodesId);
    }

    public String getExplorerId() {
        return m_explorerId;
    }

    public NoteInterface getExplorerInterface() {

        return m_explorerId == null ? null : m_ergoWallet.getErgoNetworkData().getNetwork(m_explorerId);
    }

    public void setNodesId(String nodesId) {
        m_nodesId = nodesId;
        getLastUpdated().set(LocalDateTime.now());
    }

    public void setExplorerId(String explorerId) {
        m_explorerId = explorerId;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void removeNodesId() {
        m_nodesId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void removeExplorerId() {
        m_explorerId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle == IconStyle.ROW ? ErgoWallet.getSmallAppIcon() : ErgoWallet.getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        iconButton.setButtonId(getNetworkId());
        return iconButton;
    }
}
