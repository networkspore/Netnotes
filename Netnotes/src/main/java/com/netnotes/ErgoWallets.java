package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.utils.Utils;

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


import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ErgoWallets extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Wallet allows you to create and manage wallets on the Ergo Blockchain.";
    public final static String SUMMARY = "Access can be controlled with the Ergo Wallet, in order to keep the wallet isolated, or access can be given to the Ergo Network in order to make transactions, or the Ergo Explorer to get your ERG ballance and to the KuCoin Exchange to get your ERG value real time.";
    public final static String NAME = "Ergo Wallet";
    public final static ExtensionFilter ergExt = new ExtensionFilter("Ergo Wallet", "*.erg");
    public final static String NETWORK_ID = "ERGO_WALLET";
    public final static String DONATION_ADDRESS_STRING = "9h123xUZMi26FZrHuzsFfsTpfD3mMuTxQTNEhAjTpD83EPchePU";

    private File logFile = new File("netnotes-log.txt");

    private File m_appDir = null;

    private File m_walletsDir = null;
    private File m_dataFile = null;

    private Stage m_walletsStage = null;

    private final static long EXECUTION_TIME = 500;

    private ScheduledFuture<?> m_lastExecution = null;

    private ErgoNetworkData m_ergNetData;
    private ErgoNetwork m_ergoNetwork;

    public ErgoWallets(ErgoNetworkData ergNetData, ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);
        m_ergoNetwork = ergoNetwork;
        setupWallet();
        addListeners();
        setStageIconStyle(IconStyle.ROW);
        getLastUpdated().set(LocalDateTime.now());
        m_ergNetData = ergNetData;
    }

    public ErgoWallets(ErgoNetworkData ergNetData, JsonObject jsonObject, ErgoNetwork ergoNetwork) {

        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);
        m_ergoNetwork = ergoNetwork;
        m_ergNetData = ergNetData;

        JsonElement directoriesElement = jsonObject.get("directories");
        JsonElement datFileElement = jsonObject.get("datFile");
        JsonElement stageElement = jsonObject.get("stage");

        if (directoriesElement != null && directoriesElement.isJsonObject()) {
            JsonObject directoriesObject = directoriesElement.getAsJsonObject();
            if (directoriesObject != null) {
                JsonElement appDirElement = directoriesObject.get("app");
                JsonElement walletsDirElement = directoriesObject.get("wallets");

                m_appDir = appDirElement == null ? null : new File(appDirElement.getAsString());

                m_walletsDir = walletsDirElement == null ? null : new File(walletsDirElement.getAsString());
            }
        }
        boolean save = false;
        if (m_appDir == null || m_walletsDir == null) {
            setupWallet();
            save = true;
        }

        if (datFileElement != null && datFileElement.isJsonPrimitive()) {
            m_dataFile = new File(datFileElement.getAsString());
        } else {
            m_dataFile = new File(m_appDir.getAbsolutePath() + "/ergoWallet.dat");
            save = true;
        }

        if (stageElement != null && stageElement.isJsonObject()) {
            JsonObject stageObject = stageElement.getAsJsonObject();
            JsonElement widthElement = stageObject.get("width");
            JsonElement heightElement = stageObject.get("height");
            JsonElement iconStyleElement = stageObject.get("iconStyle");

            setStageIconStyle(iconStyleElement.getAsString());
            setStageWidth(widthElement.getAsDouble());
            setStageHeight(heightElement.getAsDouble());
        }
        if (save) {
            getLastUpdated().set(LocalDateTime.now());
        }
        addListeners();
    }

    public ErgoNetworkData getErgoNetworkData() {
        return m_ergNetData;
    }

    public JsonObject getDirectoriesJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("appDir", m_appDir.getAbsolutePath());
        jsonObject.addProperty("walletsDir", getWalletsDirectory().getAbsolutePath());
        return jsonObject;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject jsonObject = super.getJsonObject();

        jsonObject.addProperty("datFile", m_dataFile.getAbsolutePath());
        jsonObject.add("directories", getDirectoriesJson());
        jsonObject.add("stage", getStageJson());
        return jsonObject;
    }
    
    public File getDataDir() throws IOException{
        File dataDir = new File(getAppDir().getAbsolutePath() + "/data");
        if(!dataDir.isDirectory()){
            Files.createDirectory(dataDir.toPath());
        }
        return dataDir;
    }

    public File getDataFile() throws IOException{
        File dataDir = getDataDir();

        File dataFile = new File(dataDir.getCanonicalPath() + "/data.dat");
        return dataFile;
    }

    public File createNewDataFile() throws IOException{
        File dataDir = getDataDir();

        File dataFile = new File(dataDir.getCanonicalPath() + "/"+FriendlyId.createFriendlyId()+".dat");
        return dataFile;
    }

    public void saveAddressInfo(String id, JsonObject json) throws IOException{
        if(id != null && json != null){
            File dataFile = getIdDataFile(id);

            try {
                Utils.saveJson(getNetworksData().getAppData().appKeyProperty().get(), json, dataFile);
           
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                try {
                    Files.writeString(logFile.toPath(),"Error saving Wallets data Array(saveAddressInfo): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                
                }
            }

        }
    }

    public JsonObject getAddressInfo(String id) throws IOException{
        File dataFile = getIdDataFile(id);
        if(dataFile.isFile()){
            try {
                return Utils.readJsonFile(getNetworksData().getAppData().appKeyProperty().get(), dataFile.toPath());
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                try {
                    Files.writeString(logFile.toPath(),"Error reading Wallets data Array(getAddressInfo): " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                
                }
            }
        }
        return null;
    }


    public File getIdDataFile(String id) throws IOException{
        
        File dataFile = getDataFile();
    
        try {
           
            
            if(dataFile.isFile()){
              
                JsonObject json = Utils.readJsonFile(getNetworksData().getAppData().appKeyProperty().get(), dataFile.toPath());
                JsonElement dataFilesElement = json.get("dataFiles");
                json.remove("dataFiles");
                if(dataFilesElement != null && dataFilesElement.isJsonArray()){
                    JsonArray dataFilesArray = dataFilesElement.getAsJsonArray();
           
                    for(int i = 0; i < dataFilesArray.size(); i ++){
                        JsonElement dataFileElement = dataFilesArray.get(i);
                        if(dataFileElement != null && dataFileElement.isJsonObject()){
                            JsonObject fileObject = dataFileElement.getAsJsonObject();
                            JsonElement idElement = fileObject.get("id");
                            if(idElement != null && idElement.isJsonPrimitive()){
                                String fileIdString = idElement.getAsString();
                                if(fileIdString.equals(id)){
                                    JsonElement fileElement = fileObject.get("file");

                                    if(fileElement != null && fileElement.isJsonPrimitive()){
                                        return new File(fileElement.getAsString());
                                    }else{
                                        File newFile = createNewDataFile();
                                        JsonObject fileJson = new JsonObject();
                                        fileJson.addProperty("id", id);
                                        fileJson.addProperty("file", newFile.getCanonicalPath());
                                        dataFilesArray.set(i, fileJson);
                                        
                                        json.add("dataFiles", dataFilesArray);

                                        try {
                                            Utils.saveJson(getNetworksData().getAppData().appKeyProperty().get(), json, dataFile);
                                        
                                        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                                                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                                    
                                        }
                                        return newFile;
                                    }
                                }
                            }
                        }
                    }

                    File newFile = createNewDataFile();

                    JsonObject fileJson = new JsonObject();
                    fileJson.addProperty("id", id);
                    fileJson.addProperty("file", newFile.getCanonicalPath());
                    dataFilesArray.add(fileJson);
                    json.add("dataFiles", dataFilesArray);
                    try {
                        Utils.saveJson(getNetworksData().getAppData().appKeyProperty().get(), json, dataFile);
                       
                    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                            | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                        try {
                            Files.writeString(logFile.toPath(),"Error saving Wallets data Array: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {
                        
                        }
                    }
                     return newFile;
                }
            }
      
        
        } catch (IOException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(),"Error saving Wallets data Array: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
             
            }
        }
        
        File newFile = createNewDataFile();

        JsonObject fileJson = new JsonObject();
        fileJson.addProperty("id", id);
        fileJson.addProperty("file", newFile.getCanonicalPath());

        JsonArray fileArray = new JsonArray();
        fileArray.add(fileJson);

        JsonObject json = new JsonObject();
        json.add("dataFiles", fileArray);

        try {
            Utils.saveJson(getNetworksData().getAppData().appKeyProperty().get(), json, dataFile);
            return newFile;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(),"Error saving Wallets data Array: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
            
            }
        }

        return null;
    }

    public static Image getAppIcon() {
        return App.ergoWallet;
    }

    public File getAppDir(){
        return m_appDir;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergo-wallet-30.png");
    }

    @Override
    public void open() {
        /* Alert a = new Alert(AlertType.NONE, "opening", ButtonType.CLOSE);
        a.show(); */
        try{
            showWalletsStage();
        }catch(Exception e){
            try {
                Files.writeString(logFile.toPath(), "ergoWallets: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
               
            }
        }
    }

    public File getWalletsDirectory() {
        return m_walletsDir;
    }

    public void showWalletsStage() {
        if (m_walletsStage == null) {

            ErgoWalletDataList walletsDataList = new ErgoWalletDataList(getStageWidth() - 30, getStageIconStyle(), m_dataFile, m_walletsDir, this);

            String title = "Wallets" + " - " + getName();

            double buttonHeight = 100;

            m_walletsStage = new Stage();
            m_walletsStage.getIcons().add(getIcon());
            m_walletsStage.setResizable(false);
            m_walletsStage.initStyle(StageStyle.UNDECORATED);
            m_walletsStage.setTitle(title);

            SimpleBooleanProperty doClose = new SimpleBooleanProperty(false);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_walletsStage.close();
                m_walletsStage = null;
            });
            doClose.addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    closeBtn.fire();
                }
            });

            HBox titleBox = App.createTopBar(getIcon(), title, closeBtn, m_walletsStage);
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            Tooltip gridTypeToolTip = new Tooltip("Toggle: List view");
            gridTypeToolTip.setShowDelay(new Duration(50));
            gridTypeToolTip.setHideDelay(new Duration(200));

            BufferedButton toggleGridTypeButton = new BufferedButton("/assets/list-outline-white-25.png", App.MENU_BAR_IMAGE_WIDTH);
            toggleGridTypeButton.setTooltip(gridTypeToolTip);
     

            HBox menuBar = new HBox(menuSpacer, toggleGridTypeButton);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 5, 1, 5));

           HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setId("darkBox");
            HBox.setHgrow(menuBarPadding, Priority.ALWAYS);
            menuBarPadding.setPadding(new Insets(0,0,4,0));
            
            VBox headerBox = new VBox(menuBarPadding);

            Region growRegion = new Region();

            VBox.setVgrow(growRegion, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane();

            scrollPane.setId("bodyBox");

            Button addButton = new Button("Add");
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setPrefWidth(getStageWidth() / 2);
            addButton.setPrefHeight(buttonHeight);

            Button removeButton = new Button("Remove");
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setDisable(true);
            removeButton.setPrefWidth(getStageWidth() / 2);
            removeButton.setPrefHeight(buttonHeight);

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            VBox layoutVBox = new VBox(titleBox, headerBox, scrollPane, menuBox);
            layoutVBox.setPadding(new Insets(0, 5, 0, 5));
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene walletsScene = new Scene(layoutVBox, getStageWidth(), getStageHeight());

            addButton.setOnAction(event -> {
                walletsDataList.showAddWalletStage();
            });

            scrollPane.prefViewportWidthProperty().bind(walletsScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(walletsScene.heightProperty().subtract(140));

            walletsDataList.gridWidthProperty().bind(walletsScene.widthProperty().subtract(40));

            VBox walletsBox = walletsDataList.getButtonGrid();

            walletsBox.prefWidthProperty().bind(walletsScene.widthProperty().subtract(25));
            
            scrollPane.setContent(walletsBox);
            scrollPane.setPadding(new Insets(5, 5, 5, 5));

            addButton.prefWidthProperty().bind(walletsScene.widthProperty().divide(2));
            removeButton.prefWidthProperty().bind(walletsScene.widthProperty().divide(2));

            walletsScene.getStylesheets().add("/css/startWindow.css");
            m_walletsStage.setScene(walletsScene);

            m_walletsStage.show();

            Runnable update = () -> {
                Platform.runLater(() -> getLastUpdated().set(LocalDateTime.now()));
            };

            toggleGridTypeButton.setOnAction(e -> {

                if (getStageIconStyle().equals(IconStyle.ICON)) {
                    setStageIconStyle(IconStyle.ROW);
                    walletsDataList.iconStyleProperty().set(IconStyle.ROW);
                } else {
                    setStageIconStyle(IconStyle.ICON);
                    walletsDataList.iconStyleProperty().set(IconStyle.ICON);
                }
                update.run();
            });

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            walletsScene.widthProperty().addListener((obs, oldVal, newVal) -> {
                setStageWidth(newVal.doubleValue());
                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(update, EXECUTION_TIME, TimeUnit.MILLISECONDS);

            });
            walletsScene.heightProperty().addListener((obs, oldVal, newVal) -> {
                setStageHeight(newVal.doubleValue());
                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }
                m_lastExecution = executor.schedule(update, EXECUTION_TIME, TimeUnit.MILLISECONDS);

            });

            walletsScene.focusOwnerProperty().addListener((obs, oldval, newval) -> {

                if (walletsScene.focusOwnerProperty().get() instanceof IconButton) {
                    IconButton iconBtn = (IconButton) walletsScene.focusOwnerProperty().get();

                    if (iconBtn.getButtonId() != null) {
                        removeButton.setDisable(false);
                        removeButton.setId("menuBarBtn");
                        walletsScene.setUserData(iconBtn.getButtonId());
                    } else {
                        removeButton.setDisable(true);
                        removeButton.setId("menuBarBtnDisabled");
                    }
                } else {

                    if (walletsScene.focusOwnerProperty().get() instanceof Button) {
                        Button btn = (Button) walletsScene.focusOwnerProperty().get();
                        if (!btn.getText().equals("Remove")) {
                            removeButton.setDisable(true);
                            removeButton.setId("menuBarBtnDisabled");
                        } else {

                            if (oldval instanceof IconButton) {
                                IconButton button = (IconButton) oldval;

                                String networkId = button.getButtonId();
                                if (networkId != null) {
                                    walletsDataList.remove(networkId);
                                    walletsDataList.save();
                                }
                            }
                        }
                    } else {
                        removeButton.setDisable(true);
                        removeButton.setId("menuBarBtnDisabled");
                    }
                }
            });

            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_walletsStage, 200, 200, rect.getWidth(), rect.getHeight());

        } else {
            if (m_walletsStage.isIconified()) {
                m_walletsStage.setIconified(false);
            }
        }

    }

    public void setupWallet() {

        m_appDir = m_appDir == null ? new File(m_ergoNetwork.getAppDir().getAbsolutePath() + "/" + NAME) : m_appDir;

        m_walletsDir = m_walletsDir == null ? new File(m_appDir.getAbsolutePath() + "/wallets") : m_walletsDir;
        m_dataFile = new File(m_appDir.getAbsolutePath() + "/ergoWallets.dat");
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

    public void addListeners(){

        getNetworksData().getAppData().appKeyProperty().addListener((obs,oldval,newval)->{
            try {
                File dataFile = getDataFile();
                if(dataFile.isFile()){
                    try {
                        JsonObject dataFileJson = Utils.readJsonFile(oldval, dataFile.toPath());
                        Utils.saveJson(newval, dataFileJson, dataFile);

                        JsonElement dataFilesElement = dataFileJson.get("dataFiles");
                        if(dataFilesElement != null && dataFilesElement.isJsonArray()){
                            JsonArray dataFilesArray = dataFilesElement.getAsJsonArray();

                            for(int i = 0; i < dataFilesArray.size() ; i++){
                                JsonElement dataFileElement = dataFilesArray.get(i);

                                if(dataFileElement != null && dataFileElement.isJsonObject()){
                                    JsonObject dataFileObject = dataFileElement.getAsJsonObject();

                                    JsonElement fileElement = dataFileObject.get("file");
                                    if(fileElement != null && fileElement.isJsonPrimitive()){
                                        File file = new File(fileElement.getAsString());
                                        if(file.isFile()){
                                            JsonObject fileObject = Utils.readJsonFile(oldval, file.toPath());
                                            Utils.saveJson(newval, fileObject, file);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                            | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                        try {
                            Files.writeString(logFile.toPath(),"Error updating wallets datafile key: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {
                        
                        }
                     
                    }

                }
            } catch (IOException e) {
             
            }
            
        });
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
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getText(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }

    
}
