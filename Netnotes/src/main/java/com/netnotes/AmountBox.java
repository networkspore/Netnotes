package com.netnotes;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class AmountBox extends HBox {

    public final static PriceCurrency DEFAULT_CURRENCY = new ErgoCurrency();
    public final static int IMAGE_WIDTH = 40;

    private SimpleObjectProperty<PriceCurrency> m_currentCurrency = new SimpleObjectProperty<PriceCurrency>(DEFAULT_CURRENCY);
    private SimpleObjectProperty<PriceAmount> m_currentAmount = new SimpleObjectProperty<PriceAmount>(new PriceAmount(0, DEFAULT_CURRENCY));

    private MenuButton m_currencyMenuBtn = new MenuButton();

    private MenuItem m_defaultItem;

    public AmountBox(double amount, PriceCurrency defaultCurrency, Node... children) {
        super(children);

        m_currentCurrency.set(defaultCurrency);

        Button amountBtn = new Button();
        amountBtn.setId("amountBtn");
        amountBtn.textProperty().bind(m_currentAmount.asString());

        m_currentAmount.set(new PriceAmount(amount, m_currentCurrency.get()));

        //  = m_defaultCurrency;
        HBox.setHgrow(this, Priority.ALWAYS);
        setAlignment(Pos.CENTER_LEFT);

        m_defaultItem = new MenuItem(DEFAULT_CURRENCY.getName(), IconButton.getIconView(DEFAULT_CURRENCY.getUnitImage(), IMAGE_WIDTH));
        m_defaultItem.setUserData(DEFAULT_CURRENCY);
        m_defaultItem.setOnAction(e -> {
            m_currentCurrency.set(DEFAULT_CURRENCY);
        });

        m_currencyMenuBtn.setGraphic(IconButton.getIconView(m_currentCurrency.get().getUnitImage(), IMAGE_WIDTH));

        m_currencyMenuBtn.setPadding(new Insets(0));

        m_currencyMenuBtn.getItems().add(m_defaultItem);

        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
        amountBtn.setPadding(new Insets(2, 5, 2, 10));

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

                PriceAmount priceAmount = Utils.getAmountByString(getText(), m_currentCurrency.get());

                m_currentAmount.set(priceAmount);
            }
        };
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");

        amountField.setPadding(new Insets(3, 10, 3, 0));

        Button enterButton = new Button("[ ENTER ]");
        enterButton.setFont(App.txtFont);
        enterButton.setId("toolBtn");

        /*amountField.textProperty().addListener((obs, oldVal, newVal) -> {

          
        });*/
        amountField.setOnKeyPressed(e -> {
            // e.consume(); 
            KeyCode keyCode = e.getCode();
            // char    charTyped = e.getCharacter().charAt(0);

            if (keyCode == KeyCode.ENTER) {
                // Object currency = m_currencyMenuBtn.getUserData();

                PriceAmount priceAmount = Utils.getAmountByString(amountField.getText(), m_currentCurrency.get());
                m_currentAmount.set(priceAmount);

            } else {
                if (!getChildren().contains(enterButton)) {
                    getChildren().add(enterButton);
                }
            }

        });
        amountField.focusedProperty().addListener((obs, old, newPropertyValue) -> {
            if (!newPropertyValue) {
                PriceAmount priceAmount = Utils.getAmountByString(amountField.getText(), m_currentCurrency.get());
                m_currentAmount.set(priceAmount);
            }
        });

        HBox.setHgrow(amountField, Priority.ALWAYS);

        amountBtn.setOnAction(actionEvent -> {
            getChildren().remove(amountBtn);
            getChildren().add(amountField);
            Platform.runLater(() -> amountField.requestFocus());
        });

        m_currentCurrency.addListener((obs, old, newValue) -> {

            m_currencyMenuBtn.setGraphic(IconButton.getIconView(newValue.getUnitImage(), IMAGE_WIDTH));
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

        });

        getChildren().addAll(m_currencyMenuBtn, amountBtn);

        amountBtn.prefWidthProperty().bind(this.widthProperty().subtract(m_currencyMenuBtn.widthProperty()));

    }

    public Image getUnitImage() {
        return new Image("/assets/unitErgo.png");
    }

    public PriceAmount getCurrentAmount() {
        return m_currentAmount.get();
    }

    public SimpleObjectProperty<PriceAmount> currentAmountProperty() {
        return m_currentAmount;
    }

}
