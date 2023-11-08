package com.netnotes;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

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

        setAlignment(Pos.CENTER_LEFT);

        String textFieldId = m_id +"TextField";

        TextField amountField = new TextField();
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_RIGHT);

        amountField.setPadding(new Insets(3, 10, 3, 0));
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

        amountBtn.setOnAction(actionEvent -> {
            getChildren().remove(amountBtn);
            getChildren().addAll(amountField, enterButton);
            Platform.runLater(() -> amountField.requestFocus());
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
            if (!(getChildren().contains(amountBtn))) {
                getChildren().add(amountBtn);
            }
        };
        enterButton.setOnAction(e->{
            PriceAmount newAmount = new PriceAmount(Double.parseDouble(amountField.getText()), m_currentAmount.get().getCurrency());
            m_currentAmount.set(newAmount);
            setNotFocused.run();
        });

         amountField.setOnAction(e->{
            enterButton.fire();
        });


    }
}
