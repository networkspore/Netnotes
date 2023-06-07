package com.netnotes;

import java.io.File;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.SecretString;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.Network.NetworkID;
import com.satergo.Wallet;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WalletsDataList {

    private File logFile = new File("walletsDataBox-log.txt");
    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private String m_selectedId;
    private VBox m_buttonGrid = null;
    private double m_width = 400;
    private String m_direction = "column";
    private Stage m_addWalletStage = null;
    private ErgoWallet m_ergoWallet;
    private File m_walletsDirectory = new File(System.getProperty("user.dir") + "/wallets");

    public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());

    public WalletsDataList(JsonArray jsonArray, ErgoWallet ergoWallet) {

        m_ergoWallet = ergoWallet;

        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            JsonElement nameElement = jsonObject.get("name");
            JsonElement idElement = jsonObject.get("id");
            JsonElement fileLocationElement = jsonObject.get("walletFile");
            JsonElement networkTypeElement = jsonObject.get("networkType");
            JsonElement nodeIdElement = jsonObject.get("nodeId");
            JsonElement explorerIdElement = jsonObject.get("explorerId");
            JsonElement exchangeIdElement = jsonObject.get("exchangeId");

            if (nameElement != null && idElement != null && fileLocationElement != null) {
                String id = idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString();
                String name = nameElement == null ? "Wallet #" + id : nameElement.getAsString();
                File walletFile = fileLocationElement == null ? null : new File(fileLocationElement.getAsString());
                NetworkType walletNetworkType = networkTypeElement == null ? NetworkType.MAINNET : NetworkType.fromValue(networkTypeElement.getAsString());
                String nodeId = nodeIdElement == null ? null : nodeIdElement.getAsString();
                String explorerId = explorerIdElement == null ? null : explorerIdElement.getAsString();
                String exchangeId = exchangeIdElement == null ? null : exchangeIdElement.getAsString();

                m_noteInterfaceList.add(new WalletData(id, name, walletFile, nodeId, explorerId, exchangeId, walletNetworkType, m_ergoWallet));

            }
        }

    }

    public void addOpen(WalletData walletData) {
        m_noteInterfaceList.add(walletData);
        updateGrid();
        lastUpdated.set(LocalDateTime.now());
        walletData.open();

    }

    public void remove(String id) {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(id)) {
                m_noteInterfaceList.remove(noteInterface);
                break;
            }
        }
        updateGrid();
        lastUpdated.set(LocalDateTime.now());
    }

    public void showAddWalletStage() {
        if (m_addWalletStage == null) {

            Image icon = m_ergoWallet.getIcon();
            String name = m_ergoWallet.getName();

            m_addWalletStage = new Stage();
            m_addWalletStage.getIcons().add(icon);
            m_addWalletStage.setResizable(false);
            m_addWalletStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_addWalletStage.close();
                m_addWalletStage = null;
            });

            HBox titleBox = App.createTopBar(icon, name + " - Add wallet", closeBtn, m_addWalletStage);

            Button imageButton = App.createImageButton(icon, name);

            HBox imageBox = new HBox(imageButton);
            imageBox.setPadding(new Insets(0, 0, 15, 0));
            HBox.setHgrow(imageBox, Priority.ALWAYS);
            imageBox.setAlignment(Pos.CENTER);

            Text walletName = new Text("> Wallet name:");
            walletName.setFill(App.txtColor);
            walletName.setFont(App.txtFont);

            String friendlyId = FriendlyId.createFriendlyId();

            TextField walletNameField = new TextField("Wallet #" + friendlyId);
            walletNameField.setFont(App.txtFont);
            walletNameField.setId("formField");
            HBox.setHgrow(walletNameField, Priority.ALWAYS);

            HBox walletNameBox = new HBox(walletName, walletNameField);
            walletNameBox.setAlignment(Pos.CENTER_LEFT);

            Text networkTxt = new Text("> Network:");
            networkTxt.setFill(App.txtColor);
            networkTxt.setFont(App.txtFont);

            MenuButton networkMenuBtn = new MenuButton(ErgoNetwork.NAME);
            networkMenuBtn.setPrefWidth(150);
            networkMenuBtn.setFont(App.txtFont);
            networkMenuBtn.setTextFill(App.altColor);
            networkMenuBtn.setUserData(NetworkID.ERGO_NETWORK);

            MenuItem networkNoneItem = new MenuItem("(none)");
            networkNoneItem.setOnAction(e -> {
                networkMenuBtn.setText(networkNoneItem.getText());
                networkMenuBtn.setUserData(null);
            });
            MenuItem networkErgoItem = new MenuItem(ErgoNetwork.NAME);
            networkErgoItem.setOnAction(e -> {
                networkMenuBtn.setText(networkErgoItem.getText());
                networkMenuBtn.setUserData(NetworkID.ERGO_NETWORK);
            });
            networkMenuBtn.getItems().add(networkNoneItem);
            networkMenuBtn.getItems().add(networkErgoItem);

            Text networkTypeTxt = new Text("|    Network type:");
            networkTypeTxt.setFill(App.txtColor);
            networkTypeTxt.setFont(App.txtFont);

            MenuButton walletTypeMenuBtn = new MenuButton(NetworkType.MAINNET.toString());
            walletTypeMenuBtn.setFont(App.txtFont);
            walletTypeMenuBtn.setTextFill(App.altColor);
            walletTypeMenuBtn.setUserData(NetworkType.MAINNET);

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

            HBox networkSelectBox = new HBox(networkTxt, networkMenuBtn, networkTypeTxt, walletTypeMenuBtn);
            networkSelectBox.setAlignment(Pos.CENTER_LEFT);

            Text explorerText = new Text("> Explorer:");
            explorerText.setFill(App.txtColor);
            explorerText.setFont(App.txtFont);

            MenuButton explorersBtn = new MenuButton(ErgoExplorer.NAME);
            explorersBtn.setFont(App.txtFont);
            explorersBtn.setTextFill(App.altColor);
            explorersBtn.setUserData(NetworkID.ERGO_EXPLORER);

            MenuItem explorerNoneItem = new MenuItem("(none)");
            explorerNoneItem.setOnAction(e -> {
                explorersBtn.setText(explorerNoneItem.getText());
                explorersBtn.setUserData(null);
            });

            MenuItem ergoExplorerItem = new MenuItem(ErgoExplorer.NAME);
            ergoExplorerItem.setOnAction(e -> {
                explorersBtn.setText(ergoExplorerItem.getText());
                explorersBtn.setUserData(NetworkID.ERGO_EXPLORER);
            });

            explorersBtn.getItems().addAll(explorerNoneItem, ergoExplorerItem);

            HBox explorerBox = new HBox(explorerText, explorersBtn);
            explorerBox.setAlignment(Pos.CENTER_LEFT);

            Text priceNetworkTxt = new Text("> Market price:");
            priceNetworkTxt.setFill(App.txtColor);
            priceNetworkTxt.setFont(App.txtFont);

            MenuButton priceNetworkBtn = new MenuButton(KucoinExchange.NAME);
            priceNetworkBtn.setFont(App.txtFont);
            priceNetworkBtn.setUserData(NetworkID.KUKOIN_EXCHANGE);

            MenuItem priceNetworkNoneItem = new MenuItem("(none)");
            priceNetworkNoneItem.setOnAction(e -> {
                priceNetworkBtn.setText(priceNetworkNoneItem.getText());
                priceNetworkBtn.setUserData(null);
            });

            MenuItem priceNetworkKuCoinItem = new MenuItem(KucoinExchange.NAME);
            priceNetworkKuCoinItem.setOnAction(e -> {
                priceNetworkBtn.setText(priceNetworkKuCoinItem.getText());
                priceNetworkBtn.setUserData(NetworkID.KUKOIN_EXCHANGE);
            });

            priceNetworkBtn.getItems().addAll(priceNetworkNoneItem, priceNetworkKuCoinItem);

            HBox priceNetworkBox = new HBox(priceNetworkTxt, priceNetworkBtn);
            priceNetworkBox.setAlignment(Pos.CENTER_LEFT);

            Text walletTxt = new Text("> Select wallet file:");
            walletTxt.setFont(App.txtFont);
            walletTxt.setFill(App.txtColor);

            HBox textWalletBox = new HBox(walletTxt);
            textWalletBox.setPadding(new Insets(5, 0, 0, 0));
            textWalletBox.setAlignment(Pos.CENTER_LEFT);

            Button newWalletBtn = new Button("Create");

            newWalletBtn.setPadding(new Insets(2, 10, 2, 10));
            newWalletBtn.setFont(App.txtFont);
            newWalletBtn.setPrefWidth(120);

            newWalletBtn.setOnAction(newWalletEvent -> {

                String seedPhrase = createMnemonicStage();
                if (!seedPhrase.equals("")) {
                    String password = App.createPassword(m_addWalletStage, "Ergo - New wallet: Password", App.ergoLogo, "New Wallet");

                    if (!password.equals("")) {
                        Alert nextAlert = new Alert(AlertType.NONE, "Notice:\n\nThis password is required along with the mnemonic phrase in order to restore this wallet.\n\nPlease be aware that you may change the password to access your wallet, but you will always need this password in order to restore this wallet.\n\nIf it is possible for you to forget this password write it down and keep it in a secure location.\n\n", ButtonType.OK);
                        nextAlert.initOwner(m_addWalletStage);
                        nextAlert.setTitle("Password: Notice");
                        nextAlert.showAndWait();

                        Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(password));

                        FileChooser saveFileChooser = new FileChooser();
                        saveFileChooser.setInitialDirectory(m_walletsDirectory);
                        saveFileChooser.setTitle("Save: Wallet file");
                        saveFileChooser.getExtensionFilters().add(ErgoWallet.ergExt);
                        saveFileChooser.setSelectedExtensionFilter(ErgoWallet.ergExt);

                        File walletFile = saveFileChooser.showSaveDialog(m_addWalletStage);

                        if (walletFile == null) {
                            Alert a = new Alert(AlertType.NONE, "Wallet creation:\n\nCanceled by user.\n\n", ButtonType.CLOSE);
                            a.initOwner(m_addWalletStage);
                            a.setTitle("Wallet creation: Canceled");
                            a.showAndWait();

                        } else {
                            NetworkType networkType = (NetworkType) walletTypeMenuBtn.getUserData();

                            Wallet.create(walletFile.toPath(), mnemonic, walletFile.getName(), password.toCharArray());
                            String nodeId = networkMenuBtn.getUserData() == null ? null : (String) networkMenuBtn.getUserData();
                            String explorerId = explorersBtn.getUserData() == null ? null : (String) explorersBtn.getUserData();
                            String marketId = priceNetworkBtn.getUserData() == null ? null : (String) priceNetworkBtn.getUserData();

                            addOpen(new WalletData(friendlyId, walletNameField.getText(), walletFile, nodeId, explorerId, marketId, networkType, m_ergoWallet));
                            m_addWalletStage.close();
                            m_addWalletStage = null;
                        }
                    }
                }

            });

            Button existingWalletBtn = new Button("Open");
            existingWalletBtn.setPadding(new Insets(2, 10, 2, 10));
            existingWalletBtn.setPrefWidth(120);
            existingWalletBtn.setFont(App.txtFont);

            existingWalletBtn.setOnAction(clickEvent -> {
                FileChooser openFileChooser = new FileChooser();
                openFileChooser.setInitialDirectory(m_walletsDirectory);
                openFileChooser.setTitle("Open: Wallet file");
                openFileChooser.getExtensionFilters().add(ErgoWallet.ergExt);
                openFileChooser.setSelectedExtensionFilter(ErgoWallet.ergExt);

                File walletFile = openFileChooser.showOpenDialog(m_addWalletStage);

                if (walletFile != null) {

                    NetworkType networkType = (NetworkType) walletTypeMenuBtn.getUserData();

                    String nodeId = networkMenuBtn.getUserData() == null ? null : (String) networkMenuBtn.getUserData();
                    String explorerId = explorersBtn.getUserData() == null ? null : (String) explorersBtn.getUserData();
                    String marketId = priceNetworkBtn.getUserData() == null ? null : (String) priceNetworkBtn.getUserData();

                    addOpen(new WalletData(friendlyId, walletNameField.getText(), walletFile, nodeId, explorerId, marketId, networkType, m_ergoWallet));

                    m_addWalletStage.close();
                    m_addWalletStage = null;
                }
            });

            Button restoreWalletBtn = new Button("Restore");
            restoreWalletBtn.setPadding(new Insets(2, 5, 2, 5));
            restoreWalletBtn.setFont(App.txtFont);
            restoreWalletBtn.setPrefWidth(120);
            restoreWalletBtn.setOnAction(clickEvent -> {
                String seedPhrase = restoreMnemonicStage();
                if (!seedPhrase.equals("")) {
                    String password = App.createPassword(m_addWalletStage, m_ergoWallet.getName() + " - Restore wallet: Password", m_ergoWallet.getIcon(), "Restore Wallet");

                    if (!password.equals("")) {
                        Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(password));

                        FileChooser saveFileChooser = new FileChooser();
                        saveFileChooser.setInitialDirectory(m_walletsDirectory);
                        saveFileChooser.setTitle("Save: Wallet file");
                        saveFileChooser.getExtensionFilters().add(ErgoWallet.ergExt);
                        saveFileChooser.setSelectedExtensionFilter(ErgoWallet.ergExt);

                        File walletFile = saveFileChooser.showSaveDialog(m_addWalletStage);

                        if (walletFile == null) {
                            Alert a = new Alert(AlertType.NONE, "Wallet restoration: Canceled", ButtonType.CLOSE);
                            a.initOwner(m_addWalletStage);
                            a.setTitle("Wallet restoration: Canceled");
                            a.showAndWait();

                        } else {
                            try {
                                NetworkType networkType = (NetworkType) walletTypeMenuBtn.getUserData();

                                Wallet.create(walletFile.toPath(), mnemonic, seedPhrase, password.toCharArray());

                                String nodeId = networkMenuBtn.getUserData() == null ? null : (String) networkMenuBtn.getUserData();
                                String explorerId = explorersBtn.getUserData() == null ? null : (String) explorersBtn.getUserData();
                                String marketId = priceNetworkBtn.getUserData() == null ? null : (String) priceNetworkBtn.getUserData();

                                addOpen(new WalletData(friendlyId, walletNameField.getText(), walletFile, nodeId, explorerId, marketId, networkType, m_ergoWallet));

                                m_addWalletStage.close();
                                m_addWalletStage = null;
                            } catch (Exception e1) {
                                Alert a = new Alert(AlertType.NONE, "Wallet creation: Cannot be saved.\n\n" + e1.toString(), ButtonType.OK);
                                a.initOwner(m_addWalletStage);
                                a.show();
                            }
                        }

                    }
                }

            });

            Region lRegion = new Region();
            lRegion.setPrefWidth(20);

            Region rRegion = new Region();
            rRegion.setPrefWidth(20);

            HBox newWalletBox = new HBox(newWalletBtn, lRegion, existingWalletBtn, rRegion, restoreWalletBtn);
            newWalletBox.setAlignment(Pos.CENTER);
            VBox.setVgrow(newWalletBox, Priority.ALWAYS);
            newWalletBox.setPadding(new Insets(30, 0, 0, 0));

            VBox bodyBox = new VBox(walletNameBox, networkSelectBox, explorerBox, priceNetworkBox, textWalletBox, newWalletBox);
            bodyBox.setPadding(new Insets(0, 20, 0, 20));

            VBox layoutBox = new VBox(titleBox, imageBox, bodyBox);

            Scene walletScene = new Scene(layoutBox, 600, 450);
            walletScene.getStylesheets().add("/css/startWindow.css");
            m_addWalletStage.setScene(walletScene);

            m_addWalletStage.show();

        } else {
            if (m_addWalletStage.isIconified()) {
                m_addWalletStage.setIconified(false);
            }
        }
    }

    public String createMnemonicStage() {

        Image icon = m_ergoWallet.getIcon();
        String titleStr = m_ergoWallet.getName() + " - New wallet: Mnemonic phrase";

        String mnemonic = Mnemonic.generateEnglishMnemonic();

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(icon);
        mnemonicStage.setResizable(false);
        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(icon, "New Wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase - Required to recover wallet:");
        subTitleTxt.setFill(App.txtColor);
        subTitleTxt.setFont(App.txtFont);

        HBox subTitleBox = new HBox(subTitleTxt);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);

        TextArea mnemonicField = new TextArea(mnemonic);
        mnemonicField.setFont(App.txtFont);
        mnemonicField.setId("textField");
        mnemonicField.setEditable(false);
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

        Button nextBtn = new Button("Next");

        nextBtn.setFont(App.txtFont);
        nextBtn.setOnAction(nxtEvent -> {
            Alert nextAlert = new Alert(AlertType.NONE, "User Agreement:\n\nI have written the mnemonic phrase down and will store it in a secure location.", ButtonType.NO, ButtonType.YES);
            nextAlert.initOwner(mnemonicStage);
            nextAlert.setTitle("User Agreement");
            Optional<ButtonType> result = nextAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                mnemonicStage.close();
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
            Alert terminateAlert = new Alert(AlertType.NONE, "Wallet creation:\n\nTerminated.", ButtonType.CLOSE);
            terminateAlert.initOwner(mnemonicStage);
            terminateAlert.setTitle("Wallet creation: Terminated");
            terminateAlert.showAndWait();

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();
    }

    public String restoreMnemonicStage() {
        String titleStr = m_ergoWallet.getName() + " - Restore wallet: Mnemonic phrase";

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(m_ergoWallet.getIcon());
        mnemonicStage.setResizable(false);
        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(m_ergoWallet.getIcon(), titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(m_ergoWallet.getIcon(), "Restore wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase - Required to recover wallet:");
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
            Alert terminateAlert = new Alert(AlertType.NONE, "Wallet creation:\n\nTerminated.", ButtonType.CLOSE);
            terminateAlert.initOwner(mnemonicStage);
            terminateAlert.setTitle("Wallet creation: Terminated");
            terminateAlert.showAndWait();

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();

    }

    public void setButtonGridNull() {
        if (m_buttonGrid != null) {
            Pane parent = (Pane) m_buttonGrid.getParent();
            if (parent == null) {
                m_buttonGrid.getChildren().clear();
                m_buttonGrid = null;
            } else {
                parent.getChildren().remove(m_buttonGrid);
                m_buttonGrid.getChildren().clear();
                m_buttonGrid = null;
            }
        }
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

    public void sendNoteToTunnelInterface(JsonObject note, String tunnelId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int index = tunnelId.indexOf(":");

        String networkId = index == -1 ? tunnelId : tunnelId.substring(0, index);

        NoteInterface networkInterface = getNoteInterface(networkId);

        for (NoteInterface noteInterface : networkInterface.getTunnelNoteInterfaces()) {

            if (noteInterface.getNetworkId().equals(tunnelId)) {
                noteInterface.sendNoteToTunnelInterface(note, tunnelId, onSucceeded, onFailed);
            }
        }

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

    public double getWidth() {
        return m_width;
    }

    public void setWidth(double width) {
        m_width = width;
        updateGrid();
    }

    public VBox getButtonGrid() {
        if (m_buttonGrid == null) {
            m_buttonGrid = new VBox();
            HBox.setHgrow(m_buttonGrid, Priority.ALWAYS);
        }
        updateGrid();
        return m_buttonGrid;
    }

    public void updateGrid() {

        int numCells = m_noteInterfaceList.size();

        m_buttonGrid.getChildren().clear();
        VBox.setVgrow(m_buttonGrid, Priority.ALWAYS);

        for (int i = 0; i < numCells; i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);

            IconButton rowButton = noteInterface.getButton();
            VBox.setVgrow(rowButton, Priority.ALWAYS);
            m_buttonGrid.getChildren().add(rowButton);
        }
        /*
            try {
                Files.writeString(logFile.toPath(), "networks: " + numCells);
            } catch (IOException e) {

            } 
           
                double imageWidth = 100;
                double cellPadding = 15;
                double cellWidth = imageWidth + (cellPadding * 2);

                int floor = (int) Math.floor(m_width / (cellWidth + 20));

                int numCol = floor == 0 ? 1 : floor;

                int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1; //  (int) ((numCells > 0) && (numCol != 0) ? Math.ceil(numCells / numCol) : 1);

                HBox[] rowsBoxes = new HBox[numRows];
                for (int i = 0; i < numRows; i++) {
                    rowsBoxes[i] = new HBox();
                    m_buttonGrid.getChildren().add(rowsBoxes[i]);
                }

                //Image iconImage = ergoNetworkImg;
                ItemIterator grid = new ItemIterator();

                for (NoteInterface noteInterface : m_noteInterfaceList) {
                    // gridBox.getChildren().add(network.getButton());
                    HBox rowBox = rowsBoxes[grid.getJ()];
                    rowBox.getChildren().add(noteInterface.getButton());

                    if (grid.getI() < numCol) {
                        grid.setI(grid.getI() + 1);
                    } else {
                        grid.setI(0);
                        grid.setJ(0);
                    }
                }*/

    }

}
