package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;

import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

public class AddressData extends Network {

    public final static long QUOTE_TIMEOUT = 120000;
  
    private int m_index;
    private Address m_address;

    private final SimpleObjectProperty<ErgoAmount> m_ergoAmountProperty = new SimpleObjectProperty<ErgoAmount>(new ErgoAmount(0, false));
    private final ArrayList<PriceAmount> m_confirmedTokensList = new ArrayList<>();


    private long m_quoteTimeout = QUOTE_TIMEOUT;
    private long m_unconfirmedNanoErgs = 0;
    private String m_priceBaseCurrency = "ERG";
    private String m_priceTargetCurrency = "USDT";
    private ArrayList<PriceAmount> m_unconfirmedTokensList = new ArrayList<>();
    private Stage m_addressStage = null;
    private File logFile = new File("netnotes-log.txt");
    private AddressesData m_addressesData;
    private int m_minImgWidth = 250;

    private SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);
    
    public AddressData(String name, int index, Address address, NetworkType networktype, AddressesData addressesData) {
        super(null, name, address.toString(), addressesData.getWalletData());
        

        m_addressesData = addressesData;
        m_index = index;
        m_address = address;

        Tooltip addressTip = new Tooltip(getName());
        addressTip.setShowDelay(new javafx.util.Duration(100));
        addressTip.setFont(App.txtFont);

        setTooltip(addressTip);
        setPadding(new Insets(0, 10, 0, 10));
        setId("rowBtn");
        setText(getButtonText());

        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);
        setTextAlignment(TextAlignment.LEFT);
       
     
        
       // getQuote();

        ChangeListener<? super PriceQuote> quoteChangeListener = (obs, oldVal, newVal) -> updateBufferedImage();

        m_addressesData.selectedMarketData().addListener((obs, oldval, newVal) -> {
            if (oldval != null) {
                oldval.priceQuoteProperty().removeListener(quoteChangeListener);
                oldval.shutdown();
            }
            if (newVal != null) {
                newVal.priceQuoteProperty().addListener(quoteChangeListener);
                newVal.start();
            }
        });

        if (m_addressesData.selectedMarketData().get() != null) {
            m_addressesData.selectedMarketData().get().priceQuoteProperty().addListener(quoteChangeListener);
        }

        m_addressesData.timeCycleProperty().addListener((obs, oldval, newval)->{
            Platform.runLater(()->updateBalance());
        });

        m_ergoAmountProperty.addListener((obs,oldval,newval)->updateBufferedImage());
        updateBufferedImage();
    }



    public String getButtonText() {
        return "  " + getName() + "\n   " + getAddressString();
    }

    /*public boolean donate(){
        BigDecimal amountFullErg = dialog.showForResult().orElse(null);
		if (amountFullErg == null) return;
		try {
			Wallet wallet = Main.get().getWallet();
			UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(Utils.createErgoClient(),
					wallet.addressStream().toList(),
					DONATION_ADDRESS, ErgoInterface.toNanoErg(amountFullErg), Parameters.MinFee, Main.get().getWallet().publicAddress(0));
			String txId = wallet.transact(Utils.createErgoClient().execute(ctx -> {
				try {
					return wallet.key().sign(ctx, unsignedTx, wallet.myAddresses.keySet());
				} catch (WalletKey.Failure ex) {
					return null;
				}
			}));
			if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
		} catch (WalletKey.Failure ignored) {
			// user already informed
		}
    }*/
 /* return ;
        }); */
    public String getNodesId() {
        return "";
    }

 

 

    @Override
    public void open() {
        super.open();
        showAddressStage();

    }

    private void showAddressStage() {
        if (m_addressStage == null) {
            String titleString = getName() + " - " + m_address.toString() + " - (" + getNetworkType().toString() + ")";
            m_addressStage = new Stage();
            m_addressStage.getIcons().add(ErgoWallets.getAppIcon());
            m_addressStage.setResizable(false);
            m_addressStage.initStyle(StageStyle.UNDECORATED);
            m_addressStage.setTitle(titleString);

            double shrunkHeight = 106;

            Button closeBtn = new Button();

            addShutdownListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> closeBtn.fire());
            });

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), titleString, closeBtn, m_addressStage);
 
        

            double imageWidth = App.MENU_BAR_IMAGE_WIDTH;

            
            Tooltip nodesTip = new Tooltip("Select node");
            nodesTip.setShowDelay(new javafx.util.Duration(50));
            nodesTip.setFont(App.txtFont);


            BufferedMenuButton nodesBtn = new BufferedMenuButton("/assets/ergoNodes-30.png", imageWidth);
            nodesBtn.setPadding(new Insets(2, 0, 0, 0));
            nodesBtn.setTooltip(nodesTip);
            


            Runnable updateNodeBtn = () ->{
            
                ErgoNodes ergoNodes = (ErgoNodes)m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
                ErgoNodeData nodeData = m_addressesData.selectedNodeData().get();
            
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
                ErgoNodes ergoNodes = (ErgoNodes)m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoNodes.NETWORK_ID);
                if(ergoNodes != null){
                    ergoNodes.getErgoNodesList().getMenu(nodesBtn, m_addressesData.selectedNodeData());
                    nodesBtn.setId("menuBtn");
                }else{
                    nodesBtn.getItems().clear();
                    nodesBtn.setId("menuBtnDisabled");
                
                }
                updateNodeBtn.run();
            };

            m_addressesData.selectedNodeData().addListener((obs, oldval, newval)->{
                    updateNodeBtn.run();
        
            m_addressesData.getWalletData().setNodesId(newval == null ? null : newval.getId());
            
            });

            
            Tooltip explorerTip = new Tooltip("Select explorer");
            explorerTip.setShowDelay(new javafx.util.Duration(50));
            explorerTip.setFont(App.txtFont);



            BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", imageWidth);
            explorerBtn.setPadding(new Insets(2, 0, 0, 2));
            explorerBtn.setTooltip(explorerTip);

            Runnable updateExplorerBtn = () ->{
                ErgoExplorers ergoExplorers = (ErgoExplorers) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

                ErgoExplorerData explorerData = m_addressesData.selectedExplorerData().get();
            
            
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
            
                ErgoExplorers ergoExplorers = (ErgoExplorers) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
                if(ergoExplorers != null){
                    explorerBtn.setId("menuBtn");
                    ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, m_addressesData.selectedExplorerData());
                }else{
                    explorerBtn.getItems().clear();
                    explorerBtn.setId("menuBtnDisabled");
                
                }
                updateExplorerBtn.run();
            };    

            m_addressesData.selectedExplorerData().addListener((obs, oldval, newval)->{
                m_addressesData.getWalletData().setExplorer(newval == null ? null : newval.getId());
                updateExplorerBtn.run();
            });

            Tooltip marketsTip = new Tooltip("Select market");
            marketsTip.setShowDelay(new javafx.util.Duration(50));
            marketsTip.setFont(App.txtFont);

            BufferedMenuButton marketsBtn = new BufferedMenuButton("/assets/ergoChart-30.png", imageWidth);
            marketsBtn.setPadding(new Insets(2, 0, 0, 0));
            marketsBtn.setTooltip(marketsTip);
            
            SimpleObjectProperty<ErgoMarketsData> ergoMarketsData = new SimpleObjectProperty<>(null);
            
            if(m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID) != null){
                String marketId = m_addressesData.selectedMarketData().get() != null ? m_addressesData.selectedMarketData().get().getMarketId() : null;
                ErgoMarkets ergoMarkets = (ErgoMarkets) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
                if(ergoMarkets != null){
                    ErgoMarketsData mData = ergoMarkets.getErgoMarketsList().getMarketsData( marketId);
                    if(mData != null){
                        
                        m_addressesData.updateSelectedMarket(mData);
                        ergoMarketsData.set(mData);
                    }
                }
            }

        
    
            Runnable updateMarketsBtn = () ->{
                ErgoMarketsData marketsData = ergoMarketsData.get();
                ErgoMarkets ergoMarkets = (ErgoMarkets) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
        
            
                if(marketsData != null && ergoMarkets != null){
                    
                    marketsTip.setText("Ergo Markets: " + marketsData.getName());
                    m_addressesData.updateSelectedMarket(marketsData);
                }else{
                
                    if(ergoMarkets == null){
                        marketsTip.setText("(install 'Ergo Markets')");
                    }else{
                        marketsTip.setText("Select market...");
                    }
                }
            
            };

            ergoMarketsData.addListener((obs,oldval,newVal) -> {
                m_addressesData.getWalletData().setMarketsId(newVal != null ? newVal.getMarketId() : null);
                updateMarketsBtn.run();
            });

            Runnable getAvailableMarketsMenu = ()->{
                ErgoMarkets ergoMarkets = (ErgoMarkets) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoMarkets.NETWORK_ID);
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
                ErgoTokens ergoTokens = (ErgoTokens) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID);  
                boolean isEnabled = m_addressesData.isErgoTokensProperty().get();

                if(ergoTokens != null){
                    tokensBtn.setId("menuBtn");
                    MenuItem tokensEnabledItem = new MenuItem("Enabled" + (isEnabled ? " (selected)" : ""));
                    tokensEnabledItem.setOnAction(e->{
                        m_addressesData.isErgoTokensProperty().set(true);
                    });
                    

                    MenuItem tokensDisabledItem = new MenuItem("Disabled" + (isEnabled ? "" : " (selected)"));
                    tokensDisabledItem.setOnAction(e->{
                        m_addressesData.isErgoTokensProperty().set(false);
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
                        m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().showwManageStage();
                    });
                    tokensTip.setText("(install 'Ergo Tokens')");
                }
            
            };
    
            m_addressesData.isErgoTokensProperty().addListener((obs,oldval,newval)->{
                m_addressesData.getWalletData().setIsErgoTokens(newval);
                updateTokensMenu.run();
            });

            
            m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
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

            Tooltip sendTip = new Tooltip("Select address");
            sendTip.setShowDelay(new javafx.util.Duration(100));

            BufferedButton sendButton = new BufferedButton("/assets/arrow-send-white-30.png", imageWidth);
            sendButton.setTooltip(sendTip);
            sendButton.setId("menuBtn");
            sendButton.setUserData("sendButton");
    
            HBox menuBar = new HBox(sendButton, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            VBox layoutVBox = new VBox();
            
            Scene addressScene = new Scene(layoutVBox, getStageWidth(), getStageHeight());

            Text addressText = new Text(getName() + ": ");
            addressText.setFont(App.txtFont);
            addressText.setFill(App.txtColor);
            HBox.setHgrow(addressText,Priority.ALWAYS);

            final String addressString = m_address.toString();
   

            TextField addressField = new TextField(addressString);
            addressField.setId("addressField");
            addressField.setEditable(false);
            addressField.setPrefWidth(Utils.measureString(addressString, new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14)) + 30);

            BufferedButton copyAddressBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
            copyAddressBtn.setOnAction(e->{
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(m_address.toString());
                clipboard.setContent(content);
            });

            HBox addressBox = new HBox(addressText, addressField, copyAddressBtn);
            HBox.setHgrow(addressBox, Priority.ALWAYS);
            addressBox.setAlignment(Pos.CENTER_LEFT);
            addressBox.setPadding(new Insets(0,15,0,5));
            addressBox.setMinHeight(40);

            ErgoAmountBox ergoAmountBox = new ErgoAmountBox(ergoAmountProperty().get(), addressScene, m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData());
            HBox.setHgrow(ergoAmountBox,Priority.ALWAYS);
            ergoAmountBox.priceQuoteProperty().bind(m_addressesData.currentPriceQuoteProperty());
            ergoAmountBox.priceAmountProperty().bind(ergoAmountProperty());

         

            HBox amountBoxPadding = new HBox(ergoAmountBox);
            amountBoxPadding.setPadding(new Insets(10,10,0,10));
            amountBoxPadding.setId("darkBox");

            AmountBoxes amountBoxes = new AmountBoxes();
            amountBoxes.setId("darkBox");
            amountBoxes.setPadding(new Insets(10,10,10,0));
            amountBoxes.setAlignment(Pos.TOP_LEFT);
            amountBoxes.prefWidthProperty().bind(addressScene.widthProperty().subtract(43));

            TextField lastUpdatedField = new TextField();

            Runnable updateAmountBoxes = ()->{
                long timestamp = System.currentTimeMillis();
                for(int i = 0; i < m_confirmedTokensList.size() ; i ++){
                    PriceAmount tokenAmount = m_confirmedTokensList.get(i);
                    AmountBox amountBox = amountBoxes.getAmountBox(tokenAmount.getCurrency().getTokenId());
                    if(amountBox == null){
                        AmountBox newAmountBox = new AmountBox(tokenAmount, addressScene, m_addressesData.isErgoTokensProperty(), m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData());
                        newAmountBox.setTimeStamp(timestamp);
                        amountBoxes.add(newAmountBox);
                    }else{
                        amountBox.priceAmountProperty().set(tokenAmount);
                        amountBox.setTimeStamp(timestamp);
                    }
                }

                amountBoxes.removeOld(timestamp);

                lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now()));                
            };

            updateAmountBoxes.run();

            m_ergoAmountProperty.addListener((obs,oldval,newval)->updateAmountBoxes.run());


            VBox boxesVBox = new VBox( amountBoxes);
            HBox.setHgrow(boxesVBox, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane(boxesVBox);
            scrollPane.setPadding(new Insets(0,0,5, 0));

            
            Text updatedTxt = new Text("Updated:");
            updatedTxt.setFill(App.altColor);
            updatedTxt.setFont(Font.font("OCR A Extended", 10));

          
            lastUpdatedField.setPrefWidth(190);
            lastUpdatedField.setId("smallPrimaryColor");

            HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
            updateBox.setPadding(new Insets(2,2,2,0));
            updateBox.setAlignment(Pos.CENTER_RIGHT);

    
            VBox paddingAmountBox = new VBox(amountBoxPadding);        
            paddingAmountBox.setPadding(new Insets(5,16,0,0));
          
            
            VBox bodyBox = new VBox(addressBox, paddingAmountBox, scrollPane);
            bodyBox.setId("bodyBox");
            bodyBox.setPadding(new Insets(0,0,0,15));
            HBox.setHgrow(bodyBox,Priority.ALWAYS);

            VBox menuPaddingBox = new VBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0,0,4,0));

            VBox bodyPaddingBox = new VBox(menuPaddingBox, bodyBox);
            bodyPaddingBox.setPadding(new Insets(0,4, 0, 4));
            HBox.setHgrow(bodyPaddingBox,Priority.ALWAYS);

            layoutVBox.getChildren().addAll(titleBox, bodyPaddingBox, updateBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);
   

            addressScene.getStylesheets().add("/css/startWindow.css");

            m_addressStage.setScene(addressScene);
            m_addressStage.show();

            scrollPane.prefViewportHeightProperty().bind(layoutVBox.heightProperty().subtract(titleBox.heightProperty()).subtract(amountBoxPadding.heightProperty()).subtract(updateBox.heightProperty()).subtract(addressBox.heightProperty()));
            amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(50));
   
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_addressStage, 200, 250, rect.getWidth(), rect.getHeight());

            sendButton.setOnAction((actionEvent) -> {
                Scene sendScene = m_addressesData.getSendScene(addressScene, m_addressStage);
                if (sendScene != null) {
                
                    m_addressStage.setScene(sendScene);
                    Rectangle currentRect = getNetworksData().getMaximumWindowBounds();
                    ResizeHelper.addResizeListener(m_addressStage, ErgoWalletData.MIN_WIDTH, ErgoWalletData.MIN_HEIGHT, currentRect.getWidth(), currentRect.getHeight());
                
                }else{
                    Alert b = new Alert(AlertType.ERROR, "Unable open 'Send' window. Please try again.", ButtonType.OK);
                    b.show();
                }
            });


           /* Runnable updateTotal = ()->{
                ErgoAmount ergoAmount = m_ergoAmountProperty.get();
                PriceQuote priceQuote = m_addressesData.currentPriceQuoteProperty().get(); 

                String totalString = ergoAmount == null ? "Σ-" : ergoAmount.toString();

                double ergoAmountDouble = (ergoAmount != null ? ergoAmount.getDoubleAmount() : 0);
                double totalPrice = priceQuote != null ? priceQuote.getDoubleAmount() * ergoAmountDouble : 0;
                String quoteString = (priceQuote != null ? ": " + Utils.formatCryptoString( totalPrice , priceQuote.getQuoteCurrency(),priceQuote.getFractionalPrecision(),  ergoAmount != null) +" (" + priceQuote.toString() + ")" : "" );

                String text = totalString  + quoteString;

            
                Platform.runLater(() -> lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now())));
            };

            updateTotal.run();*/

           // m_addressesData.currentPriceQuoteProperty().addListener((obs, oldval, newval)->updateTotal.run());
          //  m_ergoAmountProperty.addListener((obs, oldval, newval)->updateTotal.run());

            closeBtn.setOnAction(closeEvent -> {
                removeShutdownListener();

                m_addressStage.close();
                m_addressStage = null;
            });

            m_addressStage.setOnCloseRequest((closeRequest) -> {

                closeBtn.fire();
            });
         
        } else {
            if(m_addressStage.isIconified()){
                m_addressStage.setIconified(false);
                m_addressStage.show();
                m_addressStage.setAlwaysOnTop(true);
            }
        }
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        /* JsonElement subjectElement = note.get("subject");
        if (subjectElement != null) {
            String subject = subjectElement.getAsString();
            switch (subject) {
                case "GET_EXPLORER_BALANCE_UPDATE":
                    return updateBalance();

            }
        }*/
        return false;
    }

    public int getIndex() {
        return m_index;
    }

    public boolean getValid() {
        return m_addressesData.selectedMarketData().get() != null && m_addressesData.selectedMarketData().get().priceQuoteProperty().get() != null && (m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getTimeStamp() - System.currentTimeMillis() < 1000 * 60 * 2);
    }

    public Address getAddress() {
        return m_address;
    }

    public String getAddressString() {
        return m_address.toString();
    }

    public String getAddressMinimal(int show) {
        String adr = m_address.toString();
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    public BigDecimal getConfirmedAmount() {
        return ErgoInterface.toFullErg(getConfirmedNanoErgs());
    }

    public NetworkType getNetworkType() {
        return m_address.getNetworkType();
    }

    public SimpleObjectProperty<ErgoAmount> ergoAmountProperty(){
        return m_ergoAmountProperty;
    }

    public long getConfirmedNanoErgs() {
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        return ergoAmount == null ? 0 : ergoAmount.getLongAmount();
    }

    public long getUnconfirmedNanoErgs() {
        return m_unconfirmedNanoErgs;
    }

    public ArrayList<PriceAmount> getConfirmedTokenList() {
        return m_confirmedTokensList;
    }

    public ArrayList<PriceAmount> getUnconfirmedTokenList() {
        return m_unconfirmedTokensList;
    }

    public double getFullAmountDouble() {
        return (double) getConfirmedNanoErgs() / 1000000000;
    }

    public double getFullAmountUnconfirmed() {
        return (double) getUnconfirmedNanoErgs() / 1000000000;
    }

    public double getPrice() {

        return getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getDoubleAmount() : 0.0;
    }

    public double getTotalAmountPrice() {
        return getFullAmountDouble() * getPrice();
    }



   

    public int getAmountInt() {
        return (int) getFullAmountDouble();
    }

    public double getAmountDecimalPosition() {
        return getFullAmountDouble() - getAmountInt();
    }

    public Image getUnitImage() {
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        if (ergoAmount == null) {
            return new Image("/assets/unknown-unit.png");
        } else {
            return ergoAmount.getCurrency().getIcon();
        }
    }

   

    public void updateBufferedImage() {
        ErgoAmount priceAmount = m_ergoAmountProperty.get();
        boolean quantityValid = priceAmount != null && priceAmount.getAmountValid();
        double priceAmountDouble = priceAmount != null && quantityValid ? priceAmount.getDoubleAmount() : 0;

        PriceQuote priceQuote = m_addressesData.currentPriceQuoteProperty().get();
        boolean priceValid = priceQuote != null && priceQuote.getTimeStamp() != 0 && priceQuote.howOldMillis() < m_quoteTimeout;
        double priceQuoteDouble = priceValid  && priceQuote != null ? priceQuote.getDoubleAmount() : 0;
        
        String totalPrice = priceValid && priceQuote != null ? Utils.formatCryptoString( priceQuoteDouble * priceAmountDouble, priceQuote.getQuoteCurrency(), priceQuote.getFractionalPrecision(),  quantityValid && priceValid) : " -.--";
        int integers = priceAmount != null ? (int) priceAmount.getDoubleAmount() : 0;
        double decimals = priceAmount != null ? priceAmount.getDoubleAmount() - integers : 0;
        int decimalPlaces = priceAmount != null ? priceAmount.getCurrency().getFractionalPrecision() : 0;
        String cryptoName = priceAmount != null ? priceAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = cryptoName.indexOf(" ");
        cryptoName = space != -1 ? cryptoName.substring(0, space) : cryptoName;

        String currencyPrice = priceValid && priceQuote != null ? priceQuote.toString() : "-.--";

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";
    
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(amountString);
       
        int height = fm.getHeight() + 10;

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);

        //  int priceAscent = fm.getAscent();
        int integersX = priceLength + 10;
        integersX = integersX < 130 ? 130 : integersX;
        int decimalsX = integersX + stringWidth + 1;

       // int cryptoNameStringWidth = fm.stringWidth(cryptoName);
        int decsWidth = fm.stringWidth(decs);

        int width = decimalsX + stringWidth + decsWidth + (padding * 2);
        int widthIncrease = width;
        width = width < m_minImgWidth ? m_minImgWidth : width;

        widthIncrease = width - widthIncrease;

        int cryptoNameStringX = decimalsX + 2;

        g2d.dispose();
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(priceAmount != null ? priceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        g2d.drawImage(unitImage,75, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(java.awt.Color.WHITE);

        

        g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

        g2d.setFont(smallFont);
        g2d.setColor(java.awt.Color.WHITE);
        fm = g2d.getFontMetrics();
        g2d.drawString(totalPrice, padding, fm.getHeight() + 2);

        g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        g2d.drawString(currencyPrice, padding, height - 10);

        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();

      

        setImageBuffer(SwingFXUtils.toFXImage(img, null));

    }

    public SimpleObjectProperty<Image> getImageProperty() {
        return m_imgBuffer;
    }

    private void setImageBuffer(Image image) {
        m_imgBuffer.set(image == null ? null : image);

       setGraphic(m_imgBuffer.get() == null ? null : getIconView(m_imgBuffer.get(), m_imgBuffer.get().getWidth()));
         getLastUpdated().set(LocalDateTime.now());
    }

    

    public void updateBalance() {

       ErgoExplorerData explorerData =  m_addressesData.selectedExplorerData().get();
   
        if (explorerData != null) {
            
                    explorerData.getBalance(m_address.toString(),
                    success -> {
                        Object sourceObject = success.getSource().getValue();

                        if (sourceObject != null) {
                            JsonObject jsonObject = (JsonObject) sourceObject;
                            
                            Platform.runLater(() ->setBalance(jsonObject));  
                        }
                    },
                    failed -> {
                          try {
                                Files.writeString(logFile.toPath(), "\nAddressData, Explorer failed update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {
                                
                              
                            }
                   
                        //update();
                    }
            );
      
        }
    }
    
    public void setBalance(JsonObject jsonObject){
        if (jsonObject != null) {


            JsonElement confirmedElement = jsonObject != null ? jsonObject.get("confirmed") : null;
            JsonElement unconfirmedElement = jsonObject.get("unconfirmed");
            if (confirmedElement != null && unconfirmedElement != null) {

                JsonObject confirmedObject = confirmedElement.getAsJsonObject();
                JsonObject unconfirmedObject = unconfirmedElement.getAsJsonObject();

                JsonElement nanoErgElement = confirmedObject.get("nanoErgs");

               

                m_unconfirmedNanoErgs = unconfirmedObject.get("nanoErgs").getAsLong();

                JsonElement confirmedArrayElement = confirmedObject.get("tokens");
                //JsonArray unconfirmedTokenArray = unconfirmedObject.get("tokens").getAsJsonArray();


              //  int confirmedSize = confirmedTokenArray.size();
                
                
                ErgoTokens ergoTokens = m_addressesData.isErgoTokensProperty().get() ? (ErgoTokens) m_addressesData.getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoTokens.NETWORK_ID) : null;
            
                TokensList tokensList = ergoTokens != null ? ergoTokens.getTokensList(getNetworkType()) : null;

               
                
                if(confirmedArrayElement != null && confirmedArrayElement.isJsonArray()){
                    JsonArray confirmedTokenArray = confirmedArrayElement.getAsJsonArray();
                   
                    m_confirmedTokensList.clear();

                    for (JsonElement tokenElement : confirmedTokenArray) {
                        JsonObject tokenObject = tokenElement.getAsJsonObject();

                        JsonElement tokenIdElement = tokenObject.get("tokenId");
                        JsonElement amountElement = tokenObject.get("amount");
                        JsonElement decimalsElement = tokenObject.get("decimals");
                        JsonElement nameElement = tokenObject.get("name");
                        JsonElement tokenTypeElement = tokenObject.get("tokenType");
                        
                        String tokenId = tokenIdElement.getAsString();
                        long amount = amountElement.getAsLong();
                        int decimals = decimalsElement.getAsInt();
                        String name = nameElement.getAsString();
                        String tokenType = tokenTypeElement.getAsString();
                  

                        ErgoNetworkToken networkToken = tokensList != null ? tokensList.getErgoToken(tokenId) : null;
    

                    
                        if(networkToken != null){
                            networkToken.setDecimals(decimals);
                           // networkToken.setName(name);
                            networkToken.setTokenType(tokenType);
                        }

                        PriceAmount tokenAmount = networkToken != null ? new PriceAmount(amount, networkToken) : new PriceAmount(amount, new PriceCurrency(tokenId, name, name, decimals, ErgoNetwork.NETWORK_ID, "/assets/unknown-unit.png",tokenType, ""));    
                        
                        
                        m_confirmedTokensList.add(tokenAmount);
                   
                    }
                }else{
                    try {
                        Files.writeString(new File("networkToken.txt").toPath(), "\ntoken json array " + (confirmedArrayElement != null ? confirmedArrayElement.toString() : "null"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) { 
                    
                    }
                }
           
                 if (nanoErgElement != null && nanoErgElement.isJsonPrimitive()) {
                    ErgoAmount ergoAmount = new ErgoAmount(nanoErgElement.getAsLong());
                    m_ergoAmountProperty.set(ergoAmount);
                }
            } 
        } 
    }
 
   /* public JsonArray getConfirmedTokenJsonArray() {
        JsonArray confirmedTokenArray = new JsonArray();
        m_confirmedTokensList.forEach(token -> {
            confirmedTokenArray.add(token.getJsonObject());
        });
        return confirmedTokenArray;
    }*/

    /*public JsonArray getUnconfirmedTokenJsonArray() {
        JsonArray unconfirmedTokenArray = new JsonArray();
        m_unconfirmedTokensList.forEach(token -> {
            unconfirmedTokenArray.add(token.getJsonObject());
        });
        return unconfirmedTokenArray;
    }*/

    public JsonObject getJsonObject() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("id", m_address.toString());
        jsonObj.addProperty("tickerName", m_priceBaseCurrency);
        jsonObj.addProperty("name", getName());
        jsonObj.addProperty("address", m_address.toString());
        jsonObj.addProperty("networkType", m_address.getNetworkType().toString());
      //  jsonObj.addProperty("explorerValidated", m_quantityValid);
        jsonObj.addProperty("marketValidated", getValid());

        return jsonObj;

    }

    private int m_apiIndex = 0;

    public int getApiIndex() {
        return m_apiIndex;
    }

    public String getPriceBaseCurrency() {
        return m_priceBaseCurrency;
    }

    public String getPriceTargetCurrency() {
        return m_priceTargetCurrency;
    }

    @Override
    public String toString() {
  
        return getText();
    }
}
