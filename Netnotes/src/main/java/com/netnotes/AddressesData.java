package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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

import com.google.gson.JsonObject;

import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
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

import javafx.scene.text.Text;
import javafx.stage.Stage;

public class AddressesData {

    private File logFile = new File("netnotes-log.txt");
    private NetworkType m_networkType;
    private VBox m_addressBox;
    private Wallet m_wallet;
    private ErgoWalletData m_walletData;
    private Stage m_walletStage;

    private SimpleObjectProperty<AddressData> m_selectedAddressData = new SimpleObjectProperty<AddressData>(null);

    private SimpleObjectProperty<ErgoAmount> m_totalErgoAmount = new SimpleObjectProperty<>(null);

    private ScheduledFuture<?> m_lastExecution = null;
    private SimpleObjectProperty<LocalDateTime> m_timeCycle = new SimpleObjectProperty<>(LocalDateTime.now());

    private ArrayList<AddressData> m_addressDataList = new ArrayList<AddressData>();


    private ScheduledExecutorService m_balanceExecutor = null;

    private SimpleObjectProperty<ErgoMarketsData> m_selectedMarketData = new SimpleObjectProperty<ErgoMarketsData>(null);
    private SimpleObjectProperty<ErgoNodeData> m_selectedNodeData = new SimpleObjectProperty<ErgoNodeData>(null);
    private SimpleObjectProperty<ErgoExplorerData> m_selectedExplorerData = new SimpleObjectProperty<ErgoExplorerData>(null);
    private SimpleBooleanProperty m_isErgoTokens = new SimpleBooleanProperty();

    private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);

    private Stage m_promptStage = null;

    public AddressesData(String id, Wallet wallet, ErgoWalletData walletData, NetworkType networkType, Stage walletStage) {
     
        m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;
        m_walletStage = walletStage;

        ErgoNetworkData ergNetData = walletData.getErgoWallets().getErgoNetworkData();


        ErgoNodes ergoNodes = (ErgoNodes) ergNetData.getNetwork(ErgoNodes.NETWORK_ID);
        ErgoExplorers ergoExplorer = (ErgoExplorers) ergNetData.getNetwork(ErgoExplorers.NETWORK_ID);
        ErgoTokens ergoTokens = (ErgoTokens) ergNetData.getNetwork(ErgoTokens.NETWORK_ID);
  
        if(ergoNodes != null){
            m_selectedNodeData.set(ergoNodes.getErgoNodesList().getErgoNodeData(walletData.getNodesId()));
        }
        if(ergoExplorer != null){
            String explorerId = walletData.getExplorerId();
            ErgoExplorerData explorerData = ergoExplorer.getErgoExplorersList().getErgoExplorerData(explorerId);
      
            m_selectedExplorerData.set(explorerData);
        }
        
        m_isErgoTokens.set(ergoTokens == null ? false : walletData.isErgoTokens());

        m_wallet.myAddresses.forEach((index, name) -> {

            try {

                Address address = wallet.publicAddress(m_networkType, index);
                AddressData addressData = new AddressData(name, index, address, m_networkType, this);
                addAddressData(addressData);

            } catch (Failure e) {
                try {
                    Files.writeString(logFile.toPath(), "\nAddressesData - address failure: " + e.toString(),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
      
                }
            }

        });
        calculateCurrentTotal();
        m_addressBox = new VBox();

        
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
            });

        Runnable doUpdate = ()->{
            Platform.runLater(()->updateTimeCycle());
        };
        m_lastExecution = executor.schedule(doUpdate, walletData.getCyclePeriod(), walletData.getCycleTimeUnit());
    
       
        updateAddressBox();
    }

    public SimpleObjectProperty<PriceQuote> currentPriceQuoteProperty(){
        return m_currentQuote;
    }

    public ErgoWalletData getWalletData() {
        return m_walletData;
    }

    public void updateTimeCycle(){
        m_timeCycle.set(LocalDateTime.now());
    }

    public SimpleObjectProperty<LocalDateTime> timeCycleProperty(){
        return m_timeCycle;
    }

    public SimpleObjectProperty<ErgoNodeData> selectedNodeData() {
        return m_selectedNodeData;
    }

    public SimpleObjectProperty<ErgoMarketsData> selectedMarketData() {
        return m_selectedMarketData;
    }

    public SimpleObjectProperty<ErgoExplorerData> selectedExplorerData(){
        return m_selectedExplorerData;
    }

    public SimpleBooleanProperty isErgoTokensProperty(){
        return m_isErgoTokens;
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


    
    public void addAddress() {
        TextField textField = new TextField();
        Button closeBtn = new Button();
        if(m_promptStage == null){
            m_promptStage = new Stage();
            App.showGetTextInput("Address name", "Address name", App.branchImg, textField, closeBtn, m_promptStage);
            closeBtn.setOnAction(e->{
                m_promptStage.close();
                m_promptStage = null;
            });
            m_promptStage.setOnCloseRequest(e->{
                closeBtn.fire();
            });
            textField.setOnKeyPressed(e -> {

                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.ENTER) {
                    String addressName = textField.getText();
                    if (!addressName.equals("")) {
                        int nextAddressIndex = m_wallet.nextAddressIndex();
                        m_wallet.myAddresses.put(nextAddressIndex, addressName);
                        
                        try {

                            Address address = m_wallet.publicAddress(m_networkType, nextAddressIndex);
                            AddressData addressData = new AddressData(addressName, nextAddressIndex, address, m_networkType, this);
                            addAddressData(addressData);
                             updateAddressBox();
                        } catch (Failure e1) {

                            Alert a = new Alert(AlertType.ERROR, e1.toString(), ButtonType.OK);
                            a.showAndWait();
                        }
                  
                    }
                    closeBtn.fire();
                }
            });
        }

    }

    private void addAddressData(AddressData addressData){
        m_addressDataList.add(addressData);
        addressData.ergoAmountProperty().addListener((obs, oldval, newval) ->{
            long oldNanoErgs = oldval == null ? 0 : oldval.getLongAmount();
            
            long newNanoErgs = newval == null ? 0 : newval.getLongAmount();

            if(oldNanoErgs != newNanoErgs){
                calculateCurrentTotal();
            }
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

    public final static long UPDATE_PERIOD = 7;

    public void startBalanceUpdates() {
       
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

            m_balanceExecutor.scheduleAtFixedRate(() -> {
                Platform.runLater(() -> updateBalance());
            }, 0, UPDATE_PERIOD, TimeUnit.SECONDS);
        } catch (Exception e) {
            Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
            a.show();
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

   



    public boolean updateSelectedMarket(ErgoMarketsData marketsData) {
        ErgoMarketsData previousSelectedMarketsData = m_selectedMarketData.get();

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
            m_currentQuote.unbind();
            previousSelectedMarketsData.shutdown();

        }

        if (marketsData != null) {
            m_currentQuote.bind(marketsData.priceQuoteProperty());
            marketsData.start();
        }

        return true;

        // return false;
    }

    
    public void calculateCurrentTotal() {
        SimpleLongProperty totalNanoErgs = new SimpleLongProperty();
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            ErgoAmount ergoAmount = addressData.ergoAmountProperty().get();
            
            totalNanoErgs.set( totalNanoErgs.get() + (ergoAmount == null ? 0 : ergoAmount.getLongAmount()));
            
        }

        m_totalErgoAmount.set(new ErgoAmount(totalNanoErgs.get()));
    }

    public SimpleObjectProperty<ErgoAmount> totalErgoAmountProperty(){
        return m_totalErgoAmount;
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

        HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), stageName, maximizeBtn, closeBtn, parentStage);

        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        Button backButton = new Button();
        backButton.setGraphic(IconButton.getIconView(new Image("/assets/return-back-up-30.png"), App.MENU_BAR_IMAGE_WIDTH));
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        backButton.setOnAction(e -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
            // ResizeHelper.addResizeListener(parentStage, WalletData.MIN_WIDTH, WalletData.MIN_HEIGHT, m_walletData.getMaxWidth(), m_walletData.getMaxHeight());
        });

        Tooltip nodeTip = new Tooltip(selectedNodeData().get() == null ? "Node unavailable" : selectedNodeData().get().getName());
        nodeTip.setShowDelay(new javafx.util.Duration(100));
        nodeTip.setFont(App.txtFont);

        MenuButton nodeMenuBtn = new MenuButton();
        nodeMenuBtn.setGraphic(selectedNodeData().get() == null ? IconButton.getIconView(new Image("/assets/node-30.png"), App.MENU_BAR_IMAGE_WIDTH) : IconButton.getIconView(selectedNodeData().get().getIcon(), App.MENU_BAR_IMAGE_WIDTH));
        nodeMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        nodeMenuBtn.setTooltip(nodeTip);

        

        Tooltip explorerTip = new Tooltip("");
        explorerTip.setShowDelay(new javafx.util.Duration(100));
        explorerTip.setFont(App.txtFont);

        MenuButton explorerBtn = new MenuButton();
        explorerBtn.setGraphic(m_walletData.getExplorerId() != null ? IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), App.MENU_BAR_IMAGE_WIDTH) : IconButton.getIconView(new Image("/assets/ergo-explorer-30.png"), App.MENU_BAR_IMAGE_WIDTH));
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
            ErgoMarketsData marketsData = selectedMarketData().get();
            if (marketsData != null) {
                marketsTip.setText(ErgoMarketsData.getFriendlyUpdateTypeName(marketsData.getUpdateType()) + ": " + marketsData.getUpdateValue());
                marketsBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getWalletData().getNetworksData(), selectedMarketData().get().getMarketId(), true).getIcon(), App.MENU_BAR_IMAGE_WIDTH));
            } else {
                marketsTip.setText("Markets unavailable");
                marketsBtn.setGraphic(IconButton.getIconView(new Image("/assets/exchange-30.png"), App.MENU_BAR_IMAGE_WIDTH));
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

        Button addTxBtn = new Button("Add", IconButton.getIconView(new Image("/assets/add-outline-white-40.png"), App.MENU_BAR_IMAGE_WIDTH));
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
