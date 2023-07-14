package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.zip.ZipEntry;

import java.util.zip.ZipInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.bouncycastle.util.encoders.Hex;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rfksystems.blake2b.Blake2b;
import com.utils.Utils;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoTokens extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Tokens allows you to manage your interactions with the tokens on the Ergo Network.";
    public final static String SUMMARY = "Mange your tokens with Ergo Tokens.";
    public final static String NAME = "Ergo Tokens";

    private File logFile = new File("ErgoTokens-log.txt");
    private File m_dataFile = null;
    private File m_testnetDataFile = null;
    private File m_appDir = null;
    private Stage m_tokensStage = null;

    private NetworkType m_networkType = NetworkType.MAINNET;
    private String m_explorerId = NetworkID.ERGO_EXPLORER;

    public ErgoTokens(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_TOKENS, networksData);

        m_appDir = new File(System.getProperty("user.dir") + "/" + NAME);

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
            File tokensDir = new File(m_appDir.getAbsolutePath() + "\tokens");

            m_testnetDataFile = new File(m_appDir.getAbsolutePath() + "/testnet" + NAME + ".dat");
            m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");

            setupTokens(networksData.getAppKey(), tokensDir);
        } else {
            m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");
            m_testnetDataFile = new File(m_appDir.getAbsolutePath() + "/testnet" + NAME + ".dat");
        }

    }

    public ErgoTokens(JsonObject jsonObject, NetworksData networksData) {

        super(getAppIcon(), NAME, NetworkID.ERGO_TOKENS, networksData);

        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement appDirElement = jsonObject.get("appDir");
        JsonElement dataElement = jsonObject.get("dataFile");
        JsonElement testnetDataElement = jsonObject.get("testnetDataFile");

        if (networkTypeElement != null) {
            if (networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString())) {
                m_networkType = NetworkType.TESTNET;
            } else {
                m_networkType = NetworkType.MAINNET;
            }
        }

        if (appDirElement != null) {
            m_appDir = new File(appDirElement.getAsString());
        } else {

            m_appDir = new File(System.getProperty("user.dir") + "/" + ErgoTokens.NAME);
        }

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
            File tokensDir = new File(m_appDir.getAbsolutePath() + "/tokens");

            if (dataElement == null) {
                m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + ErgoTokens.NAME + ".dat");
            } else {
                m_dataFile = new File(dataElement.getAsString());
            }

            if (testnetDataElement == null) {
                m_testnetDataFile = new File(m_appDir.getAbsolutePath() + "/testnet" + ErgoTokens.NAME + ".dat");
            } else {
                m_testnetDataFile = new File(testnetDataElement.getAsString());
            }

            setupTokens(getNetworksData().getAppKey(), tokensDir);
        } else {

            if (dataElement == null) {
                m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + ErgoTokens.NAME + ".dat");
            } else {
                m_dataFile = new File(dataElement.getAsString());
            }

            if (testnetDataElement == null) {
                m_testnetDataFile = new File(m_appDir.getAbsolutePath() + "/testnet" + ErgoTokens.NAME + ".dat");
            } else {
                m_testnetDataFile = new File(testnetDataElement.getAsString());
            }
        }

    }

    @Override
    public void open() {

        showTokensStage();
    }

    public NoteInterface getExplorerInterface() {
        if (m_explorerId != null) {
            return getNetworksData().getNoteInterface(m_explorerId);
        }
        return null;
    }

    public void setNetworkType(NetworkType networkType) {
        m_networkType = networkType;
        getLastUpdated().set(LocalDateTime.now());

    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public void showTokensStage() {
        if (m_tokensStage == null) {

            TokensList tokensList = new TokensList(m_networkType, this);
            tokensList.addUpdateListener((obs, oldVal, newVal) -> {

                try {
                    Files.writeString(logFile.toPath(), "\nSaving updates", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
                save(getNetworksData().getAppKey(), tokensList.getJsonObject(), m_networkType);
            });

            double tokensStageWidth = 375;
            double tokensStageHeight = 600;
            double buttonHeight = 100;

            m_tokensStage = new Stage();
            m_tokensStage.getIcons().add(getIcon());
            m_tokensStage.initStyle(StageStyle.UNDECORATED);
            m_tokensStage.setTitle(getName() + ": Tokens " + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_tokensStage.close();
                m_tokensStage = null;
                tokensList.closeAll();
                tokensList.removeUpdateListener();
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getIcon(), maxBtn, closeBtn, m_tokensStage);

            MenuButton menuBtn = new MenuButton();
            menuBtn.setGraphic(IconButton.getIconView(new Image("/assets/menu-outline-30.png"), 30));

            MenuItem importBtn = new MenuItem(" Import JSON...");
            importBtn.setId("menuBtn");
            importBtn.setOnAction(action -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Import JSON File...");
                chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text/json", "*.json"));
                File openFile = chooser.showOpenDialog(m_tokensStage);
                if (openFile != null) {
                    tokensList.importJson(m_tokensStage, openFile);
                }
            });

            MenuItem exportBtn = new MenuItem(" Export JSON...");
            exportBtn.setId("menuBtn");
            exportBtn.setOnAction(action -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Export JSON file...");
                chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text/json", "*.json"));
                chooser.setInitialFileName("ergoTokens-" + m_networkType);
                File saveFile = chooser.showSaveDialog(m_tokensStage);

                if (saveFile != null) {
                    try {
                        Files.writeString(saveFile.toPath(), tokensList.getJsonObject().toString());
                    } catch (IOException e) {
                        Alert writeAlert = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                        writeAlert.initOwner(m_tokensStage);
                        writeAlert.setGraphic(IconButton.getIconView(getIcon(), 75));

                    }
                }
            });

            menuBtn.getItems().addAll(importBtn, exportBtn);

            Tooltip toggleTip = new Tooltip((m_networkType == NetworkType.MAINNET ? "MAINNET" : "TESTNET"));
            toggleTip.setShowDelay(new javafx.util.Duration(100));
            toggleTip.setFont(App.txtFont);

            Button toggleNetworkTypeBtn = new Button();
            toggleNetworkTypeBtn.setGraphic(m_networkType == NetworkType.MAINNET ? IconButton.getIconView(new Image("/assets/toggle-on.png"), 30) : IconButton.getIconView(new Image("/assets/toggle-off.png"), 30));
            toggleNetworkTypeBtn.setId("menuBtn");
            toggleNetworkTypeBtn.setTooltip(toggleTip);
            toggleNetworkTypeBtn.setOnAction(e -> {

                setNetworkType(m_networkType == NetworkType.MAINNET ? NetworkType.TESTNET : NetworkType.MAINNET);

                toggleTip.setText((m_networkType == NetworkType.MAINNET ? "MAINNET" : "TESTNET"));
                toggleNetworkTypeBtn.setGraphic(m_networkType == NetworkType.MAINNET ? IconButton.getIconView(new Image("/assets/toggle-on.png"), 30) : IconButton.getIconView(new Image("/assets/toggle-off.png"), 30));
                m_tokensStage.setTitle(getName() + ": Tokens " + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));

                tokensList.setNetworkType(m_networkType);
            });

            Tooltip explorerTip = new Tooltip(getExplorerInterface() == null ? "Explorer disabled" : getExplorerInterface().getName());
            explorerTip.setShowDelay(new javafx.util.Duration(100));
            explorerTip.setFont(App.txtFont);

            MenuButton explorerBtn = new MenuButton();
            explorerBtn.setGraphic(getExplorerInterface() == null ? IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30) : IconButton.getIconView(new InstallableIcon(getNetworksData(), getExplorerInterface().getNetworkId(), true).getIcon(), 30));
            explorerBtn.setPadding(new Insets(2, 0, 0, 0));
            explorerBtn.setTooltip(explorerTip);

            MenuItem explorerNullMenuItem = new MenuItem("(none)");

            explorerNullMenuItem.setOnAction(e -> {

                m_explorerId = null;
                explorerBtn.setUserData(null);
                explorerBtn.setGraphic(IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30));
                tokensList.closeAll();
            });

            MenuItem ergoExplorerMenuItem = new MenuItem(ErgoExplorer.NAME);
            ergoExplorerMenuItem.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), 30));

            ergoExplorerMenuItem.setOnAction(e -> {

                m_explorerId = NetworkID.ERGO_EXPLORER;
                explorerBtn.setUserData(NetworkID.ERGO_EXPLORER);
                explorerBtn.setGraphic(IconButton.getIconView(ErgoExplorer.getSmallAppIcon(), 30));

                if (getExplorerInterface() == null) {
                    Alert explorerAlert = new Alert(AlertType.NONE, "Attention:\n\n" + ErgoExplorer.NAME + " enabled for " + ErgoTokens.NAME + ". However " + ErgoExplorer.NAME + " is not installed on NetNotes.\n\n" + ErgoExplorer.NAME + " must be installed to use this feature.", ButtonType.OK);
                    explorerAlert.setGraphic(IconButton.getIconView(ErgoExplorer.getAppIcon(), 75));
                    explorerAlert.initOwner(m_tokensStage);
                    explorerAlert.show();
                }

                tokensList.closeAll();
            });

            explorerBtn.getItems().addAll(explorerNullMenuItem, ergoExplorerMenuItem);

            HBox rightSideMenu = new HBox(explorerBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuBtn, toggleNetworkTypeBtn, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            ImageView addImage = new ImageView(App.addImg);
            addImage.setFitHeight(10);
            addImage.setPreserveRatio(true);

            Tooltip addTip = new Tooltip("New");
            addTip.setShowDelay(new javafx.util.Duration(100));
            addTip.setFont(App.txtFont);

            VBox layoutVBox = new VBox(titleBox);
            layoutVBox.setPadding(new Insets(0, 5, 0, 5));
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            VBox tokensBox = tokensList.getButtonGrid();

            Region growRegion = new Region();

            VBox.setVgrow(growRegion, Priority.ALWAYS);

            VBox bodyBox = new VBox(tokensBox, growRegion);

            ScrollPane scrollPane = new ScrollPane(bodyBox);

            scrollPane.setId("bodyBox");

            Button addButton = new Button("New");
            // addButton.setGraphic(addImage);
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setTooltip(addTip);
            addButton.setPrefWidth(tokensStageWidth / 2);
            addButton.setPrefHeight(buttonHeight);

            Tooltip removeTip = new Tooltip("Remove");
            removeTip.setShowDelay(new javafx.util.Duration(100));
            removeTip.setFont(App.txtFont);

            Button removeButton = new Button("Remove");

            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setTooltip(removeTip);
            removeButton.setDisable(true);
            removeButton.setPrefWidth(tokensStageWidth / 2);
            removeButton.setPrefHeight(buttonHeight);
            removeButton.setUserData(null);

            removeButton.setOnAction(action -> {
                Object removeButtonData = removeButton.getUserData();

                if (removeButtonData != null && removeButtonData instanceof ErgoNetworkToken) {
                    ErgoNetworkToken selectedToken = (ErgoNetworkToken) removeButtonData;

                    Alert a = new Alert(AlertType.NONE, "Would you like to remove '" + selectedToken.getName() + "' from Ergo Tokens?", ButtonType.NO, ButtonType.YES);
                    a.initOwner(m_tokensStage);
                    a.setTitle("Remove Token - " + selectedToken.getName());
                    a.setGraphic(IconButton.getIconView(selectedToken.getIcon(), 40));
                    a.setHeaderText("Remove Token");
                    Optional<ButtonType> result = a.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.YES) {

                        tokensList.removeToken(selectedToken.getNetworkId());
                    }
                }
            });

            HBox menuBox = new HBox(addButton, removeButton);
            HBox.setHgrow(menuBox, Priority.ALWAYS);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            layoutVBox.getChildren().addAll(menuBar, scrollPane, menuBox);

            Scene tokensScene = new Scene(layoutVBox, tokensStageWidth, tokensStageHeight);

            tokensScene.focusOwnerProperty().addListener((e) -> {
                Object focusOwnerObject = tokensScene.focusOwnerProperty().get();
                if (focusOwnerObject instanceof ErgoNetworkToken) {
                    removeButton.setUserData(focusOwnerObject);
                    removeButton.setDisable(false);
                    removeButton.setId("menuBarBtn");
                } else {
                    if (focusOwnerObject instanceof Button && ((Button) focusOwnerObject).getText().equals("Remove")) {

                    } else {
                        removeButton.setUserData(null);
                        removeButton.setDisable(true);
                        removeButton.setId("menuBarBtnDisabled");
                    }
                }
            });

            scrollPane.prefViewportWidthProperty().bind(tokensScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(tokensScene.heightProperty().subtract(menuBar.heightProperty()).subtract(menuBox.heightProperty()));

            addButton.prefWidthProperty().bind(menuBox.widthProperty().divide(2));
            removeButton.prefWidthProperty().bind(menuBox.widthProperty().divide(2));

            tokensBox.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            //  bodyBox.prefHeightProperty().bind(tokensScene.heightProperty() - 40 - 100);
            tokensScene.getStylesheets().add("/css/startWindow.css");
            m_tokensStage.setScene(tokensScene);

            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            addButton.setOnAction(actionEvent -> {
                m_tokensStage.setScene(tokensList.getExistingTokenScene(null, m_networkType, m_tokensStage, tokensScene));
                ResizeHelper.addResizeListener(m_tokensStage, 500, 620, rect.getWidth(), rect.getHeight());
            });

            ResizeHelper.addResizeListener(m_tokensStage, 300, 400, rect.width, rect.height);
            m_tokensStage.setOnCloseRequest(windowEvent -> {
                tokensList.closeAll();
                tokensList.removeUpdateListener();
            });
            m_tokensStage.show();
        } else {
            if (m_tokensStage.isIconified()) {
                m_tokensStage.setIconified(false);
            }
            m_tokensStage.show();
        }

    }

    public File getDataFile() {
        return m_dataFile;
    }

    public File getTestnetDataFile() {
        return m_testnetDataFile;
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjectElement = note.get("subject");
        JsonElement networkTypeElement = note.get("networkType");

        if (subjectElement != null) {
            switch (subjectElement.getAsString()) {
                case "GET_ERGO_TOKENS_STAGE":
                    if (m_tokensStage == null) {
                        open();
                    }
                    Utils.returnObject(m_tokensStage, onSucceeded, onFailed);
                    break;

                case "GET_DATAFILE_LOCATION":
                    if (networkTypeElement != null) {
                        String networkTypeString = networkTypeElement.getAsString();
                        NetworkType networkType = networkTypeString.equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : networkTypeString.equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : null;

                        if (networkType == null) {
                            return false;
                        }

                        String path = networkType == NetworkType.MAINNET ? m_dataFile.getAbsolutePath() : m_testnetDataFile.getAbsolutePath();

                        Utils.returnObject(path, onSucceeded, onFailed);
                        return true;
                    }
                    break;
                case "GET_EXPLORER_INTERFACE_ID":
                    Utils.returnObject(m_explorerId, onSucceeded, onFailed);
                    return true;
            }
        }
        return false;

    }

    public static Image getAppIcon() {
        return new Image("/assets/diamond-150.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/diamond-30.png");
    }

    public File getAppDir() {
        return m_appDir;
    }

    public void setAppDir(String string) {
        m_appDir = new File(string);

        getLastUpdated().set(LocalDateTime.now());
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.addProperty("appDir", m_appDir.getAbsolutePath());
        json.addProperty("networkType", m_networkType.toString());
        if (m_dataFile != null) {
            json.addProperty("dataFile", m_dataFile.getAbsolutePath());
        }
        if (m_testnetDataFile != null) {
            json.addProperty("testnetDataFile", m_testnetDataFile.getAbsolutePath());
        }
        return json;
    }

    public void setupTokens(SecretKey appKey, File tokensDir) {
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
        boolean createdtokensDirectory = false;
        if (!tokensDir.isDirectory()) {
            try {
                Files.createDirectories(tokensDir.toPath());
                createdtokensDirectory = true;

            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }

        if (tokensDir.isDirectory() && createdtokensDirectory) {
            ArrayList<ErgoNetworkToken> ergoTokenList = new ArrayList<ErgoNetworkToken>();

            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/ergoTokenIcons.zip");

            ZipInputStream zipStream = null;
            String hashId = Blake2b.BLAKE2_B_256;
            try {

                zipStream = new ZipInputStream(is);
                final MessageDigest digest = MessageDigest.getInstance(hashId);

                String tokensPathString = tokensDir.getAbsolutePath() + "\\";

                if (zipStream != null) {
                    // Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {

                        String entryName = entry.getName();

                        int indexOfDir = entryName.lastIndexOf("/");

                        if (indexOfDir != entryName.length() - 1) {

                            int indexOfExt = entryName.lastIndexOf(".");

                            String fileName = entryName.substring(0, indexOfExt);

                            File newDirFile = new File(tokensPathString + "\\" + fileName);
                            if (!newDirFile.isDirectory()) {
                                Files.createDirectory(newDirFile.toPath());
                            }
                            String fileString = tokensPathString + "\\" + fileName + "\\" + entryName;
                            File entryFile = new File(fileString);
                            OutputStream outStream = null;
                            try {

                                outStream = new FileOutputStream(entryFile);
                                //outStream.write(buffer);

                                byte[] buffer = new byte[8 * 1024];
                                int bytesRead;
                                while ((bytesRead = zipStream.read(buffer)) != -1) {

                                    outStream.write(buffer, 0, bytesRead);
                                    digest.update(buffer, 0, bytesRead);
                                }
                                byte[] hashbytes = digest.digest();

                                HashData hashData = new HashData(hashbytes);
                                try {
                                    Files.writeString(logFile.toPath(), "\n" + hashData.getJsonObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {

                                }

                                ErgoNetworkToken token = createToken(fileName, fileString, hashData);
                                try {
                                    Files.writeString(logFile.toPath(), "\n" + token.getJsonObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {

                                }
                                if (token != null) {
                                    ergoTokenList.add(token);
                                }

                            } catch (IOException ex) {
                                try {
                                    Files.writeString(logFile.toPath(), "\n" + ex.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {

                                }
                            } finally {
                                if (outStream != null) {
                                    outStream.close();
                                }
                            }
                        }

                    }
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                try {
                    Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            } finally {
                if (zipStream != null) {

                    try {
                        zipStream.close();
                    } catch (IOException e2) {
                        try {
                            Files.writeString(logFile.toPath(), "\n" + e2.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {

                        }
                    }

                }
            }

            TokensList tokensList = new TokensList(ergoTokenList, m_networkType, this);
            save(appKey, tokensList.getJsonObject(), m_networkType);
        }

    }

    public ErgoNetworkToken createToken(String key, String imageString, HashData hashData) {
        ErgoNetworkToken ergoToken = null;
        NetworkType networkType = NetworkType.MAINNET;
        switch (key) {
            case "aht":
                ergoToken = new ErgoNetworkToken("Ergo Auction House", "https://ergoauctions.org/", "18c938e1924fc3eadc266e75ec02d81fe73b56e4e9f4e268dffffcb30387c42d", imageString, hashData, networkType, this);
                break;
            case "comet":
                ergoToken = new ErgoNetworkToken("Comet", "https://thecomettoken.com/", "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b", imageString, hashData, networkType, this);
                break;
            case "cypx":
                ergoToken = new ErgoNetworkToken("CyberVerse", "https://cybercitizens.io/dist/pages/cyberverse.html", "01dce8a5632d19799950ff90bca3b5d0ca3ebfa8aaafd06f0cc6dd1e97150e7f", imageString, hashData, networkType, this);
                break;
            case "egio":
                ergoToken = new ErgoNetworkToken("ErgoGames.io", "https://www.ergogames.io/", "00b1e236b60b95c2c6f8007a9d89bc460fc9e78f98b09faec9449007b40bccf3", imageString, hashData, networkType, this);
                break;
            case "epos":
                ergoToken = new ErgoNetworkToken("ErgoPOS", "https://www.tabbylab.io/", "00bd762484086cf560d3127eb53f0769d76244d9737636b2699d55c56cd470bf", imageString, hashData, networkType, this);
                break;
            case "erdoge":
                ergoToken = new ErgoNetworkToken("Erdoge", "https://erdoge.biz/", "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", imageString, hashData, networkType, this);
                break;

            case "ergold":
                ergoToken = new ErgoNetworkToken("Ergold", "https://github.com/supERGeometry/Ergold", "e91cbc48016eb390f8f872aa2962772863e2e840708517d1ab85e57451f91bed", imageString, hashData, networkType, this);
                break;
            case "ergone":
                ergoToken = new ErgoNetworkToken("ErgOne NFT", "http://ergone.io/", "fcfca7654fb0da57ecf9a3f489bcbeb1d43b56dce7e73b352f7bc6f2561d2a1b", imageString, hashData, networkType, this);
                break;
            case "ergopad":
                ergoToken = new ErgoNetworkToken("ErgoPad", "https://www.ergopad.io/", "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", imageString, hashData, networkType, this);
                break;
            case "ermoon":
                ergoToken = new ErgoNetworkToken("ErMoon", "", "9dbc8dd9d7ea75e38ef43cf3c0ffde2c55fd74d58ac7fc0489ec8ffee082991b", imageString, hashData, networkType, this);
                break;
            case "exle":
                ergoToken = new ErgoNetworkToken("Ergo-Lend", "https://exle.io/", "007fd64d1ee54d78dd269c8930a38286caa28d3f29d27cadcb796418ab15c283", imageString, hashData, networkType, this);
                break;
            case "flux":
                ergoToken = new ErgoNetworkToken("Flux", "https://runonflux.io/", "e8b20745ee9d18817305f32eb21015831a48f02d40980de6e849f886dca7f807", imageString, hashData, networkType, this);
                break;
            case "getblock":
                ergoToken = new ErgoNetworkToken("GetBlok.io", "https://www.getblok.io/", "4f5c05967a2a68d5fe0cdd7a688289f5b1a8aef7d24cab71c20ab8896068e0a8", imageString, hashData, networkType, this);
                break;
            case "kushti":
                ergoToken = new ErgoNetworkToken("Kushti", "https://github.com/kushti", "fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40", imageString, hashData, networkType, this);
                break;
            case "love":
                ergoToken = new ErgoNetworkToken("Love", "https://explorer.ergoplatform.com/en/issued-tokens?searchQuery=3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", "3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", imageString, hashData, networkType, this);
                break;
            case "lunadog":
                ergoToken = new ErgoNetworkToken("LunaDog", "https://explorer.ergoplatform.com/en/issued-tokens?searchQuery=5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", "5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", imageString, hashData, networkType, this);
                break;
            case "migoreng":
                ergoToken = new ErgoNetworkToken("Mi Goreng", "https://docs.google.com/spreadsheets/d/148c1iHNMNfyjscCcPznepkEnMp2Ycj3HuLvpcLsnWrM/edit#gid=205730070", "0779ec04f2fae64e87418a1ad917639d4668f78484f45df962b0dec14a2591d2", imageString, hashData, networkType, this);
                break;
            case "neta":
                ergoToken = new ErgoNetworkToken("anetaBTC", "https://anetabtc.io/", "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8", imageString, hashData, networkType, this);
                break;
            case "obsidian":
                ergoToken = new ErgoNetworkToken("Adventurers DAO", "https://adventurersdao.xyz/", "2a51396e09ad9eca60b1bdafd365416beae155efce64fc3deb0d1b3580127b8f", imageString, hashData, networkType, this);
                break;
            case "ogre":
                ergoToken = new ErgoNetworkToken("Ogre", "https://ogre-token.web.app", "6de6f46e5c3eca524d938d822e444b924dbffbe02e5d34bd9dcd4bbfe9e85940", imageString, hashData, networkType, this);
                break;
            case "paideia":
                ergoToken = new ErgoNetworkToken("Paideia", "https://www.paideia.im/", "1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489", imageString, hashData, networkType, this);
                break;
            case "proxie":
                ergoToken = new ErgoNetworkToken("Proxies NFT", "https://proxiesnft.io/", "01ddcc3d0205c2da8a067ffe047a2ccfc3e8241bc3fcc6f6ebc96b7f7363bb36", imageString, hashData, networkType, this);
                break;
            case "quacks":
                ergoToken = new ErgoNetworkToken("duckpools.io", "https://www.duckpools.io/", "089990451bb430f05a85f4ef3bcb6ebf852b3d6ee68d86d78658b9ccef20074f", imageString, hashData, networkType, this);
                break;
            case "sigrsv":
                ergoToken = new ErgoNetworkToken("Sigma Reserve", "https://sigmausd.io/", "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", imageString, hashData, networkType, this);
                break;
            case "sigusd":
                ergoToken = new ErgoNetworkToken("Sigma USD", "https://sigmausd.io/", "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", imageString, hashData, networkType, this);
                break;
            case "spf":
                ergoToken = new ErgoNetworkToken("Spectrum Finanace", "https://spectrum.fi/", "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d", imageString, hashData, networkType, this);
                break;
            case "terahertz":
                ergoToken = new ErgoNetworkToken("swamp.audio", "https://www.thz.fm/", "02f31739e2e4937bb9afb552943753d1e3e9cdd1a5e5661949cb0cef93f907ea", imageString, hashData, networkType, this);
                break;
            case "walrus":
                ergoToken = new ErgoNetworkToken("Walrus Dao", "https://www.walrusdao.io/", "59ee24951ce668f0ed32bdb2e2e5731b6c36128748a3b23c28407c5f8ccbf0f6", imageString, hashData, networkType, this);
                break;
            case "woodennickels":
                ergoToken = new ErgoNetworkToken("Wooden Nickles", "https://brianrxm.com/comimg/cnsmovtv_perrymason_woodennickels_12.jpg", "4c8ac00a28b198219042af9c03937eecb422b34490d55537366dc9245e85d4e1", imageString, hashData, networkType, this);
                break;
        }

        return ergoToken;
    }

    public void save(SecretKey appKey, JsonObject listJson, NetworkType networkType) {

        String tokenString = listJson.toString();

        if (networkType == NetworkType.TESTNET) {
            if (m_testnetDataFile != null) {
                try {
                    Files.writeString(m_testnetDataFile.toPath(), tokenString);
                } catch (IOException e) {
                    try {
                        Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
                }
            }
        } else {
            try {

                SecureRandom secureRandom = SecureRandom.getInstanceStrong();
                byte[] iV = new byte[12];
                secureRandom.nextBytes(iV);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

                cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

                byte[] encryptedData = cipher.doFinal(tokenString.getBytes());

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

}
