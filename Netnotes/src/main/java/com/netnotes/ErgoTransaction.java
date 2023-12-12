package com.netnotes;

import java.time.LocalDateTime;

import com.google.gson.JsonObject;

import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class ErgoTransaction {

    public final static PriceAmount UNKNOWN_PRICE_AMOUNT = new PriceAmount(0, new PriceCurrency("unknown","unknown","unknown",0,"unknown","unknown",null,"unknown",""));

    public static class TransactionType{
        public final static String SEND = "Send";
        public final static String ADVANCED = "Advanced";
    }

    public static class TransactionStatus{
        public final static String PENDING = "Pending";
        public final static String CONFIRMED = "Confirmed";
        public final static String FAILED = "Failed";
        public final static String UNKNOWN = "Failed";
    }

    private String m_txId;
    private AddressData m_parentAddress;
    private long m_timeStamp = 0;
    private String m_txType = TransactionType.ADVANCED;
    private SimpleStringProperty m_status = new SimpleStringProperty(TransactionStatus.UNKNOWN);

    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<>();

    public ErgoTransaction(String txId, AddressData parentAddress){
        m_txId = txId;
        m_parentAddress = parentAddress;
    }

    public String getTxType(){
        return m_txType;
    }

    public void setTxType(String type){
        m_txType = type;
    }

    public String getTxId(){
        return m_txId;
    }
    
    public long getTimeStamp(){
        return m_timeStamp;
    }

    public void setTimeStamp(long timeStamp){
        m_timeStamp = timeStamp;
    }    

    public SimpleStringProperty statusProperty(){
        return m_status;
    }

    public void open(){
        openLink();
    }

    public AddressData getParentAddress(){
        return m_parentAddress;
    }

    public boolean isErgoExplorer(){
        return getExplorerData() != null;
    }

    public ErgoExplorerData getExplorerData(){
        return getParentAddress().getAddressesData().selectedExplorerData().get();
    }

    public HostServices getHostServices(){
        return getParentAddress().getAddressesData().getWalletData().getErgoWallets().getNetworksData().getHostServices();
    }

    public void openLink(){
        String explorerUrlString = getExplorerData().getWebsiteTxLink(getTxId());
        getHostServices().showDocument(explorerUrlString);
    }

    public HBox getTxBox(){

        TextField txField = new TextField(m_txId);
        txField.setId("formField");
        HBox.setHgrow(txField, Priority.ALWAYS);

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

        BufferedButton linkBtn = new BufferedButton("/assets/link-20.png");
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

        return new HBox(txField, rightBox);
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated(){
        return m_lastUpdated;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
      
        json.addProperty("txtId", m_txId);
        json.addProperty("parentAddress", m_parentAddress.getAddress().toString());
        json.addProperty("timeStamp", m_timeStamp);
        json.addProperty("txType", m_txType);
        json.addProperty("status", m_status.get());
        return json;
     }
}
