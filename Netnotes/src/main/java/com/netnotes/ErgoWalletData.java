package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
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
import javafx.collections.ListChangeListener;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoWalletData extends Network implements NoteInterface {

    public final static double MIN_WIDTH = 400;
    public final static double MIN_HEIGHT = 275;
  //  private long m_explorerTimePeriod = 15;


    private File logFile = new File("netnotes-log.txt");
    private File m_walletFile = null;
    // private File m_walletDirectory;

    private NetworkType m_networkType = NetworkType.MAINNET;

    // private String m_name;
    private String m_nodesId;
    private String m_explorerId;
    private String m_marketsId;
    private boolean m_isErgoTokens;

    private long m_cyclePeriod = 7;
    private TimeUnit m_cycleTimeUnit = TimeUnit.SECONDS;

    private String m_quoteTransactionCurrency = "USD";

    private ErgoWallets m_ergoWallet;

    // private ErgoWallet m_ergoWallet;
    public ErgoWalletData(String id, String name, File walletFile, String nodesId, String explorerId, String marketsId, boolean ergoTokensEnabled, NetworkType networkType, ErgoWallets ergoWallet) {
        super(null, name, id, ergoWallet);

        m_walletFile = walletFile;
        m_networkType = networkType;
        m_isErgoTokens = ergoTokensEnabled;
        m_nodesId = nodesId;
        m_explorerId = explorerId;
        m_marketsId = marketsId;
        m_ergoWallet = ergoWallet;

        setIconStyle(IconStyle.ROW);
        getLastUpdated().set(LocalDateTime.now());
    }   

    public ErgoWalletData(String id, String name, JsonObject jsonObject, ErgoWallets ergoWallet) {
        super(null, name, id, ergoWallet);

        m_ergoWallet = ergoWallet;

       

        JsonElement fileLocationElement = jsonObject.get("file");
        JsonElement stageElement = jsonObject.get("stage");
        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement nodeIdElement = jsonObject.get("nodeId");
        JsonElement explorerElement = jsonObject.get("explorerId");
        JsonElement marketElement = jsonObject.get("marketId");
        JsonElement ergoTokensElement = jsonObject.get("isErgoTokens");

        m_isErgoTokens = ergoTokensElement != null && ergoTokensElement.isJsonPrimitive() ? ergoTokensElement.getAsBoolean() : false;
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

        m_nodesId = nodeIdElement != null && nodeIdElement.isJsonPrimitive() ? nodeIdElement.getAsString() : null;
        m_marketsId = marketElement != null && marketElement.isJsonPrimitive() ? marketElement.getAsString() : null;
        m_explorerId =  explorerElement != null && explorerElement.isJsonPrimitive() ? explorerElement.getAsString() : null;
    }
    /*
    public void setMarketObject(JsonObject json) {
        if (json == null) {
            m_marketsId = null;
     
        } else {
            JsonElement marketIdElement = json.get("networkId");
            m_marketsId = marketIdElement != null && marketIdElement.isJsonPrimitive() ? marketIdElement.getAsString() : null;

        }

    }

    public void setExplorerObject(JsonObject json) {
        if (json == null) {
            m_explorerId = null;
     
        } else {
            JsonElement explorerIdElement = json.get("networkId");
            m_explorerId = explorerIdElement != null && explorerIdElement.isJsonPrimitive() ? explorerIdElement.getAsString() : null;
           

        }

    }*/

    public long getCyclePeriod(){
        return m_cyclePeriod;
    }

    public TimeUnit getCycleTimeUnit(){
        return m_cycleTimeUnit;
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
            jsonObject.addProperty("nodeId", m_nodesId);
        }

        if (m_explorerId != null) {
            jsonObject.addProperty("explorerId", m_explorerId);
        }

        if (m_marketsId != null) {
            jsonObject.addProperty("marketId", m_marketsId);
        }
        jsonObject.addProperty("isErgoTokens", m_isErgoTokens);

        return jsonObject;
    }

    @Override
    public void open() {

        openWallet();

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
            walletStage.getIcons().add(ErgoWallets.getSmallAppIcon());
            walletStage.initStyle(StageStyle.UNDECORATED);
            walletStage.setTitle("Wallet file: Enter password");

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), getName() + " - Enter password", closeBtn, walletStage);
            closeBtn.setOnAction(event -> {
                walletStage.close();
                m_isOpen = false;
            });

            Button imageButton = App.createImageButton(ErgoWallets.getAppIcon(), "Wallet");
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

    public ErgoWallets getErgoWallets(){
        return m_ergoWallet;
    }
    
    private Scene getWalletScene(Wallet wallet, Stage walletStage) {
        

        AddressesData addressesData = new AddressesData(FriendlyId.createFriendlyId(), wallet, this, m_networkType, walletStage);
           


        try {
            Files.writeString(logFile.toPath(), "\nshowing wallet stage", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        String title = getName() + " - (" + m_networkType.toString() + ")";

        double imageWidth = 20;
        double alertImageWidth = 75;

        //  PriceChart priceChart = null;
        walletStage.setTitle(title);

        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), title, maximizeBtn, closeBtn, walletStage);

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

        Tooltip nodesTip = new Tooltip("Select node");
        nodesTip.setShowDelay(new javafx.util.Duration(50));
        nodesTip.setFont(App.txtFont);


        BufferedMenuButton nodesMenuBtn = new BufferedMenuButton("/assets/ergoNodes-30.png", imageWidth);
        nodesMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        nodesMenuBtn.setTooltip(nodesTip);



        Runnable updateNodeBtn = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            ErgoNodeData nodeData = addressesData.selectedNodeData().get();
        
            if(nodeData != null && ergoNodes != null){

               nodesTip.setText(nodeData.getName());
            
            }else{
                
                if(ergoNodes == null){
                    nodesTip.setText("(install 'Ergo Nodes')");
                }else{
                    nodesTip.setText("Select node...");
                }
            }
          
            
        };
        
        Runnable getAvailableNodeMenu = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            if(ergoNodes != null){
                ergoNodes.getErgoNodesList().getMenu(nodesMenuBtn, addressesData.selectedNodeData());
                nodesMenuBtn.setId("menuBtn");
            }else{
                nodesMenuBtn.getItems().clear();
                nodesMenuBtn.setId("menuBtnDisabled");
               
            }
            updateNodeBtn.run();
        };

        addressesData.selectedNodeData().addListener((obs, oldval, newval)->{
                updateNodeBtn.run();
    
            setNodesId(newval == null ? null : newval.getId());
           
        });

        
        Tooltip explorerTip = new Tooltip("Select explorer");
        explorerTip.setShowDelay(new javafx.util.Duration(50));
        explorerTip.setFont(App.txtFont);



        BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
        explorerBtn.setPadding(new Insets(2, 0, 0, 2));
        explorerBtn.setTooltip(explorerTip);

        Runnable updateExplorerBtn = () ->{
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

            ErgoExplorerData explorerData = addressesData.selectedExplorerData().get();
           
           
            if(explorerData != null && ergoExplorers != null){
            
                explorerTip.setText("Ergo Explorer: " + explorerData.getName());
                

            }else{
                 
                if(ergoExplorers == null){
                    explorerTip.setText("(install 'Ergo Explorer')");
                }else{
                    explorerTip.setText("Select Explorer...");
                }
            }
            
        };
        Runnable getAvailableExplorerMenu = () ->{
        
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            if(ergoExplorers != null){
                explorerBtn.setId("menuBtn");
                ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, addressesData.selectedExplorerData());
            }else{
                 explorerBtn.getItems().clear();
                 explorerBtn.setId("menuBtnDisabled");
               
            }
            updateExplorerBtn.run();
        };    

        addressesData.selectedExplorerData().addListener((obs, oldval, newval)->{
            setExplorer(newval == null ? null : newval.getId());
            updateExplorerBtn.run();
        });

        Tooltip marketsTip = new Tooltip("Select market");
        marketsTip.setShowDelay(new javafx.util.Duration(50));
        marketsTip.setFont(App.txtFont);

        BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);

  
         Runnable updateMarketsBtn = () ->{
            ErgoMarketsData marketsData = addressesData.selectedMarketData().get();
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
      
          
            if(marketsData != null && ergoMarkets != null){
                
                marketsTip.setText(marketsData.getName());
                marketsData.start();
            }else{
               
                if(ergoMarkets == null){
                    marketsTip.setText("(install 'Ergo Markets')");
                }else{
                    marketsTip.setText("Select market...");
                }
            }
          
        };

        Runnable getAvailableMarketsMenu = ()->{
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
            if(ergoMarkets != null){
                 marketsBtn.setId("menuBtn");
                
                ergoMarkets.getErgoMarketsList().getMenu(marketsBtn, addressesData.selectedMarketData());
            }else{
                marketsBtn.getItems().clear();
                marketsBtn.setId("menuBtnDisabled");
            }
            updateMarketsBtn.run();
        };

        
       

        Tooltip tokensTip = new Tooltip("Ergo Tokens");
        tokensTip.setShowDelay(new javafx.util.Duration(50));
        tokensTip.setFont(App.txtFont);


        BufferedMenuButton tokensMenuBtn = new BufferedMenuButton("/assets/diamond-30.png", imageWidth);
        tokensMenuBtn.setPadding(new Insets(2, 0, 0, 0));
        tokensMenuBtn.setTooltip(tokensTip);

        

        Runnable updateTokensMenu = ()->{
            tokensMenuBtn.getItems().clear();
            ErgoTokens ergoTokens = (ErgoTokens) m_ergoWallet.getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);  
            boolean isEnabled = addressesData.isErgoTokensProperty().get();

            if(ergoTokens != null){
                tokensMenuBtn.setId("menuBtn");
                MenuItem tokensEnabledItem = new MenuItem("Enabled" + (isEnabled ? " (selected)" : ""));
                tokensEnabledItem.setOnAction(e->{
                    addressesData.isErgoTokensProperty().set(true);
                });
                

                MenuItem tokensDisabledItem = new MenuItem("Disabled" + (isEnabled ? "" : " (selected)"));
                tokensDisabledItem.setOnAction(e->{
                    addressesData.isErgoTokensProperty().set(false);
                });

                if(isEnabled){
                    tokensTip.setText("Ergo Tokens: Enabled");
                    tokensEnabledItem.setId("selectedMenuItem");
                }else{
                    tokensTip.setText("Ergo Tokens: Disabled");
                    tokensDisabledItem.setId("selectedMenuItem");
                }
                tokensMenuBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
            }else{
                tokensMenuBtn.setId("menuBtnDisabled");
                MenuItem tokensInstallItem = new MenuItem("(install 'Ergo Tokens')");
                tokensInstallItem.setOnAction(e->{
                    m_ergoWallet.getErgoNetworkData().showwManageStage();
                });
                tokensTip.setText("(install 'Ergo Tokens')");
            }
           
        };
   
        addressesData.isErgoTokensProperty().addListener((obs,oldval,newval)->{
            setIsErgoTokens(newval);
            updateTokensMenu.run();
        });

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

        Region seperator3 = new Region();
        seperator3.setMinWidth(1);
        seperator3.setId("vSeperatorGradient");
        VBox.setVgrow(seperator3, Priority.ALWAYS);

        rightSideMenu.getChildren().addAll(nodesMenuBtn, seperator1, explorerBtn, seperator2, marketsBtn, seperator3, tokensMenuBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 0, 0, 10));
        rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

   


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
        TextField totalField = new TextField();
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

        m_ergoWallet.getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
            getAvailableNodeMenu.run();
            getAvailableExplorerMenu.run();
            getAvailableMarketsMenu.run();
            updateTokensMenu.run();
        });

        getAvailableExplorerMenu.run();
        getAvailableNodeMenu.run();
        getAvailableMarketsMenu.run();
        updateTokensMenu.run();

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

                sendButton.setId("menuBtn");
                sendButton.setDisable(false);
                
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

        Runnable calculateTotal = () ->{
            ErgoMarketsData ergoMarketData = addressesData.selectedMarketData().get();
            ErgoAmount totalErgoAmount = addressesData.totalErgoAmountProperty().get();
            
            String totalString = totalErgoAmount == null ? "-" : totalErgoAmount.toString();

            PriceQuote priceQuote = ergoMarketData == null ? null : ergoMarketData.priceQuoteProperty().get(); 
            
            if(priceQuote != null && totalErgoAmount != null){
                double totalPrice = priceQuote.getAmount() * totalErgoAmount.getDoubleAmount();
                Platform.runLater(() ->totalField.setText(totalString + " (" + Utils.formatCryptoString(totalPrice, m_quoteTransactionCurrency, true) + ")"));
            }else{
                Platform.runLater(() ->totalField.setText(totalString + " (" + Utils.currencySymbol(m_quoteTransactionCurrency)+ "-.--)"));
           
            }

            Platform.runLater(() -> lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now())));
        };

        addressesData.totalErgoAmountProperty().addListener((obs, oldval, newval)-> {
            
            try {
                Files.writeString(new File("totalErgoAmount.txt").toPath(), "\n" + (newval != null ? newval.getLongAmount() : "null"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
             
            }
            calculateTotal.run();
        });
        
        ChangeListener<PriceQuote> quoteListener = (obs, oldVal, newVal) -> calculateTotal.run();


        addressesData.selectedMarketData().addListener((obs, oldval, newval)->{
            setMarketsId(newval == null ? null : newval.getId());
            if(oldval != null){
                oldval.priceQuoteProperty().removeListener(quoteListener);
                oldval.shutdown();
            }
            updateMarketsBtn.run();
            if(newval != null)
            {
                newval.priceQuoteProperty().addListener(quoteListener);
            }
        });

        if(addressesData.selectedMarketData().get() != null){
            addressesData.selectedMarketData().get().priceQuoteProperty().addListener(quoteListener);
        }

        calculateTotal.run();

        walletStage.setOnCloseRequest(event -> {

           
            addressesData.shutdown();
            m_isOpen = false;
        });

        openWalletScene.getStylesheets().add("/css/startWindow.css");
        closeBtn.setOnAction(closeEvent -> {
            m_isOpen = false;
         
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

  

    public String getNodesId() {
        return m_nodesId;
    }


    public NoteInterface getNodeInterface() {
        return m_nodesId == null ? null : m_ergoWallet.getErgoNetworkData().getNetwork(m_nodesId);
    }

    public String getExplorerId() {
        return m_explorerId;
    }


    private void setNodesId(String nodesId) {
        m_nodesId = nodesId;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setExplorer(String explorerId) {
        m_explorerId = explorerId;

       // m_explorerTimePeriod = 15;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setMarketsId(String marketsId) {
        m_marketsId = marketsId;
        getLastUpdated().set(LocalDateTime.now());
    }

    private void setIsErgoTokens(boolean value){
        m_isErgoTokens = value;
        getLastUpdated().set(LocalDateTime.now());
    }

    public boolean isErgoTokens(){
        return m_isErgoTokens;
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle == IconStyle.ROW ? ErgoWallets.getSmallAppIcon() : ErgoWallets.getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        iconButton.setButtonId(getNetworkId());
        return iconButton;
    }
}
