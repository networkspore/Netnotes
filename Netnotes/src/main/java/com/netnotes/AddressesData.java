package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

public class AddressesData {

    private File logFile;
    private NetworkType m_networkType;
    private VBox m_addressBox;
    private Wallet m_wallet;
    private WalletData m_walletData;

    private SimpleDoubleProperty m_totalQuote = new SimpleDoubleProperty(0);

    ArrayList<AddressData> m_addressDataList = new ArrayList<AddressData>();

    public AddressesData(String id, Wallet wallet, WalletData walletData, NetworkType networkType) {
        logFile = new File("addressesData-" + walletData.getNetworkId() + ".txt");
        m_wallet = wallet;
        m_walletData = walletData;
        m_networkType = networkType;

        m_wallet.myAddresses.forEach((index, name) -> {
            AddressData addressData = null;
            try {
                Address address = wallet.publicAddress(m_networkType, index);
                addressData = new AddressData(name, index, address, m_networkType, walletData);

            } catch (Failure e) {

            }
            if (addressData != null) {
                m_addressDataList.add(addressData);
                addressData.lastUpdated.addListener((a, b, c) -> {
                    double total = calculateCurrentTotal();
                    try {
                        Files.writeString(logFile.toPath(), c + total, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    m_totalQuote.set(total);
                });
            }

        });
        m_addressBox = new VBox();
        updateAddressBox();
    }

    public SimpleDoubleProperty getTotalDoubleProperty() {
        return m_totalQuote;
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        return false;
    }

    public void addAddress() {
        String addressName = App.showGetTextInput("Address name", "Address", App.branchImg);
        if (addressName != null) {
            int nextAddressIndex = m_wallet.nextAddressIndex();
            m_wallet.myAddresses.put(nextAddressIndex, addressName);
            AddressData addressData = null;
            try {

                Address address = m_wallet.publicAddress(m_networkType, nextAddressIndex);
                addressData = new AddressData(addressName, nextAddressIndex, address, m_networkType, m_walletData);

            } catch (Failure e1) {

                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                a.show();
            }
            if (addressData != null) {
                m_addressDataList.add(addressData);
                addressData.lastUpdated.addListener((a, b, c) -> {
                    try {
                        Files.writeString(logFile.toPath(), c, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    m_totalQuote.set(calculateCurrentTotal());
                });
                updateAddressBox();
            }
        }
    }

    public VBox getAddressBox() {

        updateAddressBox();
        //    lastUpdated.addListener(e -> {
        //      updateNetworksGrid();
        //   });
        return m_addressBox;
    }

    private void updateAddressBox() {
        m_addressBox.getChildren().clear();
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            addressData.prefWidthProperty().bind(m_addressBox.widthProperty());

            m_addressBox.getChildren().add(addressData);
        }
    }

    public void updateBalance() {
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);

            addressData.updateBalance();
        }
    }

    public void setQuote(PriceQuote quote) {

        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            addressData.setQuote(quote);

        }

    }

    public double calculateCurrentTotal() {
        double total = 0;
        for (int i = 0; i < m_addressDataList.size(); i++) {
            AddressData addressData = m_addressDataList.get(i);
            total += addressData.getTotalAmountPrice();

        }

        return total;
    }

    /*
    @Override
    public boolean sendNoteToFullNetworkId(JsonObject note, String fullNetworkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int indexOfNetworkID = fullNetworkId.indexOf(getNetworkId());

        int indexOfperiod = fullNetworkId.indexOf(".", indexOfNetworkID);

        if (indexOfperiod == -1) {
            return false;
        } else {
            int indexOfSecondPeriod = fullNetworkId.indexOf(".", indexOfperiod + 1);
            String tunnelId;

            if (indexOfSecondPeriod == -1) {
                tunnelId = fullNetworkId.substring(indexOfperiod);
            } else {
                tunnelId = fullNetworkId.substring(indexOfperiod, indexOfSecondPeriod);
            }

            for (int i = 0; i < m_addressDataList.size(); i++) {
                AddressData addressData = m_addressDataList.get(i);
                if (tunnelId.equals(addressData.getNetworkId())) {
                    return addressData.sendNoteToFullNetworkId(note, fullNetworkId, onSucceeded, onFailed);

                }
            }
        }

        return false;
    } */
}
