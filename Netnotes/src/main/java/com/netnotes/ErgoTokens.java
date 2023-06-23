package com.netnotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

    TokensList m_tokensList = null;

    ArrayList<NoteInterface> m_openTokens = new ArrayList<NoteInterface>();

    private NetworkType m_networkType = NetworkType.MAINNET;
    private String m_explorerId = NetworkID.ERGO_EXPLORER;

    public ErgoTokens(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_TOKENS, networksData);

        m_appDir = new File(System.getProperty("user.dir") + "/" + ErgoTokens.NAME);
        m_testnetDataFile = new File(m_appDir.getAbsolutePath() + "/testnet" + ErgoTokens.NAME + ".dat");
        setDataFile(m_appDir.getAbsolutePath() + "/" + ErgoTokens.NAME + ".dat");

    }

    public ErgoTokens(JsonObject jsonObject, NetworksData networksData) {

        super(getAppIcon(), NAME, NetworkID.ERGO_TOKENS, networksData);

        JsonElement networkTypeElement = jsonObject.get("networkType");
        JsonElement appDirElement = jsonObject.get("appDir");
        JsonElement dataElement = jsonObject.get("dataFile");
        JsonElement testnetDataElement = jsonObject.get("testnetDataFile");

        if (networkTypeElement != null) {
            String networkTypeString = networkTypeElement.getAsString();

            if (networkTypeElement.equals(NetworkType.TESTNET.toString())) {
                m_networkType = NetworkType.TESTNET;
            }
        }

        if (appDirElement == null) {
            m_appDir = new File(System.getProperty("user.dir") + "/" + ErgoTokens.NAME);
        } else {
            m_appDir = new File(appDirElement.getAsString());
        }

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

    @Override
    public void open() {

        m_tokensList = new TokensList(getNetworksData().getAppKey(), this);
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
        m_tokensList.update(getNetworksData().getAppKey());
    }

    public NetworkType getNetworkType() {
        return m_networkType;
    }

    public void showTokensStage() {
        if (m_tokensStage == null) {

            double tokensStageWidth = 340;
            double tokensStageHeight = 500;
            double buttonHeight = 100;

            m_tokensStage = new Stage();
            m_tokensStage.getIcons().add(getIcon());
            m_tokensStage.setResizable(false);
            m_tokensStage.initStyle(StageStyle.UNDECORATED);
            m_tokensStage.setTitle(getName() + ": Tokens -" + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_tokensStage.close();
                m_tokensStage = null;
                m_tokensList.shutdownNow();
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getIcon(), maxBtn, closeBtn, m_tokensStage);

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
                m_tokensStage.setTitle(getName() + ": Tokens -" + (m_networkType == NetworkType.MAINNET ? "(MAINNET)" : "(TESTNET)"));
            });

            Tooltip explorerTip = new Tooltip(getExplorerInterface() == null ? "Explorer disabled" : getExplorerInterface().getName());
            explorerTip.setShowDelay(new javafx.util.Duration(100));
            explorerTip.setFont(App.txtFont);

            MenuButton explorerBtn = new MenuButton();
            explorerBtn.setGraphic(getExplorerInterface() == null ? IconButton.getIconView(new Image("/assets/search-outline-white-30.png"), 30) : IconButton.getIconView(new InstallableIcon(getNetworksData(), getExplorerInterface().getNetworkId(), true).getIcon(), 30));
            explorerBtn.setPadding(new Insets(2, 0, 0, 0));
            explorerBtn.setTooltip(explorerTip);

            HBox rightSideMenu = new HBox(explorerBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox menuBar = new HBox(toggleNetworkTypeBtn, spacer, rightSideMenu);
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

            VBox tokensBox = m_tokensList.getButtonGrid();

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
            // removeButton.setGraphic(addImage);
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setTooltip(removeTip);
            removeButton.setDisable(true);
            removeButton.setPrefWidth(tokensStageWidth / 2);
            removeButton.setPrefHeight(buttonHeight);

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            addButton.setOnAction(event -> {
                //m_walletsData.showAddWalletStage();
            });

            layoutVBox.getChildren().addAll(menuBar, scrollPane, menuBox);

            Scene tokensScene = new Scene(layoutVBox, tokensStageWidth, tokensStageHeight);

            scrollPane.prefViewportWidthProperty().bind(tokensScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(tokensScene.heightProperty().subtract(menuBar.heightProperty()).subtract(menuBox.heightProperty()));

            tokensBox.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty());
            //  bodyBox.prefHeightProperty().bind(tokensScene.heightProperty() - 40 - 100);
            tokensScene.getStylesheets().add("/css/startWindow.css");
            m_tokensStage.setScene(tokensScene);

            m_tokensStage.show();
        } else {
            if (m_tokensStage.isIconified()) {
                m_tokensStage.setIconified(false);
            }
            m_tokensStage.show();
        }

    }

    private void setDataFile(String fileString) {
        m_dataFile = new File(fileString);
        getLastUpdated().set(LocalDateTime.now());
    }

    public File getDataFile() {
        return m_dataFile;
    }

    public File getTestnetDataFile() {
        return m_testnetDataFile;
    }

    private void setTestnetDataFile(String fileString) {
        m_testnetDataFile = new File(fileString);
        getLastUpdated().set(LocalDateTime.now());
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

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
        if (m_dataFile != null) {
            json.addProperty("dataFile", m_dataFile.getAbsolutePath());
        }
        return json;
    }

}
