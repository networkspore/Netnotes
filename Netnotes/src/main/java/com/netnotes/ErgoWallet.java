package com.netnotes;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import org.ergoplatform.appkit.Address;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import javafx.scene.control.ScrollPane;

import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoWallet extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Wallet allows you to create and manage wallets on the Ergo Blockchain.";
    public final static String SUMMARY = "Access can be controlled with the Ergo Wallet, in order to keep the wallet isolated, or access can be given to the Ergo Network in order to make transactions, or the Ergo Explorer to get your ERG ballance and to the KuCoin Exchange to get your ERG value real time.";
    public final static String NAME = "Ergo Wallet";
    public final static ExtensionFilter ergExt = new ExtensionFilter("Ergo Wallet", "*.erg");

    public final static String DONATION_ADDRESS_STRING = "9h123xUZMi26FZrHuzsFfsTpfD3mMuTxQTNEhAjTpD83EPchePU";

    private File logFile = new File("ergoWallet - log.txt");

    private File m_appDir = null;

    private File m_walletsDir = null;

    private WalletsDataList m_walletsData;
    private Stage m_walletsStage = null;

    public ErgoWallet(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_WALLET, networksData);
        setupWallet();
        m_walletsData = new WalletsDataList(null, m_walletsDir, this);
        m_walletsData.lastUpdated.addListener(e -> {
            getLastUpdated().set(LocalDateTime.now());
        });
    }

    public ErgoWallet(JsonObject jsonObject, NetworksData networksData) {

        super(getAppIcon(), NAME, NetworkID.ERGO_WALLET, networksData);

        try {
            Files.writeString(logFile.toPath(), jsonObject.toString());
        } catch (IOException e) {

        }

        JsonElement walletsElement = jsonObject.get("wallets");
        JsonElement directoriesElement = jsonObject.get("directories");

        if (directoriesElement != null && directoriesElement.isJsonObject()) {
            JsonObject directoriesObject = directoriesElement.getAsJsonObject();
            if (directoriesObject != null) {
                JsonElement appDirElement = directoriesObject.get("app");
                JsonElement walletsDirElement = directoriesObject.get("wallets");

                m_appDir = appDirElement == null ? null : new File(appDirElement.getAsString());

                m_walletsDir = walletsDirElement == null ? null : new File(walletsDirElement.getAsString());
            }
        }

        if (m_appDir == null || m_walletsDir == null) {
            setupWallet();
        }

        if (walletsElement != null) {
            Timer initTimer = new Timer();
            ErgoWallet ergoWallet = this;

            try {
                Files.writeString(logFile.toPath(), "\ngetting wallets", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }

            initTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    JsonArray walletsArray = walletsElement == null ? null : walletsElement.getAsJsonArray();
                    try {
                        Files.writeString(logFile.toPath(), "\ntimer fired:\n" + walletsArray.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    m_walletsData = new WalletsDataList(walletsArray, m_walletsDir, ergoWallet);
                    m_walletsData.lastUpdated.addListener(e -> {
                        getLastUpdated().set(LocalDateTime.now());
                    });
                }
            }, 200);
        } else {
            m_walletsData = new WalletsDataList(null, m_walletsDir, this);
            m_walletsData.lastUpdated.addListener(e -> {
                getLastUpdated().set(LocalDateTime.now());
            });
        }

    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();
        jsonObject.addProperty("walletsDir", getWalletsDirectory().getAbsolutePath());
        JsonArray jsonArray = m_walletsData.getJsonArray();
        jsonObject.add("wallets", jsonArray);
        return jsonObject;
    }

    public static Image getAppIcon() {
        return App.ergoWallet;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergo-wallet-30.png");
    }

    @Override
    public void open() {
        /* Alert a = new Alert(AlertType.NONE, "opening", ButtonType.CLOSE);
        a.show(); */
        showWalletsStage();
    }

    public File getWalletsDirectory() {
        return m_walletsDir;
    }

    public void showWalletsStage() {
        if (m_walletsStage == null) {

            String title = getName() + ": Wallets";
            double walletsStageWidth = 310;
            double walletsStageHeight = 500;
            double buttonHeight = 100;

            m_walletsStage = new Stage();
            m_walletsStage.getIcons().add(getIcon());
            m_walletsStage.setResizable(false);
            m_walletsStage.initStyle(StageStyle.UNDECORATED);
            m_walletsStage.setTitle(title);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_walletsStage.close();
                m_walletsStage = null;
            });

            HBox titleBox = App.createTopBar(getIcon(), title, closeBtn, m_walletsStage);

            ImageView addImage = new ImageView(App.addImg);
            addImage.setFitHeight(10);
            addImage.setPreserveRatio(true);

            Tooltip addTip = new Tooltip("New");
            addTip.setShowDelay(new javafx.util.Duration(100));
            addTip.setFont(App.txtFont);

            VBox layoutVBox = new VBox(titleBox);
            layoutVBox.setPadding(new Insets(0, 5, 0, 5));
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            VBox walletsBox = m_walletsData.getButtonGrid();

            Region growRegion = new Region();

            VBox.setVgrow(growRegion, Priority.ALWAYS);

            VBox bodyBox = new VBox(walletsBox, growRegion);

            ScrollPane scrollPane = new ScrollPane(bodyBox);

            scrollPane.setId("bodyBox");

            Button addButton = new Button("New");
            // addButton.setGraphic(addImage);
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setTooltip(addTip);
            addButton.setPrefWidth(walletsStageWidth / 2);
            addButton.setPrefHeight(buttonHeight);

            Tooltip removeTip = new Tooltip("Remove");
            removeTip.setShowDelay(new javafx.util.Duration(100));
            removeTip.setFont(App.txtFont);

            Button removeButton = new Button("Remove");
            // removeButton.setGraphic(addImage);
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setTooltip(removeTip);
            removeButton.setDisable(true);
            removeButton.setPrefWidth(walletsStageWidth / 2);
            removeButton.setPrefHeight(buttonHeight);

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            addButton.setOnAction(event -> {
                m_walletsData.showAddWalletStage();
            });

            layoutVBox.getChildren().addAll(scrollPane, menuBox);

            Scene walletsScene = new Scene(layoutVBox, walletsStageWidth, walletsStageHeight);

            scrollPane.prefViewportWidthProperty().bind(walletsScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(walletsScene.heightProperty().subtract(140));

            walletsBox.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty());
            //  bodyBox.prefHeightProperty().bind(walletsScene.heightProperty() - 40 - 100);
            walletsScene.getStylesheets().add("/css/startWindow.css");
            m_walletsStage.setScene(walletsScene);

            m_walletsStage.show();
        } else {
            if (m_walletsStage.isIconified()) {
                m_walletsStage.setIconified(false);
            }
        }

    }

    public void setupWallet() {

        m_appDir = m_appDir == null ? new File(System.getProperty("user.dir") + "/" + NAME) : m_appDir;

        m_walletsDir = m_walletsDir == null ? new File(System.getProperty("user.dir") + "/" + NAME + "/wallets") : m_walletsDir;

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }

        if (!m_walletsDir.isDirectory()) {
            try {
                Files.createDirectories(m_walletsDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjecElement = note.get("subject");
        if (subjecElement != null) {
            switch (subjecElement.getAsString()) {
            }
        }
        return false;
    }

    @Override
    public boolean sendNoteToFullNetworkId(JsonObject note, String fullNetworkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int indexOfperiod = fullNetworkId.indexOf(".");

        if (indexOfperiod == -1) {
            return sendNote(note, onSucceeded, onFailed);
        }

        return m_walletsData.sendToFullNetworkId(note, fullNetworkId, null, null);

    }
}