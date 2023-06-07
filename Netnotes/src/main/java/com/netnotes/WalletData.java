package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonObject;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;

import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WalletData extends Network implements NoteInterface {

    private File logFile;
    private File m_walletFile = null;

    private NetworkType m_networkType = null;
    private Stage m_walletStage = null;
    private Stage m_passwordStage = null;

    private NoteInterface m_nodeInterface = null;
    private NoteInterface m_explorerInterface = null;
    private NoteInterface m_exchangeInterface = null;

    public WalletData(String id, String name, File walletFile, String nodeId, String explorerId, String exchangeId, NetworkType networkType, NoteInterface noteInterface) {
        super(null, name, id, noteInterface);
        logFile = new File("WalletData" + name + "-log.txt");

        try {
            Files.writeString(logFile.toPath(), "ExchangeId: " + exchangeId + " nodeId: " + nodeId + " explorerId: " + explorerId + " networkType: " + networkType);
        } catch (IOException e) {

        }
        m_walletFile = walletFile;
        m_networkType = networkType;

        m_nodeInterface = nodeId == null ? null : getNetworksData().getNoteInterface(nodeId);
        m_explorerInterface = explorerId == null ? null : getNetworksData().getNoteInterface(explorerId);
        m_exchangeInterface = exchangeId == null ? null : getNetworksData().getNoteInterface(exchangeId);

        setIconStyle(IconStyle.ROW);
    }

    @Override
    public void open() {
        try {
            Files.writeString(logFile.toPath(), "\nwalletsData opening.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        openWallet();
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {
        return false;
    }

    private void showWalletStage(Wallet wallet) {
        try {
            Files.writeString(logFile.toPath(), "\nshowing wallet stage", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        String title = getName() + " - (" + m_networkType.toString() + ")";

        double width = 450;

        m_walletStage = new Stage();
        m_walletStage.setTitle(title);
        m_walletStage.getIcons().add(ErgoWallet.getAppIcon());
        m_walletStage.setResizable(false);
        m_walletStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {

            m_walletStage.close();
            m_walletStage = null;
        });

        HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), title, closeBtn, m_walletStage);

        /*Button imageButton = createImageButton(walletImg240, title + "\n" + wallet.name.get());
        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);*/
        ImageView addImage = App.highlightedImageView(App.addImg);
        addImage.setFitHeight(10);
        addImage.setPreserveRatio(true);

        Tooltip addTip = new Tooltip("Add address");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        Button addButton = new Button();
        addButton.setGraphic(App.highlightedImageView(new Image("/assets/git-branch-outline-white-30.png")));
        addButton.setId("menuBarBtn");
        addButton.setPadding(new Insets(2, 6, 2, 6));
        addButton.setTooltip(addTip);

        Tooltip explorerUrlTip = new Tooltip("Explorer url");
        explorerUrlTip.setShowDelay(new javafx.util.Duration(100));
        explorerUrlTip.setFont(App.txtFont);

        Button explorerBtn = new Button();
        explorerBtn.setGraphic(App.highlightedImageView(new Image("/assets/search-outline-white-30.png")));
        explorerBtn.setId("menuBarBtn");
        explorerBtn.setPadding(new Insets(2, 6, 2, 6));
        explorerBtn.setTooltip(explorerUrlTip);
        explorerBtn.setDisable(true);

        TextField explorerURLField = new TextField();
        explorerURLField.setId("urlField");
        //  explorerURLField.setText();
        explorerURLField.setEditable(false);
        explorerURLField.setPrefWidth(350);
        explorerURLField.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox menuBar = new HBox(addButton, spacer, explorerBtn, explorerURLField);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(5, 5, 5, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        VBox layoutBox = new VBox();
        Font smallerFont = Font.font("OCR A Extended", 10);

        Text updatedTxt = new Text("Updated:");
        updatedTxt.setFill(App.altColor);
        updatedTxt.setFont(smallerFont);

        TextField lastUpdatedField = new TextField();
        lastUpdatedField.setFont(smallerFont);
        lastUpdatedField.setId("formField");

        HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
        updateBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacerRegion = new Region();
        VBox.setVgrow(spacerRegion, Priority.ALWAYS);

        VBox bodyVBox = new VBox(titleBox, paddingBox, layoutBox, spacerRegion, updateBox);

        ArrayList<AddressData> addressDataList = getWalletAddressDataList(wallet);

        Scene openWalletScene = new Scene(bodyVBox, width, 525);
        openWalletScene.getStylesheets().add("/css/startWindow.css");
        m_walletStage.setScene(openWalletScene);
        m_walletStage.show();

        addButton.setOnAction(e -> {
            String addressName = App.showGetTextInput("Address name", "Address name", App.branchImg);
            if (addressName != null) {
                int nextAddressIndex = wallet.nextAddressIndex();
                wallet.myAddresses.put(nextAddressIndex, addressName);
                try {

                    Address address = wallet.publicAddress(m_networkType, nextAddressIndex);
                    AddressData addressData = new AddressData(addressName, nextAddressIndex, address, m_networkType, this);
                    Button newButton = addressData.getButton();

                    addressDataList.add(addressData);
                    layoutBox.getChildren().add(newButton);
                } catch (Failure e1) {

                    Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                    a.show();
                }

            }
        });

        addressDataList.forEach(addressData -> {

            Button rowBtn = addressData.getButton();

            HBox rowBox = new HBox(rowBtn);
            HBox.setHgrow(rowBox, Priority.ALWAYS);
            layoutBox.getChildren().add(rowBox);
            HBox.setHgrow(layoutBox, Priority.ALWAYS);
        });

    }

    public ArrayList<AddressData> getWalletAddressDataList(Wallet wallet) {

        // ErgoClient ergoClient = RestApiErgoClient.create(nodeApiAddress, networkType, "", networkType == NetworkType.MAINNET ? defaultMainnetExplorerUrl : defaultTestnetExplorerUrl);
        ArrayList<AddressData> addressList = new ArrayList<>();
        wallet.myAddresses.forEach((index, name) -> {

            try {
                Address address = wallet.publicAddress(m_networkType, index);
                addressList.add(new AddressData(name, index, address, m_networkType, this));
            } catch (Failure e) {

            }

        });

        return addressList;
    }

    private void updateAddressBtn(double width, Button rowBtn, AddressData addressData) {

        //   BufferedImage imageBuffer = addressData.getBufferedImage();
        double remainingSpace = width;// - imageBuffer.getWidth();

        String addressMinimal = addressData.getAddressMinimal((int) (remainingSpace / 24));
        /*
        ImageView btnImageView = new ImageView();
        if (imageBuffer != null) {
            btnImageView.setImage(SwingFXUtils.toFXImage(imageBuffer, null));
        }*/
        String text = "> " + addressData.getName() + ": \n  " + addressMinimal;
        Tooltip addressTip = new Tooltip(addressData.getName());

        //  rowBtn.setGraphic(btnImageView);
        rowBtn.setText(text);
        rowBtn.setTooltip(addressTip);

    }

    public void openWallet() {

        try {
            Files.writeString(logFile.toPath(), "\nConfirming wallet password.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        if (m_passwordStage == null) {
            m_passwordStage = new Stage();

            m_passwordStage.setResizable(false);
            m_passwordStage.initStyle(StageStyle.UNDECORATED);
            m_passwordStage.setTitle("Wallet file: Enter password");

            Button closeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), getName() + "- Enter password", closeBtn, m_passwordStage);
            closeBtn.setOnAction(event -> {
                m_passwordStage.close();

            });
            Button imageButton = App.createImageButton(App.lockDocumentImg, "Wallet File");

            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);

            Text passwordTxt = new Text("> Enter password:");
            passwordTxt.setFill(App.txtColor);
            passwordTxt.setFont(App.txtFont);

            PasswordField passwordField = new PasswordField();
            passwordField.setFont(App.txtFont);
            passwordField.setId("passField");
            HBox.setHgrow(passwordField, Priority.ALWAYS);

            Platform.runLater(() -> passwordField.requestFocus());

            HBox passwordBox = new HBox(passwordTxt, passwordField);
            passwordBox.setAlignment(Pos.CENTER_LEFT);

            Button clickRegion = new Button();
            clickRegion.setPrefWidth(Double.MAX_VALUE);
            clickRegion.setId("transparentColor");
            clickRegion.setPrefHeight(500);

            clickRegion.setOnAction(e -> {
                passwordField.requestFocus();

            });

            VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

            VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene passwordScene = new Scene(layoutVBox, 600, 425);

            passwordScene.getStylesheets().add("/css/startWindow.css");
            m_passwordStage.setScene(passwordScene);

            passwordField.setOnKeyPressed(e -> {

                KeyCode keyCode = e.getCode();

                if (keyCode == KeyCode.ENTER) {

                    try {

                        Wallet wallet = Wallet.load(m_walletFile.toPath(), passwordField.getText());
                        showWalletStage(wallet);

                        m_passwordStage.close();
                        m_passwordStage = null;
                    } catch (Exception e1) {

                        passwordField.setText("");
                        try {
                            Files.writeString(logFile.toPath(), e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e2) {

                        }
                    }

                }
            });

            m_passwordStage.show();
        } else {
            m_passwordStage.show();
        }

    }

}
