package com.netnotes;

import java.io.File;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.InputBoxesSelectionException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.Parameters;
import org.ergoplatform.appkit.SignedTransaction;
import org.ergoplatform.appkit.UnsignedTransaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
import javafx.stage.Stage;

public class AddressesData {

    private File logFile;
    private NetworkType m_networkType;
    private VBox m_addressBox;
    private Wallet m_wallet;
    private WalletData m_walletData;
    private Stage m_walletStage;

    private SimpleObjectProperty<AddressData> m_selectedAddressData = new SimpleObjectProperty<AddressData>(null);
    private SimpleDoubleProperty m_totalQuote = new SimpleDoubleProperty(0);

    private ArrayList<AddressData> m_addressDataList = new ArrayList<AddressData>();

    private BufferedMenuButton m_marketOptionsBtn;
    private BufferedMenuButton m_nodeOptionsButton;
    private ErgoMarketsList m_ergoMarketsList = null;

    private ScheduledExecutorService m_balanceExecutor = null;

    private SimpleObjectProperty<MarketsData> m_selectedMarketData = new SimpleObjectProperty<MarketsData>(null);
    private SimpleObjectProperty<ExplorerData> m_selectedExplorerData = new SimpleObjectProperty<ExplorerData>(null);
    private SimpleObjectProperty<ErgoNodeData> m_selectedNodeData = new SimpleObjectProperty<ErgoNodeData>(null);

    public AddressesData(String id, Wallet wallet, WalletData walletData, ExplorerData explorerData, NetworkType networkType, Stage walletStage) {
        logFile = new File("addressesData-" + walletData.getNetworkId() + ".txt");
        m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;
        m_walletStage = walletStage;

        updateExplorerData(explorerData);
        setupNodes();
        setupMarkets();

        m_wallet.myAddresses.forEach((index, name) -> {

            try {

                Address address = wallet.publicAddress(m_networkType, index);
                AddressData addressData = new AddressData(name, index, address, m_networkType, this);

                m_addressDataList.add(addressData);
                addressData.addUpdateListener((a, b, c) -> {
                    double total = calculateCurrentTotal();

                    m_totalQuote.set(total);
                });
                addressData.addCmdListener((obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.get("subject") != null) {
                        String subject = newVal.get("subject").getAsString();
                        switch (subject) {
                            case "SEND":

                                m_selectedAddressData.set(addressData);
                                m_walletStage.setScene(getSendScene(m_walletStage.getScene(), m_walletStage));
                                m_walletStage.show();
                                closeAll();
                                break;
                        }
                    }
                });
            } catch (Failure e) {

            }

        });

        m_addressBox = new VBox();

        //"▼" m_marketOptionsBtn.setFont(Font.font("Arial", 12));
        m_marketOptionsBtn.setGraphic(IconButton.getIconView(new Image("/assets/caret-down-15.png"), 10));
        updateAddressBox();
    }

    public WalletData getWalletData() {
        return m_walletData;
    }

    public SimpleObjectProperty<ExplorerData> selectedExplorerDataProperty() {
        return m_selectedExplorerData;
    }

    public SimpleObjectProperty<ErgoNodeData> selectedNodeDataProperty() {
        return m_selectedNodeData;
    }

    public SimpleObjectProperty<MarketsData> selectedMarketData() {
        return m_selectedMarketData;
    }

    private void setupNodes() {
        m_nodeOptionsButton = new BufferedMenuButton();
        m_nodeOptionsButton.setGraphic(IconButton.getIconView(new Image("/assets/caret-down-15.png"), 10));
    }

    private void setupMarkets() {
        m_marketOptionsBtn = new BufferedMenuButton();
        updateMarketsList();
        String selectedMarketsDataId = m_walletData.getSelectedMarketId();
        if (selectedMarketsDataId != null && m_ergoMarketsList != null) {
            MarketsData selectedMarketsData = m_ergoMarketsList.getMarketsData(selectedMarketsDataId);
            if (selectedMarketsData != null) {
                m_selectedMarketData.set(selectedMarketsData);
                selectedMarketsData.start();
            }
        }
    }

    public void updateExplorerData(ExplorerData explorerData) {
        m_selectedExplorerData.set(explorerData);
        if (explorerData == null) {
            stopBalanceUpdates();
        } else {
            startBalanceUpdates();
        }
    }

    public void closeAll() {
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            addressData.close();
        }
    }

    public SimpleObjectProperty<AddressData> selectedAddressDataProperty() {
        return m_selectedAddressData;
    }

    public SimpleDoubleProperty getTotalDoubleProperty() {
        return m_totalQuote;
    }

    public void addAddress() {
        String addressName = App.showGetTextInput("Address name", "Address", App.branchImg);
        if (addressName != null) {
            int nextAddressIndex = m_wallet.nextAddressIndex();
            m_wallet.myAddresses.put(nextAddressIndex, addressName);
            AddressData addressData = null;
            try {

                Address address = m_wallet.publicAddress(m_networkType, nextAddressIndex);
                addressData = new AddressData(addressName, nextAddressIndex, address, m_networkType, this);

            } catch (Failure e1) {

                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                a.show();
            }
            if (addressData != null) {
                m_addressDataList.add(addressData);
                addressData.getLastUpdated().addListener((a, b, c) -> {

                    m_totalQuote.set(calculateCurrentTotal());
                });
                updateAddressBox();
            }
        }
    }

    public VBox getAddressBox() {

        updateAddressBox();

        return m_addressBox;
    }

    private void updateAddressBox() {
        m_addressBox.getChildren().clear();
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            addressData.prefWidthProperty().bind(m_addressBox.widthProperty());

            m_addressBox.getChildren().add(addressData);
        }
    }

    public void updateBalance() {
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);

            addressData.updateBalance();
        }
    }

    public void startBalanceUpdates() {
        ExplorerData explorerData = m_selectedExplorerData.get();
        if (explorerData != null) {
            try {
                if (m_balanceExecutor != null) {
                    stopBalanceUpdates();
                }

                m_balanceExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });

                m_balanceExecutor.scheduleAtFixedRate(() -> Platform.runLater(() -> updateBalance()), 0, explorerData.getPeriod(), explorerData.getTimeUnit());
            } catch (Exception e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        } else {
            m_balanceExecutor.shutdown();
        }

    }

    public void stopBalanceUpdates() {
        if (m_balanceExecutor != null) {
            m_balanceExecutor.shutdown();
            m_balanceExecutor = null;
        }
    }

    public void shutdown() {
        stopBalanceUpdates();
    }

    public void updateMarketsList() {
        NoteInterface noteInterface = m_walletData.getMarketInterface();

        if (noteInterface != null && noteInterface instanceof ErgoMarkets) {
            ErgoMarkets ergoMarkets = (ErgoMarkets) noteInterface;
            SecretKey secretKey = m_walletData.getNetworksData().appKeyProperty().get();
            if (m_ergoMarketsList != null) {
                m_ergoMarketsList.shutdown();
            }
            m_ergoMarketsList = new ErgoMarketsList(secretKey, ergoMarkets);
            updateMarketOptions();
        } else {
            if (m_ergoMarketsList != null) {
                m_ergoMarketsList.shutdown();
                m_ergoMarketsList = null;
            }
            updateMarketOptions();
        }
    }

    public BufferedMenuButton getMarketOptionsButton() {
        return m_marketOptionsBtn;
    }

    public MenuButton getNodeOptionsButton() {
        return m_nodeOptionsButton;
    }

    public boolean updateSelectedMarket(MarketsData marketsData) {
        MarketsData previousSelectedMarketsData = m_selectedMarketData.get();

        if (marketsData == null && previousSelectedMarketsData == null) {
            return false;
        }

        m_selectedMarketData.set(marketsData);

        if (previousSelectedMarketsData != null) {
            if (marketsData != null) {
                if (previousSelectedMarketsData.getId().equals(marketsData.getId()) && previousSelectedMarketsData.getMarketId().equals(marketsData.getMarketId())) {
                    return false;
                }
            }
            previousSelectedMarketsData.shutdown();

        }

        if (marketsData != null) {
            marketsData.start();
        }

        return true;

        // return false;
    }

    private void updateMarketOptions() {
        m_marketOptionsBtn.getItems().clear();
        if (m_ergoMarketsList != null) {
            ArrayList<MarketsData> dataList = m_ergoMarketsList.getMarketsDataList();
            int size = dataList.size();

            if (size > 0) {
                String selectedId = m_walletData.getSelectedMarketId();
                for (int i = 0; i < size; i++) {

                    MarketsData marketsData = dataList.get(i);

                    String id = marketsData.getId();
                    String type = marketsData.getUpdateType();
                    String value = marketsData.getUpdateValue();
                    String marketId = marketsData.getMarketId();

                    MenuItem menuItem = new MenuItem((selectedId != null && selectedId.equals(id) ? "\u25CF " : "") + (type.equals(MarketsData.REALTIME) ? "Real-time: " : "") + value + (type.equals(MarketsData.POLLED) ? "s" : ""));
                    menuItem.setId("rowBtn");
                    menuItem.setGraphic(IconButton.getIconView(new InstallableIcon(m_walletData.getNetworksData(), marketId, true).getIcon(), 30));
                    menuItem.setOnAction(e -> {
                        if (updateSelectedMarket(marketsData)) {
                            updateMarketOptions();
                        }
                    });

                    m_marketOptionsBtn.getItems().add(menuItem);

                }
            }
        } else {
            MenuItem menuItem = new MenuItem("   (not installed)");
            m_marketOptionsBtn.getItems().add(menuItem);

        }

    }

    public double calculateCurrentTotal() {
        double total = 0;
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            total += addressData.getTotalAmountPrice();

        }

        return total;
    }

    public Scene getSendScene(Scene parentScene, Stage parentStage) {
        NoteInterface networkInterface = m_walletData.getNodeInterface();
        if (networkInterface == null) {
            return null;
        }

        String oldStageName = parentStage.getTitle();

        String stageName = "Ergo Wallet - Send - " + networkInterface.getName() + " - (" + m_networkType + ")";

        parentStage.setTitle(stageName);

        VBox layoutBox = new VBox();
        Scene sendScene = new Scene(layoutBox, 800, 600);
        sendScene.getStylesheets().add("/css/startWindow.css");

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {
            parentStage.close();
        });

        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), stageName, maximizeBtn, closeBtn, parentStage);

        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        Button backButton = new Button();
        backButton.setGraphic(IconButton.getIconView(new Image("/assets/return-back-up-30.png"), 30));
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        backButton.setOnAction(e -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
            // ResizeHelper.addResizeListener(parentStage, WalletData.MIN_WIDTH, WalletData.MIN_HEIGHT, m_walletData.getMaxWidth(), m_walletData.getMaxHeight());
        });

        Tooltip nodeTip = new Tooltip(selectedNodeDataProperty().get() == null ? "Node unavailable" : selectedNodeDataProperty().get().getName());
        nodeTip.setShowDelay(new javafx.util.Duration(100));
        nodeTip.setFont(App.txtFont);

        MenuButton nodeMenuBtn = new MenuButton();
        nodeMenuBtn.setGraphic(selectedNodeDataProperty().get() == null ? IconButton.getIconView(new Image("/assets/node-30.png"), 30) : IconButton.getIconView(selectedNodeDataProperty().get().getIcon(), 30));
        nodeMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        nodeMenuBtn.setTooltip(nodeTip);

        NoteInterface explorerInterface = m_selectedExplorerData.get() != null ? m_selectedExplorerData.get().getExplorerInterface() : null;

        Tooltip explorerTip = new Tooltip(explorerInterface == null ? "Explorer unavailable" : explorerInterface.getName());
        explorerTip.setShowDelay(new javafx.util.Duration(100));
        explorerTip.setFont(App.txtFont);

        MenuButton explorerBtn = new MenuButton();
        explorerBtn.setGraphic(explorerInterface == null ? IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30) : IconButton.getIconView(new InstallableIcon(getWalletData().getNetworksData(), explorerInterface.getNetworkId(), true).getIcon(), 30));
        explorerBtn.setPadding(new Insets(2, 0, 0, 0));
        explorerBtn.setTooltip(explorerTip);

        Tooltip marketsTip = new Tooltip("Markets unavailable");
        marketsTip.setShowDelay(new javafx.util.Duration(100));
        marketsTip.setFont(App.txtFont);

        MenuButton marketsBtn = new MenuButton();
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);

        Runnable setMarketTipText = () -> {
            //
            MarketsData marketsData = selectedMarketData().get();
            if (marketsData != null) {
                marketsTip.setText(MarketsData.getFriendlyUpdateTypeName(marketsData.getUpdateType()) + ": " + marketsData.getUpdateValue());
                marketsBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getWalletData().getNetworksData(), selectedMarketData().get().getMarketId(), true).getIcon(), 30));
            } else {
                marketsTip.setText("Markets unavailable");
                marketsBtn.setGraphic(IconButton.getIconView(new Image("/assets/exchange-30.png"), 30));
            }
        };

        setMarketTipText.run();

        selectedMarketData().addListener((obs, oldVal, newval) -> setMarketTipText.run());

        HBox rightSideMenu = new HBox(nodeMenuBtn, explorerBtn, marketsBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox menuBar = new HBox(backButton, spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        Text headingText = new Text("Send");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        Text amountCaret = new Text("Amount ");
        amountCaret.setFont(App.txtFont);
        amountCaret.setFill(Color.WHITE);

        Button addTxBtn = new Button("Add", IconButton.getIconView(new Image("/assets/add-outline-white-40.png"), 18));
        addTxBtn.setId("menuBtnDisabled");
        addTxBtn.setFont(App.txtFont);
        addTxBtn.setContentDisplay(ContentDisplay.LEFT);
        addTxBtn.setDisable(true);
        addTxBtn.setPadding(new Insets(3, 10, 3, 10));

        Button sendButton = new Button("Send");
        ImageView amountNotificationIcon = IconButton.getIconView(new Image("/assets/notificationIcon.png"), 40);

        AmountBoxes amountBoxes = new AmountBoxes(m_selectedAddressData.get(), amountNotificationIcon, amountCaret);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        ImageView fromNotificationIcon = IconButton.getIconView(new Image("/assets/notificationIcon.png"), 40);

        Text fromCaret = new Text("From   ");
        fromCaret.setFont(App.txtFont);
        fromCaret.setFill(Color.WHITE);

        MenuButton fromAddressBtn = new MenuButton("");
        fromAddressBtn.setId("rowBtn");
        fromAddressBtn.textProperty().bind(Bindings.concat(selectedAddressDataProperty().asString()));
        fromAddressBtn.setContentDisplay(ContentDisplay.LEFT);
        fromAddressBtn.setAlignment(Pos.CENTER_LEFT);

        for (AddressData addressItem : m_addressDataList) {

            MenuItem addressMenuItem = new MenuItem(addressItem.getAddressString());
            addressMenuItem.textProperty().bind(addressItem.textProperty());
            Image addressImage = addressItem.getImageProperty().get();
            addressMenuItem.setGraphic(IconButton.getIconView(addressImage, addressImage.getWidth()));

            addressItem.getImageProperty().addListener((obs, oldVal, newVal) -> {
                addressMenuItem.setGraphic(IconButton.getIconView(newVal, newVal.getWidth()));
            });

            fromAddressBtn.getItems().add(addressMenuItem);

            addressMenuItem.setOnAction(actionEvent -> {
                if (!(addressItem.getAddressString().equals(m_selectedAddressData.get().getAddressString()))) {
                    m_selectedAddressData.set(addressItem);
                }
            });
        }

        // fromAddressBtn.setPadding(new Insets(2, 5, 2, 0));
        Image fromImg = selectedAddressDataProperty().get().getImageProperty().get();
        fromAddressBtn.setGraphic(IconButton.getIconView(fromImg, fromImg.getWidth()));

        selectedAddressDataProperty().get().getImageProperty().addListener(e -> {
            Image img = selectedAddressDataProperty().get().getImageProperty().get();
            fromAddressBtn.setGraphic(IconButton.getIconView(img, img.getWidth()));
        });

        HBox toAddressBox = new HBox();
        toAddressBox.setPadding(new Insets(3, 15, 5, 0));
        toAddressBox.setAlignment(Pos.CENTER_LEFT);
        Text toCaret = new Text("To     ");
        toCaret.setFont(App.txtFont);
        toCaret.setFill(Color.WHITE);

        Button toEnterButton = new Button("[ ENTER ]");
        toEnterButton.setFont(App.txtFont);
        toEnterButton.setId("toolBtn");

        AddressButton toAddressBtn = new AddressButton("", m_networkType);
        toAddressBtn.setId("rowBtn");

        toAddressBtn.setContentDisplay(ContentDisplay.LEFT);
        toAddressBtn.setAlignment(Pos.CENTER_LEFT);

        toAddressBtn.setPadding(new Insets(0, 10, 0, 10));

        ArrayList<PriceTransaction> transactionList = new ArrayList<PriceTransaction>();

        TextField toTextField = new TextField();

        toTextField.setMaxHeight(40);
        toTextField.setId("formField");
        toTextField.setPadding(new Insets(3, 10, 0, 0));
        HBox.setHgrow(toTextField, Priority.ALWAYS);

        toAddressBtn.setOnAction(e -> {

            toAddressBox.getChildren().remove(toAddressBtn);
            toAddressBox.getChildren().add(toTextField);

            Platform.runLater(() -> toTextField.requestFocus());

        });

        ImageView toNotificationIcon = IconButton.getIconView(new Image("/assets/notificationIcon.png"), 40);

        toAddressBox.getChildren().addAll(toNotificationIcon, toCaret, toAddressBtn);

        toTextField.textProperty().addListener((obs, old, newVal) -> {
            String text = newVal.trim();
            if (text.length() > 5) {
                if (!toAddressBox.getChildren().contains(toEnterButton)) {
                    toAddressBox.getChildren().add(toEnterButton);
                }

                toAddressBtn.setAddressByString(text, onVerified -> {

                    Object object = onVerified.getSource().getValue();

                    if (object != null && (Boolean) object) {

                        toAddressBox.getChildren().remove(toTextField);
                        if (toAddressBox.getChildren().contains(toEnterButton)) {
                            toAddressBox.getChildren().remove(toEnterButton);
                        }
                        toAddressBox.getChildren().add(toAddressBtn);
                    }
                });
            }

        });

        toTextField.setOnKeyPressed((keyEvent) -> {
            KeyCode keyCode = keyEvent.getCode();
            if (keyCode == KeyCode.ENTER) {
                String text = toTextField.getText();

                toAddressBox.getChildren().remove(toTextField);

                if (toAddressBox.getChildren().contains(toEnterButton)) {
                    toAddressBox.getChildren().remove(toEnterButton);
                }
                toAddressBox.getChildren().add(toAddressBtn);
            }
        });

        toTextField.focusedProperty().addListener((obs, old, newPropertyValue) -> {

            if (newPropertyValue) {

            } else {

                toAddressBox.getChildren().remove(toTextField);
                if (toAddressBox.getChildren().contains(toEnterButton)) {
                    toAddressBox.getChildren().remove(toEnterButton);
                }
                toAddressBox.getChildren().add(toAddressBtn);

                /* NoteInterface explorerInterface = m_walletData.getExplorerInterface();

                    if (explorerInterface != null) {

                    } */
            }
        });

        HBox fromAddressBox = new HBox(fromNotificationIcon, fromCaret, fromAddressBtn);
        fromAddressBox.setPadding(new Insets(7, 15, 2, 0));
        HBox.setHgrow(fromAddressBox, Priority.ALWAYS);
        fromAddressBox.setAlignment(Pos.CENTER_LEFT);

        /*    Region amountRegion = new Region();
        amountRegion.setPrefWidth(10);*/
        amountBoxes.setPadding(new Insets(2, 15, 5, 0));

        // amountBox.currentAmountProperty();
        sendButton.setGraphic(IconButton.getIconView(new Image("/assets/arrow-send-white-30.png"), 30));
        sendButton.setFont(App.txtFont);
        sendButton.setId("menuBtnDisabled");
        sendButton.setDisable(true);
        sendButton.setUserData("sendButton");
        sendButton.setContentDisplay(ContentDisplay.LEFT);
        sendButton.setPadding(new Insets(3, 15, 3, 15));
        sendButton.setOnAction(e -> {
            // sendErg(0, null, null, 0, null, null, null);
        });

        Region sendBoxSpacer = new Region();
        HBox.setHgrow(sendBoxSpacer, Priority.ALWAYS);

        HBox sendBox = new HBox(sendBoxSpacer, sendButton);
        HBox.setHgrow(sendBox, Priority.ALWAYS);
        sendBox.setPadding(new Insets(5, 10, 10, 0));

        VBox scrollBodyBox = new VBox();

        ScrollPane scrollPane = new ScrollPane(scrollBodyBox);
        scrollPane.setId("bodyBox");

        HBox scrollPaddingBox = new HBox(scrollPane);
        scrollPaddingBox.setPadding(new Insets(10, 20, 20, 20));

        Region addBoxSpacer = new Region();
        HBox.setHgrow(addBoxSpacer, Priority.ALWAYS);

        HBox addBox = new HBox(addBoxSpacer, addTxBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);
        addBox.setPadding(new Insets(0, 20, 0, 0));

        VBox bodyBox = new VBox(headingBox, fromAddressBox, toAddressBox, amountBoxes, addBox, scrollPaddingBox);
        bodyBox.setId("bodyBox");
        // bodyBox.setPadding(new Insets(5));

        VBox bodyLayoutBox = new VBox(bodyBox);
        bodyLayoutBox.setPadding(new Insets(7, 5, 5, 5));

        VBox footerBox = new VBox(sendBox);
        HBox.setHgrow(footerBox, Priority.ALWAYS);

        HBox paddingBox = new HBox(menuBar);
        HBox.setHgrow(paddingBox, Priority.ALWAYS);
        paddingBox.setPadding(new Insets(0, 5, 0, 5));

        layoutBox.getChildren().addAll(titleBox, paddingBox, bodyLayoutBox, footerBox);

        fromAddressBtn.prefWidthProperty().bind(fromAddressBox.widthProperty().subtract(fromCaret.layoutBoundsProperty().getValue().getWidth()).subtract(30));
        toAddressBtn.prefWidthProperty().bind(fromAddressBox.widthProperty().subtract(fromCaret.layoutBoundsProperty().getValue().getWidth()).subtract(30));
        scrollPane.prefViewportHeightProperty().bind(sendScene.heightProperty().subtract(titleBox.heightProperty()).subtract(menuBar.heightProperty()).subtract(headingBox.heightProperty()).subtract(fromAddressBox.heightProperty()).subtract(toAddressBox.heightProperty()).subtract(amountBoxes.heightProperty()).subtract(footerBox.heightProperty()));
        scrollPane.prefViewportWidthProperty().bind(sendScene.widthProperty());

        return sendScene;
    }

    private String transact(ErgoClient ergoClient, SignedTransaction signedTx) {
        return ergoClient.execute(ctx -> {
            String quoted = ctx.sendTransaction(signedTx);
            return quoted.substring(1, quoted.length() - 1);
        });
    }

    public JsonObject getErgoClientObject(String nodeId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("subject", "GET_CLIENT");
        jsonObject.addProperty("networkType", m_networkType.toString());
        jsonObject.addProperty("nodeId", nodeId);
        return jsonObject;
    }

    private boolean sendErg(long nanoErg, String receipientAddress, Address senderAddress, long fee, String nodeId, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {
        if (receipientAddress != null && senderAddress != null && nodeId != null && fee >= Parameters.MinFee) {
            NoteInterface nodeInterface = m_walletData.getNodeInterface();
            if (nodeInterface != null) {
                return nodeInterface.sendNote(getErgoClientObject(nodeId), (successEvent) -> {
                    WorkerStateEvent workerEvent = successEvent;
                    Object sourceObject = workerEvent.getSource().getValue();
                    if (sourceObject != null) {
                        ErgoClient ergoClient = (ErgoClient) sourceObject;
                        String txId = null;

                        JsonObject txInfoJson = new JsonObject();
                        txInfoJson.addProperty("fee", fee);
                        txInfoJson.addProperty("nanoErg", nanoErg);
                        txInfoJson.addProperty("receipientAddress", receipientAddress);
                        txInfoJson.addProperty("returnAddress", senderAddress.toString());
                        txInfoJson.addProperty("nodeId", nodeId);
                        try {

                            UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(ergoClient,
                                    m_wallet.addressStream(m_networkType).toList(),
                                    Address.create(receipientAddress), nanoErg, fee, senderAddress);

                            txId = transact(ergoClient, ergoClient.execute(ctx -> {
                                try {
                                    return m_wallet.key().sign(ctx, unsignedTx, m_wallet.myAddresses.keySet());
                                } catch (WalletKey.Failure ex) {

                                    txInfoJson.addProperty("unauthorized", ex.toString());
                                    return null;
                                }
                            }));

                            // if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
                        } catch (InputBoxesSelectionException ibsEx) {
                            txInfoJson.addProperty("insufficientFunds", ibsEx.toString());
                        }
                        if (txId != null) {
                            txInfoJson.addProperty("txId", txId);
                        }

                        Utils.returnObject(txInfoJson, onSuccess, null);
                    }
                }, onFailed);
            }
        }
        return false;
    }

}
