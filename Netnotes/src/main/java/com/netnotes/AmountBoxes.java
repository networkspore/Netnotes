package com.netnotes;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import javafx.scene.layout.VBox;

public class AmountBoxes extends HBox {

    public final static int IMAGE_WIDTH = 40;

    private ArrayList<AmountItem> m_amountsList = new ArrayList<AmountItem>();

    private VBox amountVBox = new VBox();

    private AddressData m_addressData = null;

    public AmountBoxes(AddressData addressData, Node... children) {
        super(children);
        m_addressData = addressData;
        HBox.setHgrow(this, Priority.ALWAYS);

        updateAmountBoxes();

        getChildren().add(amountVBox);
    }

    public void updateAmountBoxes() {
        amountVBox.getChildren().clear();

        AmountItem ergoAmountItem = new AmountItem(new ErgoAmount(0));

        amountVBox.getChildren().add(ergoAmountItem);

        ArrayList<TokenData> tokenList = m_addressData.getConfirmedTokenList();

        for (TokenData tokenData : tokenList) {
            // double tokenQuantityt = tokenData.getAmount() / (10^tokenData.getDecimals());
            String tokenId = tokenData.getTokenID();
            String name = tokenData.getName();
            // AmountItem amountItem = new AmountItem(new PriceAmount(0, new PriceCurrency(tokenId, name, name, tokenData.getDecimals(), m_addressData.getNetworkNetworkId())));
            //  amountVBox.getChildren().add(amountItem);
        }

    }

    public ArrayList<AmountItem> AountsList() {
        return m_amountsList;
    }

    public void setAddressData(AddressData addressData) {
        m_addressData = addressData;
    }

}
