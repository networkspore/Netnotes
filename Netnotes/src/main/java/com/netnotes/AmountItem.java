package com.netnotes;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class AmountItem extends HBox {

    private SimpleObjectProperty<PriceAmount> m_currentAmount = new SimpleObjectProperty<PriceAmount>(null);

    public AmountItem(PriceAmount priceAmount) {
        super();

        m_currentAmount.set(priceAmount);

        Button amountBtn = new Button();
        amountBtn.setId("amountBtn");
        amountBtn.textProperty().bind(m_currentAmount.asString());
        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
        amountBtn.setPadding(new Insets(2, 5, 2, 10));

        setAlignment(Pos.CENTER_LEFT);

        TextField amountField = new TextField() {
            @Override
            public void paste() {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                if (clipboard.hasString()) {
                    if (getSelectedText().length() > 0) {
                        replaceSelection(clipboard.getString());
                    } else {
                        this.setText(clipboard.getString());
                    }
                }

                PriceAmount priceAmount = Utils.getAmountByString(getText(), m_currentAmount.get().getCurrency());

                m_currentAmount.set(priceAmount);
            }
        };
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");

        amountField.setPadding(new Insets(3, 10, 3, 0));

        Button enterButton = new Button("[ ENTER ]");
        enterButton.setFont(App.txtFont);
        enterButton.setId("toolBtn");

        amountField.setOnKeyPressed(e -> {
            // e.consume(); 
            KeyCode keyCode = e.getCode();
            // char    charTyped = e.getCharacter().charAt(0);

            if (keyCode == KeyCode.ENTER) {
                // Object currency = m_currencyMenuBtn.getUserData();

                PriceAmount newAmount = Utils.getAmountByString(amountField.getText(), m_currentAmount.get().getCurrency());
                m_currentAmount.set(newAmount);

            } else {
                if (!getChildren().contains(enterButton)) {
                    getChildren().add(enterButton);
                }
            }

        });
        amountField.focusedProperty().addListener((obs, old, newPropertyValue) -> {
            if (!newPropertyValue) {
                PriceAmount newAmount = Utils.getAmountByString(amountField.getText(), m_currentAmount.get().getCurrency());
                m_currentAmount.set(newAmount);
            }
        });

        HBox.setHgrow(amountField, Priority.ALWAYS);

        amountBtn.setOnAction(actionEvent -> {
            getChildren().remove(amountBtn);
            getChildren().add(amountField);
            Platform.runLater(() -> amountField.requestFocus());
        });

        m_currentAmount.addListener((obs, old, newValue) -> {

            if (getChildren().contains(enterButton)) {
                getChildren().remove(enterButton);
            }

            if (getChildren().contains(amountField)) {
                getChildren().remove(amountField);

            }
            if (!(getChildren().contains(amountBtn))) {
                getChildren().add(amountBtn);
            }
            //  amountBtn.setGraphic(m_currentAmount);

        });

        getChildren().add(amountBtn);

        amountBtn.prefWidthProperty().bind(this.widthProperty());

    }
}
