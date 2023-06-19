package com.netnotes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.ergo.ErgoInterface;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoNetwork extends Network implements NoteInterface {

    public final static String NAME = "Ergo Network";
    public final static String DESCRIPTION = "Ergo Network allows you to connect and configure access to am Ergo Node.";
    public final static String SUMMARY = "The Ergo Node is part of Ergo's peer-to-peer network which hosts and synchronises a copy of the entire blockchain, transactions are submitted to the Node via Wallets in order to be processed.";

    public final static ErgoCurrency ERG = new ErgoCurrency();

    public final static int MAINNET_PORT = 9053;
    public final static int TESTNET_PORT = 9052;
    public final static int EXTERNAL_PORT = 9030;

    private String m_explorerId = null;

    private String m_mainnetNodeId = null;

    private ArrayList<ErgoNodeData> m_nodesDataList = new ArrayList<>();

    public ErgoNetwork(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_NETWORK, networksData);

    }

    public ErgoNetwork(JsonObject ergoNetworkJson, NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_NETWORK, networksData);

        if (ergoNetworkJson != null) {
            JsonElement nodesElement = ergoNetworkJson.get("nodes");
            JsonElement mainnetNodeIdElement = ergoNetworkJson.get("mainnetNodeId");

            m_mainnetNodeId = mainnetNodeIdElement == null ? null : mainnetNodeIdElement.getAsString();

            if (nodesElement != null && nodesElement.isJsonArray()) {
                JsonArray nodesArray = nodesElement.getAsJsonArray();

                for (JsonElement clientElement : nodesArray) {
                    if (clientElement.isJsonObject()) {
                        m_nodesDataList.add(new ErgoNodeData(clientElement.getAsJsonObject(), this));
                    }
                }
            }
        }

    }

    public static Image getAppIcon() {
        return App.ergoLogo;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergo-network-30.png");
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");
        JsonElement networkTypeElement = note.get("networkType");
        JsonElement nodeIdElement = note.get("nodeId");
        if (subjecElement != null) {
            String subject = subjecElement.getAsString();
            switch (subject) {
                case "GET_CLIENT":

                    String nodeId = nodeIdElement == null ? null : nodeIdElement.getAsString();
                    String networkType = networkTypeElement == null ? null : networkTypeElement.toString();

                    if (nodeId == null) {
                        if (m_mainnetNodeId == null) {
                            return false;
                        } else {
                            ErgoNodeData nodeData = getErgoNodeData(m_mainnetNodeId);
                            if (nodeData != null) {
                                if (networkType != null) {
                                    if (nodeData.getNetworkTypeString().equals(networkType)) {
                                        return nodeData.getClient(onSucceeded, onFailed);
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return nodeData.getClient(onSucceeded, onFailed);
                                }
                            } else {
                                return false;
                            }

                        }
                    } else {
                        ErgoNodeData nodeData = getErgoNodeData(nodeId);
                        if (nodeData != null) {
                            if (networkType == null) {
                                return nodeData.getClient(onSucceeded, onFailed);
                            } else {
                                if (nodeData.getNetworkTypeString().equals(networkType)) {
                                    return nodeData.getClient(onSucceeded, onFailed);
                                } else {
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }

                    }

                // NetworkType.MAINNET.toString().equals(networkType);
            }
        }

        return false;
    }

    private ErgoNodeData getErgoNodeData(String networkId) {
        if (networkId == null) {
            return null;
        }
        for (ErgoNodeData ergoNodeData : m_nodesDataList) {
            if (ergoNodeData.getNetworkId().equals(m_mainnetNodeId)) {
                return ergoNodeData;
            }
        }
        return null;
    }

    @Override
    public JsonObject getJsonObject() {

        JsonObject networkObj = super.getJsonObject();

        return networkObj;

    }

    public NoteInterface getExplorerInterface() {
        if (m_explorerId != null) {
            return getNetworksData().getNoteInterface(m_explorerId);
        } else {
            return null;
        }
    }
    /*
    public void showNetworkStage(Network network) {

        Stage networkStage = new Stage();
        networkStage.setTitle("Net Notes - Network");
        networkStage.getIcons().add(App.ergoLogo);
        networkStage.setResizable(false);
        networkStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {

            networkStage.close();
        });

        HBox titleBox = App.createTopBar(getIcon(), "Network", closeBtn, networkStage);

        Button imageButton = App.createImageButton(App.globeImg, "Network");
        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);

        Text networkTypeTxt = new Text("> Name:  Ergo");
        networkTypeTxt.setFill(App.txtColor);
        networkTypeTxt.setFont(App.txtFont);

        HBox networkTypeBox = new HBox(networkTypeTxt);
        networkTypeBox.setPadding(new Insets(3, 0, 5, 0));
        HBox.setHgrow(networkTypeBox, Priority.ALWAYS);

        Text applicationTxt = new Text("> Application:");
        applicationTxt.setFill(App.txtColor);
        applicationTxt.setFont(App.txtFont);

        Text addressTxt = new Text("URL:");
        addressTxt.setFont(App.txtFont);
        addressTxt.setFill(App.altColor);
        addressTxt.setId("textField");

        ImageView arrowRightImage = App.highlightedImageView(App.arrowRightImg);
        arrowRightImage.setFitHeight(15);
        arrowRightImage.setPreserveRatio(true);

        Button addressBtn = new Button();
        addressBtn.setGraphic(arrowRightImage);
        addressBtn.setPadding(new Insets(2, 15, 2, 15));
        addressBtn.setFont(App.txtFont);
        addressBtn.setVisible(false);

        TextField addressField = new TextField("Enter address or click manage...");
        addressField.setId("formField");
        HBox.setHgrow(addressField, Priority.ALWAYS);
        addressField.setOnKeyPressed(key -> {
            KeyCode keyCode = key.getCode();

            if (keyCode == KeyCode.ENTER) {
                String addressFieldText = addressField.getText();

                try {
                    setUrl(addressFieldText);
                    String currentHost = getHost();
                    if (currentHost == null) {

                        addressField.setText("Enter address or click manage...");
                    } else {
                        int currentPort = getPort();

                        addressField.setText(currentHost + ":" + currentPort);

                    }
                    addressBtn.setVisible(false);

                } catch (MalformedURLException e) {

                    setHost(null);

                    addressBtn.setVisible(false);
                    addressField.setText("Enter address or click manage...");

                }
            }
        });

        addressField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                String addressFieldText = addressField.getText();

                if (newPropertyValue) {
                    if (addressFieldText.equals("Enter address or click manage...")) {
                        addressField.setText("");
                    }
                    addressBtn.setVisible(true);
                } else {

                    try {
                        setUrl(addressFieldText);
                        String currentHost = getHost();
                        if (currentHost == null) {

                            addressField.setText("Enter address or click manage...");
                        } else {
                            int currentPort = getPort();

                            addressField.setText(currentHost + ":" + currentPort);

                        }
                        addressBtn.setVisible(false);

                    } catch (MalformedURLException e) {

                        setHost(null);

                        addressBtn.setVisible(false);
                        addressField.setText("Enter address or click manage...");

                    }

                }
            }
        });

        HBox appLocationBox = new HBox(addressTxt, addressField, addressBtn);
        appLocationBox.setPadding(new Insets(5, 0, 5, 20));
        appLocationBox.setAlignment(Pos.CENTER_LEFT);

        Button applicationBtn = new Button("Manage"); //127.0.0.1:9503
        applicationBtn.setGraphic(App.highlightedImageView(new Image("/assets/server-outline-white-20.png")));
        applicationBtn.setFont(App.txtFont);
        applicationBtn.setPadding(new Insets(2, 10, 2, 10));

        HBox manageBox = new HBox(applicationBtn);
        manageBox.setAlignment(Pos.CENTER_LEFT);
        manageBox.setPadding(new Insets(10, 0, 10, 20));

        HBox applicationBox = new HBox(applicationTxt);
        applicationBox.setAlignment(Pos.CENTER_LEFT);
        applicationBox.setPadding(new Insets(3, 0, 0, 0));

        Text walletTxt = new Text("> Wallet:");
        walletTxt.setFill(App.txtColor);
        walletTxt.setFont(App.txtFont);

        Region hBar = null;
        HBox gBox = null;
        HBox addBox = null;

        hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));
        HBox.setHgrow(gBox, Priority.ALWAYS);

        Button addBtn = new Button("Add");
        addBtn.setPadding(new Insets(2, 10, 2, 10));
        addBtn.setFont(App.txtFont);
        addBtn.setOnAction(openEvent -> {

        });

        addBox = new HBox(addBtn);
        addBox.setAlignment(Pos.CENTER);
        addBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(imageBox, networkTypeBox, applicationBox, appLocationBox, manageBox, gBox, addBox);

        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        VBox networkVBox = new VBox(titleBox, bodyBox);
        HBox.setHgrow(networkVBox, Priority.ALWAYS);

        Scene networkScene = new Scene(networkVBox, 400, 525);
        networkScene.getStylesheets().add("/css/startWindow.css");
        networkStage.setScene(networkScene);

        networkStage.show();

    }*/
}
