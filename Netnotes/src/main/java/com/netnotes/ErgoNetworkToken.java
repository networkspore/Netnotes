package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

    private File m_imageFile = null;

    private NetworkType m_networkType = NetworkType.MAINNET;
    private ErgoTokens m_ErgoTokens = null;

    private SimpleObjectProperty<ErgoNetworkTokenData> m_ergoNetworkTokenData = new SimpleObjectProperty<>(null);
    private Stage m_ergoTokenStage = null;
    private double m_sceneWidth = 450;
    private double m_sceneHeight = 450;
    private String m_urlString;

    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(null);

    //services.showDocument(updateUrl);
    public ErgoNetworkToken(String name, String tokenId, NetworkType networkType, JsonObject jsonObject, ErgoTokens ergoTokens) {
        super(null, name, tokenId, ergoTokens);

        m_networkType = networkType;
        m_ErgoTokens = ergoTokens;
        JsonElement imageStringElement = jsonObject.get("imageString");

        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement urlElement = jsonObject.get("url");

        if (urlElement != null) {
            m_urlString = urlElement.getAsString();
        }

        if (imageStringElement != null) {
            m_imageFile = new File(imageStringElement.getAsString());
        }

        m_networkType = networkTypeElement == null ? NetworkType.MAINNET : networkTypeElement.toString().equals(NetworkType.MAINNET.toString()) ? NetworkType.MAINNET : NetworkType.TESTNET;

        Image image = m_imageFile != null && m_imageFile.isFile() ? new Image(m_imageFile.getAbsolutePath()) : null;

        setIcon(image);

        setIconStyle(IconStyle.ROW);

    }

    public ErgoNetworkToken(String name, String url, String tokenId, File imageFile, NetworkType networkType, ErgoTokens ergoTokens) {
        super(null, name, tokenId, ergoTokens);

        m_urlString = url;
        m_ErgoTokens = ergoTokens;
        m_networkType = networkType;

        Image image = imageFile != null && imageFile.isFile() ? new Image(imageFile.getAbsolutePath()) : null;

        setIcon(image);

        setIconStyle(IconStyle.ROW);

        m_imageFile = imageFile;

    }

    @Override
    public void open() {
        super.open();
        updateExplorerTokenInfo();
        showTokenStage();
    }

    public void updateExplorerTokenInfo() {
        NoteInterface explorerInterface = m_ErgoTokens.getExplorerInterface();
        if (explorerInterface != null) {

            if (explorerInterface != null) {
                JsonObject note = ErgoExplorer.getIssuedTokensNote(getNetworkId(), m_networkType);

                explorerInterface.sendNote(note, succeededEvent -> {
                    WorkerStateEvent workerEvent = succeededEvent;
                    Object sourceObject = workerEvent.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof JsonObject) {
                        JsonObject sourceJson = (JsonObject) sourceObject;
                        ErgoNetworkTokenData ergoNetworkTokenData = new ErgoNetworkTokenData(sourceJson, explorerInterface.getNetworkId(), m_networkType);
                        m_ergoNetworkTokenData.set(ergoNetworkTokenData);
                        try {
                            Files.writeString(logFile.toPath(), "\nGot json: " + getName() + ": " + sourceJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }
                    }
                }, failedEvent -> {
                    try {
                        Files.writeString(logFile.toPath(), "\nFailed: " + getName() + ": " + failedEvent.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                });
            }
        }
    }

    public void showTokenStage() {
        if (m_ergoTokenStage == null) {

            m_ergoTokenStage = new Stage();
            m_ergoTokenStage.getIcons().add(ErgoWallet.getSmallAppIcon());
            m_ergoTokenStage.initStyle(StageStyle.UNDECORATED);
            m_ergoTokenStage.setTitle(getParentInterface().getName() + " - " + getName());
            m_ergoTokenStage.titleProperty().bind(Bindings.concat(getParentInterface().getName(), " - ", textProperty()));

            Button closeBtn = new Button();
            Button maximizeBtn = new Button();

            HBox titleBox = App.createTopBar(getIcon(), maximizeBtn, closeBtn, m_ergoTokenStage);

            ChangeListener<LocalDateTime> changeListener = (obs, oldVal, newVal) -> {

                close();

                m_ergoTokenStage.close();
                m_ergoTokenStage = null;
            };

            closeBtn.setOnAction(event -> {
                m_shutdownNow.removeListener(changeListener);
                close();

                m_ergoTokenStage.close();
                m_ergoTokenStage = null;
            });

            Button imageButton = App.createImageButton(getIcon(), getName());
            imageButton.setGraphicTextGap(10);
            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);

            Text promptText = new Text("");
            promptText.setFont(App.txtFont);
            promptText.setFill(Color.WHITE);

            HBox promptBox = new HBox(promptText);

            promptBox.prefHeight(40);
            promptBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(promptBox, Priority.ALWAYS);
            promptBox.setPadding(new Insets(10, 15, 10, 30));

            TextArea descriptionTextArea = new TextArea();
            descriptionTextArea.setFont(App.txtFont);

            descriptionTextArea.setEditable(false);
            descriptionTextArea.setWrapText(true);

            Label emissionLbl = new Label();
            emissionLbl.setFont(App.txtFont);
            emissionLbl.setTextFill(App.altColor);

            TextField emissionAmountField = new TextField();
            emissionAmountField.setFont(App.txtFont);
            emissionAmountField.setEditable(false);
            emissionAmountField.setId("formField");

            HBox emissionBox = new HBox(emissionLbl, emissionAmountField);
            emissionBox.setAlignment(Pos.CENTER_LEFT);
            // emissionBox.setPadding(new Insets(0, 0, 0, 15));

            VBox scrollPaneVBox = new VBox(descriptionTextArea);
            scrollPaneVBox.setPadding(new Insets(0, 40, 0, 40));
            HBox.setHgrow(scrollPaneVBox, Priority.ALWAYS);

            Button urlLink = new Button(m_urlString);
            urlLink.setFont(App.txtFont);
            urlLink.setId("menuBtn");
            urlLink.setOnAction(e -> {
                getNetworksData().getHostServices().showDocument(m_urlString);
            });

            HBox urlBox = new HBox(urlLink);
            
            urlBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(urlBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(urlBox, emissionBox);
            HBox.setHgrow(footerVBox,Priority.ALWAYS);

            HBox footerHBox = new HBox(footerVBox);
            footerHBox.setPadding(new Insets(25, 30, 25, 30));
            footerHBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(footerHBox, Priority.ALWAYS);

            VBox layoutVBox = new VBox(titleBox, imageBox, promptBox, scrollPaneVBox, footerHBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene tokenScene = new Scene(layoutVBox, m_sceneWidth, m_sceneHeight);

            tokenScene.getStylesheets().add("/css/startWindow.css");
            m_ergoTokenStage.setScene(tokenScene);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_ergoTokenStage, 300, 400, rect.getWidth(), rect.getHeight());
            m_ergoTokenStage.show();

            descriptionTextArea.prefWidthProperty().bind(scrollPaneVBox.widthProperty().subtract(40));
            descriptionTextArea.prefHeightProperty().bind(tokenScene.heightProperty().subtract(titleBox.heightProperty()).subtract(imageBox.heightProperty()).subtract(promptBox.heightProperty()).subtract(footerHBox.heightProperty()));

            m_ergoNetworkTokenData.addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    ErgoNetworkTokenData ergoNetworkTokenData = newVal;
                    promptText.setText(ergoNetworkTokenData.getName());
                    descriptionTextArea.setText(ergoNetworkTokenData.getDescription());
                    long emissionAmount = ergoNetworkTokenData.getEmissionAmount();

                    if (emissionAmount != 0) {
                        emissionLbl.setText("Total Supply:");
                        emissionAmountField.setText(emissionAmount + "");
                    }
                }
            });

            m_ergoTokenStage.setOnCloseRequest(e -> {
                m_shutdownNow.removeListener(changeListener);
                close();
                m_ergoTokenStage = null;

            });

            m_shutdownNow.addListener(changeListener);
        } else {
            if (m_ergoTokenStage.isIconified()) {
                m_ergoTokenStage.setIconified(false);
            }
            m_ergoTokenStage.show();
        }
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();
        jsonObject.addProperty("imageString", m_imageFile.getAbsolutePath());
        jsonObject.addProperty("url", m_urlString);
        return jsonObject;
    }

    public File getImageFile() {
        return m_imageFile;
    }

    public String getTokenId() {
        return getNetworkId();
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjectElement = note.get("subject");
        if (subjectElement != null) {
            switch (subjectElement.getAsString()) {
                case "SHUTDOWN_NOW":
                    m_shutdownNow.set(LocalDateTime.now());
                    break;
            }
        }
        return false;
    }

}
