package com.netnotes;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import javax.crypto.BadPaddingException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.satergo.extra.AESEncryption;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TokensList extends Network {

    private File logFile = new File("tokensList-log.txt");

    private ArrayList<ErgoNetworkToken> m_networkTokenList = new ArrayList<>();
    private VBox m_buttonGrid = null;
    private SimpleDoubleProperty m_sceneWidth = new SimpleDoubleProperty(600);
    private SimpleDoubleProperty m_sceneHeight = new SimpleDoubleProperty(630);
    private NetworkType m_networkType;
    private ErgoTokens m_ergoTokens;

    public TokensList(SecretKey secretKey, NetworkType networkType, ErgoTokens ergoTokens) {
        super(null, "Ergo Tokens - List (" + networkType.toString() + ")", "TOKENS_LIST", ergoTokens);
        m_networkType = networkType;
        m_ergoTokens = ergoTokens;

        openFile(secretKey);
    }

    public TokensList(ArrayList<ErgoNetworkToken> networkTokenList, NetworkType networkType, ErgoTokens ergoTokens) {
        super(null, "Ergo Tokens - List (" + networkType.toString() + ")", "TOKENS_LIST", ergoTokens);
        m_networkType = networkType;
        m_ergoTokens = ergoTokens;
        for (ErgoNetworkToken networkToken : networkTokenList) {

            addToken(networkToken, false);

        }
    }

    /* public SimpleObjectProperty<ErgoNetworkToken> getTokenProperty() {
        return m_ergoNetworkToken;
    }*/
    public void openFile(SecretKey secretKey) {
        NetworkType networkType = m_networkType;
        String fileString = m_ergoTokens.getFile(networkType);

        File dataFile = new File(fileString);
        if (dataFile.isFile()) {
            if (networkType == NetworkType.MAINNET) {

                readFile(secretKey, dataFile.toPath());

            } else {
                openTestnetFile(dataFile.toPath());
            }
        }

    }

    @Override
    public ArrayList<NoteInterface> getTunnelNoteInterfaces() {
        ArrayList<NoteInterface> tunnelList = new ArrayList<>();
        for (ErgoNetworkToken token : m_networkTokenList) {
            tunnelList.add(token);
        }
        return tunnelList;
    }

    public void closeAll() {
        for (int i = 0; i < m_networkTokenList.size(); i++) {
            ErgoNetworkToken networkToken = m_networkTokenList.get(i);
            networkToken.close();
        }
    }

    public void setNetworkType(SecretKey secretKey, NetworkType networkType) {

        closeAll();
        m_networkTokenList.clear();
        m_networkType = networkType;
        setName("Ergo Tokens - List (" + networkType.toString() + ")");
        openFile(secretKey);
        updateGrid();

    }

    public void openTestnetFile(Path filePath) {
        m_networkTokenList.clear();

        if (filePath != null) {
            try {
                JsonElement jsonElement = new JsonParser().parse(Files.readString(filePath));

                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject(), NetworkType.TESTNET);
                }
            } catch (JsonParseException | IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nInvalid testnet file: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }
        }
    }

    public VBox getButtonGrid() {
        if (m_buttonGrid == null) {
            m_buttonGrid = new VBox();
        }

        updateGrid();

        return m_buttonGrid;
    }

    public void updateGrid() {
        if (m_buttonGrid != null) {
            int numCells = m_networkTokenList.size();

            m_buttonGrid.getChildren().clear();
            // VBox.setVgrow(m_buttonGrid, Priority.ALWAYS);

            for (int i = 0; i < numCells; i++) {
                ErgoNetworkToken networkToken = m_networkTokenList.get(i);

                IconButton rowButton = networkToken.getButton();

                m_buttonGrid.getChildren().add(rowButton);
                rowButton.prefWidthProperty().bind(m_buttonGrid.widthProperty());
            }
        }
    }

    public ErgoNetworkToken getErgoToken(String tokenid) {

        for (int i = 0; i < m_networkTokenList.size(); i++) {
            ErgoNetworkToken networkToken = m_networkTokenList.get(i);
            if (networkToken.getNetworkId().equals(tokenid)) {
                return networkToken;
            }
        }
        return null;
    }

    public ErgoNetworkToken getTokenByName(String name) {
        for (int i = 0; i < m_networkTokenList.size(); i++) {
            ErgoNetworkToken networkToken = m_networkTokenList.get(i);
            if (networkToken.getName().equals(name)) {
                return networkToken;
            }
        }
        return null;
    }

    public JsonObject getTokensStageObject() {
        JsonObject tokenStageObject = new JsonObject();
        tokenStageObject.addProperty("subject", "GET_ERGO_TOKENS_STAGE");
        return tokenStageObject;
    }

    public void addToken(ErgoNetworkToken networkToken) {
        addToken(networkToken, true);
    }

    public void addToken(ErgoNetworkToken networkToken, boolean update) {

        if (networkToken != null) {
            if (getErgoToken(networkToken.getNetworkId()) == null) {
                m_networkTokenList.add(networkToken);
                networkToken.addUpdateListener((obs, old, newVal) -> {
                    try {
                        Files.writeString(logFile.toPath(), networkToken.getName() + " updated: " + (newVal != null ? newVal.toString() : "null"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    getLastUpdated().set(LocalDateTime.now());
                });

                networkToken.addCmdListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        JsonElement subjectElement = newVal.get("subject");
                        String subject = subjectElement != null ? subjectElement.getAsString() : null;

                        if (subject != null) {
                            switch (subject) {
                                case "EDIT":
                                    getParentInterface().sendNote(getTokensStageObject(), success -> {
                                        Object sourceObject = success.getSource().getValue();

                                        if (sourceObject != null && sourceObject instanceof Stage) {
                                            closeAll();
                                            Stage sourceStage = (Stage) sourceObject;
                                            Scene sourceScene = sourceStage.getScene();
                                            NetworkType networkType = networkToken.getNetworkType();
                                            ErgoNetworkToken token = networkToken;

                                            sourceStage.setScene(getExistingTokenScene(token, networkType, sourceStage, sourceScene));
                                            Rectangle rect = getNetworksData().getMaximumWindowBounds();

                                            ResizeHelper.addResizeListener(sourceStage, 500, 615, rect.getWidth(), rect.getHeight());
                                        }
                                    }, failed -> {
                                    });
                                    break;
                            }
                        }
                    }
                });
            }
            if (update) {
                getLastUpdated().set(LocalDateTime.now());
            }
        }

    }

    public void removeToken(String networkId, boolean update) {
        if (networkId != null) {
            ErgoNetworkToken networkToken = getErgoToken(networkId);
            if (networkToken != null) {

                networkToken.removeUpdateListener();
                networkToken.removeCmdListener();
                networkToken.close();

                m_networkTokenList.remove(networkToken);
                if (update) {
                    updateGrid();
                    getLastUpdated().set(LocalDateTime.now());
                }
            }
        }
    }

    public void removeToken(String networkId) {
        if (networkId != null) {
            ErgoNetworkToken networkToken = getErgoToken(networkId);
            if (networkToken != null) {

                networkToken.removeUpdateListener();
                networkToken.removeCmdListener();
                networkToken.close();

                m_networkTokenList.remove(networkToken);
                updateGrid();
                getLastUpdated().set(LocalDateTime.now());
            }
        }
    }

    private void readFile(SecretKey appKey, Path filePath) {

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(filePath);

            byte[] iv = new byte[]{
                fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
            };

            ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

            try {
                JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, encryptedData)));
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject(), NetworkType.MAINNET);
                }
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                Alert a = new Alert(AlertType.NONE, "Decryption error:\n\n" + e.toString(), ButtonType.CLOSE);
                Platform.runLater(() -> a.show());
            }

        } catch (IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

    public void importJson(Stage callingStage, File file) {
        boolean updated = false;
        if (file != null && file.isFile()) {
            String contentType = null;
            try {
                contentType = Files.probeContentType(file.toPath());

            } catch (IOException e) {

            }

            if (contentType != null && (contentType.equals("application/json") || contentType.substring(0, 4).equals("text"))) {

                JsonObject fileJson = null;
                try {
                    String fileString = Files.readString(file.toPath());
                    JsonElement jsonElement = new JsonParser().parse(fileString);
                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        fileJson = jsonElement.getAsJsonObject();
                    }
                } catch (JsonParseException | IOException e) {
                    Alert noFile = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                    noFile.setHeaderText("Load Error");
                    noFile.initOwner(callingStage);
                    noFile.setTitle("Import JSON - Load Error");
                    noFile.setGraphic(IconButton.getIconView(new Image("/assets/load-30.png"), 30));
                    noFile.show();
                }
                if (fileJson != null) {
                    JsonElement networkTypeElement = fileJson.get("networkType");
                    JsonElement dataElement = fileJson.get("data");
                    NetworkType networkType = null;

                    if (networkTypeElement != null && networkTypeElement.isJsonPrimitive()) {
                        String networkTypeString = networkTypeElement.getAsString();
                        networkType = networkTypeString.equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : networkTypeString.equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : null;

                    }

                    if (networkType == null) {
                        Alert a = new Alert(AlertType.NONE, "Network Type is not specified.", ButtonType.OK);
                        a.initOwner(callingStage);
                        a.setHeaderText("Network Type");
                        a.setGraphic(IconButton.getIconView(ErgoNetwork.getAppIcon(), 40));
                        a.show();
                        return;
                    } else {
                        if (networkType != m_networkType) {
                            Alert a = new Alert(AlertType.NONE, "JSON network type is: " + networkType.toString() + ". Import into " + m_networkType.toString() + " canceled.", ButtonType.OK);
                            a.initOwner(callingStage);
                            a.setHeaderText("Network Type Mismatch");
                            a.setGraphic(IconButton.getIconView(ErgoNetwork.getAppIcon(), 40));
                            a.showAndWait();
                            return;
                        }
                    }

                    if (dataElement != null && dataElement.isJsonArray()) {
                        JsonArray dataArray = dataElement.getAsJsonArray();

                        for (JsonElement tokenObjectElement : dataArray) {
                            if (tokenObjectElement.isJsonObject()) {
                                JsonObject tokenJson = tokenObjectElement.getAsJsonObject();

                                JsonElement nameElement = tokenJson.get("name");
                                JsonElement tokenIdElement = tokenJson.get("tokenId");

                                if (nameElement != null && tokenIdElement != null) {
                                    String tokenId = tokenIdElement.getAsString();
                                    String name = nameElement.getAsString();
                                    ErgoNetworkToken oldToken = getErgoToken(tokenId);
                                    if (oldToken == null) {
                                        ErgoNetworkToken nameToken = getTokenByName(name);
                                        if (nameToken == null) {
                                            updated = true;
                                            addToken(new ErgoNetworkToken(name, tokenId, networkType, tokenJson, getParentInterface()), false);
                                        } else {
                                            Alert nameAlert = new Alert(AlertType.NONE, "Token:\n\n'" + tokenJson.toString() + "'\n\nName is used by another tokenId. Token will not be loaded.", ButtonType.OK);
                                            nameAlert.setHeaderText("Token Conflict");
                                            nameAlert.setTitle("Import JSON - Token Conflict");
                                            nameAlert.initOwner(callingStage);
                                            nameAlert.showAndWait();
                                        }
                                    } else {
                                        ErgoNetworkToken newToken = new ErgoNetworkToken(name, tokenId, networkType, tokenJson, getParentInterface());

                                        Alert nameAlert = new Alert(AlertType.NONE, "Existing Token:\n\n'" + oldToken.getName() + "' exists, overwrite token with '" + newToken.getName() + "'?", ButtonType.YES, ButtonType.NO);
                                        nameAlert.setHeaderText("Resolve Conflict");
                                        nameAlert.initOwner(callingStage);
                                        nameAlert.setTitle("Import JSON - Resolve Conflict");
                                        Optional<ButtonType> result = nameAlert.showAndWait();

                                        if (result.isPresent() && result.get() == ButtonType.YES) {
                                            removeToken(oldToken.getNetworkId(), false);

                                            addToken(newToken, false);

                                            updated = true;
                                        }
                                    }
                                } else {
                                    Alert noFile = new Alert(AlertType.NONE, "Token:\n" + tokenJson.toString() + "\n\nMissing name and/or tokenId properties and cannote be loaded.\n\nContinue?", ButtonType.YES, ButtonType.NO);
                                    noFile.setHeaderText("Encoding Error");
                                    noFile.initOwner(callingStage);
                                    noFile.setTitle("Import JSON - Encoding Error");
                                    Optional<ButtonType> result = noFile.showAndWait();

                                    if (result.isPresent() && result.get() == ButtonType.NO) {
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }
            } else {
                Alert tAlert = new Alert(AlertType.NONE, "File content type: " + contentType + " not supported.\n\nContent type: text/plain or application/json required.", ButtonType.OK);
                tAlert.setHeaderText("Content Type Mismatch");
                tAlert.initOwner(callingStage);
                tAlert.setTitle("Import JSON - Content Type Mismatch");
                tAlert.setGraphic(IconButton.getIconView(new Image("/assets/load-30.png"), 30));
                tAlert.show();

            }
        }
        if (updated) {
            updateGrid();
            getLastUpdated().set(LocalDateTime.now());
        }

    }

    private void openJson(JsonObject json, NetworkType networkType) {
        m_networkTokenList.clear();

        try {
            Files.writeString(logFile.toPath(), "\nopening json:", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        JsonElement networkTypeElement = json.get("networkType");

        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray() && networkTypeElement != null) {

            JsonArray dataArray = dataElement.getAsJsonArray();

            //  if (m_ergoTokens.getNetworkType().toString().equals(networkType)) {
            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement tokenIdElement = objJson.get("tokenId");

                    if (nameElement != null && nameElement.isJsonPrimitive() && tokenIdElement != null && tokenIdElement.isJsonPrimitive()) {
                        try {
                            Files.writeString(logFile.toPath(), "\nadding token:" + nameElement.getAsString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }
                        addToken(new ErgoNetworkToken(nameElement.getAsString(), tokenIdElement.getAsString(), networkType, objJson, getParentInterface()), false);
                    }

                }
            }
            //   }
        }
    }

    public Scene getExistingTokenScene(ErgoNetworkToken token, NetworkType networkType, Stage parentStage, Scene parentScene) {
        String oldStageName = parentStage.getTitle();
        Button maximizeBtn = new Button();
        Button closeBtn = new Button();
        HBox titleBox = App.createTopBar(getParentInterface().getButton().getIcon(), maximizeBtn, closeBtn, parentStage);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        String title = getParentInterface().getName() + ": Token Editor " + (networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)");
        parentStage.setTitle(title);
        //String type = "Existing token";

        if (token == null) {
            token = new ErgoNetworkToken("", "", "", "", null, networkType, getParentInterface());
        }
        try {
            Files.writeString(logFile.toPath(), "\nSHOWING", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        closeBtn.setOnAction(close -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);

            parentStage.close();

        });

        File tokenImageFile = token != null && token.getImageString() != null && !token.getImageString().equals("") && new File(token.getImageString()).isFile() ? new File(token.getImageString()) : null;
        ImageView imageView = IconButton.getIconView(tokenImageFile == null ? getParentInterface().getButton().getIcon() : new Image(tokenImageFile.toString()), 135);

        Button imageBtn = new Button(token.getName().equals("") ? "New Token" : token.getName());
        imageBtn.setContentDisplay(ContentDisplay.TOP);
        imageBtn.setGraphicTextGap(20);
        imageBtn.setFont(App.mainFont);
        imageBtn.prefHeight(135);
        imageBtn.prefWidth(135);
        imageBtn.setId("menuBtn");

        imageBtn.setGraphic(imageView);

        Tooltip explorerTip = new Tooltip();
        explorerTip.setShowDelay(new javafx.util.Duration(100));
        explorerTip.setFont(App.txtFont);

        MenuButton explorerBtn = new MenuButton();

        explorerBtn.setPadding(new Insets(2, 0, 0, 0));
        explorerBtn.setTooltip(explorerTip);

        HBox rightSideMenu = new HBox(explorerBtn);
        rightSideMenu.setId("rightSideMenuBar");
        rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Tooltip backTip = new Tooltip("Back");
        backTip.setShowDelay(new javafx.util.Duration(100));
        backTip.setFont(App.txtFont);

        Button backButton = new Button();
        backButton.setGraphic(IconButton.getIconView(new Image("/assets/return-back-up-30.png"), 30));
        backButton.setId("menuBtn");
        backButton.setTooltip(backTip);
        backButton.setOnAction(e -> {
            parentStage.setScene(parentScene);
            parentStage.setTitle(oldStageName);
            // ResizeHelper.addResizeListener(parentStage, WalletData.MIN_WIDTH, WalletData.MIN_HEIGHT, m_walletData.getMaxWidth(), m_walletData.getMaxHeight());
        });

        HBox menuBar = new HBox(backButton, spacer, rightSideMenu);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 0, 1, 5));

        HBox imageBox = new HBox(imageBtn);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(20, 0, 10, 0));

        Text promptText = new Text("Token");
        promptText.setFont(App.txtFont);
        promptText.setFill(Color.WHITE);

        HBox promptBox = new HBox(promptText);
        promptBox.prefHeight(40);
        promptBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(promptBox, Priority.ALWAYS);
        promptBox.setPadding(new Insets(10, 15, 10, 15));
        promptBox.setId("headingBox");

        Button nameSpacerBtn = new Button();
        nameSpacerBtn.setDisable(true);
        nameSpacerBtn.setId("transparentColor");
        nameSpacerBtn.setPrefWidth(5);
        nameSpacerBtn.setPrefHeight(60);;

        Button tokenIdSpacerBtn = new Button();
        tokenIdSpacerBtn.setDisable(true);
        tokenIdSpacerBtn.setId("transparentColor");
        tokenIdSpacerBtn.setPrefHeight(60);;
        tokenIdSpacerBtn.setPrefWidth(5);

        Button urlSpacerBtn = new Button();
        urlSpacerBtn.setDisable(true);
        urlSpacerBtn.setId("transparentColor");
        urlSpacerBtn.setPrefWidth(5);
        urlSpacerBtn.setPrefHeight(60);;

        Button imgFileSpacerBtn = new Button();
        imgFileSpacerBtn.setDisable(true);
        imgFileSpacerBtn.setId("transparentColor");
        imgFileSpacerBtn.setPrefWidth(5);
        imgFileSpacerBtn.setPrefHeight(60);;

        TextField tokenIdField = new TextField(token.getTokenId());
        tokenIdField.setPadding(new Insets(9, 0, 10, 0));
        tokenIdField.setFont(App.txtFont);
        tokenIdField.setId("formField");
        HBox.setHgrow(tokenIdField, Priority.ALWAYS);

        Button tokenIdBtn = new Button(tokenIdField.getText().equals("") ? "Enter Token Id" : tokenIdField.getText());
        tokenIdBtn.setId("rowBtn");
        tokenIdBtn.setFont(App.txtFont);
        tokenIdBtn.setContentDisplay(ContentDisplay.LEFT);
        tokenIdBtn.setAlignment(Pos.CENTER_LEFT);
        tokenIdBtn.setPadding(new Insets(10, 10, 10, 10));

        TextField nameField = new TextField(token.getName());
        nameField.setPadding(new Insets(9, 0, 10, 0));
        nameField.setFont(App.txtFont);
        nameField.setId("formField");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        if (token != null) {
            File imgFile = new File(token.getImageString());
            if (imgFile != null && imgFile.isFile()) {
                //String imgString = imgFile.getAbsolutePath();
                Image img = null;
                try {
                    BufferedImage imgBuf = ImageIO.read(imgFile);
                    img = imgBuf != null ? SwingFXUtils.toFXImage(imgBuf, null) : null;
                } catch (IOException e1) {

                }
                imageBtn.setGraphic(IconButton.getIconView(img, 135));
            }
        }

        Text nameCaret = new Text("Name       ");
        nameCaret.setFont(App.txtFont);
        nameCaret.setFill(Color.WHITE);

        Button nameButton = new Button(nameField.getText().equals("") ? "Enter name" : nameField.getText());

        nameButton.setId("rowBtn");
        nameButton.setContentDisplay(ContentDisplay.LEFT);
        nameButton.setAlignment(Pos.CENTER_LEFT);
        nameButton.setPadding(new Insets(10, 10, 10, 10));

        nameButton.setFont(App.txtFont);

        HBox nameBox = new HBox(nameCaret, nameSpacerBtn, nameButton);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setPadding(new Insets(5, 0, 5, 0));

        Button nameEnterBtn = new Button("[ Enter ]");
        nameEnterBtn.setFont(App.txtFont);
        nameEnterBtn.setId("toolBtn");
        nameEnterBtn.setPadding(new Insets(5, 5, 5, 5));
        nameEnterBtn.prefWidth(75);

        nameButton.setOnAction(event -> {
            nameBox.getChildren().remove(nameButton);
            nameBox.getChildren().add(nameField);
            Platform.runLater(() -> nameField.requestFocus());

        });

        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

            } else {
                nameBox.getChildren().remove(nameField);
                nameBox.getChildren().add(nameButton);
                nameSpacerBtn.setId("transparentColor");
                String text = nameField.getText();

                nameButton.setText(text.equals("") ? "Enter name" : text);
                imageBtn.setText(nameField.getText().equals("") ? "New Token" : nameField.getText());
                if (nameBox.getChildren().contains(nameEnterBtn)) {
                    nameBox.getChildren().remove(nameEnterBtn);
                }
            }
        });

        nameField.setOnKeyPressed(keyEvent -> {
            KeyCode keyCode = keyEvent.getCode();

            if (keyCode == KeyCode.ENTER) {
                nameBox.getChildren().remove(nameField);
                nameBox.getChildren().add(nameButton);
                nameSpacerBtn.setId("transparentColor");
                String text = nameField.getText();

                nameButton.setText(text.equals("") ? "Enter name" : text);
                imageBtn.setText(nameField.getText().equals("") ? "New Token" : nameField.getText());
                if (nameBox.getChildren().contains(nameEnterBtn)) {
                    nameBox.getChildren().remove(nameEnterBtn);
                }
            }
        });

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!nameBox.getChildren().contains(nameEnterBtn)) {
                nameBox.getChildren().add(nameEnterBtn);
            }
        });

        nameEnterBtn.setOnAction(action -> {
            nameBox.getChildren().remove(nameField);
            nameBox.getChildren().add(nameButton);
            nameSpacerBtn.setId("transparentColor");
            String text = nameField.getText();

            nameButton.setText(text.equals("") ? "Enter name" : text);
            imageBtn.setText(nameField.getText().equals("") ? "New Token" : nameField.getText());
            if (nameBox.getChildren().contains(nameEnterBtn)) {
                nameBox.getChildren().remove(nameEnterBtn);
            }
        });

        HBox.setHgrow(tokenIdField, Priority.ALWAYS);

        Text tokenIdCaret = new Text("Token Id   ");
        tokenIdCaret.setFont(App.txtFont);
        tokenIdCaret.setFill(Color.WHITE);

        Button tokenIdEnterBtn = new Button("[ Enter ]");
        tokenIdEnterBtn.setFont(App.txtFont);
        tokenIdEnterBtn.setId("toolBtn");
        tokenIdEnterBtn.setPadding(new Insets(5, 5, 5, 5));
        tokenIdEnterBtn.prefWidth(75);

        HBox tokenIdBox = new HBox(tokenIdCaret, tokenIdSpacerBtn, tokenIdBtn);
        HBox.setHgrow(tokenIdBox, Priority.ALWAYS);
        tokenIdBox.setAlignment(Pos.CENTER_LEFT);
        tokenIdBox.setPadding(new Insets(5, 0, 5, 0));

        // tokenIdBox.setPrefHeight(60);;
        tokenIdBtn.setOnAction(action -> {
            tokenIdBox.getChildren().remove(tokenIdBtn);
            tokenIdBox.getChildren().add(tokenIdField);
            Platform.runLater(() -> tokenIdField.requestFocus());

        });

        tokenIdField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

            } else {
                String tokenIdFieldText = tokenIdField.getText();
                tokenIdSpacerBtn.setId("transparentColor");
                if (tokenIdFieldText.equals("")) {
                    tokenIdBtn.setText("Enter token id");

                } else {

                    tokenIdBtn.setText(tokenIdFieldText);
                }
                if (tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                    tokenIdBox.getChildren().remove(tokenIdEnterBtn);
                }
                tokenIdBox.getChildren().remove(tokenIdField);
                tokenIdBox.getChildren().add(tokenIdBtn);
            }
        });

        tokenIdEnterBtn.setOnAction(action -> {
            String tokenIdFieldText = tokenIdField.getText();
            tokenIdSpacerBtn.setId("transparentColor");
            if (tokenIdFieldText.equals("")) {
                tokenIdBtn.setText("Enter token id");

            } else {

                tokenIdBtn.setText(tokenIdFieldText);
            }
            if (tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                tokenIdBox.getChildren().remove(tokenIdEnterBtn);
            }
            tokenIdBox.getChildren().remove(tokenIdField);
            tokenIdBox.getChildren().add(tokenIdBtn);
        });

        tokenIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                tokenIdBox.getChildren().add(tokenIdEnterBtn);
            }
        });

        tokenIdField.setOnKeyPressed(keyEvent -> {
            KeyCode keyCode = keyEvent.getCode();
            if (keyCode == KeyCode.ENTER) {
                String tokenIdFieldText = tokenIdField.getText();
                tokenIdSpacerBtn.setId("transparentColor");
                if (tokenIdFieldText.equals("")) {
                    tokenIdBtn.setText("Enter token id");

                } else {

                    tokenIdBtn.setText(tokenIdFieldText);
                }
                if (tokenIdBox.getChildren().contains(tokenIdEnterBtn)) {
                    tokenIdBox.getChildren().remove(tokenIdEnterBtn);
                }
                tokenIdBox.getChildren().remove(tokenIdField);
                tokenIdBox.getChildren().add(tokenIdBtn);
            }
        });

        TextField urlLinkField = new TextField(token.getUrlString());
        urlLinkField.setPadding(new Insets(9, 0, 10, 0));
        urlLinkField.setFont(App.txtFont);
        urlLinkField.setId("formField");
        HBox.setHgrow(urlLinkField, Priority.ALWAYS);

        Text urlLinkCaret = new Text("URL        ");
        urlLinkCaret.setFont(App.txtFont);
        urlLinkCaret.setFill(Color.WHITE);

        Button urlLinkBtn = new Button(urlLinkField.getText().equals("") ? "Enter URL" : urlLinkField.getText());
        urlLinkBtn.setId("rowBtn");
        urlLinkBtn.setFont(App.txtFont);
        urlLinkBtn.setContentDisplay(ContentDisplay.LEFT);
        urlLinkBtn.setAlignment(Pos.CENTER_LEFT);
        urlLinkBtn.setPadding(new Insets(10, 10, 10, 10));

        Button urlEnterBtn = new Button("[ Enter ]");
        urlEnterBtn.setFont(App.txtFont);
        urlEnterBtn.setId("toolBtn");
        urlEnterBtn.setPadding(new Insets(5, 5, 5, 5));
        urlEnterBtn.prefWidth(75);

        HBox urlBox = new HBox(urlLinkCaret, urlSpacerBtn, urlLinkBtn);

        HBox.setHgrow(urlBox, Priority.ALWAYS);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        urlBox.setPadding(new Insets(5, 0, 5, 0));
        urlBox.setPrefHeight(60);;

        urlLinkBtn.setOnAction(action -> {
            urlBox.getChildren().remove(urlLinkBtn);
            urlBox.getChildren().add(urlLinkField);
            Platform.runLater(() -> urlLinkField.requestFocus());

        });

        urlLinkField.textProperty().addListener((obs, oldVal, newVal) -> {

            if (!urlBox.getChildren().contains(urlEnterBtn)) {
                urlBox.getChildren().add(urlEnterBtn);
            }

        });

        urlLinkField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {

            } else {
                String text = urlLinkField.getText();

                urlLinkBtn.setText(text.equals("") ? "Enter url" : text);

                urlBox.getChildren().remove(urlLinkField);
                urlBox.getChildren().add(urlLinkBtn);
                if (urlBox.getChildren().contains(urlEnterBtn)) {
                    urlBox.getChildren().remove(urlEnterBtn);
                }

            }
        });

        urlLinkField.setOnKeyPressed((keyEvent) -> {

            KeyCode keyCode = keyEvent.getCode();
            if (keyCode == KeyCode.ENTER) {
                String text = urlLinkField.getText();

                urlLinkBtn.setText(text.equals("") ? "Enter url" : text);

                urlBox.getChildren().remove(urlLinkField);

                if (urlBox.getChildren().contains(urlEnterBtn)) {
                    urlBox.getChildren().remove(urlEnterBtn);
                }
                urlBox.getChildren().add(urlLinkBtn);
            }
        });

        urlEnterBtn.setOnAction((keyEvent) -> {

            String text = urlLinkField.getText();

            urlLinkBtn.setText(text.equals("") ? "Enter url" : text);

            urlBox.getChildren().remove(urlLinkField);

            if (urlBox.getChildren().contains(urlEnterBtn)) {
                urlBox.getChildren().remove(urlEnterBtn);
            }
            urlBox.getChildren().add(urlLinkBtn);

        });

        Text imageFileCaret = new Text("Image File ");
        imageFileCaret.setFont(App.txtFont);
        imageFileCaret.setFill(Color.WHITE);

        String imageFileString = token.getImageString() != null ? token.getImageString() : "";

        Button imageFileBtn = new Button(imageBtn.getText().equals("") ? "Select an image" : imageFileString);
        imageFileBtn.setId("rowBtn");
        imageFileBtn.setFont(App.txtFont);

        imageFileBtn.setContentDisplay(ContentDisplay.LEFT);
        imageFileBtn.setAlignment(Pos.CENTER_LEFT);
        imageFileBtn.setPadding(new Insets(10, 10, 10, 10));

        HBox imageFileBox = new HBox(imageFileCaret, imgFileSpacerBtn, imageFileBtn);
        HBox.setHgrow(imageFileBox, Priority.ALWAYS);
        imageFileBox.setAlignment(Pos.CENTER_LEFT);
        imageFileBox.setPadding(new Insets(5, 0, 5, 0));
        imageFileBox.setPrefHeight(60);;

        VBox scrollPaneVBox = new VBox(nameBox, tokenIdBox, urlBox, imageFileBox);
        scrollPaneVBox.setPadding(new Insets(0, 20, 20, 40));
        HBox.setHgrow(scrollPaneVBox, Priority.ALWAYS);
        scrollPaneVBox.setId("bodyBox");

        Button spacerBoxBtn = new Button();
        spacerBoxBtn.setId("transparentColor");
        spacerBoxBtn.setDisable(true);
        spacerBoxBtn.setPrefWidth(40);

        HBox spacerBox = new HBox(spacerBoxBtn);

        VBox bodyVBox = new VBox(menuBar, imageBox, promptBox, scrollPaneVBox);

        Button okButton = new Button("Ok");
        okButton.setFont(App.txtFont);
        okButton.setPrefWidth(100);
        okButton.setPrefHeight(30);

        okButton.setOnAction(e -> {

            if (nameField.getText().length() < 3) {
                Alert nameAlert = new Alert(AlertType.NONE, "Name must be at least 3 characters long.", ButtonType.OK);
                nameAlert.initOwner(parentStage);
                nameAlert.setGraphic(IconButton.getIconView(getParentInterface().getButton().getIcon(), 75));
                nameAlert.show();
            } else {
                if (tokenIdField.getText().length() < 3) {
                    Alert tokenAlert = new Alert(AlertType.NONE, "Token Id must be at least 3 characters long.", ButtonType.OK);
                    tokenAlert.initOwner(parentStage);
                    tokenAlert.setGraphic(IconButton.getIconView(getParentInterface().getButton().getIcon(), 75));
                    tokenAlert.show();
                } else {

                    byte[] bytes = null;
                    try {
                        bytes = Utils.digestFile(new File(imageFileBtn.getText()));
                    } catch (Exception e1) {

                    }
                    HashData hashData = bytes != null ? new HashData(bytes) : null;
                    ErgoNetworkToken newToken = new ErgoNetworkToken(nameField.getText(), urlLinkField.getText(), tokenIdField.getText(), imageFileBtn.getText(), hashData, networkType, getParentInterface());
                    ErgoNetworkToken oldToken = getErgoToken(newToken.getTokenId());

                    if (oldToken != null) {
                        Alert tokenAlert = new Alert(AlertType.NONE, "Warning\n\n\nA token named '" + oldToken.getName() + "' exists with this token Id. Would you like to overwrite it?\n\n(Token Id:" + oldToken.getTokenId() + ")", ButtonType.NO, ButtonType.YES);
                        tokenAlert.initOwner(parentStage);

                        tokenAlert.setTitle("Replace or skip");
                        Optional<ButtonType> result = tokenAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.YES) {
                            removeToken(oldToken.getNetworkId(), false);

                            addToken(newToken, false);
                            updateGrid();
                            getLastUpdated().set(LocalDateTime.now());
                            parentStage.setScene(parentScene);
                        }

                    } else {
                        if (getTokenByName(newToken.getName()) != null) {
                            Alert tokenAlert = new Alert(AlertType.NONE, "Token name already exists in Ergo Tokens. \n\nPlease enter a new name.", ButtonType.OK);
                            tokenAlert.initOwner(parentStage);
                            tokenAlert.setTitle("Cancel");
                            tokenAlert.setGraphic(IconButton.getIconView(getParentInterface().getButton().getIcon(), 75));
                        } else {
                            m_networkTokenList.add(newToken);
                            updateGrid();
                            getLastUpdated().set(LocalDateTime.now());
                            parentStage.setScene(parentScene);
                        }
                    }

                }
            }
        });

        HBox okButtonBox = new HBox(okButton);
        HBox.setHgrow(okButtonBox, Priority.ALWAYS);
        okButtonBox.setAlignment(Pos.CENTER_RIGHT);
        okButtonBox.setPadding(new Insets(10, 10, 10, 10));
        okButtonBox.setPrefHeight(60);;

        VBox footerVBox = new VBox(okButtonBox);
        HBox.setHgrow(footerVBox, Priority.ALWAYS);

        HBox footerHBox = new HBox(footerVBox);
        footerHBox.setPadding(new Insets(10, 10, 10, 5));
        footerHBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footerHBox, Priority.ALWAYS);

        HBox bodyPaddingBox = new HBox(bodyVBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));
        HBox.setHgrow(bodyPaddingBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, bodyPaddingBox, footerHBox, spacerBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);
        HBox.setHgrow(layoutVBox, Priority.ALWAYS);

        Scene tokenEditorScene = new Scene(layoutVBox, m_sceneWidth.get(), m_sceneHeight.get());
        tokenEditorScene.getStylesheets().add("/css/startWindow.css");

        menuBar.prefWidthProperty().bind(tokenEditorScene.widthProperty());
        nameButton.prefWidthProperty().bind(nameBox.widthProperty().subtract(nameCaret.layoutBoundsProperty().getValue().getWidth()));
        imageFileBtn.prefWidthProperty().bind(imageFileBox.widthProperty().subtract(imageFileCaret.layoutBoundsProperty().getValue().getWidth()));
        tokenIdBtn.prefWidthProperty().bind(tokenIdBox.widthProperty().subtract(tokenIdCaret.layoutBoundsProperty().getValue().getWidth()));
        urlLinkBtn.prefWidthProperty().bind(urlBox.widthProperty().subtract(urlLinkCaret.layoutBoundsProperty().getValue().getWidth()));

        spacerBoxBtn.prefHeightProperty().bind(tokenEditorScene.heightProperty().subtract(titleBox.heightProperty()).subtract(footerHBox.heightProperty()).subtract(menuBar.heightProperty()).subtract(scrollPaneVBox.heightProperty()));

        getParentInterface().sendNote(Utils.getExplorerInterfaceIdObject(), success -> {
            WorkerStateEvent event = success;
            Object explorerIdObject = event.getSource().getValue();
            if (explorerIdObject != null && explorerIdObject instanceof String) {
                String explorerId = (String) explorerIdObject;
                NoteInterface explorerInterface = getParentInterface().getNetworksData().getNoteInterface(explorerId);
                if (explorerInterface == null) {
                    Platform.runLater(() -> {

                        explorerTip.setText("Explorer: Disabled");
                        explorerBtn.setGraphic(IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30));
                    });
                } else {
                    Platform.runLater(() -> {

                        explorerTip.setText(explorerInterface.getName() + ": Enabled");
                        explorerBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getParentInterface().getNetworksData(), explorerInterface.getNetworkId(), true).getIcon(), 30));
                    });
                }

            } else {
                Platform.runLater(() -> explorerBtn.setGraphic(IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30)));
            }
        }, onFailed -> {
        });

        EventHandler<ActionEvent> imageClickEvent = (event) -> {

            FileChooser imageChooser = new FileChooser();
            imageChooser.setTitle("Token Editor - Select Image file");
            imageChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image (*.jpg, *.png)", "*.png", "*.jpg", "*.jpeg"));

            File chosenFile = imageChooser.showOpenDialog(parentStage);

            if (chosenFile != null && chosenFile.isFile()) {

                String mimeTypeString = null;
                try {
                    mimeTypeString = Files.probeContentType(chosenFile.toPath());
                    mimeTypeString = mimeTypeString.split("/")[0];

                } catch (IOException e) {

                }
                if (mimeTypeString != null && mimeTypeString.equals("image")) {
                    String fileString = chosenFile.getAbsolutePath();
                    imageFileBtn.setText(fileString);
                    imageBtn.setGraphic(IconButton.getIconView(new Image(fileString), 135));
                }
            }

        };

        imageBtn.setOnAction(imageClickEvent);

        imageFileBtn.setOnAction(imageClickEvent);

        return tokenEditorScene;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject tokensListJson = super.getJsonObject();
        tokensListJson.addProperty("networkType", m_networkType.toString());
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < m_networkTokenList.size(); i++) {
            ErgoNetworkToken ergoNetworkToken = m_networkTokenList.get(i);
            jsonArray.add(ergoNetworkToken.getJsonObject());

        }

        tokensListJson.add("data", jsonArray);

        return tokensListJson;
    }

}
