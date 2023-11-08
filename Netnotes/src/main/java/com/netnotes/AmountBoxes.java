package com.netnotes;

import java.util.ArrayList;

import javafx.scene.Node;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountBoxes extends HBox {

    public final static int IMAGE_WIDTH = 40;

    private ArrayList<AmountBox> m_amountsList = new ArrayList<AmountBox>();

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

      //  AmountBox ergoAmountItem = new AmountBox(new ErgoAmount(0));

      //  amountVBox.getChildren().add(ergoAmountItem);

        ArrayList<TokenData> tokenList = m_addressData.getConfirmedTokenList();

        for (TokenData tokenData : tokenList) {

            String tokenId = tokenData.getTokenID();
            String name = tokenData.getName();

        }

    }

    public ArrayList<AmountBox> AountsList() {
        return m_amountsList;
    }

    public void setAddressData(AddressData addressData) {
        m_addressData = addressData;
    }

}
