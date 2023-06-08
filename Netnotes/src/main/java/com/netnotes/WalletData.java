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
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
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
    private String m_name;

    private NoteInterface m_nodeInterface = null;
    private NoteInterface m_explorerInterface = null;
    private NoteInterface m_marketInterface = null;

    public WalletData(String id, String name, File walletFile, String nodeId, String explorerId, String exchangeId, NetworkType networkType, NoteInterface noteInterface) {
        super(null, name, id, noteInterface);
        m_name = name;
        logFile = new File("WalletData" + name + "-log.txt");

        try {
            Files.writeString(logFile.toPath(), "ExchangeId: " + exchangeId + " nodeId: " + nodeId + " explorerId: " + explorerId + " networkType: " + networkType);
        } catch (IOException e) {

        }
        m_walletFile = walletFile;
        m_networkType = networkType;

        m_nodeInterface = nodeId == null ? null : getNetworksData().getNoteInterface(nodeId);
        m_explorerInterface = explorerId == null ? null : getNetworksData().getNoteInterface(explorerId);
        m_marketInterface = exchangeId == null ? null : getNetworksData().getNoteInterface(exchangeId);

        setIconStyle(IconStyle.ROW);
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("id", getNetworkId());
        jsonObject.addProperty("file", m_walletFile.getAbsolutePath());
        jsonObject.addProperty("networkType", m_networkType.toString());

        if (m_nodeInterface != null) {
            jsonObject.addProperty("nodeId", m_nodeInterface.getNetworkId());
        }
        if (m_explorerInterface != null) {
            jsonObject.addProperty("explorerId", m_explorerInterface.getNetworkId());
        }
        if (m_marketInterface != null) {
            jsonObject.addProperty("exchangeId", m_marketInterface.getNetworkId());
        }

        /*jsonObject.set("name");
        jsonObject.get("id");
        jsonObject.get("file");
        jsonObject.get("networkType");
        jsonObject.get("nodeId");
        jsonObject.get("explorerId");
        jsonObject.get("exchangeId");*/
        return jsonObject;
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

            HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), getName() + " - Enter password", closeBtn, m_passwordStage);
            closeBtn.setOnAction(event -> {
                m_passwordStage.close();

            });
            Button imageButton = App.createImageButton(ErgoWallet.getAppIcon(), "Wallet");
            imageButton.setGraphicTextGap(10);
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
            clickRegion.setPrefHeight(40);

            clickRegion.setOnAction(e -> {
                passwordField.requestFocus();

            });

            VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

            VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene passwordScene = new Scene(layoutVBox, 600, 375);

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

    private void showWalletStage(Wallet wallet) {
        try {
            Files.writeString(logFile.toPath(), "\nshowing wallet stage", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        String title = m_name + " - (" + m_networkType.toString() + ")";

        double width = 450;
        double imageWidth = 25;

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
        addButton.setGraphic(IconButton.getIconView(new Image("/assets/git-branch-outline-white-30.png"), 30));
        addButton.setId("menuBtn");
        addButton.setTooltip(addTip);

        Tooltip networkTip = new Tooltip(m_explorerInterface == null ? "Select network: (none)" : "Select network: " + m_nodeInterface.getName());
        networkTip.setShowDelay(new javafx.util.Duration(100));
        networkTip.setFont(App.txtFont);

        ImageView nodeView = IconButton.getIconView(new Image("/assets/node-30.png"), imageWidth);

        MenuButton networkMenuBtn = new MenuButton();
        networkMenuBtn.setGraphic(m_nodeInterface == null ? nodeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_nodeInterface.getNetworkId(), true).getIcon(), imageWidth));
        networkMenuBtn.setPadding(new Insets(0, 0, 0, 15));
        networkMenuBtn.setTooltip(networkTip);

        MenuItem nodeNullMenuItem = new MenuItem("(none)");
        nodeNullMenuItem.setOnAction(e -> {
            removeNodeInterface();
            networkMenuBtn.setGraphic(nodeView);
            networkTip.setText("Select network: (none)");

        });

        MenuItem nodeMenuItem = new MenuItem("Ergo Network");
        nodeMenuItem.setOnAction(e -> {
            setNodeInterface(NetworkID.ERGO_NETWORK);
            if (m_nodeInterface != null) {
                networkTip.setText("Select network: " + m_nodeInterface.getName());
                networkMenuBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getNetworksData(), m_nodeInterface.getNetworkId(), true).getIcon(), imageWidth));
            } else {
                networkTip.setText("Select network: (none)");
                networkMenuBtn.setGraphic(nodeView);
            }
        });

        networkMenuBtn.getItems().addAll(nodeNullMenuItem, nodeMenuItem);

        Tooltip explorerUrlTip = new Tooltip(m_explorerInterface == null ? "Select explorer: (none)" : "Select explorer: " + m_explorerInterface.getName());
        explorerUrlTip.setShowDelay(new javafx.util.Duration(100));
        explorerUrlTip.setFont(App.txtFont);

        ImageView searchView = IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), imageWidth);

        MenuButton explorerBtn = new MenuButton();
        explorerBtn.setGraphic(m_explorerInterface == null ? searchView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_explorerInterface.getNetworkId(), true).getIcon(), imageWidth));

        explorerBtn.setTooltip(explorerUrlTip);

        MenuItem explorerNullMenuItem = new MenuItem("(none)");
        explorerNullMenuItem.setOnAction(e -> {
            removeExplorerInterface();
            explorerUrlTip.setText("Select explorer: (none)");
            explorerBtn.setGraphic(searchView);
        });

        MenuItem ergoExplorerMenuItem = new MenuItem("Ergo Explorer");
        ergoExplorerMenuItem.setOnAction(e -> {
            setExplorerInterface(NetworkID.ERGO_EXPLORER);
            if (m_explorerInterface != null) {
                explorerUrlTip.setText("Select explorer: " + m_explorerInterface.getName());
                explorerBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getNetworksData(), m_explorerInterface.getNetworkId(), true).getIcon(), imageWidth));
            } else {
                explorerUrlTip.setText("Select explorer: (none)");
                explorerBtn.setGraphic(searchView);
            }
        });

        explorerBtn.getItems().addAll(explorerNullMenuItem, ergoExplorerMenuItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Tooltip marketTip = new Tooltip((m_marketInterface == null ? "Select market: (none)" : "Select market: " + m_marketInterface.getName()));
        marketTip.setShowDelay(new javafx.util.Duration(100));
        marketTip.setFont(App.txtFont);

        ImageView exchangeView = IconButton.getIconView(new Image("/assets/exchange-30.png"), imageWidth);

        MenuButton marketBtn = new MenuButton();
        marketBtn.setGraphic(m_marketInterface == null ? exchangeView : IconButton.getIconView(new InstallableIcon(getNetworksData(), m_marketInterface.getNetworkId(), true).getIcon(), imageWidth));
        marketBtn.setTooltip(marketTip);

        MenuItem marketNullMenuItem = new MenuItem("(none)");
        marketNullMenuItem.setOnAction(e -> {
            removeMarketInterface();
            marketTip.setText("Select market: (none)");
            marketBtn.setGraphic(exchangeView);
        });

        MenuItem kucoinMenuItem = new MenuItem("KuCoin Exchange");
        kucoinMenuItem.setOnAction(e -> {
            setMarketInterface(NetworkID.KUKOIN_EXCHANGE);
            if (m_marketInterface != null) {
                marketTip.setText("Select market: " + m_marketInterface.getName());
                marketBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getNetworksData(), m_marketInterface.getNetworkId(), true).getIcon(), imageWidth));

            } else {
                marketTip.setText("Select market: (none)");
                marketBtn.setGraphic(exchangeView);
            }
        });

        marketBtn.getItems().addAll(marketNullMenuItem, kucoinMenuItem);

        HBox rightSideMenu = new HBox(networkMenuBtn, explorerBtn, marketBtn);
        rightSideMenu.setId("rightSideMenuBar");

        HBox menuBar = new HBox(addButton, spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(5, 0, 5, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        VBox layoutBox = new VBox();
        layoutBox.setPadding(SMALL_INSETS);
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

        ScrollPane scrollPane = new ScrollPane(layoutBox);

        VBox bodyVBox = new VBox(titleBox, paddingBox, scrollPane, updateBox);

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

        HBox.setHgrow(layoutBox, Priority.ALWAYS);

        scrollPane.prefViewportWidthProperty().bind(openWalletScene.widthProperty());
        scrollPane.prefViewportHeightProperty().bind(openWalletScene.heightProperty().subtract(titleBox.heightProperty().get()).subtract(menuBar.heightProperty().get()).subtract(updateBox.heightProperty().get()));

        addressDataList.forEach(addressData -> {

            IconButton rowBtn = addressData.getButton();
            rowBtn.prefWidthProperty().bind(openWalletScene.widthProperty().subtract(15));
            // HBox rowBox = new HBox(rowBtn);
            //  HBox.setHgrow(rowBox, Priority.ALWAYS);
            layoutBox.getChildren().add(rowBtn);

        });

    }

    public NoteInterface getNodeInterface() {
        return m_nodeInterface;
    }

    public NoteInterface getExplorerInterface() {
        return m_explorerInterface;
    }

    public NoteInterface getMarketInterface() {
        return m_marketInterface;
    }

    private void setNodeInterface(String networkId) {
        m_nodeInterface = getNetworksData().getNoteInterface(networkId);
    }

    private void setExplorerInterface(String networkId) {
        m_explorerInterface = getNetworksData().getNoteInterface(networkId);
    }

    private void setMarketInterface(String networkId) {
        m_marketInterface = getNetworksData().getNoteInterface(networkId);
    }

    private void removeNodeInterface() {
        m_nodeInterface = null;
    }

    private void removeExplorerInterface() {
        m_explorerInterface = null;
    }

    private void removeMarketInterface() {
        m_marketInterface = null;
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

}
