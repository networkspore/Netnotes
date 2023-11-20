package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountSendBox extends AmountBox {



    public AmountSendBox(PriceAmount priceAmount, Scene scene, boolean editable) {
        super();
        setId("darkBox");
        setMinHeight(40);
        priceAmountProperty().set(priceAmount);
        
        
        Button amountBtn = new Button();
        amountBtn.setId("amountBtn");
       // amountBtn.textProperty().bind(m_currentAmount.asString());
        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
        amountBtn.setPadding(new Insets(2, 5, 2, 10));
        amountBtn.setGraphicTextGap(25);
        imageBufferProperty().addListener((obs,oldval,newval)-> {
            if(newval != null){    
                amountBtn.setGraphic(IconButton.getIconView(newval, newval.getWidth()));
            }
        });

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = getBoxId() +"TextField";

        int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

     

        TextField amountField = new TextField(df.format(priceAmount.getDoubleAmount()));
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(editable);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        amountField.textProperty().addListener((obs, oldval, newval)->{
           
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > priceAmount.getCurrency().getDecimals() ? rightSide.substring(0, priceAmount.getCurrency().getDecimals()) : rightSide;
        
            amountField.setText(leftSide +  rightSide);
        });
      

        Button enterButton = new Button("[ ENTER ]");
        enterButton.setFont(App.txtFont);
        enterButton.setId("toolBtn");
   

       
        SimpleBooleanProperty isFieldFocused = new SimpleBooleanProperty(false);
        
        scene.focusOwnerProperty().addListener((obs, old, newPropertyValue) -> {
            if (newPropertyValue != null && newPropertyValue instanceof TextField) {
                TextField focusedField = (TextField) newPropertyValue;
                Object userData = focusedField.getUserData();
                if(userData != null && userData instanceof String){
                    String userDataString = (String) userData;
                    if(userDataString.equals(textFieldId)){
                        isFieldFocused.set(true);
                    }else{
                        if(isFieldFocused.get()){
                            isFieldFocused.set(false);
                            enterButton.fire();
                        }
                    }
                }else{
                    if(isFieldFocused.get()){
                        isFieldFocused.set(false);
                        enterButton.fire();
                    }
                }
            }else{
                if(isFieldFocused.get()){
                    isFieldFocused.set(false);
                    enterButton.fire();
                }
            }
        });

        HBox.setHgrow(amountField, Priority.ALWAYS);

        ImageView textViewImage = IconButton.getIconView( new Image( priceAmount.getCurrency().getImageString()),35);

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setMinHeight(40);
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);

        amountBtn.setOnAction(actionEvent -> {
            getChildren().remove(amountBtn);
            if(editable){
                getChildren().addAll( imgPaddingBox, amountField, enterButton);
            }else{
                 getChildren().addAll( imgPaddingBox, amountField);
            }
            if(priceAmount.getDoubleAmount() == 0){
                Platform.runLater(() -> {
                    amountField.requestFocus();
                    amountField.selectAll();
                });
            }else{
                Platform.runLater(()-> {
                    amountField.requestFocus();

                });
            }
        });

 
        getChildren().add(amountBtn);

        amountBtn.prefWidthProperty().bind(this.widthProperty());

        Runnable setNotFocused = () ->{
            if (getChildren().contains(enterButton)) {
                getChildren().remove(enterButton);
            }

            if (getChildren().contains(amountField)) {
                getChildren().remove(amountField);

            }
            if (getChildren().contains( imgPaddingBox)) {
                getChildren().removeAll( imgPaddingBox);
            }

            if (!(getChildren().contains(amountBtn))) {
                getChildren().add(amountBtn);
            }

            
        };
        enterButton.setOnAction(e->{
            if(editable){
                String text = amountField.getText();
                PriceAmount newAmount = new PriceAmount(Double.parseDouble(text.equals("") ? "0" : text), priceAmountProperty().get().getCurrency());
                priceAmountProperty().set(newAmount);
            }
            setNotFocused.run();
        });

         amountField.setOnAction(e->{
            enterButton.fire();
        });
        
        priceQuoteProperty().addListener((obs, oldval, newval)->updateBufferedImage());

        priceAmountProperty().addListener((obs,oldval, newval)-> updateBufferedImage());
  
        updateBufferedImage();
    }



   

}
