package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoNetworkToken extends Network implements NoteInterface {

    private File logFile = new File("ErgoNetworkToken-log.txt");

    private HashData m_hashData = null;

    private NetworkType m_networkType = NetworkType.MAINNET;
    // private ErgoTokens m_ErgoTokens = null;
    private SimpleBooleanProperty m_explorerVerified = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<ErgoNetworkTokenData> m_ergoNetworkTokenData = new SimpleObjectProperty<ErgoNetworkTokenData>(null);

    private SimpleDoubleProperty m_sceneWidth = new SimpleDoubleProperty(450);
    private SimpleDoubleProperty m_sceneHeight = new SimpleDoubleProperty(575);
    private String m_urlString;
    private String m_imageString;
    private Stage m_ergoTokenStage = null;

    public ErgoNetworkToken(String name, String tokenId, NetworkType networkType, JsonObject jsonObject, NoteInterface noteInterface) {
        super(null, name, tokenId, noteInterface);

        JsonElement imageStringElement = jsonObject.get("imageString");
        JsonElement urlElement = jsonObject.get("url");
        JsonElement sceneWidthElement = jsonObject.get("sceneWidth");
        JsonElement sceneHeightElement = jsonObject.get("sceneHeight");
        JsonElement tokenDataElement = jsonObject.get("tokenData");
        JsonElement hashDataElement = jsonObject.get("hashData");

        if (sceneWidthElement != null) {
            m_sceneWidth.set(sceneWidthElement.getAsDouble());
        }

        if (sceneHeightElement != null) {
            m_sceneHeight.set(sceneHeightElement.getAsDouble());
        }

        if (hashDataElement != null && hashDataElement.isJsonObject()) {
            JsonObject json = hashDataElement.getAsJsonObject();
            JsonElement hashIdElement = json.get("id");
            JsonElement hashNameElement = json.get("name");
            JsonElement hashHashElement = json.get("hash");

            if (hashIdElement != null && hashNameElement != null && hashHashElement != null) {
                try {
                    m_hashData = new HashData(hashIdElement.getAsString(), hashNameElement.getAsString(), hashHashElement.getAsString());
                    try {
                        Files.writeString(logFile.toPath(), "\nhashData: " + m_hashData.getJsonObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                } catch (DecoderException e) {
                    m_hashData = null;
                }
            }
        }

        if (urlElement != null) {
            m_urlString = urlElement.getAsString();
        }

        if (imageStringElement != null && m_hashData != null) {
            m_imageString = imageStringElement.getAsString();
            checkAndloadImage(imageStringElement.getAsString(), m_hashData);

            //  m_imageFile = ;
        }

        m_networkType = networkType;

        setIconStyle(IconStyle.ROW);
        setGraphicTextGap(15);

        if (tokenDataElement != null && tokenDataElement.isJsonObject()) {

            m_ergoNetworkTokenData.set(new ErgoNetworkTokenData(tokenDataElement.getAsJsonObject()));

        }

    }

    public ErgoNetworkToken(String name, String url, String tokenId, String fileString, HashData hashData, NetworkType networkType, NoteInterface noteInterface) {
        super(null, name, tokenId, noteInterface);

        m_urlString = url;
        m_hashData = hashData;
        m_networkType = networkType;

        setIconStyle(IconStyle.ROW);

        if (fileString != null && fileString != "") {
            m_imageString = fileString;
            File imageFile = new File(m_imageString);
            if (imageFile.isFile()) {
                setIcon(new Image(m_imageString));
            }
        }

        setGraphicTextGap(15);
    }

    @Override
    public void open() {
        super.open();
        ErgoNetworkTokenData tokenData = m_ergoNetworkTokenData.get();
        if (tokenData == null) {
            updateTokenInfo();
        } else {
            if (m_ergoNetworkTokenData.get().timeStamp() == 0) {
                updateTokenInfo();
            }
        }
        showTokenStage();
    }

    private void checkAndloadImage(String imageString, HashData hashData) {
        File checkFile = new File(imageString);

        byte[] bytes = null;
        try {
            bytes = Utils.digestFile(checkFile);
            String hashString = Hex.toHexString(bytes);
            try {
                Files.writeString(logFile.toPath(), "\nhashString: " + hashString + " hashDataString: " + hashData.getHashString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            if (hashString.equals(hashData.getHashString())) {
                setImageByFile(checkFile);
            }
        } catch (Exception e) {
            try {
                Files.writeString(logFile.toPath(), "\n" + e, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e2) {

            }
        }

        if (bytes == null) {
            setIcon(new Image("/assets/unknown-unit.png"));
        }

    }

    public void setImageByFile(File file) {
        if (file != null && file.isFile()) {
            String contentType = null;
            try {
                contentType = Files.probeContentType(file.toPath());
                contentType = contentType.split("/")[0];
                Files.writeString(logFile.toPath(), "\ncontentType: " + contentType, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }

            if (contentType != null && contentType.equals("image")) {
                Image image = new Image(file.getAbsolutePath());
                Platform.runLater(() -> setIcon(image));
            }

        }
    }

    public String getImageString() {
        return m_imageString;
    }

    public SimpleBooleanProperty explorerVerifiedProperty() {
        return m_explorerVerified;
    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public void updateTokenInfo() {

        getParentInterface().sendNote(Utils.getExplorerInterfaceIdObject(), success -> {
            WorkerStateEvent event = success;
            Object explorerIdObject = event.getSource().getValue();
            if (explorerIdObject != null && explorerIdObject instanceof String) {
                String explorerId = (String) explorerIdObject;
                NoteInterface explorerInterface = getParentInterface().getNetworksData().getNoteInterface(explorerId);
                if (explorerInterface != null) {
                    if (explorerInterface != null) {

                        JsonObject note = ErgoExplorer.getTokenInfoObject(getTokenId(), m_networkType);

                        explorerInterface.sendNote(note, succeededEvent -> {
                            WorkerStateEvent workerEvent = succeededEvent;
                            Object sourceObject = workerEvent.getSource().getValue();

                            if (sourceObject != null && sourceObject instanceof JsonObject) {
                                JsonObject sourceJson = (JsonObject) sourceObject;
                                sourceJson.addProperty("explorerId", explorerId);
                                sourceJson.addProperty("networkType", m_networkType.toString());
                                sourceJson.addProperty("timeStamp", Utils.getNowEpochMillis());

                                try {
                                    Files.writeString(logFile.toPath(), "\nExplorer object: " + getName() + ": " + sourceJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e) {

                                }

                                Platform.runLater(() -> m_ergoNetworkTokenData.set(new ErgoNetworkTokenData(sourceJson)));
                                m_explorerVerified.set(true);
                                getLastUpdated().set(LocalDateTime.now());
                            }
                        }, failedEvent -> {
                            m_explorerVerified.set(false);
                            try {
                                Files.writeString(logFile.toPath(), "\nFailed: " + getName() + ": " + failedEvent.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {

                            }
                        });
                    }
                }
            }
        }, failed -> {
        });

    }

    public SimpleObjectProperty<ErgoNetworkTokenData> tokenDataProperty() {
        return m_ergoNetworkTokenData;
    }

    public void showTokenStage() {
        if (m_ergoTokenStage == null) {

            m_ergoTokenStage = new Stage();
            m_ergoTokenStage.getIcons().add(ErgoWallet.getSmallAppIcon());
            m_ergoTokenStage.initStyle(StageStyle.UNDECORATED);
            m_ergoTokenStage.setTitle(getParentInterface().getName() + " - " + getName());
            m_ergoTokenStage.titleProperty().bind(Bindings.concat(getParentInterface().getName(), " - ", textProperty()));

            Text promptText = new Text("");
            TextArea descriptionTextArea = new TextArea();

            Label emissionLbl = new Label();
            TextField emissionAmountField = new TextField();
            Button closeBtn = new Button();
            Button maximizeBtn = new Button();
            if (m_ergoNetworkTokenData.get() != null) {

                ErgoNetworkTokenData ergoNetworkTokenData = m_ergoNetworkTokenData.get();
                promptText.setText(ergoNetworkTokenData.getName());
                descriptionTextArea.setText(ergoNetworkTokenData.getDescription());
                long emissionAmount = ergoNetworkTokenData.getEmissionAmount();

                if (emissionAmount != 0) {
                    emissionLbl.setText("Total Supply:");
                    emissionAmountField.setText(emissionAmount + "");
                }
            } else {
                promptText.setText("");
                descriptionTextArea.setText("No informaiton available.");
                emissionLbl.setText("");
                emissionAmountField.setText("");
            }

            ChangeListener<ErgoNetworkTokenData> tokenDataListener = (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    ErgoNetworkTokenData ergoNetworkTokenData = newVal;
                    promptText.setText(ergoNetworkTokenData.getName());
                    descriptionTextArea.setText(ergoNetworkTokenData.getDescription());
                    long emissionAmount = ergoNetworkTokenData.getEmissionAmount();

                    if (emissionAmount != 0) {
                        emissionLbl.setText("Total Supply:");
                        emissionAmountField.setText(emissionAmount + "");
                    }
                } else {
                    promptText.setText("");
                    descriptionTextArea.setText("No informaiton available.");
                    emissionLbl.setText("");
                    emissionAmountField.setText("");
                }
            };

            HBox titleBox = App.createTopBar(getIcon(), maximizeBtn, closeBtn, m_ergoTokenStage);

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

            Tooltip editTip = new Tooltip("Edit");
            editTip.setShowDelay(new javafx.util.Duration(100));
            editTip.setFont(App.txtFont);

            Button editButton = new Button();
            editButton.setGraphic(IconButton.getIconView(new Image("/assets/options-outline-white-30.png"), 30));
            editButton.setId("menuBtn");
            editButton.setTooltip(editTip);
            editButton.setOnAction(e -> {
                cmdProperty().set(getEditTokenJson());

            });

            HBox menuBar = new HBox(editButton, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            Button imageButton = App.createImageButton(getIcon(), getName());
            imageButton.setGraphicTextGap(25);

            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setPadding(new Insets(25, 0, 25, 0));

            promptText.setFont(App.txtFont);
            promptText.setFill(Color.WHITE);

            HBox promptBox = new HBox(promptText);

            promptBox.prefHeight(40);
            promptBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(promptBox, Priority.ALWAYS);
            promptBox.setPadding(new Insets(10, 15, 10, 30));

            descriptionTextArea.setFont(App.txtFont);
            descriptionTextArea.setId("bodyBox");
            descriptionTextArea.setEditable(false);
            descriptionTextArea.setWrapText(true);

            emissionLbl.setFont(App.txtFont);
            emissionLbl.setTextFill(App.altColor);

            emissionAmountField.setFont(App.txtFont);
            emissionAmountField.setEditable(false);
            emissionAmountField.setId("formField");

            Button urlLink = new Button("visit: " + m_urlString);
            urlLink.setFont(App.txtFont);
            urlLink.setId("addressBtn");
            urlLink.setOnAction(e -> {
                getNetworksData().getHostServices().showDocument(m_urlString);
            });

            HBox urlBox = new HBox(urlLink);
            urlBox.setPadding(new Insets(25, 30, 25, 30));
            urlBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(urlBox, Priority.ALWAYS);

            VBox scrollPaneVBox = new VBox(descriptionTextArea, urlBox);
            scrollPaneVBox.setPadding(new Insets(0, 40, 0, 40));
            HBox.setHgrow(scrollPaneVBox, Priority.ALWAYS);

            HBox emissionBox = new HBox(emissionLbl, emissionAmountField);
            emissionBox.setAlignment(Pos.CENTER_LEFT);
            // emissionBox.setPadding(new Insets(0, 0, 0, 15));

            VBox footerVBox = new VBox(emissionBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);

            HBox footerHBox = new HBox(footerVBox);
            footerHBox.setPadding(new Insets(25, 30, 25, 30));
            footerHBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(footerHBox, Priority.ALWAYS);

            VBox bodyVBox = new VBox(menuBar, imageBox, promptBox, scrollPaneVBox);

            HBox bodyPaddingBox = new HBox(bodyVBox);
            bodyPaddingBox.setPadding(SMALL_INSETS);

            VBox layoutVBox = new VBox(titleBox, bodyPaddingBox, footerHBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

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
                            explorerBtn.setGraphic(IconButton.getIconView(new InstallableIcon(getNetworksData(), explorerInterface.getNetworkId(), true).getIcon(), 30));
                        });
                    }

                } else {
                    Platform.runLater(() -> explorerBtn.setGraphic(IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30)));
                }
            }, onFailed -> {
            });

            Scene tokenScene = new Scene(layoutVBox, m_sceneWidth.get(), m_sceneHeight.get());

            tokenScene.getStylesheets().add("/css/startWindow.css");
            m_ergoTokenStage.setScene(tokenScene);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_ergoTokenStage, 300, 600, rect.getWidth(), rect.getHeight());
            m_ergoTokenStage.show();

            urlLink.maxWidthProperty().bind(tokenScene.widthProperty().multiply(0.75));

            descriptionTextArea.prefWidthProperty().bind(m_ergoTokenStage.widthProperty().subtract(40));
            descriptionTextArea.prefHeightProperty().bind(tokenScene.heightProperty().subtract(titleBox.heightProperty()).subtract(imageBox.heightProperty()).subtract(promptBox.heightProperty()).subtract(footerHBox.heightProperty()));

            m_ergoNetworkTokenData.addListener(tokenDataListener);

            addShutdownListener((obs, oldVal, newVal) -> {

                Platform.runLater(() -> closeBtn.fire());
            });

            m_ergoTokenStage.setOnCloseRequest(e -> {
                removeShutdownListener();
                m_ergoNetworkTokenData.removeListener(tokenDataListener);
                close();
                m_ergoTokenStage = null;

            });

            closeBtn.setOnAction(event -> {
                removeShutdownListener();
                m_ergoNetworkTokenData.removeListener(tokenDataListener);
                close();

                m_ergoTokenStage.close();
                m_ergoTokenStage = null;
            });

        } else {
            if (m_ergoTokenStage.isIconified()) {
                m_ergoTokenStage.setIconified(false);
            }
            m_ergoTokenStage.show();
        }
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", getName());
        jsonObject.addProperty("tokenId", getTokenId());
        jsonObject.addProperty("imageString", m_imageString);
        jsonObject.addProperty("url", m_urlString);
        jsonObject.addProperty("sceneWidth", m_sceneWidth.get());
        jsonObject.addProperty("sceneHeight", m_sceneHeight.get());
        jsonObject.addProperty("networkType", m_networkType.toString());
        if (m_hashData != null) {
            jsonObject.add("hashData", m_hashData.getJsonObject());
        }
        if (m_ergoNetworkTokenData.get() != null) {
            jsonObject.add("tokenData", m_ergoNetworkTokenData.get().getJsonObject());
        }
        return jsonObject;
    }

    public String getTokenId() {
        return getNetworkId();
    }

    public String getUrlString() {
        return m_urlString;
    }

    public HashData getHashData() {
        return m_hashData;
    }

    public JsonObject getEditTokenJson() {
        JsonObject editObject = new JsonObject();
        editObject.addProperty("subject", "EDIT");
        editObject.addProperty("timeStamp", Utils.getNowEpochMillis());
        return editObject;
    }

    @Override
    public String toString() {
        return getName();
    }

}
