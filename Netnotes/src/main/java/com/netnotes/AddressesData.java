package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;



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
import javafx.beans.property.SimpleBooleanProperty;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
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

        Button statusBoxBtn = new Button();
        statusBoxBtn.setId("tokenBtn");


        
        HBox nodeStatusBox = new HBox();
        nodeStatusBox.setPadding(new Insets(0,15,0,15));
        nodeStatusBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(nodeStatusBox, Priority.ALWAYS);
        

        Runnable updateNodeBtn = () ->{
            ErgoNodes ergoNodes = (ErgoNodes) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
            ErgoNodeData nodeData = selectedNodeData().get();
            nodeStatusBox.getChildren().clear();

            if(nodeData != null && ergoNodes != null){
                nodesTip.setText(nodeData.getName());
                nodeStatusBox.getChildren().add(nodeData.getStatusBox());
                nodeStatusBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                   ergoNodes.open(); 
                });
                nodeStatusBox.setId("tokenBtn");
            }else{
                nodeStatusBox.setId(null);
                nodeStatusBox.getChildren().add(statusBoxBtn);
               
                if(ergoNodes == null){
                    String statusBtnText = "Install Ergo Nodes";
                    nodesTip.setText(statusBtnText);
                    statusBoxBtn.setText(statusBtnText);
                }else{
                    String statusBtnText = "Select node...";
                    nodesTip.setText(statusBtnText);
                    statusBoxBtn.setText(statusBtnText);
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

        
        Tooltip explorerTip = new Tooltip("Select explorer");
        explorerTip.setShowDelay(new javafx.util.Duration(50));
        explorerTip.setFont(App.txtFont);



        BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
        explorerBtn.setPadding(new Insets(2, 0, 0, 2));
        explorerBtn.setTooltip(explorerTip);

        Runnable updateExplorerBtn = () ->{
            ErgoExplorers ergoExplorers = (ErgoExplorers) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

            ErgoExplorerData explorerData = selectedExplorerData().get();
           
           
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
                ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, selectedExplorerData());
            }else{
                 explorerBtn.getItems().clear();
                 explorerBtn.setId("menuBtnDisabled");
               
            }
            updateExplorerBtn.run();
        };    

        selectedExplorerData().addListener((obs, oldval, newval)->{
            m_walletData.setExplorer(newval == null ? null : newval.getId());
            updateExplorerBtn.run();
        });

        Tooltip marketsTip = new Tooltip("Select market");
        marketsTip.setShowDelay(new javafx.util.Duration(50));
        marketsTip.setFont(App.txtFont);

        BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
        marketsBtn.setPadding(new Insets(2, 0, 0, 0));
        marketsBtn.setTooltip(marketsTip);
        
        SimpleObjectProperty<ErgoMarketsData> ergoMarketsData = new SimpleObjectProperty<>(null);
        
        if(m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID) != null){
            String marketId = selectedMarketData().get() != null ? selectedMarketData().get().getMarketId() : null;
            ErgoMarkets ergoMarkets = (ErgoMarkets) m_walletData.getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
            if(ergoMarkets != null){
                ErgoMarketsData mData = ergoMarkets.getErgoMarketsList().getMarketsData( marketId);
                if(mData != null){
                    updateSelectedMarket(mData);
                    ergoMarketsData.set(mData);
                }
            }
        }

      
  
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



        
        BufferedButton sendBtn = new BufferedButton("Send", "/assets/arrow-send-white-30.png", 25);



        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        

        Text fromText = new Text("From   ");
        fromText.setFont(App.txtFont);
        fromText.setFill(App.txtColor);

        String nullAddressImageString = "/assets/enterAddress.png";
        Image nullAddressImg = new Image(nullAddressImageString);

        MenuButton fromAddressBtn = new MenuButton();
        fromAddressBtn.setId("rowBox");
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
                m_selectedAddressData.set(addressItem);
            });
        }
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

       

        selectedAddressDataProperty().addListener((obs, oldval, newval) -> updateAddressBtn.run());
        addressImageProperty.addListener((obs,oldval,newval)->{
            ImageView imgView = newval != null ? IconButton.getIconView(newval, newval.getWidth()): IconButton.getIconView(nullAddressImg, nullAddressImg.getWidth());
            fromAddressBtn.setGraphic(imgView);
        });

  
        updateAddressBtn.run();

        Text toText = new Text("To     ");
        toText.setFont(App.txtFont);
        toText.setFill(App.txtColor);


        AddressBox toAddressEnterBox = new AddressBox(new AddressInformation(""), sendScene, m_networkType );
        toAddressEnterBox.setId("rowBox");
        toAddressEnterBox.setMinHeight(40);
        HBox.setHgrow(toAddressEnterBox, Priority.ALWAYS);

        HBox toAddressBox = new HBox(toText, toAddressEnterBox);
        toAddressBox.setPadding(new Insets(3, 15, 5, 30));
        toAddressBox.setAlignment(Pos.CENTER_LEFT);
     



        HBox fromAddressBox = new HBox(fromText, fromAddressBtn);
        fromAddressBox.setPadding(new Insets(3, 15, 5, 30));
   
        HBox.setHgrow(fromAddressBox, Priority.ALWAYS);
        fromAddressBox.setAlignment(Pos.CENTER_LEFT);

     

        Text amountText = new Text("Amount ");
        amountText.setFont(App.txtFont);
        amountText.setFill(App.txtColor);

        

        VBox amountBoxRow = new VBox(amountText);
        amountBoxRow.setPadding(new Insets(0, 15, 10, 30));
        amountBoxRow.setMinHeight(40);
        amountBoxRow.setAlignment(Pos.BOTTOM_LEFT);

        AmountSendBox ergoAmountBox = new AmountSendBox(new ErgoAmount(0, m_networkType), sendScene, true);
        ergoAmountBox.priceQuoteProperty().bind(m_currentQuote);
        HBox.setHgrow(ergoAmountBox,Priority.ALWAYS);

        Tooltip addCryptoBtnTip = new Tooltip("Add Token");
        addCryptoBtnTip.setShowDelay(new Duration(100));

        MenuButton addCryptoBtn = new MenuButton("[+]");
        addCryptoBtn.setAlignment(Pos.CENTER);
        addCryptoBtn.setTooltip(addCryptoBtnTip);
        addCryptoBtn.setId("amountMenuBtn");
        addCryptoBtn.setPrefHeight(40);
        addCryptoBtn.setPadding(new Insets(2,0,2,0));

        AmountBoxes amountBoxes = new AmountBoxes();
        amountBoxes.setPadding(new Insets(10,10,10,0));

        amountBoxes.setAlignment(Pos.TOP_LEFT);
        amountBoxes.setLastRowItem(addCryptoBtn, AmountBoxes.ADD_AS_LAST_ROW);
        amountBoxes.setId("bodyBox");

        addCryptoBtn.prefWidthProperty().bind(amountBoxes.widthProperty());
       
        Region sendBoxSpacer = new Region();
        HBox.setHgrow(sendBoxSpacer, Priority.ALWAYS);
      
        Runnable finalCheckAndSend = () ->{
            Alert a = new Alert(AlertType.NONE, "Final check and send.", ButtonType.OK);
            a.setTitle("Check");
            a.setHeaderText("Check");
            a.initOwner(parentStage);
            a.show();
        };

        
        sendBtn.setFont(App.txtFont);
        sendBtn.setId("menuBtn");
        sendBtn.setUserData("sendButton");
        sendBtn.setContentDisplay(ContentDisplay.LEFT);
        sendBtn.setPadding(new Insets(3, 15, 3, 15));
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
        sendBox.setPrefHeight(70);
        sendBox.setPadding(new Insets(0,0,0,10));
        sendBox.setAlignment(Pos.CENTER_RIGHT);
        
        HBox ergoAmountPaddingBox = new HBox(ergoAmountBox);
        ergoAmountPaddingBox.setId("bodyBox");
        ergoAmountPaddingBox.setPadding(new Insets(10,10,0,10));

        VBox scrollPaneContentVBox = new VBox(ergoAmountPaddingBox, amountBoxes);

        

        ScrollPane scrollPane = new ScrollPane(scrollPaneContentVBox);
        scrollPane.setPadding(new Insets(10,0,0, 20));

        VBox scrollPaddingBox = new VBox(scrollPane);
        HBox.setHgrow(scrollPaddingBox,Priority.ALWAYS);
        scrollPaddingBox.setPadding(new Insets(0,5,0,5));
        VBox bodyBox = new VBox( fromAddressBox, toAddressBox, amountBoxRow, scrollPaddingBox);
        VBox.setVgrow(bodyBox,Priority.ALWAYS);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15,0,0,0));

        VBox bodyLayoutBox = new VBox(headingBox, bodyBox);
        VBox.setVgrow(bodyLayoutBox,Priority.ALWAYS);
        bodyLayoutBox.setPadding(new Insets(0, 4, 4,4));


        HBox footerBox = new HBox(nodeStatusBox, sendBox);
       
        HBox.setHgrow(footerBox, Priority.ALWAYS);
        footerBox.setPadding(new Insets(10,30,10,15));
        footerBox.setAlignment(Pos.CENTER_LEFT);

        HBox paddingBox = new HBox(menuBar);
        HBox.setHgrow(paddingBox, Priority.ALWAYS);
        paddingBox.setPadding(new Insets(0, 4, 4, 4));

        layoutBox.getChildren().addAll(titleBox, paddingBox, bodyLayoutBox, footerBox);
        VBox.setVgrow(layoutBox, Priority.ALWAYS);
        layoutBox.setAlignment(Pos.TOP_LEFT);

        fromAddressBtn.prefWidthProperty().bind(fromAddressBox.widthProperty().subtract(fromText.layoutBoundsProperty().getValue().getWidth()).subtract(30));
     
        scrollPane.prefViewportHeightProperty().bind(layoutBox.heightProperty().subtract(20).subtract(titleBox.heightProperty()).subtract(paddingBox.heightProperty()).subtract(headingBox.heightProperty()).subtract(fromAddressBox.heightProperty()).subtract(toAddressBox.heightProperty()).subtract( amountBoxRow.heightProperty()).subtract(footerBox.heightProperty()).subtract(15));
        amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(20).subtract(ergoAmountPaddingBox.heightProperty()));
        scrollPane.prefViewportWidthProperty().bind(sendScene.widthProperty().subtract(60));
        amountBoxes.prefWidthProperty().bind(sendScene.widthProperty().subtract(60));
        ergoAmountPaddingBox.prefWidthProperty().bind(sendScene.widthProperty().subtract(60));

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
