package com.netnotes;

import java.text.DecimalFormat;

import com.devskiller.friendly_id.FriendlyId;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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

public class AmountBox extends HBox {

    private SimpleObjectProperty<PriceAmount> m_currentAmount = new SimpleObjectProperty<PriceAmount>(null);
    private String m_id = FriendlyId.createFriendlyId();
 
   

    public AmountBox(PriceAmount priceAmount, Scene scene) {
        super();
        setId("blackBox");
        setMinHeight(40);

        m_currentAmount.set(priceAmount);
  

       
        
        Button amountBtn = new Button();
        amountBtn.setId("amountBtn");
        amountBtn.textProperty().bind(m_currentAmount.asString());
        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
        amountBtn.setPadding(new Insets(2, 5, 2, 10));
        amountBtn.setGraphicTextGap(25);
        Runnable updateAmountBtnGraphic = ()->{
   
            amountBtn.setGraphic(IconButton.getIconView( new Image( priceAmount.getCurrency().getImageString()),35));
        };
        updateAmountBtnGraphic.run();

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";

        int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

     

        TextField amountField = new TextField(df.format(priceAmount.getDoubleAmount()));
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);

        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        amountField.textProperty().addListener((obs, oldval, newval)->{
           
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > 9 ? rightSide.substring(0, 9) : rightSide;
        
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
            getChildren().addAll( imgPaddingBox, amountField, enterButton);
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
            String text = amountField.getText();
            PriceAmount newAmount = new PriceAmount(Double.parseDouble(text.equals("") ? "0" : text), m_currentAmount.get().getCurrency());
            m_currentAmount.set(newAmount);
            setNotFocused.run();
        });

         amountField.setOnAction(e->{
            enterButton.fire();
        });

        m_currentAmount.addListener((obs,oldval, newval)->updateAmountBtnGraphic.run());
        
    }

     public AmountBox(String id, PriceAmount priceAmount, Scene scene) {
        this(priceAmount, scene);
        m_id = id;
    }

    public String getBoxId(){
        return m_id;
    }

    public void setBoxId(String id){
        m_id = id;
    }

   
}
