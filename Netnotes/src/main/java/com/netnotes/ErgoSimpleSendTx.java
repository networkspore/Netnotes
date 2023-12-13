package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ErgoSimpleSendTx extends ErgoTransaction  {

   // private static File logFile = new File("netnotes-log.txt");
    
    private AddressInformation m_receipientAddress;
    private final static long FAILED_MILLIS = (1000*60*35);

    private SimpleLongProperty m_numConfirmations = new SimpleLongProperty(0);
    private String m_explorerUrl = "";
    private String m_nodeUrl;

    private ErgoAmount m_ergoAmount;
    private PriceAmount m_feeAmount;
    private PriceAmount[] m_tokens;
   

    private double m_stageWidth = 600;
    private double m_stageHeight = 600;
    private Stage m_stage;

    private int m_checkTimes = 0;
    private SimpleBooleanProperty m_updating = new SimpleBooleanProperty(false);


    private ChangeListener<LocalDateTime> m_updateTimeListener = (obs,oldval,newval)->{
        
      
        ErgoExplorerData explorerData = getExplorerData();
        if(explorerData != null){
            //check every 70 seconds
            
            if((m_checkTimes % 5) == 0){
                explorerData.getTransaction(getTxId(), (onSucceeded)->{
                    Object sourceValue = onSucceeded.getSource().getValue();
                    if(sourceValue != null && sourceValue instanceof JsonObject){
                        checkForConfirmation((JsonObject) sourceValue);

                        if(m_numConfirmations.get() >= 20){
                        
                            removeListener();
                        }
                    }
                }, (onFailed)->{
                     
                    long currentTimeMillis = System.currentTimeMillis();
                     if( (currentTimeMillis - getTimeStamp()) > FAILED_MILLIS){
                        statusProperty().set(TransactionStatus.FAILED);
                        removeListener();
                     }
                });
            
            }
        }
        m_checkTimes++;
    };

    public ErgoSimpleSendTx(String txId, AddressData parentAddress, AddressInformation receipientAddress, long ergoAmount, PriceAmount feeAmount, PriceAmount[] tokens, String nodeUrl,  String explorerUrl, String status, long created){
        super(txId, parentAddress);

       
    
        m_ergoAmount = new ErgoAmount(ergoAmount, getNetworkType());
        m_receipientAddress = receipientAddress;
        m_feeAmount = feeAmount;
        m_tokens = tokens == null ? new PriceAmount[0] : tokens; 
        m_nodeUrl = nodeUrl;
        m_explorerUrl = explorerUrl == null ? "" : explorerUrl;
        statusProperty().set(status);
        setTimeStamp(created);
        setTxType(TransactionType.SEND);
     
        
        addListener();
    }

    public void addListener(){
        m_updating.set(true);
        getParentAddress().getAddressesData().timeCycleProperty().addListener(m_updateTimeListener);
    }

    public ErgoSimpleSendTx(String txId,AddressData parentAddress, JsonObject json) throws Exception{
        super(txId, parentAddress);
        setTxType(TransactionType.SEND);

        JsonElement nanoErgsElement = json.get("nanoErgs");
        JsonElement feeAmountElement = json.get("feeAmount");
        JsonElement tokensElement = json.get("tokens");

        JsonElement explorerUrlElement = json.get("explorerUrl");
        JsonElement nodeUrlElement = json.get("nodeUrl");
        JsonElement statusElement = json.get("status");
        JsonElement timeStampElement = json.get("timeStamp");


      

        if(nanoErgsElement == null || feeAmountElement == null || parentAddress == null){
            throw new Exception("Invalid arguments");
        }

        if(timeStampElement != null && timeStampElement.isJsonPrimitive()){
            setTimeStamp(timeStampElement.getAsLong());
        }else{
            setTimeStamp(System.currentTimeMillis());
        }



        m_ergoAmount = new ErgoAmount(nanoErgsElement.getAsLong(), getParentAddress().getNetworkType());
        m_feeAmount = new PriceAmount(feeAmountElement.getAsJsonObject());

        m_explorerUrl = explorerUrlElement != null && explorerUrlElement.isJsonPrimitive() ? explorerUrlElement.getAsString() : "";
        m_nodeUrl = nodeUrlElement != null && nodeUrlElement.isJsonPrimitive() ? nodeUrlElement.getAsString() : "";
        JsonArray tokensArray = tokensElement != null && tokensElement.isJsonArray() ? tokensElement.getAsJsonArray() : new JsonArray();
        int tokensArrayLength = tokensArray.size();

        PriceAmount[] tokenAmounts = new PriceAmount[tokensArrayLength];
        for(int i = 0; i < tokensArrayLength ; i ++ ){
            JsonElement tokenElement = tokensArray.get(i);
            
            tokenAmounts[i] = tokenElement != null && tokenElement.isJsonObject() ? new PriceAmount(tokenElement.getAsJsonObject()) : UNKNOWN_PRICE_AMOUNT;
        }
        String status = statusElement != null && statusElement.isJsonPrimitive() ? statusElement.getAsString() : TransactionStatus.PENDING;
        statusProperty().set(status);

        m_tokens = tokenAmounts;
 
        if(status.equals(TransactionStatus.PENDING)){
            addListener();
        }
        
    }

    private void checkForConfirmation(JsonObject json){
        if(json != null){
            JsonElement numConfirmationsElement = json.get("numConfirmations");

            if(numConfirmationsElement != null && numConfirmationsElement.isJsonPrimitive()){
                long numConfirmations = numConfirmationsElement.getAsLong();

                if(numConfirmations > 0){
                    statusProperty().set(TransactionStatus.CONFIRMED);
                    m_numConfirmations.set(numConfirmations);
                  
                }
            }
        }
    }

    public void removeListener(){
        
        getParentAddress().getAddressesData().timeCycleProperty().removeListener(m_updateTimeListener);
        m_updating.set(false);
    }

 
    public void open(){
        showErgoTxStage();
    }

    public void showErgoTxStage(){
        if(m_stage == null){
            Runnable getUpdates = ()->{
                if(!m_updating.get() && !statusProperty().get().equals(TransactionStatus.FAILED)){
                    getParentAddress().getAddressesData().timeCycleProperty().addListener((obs,oldval, newval)->{
                        ErgoExplorerData explorerData = getExplorerData();
                        if(explorerData != null){
                            //check every 70 seconds
                  
                            if((m_checkTimes % 5) == 0){
                                explorerData.getTransaction(getTxId(), (onSucceeded)->{
                                    Object sourceValue = onSucceeded.getSource().getValue();
                                    if(sourceValue != null && sourceValue instanceof JsonObject){
                                        checkForConfirmation((JsonObject) sourceValue);
                                    }
                                }, (onFailed)->{
                                    
                                    long currentTimeMillis = System.currentTimeMillis();
                                    if( (currentTimeMillis - getTimeStamp()) > FAILED_MILLIS){
                                        statusProperty().set(TransactionStatus.FAILED);
                                    }
                                });
                            
                            }

                        }
                        m_checkTimes++;
                    });
                }
            };
            getUpdates.run();
            m_updating.addListener((obs,oldval,newval)->getUpdates.run());
            VBox layoutVBox = new VBox();
            Scene txScene = new Scene(layoutVBox, m_stageWidth, m_stageHeight);
            txScene.getStylesheets().add("/css/startWindow.css");

            String titleString = "Send - " + statusProperty().get() +": " + m_ergoAmount.toString() + " - " + getTxId();

            m_stage = new Stage();
            m_stage.getIcons().add(ErgoWallets.getAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(titleString);

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), titleString, closeBtn, m_stage);

            Tooltip explorerTip = new Tooltip("Select explorer");
            explorerTip.setShowDelay(new javafx.util.Duration(50));
            explorerTip.setFont(App.txtFont);



            BufferedMenuButton explorerBtn = new BufferedMenuButton("/assets/ergo-explorer-30.png", App.MENU_BAR_IMAGE_WIDTH);
            explorerBtn.setPadding(new Insets(2, 0, 0, 2));
            explorerBtn.setTooltip(explorerTip);

            Runnable updateExplorerBtn = () ->{
                ErgoExplorers ergoExplorers = (ErgoExplorers) getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);

                ErgoExplorerData explorerData = getParentAddress().getAddressesData().selectedExplorerData().get();
            
            
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
            
                ErgoExplorers ergoExplorers = (ErgoExplorers) getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().getNetwork(ErgoExplorers.NETWORK_ID);
                if(ergoExplorers != null){
                    explorerBtn.setId("menuBtn");
                    ergoExplorers.getErgoExplorersList().getMenu(explorerBtn, getParentAddress().getAddressesData().selectedExplorerData());
                }else{
                    explorerBtn.getItems().clear();
                    explorerBtn.setId("menuBtnDisabled");
                
                }
                updateExplorerBtn.run();
            };    

            getParentAddress().getAddressesData().selectedExplorerData().addListener((obs, oldval, newval)->{
                getParentAddress().getAddressesData().getWalletData().setExplorer(newval == null ? null : newval.getId());
                updateExplorerBtn.run();
            });

            HBox rightSideMenu = new HBox( explorerBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 0, 0, 0));
            rightSideMenu.setAlignment(Pos.CENTER_RIGHT);

             Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox menuBar = new HBox(spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));


            Text txText = new Text();
            txText.setFont(App.txtFont);
            txText.setFill(App.txtColor);
            txText.textProperty().bind(Bindings.concat("Send - ", statusProperty(), Bindings.when(m_numConfirmations.greaterThan(1)).then( Bindings.concat(" (",m_numConfirmations," confirmations)")).otherwise("") ));

            
            Text txIdText = new Text("Tx:");
            txIdText.setFont(App.txtFont);
            txIdText.setFill(App.txtColor);

            TextField txField = new TextField(getTxId());
            txField.setId("txField");
            txField.setEditable(false);
            txField.setPrefWidth(Utils.measureString(getTxId(), new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14)) + 30);

            Tooltip copiedTooltip = new Tooltip("copied");

            BufferedButton copyTxBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
            copyTxBtn.setOnAction(e->{
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(getTxId());
                clipboard.setContent(content);

                Point2D p = copyTxBtn.localToScene(0.0, 0.0);

                copiedTooltip.show(
                    copyTxBtn,  
                    p.getX() + copyTxBtn.getScene().getX() + copyTxBtn.getScene().getWindow().getX(), 
                    (p.getY()+ copyTxBtn.getScene().getY() + copyTxBtn.getScene().getWindow().getY())-copyTxBtn.getLayoutBounds().getHeight()
                    );
                PauseTransition pt = new PauseTransition(Duration.millis(1600));
                pt.setOnFinished(ptE->{
                    copiedTooltip.hide();
                });
                pt.play();
            });

            Tooltip unknownExplorerTip = new Tooltip("Select Explorer");

        

            BufferedButton linkBtn = new BufferedButton("/assets/link-20.png", App.MENU_BAR_IMAGE_WIDTH);
            
            linkBtn.setOnAction(e->{
                if(isErgoExplorer()){
                    openLink();
                }else{
                    Point2D p = linkBtn.localToScene(0.0, 0.0);

                    unknownExplorerTip.show(
                        linkBtn,  
                        p.getX() + linkBtn.getScene().getX() + linkBtn.getScene().getWindow().getX(), 
                        (p.getY()+ linkBtn.getScene().getY() + linkBtn.getScene().getWindow().getY())-linkBtn.getLayoutBounds().getHeight()
                    );

                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE->{
                        unknownExplorerTip.hide();
                    });
                    pt.play();
                }
            });

            HBox txBox = new HBox(txText);
            txBox.setId("headingBox");
            HBox.setHgrow(txBox, Priority.ALWAYS);
            txBox.setAlignment(Pos.CENTER_LEFT);
            txBox.setPadding(new Insets(0,15,0,15));
            txBox.setMinHeight(40);

            HBox txidBox = new HBox( txIdText, txField, copyTxBtn, linkBtn);
            HBox.setHgrow(txidBox, Priority.ALWAYS);
            txidBox.setMinHeight(40);
            txidBox.setPadding(new Insets(0,15,0,10));
            txidBox.setAlignment(Pos.CENTER_LEFT);

            AmountConfirmBox ergoAmountBox = new AmountConfirmBox(m_ergoAmount, m_feeAmount, txScene);
            HBox.setHgrow(ergoAmountBox, Priority.ALWAYS);
            ergoAmountBox.priceQuoteProperty().bind(getParentAddress().getAddressesData().currentPriceQuoteProperty());


            HBox amountBoxPadding = new HBox(ergoAmountBox);
            amountBoxPadding.setPadding(new Insets(10, 10, 0, 10));

            AmountBoxes amountBoxes = new AmountBoxes();
            amountBoxes.setPadding(new Insets(5, 10, 5, 0));
            amountBoxes.setAlignment(Pos.TOP_LEFT);

    
            if (m_tokens != null && m_tokens.length > 0) {
                int numTokens = m_tokens.length;
                for (int i = 0; i < numTokens; i++) {
                    PriceAmount tokenAmount = m_tokens[i];

                    AmountConfirmBox confirmBox = new AmountConfirmBox(tokenAmount,null, txScene);
                    amountBoxes.add(confirmBox);
                    
                }
            }
            
  
            
            VBox boxesVBox = new VBox(amountBoxPadding, amountBoxes);
            HBox.setHgrow(boxesVBox, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane(boxesVBox);
            scrollPane.setPadding(new Insets(0, 0, 5, 0));
            

            VBox bodyBox = new VBox(txBox, txidBox, scrollPane);
            bodyBox.setPadding(new Insets(4));
            bodyBox.setId("bodyBox");

            Region menuBarRegion = new Region();
            menuBarRegion.setMinHeight(4);

            VBox bodyPaddingBox = new VBox(menuBar,menuBarRegion,bodyBox);
            
            bodyPaddingBox.setPadding(new Insets(0,4,4,4));
            layoutVBox.getChildren().addAll(titleBox,bodyPaddingBox);

            m_stage.setScene(txScene);
            
            getParentAddress().getAddressesData().getWalletData().getErgoWallets().getErgoNetworkData().addNetworkListener((ListChangeListener.Change<? extends NoteInterface> c) -> {
               
                getAvailableExplorerMenu.run();
            });

            getAvailableExplorerMenu.run();

            
            scrollPane.prefViewportWidthProperty().bind(txScene.widthProperty().subtract(60));
            scrollPane.prefViewportHeightProperty().bind(txScene.heightProperty().subtract(titleBox.heightProperty()).subtract(txBox.heightProperty()).subtract(10));
            amountBoxes.minHeightProperty().bind(scrollPane.prefViewportHeightProperty().subtract(60));
            amountBoxes.prefWidthProperty().bind(txScene.widthProperty().subtract(60));

            java.awt.Rectangle rect = getParentAddress().getAddressesData().getWalletData().getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_stage, 200, 250, rect.getWidth(), rect.getHeight());
            
            m_stage.show();
            closeBtn.setOnAction(e->{
                m_stage.close();
                m_stage = null;
            });

            m_stage.setOnCloseRequest(e->closeBtn.fire());
        }else{
            if(m_stage.isIconified()){
                m_stage.setIconified(false);
                m_stage.show();
                m_stage.toFront();
            }else{
                Platform.runLater(()-> m_stage.requestFocus());
            }
        }
    }

  
   

    @Override
    public HBox getTxBox(){

        Text txStatus = new Text();
        txStatus.setFont(App.txtFont);
        txStatus.setFill(App.txtColor);
        txStatus.textProperty().bind(statusProperty());

        String tokensString = "";
        for(int i = 0; i < m_tokens.length ; i ++){
            tokensString += ", " + m_tokens[i].toString();
        }

        Button txText = new Button(m_ergoAmount.toString() + tokensString);
        txText.setFont(App.txtFont);
        txText.setId("formField");

        txText.setMinWidth(100);
        txText.setTextAlignment(TextAlignment.LEFT);
        txText.setAlignment(Pos.CENTER_LEFT);
        txText.setPrefHeight(25);

    

        TextField txField = new TextField(getTxId());
        txField.setId("addressField");
        txField.setEditable(false);
        txField.setPrefWidth(Utils.measureString(getTxId(), new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 14)) + 30);
        
        Tooltip copiedTooltip = new Tooltip("copied");

        BufferedButton copyTxBtn = new BufferedButton("/assets/copy-30.png", App.MENU_BAR_IMAGE_WIDTH);
        copyTxBtn.setOnAction(e->{
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(getTxId());
            clipboard.setContent(content);

            Point2D p = copyTxBtn.localToScene(0.0, 0.0);

            copiedTooltip.show(
                copyTxBtn,  
                p.getX() + copyTxBtn.getScene().getX() + copyTxBtn.getScene().getWindow().getX(), 
                (p.getY()+ copyTxBtn.getScene().getY() + copyTxBtn.getScene().getWindow().getY())-copyTxBtn.getLayoutBounds().getHeight()
                );
            PauseTransition pt = new PauseTransition(Duration.millis(1600));
            pt.setOnFinished(ptE->{
                copiedTooltip.hide();
            });
            pt.play();
        });

        BufferedButton openBtn = new BufferedButton("/assets/open-outline-white-20.png", 15);
        openBtn.setOnAction(e->{
            open();
        });

        Tooltip unknownExplorerTip = new Tooltip("Select Explorer");

        BufferedButton linkBtn = new BufferedButton("/assets/link-20.png", App.MENU_BAR_IMAGE_WIDTH);
        linkBtn.setOnAction(e->{
            if(isErgoExplorer()){
                    openLink();
                }else{
                    Point2D p = linkBtn.localToScene(0.0, 0.0);

                    unknownExplorerTip.show(
                        linkBtn,  
                        p.getX() + linkBtn.getScene().getX() + linkBtn.getScene().getWindow().getX(), 
                        (p.getY()+ linkBtn.getScene().getY() + linkBtn.getScene().getWindow().getY())-linkBtn.getLayoutBounds().getHeight()
                    );

                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE->{
                        unknownExplorerTip.hide();
                    });
                    pt.play();
                }
        });
        HBox topRightBox = new HBox( copyTxBtn, linkBtn, openBtn);

        HBox botRightBox = new HBox();
        botRightBox.setMinHeight(10);

        VBox rightBox = new VBox(topRightBox, botRightBox);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        HBox txTextBox = new HBox(txText);
        txTextBox.setPadding(new Insets(5,5,5,15));

        VBox leftVBox = new VBox(txStatus, txTextBox);

        HBox txBox = new HBox(leftVBox, txField, rightBox);
        HBox.setHgrow(txBox, Priority.ALWAYS);
        txBox.setAlignment(Pos.CENTER_LEFT);
        txBox.setPadding(new Insets(10,15,0,10));
        txBox.setMinHeight(40);
        txBox.setId("rowBox");

        txBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e->{
            getParentAddress().selectedTransaction().set(this);
            if(e.getClickCount() == 2){
                open();
            }
        }); 

        getParentAddress().selectedTransaction().addListener((obs,oldval,newval)->{
            if(newval != null && newval.getTxId().equals(getTxId())){
                txBox.setId("bodyRowBox");
            }else{
                txBox.setId("rowBox");
            }
        });
        
        return txBox;
    }




    public PriceAmount[] getTokens(){
        return m_tokens;
    }

  

   
    public PriceAmount getFeeAmount(){
        return m_feeAmount;
    }

    public PriceAmount getErgoAmount(){
        return m_ergoAmount;
    }

    public AddressInformation getReceipientAddressInfo(){
        return m_receipientAddress;
    }

    public NetworkType getNetworkType(){
        return getParentAddress().getNetworkType();
    }

    public String getExplorerUrl(){
        return m_explorerUrl;
    }

    public String getNodeUrl(){
        return m_nodeUrl;
    }


    public boolean isSent(){
        return getTxId() != null;
    }


    public JsonArray getTokenJsonArray(){
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < m_tokens.length ; i++){
            jsonArray.add(m_tokens[i].getJsonObject());
        }
        return jsonArray;
    }

    @Override
    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("txId",getTxId());
        json.addProperty("parentAddress",getParentAddress().getAddress().toString());
        json.addProperty("timeStamp", getTimeStamp());
        json.addProperty("txType", getTxType());
        json.addProperty("status", statusProperty().get());

        json.addProperty("explorerUrl", m_explorerUrl);
        json.addProperty("nodeUrl", m_nodeUrl);
        json.add("feeAmount", m_feeAmount.getJsonObject());
        json.addProperty("nanoErgs", m_ergoAmount.getLongAmount());
        json.add("tokens", getTokenJsonArray());
        json.add("recipientAddress", m_receipientAddress.getJsonObject());
        
        return json;

    }
    
    
}
