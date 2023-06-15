package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.InputBoxesSelectionException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.Parameters;
import org.ergoplatform.appkit.SignedTransaction;
import org.ergoplatform.appkit.UnsignedTransaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

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

        //wallet.transact(networkType, id, null)
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

    public JsonObject getErgoClientObject(String nodeId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("subject", "GET_CLIENT");
        jsonObject.addProperty("networkType", m_networkType.toString());
        jsonObject.addProperty("nodeId", nodeId);
        return jsonObject;
    }

    public String transact(ErgoClient ergoClient, SignedTransaction signedTx) {
        return ergoClient.execute(ctx -> {
            String quoted = ctx.sendTransaction(signedTx);
            return quoted.substring(1, quoted.length() - 1);
        });
    }

    public boolean sendErg(long nanoErg, String receipientAddress, Address senderAddress, long fee, String nodeId, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {
        if (receipientAddress != null & senderAddress != null && nodeId != null && fee >= Parameters.MinFee) {
            NoteInterface nodeInterface = m_walletData.getNodeInterface();
            if (nodeInterface != null) {
                return nodeInterface.sendNote(getErgoClientObject(nodeId), (successEvent) -> {
                    WorkerStateEvent workerEvent = successEvent;
                    Object sourceObject = workerEvent.getSource().getValue();
                    if (sourceObject != null) {
                        ErgoClient ergoClient = (ErgoClient) sourceObject;
                        String txId = null;

                        JsonObject txInfoJson = new JsonObject();
                        txInfoJson.addProperty("fee", fee);
                        txInfoJson.addProperty("nanoErg", nanoErg);
                        txInfoJson.addProperty("receipientAddress", receipientAddress);
                        txInfoJson.addProperty("returnAddress", senderAddress.toString());
                        txInfoJson.addProperty("nodeId", nodeId);
                        try {

                            UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(ergoClient,
                                    m_wallet.addressStream(m_networkType).toList(),
                                    Address.create(receipientAddress), nanoErg, fee, senderAddress);

                            txId = transact(ergoClient, ergoClient.execute(ctx -> {
                                try {
                                    return m_wallet.key().sign(ctx, unsignedTx, m_wallet.myAddresses.keySet());
                                } catch (WalletKey.Failure ex) {

                                    txInfoJson.addProperty("unauthorized", ex.toString());
                                    return null;
                                }
                            }));

                            // if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
                        } catch (InputBoxesSelectionException ibsEx) {
                            txInfoJson.addProperty("insufficientFunds", ibsEx.toString());
                        }
                        if (txId != null) {
                            txInfoJson.addProperty("txId", txId);
                        }

                        Utils.returnObject(txInfoJson, onSuccess, null);
                    }
                }, onFailed);
            }
        }
        return false;
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
