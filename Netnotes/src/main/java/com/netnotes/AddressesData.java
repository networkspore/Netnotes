package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;

import org.ergoplatform.appkit.Address;

import org.ergoplatform.appkit.NetworkType;

import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

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
    private final SimpleObjectProperty<LocalDateTime> m_timeCycle = new SimpleObjectProperty<>(LocalDateTime.now());

    private ArrayList<AddressData> m_addressDataList = new ArrayList<AddressData>();




    private SimpleObjectProperty<ErgoMarketsData> m_selectedMarketData = new SimpleObjectProperty<ErgoMarketsData>(null);
    private SimpleObjectProperty<ErgoNodeData> m_selectedNodeData = new SimpleObjectProperty<ErgoNodeData>(null);
    private SimpleObjectProperty<ErgoExplorerData> m_selectedExplorerData = new SimpleObjectProperty<ErgoExplorerData>(null);
    private SimpleBooleanProperty m_isErgoTokens = new SimpleBooleanProperty();

    private SimpleObjectProperty<PriceQuote> m_currentQuote = new SimpleObjectProperty<>(null);
    private ScheduledExecutorService m_timeExecutor = null;

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
  
        if(ergoNodes != null && walletData.getNodesId() != null){
            m_selectedNodeData.set(ergoNodes.getErgoNodesList().getErgoNodeData(walletData.getNodesId()));
        }
        if(ergoExplorer != null && walletData.getExplorerId() != null){
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

        
    
    
       
        updateAddressBox();
        setupTimer();


   

        
    }


    public void setupTimer(){

        if(m_lastExecution != null){
            m_lastExecution.cancel(false);
        }

        if(m_timeExecutor != null){
            m_timeExecutor.shutdownNow();
            m_timeExecutor = null;
        }
        
        if(m_walletData.getCyclePeriod() > 0){
            m_timeExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            
            Runnable doUpdate = ()->{
                Platform.runLater(()->updateTimeCycle());
            };

            m_lastExecution = m_timeExecutor.scheduleAtFixedRate(doUpdate, 0, m_walletData.getCyclePeriod(), m_walletData.getCycleTimeUnit());
        }
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
        
        if(m_promptStage == null){
      
            m_promptStage = new Stage();
            m_promptStage.initStyle(StageStyle.UNDECORATED);
            m_promptStage.getIcons().add(new Image("/assets/git-branch-outline-white-30.png"));
            m_promptStage.setTitle("Add Address - "+m_walletData.getName() + " - Ergo Wallets");

            TextField textField = new TextField();
            Button closeBtn = new Button();

            App.showGetTextInput("Address name", "Add Address", new Image("/assets/git-branch-outline-white-240.png"), textField, closeBtn, m_promptStage);
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
        }else{
            if(m_promptStage.isIconified()){
                m_promptStage.setIconified(false);
            }else{
                m_promptStage.show();
                Platform.runLater(()->m_promptStage.requestFocus());
            }
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



    /*public void startBalanceUpdates() {
       
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
   
    }*/

    public void stopUpdates() {
        if (m_timeExecutor != null) {
            if(m_lastExecution != null){
                m_lastExecution.cancel(false);
            }
            m_timeExecutor.shutdownNow();
            m_timeExecutor = null;
        }
    }

    public void shutdown() {
        stopUpdates();
    }

   
    public boolean updateSelectedExplorer(ErgoExplorerData ergoExplorerData){
        ErgoExplorerData previousSelectedExplorerData = m_selectedExplorerData.get();

        if ( ergoExplorerData == null && previousSelectedExplorerData == null) {
            return false;
        }


        m_selectedExplorerData.set(ergoExplorerData);

        /* if (previousSelectedExplorerData != null) {
        
           //update services if implemented

        }
        
        }*/

        return true;
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

        m_totalErgoAmount.set(new ErgoAmount(totalNanoErgs.get(), m_networkType));
    }

    public SimpleObjectProperty<ErgoAmount> totalErgoAmountProperty(){
        return m_totalErgoAmount;
    }



    public Scene getSendScene(Scene parentScene, Stage parentStage, Button closeBtn) {
        
        if(selectedAddressDataProperty().get() == null){
            if(m_addressDataList.size() == 1){
                selectedAddressDataProperty().set(m_addressDataList.get(0));
            }
        }

        Runnable requiredErgoNodes = () ->{
            if (m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID) == null) {
                Alert a = new Alert(AlertType.NONE, "Ergo Nodes must be installed in order to send from this wallet. \n\nWould you like to install Ergo Nodes?\n\n", ButtonType.YES, ButtonType.NO);
                a.setTitle("Required: Ergo Nodes");
                a.setHeaderText("Required: Ergo Nodes");
                a.initOwner(parentStage);
                Optional<ButtonType> result = a.showAndWait();
                if(result != null && result.isPresent() && result.get() == ButtonType.YES){
                    m_walletData.getErgoWallets().getErgoNetworkData().installNetwork(ErgoNodes.NETWORK_ID);
                    if(selectedNodeData().get() == null){
                        ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
                        if(ergoNodes != null && ergoNodes.getErgoNodesList().defaultNodeIdProperty() != null){
                            ErgoNodeData ergoNodeData = ergoNodes.getErgoNodesList().getErgoNodeData(ergoNodes.getErgoNodesList().defaultNodeIdProperty().get());
                            if(ergoNodeData != null){
                                selectedNodeData().set(ergoNodeData);
                            }
                        }
                    }
                }
            
            }
        };
        requiredErgoNodes.run();

        String oldStageName = parentStage.getTitle();

        String stageName = "Send - " + m_walletData.getName() + " - (" + m_networkType + ")";

        parentStage.setTitle(stageName);

        VBox layoutBox = new VBox();
        Scene sendScene = new Scene(layoutBox, 800, 600);
        sendScene.getStylesheets().add("/css/startWindow.css");

    
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), stageName, maximizeBtn, closeBtn, parentStage);
        maximizeBtn.setOnAction(e->{
            parentStage.setMaximized(!parentStage.isMaximized());
        });
        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        BufferedButton backButton = new BufferedButton("/assets/return-back-up-30.png",App.MENU_BAR_IMAGE_WIDTH) ;
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        backButton.setOnAction(e -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
            // ResizeHelper.addResizeListener(parentStage, WalletData.MIN_WIDTH, WalletData.MIN_HEIGHT, m_walletData.getMaxWidth(), m_walletData.getMaxHeight());
        });

        double imageWidth = App.MENU_BAR_IMAGE_WIDTH;

        
        Tooltip nodesTip = new Tooltip("Select node");
        nodesTip.setShowDelay(new javafx.util.Duration(50));
        nodesTip.setFont(App.txtFont);


        BufferedMenuButton nodesBtn = new BufferedMenuButton("/assets/ergoNodes-30.png", imageWidth);
        nodesBtn.setPadding(new Insets(2, 0, 0, 0));
        nodesBtn.setTooltip(nodesTip);


        
        Tooltip explorerTip = new Tooltip("Select explorer");
        explorerTip.setShowDelay(new javafx.util.Duration(50));
        explorerTip.setFont(App.txtFont);



        BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
        explorerBtn.setPadding(new Insets(2, 0, 0, 2));
        explorerBtn.setTooltip(explorerTip);

        SimpleObjectProperty<ErgoExplorerData> ergoExplorerProperty = new SimpleObjectProperty<>(selectedExplorerData().get()); 

        Runnable updateExplorerBtn = () ->{
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

            ErgoExplorerData explorerData = ergoExplorerProperty.get();
           
           
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
        
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
            if(ergoExplorers != null){
                explorerBtn.setId("menuBtn");
                ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, ergoExplorerProperty);
            }else{
                 explorerBtn.getItems().clear();
                 explorerBtn.setId("menuBtnDisabled");
               
            }
            updateExplorerBtn.run();
        };    

        ergoExplorerProperty.addListener((obs, oldval, newval)->{
            m_walletData.setExplorer(newval == null ? null : newval.getId());
            updateSelectedExplorer(newval);
            updateExplorerBtn.run();
        });

        Tooltip marketsTip = new Tooltip("Select market");
        marketsTip.setShowDelay(new javafx.util.Duration(50));
        marketsTip.setFont(App.txtFont);

        BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);
        
        SimpleObjectProperty<ErgoMarketsData> ergoMarketsData = new SimpleObjectProperty<>(selectedMarketData().get());
        
   

      
  
         Runnable updateMarketsBtn = () ->{
            ErgoMarketsData marketsData = ergoMarketsData.get();
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
      
          
            if(marketsData != null && ergoMarkets != null){
                
                marketsTip.setText("Ergo Markets: " + marketsData.getName());
                updateSelectedMarket(marketsData);
           
            }else{
               
                if(ergoMarkets == null){
                    marketsTip.setText("(install 'Ergo Markets')");
                }else{
                    marketsTip.setText("Select market...");
                }
            }
          
        };

        ergoMarketsData.addListener((obs,oldval,newVal) -> {
            m_walletData.setMarketsId(newVal != null ? newVal.getMarketId() : null);
            updateMarketsBtn.run();
        });

        Runnable getAvailableMarketsMenu = ()->{
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
            if(ergoMarkets != null){
                 marketsBtn.setId("menuBtn");
                
                ergoMarkets.getErgoMarketsList().getMenu(marketsBtn, ergoMarketsData);
            }else{
                marketsBtn.getItems().clear();
                marketsBtn.setId("menuBtnDisabled");
            }
            updateMarketsBtn.run();
        };

        
       

        Tooltip tokensTip = new Tooltip("Ergo Tokens");
        tokensTip.setShowDelay(new javafx.util.Duration(50));
        tokensTip.setFont(App.mainFont);


        BufferedMenuButton tokensBtn = new BufferedMenuButton("/assets/diamond-30.png", imageWidth);
        tokensBtn.setPadding(new Insets(2, 0, 0, 0));
      
        

        Runnable updateTokensMenu = ()->{
            tokensBtn.getItems().clear();
            ErgoTokens ergoTokens = (ErgoTokens) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);  
            boolean isEnabled = isErgoTokensProperty().get();

            if(ergoTokens != null){
                tokensBtn.setId("menuBtn");
                MenuItem tokensEnabledItem = new MenuItem("Enabled" + (isEnabled ? " (selected)" : ""));
                tokensEnabledItem.setOnAction(e->{
                    isErgoTokensProperty().set(true);
                });
                

                MenuItem tokensDisabledItem = new MenuItem("Disabled" + (isEnabled ? "" : " (selected)"));
                tokensDisabledItem.setOnAction(e->{
                    isErgoTokensProperty().set(false);
                });

                if(isEnabled){
                    tokensTip.setText("Ergo Tokens: Enabled");
                    tokensEnabledItem.setId("selectedMenuItem");
                }else{
                    tokensTip.setText("Ergo Tokens: Disabled");
                    tokensDisabledItem.setId("selectedMenuItem");
                }
                tokensBtn.getItems().addAll(tokensEnabledItem, tokensDisabledItem);
            }else{
                tokensBtn.setId("menuBtnDisabled");
                MenuItem tokensInstallItem = new MenuItem("(install 'Ergo Tokens')");
                tokensInstallItem.setOnAction(e->{
                    m_walletData.getErgoWallets().getErgoNetworkData().showwManageStage();
                });
                tokensTip.setText("(install 'Ergo Tokens')");
            }
           
        };
   
        isErgoTokensProperty().addListener((obs,oldval,newval)->{
            m_walletData.setIsErgoTokens(newval);
            updateTokensMenu.run();
        });

      

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

        HBox rightSideMenu = new HBox(nodesBtn, seperator1, explorerBtn, seperator2, marketsBtn, seperator3, tokensBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
        rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

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



        
        



        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        

        Text fromText = new Text("From   ");
        fromText.setFont(App.txtFont);
        fromText.setFill(App.txtColor);

        String nullAddressImageString = "/assets/selectAddress.png";
        Image nullAddressImg = new Image(nullAddressImageString);

        MenuButton fromAddressBtn = new MenuButton();
        fromAddressBtn.setMinHeight(50);
        fromAddressBtn.setContentDisplay(ContentDisplay.LEFT);
        fromAddressBtn.setAlignment(Pos.CENTER_LEFT);

        Runnable updateAvaliableAddresses = ()->{
            fromAddressBtn.getItems().clear();
            for (AddressData addressItem : m_addressDataList) {

                MenuItem addressMenuItem = new MenuItem(addressItem.getAddressString());
                addressMenuItem.textProperty().bind(addressItem.textProperty());
                Image addressImage = addressItem.getImageProperty().get();
                addressMenuItem.setGraphic(IconButton.getIconView(addressImage, addressImage.getWidth()));

                addressItem.getImageProperty().addListener((obs, oldVal, newVal) -> {
                    if(newVal != null){
                        addressMenuItem.setGraphic(IconButton.getIconView(newVal, newVal.getWidth()));
                    }
                });

                fromAddressBtn.getItems().add(addressMenuItem);

                addressMenuItem.setOnAction(actionEvent -> {
                    m_selectedAddressData.set(addressItem);
                });
            }
        };
        updateAvaliableAddresses.run();
        
        SimpleObjectProperty<Image> addressImageProperty = new SimpleObjectProperty<>(new Image(nullAddressImageString));
        
        // fromAddressBtn.setPadding(new Insets(2, 5, 2, 0));
        Runnable updateAddressBtn = () ->{
            AddressData addressData = selectedAddressDataProperty().get() ;
            

            if(addressData != null){
                addressImageProperty.bind(addressData.getImageProperty());
                fromAddressBtn.setText(addressData.getButtonText());
            }else{
                addressImageProperty.unbind();
                addressImageProperty.set(nullAddressImg);
                
            }

          
        };

       

      
        addressImageProperty.addListener((obs,oldval,newval)->{
            ImageView imgView = newval != null ? IconButton.getIconView(newval, newval.getWidth()): IconButton.getIconView(nullAddressImg, nullAddressImg.getWidth());
            fromAddressBtn.setGraphic(imgView);
        });

  
        updateAddressBtn.run();

        Text toText = new Text("To     ");
        toText.setFont(App.txtFont);
        toText.setFill(App.txtColor);


        AddressBox toAddressEnterBox = new AddressBox(new AddressInformation(""), sendScene, m_networkType );
        toAddressEnterBox.setId("bodyRowBox");
        toAddressEnterBox.setMinHeight(50);
     
        HBox.setHgrow(toAddressEnterBox, Priority.ALWAYS);

        HBox toAddressBox = new HBox(toText, toAddressEnterBox);
        toAddressBox.setPadding(new Insets(0, 15, 10, 30));
        toAddressBox.setAlignment(Pos.CENTER_LEFT);
     
        HBox fromRowBox = new HBox(fromAddressBtn);
        HBox.setHgrow(fromRowBox, Priority.ALWAYS);
        fromRowBox.setAlignment(Pos.CENTER_LEFT);
        fromRowBox.setId("bodyRowBox");
      
        fromRowBox.setPadding(new Insets(0));

        HBox fromAddressBox = new HBox(fromText,fromRowBox);
        fromAddressBox.setPadding(new Insets(3, 15, 8, 30));
   
        HBox.setHgrow(fromAddressBox, Priority.ALWAYS);
        fromAddressBox.setAlignment(Pos.CENTER_LEFT);

          
        Button statusBoxBtn = new Button();
        statusBoxBtn.setId("bodyRowBox");
        statusBoxBtn.setPrefHeight(50);
        statusBoxBtn.setFont(App.txtFont);
        statusBoxBtn.setAlignment(Pos.CENTER_LEFT);
        statusBoxBtn.setPadding(new Insets(0));
        statusBoxBtn.setOnAction(e->{
            nodesBtn.show();
        });

        
        HBox nodeStatusBox = new HBox();
        nodeStatusBox.setId("bodyRowBox");
        nodeStatusBox.setPadding(new Insets(0,0,0,0));
        nodeStatusBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodeStatusBox, Priority.ALWAYS);
        nodeStatusBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            nodesBtn.show();
        });

        Runnable updateNodeBtn = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            ErgoNodeData nodeData = selectedNodeData().get();
            nodeStatusBox.getChildren().clear();

            if(nodeData != null && ergoNodes != null){
                nodesTip.setText(nodeData.getName());
                HBox statusBox =  nodeData.getStatusBox();
               
                nodeStatusBox.getChildren().add(statusBox);
               
                nodeStatusBox.setId("tokenBtn");
            }else{
                nodeStatusBox.setId(null);
                nodeStatusBox.getChildren().add(statusBoxBtn);
                statusBoxBtn.prefWidthProperty().bind(fromAddressBtn.widthProperty());
                if(ergoNodes == null){
                    String statusBtnText = "Install Ergo Nodes";
                    nodesTip.setText(statusBtnText);
                    statusBoxBtn.setGraphic(IconButton.getIconView(new Image("/assets/selectNode.png"), 164));
                }else{
                    String statusBtnText = "Select node";
                    nodesTip.setText(statusBtnText);
                    statusBoxBtn.setGraphic(IconButton.getIconView(new Image("/assets/selectNode.png"), 164));
                }
            }
          
            
        };
        
        Runnable getAvailableNodeMenu = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            if(ergoNodes != null){
                ergoNodes.getErgoNodesList().getMenu(nodesBtn, selectedNodeData());
                nodesBtn.setId("menuBtn");
            }else{
                nodesBtn.getItems().clear();
                nodesBtn.setId("menuBtnDisabled");
               
            }
            updateNodeBtn.run();
        };

        selectedNodeData().addListener((obs, oldval, newval)->{
                updateNodeBtn.run();
    
            m_walletData.setNodesId(newval == null ? null : newval.getId());
           
        });
        m_walletData.getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
            getAvailableNodeMenu.run();
            getAvailableExplorerMenu.run();
            getAvailableMarketsMenu.run();
            updateTokensMenu.run();
        });

        getAvailableExplorerMenu.run();
        getAvailableNodeMenu.run();
        getAvailableMarketsMenu.run();
        updateTokensMenu.run();

        Text nodeText = new Text("Node   ");
        nodeText.setFont(App.txtFont);
        nodeText.setFill(App.txtColor);


        HBox nodeRowBox = new HBox(nodeText, nodeStatusBox);
        nodeRowBox.setPadding(new Insets(0, 15, 10, 30));
        nodeRowBox.setMinHeight(60);
        nodeRowBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodeRowBox, Priority.ALWAYS);
     

        Text amountText = new Text("Amount ");
        amountText.setFont(App.txtFont);
        amountText.setFill(App.txtColor);

     //   BufferedButton addTokenBtn = new BufferedButton("/assets/add-outline-white-40.png", 15);
    
        Tooltip addTokenBtnTip = new Tooltip("Add Token");
        addTokenBtnTip.setShowDelay(new Duration(100));

        AmountMenuButton addTokenBtn = new AmountMenuButton("/assets/add-outline-white-40.png", 20);
        addTokenBtn.setAlignment(Pos.CENTER);
        addTokenBtn.setTooltip(addTokenBtnTip);
        addTokenBtn.setPadding(new Insets(2,0,2,0));
        addTokenBtn.setNullDisable(true);
        addTokenBtn.addressDataProperty().bind(m_selectedAddressData);

        BufferedButton deleteTokenBtn = new BufferedButton();

        HBox amountRightSideBox = new HBox(addTokenBtn);
        amountRightSideBox.setPadding(new Insets(0));
        amountRightSideBox.setAlignment(Pos.BOTTOM_RIGHT);
        VBox.setVgrow(amountRightSideBox, Priority.ALWAYS);
        amountRightSideBox.setId("bodyBox");
    //    HBox.setHgrow(amountRightSideBox,Priority.ALWAYS);


        HBox amountTextBox = new HBox(amountText);
        amountTextBox.setAlignment(Pos.CENTER_LEFT);
        amountTextBox.setMinHeight(40);
        HBox.setHgrow(amountTextBox,Priority.ALWAYS);

        HBox amountBoxRow = new HBox(amountTextBox, amountRightSideBox);
        amountBoxRow.setPadding(new Insets(10, 20, 0, 30));
     
        amountBoxRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(amountBoxRow,Priority.ALWAYS);

        AmountSendBox ergoAmountBox = new AmountSendBox(new ErgoAmount(0, m_networkType), sendScene, true);
        ergoAmountBox.priceQuoteProperty().bind(m_currentQuote);
        
        ergoAmountBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if(selectedAddressDataProperty().get() == null){
         
                fromAddressBtn.show();
            }
        });

        HBox.setHgrow(ergoAmountBox,Priority.ALWAYS);

      

       // addTokenBtn.setOnAction(e->addTokenBtn.show());

        AmountBoxes amountBoxes = new AmountBoxes();
        amountBoxes.setPadding(new Insets(10,0,10,0));

        amountBoxes.setAlignment(Pos.TOP_LEFT);
     //   amountBoxes.setLastRowItem(addTokenBtn, AmountBoxes.ADD_AS_LAST_ROW);
        amountBoxes.setId("bodyBox");

    
        addTokenBtn.setAmountBoxes(amountBoxes);

        Runnable updateErgoMaxBalance = ()->{
            AddressData addressData = m_selectedAddressData.get();
            if(addressData != null){
                ergoAmountBox.balanceAmountProperty().bind( addressData.ergoAmountProperty()) ;
            }else{
                ergoAmountBox.balanceAmountProperty().unbind();
                ergoAmountBox.balanceAmountProperty().set(null);
            }
        };
        updateErgoMaxBalance.run();


        Runnable updateTokensMaxBalance = () ->{
            AddressData addressData = m_selectedAddressData.get();
            if(addressData != null){
              
                for(int i = 0; i < amountBoxes.amountsList().size() ; i++ ){
                    AmountBox amountBox = amountBoxes.amountsList().get(i);
                    if(amountBox != null && amountBox instanceof AmountSendBox){
                        AmountSendBox amountSendBox = (AmountSendBox) amountBox;
                        
                        PriceAmount tokenAmount = addressData.getConfirmedTokenAmount(amountSendBox.getTokenId());

                        amountSendBox.balanceAmountProperty().set(tokenAmount);

                    }
               }
            }else{
             
               for(AmountBox amountBox : amountBoxes.amountsList() ){
                    if(amountBox != null && amountBox instanceof AmountSendBox){
                        AmountSendBox amountSendBox = (AmountSendBox) amountBox;
                        amountSendBox.balanceAmountProperty().set(null);
                    }
               }
            }
        };
        updateTokensMaxBalance.run();
        ChangeListener<? super LocalDateTime> balanceChangeListener = (obs, oldVal, newVal) -> {
  
        
            updateTokensMaxBalance.run();
        };

        selectedAddressDataProperty().addListener((obs, oldval, newval) ->{ 
            updateAddressBtn.run();
            updateErgoMaxBalance.run();

            if(oldval != null){
                oldval.getLastUpdated().removeListener(balanceChangeListener);
            }
            if(newval != null){
                
                newval.getLastUpdated().addListener(balanceChangeListener);
            }
        });
    

        

      //  addTokenBtn.prefWidthProperty().bind(amountBoxes.widthProperty());
       
        Region sendBoxSpacer = new Region();
        HBox.setHgrow(sendBoxSpacer, Priority.ALWAYS);
      
        Runnable finalCheckAndSend = () ->{
            Alert a = new Alert(AlertType.NONE, "Final check and send.", ButtonType.OK);
            a.setTitle("Check");
            a.setHeaderText("Check");
            a.initOwner(parentStage);
            a.show();
        };

        BufferedButton sendBtn = new BufferedButton("Send", "/assets/arrow-send-white-30.png", 30);
        sendBtn.setFont(App.txtFont);
        sendBtn.setId("toolBtn");
        sendBtn.setUserData("sendButton");
        sendBtn.setContentDisplay(ContentDisplay.LEFT);
        sendBtn.setPadding(new Insets(3, 15, 3, 5));
        sendBtn.setOnAction(e -> {
            requiredErgoNodes.run();
            if(m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID) != null){
                ErgoNodeData ergoNodeData = selectedNodeData().get();
                if(ergoNodeData != null){
                    if(ergoNodeData instanceof ErgoNodeLocalData){
                        ErgoNodeLocalData localErgoNode = (ErgoNodeLocalData) ergoNodeData;
                        if(localErgoNode.isSetupProperty.get()){

                        }else{
                            Alert a = new Alert(AlertType.NONE, "The selected node requires setup. Would you like to set it up now?", ButtonType.YES, ButtonType.NO);
                            a.setTitle("Setup Required");
                            a.setHeaderText("Setup Required");
                            a.initOwner(parentStage);
                            Optional<ButtonType> result = a.showAndWait();
                            if(result != null && result.isPresent() && result.get() == ButtonType.YES){
                                localErgoNode.setup();
                            }
                        }
                    }else{
                        boolean nodeAvailable = ergoNodeData.availableProperty.get();
                        if(nodeAvailable){
                            finalCheckAndSend.run();
                        }else{
                            Alert a = new Alert(AlertType.NONE, "The selected node cannot be reached. Please select an alternate node or add a node using Ergo Nodes.", ButtonType.OK);
                            a.setTitle("Node Unavailable");
                            a.setHeaderText("Node Unavailable");
                            a.initOwner(parentStage);
                            a.showAndWait();
                            nodesBtn.show();
                        }
                    }
                }else{
                    Alert a = new Alert(AlertType.NONE, "Please select a node from the drop down menu.", ButtonType.OK);
                    a.setTitle("Select Node");
                    a.setHeaderText("Select Node");
                    a.initOwner(parentStage);
                    a.showAndWait();

                    nodesBtn.show();
              
                }
            }
        });


        HBox sendBox = new HBox(sendBtn);
        VBox.setVgrow(sendBox, Priority.ALWAYS);
        sendBox.setPadding(new Insets(0,30,8,15));
        sendBox.setAlignment(Pos.CENTER_RIGHT);

        HBox ergoAmountPaddingBox = new HBox(ergoAmountBox);
        ergoAmountPaddingBox.setId("bodyBox");
        ergoAmountPaddingBox.setPadding(new Insets(10,10,0,10));


  


        VBox scrollPaneContentVBox = new VBox(ergoAmountPaddingBox, amountBoxes);

        

        ScrollPane scrollPane = new ScrollPane(scrollPaneContentVBox);
        scrollPane.setPadding(new Insets(0,0,0, 20));

        VBox scrollPaddingBox = new VBox(scrollPane);
        HBox.setHgrow(scrollPaddingBox,Priority.ALWAYS);
        scrollPaddingBox.setPadding(new Insets(0,5,0,5));

        VBox bodyBox = new VBox( fromAddressBox, toAddressBox, nodeRowBox, amountBoxRow, scrollPaddingBox);
        VBox.setVgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15,0,0,0));

        VBox bodyLayoutBox = new VBox(headingBox, bodyBox);
        VBox.setVgrow(bodyLayoutBox,Priority.ALWAYS);
        bodyLayoutBox.setPadding(new Insets(0, 4, 4,4));

        
       
        HBox footerBox = new HBox(sendBox);
        
        /*nodeScroll.setPrefViewportHeight(60);
        nodeScroll.prefViewportWidthProperty().bind(footerBox.widthProperty().subtract(sendBox.widthProperty()).subtract(70));
        
        nodeStatusBox.prefWidthProperty().bind(nodeScroll.prefViewportWidthProperty().subtract(20));*/
        
        HBox.setHgrow(footerBox, Priority.ALWAYS);
        footerBox.setPadding(new Insets(5,30,0,5));
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        HBox paddingBox = new HBox(menuBar);
        HBox.setHgrow(paddingBox, Priority.ALWAYS);
        paddingBox.setPadding(new Insets(0, 4, 4, 4));

        layoutBox.getChildren().addAll(titleBox, paddingBox, bodyLayoutBox, footerBox);
        VBox.setVgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setAlignment(Pos.TOP_LEFT);

        fromAddressBtn.prefWidthProperty().bind(fromAddressBox.widthProperty().subtract(fromText.layoutBoundsProperty().getValue().getWidth()).subtract(30));
        
        scrollPane.prefViewportHeightProperty().bind(layoutBox.heightProperty().subtract(20).subtract(titleBox.heightProperty()).subtract(paddingBox.heightProperty()).subtract(headingBox.heightProperty()).subtract(fromAddressBox.heightProperty()).subtract(toAddressBox.heightProperty()).subtract(nodeRowBox.heightProperty()).subtract( amountBoxRow.heightProperty()).subtract(footerBox.heightProperty()).subtract(15));
        amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(20).subtract(ergoAmountPaddingBox.heightProperty()));
        scrollPane.prefViewportWidthProperty().bind(sendScene.widthProperty().subtract(60));
        amountBoxes.prefWidthProperty().bind(sendScene.widthProperty().subtract(60));
        ergoAmountPaddingBox.prefWidthProperty().bind(sendScene.widthProperty().subtract(60));

        return sendScene;
    }




}
