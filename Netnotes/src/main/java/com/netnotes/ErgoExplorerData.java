package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;


import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class ErgoExplorerData {
     public final static String TESTNET_STRING = NetworkType.TESTNET.toString();
     public final static String MAINNET_STRING = NetworkType.MAINNET.toString();
     
     private File logFile = new File("netnotes-log.txt");

   
     public final SimpleObjectProperty< ErgoNetworkUrl> ergoNetworkUrlProperty = new SimpleObjectProperty<>();
     private ErgoExplorerList m_explorerList = null;

     private String m_radioOffUrl = "/assets/radio-button-off-30.png";
     private String m_radioOnUrl = "/assets/radio-button-on-30.png";

    private String m_startImgUrl = "/assets/refresh-white-30.png";
    private String m_stopImgUrl = "/assets/stop-30.png";

     private Color m_secondaryColor = new Color(.4, .4, .4, .9);
     private Color m_primaryColor = new Color(.7, .7, .7, .9); 

     private Font m_largeFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
     private Font m_font = Font.font("OCR A Extended", FontWeight.BOLD, 13);
     private Font m_smallFont = Font.font("OCR A Extended", FontWeight.NORMAL, 10);

     public final SimpleStringProperty statusProperty = new SimpleStringProperty(ErgoMarketsData.STOPPED);
     public final SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(null);
     public final SimpleObjectProperty<LocalDateTime> shutdownNow = new SimpleObjectProperty<>(LocalDateTime.now());
     public final SimpleStringProperty displayTextProperty = new SimpleStringProperty("");

     private ChangeListener<LocalDateTime> m_updateListener = null;


     public ErgoExplorerData(String id, ErgoExplorerList explorerList){
       
          ergoNetworkUrlProperty.set(new ErgoNetworkUrl(id,"Ergo Platform", "https", "api.ergoplatform.com",443, NetworkType.MAINNET ));
          m_explorerList = explorerList;
          
     }

     public ErgoExplorerData(JsonObject json, ErgoExplorerList explorerList) throws Exception{
          m_explorerList = explorerList;
          openJson(json);
     }

     public void openJson(JsonObject json) throws Exception{
          JsonElement ergoNetworkUrl = json.get("ergoNetworkUrl");
          if(ergoNetworkUrl != null && ergoNetworkUrl.isJsonObject()){
               ergoNetworkUrlProperty.set( new ErgoNetworkUrl(ergoNetworkUrl.getAsJsonObject()) );          
          }else{
               throw new Exception("Insuffient data");
          }
     }
  
     public void getLatestBlocks(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        ErgoNetworkUrl namedUrl =  ergoNetworkUrlProperty.get();

        String urlString = namedUrl.getUrlString() + "/api/v1/blocks/";
        Utils.getUrlJson(urlString, onSucceeded, onFailed, null);
    }


    public void getTokenInfo(String tokenId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        ErgoNetworkUrl namedUrl =  ergoNetworkUrlProperty.get();

        String urlString = namedUrl.getUrlString() + "/api/v1/tokens/" + tokenId;
        Utils.getUrlJson(urlString, onSucceeded, onFailed, null);
    }

     public void getBalance(String address, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

          ErgoNetworkUrl namedUrl =  ergoNetworkUrlProperty.get();

          String urlString = namedUrl.getUrlString() + "/api/v1/addresses/" + address + "/balance/total";
   
          Utils.getUrlJson(urlString, onSucceeded, onFailed, null);
     }

     public String getId(){
          return ergoNetworkUrlProperty.get().getId();
     }

     public String getName() {
          return ergoNetworkUrlProperty.get() == null ? "INVALID" : ergoNetworkUrlProperty.get().getName();
     }

     public NetworkType getNetworkType() {
        return ergoNetworkUrlProperty.get() == null ? null : ergoNetworkUrlProperty.get().getNetworkType();
    }


     public String getNetworkTypeString() {
        return getNetworkType() != null ? getNetworkType().toString() : "NONE";
    }

    
     public HBox getRowItem() {
          String defaultId = m_explorerList.defaultIdProperty().get();

          BufferedButton defaultBtn = new BufferedButton(defaultId != null && getId().equals(defaultId) ? m_radioOnUrl : m_radioOffUrl, 15);

          m_explorerList.defaultIdProperty().addListener((obs, oldval, newVal) -> {
               defaultBtn.setImage(new Image(newVal != null && getId().equals(newVal) ? m_radioOnUrl : m_radioOffUrl));
          });

          defaultBtn.setOnAction(e->{
               String explorerDefaultId = m_explorerList.defaultIdProperty().get();

               if(explorerDefaultId != null && explorerDefaultId.equals(getId())){
                    m_explorerList.defaultIdProperty().set(null);
               }else{           
                    m_explorerList.defaultIdProperty().set(getId());
               }
          });

          Text topInfoStringText = new Text((ergoNetworkUrlProperty.get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
          topInfoStringText.setFont(m_font);
          topInfoStringText.setFill(m_primaryColor);

          Text topRightText = new Text("");
          topRightText.setFont(m_smallFont);
          topRightText.setFill(m_secondaryColor);

          Text botTimeText = new Text();
          botTimeText.setFont(m_smallFont);
          botTimeText.setFill(m_secondaryColor);
          botTimeText.textProperty().bind(lastUpdated.asString());

          TextField centerField = new TextField("test test ...testtset");
          centerField.setFont(m_largeFont);
          centerField.setId("formField");
          centerField.setEditable(false);
          centerField.setAlignment(Pos.CENTER);
          centerField.setPadding(new Insets(0, 10, 0, 0));
          HBox.setHgrow(centerField, Priority.ALWAYS);
          centerField.textProperty().bind(displayTextProperty);

          Text middleTopRightText = new Text();
          middleTopRightText.setFont(m_font);
          middleTopRightText.setFill(m_secondaryColor);

       //   middleTopRightText.textProperty().bind(cmdProperty);

          Text middleBottomRightText = new Text(getNetworkTypeString());
          middleBottomRightText.setFont(m_font);
          middleBottomRightText.setFill(m_primaryColor);

          VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
          centerRightBox.setAlignment(Pos.CENTER_RIGHT);

          VBox.setVgrow(centerRightBox, Priority.ALWAYS);

          BufferedButton statusBtn = new BufferedButton(statusProperty.get().equals(ErgoMarketsData.STOPPED) ? m_startImgUrl : m_stopImgUrl, 20);
          statusBtn.setId("statusBtn");
          statusBtn.setPadding(new Insets(0, 10, 0, 10));
          

          statusProperty.addListener((obs, oldVal, newVal) -> {
               switch (newVal) {
                    case ErgoMarketsData.STOPPED:
                    Platform.runLater(()->statusBtn.setImage(new Image(m_startImgUrl)));
                    break;
                    default:
                    Platform.runLater(()->statusBtn.setImage(new Image(m_stopImgUrl)));
                    break;
               }
          });

         

          HBox leftBox = new HBox(defaultBtn);
          HBox rightBox = new HBox(statusBtn);

          leftBox.setAlignment(Pos.CENTER_LEFT);
          rightBox.setAlignment(Pos.CENTER_RIGHT);
          leftBox.setId("bodyBox");
          rightBox.setId("bodyBox");

          Region currencySpacer = new Region();
          currencySpacer.setMinWidth(10);

          HBox centerBox = new HBox(centerField, centerRightBox);
          centerBox.setPadding(new Insets(0, 5, 0, 5));
          centerBox.setAlignment(Pos.CENTER_RIGHT);
          centerBox.setId("darkBox");

          HBox topSpacer = new HBox();
          HBox bottomSpacer = new HBox();

          topSpacer.setMinHeight(2);
          bottomSpacer.setMinHeight(2);

          HBox.setHgrow(topSpacer, Priority.ALWAYS);
          HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
          topSpacer.setId("bodyBox");
          bottomSpacer.setId("bodyBox");

          Region topMiddleRegion = new Region();
          HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

          HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
          topBox.setId("darkBox");

          Text urlText = new Text(ergoNetworkUrlProperty.get() != null ? (ergoNetworkUrlProperty.get().getUrlString() == null ? "INVALID" : ergoNetworkUrlProperty.get().getUrlString()) : "Configure url");
          urlText.setFill(m_primaryColor);
          urlText.setFont(m_smallFont);

          Region bottomMiddleRegion = new Region();
          HBox.setHgrow(bottomMiddleRegion, Priority.ALWAYS);

          HBox bottomBox = new HBox(urlText, bottomMiddleRegion, botTimeText);
          bottomBox.setId("darkBox");
          bottomBox.setAlignment(Pos.CENTER_LEFT);

          HBox.setHgrow(bottomBox, Priority.ALWAYS);

          VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
          HBox.setHgrow(bodyBox, Priority.ALWAYS);

          HBox rowBox = new HBox(leftBox, bodyBox, rightBox);
          rowBox.setAlignment(Pos.CENTER_LEFT);
          rowBox.setId("rowBox");

          rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            Platform.runLater(() -> {
                m_explorerList.selectedIdProperty().set(getId());
                e.consume();
            });
        });

          // centerBox.prefWidthProperty().bind(rowBox.widthProperty().subtract(leftBox.widthProperty()).subtract(rightBox.widthProperty()));
          Runnable updateBlockInfo = () ->{
               statusProperty.set(ErgoMarketsData.STARTED);
               getLatestBlocks((onSucceeded)->{
                    Object sourceValue = onSucceeded.getSource().getValue();
                    if(sourceValue != null && sourceValue instanceof JsonObject){
                         JsonObject latestBlocksObject = (JsonObject) sourceValue;
                         /*try {
                              Files.writeString(new File("blocks.json").toPath(), latestBlocksObject.toString());
                         } catch (IOException e) {
                    
                         }*/
                         
                         JsonElement itemsElement = latestBlocksObject.get("items");
                         if(itemsElement != null && itemsElement.isJsonArray()){
                              JsonArray itemsArray = itemsElement.getAsJsonArray();
                              if(itemsArray.size() > 0){

                                   JsonElement latestItemElement = itemsArray.get(0);
                                   if(latestItemElement.isJsonObject()){
                                        JsonObject latestItemObject = latestItemElement.getAsJsonObject();
                                        
                                        JsonElement heightElement = latestItemObject.get("height");
                                        JsonElement timestampElement = latestItemObject.get("timestamp");
                                        JsonElement minerRewardElement = latestItemObject.get("minerReward");
                                        String displayText = "";
                                        if(heightElement != null && heightElement.isJsonPrimitive()){
                                             long blockHeight = heightElement.getAsLong();
                                             displayText += "Latest block: " + blockHeight;
                                            
                                        }

                                        if(minerRewardElement != null && minerRewardElement.isJsonPrimitive()){
                                             long minerReward = minerRewardElement.getAsLong();
                                             ErgoAmount ergoAmount = new ErgoAmount(minerReward);

                                             displayText += (displayText.equals("") ? "" : " - ") + "Reward: " + ergoAmount;
                                        }
                                        if(timestampElement != null && timestampElement.isJsonPrimitive()){
                                             LocalDateTime latestItemTimestamp = Utils.milliToLocalTime(timestampElement.getAsLong());
                                             lastUpdated.set(latestItemTimestamp);
                                        }else{
                                             lastUpdated.set(LocalDateTime.now());
                                        }
                                        
                                        displayTextProperty.set(displayText);
                                        /*try {
                                             Files.writeString(new File("latestBlock.json").toPath(), latestItemObject.toString());
                                        } catch (IOException e) {
                                   
                                        }*/
                                   }

                              }
                         }
                    }else{
                        
                    }
                    Platform.runLater(()-> statusProperty.set(ErgoMarketsData.STOPPED));
               }, (onFailed)->{
                    displayTextProperty.set("Error: " + onFailed.getSource().getException().toString());
                    Platform.runLater(()-> statusProperty.set(ErgoMarketsData.STOPPED));
               });
          };
          updateBlockInfo.run();

          Runnable updateSelected = () -> {
            String selectedId =   m_explorerList.selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            centerField.setId(isSelected ? "selectedField" : "formField");

            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

          m_explorerList.selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());

          statusBtn.setOnAction(action -> {
               if (statusProperty.get().equals(ErgoMarketsData.STOPPED)) {
                   
               } else {
                    shutdownNow.set(LocalDateTime.now());
               }
          });

          return rowBox;
    }
    
    public JsonObject getJsonObject(){

          JsonObject json = new JsonObject();
          
          json.add("ergoNetworkUrl", ergoNetworkUrlProperty.get().getJsonObject());
          return json;
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_updateListener = changeListener;
        if (m_updateListener != null) {
            lastUpdated.addListener(m_updateListener);

        }
        // lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_updateListener != null) {
            lastUpdated.removeListener(m_updateListener);
            m_updateListener = null;
        }
    }

}