package com.netnotes;

import java.awt.Rectangle;
import java.io.File;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoToken extends Network implements NoteInterface {

    private File m_imageFile = null;

    private Stage m_ergoTokenStage = null;
    private double m_sceneWidth = 450;
    private double m_sceneHeight = 350;

    public ErgoToken(String name, String tokenId, JsonObject jsonObject, NoteInterface parent) {
        super(null, name, tokenId, parent);

        JsonElement imageStringElement = jsonObject.get("imageString");

        if (imageStringElement != null) {
            m_imageFile = new File(imageStringElement.getAsString());
        }

        Image image = m_imageFile != null && m_imageFile.isFile() ? new Image(m_imageFile.getAbsolutePath()) : null;

        setIcon(image);

        setIconStyle(IconStyle.ROW);

    }

    public ErgoToken(String name, String tokenId, File imageFile, NoteInterface parent) {
        super(null, name, tokenId, parent);

        Image image = imageFile != null && imageFile.isFile() ? new Image(imageFile.getAbsolutePath()) : null;

        setIcon(image);

        setIconStyle(IconStyle.ROW);

        m_imageFile = imageFile;

    }

    @Override
    public void open() {
        showTokenStage();
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

            HBox titleBox = App.createTopBar(ErgoWallet.getSmallAppIcon(), maximizeBtn, closeBtn, m_ergoTokenStage);

            closeBtn.setOnAction(event -> {
                m_ergoTokenStage.close();

            });

            Button imageButton = App.createImageButton(ErgoWallet.getAppIcon(), getName());
            imageButton.setGraphicTextGap(10);
            HBox imageBox = new HBox(imageButton);
            imageBox.setAlignment(Pos.CENTER);

            VBox layoutVBox = new VBox(titleBox, imageBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene tokenScene = new Scene(layoutVBox, m_sceneWidth, m_sceneHeight);

            tokenScene.getStylesheets().add("/css/startWindow.css");
            m_ergoTokenStage.setScene(tokenScene);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_ergoTokenStage, 300, 400, rect.getWidth(), rect.getHeight());
        }
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();
        jsonObject.addProperty("imageString", m_imageFile.getAbsolutePath());
        return jsonObject;
    }

    public File getImageFile() {
        return m_imageFile;
    }

    public String getTokenId() {
        return getNetworkId();
    }

}
