package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.SecretString;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;
import com.satergo.Wallet;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WalletsDataList {

    private File logFile = new File("walletsDataBox-log.txt");
    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private String m_selectedId;
    private VBox m_gridBox;
    //  private double m_width = 400;
    // private String m_direction = "column";

    private ErgoWallet m_ergoWallet;
    private File m_dataFile;
    private SimpleDoubleProperty m_gridWidth;
    private SimpleStringProperty m_iconStyle;
    private double m_stageWidth = 600;
    private double m_stageHeight = 450;

    public WalletsDataList(double width, String iconStyle, File dataFile, File walletsDirectory, ErgoWallet ergoWallet) {
        m_gridWidth = new SimpleDoubleProperty(width);
        m_iconStyle = new SimpleStringProperty(iconStyle);
        m_gridBox = new VBox();

        m_ergoWallet = ergoWallet;
        m_dataFile = dataFile;
        readFile(m_dataFile);

        m_iconStyle.addListener((obs, oldval, newval) -> updateGrid());
    }

    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidth;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_iconStyle;
    }

    public void readFile(File dataFile) {
        if (dataFile != null && dataFile.isFile()) {
            try {

                openJson(Utils.readJsonFile(m_ergoWallet.getNetworksData().appKeyProperty().get(), dataFile.toPath()));

            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {

            }
        }
    }

    public void openJson(JsonObject json) {
        if (json != null) {
            JsonElement stageElement = json.get("stage");
            JsonElement walletsElement = json.get("wallets");

            if (stageElement != null && stageElement.isJsonObject()) {
                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement widthElement = stageObject.get("width");
                JsonElement heightElement = stageObject.get("height");

                if (widthElement != null && widthElement.isJsonPrimitive()) {
                    m_stageWidth = widthElement.getAsDouble();
                }
                m_stageHeight = heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : m_stageHeight;
            }

            if (walletsElement != null && walletsElement.isJsonArray()) {
                JsonArray jsonArray = walletsElement.getAsJsonArray();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonElement jsonElement = jsonArray.get(i);

                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();

                        JsonElement nameElement = jsonObject.get("name");
                        JsonElement idElement = jsonObject.get("id");
                        JsonElement fileLocationElement = jsonObject.get("file");
                        JsonElement windowSizeElement = jsonObject.get("windowSize");
                        JsonElement networkTypeElement = jsonObject.get("networkType");
                        JsonElement nodesIdElement = jsonObject.get("nodesId");
                        JsonElement selectedNodeIdElement = jsonObject.get("selectedNodeId");
                        JsonElement explorerIdElement = jsonObject.get("explorerId");
                        JsonElement explorerUpdatesElement = jsonObject.get("explorerUpdates");
                        JsonElement marketUpdatesElement = jsonObject.get("marketUpdates");

                        if (nameElement != null && idElement != null && fileLocationElement != null) {
                            String id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
                            String name = nameElement == null ? "Wallet " + id : nameElement.getAsString();
                            File walletFile = fileLocationElement == null ? null : new File(fileLocationElement.getAsString());
                            NetworkType walletNetworkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;

                            JsonObject windowSize = windowSizeElement != null && windowSizeElement.isJsonObject() ? windowSizeElement.getAsJsonObject() : null;
                            JsonElement windowWidth = windowSize != null ? windowSize.get("width") : null;
                            JsonElement windowHeight = windowSize != null ? windowSize.get("height") : null;

                            double sceneWidth = windowSize != null && windowWidth != null && windowWidth.isJsonPrimitive() ? windowWidth.getAsDouble() : 400;
                            double sceneHeight = windowSize != null && windowHeight != null && windowHeight.isJsonPrimitive() ? windowHeight.getAsDouble() : 700;

                            String nodesId = nodesIdElement == null ? null : nodesIdElement.getAsString();
                            String explorerId = explorerIdElement == null ? null : explorerIdElement.getAsString();
                            String explorerUpdates = explorerUpdatesElement == null ? null : explorerUpdatesElement.getAsString();

                            String selectedNodeId = selectedNodeIdElement == null ? null : selectedNodeIdElement.getAsString();

                            JsonObject marketUpdatesJson = marketUpdatesElement != null && marketUpdatesElement.isJsonObject() ? marketUpdatesElement.getAsJsonObject() : null;

                            MarketUpdates marketUpdates = new MarketUpdates(m_ergoWallet, marketUpdatesJson);

                            WalletData walletData = new WalletData(id, name, walletFile, sceneWidth, sceneHeight, nodesId, selectedNodeId, explorerId, explorerUpdates, marketUpdates, walletNetworkType, m_ergoWallet);
                            m_noteInterfaceList.add(walletData);

                            walletData.addUpdateListener((obs, oldValue, newValue) -> save());

                        }
                    }
                }

            }
        }
    }

    public File getWalletsDirectory() {
        return m_ergoWallet.getWalletsDirectory();
    }

    public void add(WalletData walletData) {
        m_noteInterfaceList.add(walletData);
        walletData.addUpdateListener((obs, oldval, newVal) -> save());
        updateGrid();

    }

    public void remove(String id) {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(id)) {
                noteInterface.removeUpdateListener();
                m_noteInterfaceList.remove(noteInterface);
                break;
            }
        }

        updateGrid();

    }

    public void showAddWalletStage() {

        Image icon = m_ergoWallet.getIcon();
        String name = m_ergoWallet.getName();

        VBox layoutBox = new VBox();

        Stage stage = new Stage();
        stage.getIcons().add(ErgoWallet.getSmallAppIcon());
        stage.setResizable(false);
        stage.initStyle(StageStyle.UNDECORATED);

        Scene walletScene = new Scene(layoutBox, m_stageWidth, m_stageHeight);

        Button maximizeBtn = new Button();

        String heading = "New";
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, stage);
        String titleString = heading + " - " + name;
        stage.setTitle(titleString);

        Text headingText = new Text(heading);
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        /*HBox menuBar = new HBox();
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 5, 1, 5)); */
        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 2, 2, 2));
        //  VBox menuBarBox = new VBox(menuBar);
        //   menuBarBox.setId("bodyBox");
        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(2, 5, 0, 5));

        Text walletName = new Text(String.format("%-15s", "Name"));
        walletName.setFill(App.txtColor);
        walletName.setFont(App.txtFont);

        String friendlyId = FriendlyId.createFriendlyId();

        TextField walletNameField = new TextField("Wallet " + friendlyId);
        walletNameField.setFont(App.txtFont);
        walletNameField.setId("formField");
        HBox.setHgrow(walletNameField, Priority.ALWAYS);

        HBox walletNameBox = new HBox(walletName, walletNameField);
        walletNameBox.setAlignment(Pos.CENTER_LEFT);

        Text networkTxt = new Text(String.format("%-15s", "Node"));
        networkTxt.setFill(App.txtColor);
        networkTxt.setFont(App.txtFont);

        MenuButton nodesMenuBtn = new MenuButton(ErgoNodes.NAME);
        nodesMenuBtn.setPrefWidth(150);
        nodesMenuBtn.setFont(App.txtFont);
        nodesMenuBtn.setTextFill(App.altColor);
        nodesMenuBtn.setUserData(ErgoNodes.NETWORK_ID);
        nodesMenuBtn.setPrefWidth(200);

        MenuItem networkNoneItem = new MenuItem("(none)");
        MenuItem networkErgoItem = new MenuItem(ErgoNodes.NAME);
        nodesMenuBtn.getItems().add(networkNoneItem);
        nodesMenuBtn.getItems().add(networkErgoItem);

        MenuButton nodesMenuBtn2 = new MenuButton("(default)");
        nodesMenuBtn2.setPadding(new Insets(4, 5, 0, 5));
        nodesMenuBtn2.setPrefWidth(150);
        nodesMenuBtn2.setFont(Font.font("OCR A Extended", 12));
        nodesMenuBtn2.setTextFill(App.altColor);
        nodesMenuBtn2.setUserData(null);

        networkNoneItem.setOnAction(e -> {
            nodesMenuBtn.setText(networkNoneItem.getText());
            nodesMenuBtn.setUserData(null);
            nodesMenuBtn2.setVisible(false);
        });
        networkErgoItem.setOnAction(e -> {
            nodesMenuBtn.setText(networkErgoItem.getText());
            nodesMenuBtn.setUserData(ErgoNodes.NETWORK_ID);
            nodesMenuBtn2.setVisible(true);
        });

        MenuItem nodeDefaultItem = new MenuItem("default");
        nodeDefaultItem.setOnAction(e -> {
            nodesMenuBtn.setText(networkNoneItem.getText());
            nodesMenuBtn.setUserData(null);
        });

        nodesMenuBtn2.getItems().add(nodeDefaultItem);

        Text networkTypeTxt = new Text(String.format("%-15s", "Network type"));
        networkTypeTxt.setFill(App.txtColor);
        networkTypeTxt.setFont(App.txtFont);

        MenuButton walletTypeMenuBtn = new MenuButton(NetworkType.MAINNET.toString());
        walletTypeMenuBtn.setFont(App.txtFont);
        walletTypeMenuBtn.setTextFill(App.altColor);
        walletTypeMenuBtn.setUserData(NetworkType.MAINNET);
        walletTypeMenuBtn.setPrefWidth(200);

        MenuItem testnetItem = new MenuItem(NetworkType.TESTNET.toString());
        testnetItem.setOnAction(e -> {
            walletTypeMenuBtn.setText(testnetItem.getText());
            walletTypeMenuBtn.setUserData(NetworkType.TESTNET);
        });
        MenuItem mainnetItem = new MenuItem(NetworkType.MAINNET.toString());
        mainnetItem.setOnAction(e -> {
            walletTypeMenuBtn.setText(mainnetItem.getText());
            walletTypeMenuBtn.setUserData(NetworkType.MAINNET);
        });
        walletTypeMenuBtn.getItems().add(mainnetItem);
        walletTypeMenuBtn.getItems().add(testnetItem);

        HBox networkSelectBox = new HBox(networkTxt, nodesMenuBtn, nodesMenuBtn2);
        networkSelectBox.setAlignment(Pos.CENTER_LEFT);

        HBox networkTypeBox = new HBox(networkTypeTxt, walletTypeMenuBtn);
        networkTypeBox.setAlignment(Pos.CENTER_LEFT);

        Text explorerText = new Text(String.format("%-15s", "Explorer"));
        explorerText.setFill(App.txtColor);
        explorerText.setFont(App.txtFont);

        MenuButton explorersBtn = new MenuButton(ErgoExplorer.NAME);
        explorersBtn.setFont(App.txtFont);
        explorersBtn.setTextFill(App.altColor);
        explorersBtn.setUserData(ErgoExplorer.NETWORK_ID);
        explorersBtn.setPrefWidth(200);

        MenuItem explorerNoneItem = new MenuItem("(none)");
        explorerNoneItem.setOnAction(e -> {
            explorersBtn.setText(explorerNoneItem.getText());
            explorersBtn.setUserData(null);
        });

        MenuItem ergoExplorerItem = new MenuItem(ErgoExplorer.NAME);
        ergoExplorerItem.setOnAction(e -> {
            explorersBtn.setText(ergoExplorerItem.getText());
            explorersBtn.setUserData(ErgoExplorer.NETWORK_ID);
        });

        explorersBtn.getItems().addAll(explorerNoneItem, ergoExplorerItem);

        MenuButton explorerUpdatesBtn = new MenuButton("15s");
        explorerUpdatesBtn.setPadding(new Insets(4, 5, 0, 5));
        explorerUpdatesBtn.setFont(Font.font("OCR A Extended", 12));
        explorerUpdatesBtn.setUserData("15");

        MenuItem explorerUpdates5secItem = new MenuItem("5s");
        explorerUpdates5secItem.setOnAction(e -> {
            explorerUpdatesBtn.setText(explorerUpdates5secItem.getText());
            explorerUpdatesBtn.setUserData("5");
        });

        MenuItem explorerUpdates15secItem = new MenuItem("15s");
        explorerUpdates15secItem.setOnAction(e -> {
            explorerUpdatesBtn.setText(explorerUpdates15secItem.getText());
            explorerUpdatesBtn.setUserData("15");
        });

        MenuItem explorerUpdates30secItem = new MenuItem("30s");
        explorerUpdates30secItem.setOnAction(e -> {
            explorerUpdatesBtn.setText(explorerUpdates30secItem.getText());
            explorerUpdatesBtn.setUserData("30");
        });

        MenuItem explorerUpdates1minItem = new MenuItem("1 min");
        explorerUpdates1minItem.setOnAction(e -> {
            explorerUpdatesBtn.setText(explorerUpdates1minItem.getText());
            explorerUpdatesBtn.setUserData("60");
        });

        HBox explorerBox = new HBox(explorerText, explorersBtn, explorerUpdatesBtn);
        explorerBox.setAlignment(Pos.CENTER_LEFT);

        Text marketTxt = new Text(String.format("%-15s", "Price"));
        marketTxt.setFill(App.txtColor);
        marketTxt.setFont(App.txtFont);

        MenuButton marketBtn = new MenuButton(KucoinExchange.NAME);
        marketBtn.setFont(App.txtFont);
        marketBtn.setUserData(KucoinExchange.NETWORK_ID);
        marketBtn.setPrefWidth(200);

        MenuItem marketNoneItem = new MenuItem("(disabled)");

        MenuItem marketKuCoinItem = new MenuItem(KucoinExchange.NAME);

        marketBtn.getItems().addAll(marketNoneItem, marketKuCoinItem);

        MenuButton marketUpdatesBtn = new MenuButton("Real-time: Ticker");

        marketUpdatesBtn.setPadding(new Insets(4, 5, 0, 5));
        marketUpdatesBtn.setFont(Font.font("OCR A Extended", 12));
        marketUpdatesBtn.setUserData("realtime:ticker");

        MenuItem updatesDisabledItem = new MenuItem("(disabled)");
        updatesDisabledItem.setOnAction(e -> {
            marketUpdatesBtn.setText(updatesDisabledItem.getText());
            marketUpdatesBtn.setUserData(null);
        });

        MenuItem updatesRealTimeItem = new MenuItem("Real-time: Ticker");
        updatesRealTimeItem.setOnAction(e -> {
            marketUpdatesBtn.setText("Real-time");
            marketUpdatesBtn.setUserData("realtime:ticker");
        });

        MenuItem updates5secItem = new MenuItem("5s");
        updates5secItem.setOnAction(e -> {
            marketUpdatesBtn.setText(updates5secItem.getText());
            marketUpdatesBtn.setUserData("5");
        });

        MenuItem updates15secItem = new MenuItem("15s");
        updates15secItem.setOnAction(e -> {
            marketUpdatesBtn.setText(updates15secItem.getText());
            marketUpdatesBtn.setUserData("15");
        });

        MenuItem updates30secItem = new MenuItem("30s");
        updates30secItem.setOnAction(e -> {
            marketUpdatesBtn.setText(updates30secItem.getText());
            marketUpdatesBtn.setUserData("30");
        });

        MenuItem updates1minItem = new MenuItem("1 min");
        updates1minItem.setOnAction(e -> {
            marketUpdatesBtn.setText(updates1minItem.getText());
            marketUpdatesBtn.setUserData("60");
        });

        marketNoneItem.setOnAction(e -> {
            marketBtn.setText(marketNoneItem.getText());
            marketBtn.setUserData(null);

        });

        marketUpdatesBtn.getItems().addAll(updatesDisabledItem, updates5secItem, updates15secItem, updates30secItem, updates1minItem);

        marketKuCoinItem.setOnAction(e -> {
            marketBtn.setText(marketKuCoinItem.getText());
            marketBtn.setUserData(KucoinExchange.NETWORK_ID);

        });

        HBox marketBox = new HBox(marketTxt, marketBtn, marketUpdatesBtn);
        marketBox.setAlignment(Pos.CENTER_LEFT);

        Text walletTxt = new Text(String.format("%-15s", ""));
        walletTxt.setFont(App.txtFont);
        walletTxt.setFill(App.txtColor);

        Button newWalletBtn = new Button("Create");
        newWalletBtn.setId("menuBarBtn");
        newWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        newWalletBtn.setFont(App.txtFont);

        Rectangle windowBounds = m_ergoWallet.getNetworksData().getMaximumWindowBounds();

        newWalletBtn.setOnAction(newWalletEvent -> {
            String nodeId = nodesMenuBtn.getUserData() == null ? null : (String) nodesMenuBtn.getUserData();
            String selectedNode = nodeId == null ? null : nodesMenuBtn2.getText();
            String explorerId = explorersBtn.getUserData() == null ? null : (String) explorersBtn.getUserData();
            String explorerUpdates = explorerUpdatesBtn.getUserData() == null ? null : (String) explorerUpdatesBtn.getUserData();
            String marketId = marketBtn.getUserData() == null ? null : (String) marketBtn.getUserData();

            String marketUpdates = marketUpdatesBtn.getUserData() == null ? null : (String) marketUpdatesBtn.getUserData();

            NetworkType networkType = (NetworkType) walletTypeMenuBtn.getUserData();

            Scene mnemonicScene = createMnemonicScene(friendlyId, walletNameField.getText(), nodeId, selectedNode, explorerId, explorerUpdates, marketId, marketUpdates, networkType, stage, () -> {
                stage.setScene(walletScene);
                stage.setTitle(titleString);
            });
            stage.setScene(mnemonicScene);
            ResizeHelper.addResizeListener(stage, 500, 425, windowBounds.getWidth(), windowBounds.getHeight());
        });

        Button existingWalletBtn = new Button("Open");
        existingWalletBtn.setId("menuBarBtn");
        existingWalletBtn.setPadding(new Insets(2, 10, 2, 10));

        existingWalletBtn.setFont(App.txtFont);

        existingWalletBtn.setOnAction(clickEvent -> {
            FileChooser openFileChooser = new FileChooser();
            openFileChooser.setInitialDirectory(getWalletsDirectory());
            openFileChooser.setTitle("Open: Wallet file");
            openFileChooser.getExtensionFilters().add(ErgoWallet.ergExt);
            openFileChooser.setSelectedExtensionFilter(ErgoWallet.ergExt);

            File walletFile = openFileChooser.showOpenDialog(stage);

            if (walletFile != null) {

                NetworkType networkType = (NetworkType) walletTypeMenuBtn.getUserData();

                String nodeId = nodesMenuBtn.getUserData() == null ? null : (String) nodesMenuBtn.getUserData();
                String selectedNode = nodeId == null ? null : nodesMenuBtn2.getText();
                String explorerId = explorersBtn.getUserData() == null ? null : (String) explorersBtn.getUserData();
                String explorerUpdates = explorerUpdatesBtn.getUserData() == null ? null : (String) explorerUpdatesBtn.getUserData();
                String marketId = marketBtn.getUserData() == null ? null : (String) marketBtn.getUserData();
                String marketUpdateType = marketUpdatesBtn.getUserData() == null ? null : (String) marketUpdatesBtn.getUserData();

                WalletData walletData = new WalletData(friendlyId, walletNameField.getText(), walletFile, 400, 700, nodeId, selectedNode, explorerId, explorerUpdates, new MarketUpdates(m_ergoWallet, marketId, marketUpdateType), networkType, m_ergoWallet);

                add(walletData);
                save();
                stage.close();

            }
        });

        Button restoreWalletBtn = new Button("Restore");
        restoreWalletBtn.setId("menuBarBtn");
        restoreWalletBtn.setPadding(new Insets(2, 5, 2, 5));
        restoreWalletBtn.setFont(App.txtFont);

        restoreWalletBtn.setOnAction(clickEvent -> {
            String seedPhrase = restoreMnemonicStage();
            if (!seedPhrase.equals("")) {
                App.createPassword(m_ergoWallet.getName() + " - Restore wallet: Password", m_ergoWallet.getIcon(), ErgoWallet.getAppIcon(), stage, onSuccess -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {

                        String passwordString = (String) sourceObject;
                        if (!passwordString.equals("")) {
                            Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(passwordString));

                            FileChooser saveFileChooser = new FileChooser();
                            saveFileChooser.setInitialDirectory(getWalletsDirectory());
                            saveFileChooser.setTitle("Save: Wallet file");
                            saveFileChooser.getExtensionFilters().add(ErgoWallet.ergExt);
                            saveFileChooser.setSelectedExtensionFilter(ErgoWallet.ergExt);

                            File walletFile = saveFileChooser.showSaveDialog(stage);

                            if (walletFile != null) {

                                try {
                                    NetworkType networkType = (NetworkType) walletTypeMenuBtn.getUserData();

                                    Wallet.create(walletFile.toPath(), mnemonic, seedPhrase, passwordString.toCharArray());

                                    String nodeId = nodesMenuBtn.getUserData() == null ? null : (String) nodesMenuBtn.getUserData();
                                    String selectedNode = nodeId == null ? null : nodesMenuBtn2.getText();
                                    String explorerId = explorersBtn.getUserData() == null ? null : (String) explorersBtn.getUserData();
                                    String explorerUpdates = explorerUpdatesBtn.getUserData() == null ? null : (String) explorerUpdatesBtn.getUserData();
                                    String marketId = marketBtn.getUserData() == null ? null : (String) marketBtn.getUserData();
                                    String marketUpdates = marketUpdatesBtn.getUserData() == null ? null : (String) marketUpdatesBtn.getUserData();

                                    WalletData walletData = new WalletData(friendlyId, walletNameField.getText(), walletFile, 400, 700, nodeId, selectedNode, explorerId, explorerUpdates, new MarketUpdates(m_ergoWallet, marketId, marketUpdates), networkType, m_ergoWallet);
                                    add(walletData);
                                    save();

                                    walletData.open(passwordString);
                                } catch (Exception e1) {
                                    Alert a = new Alert(AlertType.NONE, "Wallet creation: Cannot be saved.\n\n" + e1.toString(), ButtonType.OK);
                                    a.initOwner(stage);
                                    a.show();
                                }
                            }

                        }
                        stage.setScene(walletScene);
                        closeBtn.fire();
                    }
                });

            }

        });

        HBox newWalletBox = new HBox(newWalletBtn, existingWalletBtn, restoreWalletBtn);
        newWalletBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(newWalletBox, Priority.ALWAYS);

        HBox walletBox = new HBox(walletTxt);
        walletBox.setAlignment(Pos.CENTER_LEFT);

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

        walletNameBox.minHeightProperty().bind(rowHeight);
        networkSelectBox.minHeightProperty().bind(rowHeight);
        networkTypeBox.minHeightProperty().bind(rowHeight);
        explorerBox.minHeightProperty().bind(rowHeight);
        marketBox.minHeightProperty().bind(rowHeight);
        walletBox.minHeightProperty().bind(rowHeight);
        newWalletBox.minHeightProperty().bind(rowHeight);

        newWalletBtn.prefHeightProperty().bind(rowHeight);
        restoreWalletBtn.prefHeightProperty().bind(rowHeight);
        existingWalletBtn.prefHeightProperty().bind(rowHeight);

        newWalletBtn.prefWidthProperty().bind(walletScene.widthProperty().subtract(20).divide(3));

        existingWalletBtn.prefWidthProperty().bind(walletScene.widthProperty().subtract(20).divide(3));

        restoreWalletBtn.prefWidthProperty().bind(walletScene.widthProperty().subtract(30).divide(3));

        VBox mainBodyBox = new VBox(walletNameBox, networkTypeBox, networkSelectBox, explorerBox, marketBox, walletBox);
        HBox.setHgrow(mainBodyBox, Priority.ALWAYS);
        Region leftP = new Region();
        leftP.setPrefWidth(40);

        HBox bodyBox = new HBox(leftP, mainBodyBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(0, 5, 0, 20));

        VBox bodyPaddBox = new VBox(bodyBox);
        bodyPaddBox.setPadding(new Insets(0, 5, 10, 5));

        VBox footerBox = new VBox(newWalletBox);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setPadding(new Insets(5, 20, 0, 20));

        layoutBox.getChildren().addAll(titleBox, headerBox, bodyPaddBox, footerBox);
        walletScene.getStylesheets().add("/css/startWindow.css");
        stage.setScene(walletScene);
        Rectangle maxSize = m_ergoWallet.getNetworksData().getMaximumWindowBounds();

        ResizeHelper.addResizeListener(stage, 380, 420, maxSize.getWidth(), maxSize.getHeight());

        ChangeListener<Number> walletWidthListener = (obs, oldval, newVal) -> {
            m_stageWidth = newVal.doubleValue();
        };

        ChangeListener<Number> walletHeightListener = (obs, oldval, newVal) -> {
            m_stageHeight = newVal.doubleValue();
            // double height = m_stageHeight - titleBox.heightProperty().get() - headerBox.heightProperty().get();
            // rowHeight.set((height-5) / 6);
        };

        rowHeight.bind(stage.heightProperty().subtract(titleBox.heightProperty()).subtract(headerBox.heightProperty()).subtract(35).divide(7));
        walletScene.widthProperty().addListener(walletWidthListener);

        walletScene.heightProperty().addListener(walletHeightListener);
        closeBtn.setOnAction(e -> {
            stage.close();
        });
        stage.show();

    }

    public Scene createMnemonicScene(String id, String name, String nodeId, String selectedNode, String explorerId, String explorerUpdates, String marketId, String marketUpdates, NetworkType networkType, Stage stage, Runnable onBack) {
        //String oldStageName = mnemonicStage.getTitle();

        String titleStr = "Mnemonic phrase - " + m_ergoWallet.getName();

        stage.setTitle(titleStr);

        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), maximizeBtn, closeBtn, stage);

        //Region spacer = new Region();
        //HBox.setHgrow(spacer, Priority.ALWAYS);
        BufferedButton backBtn = new BufferedButton("/assets/return-back-up-30.png", 15);

        HBox menuBar = new HBox(backBtn);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        VBox menuPaddingBox = new VBox(menuBar);
        menuPaddingBox.setPadding(new Insets(0, 2, 5, 2));

        Text headingText = new Text("Mnemonic phrase");
        headingText.setFill(App.txtColor);
        headingText.setFont(App.txtFont);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(40);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 15, 10, 15));
        headingBox.setId("headingBox");

        VBox headerBox = new VBox(headingBox);
        headerBox.setPadding(new Insets(0, 5, 2, 5));

        TextArea mnemonicField = new TextArea(Mnemonic.generateEnglishMnemonic());
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("textFieldCenter");
        mnemonicField.setEditable(false);
        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicFieldBox = new HBox(mnemonicField);

        VBox mnemonicBox = new VBox(mnemonicFieldBox);
        mnemonicBox.setAlignment(Pos.CENTER);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Next");

        nextBtn.setFont(App.txtFont);

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, menuPaddingBox, headerBox, bodyBox);

        mnemonicFieldBox.setMaxWidth(900);
        HBox.setHgrow(mnemonicFieldBox, Priority.ALWAYS);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);
        mnemonicScene.getStylesheets().add("/css/startWindow.css");

        mnemonicBox.prefHeightProperty().bind(mnemonicScene.heightProperty().subtract(titleBox.heightProperty()).subtract(headerBox.heightProperty()).subtract(130));

        closeBtn.setOnAction(e -> {
            mnemonicField.setText("");
            stage.close();

        });

        backBtn.setOnAction(e -> {
            mnemonicField.setText("");
            onBack.run();
        });

        nextBtn.setOnAction(nxtEvent -> {
            Alert nextAlert = new Alert(AlertType.NONE, "This mnemonic phrase may be used to generate copies of this wallet, or to recover this wallet if it is lost. It is strongly recommended to always maintain a paper copy of this phrase in a secure location. \n\nWarning: Loss of your mnemonic phrase could lead to the loss of your ability to recover this wallet.", ButtonType.CANCEL, ButtonType.OK);
            nextAlert.setHeaderText("Notice");
            nextAlert.initOwner(stage);
            nextAlert.setTitle("Notice - Mnemonic phrase - Add wallet");
            Optional<ButtonType> result = nextAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                App.createPassword("Wallet password - " + ErgoWallet.NAME, ErgoWallet.getSmallAppIcon(), ErgoWallet.getAppIcon(), stage, onSuccess -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {

                        String password = (String) sourceObject;
                        if (!password.equals("")) {

                            FileChooser saveFileChooser = new FileChooser();
                            saveFileChooser.setInitialDirectory(getWalletsDirectory());
                            saveFileChooser.setTitle("Save: Wallet file");
                            saveFileChooser.getExtensionFilters().add(ErgoWallet.ergExt);
                            saveFileChooser.setSelectedExtensionFilter(ErgoWallet.ergExt);

                            File walletFile = saveFileChooser.showSaveDialog(stage);

                            if (walletFile != null) {

                                Wallet.create(walletFile.toPath(), Mnemonic.create(SecretString.create(mnemonicField.getText()), SecretString.create(password)), walletFile.getName(), password.toCharArray());
                                mnemonicField.setText("-");

                                WalletData walletData = new WalletData(id, name, walletFile, 400, 700, nodeId, selectedNode, explorerId, explorerUpdates, new MarketUpdates(m_ergoWallet, marketId, marketUpdates), networkType, m_ergoWallet);
                                add(walletData);
                                save();
                                walletData.open(password);

                            }

                        }

                    }
                    stage.setScene(mnemonicScene);
                    closeBtn.fire();
                });

            }
        });

        return mnemonicScene;
    }

    public String restoreMnemonicStage() {
        String titleStr = m_ergoWallet.getName() + " - Restore wallet: Mnemonic phrase";

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(m_ergoWallet.getIcon());

        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(m_ergoWallet.getIcon(), titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(m_ergoWallet.getIcon(), "Restore wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase:");
        subTitleTxt.setFill(App.txtColor);
        subTitleTxt.setFont(App.txtFont);

        HBox subTitleBox = new HBox(subTitleTxt);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);

        TextArea mnemonicField = new TextArea();
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("formField");

        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicBox = new HBox(mnemonicField);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Words left: 15");
        nextBtn.setId("toolBtn");
        nextBtn.setFont(App.txtFont);
        nextBtn.setDisable(true);
        nextBtn.setOnAction(nxtEvent -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");

            List<String> mnemonicList = Arrays.asList(words);
            try {
                Mnemonic.checkEnglishMnemonic(mnemonicList);
                mnemonicStage.close();
            } catch (MnemonicValidationException e) {
                Alert a = new Alert(AlertType.NONE, "Error: Mnemonic invalid\n\nPlease correct the mnemonic phrase and try again.", ButtonType.CLOSE);
                a.initOwner(mnemonicStage);
                a.setTitle("Error: Mnemonic invalid.");
            }

        });

        mnemonicField.setOnKeyPressed(e1 -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");
            int numWords = words.length;
            if (numWords == 15) {
                nextBtn.setText("Ok");

                List<String> mnemonicList = Arrays.asList(words);
                try {
                    Mnemonic.checkEnglishMnemonic(mnemonicList);
                    nextBtn.setDisable(false);

                } catch (MnemonicValidationException e) {
                    nextBtn.setText("Invalid");
                    nextBtn.setId("toolBtn");
                    nextBtn.setDisable(true);
                }

            } else {
                if (nextBtn.getText().equals("")) {
                    nextBtn.setText("Words left: 15");
                } else {
                    nextBtn.setText("Words left: " + (15 - numWords));
                }

                nextBtn.setId("toolBtn");
                nextBtn.setDisable(true);
            }

        });

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(subTitleBox, mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);

        mnemonicScene.getStylesheets().add("/css/startWindow.css");
        mnemonicStage.setScene(mnemonicScene);

        closeBtn.setOnAction(e -> {

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();

    }

    public void setSelected(String networkId) {

        if (m_selectedId != null) {
            NoteInterface prevInterface = getNoteInterface(m_selectedId);
            if (prevInterface != null) {
                prevInterface.getButton().setCurrent(false);
            }
        }
        if (networkId != null) {
            NoteInterface currentInterface = getNoteInterface(networkId);
            if (currentInterface != null) {
                currentInterface.getButton().setCurrent(true);
            }
        }
    }

    public String getSelected() {
        return m_selectedId;
    }

    public NoteInterface getNoteInterface(String networkId) {

        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(networkId)) {
                return noteInterface;
            }
        }
        return null;
    }

    public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        m_noteInterfaceList.forEach(noteInterface -> {
            if (noteInterface.getNetworkId().equals(networkId)) {

                noteInterface.sendNote(note, onSucceeded, onFailed);
            }
        });
    }

    public int size() {
        return m_noteInterfaceList.size();
    }

    public JsonArray getJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        return jsonArray;
    }

    public VBox getButtonGrid() {
        updateGrid();
        return m_gridBox;
    }

    public void updateGrid() {

        int numCells = m_noteInterfaceList.size();
        String currentIconStyle = m_iconStyle.get();
        m_gridBox.getChildren().clear();

        if (currentIconStyle.equals(IconStyle.ROW)) {
            for (int i = 0; i < numCells; i++) {
                NoteInterface network = m_noteInterfaceList.get(i);
                IconButton iconButton = network.getButton(currentIconStyle);
                iconButton.prefWidthProperty().bind(m_gridWidth);
                m_gridBox.getChildren().add(iconButton);
            }
        } else {

            double width = m_gridWidth.get();
            double imageWidth = 75;
            double cellPadding = 15;
            double cellWidth = imageWidth + (cellPadding * 2);

            int floor = (int) Math.floor(width / cellWidth);
            int numCol = floor == 0 ? 1 : floor;
            // currentNumCols.set(numCol);
            int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            HBox[] rowsBoxes = new HBox[numRows];
            for (int i = 0; i < numRows; i++) {
                rowsBoxes[i] = new HBox();
                m_gridBox.getChildren().add(rowsBoxes[i]);
            }

            ItemIterator grid = new ItemIterator();

            for (NoteInterface noteInterface : m_noteInterfaceList) {

                HBox rowBox = rowsBoxes[grid.getJ()];
                rowBox.getChildren().add(noteInterface.getButton(IconStyle.ICON));

                if (grid.getI() < numCol) {
                    grid.setI(grid.getI() + 1);
                } else {
                    grid.setI(0);
                    grid.setJ(grid.getJ() + 1);
                }
            }

        }

    }

    public JsonArray getWalletsJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }
        return jsonArray;
    }

    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("width", m_stageWidth);
        json.addProperty("height", m_stageHeight);
        return json;
    }

    public void save() {
        JsonObject fileObject = new JsonObject();
        fileObject.add("stage", getStageJson());
        fileObject.add("wallets", getWalletsJsonArray());

        String jsonString = fileObject.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {

            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iV = new byte[12];
            secureRandom.nextBytes(iV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, m_ergoWallet.getNetworksData().appKeyProperty().get(), parameterSpec);

            byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

            try {

                if (m_dataFile.isFile()) {
                    Files.delete(m_dataFile.toPath());
                }

                FileOutputStream outputStream = new FileOutputStream(m_dataFile);
                FileChannel fc = outputStream.getChannel();

                ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

                fc.write(byteBuffer);

                int written = 0;
                int bufferLength = 1024 * 8;

                while (written < encryptedData.length) {

                    if (written + bufferLength > encryptedData.length) {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
                    } else {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
                    }

                    written += fc.write(byteBuffer);
                }

                outputStream.close();

            } catch (IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nIO exception:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(), "\nKey error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }
    }

}
