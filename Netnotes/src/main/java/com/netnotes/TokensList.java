package com.netnotes;

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

    private ArrayList<ErgoNetworkToken> m_noteInterfaceList = new ArrayList<>();
    private VBox m_buttonGrid = null;
    private SimpleDoubleProperty m_sceneWidth = new SimpleDoubleProperty(600);
    private SimpleDoubleProperty m_sceneHeight = new SimpleDoubleProperty(585);

    private SimpleObjectProperty<ErgoNetworkToken> m_ergoNetworkToken = new SimpleObjectProperty<ErgoNetworkToken>(null);

    public TokensList(NetworkType networkType, NoteInterface noteInterface) {
        super(null, "Ergo Tokens - List (" + networkType.toString() + ")", "TOKENS_LIST", noteInterface);

        try {
            Files.writeString(logFile.toPath(), "\n" + getName(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        getFile(networkType);

    }

    public SimpleObjectProperty<ErgoNetworkToken> getTokenProperty() {
        return m_ergoNetworkToken;
    }

    public void getFile(NetworkType networkType) {
        if (getParentInterface().getNetworksData().getNoteInterface(NetworkID.ERGO_TOKENS) != null) {
            getParentInterface().getNetworksData().getNoteInterface(NetworkID.ERGO_TOKENS).sendNote(getDataFileLocation(networkType), onSuccess -> {
                WorkerStateEvent successEvent = onSuccess;

                Object sourceObject = successEvent.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof String) {

                    String fileString = (String) sourceObject;
                    File dataFile = new File(fileString);
                    if (dataFile.isFile()) {
                        if (networkType == NetworkType.MAINNET) {

                            readFile(getParentInterface().getNetworksData().getAppKey(), dataFile.toPath());

                        } else {
                            openTestnetFile(dataFile.toPath());
                        }
                    }
                }
                Platform.runLater(() -> updateGrid());
            }, onFailed -> {

            });

        }
    }

    @Override
    public ArrayList<NoteInterface> getTunnelNoteInterfaces() {
        ArrayList<NoteInterface> tunnelList = new ArrayList<>();
        for (ErgoNetworkToken token : m_noteInterfaceList) {
            tunnelList.add(token);
        }
        return tunnelList;
    }

    public JsonObject getDataFileLocation(NetworkType networkType) {
        JsonObject getTokensFileObject = new JsonObject();
        getTokensFileObject.addProperty("subject", "GET_DATAFILE_LOCATION");
        getTokensFileObject.addProperty("networkType", networkType.toString());
        return getTokensFileObject;
    }

    public JsonObject getShutdownObject() {
        JsonObject shutdownObject = new JsonObject();
        shutdownObject.addProperty("subject", "SHUTDOWN_NOW");
        shutdownObject.addProperty("caller", getParentInterface().getNetworkId());
        return shutdownObject;
    }

    public void closeAll() {
        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            noteInterface.sendNote(getShutdownObject(), null, null);
        }
    }

    public void setNetworkType(NetworkType networkType) {
        closeAll();
        m_noteInterfaceList.clear();

        getFile(networkType);

    }

    public void openTestnetFile(Path filePath) {
        m_noteInterfaceList.clear();

        if (filePath != null) {
            try {
                JsonElement jsonElement = new JsonParser().parse(Files.readString(filePath));

                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject());
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
            int numCells = m_noteInterfaceList.size();

            m_buttonGrid.getChildren().clear();
            // VBox.setVgrow(m_buttonGrid, Priority.ALWAYS);

            for (int i = 0; i < numCells; i++) {
                NoteInterface noteInterface = m_noteInterfaceList.get(i);

                IconButton rowButton = noteInterface.getButton();

                m_buttonGrid.getChildren().add(rowButton);
                rowButton.prefWidthProperty().bind(m_buttonGrid.widthProperty());
            }
        }
    }

    public ErgoNetworkToken getErgoToken(String tokenid) {

        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            ErgoNetworkToken noteInterface = m_noteInterfaceList.get(i);
            if (noteInterface.getNetworkId().equals(tokenid)) {
                return noteInterface;
            }
        }
        return null;
    }

    public void addToken(ErgoNetworkToken noteInterface) {

        if (noteInterface != null) {
            if (getErgoToken(noteInterface.getNetworkId()) == null) {
                m_noteInterfaceList.add(noteInterface);
                noteInterface.addUpdateListener((obs, old, newVal) -> {
                    try {
                        Files.writeString(logFile.toPath(), noteInterface.getName() + " updated: " + (newVal != null ? newVal.toString() : "null"));
                    } catch (IOException e) {

                    }
                    getLastUpdated().set(LocalDateTime.now());
                });
            }
        }

    }

    public void removeToken(String networkId) {
        if (networkId != null) {
            NoteInterface noteInterface = getErgoToken(networkId);
            if (noteInterface != null) {
                noteInterface.removeUpdateListener();
                m_noteInterfaceList.remove(noteInterface);

            }
        }
    }

    private void readFile(SecretKey appKey, Path filePath) {
        try {
            Files.writeString(logFile.toPath(), "\nReading file:" + filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

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
                    openJson(jsonElement.getAsJsonObject());
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

    private void openJson(JsonObject json) {
        m_noteInterfaceList.clear();

        try {
            Files.writeString(logFile.toPath(), "\nopening json:\n" + json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray()) {

            JsonArray dataArray = dataElement.getAsJsonArray();

            //  if (m_ergoTokens.getNetworkType().toString().equals(networkType)) {
            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement tokenIdElement = objJson.get("networkId");

                    if (nameElement != null && nameElement.isJsonPrimitive() && tokenIdElement != null && tokenIdElement.isJsonPrimitive()) {
                        addToken(new ErgoNetworkToken(nameElement.getAsString(), tokenIdElement.getAsString(), objJson, getParentInterface()));
                    }

                }
            }
            //   }
        }
    }

    public Scene getExistingTokenScene(NetworkType networkType, Stage parentStage, Scene parentScene) {
        String oldStageName = parentStage.getTitle();
        Button maximizeBtn = new Button();
        Button closeBtn = new Button();
        HBox titleBox = App.createTopBar(getParentInterface().getButton().getIcon(), maximizeBtn, closeBtn, parentStage);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        String title = getParentInterface().getName() + ": Token Editor " + (networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)");
        parentStage.setTitle(title);
        //String type = "Existing token";

        ErgoNetworkToken token = m_ergoNetworkToken.get();

        if (token == null) {
            token = new ErgoNetworkToken("", "", "", null, networkType, getParentInterface());
        }
        try {
            Files.writeString(logFile.toPath(), "\nSHOWING", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        closeBtn.setOnAction(close -> {
            parentStage.close();

        });

        //    m_ergoTokenStage.setTitle(getParentInterface().getName() + " - Token Editor - " + token != null ? token.toString() : "New Token");
        //    m_ergoTokenStage.titleProperty().bind(Bindings.concat(getParentInterface().getName(), " - Token Editor - ", m_ergoNetworkToken.asString()));
        File tokenImageFile = token != null && token.getImageFile() != null && token.getImageFile().isFile() ? token.getImageFile() : null;
        ImageView imageView = IconButton.getIconView(tokenImageFile == null ? getParentInterface().getButton().getIcon() : new Image(tokenImageFile.toString()), 135);

        Button imageBtn = new Button(token.getName());

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

        Text promptText = new Text("Existing Token");
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
            File imgFile = token.getImageFile();
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

        String imageFileString = token.getImageFile() != null ? token.getImageFile().toString() : "";

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

}
