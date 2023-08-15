package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WalletData extends Network implements NoteInterface {

    public final static double MIN_WIDTH = 400;
    public final static double MIN_HEIGHT = 275;

    private File logFile = new File("WalletData-log.txt");
    private File m_walletFile = null;
    // private File m_walletDirectory;

    private NetworkType m_networkType = NetworkType.MAINNET;

    // private String m_name;
    private String m_nodesId;
    private String m_selectedNodeId;
    private String m_explorerId;
    private long m_explorerTimePeriod;

    private String m_marketsId;
    private String m_selectedMarketId;

    private String m_quoteTransactionCurrency = "USD";
    private SimpleObjectProperty<PriceQuote> m_lastQuote = new SimpleObjectProperty<PriceQuote>(null);
    private ErgoWallet m_ergoWallet;

    // private ErgoWallet m_ergoWallet;
    public WalletData(String id, String name, File walletFile, String nodesId, String selectedNodeId, String explorerId, long explorerTimePeriod, String marketsId, String selectedMarketId, NetworkType networkType, ErgoWallet ergoWallet) {
        super(null, name, id, ergoWallet);

        m_walletFile = walletFile;
        m_networkType = networkType;

        m_nodesId = nodesId;
        m_selectedNodeId = selectedNodeId;
        m_explorerId = explorerId;
        m_explorerTimePeriod = explorerTimePeriod;
        m_marketsId = marketsId;
        m_selectedMarketId = selectedMarketId;

        m_ergoWallet = ergoWallet;

        setIconStyle(IconStyle.ROW);

    }

    public WalletData(String id, String name, JsonObject jsonObject, ErgoWallet ergoWallet) {
        super(null, name, id, ergoWallet);

        m_ergoWallet = ergoWallet;

        try {
            Files.writeString(logFile.toPath(), jsonObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        JsonElement fileLocationElement = jsonObject.get("file");
        JsonElement stageElement = jsonObject.get("stage");
        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement nodeIdElement = jsonObject.get("node");
        JsonElement explorerElement = jsonObject.get("explorer");
        JsonElement marketElement = jsonObject.get("market");

        m_walletFile = fileLocationElement == null ? null : new File(fileLocationElement.getAsString());
        m_networkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;

        JsonObject stageJson = stageElement != null && stageElement.isJsonObject() ? stageElement.getAsJsonObject() : null;
        JsonElement stageWidthElement = stageJson != null ? stageJson.get("width") : null;
        JsonElement stageHeightElement = stageJson != null ? stageJson.get("height") : null;

        double stageWidth = stageWidthElement != null && stageWidthElement.isJsonPrimitive() ? stageWidthElement.getAsDouble() : 700;
        double stageHeight = stageHeightElement != null && stageHeightElement.isJsonPrimitive() ? stageHeightElement.getAsDouble() : 350;

        setIconStyle(IconStyle.ROW);
        setStageWidth(stageWidth);
        setStageHeight(stageHeight);

        setNodeObject(nodeIdElement != null && nodeIdElement.isJsonObject() ? nodeIdElement.getAsJsonObject() : null);
        setMarketObject(marketElement != null && marketElement.isJsonObject() ? marketElement.getAsJsonObject() : null);
        setExplorerObject(explorerElement != null && explorerElement.isJsonObject() ? explorerElement.getAsJsonObject() : null);
    }

    public void setMarketObject(JsonObject json) {
        if (json == null) {
            m_marketsId = null;
            m_selectedMarketId = null;
        } else {
            JsonElement marketIdElement = json.get("networkId");
            JsonElement selectedMarketElement = json.get("selectedId");

            m_marketsId = marketIdElement != null && marketIdElement.isJsonPrimitive() ? marketIdElement.getAsString() : null;
            String selectedMarketId = selectedMarketElement != null && selectedMarketElement.isJsonPrimitive() ? selectedMarketElement.getAsString() : null;
            m_selectedMarketId = selectedMarketId;
        }

    }

    public void setExplorerObject(JsonObject json) {
        if (json == null) {
            m_explorerId = null;
            m_explorerTimePeriod = -1;
        } else {
            JsonElement explorerIdElement = json.get("networkId");
            JsonElement explorerTimePeriodElement = json.get("timePeriod");

            m_explorerId = explorerIdElement != null && explorerIdElement.isJsonPrimitive() ? explorerIdElement.getAsString() : null;
            m_explorerTimePeriod = explorerTimePeriodElement != null && explorerTimePeriodElement.isJsonPrimitive() ? explorerTimePeriodElement.getAsLong() : -1;

        }

    }

    public void setNodeObject(JsonObject json) {
        if (json == null) {
            m_nodesId = null;
            m_selectedNodeId = null;
        } else {
            JsonElement idElement = json.get("networkId");
            JsonElement selectedIdElement = json.get("selectedId");

            m_nodesId = idElement != null && idElement.isJsonPrimitive() ? idElement.getAsString() : null;
            m_selectedNodeId = selectedIdElement != null && selectedIdElement.isJsonPrimitive() ? selectedIdElement.getAsString() : null;
        }
    }

    public JsonObject getNodesObject() {
        JsonObject json = new JsonObject();
        json.addProperty("networkId", m_nodesId);
        json.addProperty("selectedId", m_selectedNodeId);
        return json;
    }

    public JsonObject getExplorerObject() {
        JsonObject json = new JsonObject();
        json.addProperty("networkId", m_explorerId);
        json.addProperty("timePeriod", m_explorerTimePeriod);

        return json;
    }

    public JsonObject getMarketsObject() {
        JsonObject json = new JsonObject();
        json.addProperty("networkId", m_marketsId);
        json.addProperty("selectedId", m_selectedMarketId);
        return json;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("id", getNetworkId());
        jsonObject.addProperty("file", m_walletFile.getAbsolutePath());
        jsonObject.addProperty("networkType", m_networkType.toString());

        jsonObject.add("stage", getStageJson());

        if (m_nodesId != null) {
            jsonObject.add("node", getNodesObject());
        }

        if (m_explorerId != null) {
            jsonObject.add("explorer", getExplorerObject());
        }

        if (m_marketsId != null) {
            jsonObject.add("market", getMarketsObject());
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

        // String subject = subjectElement.getAsString();
        // String networkId = networkIdElement.getAsString();
        return false;
    }
    private boolean m_isOpen = false;

    public void openWallet() {
        if (!m_isOpen) {
            m_isOpen = true;
            double sceneWidth = 600;
            double sceneHeight = 320;

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
                m_isOpen = false;
            });

            Button imageButton = App.createImageButton(ErgoWallet.getAppIcon(), "Wallet");
            imageButton.setGraphicTextGap(10);
            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setPadding(new Insets(0, 0, 15, 0));

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

            Scene passwordScene = new Scene(layoutVBox, sceneWidth, sceneHeight);

            passwordScene.getStylesheets().add("/css/startWindow.css");
            walletStage.setScene(passwordScene);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            passwordField.setOnKeyPressed(e -> {

                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.ENTER) {

                    try {

                        Wallet wallet = Wallet.load(m_walletFile.toPath(), passwordField.getText());
                        passwordField.setText("");

                        walletStage.setScene(getWalletScene(wallet, walletStage));
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
            walletStage.setOnCloseRequest(e -> {
                m_isOpen = false;
            });
        }
    }

    public NoteInterface getMarketInterface() {
        if (m_marketsId != null) {
            return m_ergoWallet.getErgoNetworkData().getNetwork(m_marketsId);
        }
        return null;
    }

    private Scene getWalletScene(Wallet wallet, Stage walletStage) {
        ExplorerData explorerData = getExplorerInterface() != null && m_explorerTimePeriod != -1 ? new ExplorerData(m_ergoWallet, m_explorerId, m_explorerTimePeriod, TimeUnit.SECONDS) : null;

        AddressesData addressesData = new AddressesData(FriendlyId.createFriendlyId(), wallet, this, explorerData, m_networkType, walletStage);

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

        Tooltip sendTip = new Tooltip("Select address");
        sendTip.setShowDelay(new javafx.util.Duration(100));
        sendTip.setFont(App.txtFont);

        Button sendButton = new Button();
        sendButton.setGraphic(IconButton.getIconView(new Image("/assets/arrow-send-white-30.png"), 30));
        sendButton.setId("menuBtn");
        sendButton.setTooltip(sendTip);

        sendButton.setUserData("sendButton");

        //   addressesData.currentAddressProperty
        Tooltip addTip = new Tooltip("Add address");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        Button addButton = new Button();
        addButton.setGraphic(IconButton.getIconView(new Image("/assets/git-branch-outline-white-30.png"), 30));
        addButton.setId("menuBtn");
        addButton.setTooltip(addTip);

        HBox rightSideMenu = new HBox();

        Tooltip nodesTip = new Tooltip("Select a node");
        nodesTip.setShowDelay(new javafx.util.Duration(100));
        nodesTip.setFont(App.txtFont);

        String emptyNodeUrl = "/assets/node-30.png";

        MenuButton nodesMenuBtn = new MenuButton();
        nodesMenuBtn.setGraphic(m_nodesId == null ? IconButton.getIconView(new Image(emptyNodeUrl), imageWidth) : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_nodesId, true).getIcon(), imageWidth));
        nodesMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        nodesMenuBtn.setTooltip(nodesTip);

        MenuButton nodeOptionsMenuBtn = addressesData.getNodeOptionsButton();
        nodeOptionsMenuBtn.setPadding(new Insets(2, 2, 0, 0));
        MenuItem nodeMenuItem = new MenuItem(ErgoNodes.NAME);
        MenuItem nodeNullMenuItem = new MenuItem("(none)");
        nodeNullMenuItem.setId("rowBtn");

        nodeNullMenuItem.setOnAction(e -> {
            setNodesId(null);
            nodesMenuBtn.setGraphic(IconButton.getIconView(new Image(emptyNodeUrl), imageWidth));

            if (!nodesMenuBtn.getItems().contains(nodeMenuItem)) {
                nodesMenuBtn.getItems().add(nodeMenuItem);
            }

            if (nodesMenuBtn.getItems().contains(nodeNullMenuItem)) {
                nodesMenuBtn.getItems().remove(nodeNullMenuItem);
            }

            if (rightSideMenu.getChildren().contains(nodeOptionsMenuBtn)) {

                rightSideMenu.getChildren().remove(nodeOptionsMenuBtn);

            }
        });

        nodeMenuItem.setId("rowBtn");
        nodeMenuItem.setGraphic(IconButton.getIconView(ErgoNodes.getSmallAppIcon(), imageWidth));
        nodeMenuItem.setOnAction(e -> {
            setNodesId(ErgoNodes.NETWORK_ID);
            nodesMenuBtn.setGraphic(IconButton.getIconView(ErgoNodes.getSmallAppIcon(), imageWidth));

            if (getNodeInterface() == null) {
                Alert nodeAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + ErgoNetwork.NAME + "' to use this feature.", ButtonType.OK);
                nodeAlert.setGraphic(IconButton.getIconView(ErgoNetwork.getAppIcon(), alertImageWidth));
                nodeAlert.initOwner(walletStage);
                nodeAlert.show();
            }

            if (nodesMenuBtn.getItems().contains(nodeMenuItem)) {
                nodesMenuBtn.getItems().remove(nodeMenuItem);
            }

            if (!nodesMenuBtn.getItems().contains(nodeNullMenuItem)) {
                nodesMenuBtn.getItems().add(nodeNullMenuItem);
            }

            if (!rightSideMenu.getChildren().contains(nodeOptionsMenuBtn)) {

                rightSideMenu.getChildren().add(rightSideMenu.getChildren().indexOf(nodesMenuBtn) + 1, nodeOptionsMenuBtn);

            }

        });

        Tooltip explorerUrlTip = new Tooltip("Select explorer");
        explorerUrlTip.setShowDelay(new javafx.util.Duration(100));
        explorerUrlTip.setFont(App.txtFont);

        String explorerEmptyUrl = "/assets/search-outline-white-30.png";

        MenuButton explorerBtn = new MenuButton();
        explorerBtn.setGraphic(m_explorerId == null ? IconButton.getIconView(new Image(explorerEmptyUrl), imageWidth) : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_explorerId, true).getIcon(), imageWidth));
        explorerBtn.setPadding(new Insets(2, 0, 0, 2));
        explorerBtn.setTooltip(explorerUrlTip);

        MenuButton explorerOptionsBtn = new MenuButton(m_explorerTimePeriod == -1 ? "-" : m_explorerTimePeriod + "s");
        explorerOptionsBtn.setPadding(new Insets(2, 2, 0, 0));
        explorerOptionsBtn.setFont(Font.font("OCR A Extended", FontWeight.BOLD, 10));

        MenuItem explorerOption5s = new MenuItem("5s");
        explorerOption5s.setId("rowBtn");
        explorerOption5s.setOnAction((e) -> {
            setExplorer(ErgoExplorer.NETWORK_ID, 30);
            if (getExplorerInterface() != null) {

                addressesData.updateExplorerData(new ExplorerData(m_ergoWallet, ErgoExplorer.NETWORK_ID, 30, TimeUnit.SECONDS));
                explorerOptionsBtn.setText("5s");
            } else {
                Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + ErgoExplorer.NAME + " to use this feature.", ButtonType.OK);
                explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                explorerAlert.initOwner(walletStage);
                explorerAlert.setTitle("Attention");
                explorerAlert.show();

                addressesData.updateExplorerData(null);
                explorerOptionsBtn.setText("-");
            }
        });

        MenuItem explorerOption15s = new MenuItem("15s");
        explorerOption15s.setId("rowBtn");
        explorerOption15s.setOnAction((e) -> {

            setExplorer(ErgoExplorer.NETWORK_ID, 15);
            if (getExplorerInterface() != null) {
                explorerOptionsBtn.setText("15s");
                addressesData.updateExplorerData(new ExplorerData(m_ergoWallet, ErgoExplorer.NETWORK_ID, 15, TimeUnit.SECONDS));
            } else {
                Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + ErgoExplorer.NAME + " to use this feature.", ButtonType.OK);
                explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                explorerAlert.initOwner(walletStage);
                explorerAlert.setTitle("Attention");
                explorerAlert.show();

                explorerOptionsBtn.setText("-");
                addressesData.updateExplorerData(null);
            }
        });

        MenuItem explorerOption30s = new MenuItem("30s");
        explorerOption30s.setId("rowBtn");
        explorerOption30s.setOnAction(e -> {
            explorerOptionsBtn.setText("30s");
            setExplorer(ErgoExplorer.NETWORK_ID, 30);
            if (getExplorerInterface() != null) {
                addressesData.updateExplorerData(new ExplorerData(m_ergoWallet, ErgoExplorer.NETWORK_ID, 30, TimeUnit.SECONDS));
            } else {
                Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + ErgoExplorer.NAME + " to use this feature.", ButtonType.OK);
                explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                explorerAlert.initOwner(walletStage);
                explorerAlert.setTitle("Attention");
                explorerAlert.show();

                addressesData.updateExplorerData(null);
            }
        });

        explorerOptionsBtn.getItems().addAll(explorerOption5s, explorerOption15s, explorerOption30s);

        MenuItem explorerNullMenuItem = new MenuItem("(none)");
        explorerNullMenuItem.setId("rowBtn");
        MenuItem ergoExplorerMenuItem = new MenuItem(ErgoExplorer.NAME);
        ergoExplorerMenuItem.setId("rowBtn");
        ergoExplorerMenuItem.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), imageWidth));

        ergoExplorerMenuItem.setOnAction(e -> {
            setExplorer(ErgoExplorer.NETWORK_ID, 15);
            explorerBtn.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), imageWidth));

            if (getExplorerInterface() == null) {
                Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall " + ErgoExplorer.NAME + " to use this feature.", ButtonType.OK);
                explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), alertImageWidth));
                explorerAlert.initOwner(walletStage);
                explorerAlert.setTitle("Attention");
                explorerAlert.show();
            } else {

                if (explorerBtn.getItems().contains(ergoExplorerMenuItem)) {
                    explorerBtn.getItems().remove(ergoExplorerMenuItem);
                }
                if (!explorerBtn.getItems().contains(explorerNullMenuItem)) {
                    explorerBtn.getItems().add(explorerNullMenuItem);
                }

                if (!rightSideMenu.getChildren().contains(explorerOptionsBtn)) {

                    rightSideMenu.getChildren().add(rightSideMenu.getChildren().indexOf(explorerBtn) + 1, explorerOptionsBtn);

                }
                addressesData.updateExplorerData(new ExplorerData(m_ergoWallet, ErgoExplorer.NETWORK_ID, m_explorerTimePeriod, TimeUnit.SECONDS));

            }

        });

        explorerNullMenuItem.setOnAction(e -> {

            setExplorer(null, -1);
            explorerBtn.setGraphic(IconButton.getIconView(new Image(explorerEmptyUrl), imageWidth));

            if (!explorerBtn.getItems().contains(ergoExplorerMenuItem)) {
                explorerBtn.getItems().add(ergoExplorerMenuItem);
            }
            if (explorerBtn.getItems().contains(explorerNullMenuItem)) {
                explorerBtn.getItems().remove(explorerNullMenuItem);
            }

            if (rightSideMenu.getChildren().contains(explorerOptionsBtn)) {
                rightSideMenu.getChildren().remove(explorerOptionsBtn);
            }
            addressesData.updateExplorerData(null);
        });

        String emptyMarketUrl = "/assets/exchange-30.png";

        Tooltip marketsTip = new Tooltip("Select market");
        marketsTip.setShowDelay(new javafx.util.Duration(100));
        marketsTip.setFont(App.txtFont);

        MenuButton marketsBtn = new MenuButton();
        marketsBtn.setGraphic(m_marketsId == null ? IconButton.getIconView(new Image(emptyMarketUrl), imageWidth) : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_marketsId, true).getIcon(), imageWidth));
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);

        BufferedMenuButton marketOptionsBtn = addressesData.getMarketOptionsButton();
        marketOptionsBtn.setPadding(new Insets(2, 2, 0, 0));
        marketOptionsBtn.setUserData(m_selectedMarketId);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Region seperator1 = new Region();
        seperator1.setMinWidth(1);
        seperator1.setId("vSeperatorGradient");
        VBox.setVgrow(seperator1, Priority.ALWAYS);

        Region seperator2 = new Region();
        seperator2.setMinWidth(1);
        seperator2.setId("vSeperatorGradient");
        VBox.setVgrow(seperator2, Priority.ALWAYS);

        rightSideMenu.getChildren().addAll(nodesMenuBtn, seperator1, explorerBtn, seperator2, marketsBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 0, 0, 10));
        rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

        if (m_explorerId == null) {
            explorerBtn.getItems().add(ergoExplorerMenuItem);
        } else {
            explorerBtn.getItems().add(explorerNullMenuItem);
            rightSideMenu.getChildren().add(rightSideMenu.getChildren().indexOf(explorerBtn) + 1, explorerOptionsBtn);
        }

        if (m_nodesId == null) {
            nodesMenuBtn.getItems().add(nodeMenuItem);
        } else {

            rightSideMenu.getChildren().add(rightSideMenu.getChildren().indexOf(nodesMenuBtn) + 1, nodeOptionsMenuBtn);
            nodesMenuBtn.getItems().add(nodeNullMenuItem);
        }

        Runnable updateMarketOptions = () -> {
            NoteInterface marketInterface = getMarketInterface();

            if (marketInterface != null) {
                marketsBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getNetworksData(), m_marketsId, true).getIcon(), imageWidth));
                if (!rightSideMenu.getChildren().contains(marketOptionsBtn)) {
                    rightSideMenu.getChildren().add(rightSideMenu.getChildren().indexOf(marketsBtn) + 1, marketOptionsBtn);
                }
            } else {
                if (m_marketsId == null) {
                    if (rightSideMenu.getChildren().contains(marketOptionsBtn)) {
                        rightSideMenu.getChildren().remove(marketOptionsBtn);
                    }
                    marketsBtn.setGraphic(IconButton.getIconView(new Image(emptyMarketUrl), imageWidth));
                }
            }
            addressesData.updateMarketsList();
        };

        MenuItem ergoMarketsMenuItem = new MenuItem(ErgoMarkets.NAME);
        ergoMarketsMenuItem.setGraphic(IconButton.getIconView(ErgoMarkets.getSmallAppIcon(), imageWidth));
        ergoMarketsMenuItem.setId("rowBtn");
        ergoMarketsMenuItem.setOnAction(e -> {
            setMarketsId(ErgoMarkets.NETWORK_ID);
            if (marketsBtn.getItems().contains(ergoMarketsMenuItem)) {
                marketsBtn.getItems().remove(ergoMarketsMenuItem);
            }
            updateMarketOptions.run();

            if (getMarketInterface() == null) {
                Alert marketAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall '" + ErgoMarkets.NAME + "' in the Ergo Network to use this feature.", ButtonType.OK);
                marketAlert.setGraphic(IconButton.getIconView(ErgoMarkets.getAppIcon(), 75));
                marketAlert.initOwner(walletStage);
                marketAlert.show();
            }

        });

        MenuItem marketsNullMenuItem = new MenuItem("(none)");
        marketsNullMenuItem.setId("rowBtn");
        marketsNullMenuItem.setOnAction(e -> {
            setMarketsId(null);
            if (!marketsBtn.getItems().contains(ergoMarketsMenuItem)) {
                marketsBtn.getItems().add(ergoMarketsMenuItem);
            }
            updateMarketOptions.run();
        });

        if (getMarketInterface() == null) {
            marketsBtn.getItems().addAll(marketsNullMenuItem, ergoMarketsMenuItem);
        } else {
            marketsBtn.getItems().add(marketsNullMenuItem);
            updateMarketOptions.run();
        }

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
        updateBox.setPadding(new Insets(2));
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

        Scene openWalletScene = new Scene(bodyVBox, getStageWidth(), getStageHeight());

        layoutBox.prefWidthProperty().bind(openWalletScene.widthProperty().subtract(30));

        addButton.setOnAction(e -> {
            addressesData.addAddress();
        });

        HBox.setHgrow(layoutBox, Priority.ALWAYS);

        scrollPane.prefViewportWidthProperty().bind(openWalletScene.widthProperty().subtract(10));
        scrollPane.prefViewportHeightProperty().bind(openWalletScene.heightProperty().subtract(titleBox.heightProperty()).subtract(menuBar.heightProperty()).subtract(updateBox.heightProperty()).subtract(summaryBox.heightProperty()).subtract(5));

        addressesData.selectedAddressDataProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                sendTip.setText("Select address");
            } else {
                sendTip.setText("Send");
            }
        });

        sendButton.setOnAction((actionEvent) -> {
            if (getNodeInterface() == null) {
                Alert nodeAlert = new Alert(AlertType.NONE, "Attention:\n\nInstall and enable '" + ErgoNodes.NAME + "' to use this feature.", ButtonType.OK);
                nodeAlert.setGraphic(IconButton.getIconView(ErgoNodes.getAppIcon(), alertImageWidth));
                nodeAlert.initOwner(walletStage);
                nodeAlert.show();
            } else {
                if (addressesData.selectedAddressDataProperty().get() != null) {
                    Scene sendScene = addressesData.getSendScene(openWalletScene, walletStage);
                    if (sendScene != null) {
                        walletStage.setScene(sendScene);
                        Rectangle rect = getNetworksData().getMaximumWindowBounds();
                        ResizeHelper.addResizeListener(walletStage, MIN_WIDTH, MIN_HEIGHT, rect.getWidth(), rect.getHeight());
                    }
                }
            }

        });
        openWalletScene.focusOwnerProperty().addListener((e) -> {
            if (openWalletScene.focusOwnerProperty().get() instanceof AddressData) {
                AddressData addressData = (AddressData) openWalletScene.focusOwnerProperty().get();

                addressesData.selectedAddressDataProperty().set(addressData);
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
                            addressesData.selectedAddressDataProperty().set(null);
                            sendButton.setId("menuBtnDisabled");
                            sendButton.setDisable(true);

                        }
                    } else {
                        addressesData.selectedAddressDataProperty().set(null);
                        sendButton.setId("menuBtnDisabled");
                        sendButton.setDisable(true);
                    }
                } else {
                    addressesData.selectedAddressDataProperty().set(null);
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

        addressesData.selectedMarketData().addListener((obs, oldVal, newVal) -> {
            setSelectedMarketId(newVal == null ? null : newVal.getId());
        });

        walletStage.setOnCloseRequest(event -> {

            addressesData.getTotalDoubleProperty().removeListener(totalListener);
            addressesData.shutdown();
            m_isOpen = false;
        });

        openWalletScene.getStylesheets().add("/css/startWindow.css");
        closeBtn.setOnAction(closeEvent -> {
            m_isOpen = false;
            addressesData.getTotalDoubleProperty().removeListener(totalListener);
            addressesData.shutdown();
            walletStage.close();

        });

        m_ergoWallet.shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
            closeBtn.fire();
        });

        return openWalletScene;

    }

    public JsonObject getMarketData() {
        JsonObject json = new JsonObject();
        json.addProperty("subject", App.GET_DATA);
        return json;
    }

    public String getMarketsId() {
        return m_marketsId;
    }

    public String getSelectedMarketId() {
        return m_selectedMarketId;
    }

    public String getNodesId() {
        return m_nodesId;
    }

    public String getSelectedNodeId() {
        return m_selectedNodeId;
    }

    public NoteInterface getNodeInterface() {
        return m_nodesId == null ? null : m_ergoWallet.getErgoNetworkData().getNetwork(m_nodesId);
    }

    public String getExplorerId() {
        return m_explorerId;
    }

    private NoteInterface getExplorerInterface() {

        return m_explorerId == null ? null : m_ergoWallet.getErgoNetworkData().getNetwork(m_explorerId);
    }

    private void setNodesId(String nodesId) {
        m_nodesId = nodesId;
        m_selectedNodeId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setExplorer(String explorerId, long period) {
        m_explorerId = explorerId;

        m_explorerTimePeriod = 15;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setMarketsId(String marketsId) {
        m_marketsId = marketsId;
        m_selectedMarketId = null;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setSelectedMarketId(String id) {

        m_selectedMarketId = id;
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
