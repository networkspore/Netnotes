package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.SecretString;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoWallet extends Network implements NoteInterface {

    public static String DESCRIPTION = "Ergo Wallet allows you to create and manage wallets on the Ergo Blockchain.";
    public static String SUMMARY = "Access can be controlled with the Ergo Wallet, in order to keep the wallet isolated, or access can be given to the Ergo Network in order to make transactions, or the Ergo Explorer to get your ERG ballance and to the KuCoin Exchange to get your ERG value real time.";
    public static String NAME = "Ergo Wallet";
    public static ExtensionFilter ergExt = new ExtensionFilter("Ergo wallet", "*.erg");

    private File logFile = new File("ergoWallet - log.txt");

    private File m_walletFile = null;
    private NetworkType m_networkType = NetworkType.MAINNET;
    private File m_walletsDir = new File("/wallets");
    private String m_explorerNetworkID = NetworkID.ERGO_EXPLORER;

    private PriceChart m_ergoPriceChart;

    private String m_direction = "row";

    private WalletsDataList m_walletsData;
    private Stage m_addWalletStage = null;
    private Stage m_walletsStage = null;

    public ErgoWallet(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_WALLET, networksData);
        createWalletDirectory();
        m_explorerNetworkID = NetworkID.ERGO_EXPLORER;
        m_walletsData = new WalletsDataList(new JsonArray(), this);
        m_networkType = NetworkType.MAINNET;
        m_ergoPriceChart = new PriceChart(this, NetworkID.KUKOIN_EXCHANGE, "ERG-USDT", "30min");
    }

    public ErgoWallet(JsonObject jsonObject, NetworksData networksData) {

        super(getAppIcon(), NAME, NetworkID.ERGO_WALLET, networksData);

        if (jsonObject != null) {
            JsonElement exchangeElement = jsonObject.get("exchangeData");
            JsonElement explorerIdElement = jsonObject.get("explorerId");
            JsonElement walletsElement = jsonObject.get("walletsData");
            JsonElement typeElement = jsonObject.get("networkType");

            JsonArray walletsArray = walletsElement != null ? walletsElement.getAsJsonArray() : new JsonArray();
            m_explorerNetworkID = explorerIdElement == null ? null : explorerIdElement.getAsString();
            m_walletsData = new WalletsDataList(walletsArray, this);
            m_networkType = typeElement != null ? NetworkType.fromValue(typeElement.getAsString()) : NetworkType.MAINNET;
            m_ergoPriceChart = exchangeElement != null ? new PriceChart(exchangeElement.getAsJsonObject(), this) : new PriceChart(this, null, "ERG-USDT", "30min");

        }

        // m_ergoPriceChart = new PriceChart(this, NetworkID.KUKOIN_EXCHANGE, "ERG-USDT", "30min");
    }

    public static Image getAppIcon() {
        return App.ergoWallet;
    }

    @Override
    public void open() {
        /* Alert a = new Alert(AlertType.NONE, "opening", ButtonType.CLOSE);
        a.show(); */
        showWalletsStage();
    }

    public void showWalletsStage() {
        if (m_walletsStage == null) {
            m_walletsStage = new Stage();
            m_walletsStage.getIcons().add(getIcon());
            m_walletsStage.setResizable(false);
            m_walletsStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_walletsStage.close();
                m_walletsStage = null;
            });

            HBox titleBox = App.createTopBar(getIcon(), getName() + " - Wallets (" + m_networkType + ")", closeBtn, m_walletsStage);

            ImageView addImage = new ImageView(App.addImg);
            addImage.setFitHeight(10);
            addImage.setPreserveRatio(true);

            Tooltip addTip = new Tooltip("New");
            addTip.setShowDelay(new javafx.util.Duration(100));
            addTip.setFont(App.txtFont);

            Tooltip removeTip = new Tooltip("Remove");
            removeTip.setShowDelay(new javafx.util.Duration(100));
            removeTip.setFont(App.txtFont);

            Button addButton = new Button("New");
            // addButton.setGraphic(addImage);
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setTooltip(addTip);
            HBox.setHgrow(addButton, Priority.ALWAYS);

            Button removeButton = new Button("Remove");
            // removeButton.setGraphic(addImage);
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setTooltip(removeTip);
            removeButton.setDisable(true);

            HBox.setHgrow(removeButton, Priority.ALWAYS);

            HBox menuBar = new HBox(addButton, removeButton);
            menuBar.setId("blackMenu");
            menuBar.setPrefHeight(60);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setPadding(new Insets(5, 5, 5, 5));

            VBox walletsBox = m_walletsData.getButtonGrid();

            VBox layoutVBox = new VBox(titleBox, walletsBox, menuBar);

            Scene walletsScene = new Scene(layoutVBox, 400, 500);

            walletsScene.getStylesheets().add("/css/startWindow.css");
            m_walletsStage.setScene(walletsScene);

            m_walletsStage.show();

            addButton.setOnAction(event -> {
                showAddWalletStage();
            });

            if (m_walletsData.size() == 0) {
                showAddWalletStage();
            }
        } else {
            if (m_walletsStage.isIconified()) {
                m_walletsStage.setIconified(false);
            }
        }

    }

    public void showAddWalletStage() {
        if (m_addWalletStage == null) {
            m_addWalletStage = new Stage();
            m_addWalletStage.getIcons().add(getIcon());
            m_addWalletStage.setResizable(false);
            m_addWalletStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_addWalletStage.close();
                m_addWalletStage = null;
            });

            HBox titleBox = App.createTopBar(getIcon(), getName() + " - Add wallet (" + m_networkType + ")", closeBtn, m_addWalletStage);

            Button lockDocBtn = App.createImageButton(App.lockDocumentImg, "Wallet File");
            HBox imageBox = new HBox(lockDocBtn);
            imageBox.setAlignment(Pos.CENTER);

            Text walletTxt = new Text("> Select wallet file:");
            walletTxt.setFont(App.txtFont);
            walletTxt.setFill(App.txtColor);

            HBox textWalletBox = new HBox(walletTxt);
            textWalletBox.setAlignment(Pos.CENTER_LEFT);
            textWalletBox.setPadding(new Insets(10, 0, 0, 0));

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
                        saveFileChooser.setInitialDirectory(m_walletsDir);
                        saveFileChooser.setTitle("Save: Wallet file");
                        saveFileChooser.getExtensionFilters().add(ergExt);
                        saveFileChooser.setSelectedExtensionFilter(ergExt);

                        File walletFile = saveFileChooser.showSaveDialog(m_addWalletStage);

                        if (walletFile == null) {
                            Alert a = new Alert(AlertType.NONE, "Wallet creation:\n\nCanceled by user.\n\n", ButtonType.CLOSE);
                            a.initOwner(m_addWalletStage);
                            a.setTitle("Wallet creation: Canceled");
                            a.showAndWait();

                        } else {
                            String newId = FriendlyId.createFriendlyId();
                            Wallet.create(walletFile.toPath(), mnemonic, walletFile.getName(), password.toCharArray());

                            m_walletsData.addOpen(new WalletData(newId, walletFile.getName(), walletFile, m_networkType, this));
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
                openFileChooser.setInitialDirectory(m_walletsDir);
                openFileChooser.setTitle("Open: Wallet file");
                openFileChooser.getExtensionFilters().add(ergExt);
                openFileChooser.setSelectedExtensionFilter(ergExt);

                File walletFile = openFileChooser.showOpenDialog(m_addWalletStage);

                if (walletFile != null) {

                    m_walletsData.addOpen(new WalletData(FriendlyId.createFriendlyId(), walletFile.getName(), walletFile, m_networkType, this));

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
                    String password = App.createPassword(m_addWalletStage, getName() + " - Restore wallet: Password", getIcon(), "Restore Wallet");

                    if (!password.equals("")) {
                        Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(password));

                        FileChooser saveFileChooser = new FileChooser();
                        saveFileChooser.setInitialDirectory(m_walletsDir);
                        saveFileChooser.setTitle("Save: Wallet file");
                        saveFileChooser.getExtensionFilters().add(ergExt);
                        saveFileChooser.setSelectedExtensionFilter(ergExt);

                        File walletFile = saveFileChooser.showSaveDialog(m_addWalletStage);

                        if (walletFile == null) {
                            Alert a = new Alert(AlertType.NONE, "Wallet restoration: Canceled", ButtonType.CLOSE);
                            a.initOwner(m_addWalletStage);
                            a.setTitle("Wallet restoration: Canceled");
                            a.showAndWait();

                        } else {
                            try {
                                Wallet.create(walletFile.toPath(), mnemonic, seedPhrase, password.toCharArray());

                                m_walletsData.addOpen(new WalletData(FriendlyId.createFriendlyId(), walletFile.getName(), walletFile, m_networkType, this));

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
            newWalletBox.setPadding(new Insets(45, 0, 0, 0));

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setId("hGradient");

            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(45, 0, 0, 0));
            HBox.setHgrow(gBox, Priority.ALWAYS);

            Button okBtn = new Button("Close");
            okBtn.setPadding(new Insets(2, 5, 2, 5));
            okBtn.setFont(App.txtFont);
            okBtn.setPrefWidth(120);
            okBtn.setOnAction(clickEvent -> {
                m_addWalletStage.close();
                m_addWalletStage = null;
            });

            HBox okBox = new HBox(okBtn);
            okBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(okBox, Priority.ALWAYS);
            okBox.setPadding(new Insets(20, 0, 0, 0));

            VBox bodyBox = new VBox(textWalletBox, newWalletBox, gBox, okBox);
            bodyBox.setPadding(new Insets(0, 20, 0, 20));

            VBox layoutBox = new VBox(titleBox, imageBox, bodyBox);

            Scene walletScene = new Scene(layoutBox, 600, 425);
            walletScene.getStylesheets().add("/css/startWindow.css");
            m_addWalletStage.setScene(walletScene);

            m_addWalletStage.show();

        } else {
            if (m_addWalletStage.isIconified()) {
                m_addWalletStage.setIconified(false);
            }
        }
    }

    public void createWalletDirectory() {
        if (!m_walletsDir.isDirectory()) {
            try {
                Files.createDirectories(m_walletsDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjecElement = note.get("subject");
        if (subjecElement != null) {
            switch (subjecElement.getAsString()) {
            }
        }
        return false;
    }

    public String restoreMnemonicStage() {
        String titleStr = getName() + " - Restore wallet: Mnemonic phrase";

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(getIcon());
        mnemonicStage.setResizable(false);
        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(getIcon(), titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(getIcon(), "Restore wallet");

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

    public String createMnemonicStage() {
        String titleStr = getName() + " - New wallet: Mnemonic phrase";

        String mnemonic = Mnemonic.generateEnglishMnemonic();

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(getIcon());
        mnemonicStage.setResizable(false);
        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(getIcon(), titleStr, closeBtn, mnemonicStage);

        Button imageButton = App.createImageButton(getIcon(), "New Wallet");

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

}
