package com.netnotes;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import org.apache.commons.io.FileUtils;
import org.ergoplatform.appkit.NetworkType;
import org.reactfx.util.FxTimer;

import com.utils.Utils;
import com.utils.Version;

import javafx.application.Platform;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.google.gson.JsonObject;
import com.netnotes.ErgoNodeConfig.BlockchainMode;
import com.netnotes.ErgoNodeConfig.DigestAccess;
import com.netnotes.ErgoNodeConfig.ConfigMode;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class ErgoNodeLocalData extends ErgoNodeData {

    public final static String DEFAULT_NODE_NAME = "Local Node";
    public final static String DEFAULT_CONFIG_NAME = "ergo.conf";
    public final static int MAX_CONSOLE_ROWS = 200;
    public final static int MAX_INPUT_BUFFER_SIZE = 30;

    final private List<ErgoNodeMsg> m_nodeMsgBuffer = Collections.synchronizedList(new ArrayList<ErgoNodeMsg>());

    private File logFile = new File("ergoLocalNode-log.txt");

    //  public SimpleStringProperty nodeApiAddress;
    private String GitHub_LATEST_URL = "https://api.github.com/repos/ergoplatform/ergo/releases/latest";

    private String m_setupImgUrl = "/assets/open-outline-white-20.png";

    private boolean m_runOnStart = false;

    private File m_appDir = null;
    private String m_appFileName = null;
    private HashData m_appFileHashData = null;
    private String m_execParams = "";

    private long m_spaceRequired = 50L * (1024L * 1024L * 1024L);
    private Stage m_stage = null;
    private Stage m_configStage = null;
    public final static long EXECUTION_TIME = 500;

    public double SETUP_STAGE_WIDTH = 700;
    public double SETUP_STAGE_HEIGHT = 580;

    public double CORE_SETUP_STAGE_WIDTH = 700;
    public double CORE_SETUP_STAGE_HEIGHT = 395;

    public double SETTINGS_STAGE_WIDTH = 1024;
    public double SETTINGS_STAGE_HEIGHT = 768;

    private ErgoNodeConfig m_nodeConfigData = null;
    
    
    private ExecutorService m_executor = null;
    private Future<?> m_future = null;
    private ScheduledFuture<?> m_scheduledFuture = null;
    private long m_pid = -1;
    private SimpleStringProperty m_consoleOutputProperty = new SimpleStringProperty("");

    public final SimpleBooleanProperty syncedProperty = new SimpleBooleanProperty(false);
    public final SimpleLongProperty nodeBlockHeightProperty = new SimpleLongProperty(-1);
    public final SimpleLongProperty networkBlockHeightProperty = new SimpleLongProperty(-1);
    private SimpleObjectProperty<Version> m_appVersion = new SimpleObjectProperty<Version>(new Version());

    private AtomicReference<String> m_lastNodeMsgId = new AtomicReference<>(null);
    private AtomicInteger m_inputCycleIndex = new AtomicInteger(0);
    public final AtomicBoolean isGettingInfo = new AtomicBoolean(false);
    public long INPUT_CYCLE_PERIOD = 100;

    
    
    private boolean m_deleteOldFiles = true;

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);

            return t;
        }
    });

    public ErgoNodeLocalData(String id, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, LOCAL_NODE, new NamedNodeUrl(id, "not installed", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));

        setListeners();

    }

    public ErgoNodeLocalData(NamedNodeUrl namedNode, JsonObject json, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, LOCAL_NODE, namedNode);
        openJson(json);
        setListeners();
        if(m_runOnStart){
            start();
        }
    }

    private void setListeners() {
        getErgoNodesList().getErgoNodes().getNetworksData().shutdownNowProperty().addListener((obs, oldVal, newVal) -> stop());
        // syncedProperty.bind(Bindings.equal(nodeBlockHeightProperty, networkBlockHeightProperty).and(networkBlockHeightProperty.isNotEqualTo(-1L).and(statusProperty.isNotEqualTo(MarketsData.STOPPED))));
        statusProperty.addListener((obs, oldval, newVal) -> {
            if (newVal != null && newVal.equals(ErgoMarketsData.STOPPED)) {
                Platform.runLater(() -> cmdProperty.set(""));
            }
        });
        Runnable syncUpdate = () -> {
            long nodeBlockHeight = nodeBlockHeightProperty.get();
            long networkBlockHeight = networkBlockHeightProperty.get();
            boolean running = statusProperty.get() != null && !statusProperty.get().equals(ErgoMarketsData.STOPPED);

            if (nodeBlockHeight != -1 && networkBlockHeight != -1 && nodeBlockHeight == networkBlockHeight && running) {
                syncedProperty.set(true);
            } else {
                if (nodeBlockHeight > networkBlockHeight && !isGettingInfo.get() && running) {
                    updateCycle();
                }
            }
        };
        nodeBlockHeightProperty.addListener((obs, oldVal, newVal) -> Platform.runLater(() -> syncUpdate.run()));
        networkBlockHeightProperty.addListener((obs, oldval, newVal) -> Platform.runLater(() -> syncUpdate.run()));
        syncUpdate.run();
    }

    public void coreFileError() {
        Alert a = new Alert(AlertType.WARNING, "Local node core file has been altered.", ButtonType.OK);
        a.setHeaderText("Error: Core Mismatch");
        a.setTitle("Error: Core File Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    public void configFileError() {
        Alert a = new Alert(AlertType.WARNING, "Local node config file has been altered.", ButtonType.OK);
        a.setHeaderText("Error: Config Mismatch");
        a.setTitle("Error: Config Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    @Override
    public void openJson(JsonObject jsonObj) {
        
        if (jsonObj != null) {
            /*try {
                Files.writeString(logFile.toPath(), "\nJsonData: " + jsonObj.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
            
            }*/
            
            JsonElement isSetupElement = jsonObj.get("isSetup");
            JsonElement runOnStartElement = jsonObj.get("runOnStart");
            JsonElement appDirElement = jsonObj.get("appDir");
            JsonElement appFileNameElement = jsonObj.get("appFileName");
            JsonElement appFileHashDataElement = jsonObj.get("appFileHashData");
            JsonElement appExecParamsElement = jsonObj.get("appExecParams");
            JsonElement appVersionElement = jsonObj.get("appVersion");
            JsonElement configElement = jsonObj.get("config");

            File appDir = appDirElement != null && appDirElement.isJsonPrimitive() ? new File(appDirElement.getAsString()) : null;

            m_appDir = appDir != null && appDir.isDirectory() ? appDir : null;
            isSetupProperty.set(isSetupElement != null && isSetupElement.isJsonPrimitive() ? isSetupElement.getAsBoolean() : false);
            m_runOnStart = runOnStartElement != null && runOnStartElement.isJsonPrimitive() ? runOnStartElement.getAsBoolean() : false;
            m_appFileHashData = null;
            m_nodeConfigData = null;

            String appFileName = m_appDir != null && appFileNameElement != null && appFileNameElement.isJsonPrimitive() ? appFileNameElement.getAsString() : null;
            File appFile = appFileName != null ? new File(m_appDir.getAbsolutePath() + "/" + appFileName) : null;

            if (appFile != null && appFile.isFile() && appFileHashDataElement != null && appFileHashDataElement.isJsonObject() && configElement != null && configElement.isJsonObject()) {
                m_appFileName = appFileName;
                boolean isCorrectHash = false;

                try {
                    m_appFileHashData = new HashData(appFileHashDataElement.getAsJsonObject());
                    m_appVersion.set(appVersionElement != null && appVersionElement.isJsonPrimitive() ? new Version(appVersionElement.getAsString()) : new Version());

                    HashData appFileHashData = new HashData(appFile);
/*
                    Files.writeString(logFile.toPath(), "\nAppfiledata: " +appFileHashData.getJsonObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
*/
                    isCorrectHash = m_appFileHashData.getHashStringHex().equals(appFileHashData.getHashStringHex());
                    
                } catch (Exception e) {
                    try {
                        Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {
              
                    }
                }
                
                if (isCorrectHash) {

                    try {
                        m_nodeConfigData = new ErgoNodeConfig(configElement.getAsJsonObject(), m_appDir);
                        
                        if (m_nodeConfigData != null && m_nodeConfigData.getConfigFile() != null && m_nodeConfigData.getConfigFile().isFile() &&  m_nodeConfigData.getConfigFileHashData() != null) {
                           
                            HashData configFileHashData = new HashData(m_nodeConfigData.getConfigFile());
                        
                            String nodeConfigHash = m_nodeConfigData.getConfigFileHashData().getHashStringHex();
                            if (nodeConfigHash.equals(configFileHashData.getHashStringHex()) ) {

                                if (m_runOnStart) {
                                    runNode(m_nodeConfigData.getConfigFile(), appFile);
                                }
                            } else {
                                
                                m_runOnStart = false;
                                isSetupProperty.set(false);
                                configFileError();
                            }
                      
                        } else {
                    
                            m_runOnStart = false;
                            isSetupProperty.set(false);
                            configFileError();
                        }
                     } catch (Exception e) {
                        m_runOnStart = false;
                        isSetupProperty.set(false);
                        configFileError();
                        
                    }
                   

                } else {
                    m_runOnStart = false;
                    m_appFileHashData = null;
                    m_nodeConfigData = null;

                    if (isSetupProperty.get()) {
                        isSetupProperty.set(false);
                        coreFileError();
                    }
                }

            }

            if(appExecParamsElement != null && appExecParamsElement.isJsonPrimitive()){
                m_execParams = appExecParamsElement.getAsString();
            }

         

        }

    }

    public long getSpaceRequired() {
        return m_spaceRequired;
    }

    public boolean getRunOnStart() {
        return m_runOnStart;

    }

    public String getInstallImgUrl() {
        return m_setupImgUrl;
    }

    public File getAppDir() {
        return m_appDir;
    }

    public File getAppFile() {
        return m_appDir != null && m_appDir.isDirectory() && m_appFileName != null ? new File(m_appDir.getAbsolutePath() + "/" + m_appFileName) : null;
    }

    private final SimpleIntegerProperty peerCountProperty = new SimpleIntegerProperty(-1);

    private final void updateCycle() {
        //get Network info
        final String localApiString = namedNodeUrlProperty.get().getUrlString() + "/info";
        final String prevVersionString = m_appVersion.get().get();

        isGettingInfo.set(true);
        Utils.getUrlJson(localApiString, (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                JsonObject json = (JsonObject) sourceObject;

                //JsonElement fullHeightElement = json.get("fullHeight");
                JsonElement maxPeerHeightElement = json.get("maxPeerHeight");
                JsonElement peerCountElement = json.get("peersCount");
                //  JsonElement headerHeightElement = json.get("headerHeight");
                JsonElement appVersionElement = json.get("appVersion");

                try {
                    Files.writeString(new File(m_appDir.getAbsolutePath() + "/info.json").toPath(), json.toString());
                } catch (IOException e) {
                
                }

                // long headerHeight = headerHeightElement != null && headerHeightElement.isJsonPrimitive() ? headerHeightElement.getAsLong() : -1;
                //long fullHeight = fullHeightElement != null && fullHeightElement.isJsonPrimitive() ? fullHeightElement.getAsLong() : -1;
                final long networkHeight = maxPeerHeightElement != null && maxPeerHeightElement.isJsonPrimitive() ? maxPeerHeightElement.getAsLong() : -1;
                final int peerCount = peerCountElement != null && peerCountElement.isJsonPrimitive() ? peerCountElement.getAsInt() : -1;
                final String appVersionString = appVersionElement != null && appVersionElement.isJsonPrimitive() ? appVersionElement.getAsString() : null;

                Platform.runLater(() -> {
                    cmdStatusUpdated.set(String.format("%29s", Utils.formatDateTimeString(LocalDateTime.now())));
                    peerCountProperty.set(peerCount);
                    networkBlockHeightProperty.set(networkHeight);

                    if(appVersionString != null && !prevVersionString.equals(appVersionString)){
                        try{
   
                            m_appVersion.set(new Version(appVersionString));
                            lastUpdated.set(LocalDateTime.now());
                             
                        }catch(IllegalArgumentException e){

                        }
                    }
                
                });
                isGettingInfo.set(false);
            }
        }, (onFailed) -> {
            //String errMsg = onFailed.getSource().getException().toString();
            Platform.runLater(() -> {
                networkBlockHeightProperty.set(-1);
                peerCountProperty.set(0);
            });
            isGettingInfo.set(false);
        }, null);

    }

    final private Runnable m_readNodeInput = () -> {
        int cycle = m_inputCycleIndex.incrementAndGet();
        if (cycle == 100) {
            m_inputCycleIndex.set(0);

            updateCycle();
        }

        ArrayList<ErgoNodeMsg> newMsgs = new ArrayList<ErgoNodeMsg>();
        if (m_nodeMsgBuffer.size() > 0) {
            synchronized (m_nodeMsgBuffer) {
                int j = m_nodeMsgBuffer.size() - 1;

                ErgoNodeMsg ergoNodeMsg = m_nodeMsgBuffer.get(j);
                String lastInputId = m_lastNodeMsgId.get();
                if (!(ergoNodeMsg.getId().equals(lastInputId))) {
                    m_lastNodeMsgId.set(lastInputId);
                    newMsgs.add(ergoNodeMsg);

                    j--;
                    while (j > 0 && !(ergoNodeMsg.getId().equals(lastInputId))) {
                        newMsgs.add(0, ergoNodeMsg);
                        ergoNodeMsg = m_nodeMsgBuffer.get(j);
                        j--;
                    }
                }

            }
        }
        int size = newMsgs.size();
        if (size > 0) {

            for (int i = 0; i < size; i++) {

                inputNodeMsg(newMsgs.get(i));
            }
        }

    };

    final private void inputNodeMsg(ErgoNodeMsg msg) {
        final String type = msg.getType();
        m_lastNodeMsgId.set(msg.getId());
        Platform.runLater(() -> {
            cmdProperty.set(type);
            statusString.set(msg.getBody());
            if (type.equals(ErgoNodeMsg.MsgTypes.NEW_HEIGHT)) {
                nodeBlockHeightProperty.set(msg.getHeight());
            }
            cmdStatusUpdated.set(String.format("%29s", msg.getDateTimeString()));
        });

    }

    public String getExecCmd(File appFile, File configFile){

         String networkTypeString = namedNodeUrlProperty.get().getNetworkType() == null ? NetworkType.MAINNET.toString() : namedNodeUrlProperty.get().getNetworkType().toString();

        String networkTypeFlag = networkTypeString.equals(NetworkType.MAINNET.toString()) ? "--mainnet" : "--testnet";

        String cmdPrefix = "java -jar " + appFile.getName() + " " + networkTypeFlag;
        
        String cmdPostfix = " -c " + configFile.getName();
        if(m_execParams == null){
            m_execParams = "";
        }
        String params = m_execParams.trim().length() > 0 ? " " +  m_execParams.trim() : "";

        return  cmdPrefix +  params  + cmdPostfix;
    }

    private void runNode(File appFile, File configFile) {
        if (m_executor == null) {
            m_executor = Executors.newSingleThreadExecutor();

        }

        if ((m_future == null || m_future != null && m_future.isDone()) && m_pid == -1) {
           



            String cmd = "cmd /c " + getExecCmd(appFile, configFile);


            m_future = m_executor.submit(new Runnable() {

                /*private final ExecutorService executor = Executors.newCachedThreadPool(
                        new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });
                private Future<?> lastExecution = null;*/
                @Override
                public void run() {

                    try {
                        Process proc = Runtime.getRuntime().exec(cmd, null, appFile.getParentFile());
                        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                        if (proc.isAlive()) {
                            Platform.runLater(() -> statusProperty.set(ErgoMarketsData.STARTED));
                            if (m_scheduledFuture == null || (m_scheduledFuture != null && m_scheduledFuture.isDone())) {
                                m_scheduledFuture = scheduledExecutor.scheduleAtFixedRate(m_readNodeInput, 0, INPUT_CYCLE_PERIOD, TimeUnit.MILLISECONDS);
                            }

                        }

                        String s = null;
                        try {
                            s = stdInput.readLine();
                        } catch (IOException e) {

                        }

                        while (s != null) {
                            try {

                                s = stdInput.readLine();
                                String str = s;
                                synchronized (m_nodeMsgBuffer) {
                                    if (m_nodeMsgBuffer.size() > MAX_INPUT_BUFFER_SIZE) {
                                        m_nodeMsgBuffer.remove(0);
                                    }
                                    m_nodeMsgBuffer.add(new ErgoNodeMsg(str));
                                }
                            } catch (IOException e) {

                                proc.waitFor(2, TimeUnit.SECONDS);
                                try {
                                    s = stdInput.readLine();
                                    String str = s;
                                    synchronized (m_nodeMsgBuffer) {
                                        if (m_nodeMsgBuffer.size() > MAX_INPUT_BUFFER_SIZE) {
                                            m_nodeMsgBuffer.remove(0);
                                        }
                                        m_nodeMsgBuffer.add(new ErgoNodeMsg(str));
                                    }
                                } catch (IOException e1) {

                                }

                            }

                        }

                    } catch (Exception e) {

                    }
                    //   m_pid = -1;
                    //   statusProperty().set(MarketsData.STOPPED);
                }

            });

        }

        //t.start();
    }

    @Override
    public void start() {
        if (isSetupProperty.get()) {
            Runnable runError = () -> {
                Platform.runLater(() -> {
                    isSetupProperty.set(false);
                    statusProperty.set(ErgoMarketsData.STOPPED);
                    networkBlockHeightProperty.set(-1);
                });
            };

            Platform.runLater(() -> statusProperty.set(ErgoMarketsData.STARTING));
            File appFile = getAppFile();

            File configFile = m_nodeConfigData != null ? m_nodeConfigData.getConfigFile() : null;
          
           
            if (appFile != null && appFile.isFile() && configFile != null && configFile.isFile() && m_appFileHashData != null && m_nodeConfigData != null  && m_nodeConfigData.getConfigFileHashData()  != null) {
                /*
                try {
                    Files.writeString(logFile.toPath(), "\nappFile: " + m_appFileHashData.getHashStringHex() + "\nconfigFile: " + m_nodeConfigData.getConfigFileHashData().getHashStringHex(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
            
                }*/
                try {
                    HashData appFileHashData = new HashData( Utils.digestFile(appFile));
                    HashData configFileHashData = new HashData(Utils.digestFile(configFile));
               

                    if (m_appFileHashData.getHashStringHex().equals(appFileHashData.getHashStringHex())) {
                        if (m_nodeConfigData.getConfigFileHashData().getHashStringHex().equals(configFileHashData.getHashStringHex())) {
                            runNode(appFile, configFile);
                        } else {
                            runError.run();
                            configFileError();
                        }

                    } else {

                        runError.run();
                        coreFileError();
                    }

                 } catch (Exception e) {
                    runError.run();
                }
            } else {
                runError.run();
            }

        }

    }

    private Scene initialSetupScene(Button nextBtn, MenuButton configModeBtn, MenuButton digestAccessBtn, MenuButton blockchainModeBtn, TextField apiKeyField, SimpleObjectProperty<File> configFileOption, SimpleObjectProperty<File> directoryRoot, TextField directoryNameField, Stage stage) {
        String titleString = "Setup - Local Node - " + ErgoNodes.NAME;
        stage.setTitle(titleString);

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 30;
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleString, closeBtn, stage);
        Text headingText = new Text("Setup");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text configText = new Text("Config");
        configText.setFill(App.txtColor);
        configText.setFont(App.txtFont);

        MenuItem simpleItem = new MenuItem(m_nodeConfigData != null ? m_nodeConfigData.getConfigMode() : ConfigMode.BASIC);
        simpleItem.setOnAction(e -> {
            configModeBtn.setText(simpleItem.getText());
        });

        MenuItem advancedItem = new MenuItem(ConfigMode.ADVANCED);
        advancedItem.setOnAction(e -> {
            configModeBtn.setText(advancedItem.getText());
        });

        configModeBtn.getItems().addAll(simpleItem, advancedItem);
        configModeBtn.setFont(App.txtFont);

        HBox configBox = new HBox(configText);
        configBox.setAlignment(Pos.CENTER_LEFT);
        configBox.setMinHeight(40);
        configBox.setId("headingBox");
        configBox.setPadding(new Insets(0, 0, 0, 15));

        Text apiKeyText = new Text(String.format("%-15s", " API Key"));
        apiKeyText.setFill(getPrimaryColor());
        apiKeyText.setFont((App.txtFont));

        apiKeyField.setPromptText("Enter key");
        apiKeyField.setId("formField");
        HBox.setHgrow(apiKeyField, Priority.ALWAYS);

        PasswordField apiKeyHidden = new PasswordField();
        apiKeyHidden.setPromptText("Enter key");
        apiKeyHidden.setId("formField");
        HBox.setHgrow(apiKeyHidden, Priority.ALWAYS);

        apiKeyHidden.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                apiKeyField.setText(apiKeyHidden.getText());
            }
        });

        BufferedButton showKeyBtn = new BufferedButton("/assets/eye-30.png", 20);

        Tooltip randomApiKeyTip = new Tooltip("Random API Key");

        Button randomApiKeyBtn = new BufferedButton("/assets/d6-30.png", 20);
        randomApiKeyBtn.setTooltip(randomApiKeyTip);
        randomApiKeyBtn.setOnAction(e -> {
            try {
                int length = Utils.getRandomInt(12, 20);
                char key[] = new char[length];
                for (int i = 0; i < length; i++) {
                    key[i] = (char) Utils.getRandomInt(33, 126);
                }
                String keyString = new String(key);
                apiKeyField.setText(keyString);
                apiKeyHidden.setText(keyString);
            } catch (NoSuchAlgorithmException e1) {
                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CANCEL);
                a.initOwner(stage);
                a.setHeaderText("Error");
                a.setTitle("Error - Setup - Local Node - " + ErgoNodes.NAME);
                a.show();
            }
        });

        HBox apiKeyBox = new HBox(apiKeyText, apiKeyHidden, showKeyBtn, randomApiKeyBtn);
        apiKeyBox.setPadding(new Insets(0, 0, 0, 15));;
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);

        apiKeyBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

        showKeyBtn.setOnAction(e -> {
            if (apiKeyBox.getChildren().contains(apiKeyHidden)) {
                apiKeyField.setText(apiKeyHidden.getText());
                apiKeyBox.getChildren().remove(apiKeyHidden);
                apiKeyBox.getChildren().add(1, apiKeyField);
                showKeyBtn.setImage(new Image("/assets/eye-off-30.png"));
            } else {
                apiKeyHidden.setText(apiKeyField.getText());
                apiKeyBox.getChildren().remove(apiKeyField);
                apiKeyBox.getChildren().add(1, apiKeyHidden);
                showKeyBtn.setImage(new Image("/assets/eye-30.png"));
            }
        });

        Text digestModeText = new Text(String.format("%-15s", " Transactions"));
        digestModeText.setFill(getPrimaryColor());
        digestModeText.setFont(App.txtFont);

        MenuItem localItem = new MenuItem(DigestAccess.LOCAL);

        localItem.setOnAction(e -> {
            digestAccessBtn.setText(localItem.getText());
        });
        MenuItem allItem = new MenuItem(DigestAccess.ALL);

        allItem.setOnAction(e -> {
            digestAccessBtn.setText(allItem.getText());
        });
        digestAccessBtn.getItems().addAll(localItem, allItem);
        digestAccessBtn.setId("formField");
        digestAccessBtn.setFont(App.txtFont);

        HBox digestModeBox = new HBox(digestModeText, digestAccessBtn);
        digestModeBox.setAlignment(Pos.CENTER_LEFT);
        digestModeBox.setPadding(new Insets(0, 0, 0, 15));
        digestModeBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(digestModeBox, Priority.ALWAYS);

        Text blockchainModeText = new Text(String.format("%-15s", " Blockchain"));
        blockchainModeText.setFill(getPrimaryColor());
        blockchainModeText.setFont(App.txtFont);

        MenuItem bootstrapItem = new MenuItem(BlockchainMode.PRUNED);

        bootstrapItem.setOnAction(e -> {
            blockchainModeBtn.setText(bootstrapItem.getText());
        });
        MenuItem latestItem = new MenuItem(BlockchainMode.RECENT_ONLY);

        latestItem.setOnAction(e -> {
            blockchainModeBtn.setText(latestItem.getText());
        });
        MenuItem fullItem = new MenuItem(BlockchainMode.FULL);

        fullItem.setOnAction(e -> {
            blockchainModeBtn.setText(fullItem.getText());
        });

        blockchainModeBtn.getItems().addAll(bootstrapItem, latestItem, fullItem);
        blockchainModeBtn.setFont(App.txtFont);

        HBox blockchainModeBox = new HBox(blockchainModeText, blockchainModeBtn);
        blockchainModeBox.setAlignment(Pos.CENTER_LEFT);
        blockchainModeBox.setPadding(new Insets(0, 0, 0, 15));
        blockchainModeBox.minHeightProperty().bind(rowHeight);

        HBox.setHgrow(blockchainModeBox, Priority.ALWAYS);

        /* Text noticeText = new Text(String.format("%-15s", "   Notice"));
        noticeText.setFill(getPrimaryColor());
        noticeText.setFont(App.txtFont);

        TextField noticeField = new TextField("An old wallet requires the full blockchain.");
        noticeField.setId("formField");
        noticeField.setEditable(false);
        HBox.setHgrow(noticeField, Priority.ALWAYS);

        HBox noticeBox = new HBox();
        noticeBox.setAlignment(Pos.CENTER_LEFT);
        noticeBox.setPadding(new Insets(0, 0, 0, 15));
        noticeBox.minHeightProperty().bind(rowHeight);*/
        Text modeText = new Text(String.format("%-9s", "Mode"));
        modeText.setFill(App.txtColor);
        modeText.setFont((App.txtFont));

        HBox configModeBox = new HBox(modeText, configModeBtn);
        configModeBox.setAlignment(Pos.CENTER_LEFT);
        configModeBox.setPadding(new Insets(0, 0, 0, 15));
        configModeBox.setMinHeight(40);

        VBox modeOptionsBodyBox = new VBox(apiKeyBox, digestModeBox, blockchainModeBox);
        modeOptionsBodyBox.setPadding(new Insets(0, 0, 0, 15));

        Text advFileModeText = new Text(String.format("%-8s", "File"));
        advFileModeText.setFill(getPrimaryColor());
        advFileModeText.setFont(App.txtFont);

        Button advFileModeBtn = new Button("Browse...");
        advFileModeBtn.setFont(App.txtFont);
        advFileModeBtn.setId("rowBtn");
        HBox.setHgrow(advFileModeBtn, Priority.ALWAYS);

        advFileModeBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select location");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Config (text)", "*.conf", "*.config", "*.cfg"));
            File configFile = chooser.showOpenDialog(stage);
            if (configFile != null && configFile.isFile()) {
                configFileOption.set(configFile);
                advFileModeBtn.setText(configFile.getAbsolutePath());
            }
        });

        HBox advFileModeBox = new HBox(advFileModeText, advFileModeBtn);
        advFileModeBox.setAlignment(Pos.CENTER_LEFT);
        advFileModeBox.setPadding(new Insets(0, 0, 0, 30));
        advFileModeBox.minHeightProperty().bind(rowHeight);

        VBox modeBodyBox = new VBox(configModeBox, modeOptionsBodyBox);
        modeBodyBox.setPadding(new Insets(15));
        modeBodyBox.setId("bodyBox");
        HBox.setHgrow(modeBodyBox, Priority.ALWAYS);

        VBox modeBox = new VBox(configBox, modeBodyBox);
        modeBox.setPadding(new Insets(0, 0, 15, 0));

        //configModeBtn.prefWidthProperty().bind(configModeBox.widthProperty().subtract(configModeText.layoutBoundsProperty().get().getWidth()));
        Text directoryText = new Text("Directory");
        directoryText.setFill(App.txtColor);
        directoryText.setFont(App.txtFont);

        HBox directoryBox = new HBox(directoryText);
        directoryBox.setAlignment(Pos.CENTER_LEFT);
        directoryBox.setMinHeight(40);;
        directoryBox.setId("headingBox");
        directoryBox.setPadding(new Insets(0, 0, 0, 15));

        Text directoryRootText = new Text(String.format("%-9s", "Location"));
        directoryRootText.setFill(App.txtColor);
        directoryRootText.setFont(App.txtFont);

        Text directoryNameText = new Text(String.format("%-9s", "Folder"));
        directoryNameText.setFill(App.txtColor);
        directoryNameText.setFont(App.txtFont);

        directoryNameField.setFont(App.txtFont);
        directoryNameField.setId("formField");
        HBox.setHgrow(directoryNameField, Priority.ALWAYS);

        HBox directoryNameBox = new HBox(directoryNameText, directoryNameField);
        directoryNameBox.setAlignment(Pos.CENTER_LEFT);
        directoryNameBox.setPadding(new Insets(0, 0, 0, 15));
        directoryNameBox.minHeightProperty().bind(rowHeight);

        Button directoryRootBtn = new Button();
        directoryRootBtn.setFont(App.txtFont);
        directoryRootBtn.setId("rowBtn");
        HBox.setHgrow(directoryRootBtn, Priority.ALWAYS);
        directoryRootBtn.textProperty().bind(directoryRoot.asString());
        directoryRootBtn.setOnAction(e -> {

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select location");
            chooser.setInitialDirectory(directoryRoot.get());

            File locationDir = chooser.showDialog(stage);
            if (locationDir != null && locationDir.isDirectory()) {
                directoryRoot.set(locationDir);
            }
        });

        HBox directoryRootBox = new HBox(directoryRootText, directoryRootBtn);
        directoryRootBox.setPadding(new Insets(0, 0, 0, 15));
        directoryRootBox.setAlignment(Pos.CENTER_LEFT);
        directoryRootBox.minHeightProperty().bind(rowHeight);

        Text useableText = new Text(" Available Space  ");
        useableText.setFill(getPrimaryColor());
        useableText.setFont(App.txtFont);

        TextField useableField = new TextField(Utils.formatedBytes(directoryRoot.get().getUsableSpace(), 2));
        useableField.setFont(App.txtFont);
        useableField.setId("formField");
        useableField.setEditable(false);
        HBox.setHgrow(useableField, Priority.ALWAYS);

        directoryRoot.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                useableField.setText(Utils.formatedBytes(directoryRoot.get().getUsableSpace(), 2));
            } else {
                useableField.setText("-");
            }
        });

        HBox useableBox = new HBox(useableText, useableField);
        useableBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(useableBox, Priority.ALWAYS);
        useableBox.setPadding(new Insets(0, 0, 0, 15));
        useableBox.setAlignment(Pos.CENTER_LEFT);

        Text requiredText = new Text(" Required Space   ");
        requiredText.setFill(getPrimaryColor());
        requiredText.setFont(App.txtFont);

        final String advSpaceRequiredString = "~100 Mb - >50 Gb";

        TextField requiredField = new TextField("~500 MB");
        requiredField.setFont(App.txtFont);
        requiredField.setId("formField");
        requiredField.setEditable(false);
        HBox.setHgrow(requiredField, Priority.ALWAYS);

        Runnable estimateSpaceRequired = () -> {
            switch (blockchainModeBtn.getText()) {
                case BlockchainMode.RECENT_ONLY:
                    requiredField.setText("~100 Mb");
                    break;
                case BlockchainMode.PRUNED:
                    requiredField.setText("~500 MB");
                    break;
                case BlockchainMode.FULL:
                    requiredField.setText(">50 GB");
                    break;
            }
        };
        blockchainModeBtn.textProperty().addListener((obs, oldval, newval) -> {
            estimateSpaceRequired.run();
        });

        configModeBtn.textProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case ConfigMode.ADVANCED:
                    requiredField.setText(advSpaceRequiredString);
                    if (modeBodyBox.getChildren().contains(modeOptionsBodyBox)) {
                        modeBodyBox.getChildren().remove(modeOptionsBodyBox);
                    }

                    if (!modeBodyBox.getChildren().contains(advFileModeBox)) {
                        modeBodyBox.getChildren().add(advFileModeBox);
                    }

                    break;
                default:
                    estimateSpaceRequired.run();
                    if (modeBodyBox.getChildren().contains(advFileModeBox)) {
                        modeBodyBox.getChildren().remove(advFileModeBox);
                    }
                    if (!modeBodyBox.getChildren().contains(modeOptionsBodyBox)) {
                        modeBodyBox.getChildren().add(modeOptionsBodyBox);
                    }

                    break;
            }
        });

        HBox requiredBox = new HBox(requiredText, requiredField);
        requiredBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(requiredBox, Priority.ALWAYS);
        requiredBox.setPadding(new Insets(0, 0, 0, 15));
        requiredBox.setAlignment(Pos.CENTER_LEFT);

        VBox directorySpaceBox = new VBox(useableBox, requiredBox);
        directorySpaceBox.setPadding(new Insets(0, 0, 0, 105));

        VBox directoryBodyBox = new VBox(directoryNameBox, directoryRootBox, directorySpaceBox);
        directoryBodyBox.setPadding(new Insets(15));
        directoryBodyBox.setId("bodyBox");
        HBox.setHgrow(directoryBodyBox, Priority.ALWAYS);

        HBox padBox = new HBox(directoryBodyBox);
        padBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(padBox, Priority.ALWAYS);

        VBox directoryPaddingBox = new VBox(directoryBox, padBox);
        HBox.setHgrow(directoryPaddingBox, Priority.ALWAYS);

        nextBtn.setPadding(new Insets(5, 15, 5, 15));
        HBox nextBox = new HBox(nextBtn);

        nextBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(nextBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(modeBox, directoryPaddingBox, nextBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);
        
        

        VBox layoutBox = new VBox(titleBox, headerBox, bodyPaddingBox , footerBox);
        Scene setupNodeScene = new Scene(layoutBox, SETUP_STAGE_WIDTH, SETUP_STAGE_HEIGHT);
        setupNodeScene.getStylesheets().add("/css/startWindow.css");

        Runnable closeStage = () -> {
            stage.close();
            m_stage = null;
        };

        


        closeBtn.setOnAction(e -> closeStage.run());
        m_stage.setOnCloseRequest(e -> closeStage.run());
        return setupNodeScene;
    }

    private Scene getFinalSetupScene(Button nextBtn, Button backBtn, SimpleObjectProperty<File> jarFile, SimpleBooleanProperty getLatestBoolean, SimpleStringProperty downloadUrlProperty, SimpleStringProperty downloadFileName, Stage stage) {

        String titleString = "Core File - Setup - Local Node - " + ErgoNodes.NAME;
        stage.setTitle(titleString);

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 30;
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleString, closeBtn, stage);
        Text headingText = new Text("Setup");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text appFileText = new Text("Core File");
        appFileText.setFill(App.txtColor);
        appFileText.setFont(App.txtFont);

        HBox appFileBox = new HBox(appFileText);
        appFileBox.setPadding(new Insets(0, 0, 0, 15));
        appFileBox.setAlignment(Pos.CENTER_LEFT);
        appFileBox.setId("headingBox");
        appFileBox.setMinHeight(defaultRowHeight);

        BufferedButton latestJarRadio = new BufferedButton("/assets/radio-button-on-30.png", 15);

        Text latestJarText = new Text(String.format("%-10s", "Download"));
        latestJarText.setFill(App.txtColor);
        latestJarText.setFont((App.txtFont));
        latestJarText.setOnMouseClicked(e -> {
            getLatestBoolean.set(true);
        });

        TextField latestJarNameField = new TextField("ergo-5.0.13.jar");
        latestJarNameField.setFont(App.txtFont);
        latestJarNameField.setId("formField");
        latestJarNameField.setEditable(false);
        HBox.setHgrow(latestJarNameField, Priority.ALWAYS);

        TextField latestJarUrlField = new TextField();
        latestJarUrlField.setFont(App.txtFont);
        latestJarUrlField.setId("formField");
        latestJarUrlField.setEditable(false);
        HBox.setHgrow(latestJarUrlField, Priority.ALWAYS);

        latestJarUrlField.textProperty().bind(downloadUrlProperty);

        Runnable getLatestUrl = () -> {

            Utils.getUrlJson(GitHub_LATEST_URL, (onSucceeded) -> {
                Object sourceObject = onSucceeded.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonObject) {
                    JsonObject gitHubApiJson = (JsonObject) sourceObject;

                    JsonElement assetsElement = gitHubApiJson.get("assets");
                    if (assetsElement != null && assetsElement.isJsonArray()) {
                        JsonArray assetsArray = assetsElement.getAsJsonArray();
                        if (assetsArray.size() > 0) {
                            JsonElement primaryAssetElement = assetsArray.get(0);

                            if (primaryAssetElement != null && primaryAssetElement.isJsonObject()) {
                                JsonObject assetObject = primaryAssetElement.getAsJsonObject();

                                JsonElement downloadUrlElement = assetObject.get("browser_download_url");
                                JsonElement nameElement = assetObject.get("name");

                                if (downloadUrlElement != null && downloadUrlElement.isJsonPrimitive()) {
                                    String url = downloadUrlElement.getAsString();
                                    String name = nameElement.getAsString();
                                    latestJarNameField.setText(name);
                                    downloadFileName.set(name);
                                    downloadUrlProperty.set(url);
                                    getLatestBoolean.set(true);

                                } else {
                                    latestJarNameField.setText("Asset url corrupt (try again ->)");
                                }
                            } else {
                                latestJarNameField.setText("Asset unavailable (try again ->)");
                            }
                        } else {
                            latestJarNameField.setText("Received empty assets list (try again ->");
                        }
                    } else {
                        latestJarNameField.setText("Received unexpected file format (try again ->)");
                    }

                } else {
                    latestJarNameField.setText("Unable to connect to GitHub (try again ->)");
                }
            }, onFailed -> {
                latestJarNameField.setText("Unable to connect to GitHub (try again ->)");
            }, null);
        };

        Tooltip downloadBtnTip = new Tooltip("Get GitHub Info");
        downloadBtnTip.setShowDelay(new Duration(200));

        BufferedButton downloadBtn = new BufferedButton("/assets/sync-30.png", 30);
        downloadBtn.setTooltip(downloadBtnTip);
        downloadBtn.setOnAction(e -> getLatestUrl.run());
        ErgoNodes ergoNodes = getErgoNodesList().getErgoNodes();
        Runnable downloadBtnEffect = () -> {
            if (!ergoNodes.getNetworksData().getAppData().getUpdates()) {
                if (downloadBtn.getBufferedImageView().getEffect("updateEffectId") == null) {
                    downloadBtn.getBufferedImageView().applyEffect(new InvertEffect("updateEffectId", 0.7));
                }
            } else {
                downloadBtn.getBufferedImageView().removeEffect("updateEffectId");
            }
        };
        /*
        ergoNodes.getNetworksData().getAppData().updatesProperty().addListener((obs, oldVal, newVal) -> {
            downloadBtnEffect.run();
            if (newVal.booleanValue()) {
                getLatestUrl.run();
            }

        }); */

        if (ergoNodes.getNetworksData().getAppData().getUpdates()) {
            getLatestUrl.run();
        }

        downloadBtnEffect.run();

        latestJarRadio.setOnAction(e -> {
            getLatestBoolean.set(true);
        });

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        HBox latestJarBox = new HBox(latestJarRadio, latestJarText, btnSpacer, downloadBtn);
        latestJarBox.setPadding(new Insets(0, 0, 0, 5));
        latestJarBox.setAlignment(Pos.CENTER_LEFT);
        latestJarBox.setMinHeight(defaultRowHeight);

        Text latestJarNameText = new Text(String.format("%-10s", "Name"));
        latestJarNameText.setFill(App.txtColor);
        latestJarNameText.setFont((App.txtFont));

        HBox latestJarNameBox = new HBox(latestJarNameText, latestJarNameField);
        latestJarNameBox.setPadding(new Insets(0, 0, 0, 45));
        latestJarNameBox.setAlignment(Pos.CENTER_LEFT);
        latestJarNameBox.minHeightProperty().bind(rowHeight);

        Text latestJarUrlText = new Text(String.format("%-10s", "Url"));
        latestJarUrlText.setFill(App.txtColor);
        latestJarUrlText.setFont((App.txtFont));

        HBox latestJarUrlBox = new HBox(latestJarUrlText, latestJarUrlField);
        latestJarUrlBox.setPadding(new Insets(0, 0, 0, 45));
        latestJarUrlBox.setAlignment(Pos.CENTER_RIGHT);
        latestJarUrlBox.minHeightProperty().bind(rowHeight);

        BufferedButton selectJarRadio = new BufferedButton("/assets/radio-button-off-30.png", 15);
        selectJarRadio.setOnAction(e -> {
            getLatestBoolean.set(false);
        });

        Text customText = new Text("Custom");
        customText.setFill(App.txtColor);
        customText.setFont((App.txtFont));
        customText.setOnMouseClicked(e -> {
            getLatestBoolean.set(false);
        });

        HBox customBox = new HBox(selectJarRadio, customText);
        customBox.setPadding(new Insets(0, 0, 0, 5));
        customBox.setAlignment(Pos.CENTER_LEFT);

        Text jarFileText = new Text(String.format("%-10s", "File"));
        jarFileText.setFill(App.txtColor);
        jarFileText.setFont((App.txtFont));

        Button jarFileBtn = new Button("Browse...");
        jarFileBtn.setId("rowBtn");
        jarFileBtn.setOnAction(e -> {

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Core File (*.jar)");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Ergo Node Jar", "*.jar"));

            File appFile = chooser.showOpenDialog(m_stage);
            if (appFile != null) {
                if (Utils.checkJar(appFile)) {
                    jarFileBtn.setText(appFile.getAbsolutePath());
                    getLatestBoolean.set(false);

                } else {
                    Alert a = new Alert(AlertType.NONE, "Unable to open file.", ButtonType.CANCEL);
                    a.initOwner(stage);
                    a.setHeaderText("Invalid file");
                    a.setTitle("Invalid File - Setup - " + ErgoNodes.NAME);
                    a.show();
                }
            }
        });

        HBox jarFileBox = new HBox(jarFileText, jarFileBtn);
        jarFileBox.setPadding(new Insets(0, 0, 0, 45));
        jarFileBox.setAlignment(Pos.CENTER_LEFT);
        jarFileBox.minHeightProperty().bind(rowHeight);

        getLatestBoolean.addListener((obs, oldVal, newVal) -> {
            if (newVal.booleanValue()) {
                latestJarRadio.setImage(new Image("/assets/radio-button-on-30.png"));
                selectJarRadio.setImage(new Image("/assets/"));
            } else {
                latestJarRadio.setImage(new Image("/assets/radio-button-off-30.png"));
                selectJarRadio.setImage(new Image("/assets/radio-button-on-30.png"));
            }
        });

        VBox jarBodyBox = new VBox(latestJarBox, latestJarNameBox, latestJarUrlBox, customBox, jarFileBox);
        jarBodyBox.setPadding(new Insets(15));
        jarBodyBox.setId("bodyBox");
        HBox.setHgrow(jarBodyBox, Priority.ALWAYS);

        HBox jarbodyPadBox = new HBox(jarBodyBox);
        jarbodyPadBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(jarbodyPadBox, Priority.ALWAYS);

        VBox jarBox = new VBox(appFileBox, jarbodyPadBox);
        HBox.setHgrow(jarBox, Priority.ALWAYS);
        Region smallRegion = new Region();
        smallRegion.setMinWidth(15);
        backBtn.setPadding(new Insets(5, 15, 5, 15));
        nextBtn.setPadding(new Insets(5, 15, 5, 15));
        HBox nextBox = new HBox(backBtn, smallRegion, nextBtn);
        nextBox.setMinHeight(35);
        nextBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(nextBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(jarBox, nextBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);

        VBox layoutBox = new VBox(titleBox, headerBox, bodyPaddingBox, footerBox);
        Scene setupNodeScene = new Scene(layoutBox, CORE_SETUP_STAGE_WIDTH, CORE_SETUP_STAGE_HEIGHT);
        setupNodeScene.getStylesheets().add("/css/startWindow.css");

        Runnable closeStage = () -> {
            stage.close();
            m_stage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        m_stage.setOnCloseRequest(e -> closeStage.run());
        return setupNodeScene;
    }



    public static long getRequiredSpace(String blockchainMode) {
        switch (blockchainMode) {
            case BlockchainMode.RECENT_ONLY:
                return 150L * 1024L * 1024L;

            case BlockchainMode.PRUNED:
                return 500L * 1024L * 1024L;

            case BlockchainMode.FULL:
            default:
                return 50L * 1024L * 1024L * 1024L;
        }
    }

    public boolean checkValidSetup() {
        File appDir = m_appDir;
        File configFile = m_nodeConfigData.getConfigFile();
        File appFile = getAppFile();

        return (appDir != null && appDir.isDirectory() && configFile != null && configFile.isFile() && appFile != null && appFile.isFile());
    }

    public void setup() {

        if (m_stage == null) {

            SimpleObjectProperty<File> directory = new SimpleObjectProperty<File>(getErgoNodesList().getErgoNodes().getAppDir());
            TextField folderNameField = new TextField(DEFAULT_NODE_NAME);

            TextField configApiKey = new TextField();
            MenuButton configModeBtn = new MenuButton(ConfigMode.BASIC);
            MenuButton configDigestAccess = new MenuButton(DigestAccess.LOCAL);
            MenuButton configBlockchainMode = new MenuButton(BlockchainMode.PRUNED);

            SimpleObjectProperty<File> configFileOption = new SimpleObjectProperty<>(null);

            Button nextBtn = new Button("Next");

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);

            Scene initialScene = initialSetupScene(nextBtn, configModeBtn, configDigestAccess, configBlockchainMode, configApiKey, configFileOption, directory, folderNameField, m_stage);
            m_stage.setScene(initialScene);

            m_stage.show();

            nextBtn.setOnAction(e -> {
                final String configMode = configModeBtn.getText();
                final String digestMode = configDigestAccess.getText();
                final String blockchainMode = configBlockchainMode.getText();
                final String apiKey = configApiKey.getText();
                final File dir = directory.get();
                String directoryString = dir.getAbsolutePath();

                final File installDir = new File(dir.getAbsolutePath() + "/" + folderNameField.getText());

                long useableSpace = directory.get().getUsableSpace();
                long requiredSpace = getRequiredSpace(blockchainMode);
                String errorMsg = "";

                if (!installDir.isDirectory()) {
                    try {
                        Files.createDirectory(installDir.toPath());

                    } catch (IOException e1) {
                        errorMsg = e1.toString();
                    }
                }
                if (installDir.isDirectory()) {
                    if (requiredSpace > useableSpace) {
                        Alert a = new Alert(AlertType.NONE, "The selected directory does not meet the space requirements.\n\nUseable space: " + Utils.formatedBytes(useableSpace, 2) + "\nRequired space: " + Utils.formatedBytes(requiredSpace, 2), ButtonType.OK);
                        a.initOwner(m_stage);
                        a.setHeaderText("Required Space");
                        a.setTitle("Required Space - Setup - Local Node - Ergo Nodes");
                        a.show();
                    } else {
                        if (configMode.equals(ConfigMode.BASIC)) {

                            install(configMode, DEFAULT_CONFIG_NAME, digestMode, blockchainMode, apiKey, installDir, initialScene);
                        } else {
                            File configFile = configFileOption.get();
                            if (configFile != null && configFile.isFile()) {
                                final String configFileName = configFile.getName();
                                File configParent = configFile.getParentFile();
                                File newConfig = new File(directoryString + "/" + configFileName);

                                String configFileErr = "Cannot find config file.";
                                if (!configParent.getAbsolutePath().equals(directoryString)) {

                                    try {
                                        Files.copy(configFile.toPath(), newConfig.toPath());
                                    } catch (IOException e1) {
                                        configFileErr = e1.toString();
                                    }
                                }
                                if (newConfig.isFile()) {
                                    install(configMode, configFileName, digestMode, blockchainMode, apiKey, installDir, initialScene);
                                } else {
                                    Alert a = new Alert(AlertType.NONE, configFileErr, ButtonType.OK);
                                    a.initOwner(m_stage);
                                    a.setHeaderText("Config File Error");
                                    a.setTitle("Config File Error - Setup - Local Node - Ergo Nodes");
                                    a.show();
                                }
                            } else {
                                Alert a = new Alert(AlertType.NONE, "Select an existing config text file, or select 'Basic' mode.", ButtonType.OK);
                                a.initOwner(m_stage);
                                a.setHeaderText("Config File");
                                a.setTitle("Config File - Setup - Local Node - Ergo Nodes");
                                a.show();
                            }
                        }
                    }
                } else {
                    Alert a = new Alert(AlertType.NONE, errorMsg, ButtonType.OK);
                    a.initOwner(m_stage);
                    a.setHeaderText("Directory Creation Error");
                    a.setTitle("Directory Creation Error - Setup - Local Node - Ergo Nodes");
                    a.show();
                }

            });

        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            m_stage.show();
            m_stage.toFront();
        }

    }

    public SimpleStringProperty consoleOutputProperty() {
        return m_consoleOutputProperty;
    }

    public void install(String configMode, String configFileName, String digestMode, String blockchainMode, String apiKeyString, File installDir, Scene initialScene) {
        if (m_stage == null) {
            setup();
        } else {
            Button installBtn = new Button("Install");
            Button backBtn = new Button("Back");

            // 
            SimpleBooleanProperty getLatestBoolean = new SimpleBooleanProperty(true);
            SimpleStringProperty downloadUrl = new SimpleStringProperty("https://github.com/ergoplatform/ergo/releases/download/v5.0.14/ergo-5.0.14.jar");
            SimpleStringProperty downloadFileName = new SimpleStringProperty("ergo-5.0.14.jar");
            SimpleObjectProperty<File> jarFile = new SimpleObjectProperty<File>(null);

            Scene finalSetupScene = getFinalSetupScene(installBtn, backBtn, jarFile, getLatestBoolean, downloadUrl, downloadFileName, m_stage);

            m_stage.setScene(finalSetupScene);

            backBtn.setOnAction(e -> {
                m_stage.setScene(initialScene);
            });

            installBtn.setOnAction(e -> {

                if (!getLatestBoolean.get() && jarFile.get() == null) {

                    Alert a = new Alert(AlertType.NONE, "Select a custom node file.", ButtonType.OK);
                    a.initOwner(m_configStage);
                    a.setHeaderText("Custom File");
                    a.setTitle("Custom File - Setup - Ergo Nodes");
                    a.show();

                } else {
                    ProgressBar progressBar = new ProgressBar();

                    boolean isDownload = getLatestBoolean.get();

                    Runnable installComplete = () -> {

                        m_stage.close();
                        m_stage = null;

                        try {
                            m_nodeConfigData = new ErgoNodeConfig(apiKeyString, configMode, digestMode, blockchainMode, configFileName, m_appDir);
                            namedNodeUrlProperty.set(new NamedNodeUrl(getId(), blockchainMode, "127.0.0.1", ErgoNodes.MAINNET_PORT, apiKeyString, NetworkType.MAINNET));
                            isSetupProperty.set(true);
                            lastUpdated.set(LocalDateTime.now());
                            start();

                        } catch (Exception e1) {
                            Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                            a.initOwner(m_stage);
                            a.setTitle("Config Creation Error - Setup - Ergo Nodes");
                            a.setHeaderText("Config Creation Error");
                            a.show();
                            m_stage.setScene(initialScene);

                        }

                        

                    };

                    if (installDir.isDirectory()) {

                        if (isDownload) {
                            File appFile = new File(installDir + "/" + downloadFileName.get());

                            Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, downloadFileName, progressBar, m_stage);
                            m_stage.setScene(progressScene);
                            Utils.getUrlFileHash(downloadUrl.get(), appFile, (onSucceeded) -> {
                                Object sourceObject = onSucceeded.getSource().getValue();
                                if (sourceObject != null && sourceObject instanceof HashData) {
                                    m_appDir = installDir;
                                    m_appFileName = downloadFileName.get();
                                    m_appFileHashData = (HashData) sourceObject;
                                    /*
                                    try {
                                        Files.writeString(logFile.toPath(), "\n" +m_appFileName +": " + m_appFileHashData.getJsonObject(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e1) {

                                    }*/
                                    installComplete.run();
                                } else {
                                    Alert a = new Alert(AlertType.NONE, "Check the download URL and destination path and then try again.", ButtonType.OK);
                                    a.initOwner(m_stage);
                                    a.setTitle("Download Failed - Setup - Ergo Nodes");
                                    a.setHeaderText("Download Failed");
                                    a.show();
                                    m_stage.setScene(initialScene);
                                }
                            }, (onFailed) -> {
                                String errorString = onFailed.getSource().getException().toString();

                                Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                a.initOwner(m_stage);
                                a.setTitle("Error - Setup - Ergo Nodes");
                                a.setHeaderText("Error");
                                a.show();
                                m_stage.setScene(initialScene);
                            }, progressBar);
                        } else {
                            File customFile = jarFile.get();

                            if (Utils.checkJar(customFile)) {

                                SimpleStringProperty fileNameProperty = new SimpleStringProperty(customFile.getName());
                                File appFile = new File(installDir.getAbsolutePath() + "/" + fileNameProperty.get());

                                Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, fileNameProperty, progressBar, m_stage);
                                m_stage.setScene(progressScene);

                                if (customFile.getAbsolutePath().equals(appFile.getAbsolutePath())) {
                              
                                    try {
                                       
                                        m_appDir = installDir;
                                        m_appFileName = fileNameProperty.get();
                                        m_appFileHashData = new HashData(appFile);

                                        installComplete.run();
                                    } catch(Exception er) {

                                        Alert a = new Alert(AlertType.NONE, "Error: " + er.toString(), ButtonType.OK);
                                        a.initOwner(m_stage);
                                        a.setTitle("Error - Setup - Ergo Nodes");
                                        a.setHeaderText("Error");
                                        a.show();

                                        m_stage.setScene(initialScene);
                                    }
                                } else {

                                    Utils.moveFileAndHash(customFile, appFile, onSucceeded -> {

                                        Object sourceObject = onSucceeded.getSource().getValue();
                                        if (sourceObject != null && sourceObject instanceof HashData) {
                                            m_appDir = installDir;
                                            m_appFileName = fileNameProperty.get();
                                            m_appFileHashData = (HashData) sourceObject;

                                            installComplete.run();
                                        } else {
                                            Alert a = new Alert(AlertType.NONE, "Check the selected file and destination path and then try again.", ButtonType.OK);
                                            a.initOwner(m_stage);
                                            a.setTitle("Download Failed - Setup - Ergo Nodes");
                                            a.setHeaderText("Download Failed");
                                            a.show();
                                            m_stage.setScene(initialScene);
                                        }

                                    }, onFailed -> {

                                        String errorString = onFailed.getSource().getException().toString();

                                        Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                        a.initOwner(m_stage);
                                        a.setTitle("Error - Setup - Ergo Nodes");
                                        a.setHeaderText("Error");
                                        a.show();
                                        m_stage.setScene(initialScene);

                                    }, progressBar);

                                }

                            } else {
                                Alert a = new Alert(AlertType.NONE, "Select a valid Ergo core file. (ergo-<Version>.jar)", ButtonType.OK);
                                a.initOwner(m_stage);
                                a.setTitle("Invalid Core File - Setup - Ergo Nodes");
                                a.setHeaderText("Invalid Core File");
                                a.show();

                                m_stage.setScene(initialScene);
                            }

                        }

                    } else {
                        Alert a = new Alert(AlertType.NONE, "File system cannote be accessed.", ButtonType.OK);
                        a.initOwner(m_stage);
                        a.setTitle("File System Error - Setup - Ergo Nodes");
                        a.setHeaderText("File System Error");
                        a.show();

                    }

                }

            });
        }

    }

    @Override
    public void stop() {

        if (m_scheduledFuture != null && !m_scheduledFuture.isDone()) {
            m_scheduledFuture.cancel(false);

        }
        if (!statusProperty.get().equals(ErgoMarketsData.STOPPED)) {

            Utils.wmicTerminate(m_appFileName);

            statusProperty.set(ErgoMarketsData.STOPPED);

        }

    }

    public boolean getIsSetup() {
        return isSetupProperty.get();
    }

    public void resetToDefault(){
        getErgoNodesList().remove(getId());

    }

    @Override
    public HBox getRowItem() {

        Button powerBtn = new Button();
        powerBtn.setGraphic(IconButton.getIconView(new Image(syncedProperty.get() ? getPowerOnUrl() : (statusProperty.get().equals(ErgoMarketsData.STOPPED) ? getPowerOffUrl() : getPowerInitUrl())), 15));
        powerBtn.setId("transparentColor");

        statusString.set(getIsSetup() ? "Offline" : "(Not Installed)");

        Text topInfoStringText = new Text();
        topInfoStringText.setFont(getFont());
        topInfoStringText.setFill(getPrimaryColor());
        topInfoStringText.textProperty().bind(namedNodeUrlProperty.asString());

        Text topRightText = new Text();
        topRightText.setFont(getSmallFont());
        topRightText.setFill(getSecondaryColor());

        Text botTimeText = new Text();
        botTimeText.setFont(getSmallFont());
        botTimeText.setFill(getSecondaryColor());
        botTimeText.textProperty().bind(cmdStatusUpdated);

        TextField centerField = new TextField();
        centerField.setFont(getLargeFont());
        centerField.setId("formField");
        centerField.setEditable(false);
        centerField.setAlignment(Pos.CENTER);
        centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(statusString);

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(getFont());
        middleTopRightText.setFill(getSecondaryColor());

        middleTopRightText.textProperty().bind(cmdProperty);

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(getFont());
        middleBottomRightText.setFill(getPrimaryColor());

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip("");
        statusBtnTip.setShowDelay(new Duration(100));
        //m_startImgUrl : m_stopImgUrl
        BufferedButton statusBtn = new BufferedButton(statusProperty.get().equals(ErgoMarketsData.STOPPED) ? (getIsSetup() ? getStartImgUrl() : getInstallImgUrl()) : getStopImgUrl(), 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (statusProperty.get().equals(ErgoMarketsData.STOPPED)) {
                if (getIsSetup()) {
                    start();
                } else {
                    setup();
                }
            } else {
                stop();

            }
        });

        HBox leftBox = new HBox(powerBtn);
        HBox rightBox = new HBox(statusBtn);

        leftBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        leftBox.setId("bodyBox");
        rightBox.setId("bodyBox");

        Region currencySpacer = new Region();
        currencySpacer.setMinWidth(10);

        HBox centerBox = new HBox(centerField, centerRightBox);
        centerBox.setPadding(new Insets(0, 5, 0, 5));
        centerBox.setAlignment(Pos.CENTER_RIGHT);
        centerBox.setId("darkBox");

        centerField.prefWidthProperty().bind(centerBox.widthProperty().subtract(centerRightBox.widthProperty()).subtract(20));

        Runnable checkStatus = () -> {

            String value = statusProperty.get() == null ? ErgoMarketsData.STOPPED : statusProperty.get();

            topRightText.setText(getAppFile() != null ? m_appFileName : "");

            if (value.equals(ErgoMarketsData.STOPPED)) {
                String stoppedString = getIsSetup() ? "Start" : "Setup";
                if (!statusBtnTip.getText().equals(stoppedString)) {

                    statusBtnTip.setText(stoppedString);
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getIsSetup() ? getStartImgUrl() : getInstallImgUrl()), 15);
                    centerField.setAlignment(Pos.CENTER);
                    statusString.set(getIsSetup() ? "Offline" : "(Not Installed)");
                    powerBtn.setGraphic(IconButton.getIconView(new Image(getPowerOffUrl()), 15));

                }
            } else {
                if (!statusBtnTip.getText().equals("Stop")) {

                    statusBtnTip.setText("Stop");
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getStopImgUrl()), 15);
                    statusString.set(value);

                    centerField.setAlignment(Pos.CENTER_LEFT);
                    if (!syncedProperty.get()) {
                        powerBtn.setGraphic(IconButton.getIconView(new Image(getPowerInitUrl()), 15));
                    }
                }
            }
        };

        statusProperty.addListener((obs, oldVal, newVal) -> checkStatus.run());

        HBox topSpacer = new HBox();
        HBox bottomSpacer = new HBox();

        topSpacer.setMinHeight(2);
        bottomSpacer.setMinHeight(2);

        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        topSpacer.setId("bodyBox");
        bottomSpacer.setId("bodyBox");

        Region topMiddleRegion = new Region();
        HBox.setHgrow(topMiddleRegion, Priority.ALWAYS);

        HBox topBox = new HBox(topInfoStringText, topMiddleRegion, topRightText);
        topBox.setId("darkBox");

        Text ipText = new Text(namedNodeUrlProperty.get().getUrlString());
        ipText.setFill(getPrimaryColor());
        ipText.setFont(getSmallFont());

        Text syncText = new Text();
        syncText.setFill(syncedProperty.get() ? getPrimaryColor() : getSecondaryColor());
        syncText.setFont(getSmallFont());

        Region lbotRegion = new Region();
        lbotRegion.setMinWidth(5);
        HBox.setHgrow(lbotRegion, Priority.ALWAYS);

        Region rbotRegion = new Region();
        rbotRegion.setMinWidth(5);
        HBox.setHgrow(rbotRegion, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, lbotRegion, syncText, rbotRegion, botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        //syncText.prefWidthProperty().bind(bottomBox.widthProperty().subtract(ipText.layoutBoundsProperty().get().getWidth()).subtract(botTimeText.layoutBoundsProperty().get().getWidth()));
        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox contentsBox = new HBox(leftBox, bodyBox, rightBox);
        contentsBox.setId("rowBox");
        HBox.setHgrow(contentsBox, Priority.ALWAYS);

        HBox rowBox = new HBox(contentsBox);
        rowBox.setPadding(new Insets(0, 0, 5, 0));
        rowBox.setAlignment(Pos.CENTER_RIGHT);
        rowBox.setId("unselected");
        HBox.setHgrow(rowBox, Priority.ALWAYS);
        // rowBox.setId("rowBox");

        checkStatus.run();

        namedNodeUrlProperty.addListener((obs, oldval, newVal) -> {
            ipText.setText(newVal.getUrlString());

        });

        Runnable updateSynced = () -> {
            String status = statusProperty.get() == null ? ErgoMarketsData.STOPPED : statusProperty.get();

            if (!status.equals(ErgoMarketsData.STOPPED)) {

                Platform.runLater(() -> {
                    boolean synced = syncedProperty.get();
                    int peerCount = peerCountProperty.get();
                    long networkBlockHeight = networkBlockHeightProperty.get();
                    long nodeBlockHeight = nodeBlockHeightProperty.get();

                    if (!synced) {
                        //  double p = (networkBlockHeight / nodeBlockHeight);
                        //if (networkBlockHeight == -1 || nodeBlockHeight == -1) {
                        //    syncText.setText("Updating sync status...");
                        //  } else {

                        syncText.setText((nodeBlockHeight == -1 ? "Getting block height..." : nodeBlockHeight) + " / " + (networkBlockHeight == -1 ? "Getting: Network height..." : networkBlockHeight) + (peerCount > 0 ? "   Peers: " + peerCount : ""));

                        //+ " (" + String.format("%.1f", p * 100) + ")");
                        // }
                    } else {
                        syncText.setText("Synchronized: " + nodeBlockHeight + (peerCount > 0 ? "   Peers: " + peerCount : ""));
                    }
                });
            } else {
                Platform.runLater(() -> {
                    syncText.setText("");
                });
            }

        };


        nodeBlockHeightProperty.addListener((obs, oldVal, newVal) -> updateSynced.run());
        networkBlockHeightProperty.addListener((obs, oldval, newVal) -> updateSynced.run());
        statusProperty.addListener((obs, oldval, newval) -> updateSynced.run());
        syncedProperty.addListener((obs, oldVal, newVal) -> {

            syncText.setFill(newVal ? getPrimaryColor() : getSecondaryColor());
            powerBtn.setGraphic(IconButton.getIconView(new Image(newVal ? getPowerOnUrl() : (statusProperty.get().equals(ErgoMarketsData.STOPPED) ? getPowerOffUrl() : getPowerInitUrl())), 15));

        });
        updateSynced.run();

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            Platform.runLater(() -> {
                getErgoNodesList().selectedIdProperty().set(getId());
                e.consume();
            });
        });

        Runnable updateSelected = () -> {
            String selectedId = getErgoNodesList().selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            centerField.setId(isSelected ? "selectedField" : "formField");
            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

        //double width = bottomBox.layoutBoundsProperty().get().getWidth() - ipText.layoutBoundsProperty().get().getWidth() - botTimeText.layoutBoundsProperty().get().getWidth();
        // syncField.minWidthProperty().bind(rowBox.widthProperty().subtract(botTimeText.layoutBoundsProperty().get().getWidth()).subtract(200));
        getErgoNodesList().selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());
        updateSelected.run();

        return rowBox;
    }

    @Override
    public String getStartImgUrl(){
        return "/assets/play-30.png";
    }

    @Override
    public String getStopImgUrl() { 
        return "/assets/stop-30.png";
    }

    @Override
    public String getName(){
        return "Local Node (" + super.getName() + ")";
    }
    
 

    public void setExecParams(String params){
        m_execParams = params;
    }

    public String getExecParams(){
        
        return m_execParams;
    }

    @Override
    public JsonObject getJsonObject() {
        NamedNodeUrl namedNodeUrl = namedNodeUrlProperty.get();

        JsonObject json = new JsonObject();
        

        if (namedNodeUrl != null) {
            json.add("namedNode", namedNodeUrl.getJsonObject());
        }

        json.addProperty("isSetup", getIsSetup());
        json.addProperty("runOnStart", m_runOnStart);

        if (m_nodeConfigData != null) {
            json.add("config", m_nodeConfigData.getJsonObject());
        }

        if (m_appDir != null && m_appDir.isDirectory()) {
            json.addProperty("appDir", getAppDir().getAbsolutePath());
        }
        if (m_appFileName != null && getAppFile() != null) {
            json.addProperty("appFileName", m_appFileName);
        }
        if (m_appFileHashData != null) {
            json.add("appFileHashData", m_appFileHashData.getJsonObject());
        }

        json.addProperty("appVersion", m_appVersion.get().get());
        json.addProperty("appExecParams", m_execParams);

        return json;

    }

    public void openAppFile(Stage stage, Runnable onSuccess, Runnable onFailed){
        stop();
        FileChooser chooser = new FileChooser();
            chooser.setTitle("Select 'Ergo Core (.jar)'");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Ergo Core (.jar)", "*.jar"));
            File coreFile = chooser.showOpenDialog(stage);
            String errorString = "File cannot be opened.";
            if(coreFile!= null){
                if (Utils.checkJar(coreFile)) {

                    try {
                        final String coreFileName = coreFile.getName();
                        final HashData hashData = new HashData(coreFile);
                        if(!coreFile.getAbsolutePath().equals(m_appDir.getAbsolutePath())){
                            Files.move(coreFile.toPath(), new File(m_appDir.getAbsolutePath() + "/" +coreFileName).toPath(), StandardCopyOption.REPLACE_EXISTING );
                        }
                        
                        final Version fileNameVersion = Utils.getFileNameVersion(coreFileName);
                        
                        
                        
                        Platform.runLater(()-> {
                            m_appFileHashData = hashData;
                            m_appFileName = coreFileName;
                            m_appVersion.set( fileNameVersion != null ? fileNameVersion : new Version() );
                            lastUpdated.set(LocalDateTime.now());
                            onSuccess.run();
                        });

                    } catch (Exception e1) {
                        Alert a = new Alert(AlertType.NONE, errorString + ":\n\n" + e1, ButtonType.OK);
                        a.setHeaderText("Error Opening");
                        a.setTitle("Error Opening - Settings - Local Node - Ergo Nodes");
                        a.initOwner(m_stage);
                        a.show();
                    
                        Platform.runLater(()-> onFailed.run());
                    
                    }


                }else{
                    Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                    a.setHeaderText("Error Opening");
                    a.setTitle("Error Opening - Settings - Local Node - Ergo Nodes");
                    a.initOwner(m_stage);
                    a.show();
                    Platform.runLater(()-> onFailed.run());
                    
                }
            }
    }


    public void updateAppFile(Stage stage, Scene previousScene, Runnable onComplete, Runnable noUpdate){
            Utils.getUrlJson(GitHub_LATEST_URL, (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            SimpleStringProperty errorText = new SimpleStringProperty(null);
             Rectangle rect =  getErgoNodesList().getErgoNodes().getNetworksData().getMaximumWindowBounds();

            if (sourceObject != null && sourceObject instanceof JsonObject) {
                JsonObject gitHubApiJson = (JsonObject) sourceObject;

                JsonElement assetsElement = gitHubApiJson.get("assets");
                if (assetsElement != null && assetsElement.isJsonArray()) {
                    JsonArray assetsArray = assetsElement.getAsJsonArray();
                    if (assetsArray.size() > 0) {
                        JsonElement primaryAssetElement = assetsArray.get(0);
                     
                        if (primaryAssetElement != null && primaryAssetElement.isJsonObject()) {
                            JsonObject assetObject = primaryAssetElement.getAsJsonObject();
                            
                        
                                
                            JsonElement downloadUrlElement = assetObject.get("browser_download_url");
                            JsonElement nameElement = assetObject.get("name");

                            if (downloadUrlElement != null && downloadUrlElement.isJsonPrimitive()) {
                                String url = downloadUrlElement.getAsString();
                                String name = nameElement.getAsString();

                                try{

                                    Version newVersion = Utils.getFileNameVersion(name);
                                    Version prevVersion = m_appVersion.get();
                                    File prevAppFile = getAppFile();

                                    

                                    if(newVersion.compareTo(prevVersion) > 0 || prevAppFile == null){
                                    
                                        if(m_deleteOldFiles){
                                            if(prevAppFile != null && !prevAppFile.getName().equals(name) && prevAppFile.isFile()){
                                                prevAppFile.delete();
                                            }
                                        }
                                        
                                        File appFile = new File(m_appDir.getAbsolutePath() + "/" + name);
                                        ProgressBar progressBar = new ProgressBar();
                                        
                                    
                                        if(stage != null){
                                            Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Settings - Ergo Local Node - " + ErgoNodes.NAME , new SimpleStringProperty( name), progressBar, stage);
                                            stage.setScene(progressScene);
                                           
                                            stage.setX((rect.getWidth() /2) - (stage.getWidth() / 2));
                                            stage.setY((rect.getHeight()/2) - (stage.getHeight() / 2));
                                        }
                                        Utils.getUrlFileHash(url, appFile, (onDlSucceeded) -> {
                                            Object dlObject = onDlSucceeded.getSource().getValue();
                                            if (dlObject != null && dlObject instanceof HashData) {
                                                m_appFileHashData = (HashData) dlObject;
                                                m_appFileName = name;
                                                m_appVersion.set(newVersion);
                                                lastUpdated.set(LocalDateTime.now());
                                                if(onComplete != null){
                                                    onComplete.run();
                                                }
                                                if(stage != null){ 
                                                    
                                                    stage.setScene(previousScene);
                                                    stage.setX((rect.getWidth()/2) - (stage.getWidth() / 2));
                                                    stage.setY((rect.getHeight()/2) - (stage.getHeight() / 2));
                                                }
                                            } else {
                                                Alert a = new Alert(AlertType.NONE, "The download returned an invalid file.", ButtonType.OK);
                                                a.initOwner(stage);
                                                a.setTitle("Download Failed - Setup - Ergo Nodes");
                                                a.setHeaderText("Download Failed");
                                                a.show();
                                                if(stage != null){
                                                    stage.setScene(previousScene);
                                                    stage.setScene(previousScene);
                                                    stage.setX((rect.getWidth()/2) - (stage.getWidth() / 2));
                                                    stage.setY((rect.getHeight()/2) - (stage.getHeight() / 2));
                                                }
                                            }
                                        }, (onDlFailed) -> {
                                            String errorString = onDlFailed.getSource().getException().toString();

                                            Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                            a.initOwner(stage);
                                            a.setTitle("Error - Setup - Ergo Nodes");
                                            a.setHeaderText("Error");
                                            a.show();
                                            if(stage != null){
                                                stage.setScene(previousScene);
                                                stage.setScene(previousScene);
                                                stage.setX(rect.getWidth() - (stage.getWidth() / 2));
                                                stage.setY(rect.getHeight() - (stage.getHeight() / 2));
                                            }
                                        },stage != null ? progressBar : null);
                                        
                                    }else{
                                        if(noUpdate != null){
                                            noUpdate.run();
                                        }
                                    }
                                }catch(Exception e){
                                    errorText.set(e.toString());
                                }
                                
                            } else {
                                errorText.set("Asset url corrupt (try again ->)");
                            }
                        } else {
                            errorText.set("Asset unavailable (try again ->)");
                        }
                    } else {
                        errorText.set("Received empty assets list (try again ->");
                    }
                } else {
                    errorText.set("Received unexpected file format (try again ->)");
                }

            } else {
                errorText.set("Unable to connect to GitHub (try again ->)");
            }
            if(errorText.get() != null){
                Alert a = new Alert(AlertType.NONE, "Download manifest corrupt:\n\n" + errorText.get(), ButtonType.OK);
                a.setHeaderText("Manifest Error");
                a.setTitle("Manifest Error - Local Node - Ergo Nodes");
                if(stage != null){
                    a.initOwner(stage);
                }
                a.show();
            }
        }, onFailed -> {
            Alert a = new Alert(AlertType.NONE, onFailed.getSource().getMessage(), ButtonType.OK);
            a.setHeaderText("Download Error");
            a.setTitle("Download Error - Local Node - Ergo Nodes");
            if(stage != null){
                a.initOwner(stage);
            }
            a.show();
        }, null);
    }

    private String m_downArrowUrlString = "/assets/caret-down-15.png";
    private String m_upArrowUrlString = "/assets/caret-up-15.png";

    private Scene settingsScene( Stage stage) {
        String titleString = "Settings - Local Node - " + ErgoNodes.NAME;
    
        stage.setTitle(titleString);

        
        VBox layoutBox = new VBox();
        Scene settingsScene = new Scene(layoutBox, SETTINGS_STAGE_WIDTH, SETTINGS_STAGE_HEIGHT);

        

        settingsScene.getStylesheets().add("/css/startWindow.css");
   
        ErgoNodeConfig nodeConfig = new ErgoNodeConfig(m_nodeConfigData);

         NamedNodeUrl namedNode = namedNodeUrlProperty.get();

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 40;
        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, titleString, closeBtn, stage);

        Text headingText = new Text("Settings");
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));


        
        Tooltip setupTooltip = new Tooltip("Setup");
        setupTooltip.setShowDelay(new Duration(100));

        BufferedButton setupBtn = new BufferedButton(getInstallImgUrl(), 20);
        setupBtn.setTooltip(setupTooltip);
        setupBtn.setOnAction(e->{
            closeBtn.fire();
            setup();
        });
        Tooltip deleteTip = new Tooltip("Remove Node");
        deleteTip.setShowDelay(new Duration(100));

        BufferedButton deleteBtn = new BufferedButton("/assets/trash-outline-white-30.png",20);
        deleteBtn.setTooltip(deleteTip);

        deleteBtn.setOnAction(e->{
            Alert a = new Alert(AlertType.NONE, "Delete all local node files?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            a.setTitle("Remove Node - " + namedNode.getName() + " - Ergo Nodes");
            a.setHeaderText("Clear Node");
            a.initOwner(m_stage);
            Optional<ButtonType> result = a.showAndWait();

            if(result != null && result.isPresent() && result.get() != ButtonType.CANCEL){
                stop();
                closeBtn.fire();
                resetToDefault();

                if(result.get() == ButtonType.YES){
                    
                    FxTimer.runLater(java.time.Duration.ofMillis(500), ()->{
                        try {
                            FileUtils.deleteDirectory(m_appDir);
                        } catch (IOException e1) {
                            Alert a1 = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                            a1.setTitle("Error");
                            a1.initOwner(m_stage);
                            a1.setHeaderText("Error");
                            a1.show();
                        }
                    });
                    
                }
            }
        });


        TextField appDirField = new TextField(m_appDir.getAbsolutePath());
        appDirField.setId("urlField");
        appDirField.setEditable(false);
        


        HBox.setHgrow(appDirField, Priority.ALWAYS);

        Tooltip navTooltip = new Tooltip("Open in File Explorer");
        
        BufferedButton navBtn = new BufferedButton("/assets/navigate-outline-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
        navBtn.setText("Location");
        navBtn.setGraphicTextGap(15);
        navBtn.setId("titleBtn");
        navBtn.setContentDisplay(ContentDisplay.RIGHT);
        navBtn.setTooltip(navTooltip);
        navBtn.setOnAction(e->{
            try {
                Desktop.getDesktop().open(m_appDir);
            } catch (IOException e1) {
                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                a.setTitle("Error");
                a.initOwner(m_stage);
                a.setHeaderText("Error");
                a.show();
            }
        });

        HBox menuBar = new HBox(navBtn, appDirField,setupBtn, deleteBtn);

       
        menuBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setId("menuBar");

        HBox menubarPaddingBox = new HBox(menuBar);

        menubarPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox menuBarBox = new VBox(menubarPaddingBox);

       menuBarBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text configText = new Text("Config");
        configText.setFill(App.txtColor);
        configText.setFont(App.txtFont);
    


        HBox configBox = new HBox(configText);
        configBox.setAlignment(Pos.CENTER_LEFT);
        configBox.setMinHeight(40);
        configBox.setId("headingBox");
        configBox.setPadding(new Insets(0, 0, 0, 15));

        SimpleBooleanProperty showConfigFile = new SimpleBooleanProperty(false);


        BufferedButton configFileHeadingBtn = new BufferedButton(m_downArrowUrlString, App.MENU_BAR_IMAGE_WIDTH);
        configFileHeadingBtn.setText("File");
        configFileHeadingBtn.setId("titleBtn");
        configFileHeadingBtn.setGraphicTextGap(15);
        configFileHeadingBtn.setPadding(new Insets(0, 0, 0, 0));
        configFileHeadingBtn.setContentDisplay(ContentDisplay.RIGHT);
        configFileHeadingBtn.setOnAction(e->{
            showConfigFile.set(!showConfigFile.get());
        });

        HBox configFileHeadingBox = new HBox(configFileHeadingBtn);
        configFileHeadingBox.setAlignment(Pos.CENTER_LEFT);
        configFileHeadingBox.minHeightProperty().bind(rowHeight);
    
        
        Text settingsFileNameText = new Text(String.format("%-14s", "Name"));
        settingsFileNameText.setFill(getPrimaryColor());
        settingsFileNameText.setFont(App.txtFont);
        

        TextField settingsFileNameField = new TextField(nodeConfig.getConfigFile().getName());
        settingsFileNameField.setId("formField");
        settingsFileNameField.setEditable(false);
        HBox.setHgrow(settingsFileNameField, Priority.ALWAYS);

        HBox settingsFileNameBox = new HBox(settingsFileNameText, settingsFileNameField);
        settingsFileNameBox.setAlignment(Pos.CENTER_LEFT);
        settingsFileNameBox.minHeightProperty().bind(rowHeight);
        
     

        Text settingsFileText = new Text(String.format("%-14s", "Location"));
        settingsFileText.setFill(getPrimaryColor());
        settingsFileText.setFont(App.txtFont);

        TextField settingsFileField = new TextField(nodeConfig.getConfigFile().getAbsolutePath());
        settingsFileField.setId("formField");
        settingsFileField.setEditable(false);
        HBox.setHgrow(settingsFileField, Priority.ALWAYS);

        HBox settingsFileBox = new HBox(settingsFileText, settingsFileField);
        settingsFileBox.setAlignment(Pos.CENTER_LEFT);
        settingsFileBox.minHeightProperty().bind(rowHeight);

        Text settingsFileHashText = new Text(String.format("%-14s", "Hash"));
        settingsFileHashText.setFill(getPrimaryColor());
        settingsFileHashText.setFont(App.txtFont);

        TextField settingsFileHashField = new TextField(nodeConfig.getConfigFileHashData().getHashStringHex());
        settingsFileHashField.setId("formField");
        settingsFileHashField.setEditable(false);

    
        HBox.setHgrow(settingsFileHashField, Priority.ALWAYS);


        Text configHashName = new Text("(" + nodeConfig.getConfigFileHashData().getHashName()+")");
        configHashName.setFill(getSecondaryColor());
        configHashName.setFont(App.txtFont);

        Region configHashSpacer = new Region();
        configHashSpacer.setMinWidth(5);

        HBox settingsFileHashBox = new HBox(settingsFileHashText,configHashSpacer, configHashName, settingsFileHashField);
        settingsFileHashBox.setAlignment(Pos.CENTER_LEFT);
        settingsFileHashBox.minHeightProperty().bind(rowHeight);

        VBox configFileBodyBox = new VBox(settingsFileNameBox, settingsFileHashBox, settingsFileBox);
        configFileBodyBox.setId("bodyBox");
        configFileBodyBox.setPadding(new Insets(0,0,0, 30));
        HBox.setHgrow(configFileBodyBox,Priority.ALWAYS);       



        VBox configBodyBox = new VBox(configFileHeadingBox, configFileBodyBox);
        configBodyBox.setPadding(new Insets(10, 20,10, 30));
        configBodyBox.setId("bodyBox");
        HBox.setHgrow(configBodyBox, Priority.ALWAYS);

        Runnable updateConfigFileInfo = () ->{
            boolean isShowing = showConfigFile.get();
            if(isShowing){
                if(!configBodyBox.getChildren().contains(configFileBodyBox)){
                    int index = configBodyBox.getChildren().indexOf(configFileHeadingBox) + 1;

                    configBodyBox.getChildren().add(index, configFileBodyBox);
                }
                configFileHeadingBtn.setImage(new Image(m_upArrowUrlString));
            }else{
                if(configBodyBox.getChildren().contains(configFileBodyBox)){
                    configBodyBox.getChildren().remove(configFileBodyBox);
                }
                configFileHeadingBtn.setImage(new Image(m_downArrowUrlString));
            }
        };
        updateConfigFileInfo.run();
        showConfigFile.addListener((obs, oldVal, newVal)->updateConfigFileInfo.run());
        
        VBox configVBox = new VBox(configBox, configBodyBox);
        configVBox.setPadding(new Insets(0, 0, 15, 0));

        Text appHeadingText = new Text("Application");
        appHeadingText.setFill(App.txtColor);
        appHeadingText.setFont(App.txtFont);
        

    


        HBox appHeadingBox = new HBox(appHeadingText );
        appHeadingBox.setAlignment(Pos.CENTER_LEFT);
        appHeadingBox.setMinHeight(40);
        appHeadingBox.setId("headingBox");
        appHeadingBox.setPadding(new Insets(0, 0, 0, 15));

        
    
      

        Text appNetworkTypeText = new Text(String.format("%-14s", "Network Type"));
        appNetworkTypeText.setFill(getPrimaryColor());
        appNetworkTypeText.setFont(App.txtFont);


        TextField appNetworkTypeField = new TextField(namedNode.getNetworkType().toString());
        appNetworkTypeField.setId("formField");
        appNetworkTypeField.setEditable(false);
        HBox.setHgrow(appNetworkTypeField, Priority.ALWAYS);

        HBox appNetworkTypeBox = new HBox(appNetworkTypeText, appNetworkTypeField);
        appNetworkTypeBox.setAlignment(Pos.CENTER_LEFT);
        appNetworkTypeBox.minHeightProperty().bind(rowHeight);
        
       Text appRunOnStartText = new Text(String.format("%-14s", "Autorun"));
        appRunOnStartText.setFill(getPrimaryColor());
        appRunOnStartText.setFont(App.txtFont);

     

        BufferedButton appRunOnStartBtn = new BufferedButton("/assets/checkmark-25.png", 20);
        appRunOnStartBtn.setPrefWidth(30);
        appRunOnStartBtn.setPrefHeight(30);
        appRunOnStartBtn.setId("checkBtn");
        
        Runnable updateRunOnStart = ()->{
            boolean runOnStart = m_runOnStart;
            appRunOnStartBtn.setImage(runOnStart ? new Image("/assets/checkmark-25.png") : null);
        };
        updateRunOnStart.run();

        appRunOnStartBtn.setOnAction(e->{
            m_runOnStart = !m_runOnStart;
            lastUpdated.set(LocalDateTime.now());
            updateRunOnStart.run();
        });

        Region appRunOnStartRegion = new Region();
        appRunOnStartRegion.setMinWidth(9);

        HBox appRunOnStartBox = new HBox(appRunOnStartText, appRunOnStartRegion, appRunOnStartBtn);
        appRunOnStartBox.setAlignment(Pos.CENTER_LEFT);
        appRunOnStartBox.minHeightProperty().bind(rowHeight);
     

        Text appNameText = new Text(String.format("%-14s", "Name"));
        appNameText.setFill(getPrimaryColor());
        appNameText.setFont(App.txtFont);

          

        TextField appNameField = new TextField();
        appNameField.setId("formField");
        appNameField.setEditable(false);
        HBox.setHgrow(appNameField, Priority.ALWAYS);

        HBox appNameBox = new HBox(appNameText, appNameField);
        appNameBox.setAlignment(Pos.CENTER_LEFT);
        appNameBox.minHeightProperty().bind(rowHeight);
        
     

        Text appFileText = new Text(String.format("%-14s", "Location"));
        appFileText.setFill(getPrimaryColor());
        appFileText.setFont(App.txtFont);

        TextField appFileField = new TextField();
        appFileField.setId("formField");
        appFileField.setEditable(false);
        HBox.setHgrow(appFileField, Priority.ALWAYS);

        HBox appFileBox = new HBox(appFileText, appFileField);
        appFileBox.setAlignment(Pos.CENTER_LEFT);
        appFileBox.minHeightProperty().bind(rowHeight);

       



        Text appHashText = new Text(String.format("%-14s", "Hash"));
        appHashText.setFill(getPrimaryColor());
        appHashText.setFont(App.txtFont);

        TextField appHashField = new TextField();
        appHashField.setId("formField");
        appHashField.setEditable(false);
        HBox.setHgrow(appHashField, Priority.ALWAYS);


        Text appHashName = new Text();
        appHashName.setFill(getSecondaryColor());
        appHashName.setFont(App.txtFont);

  
        

        Region appHashSpacer = new Region();
        appHashSpacer.setMinWidth(5);

        HBox appHashBox = new HBox(appHashText, appHashSpacer, appHashName, appHashField);
        appHashBox.setAlignment(Pos.CENTER_LEFT);
        appHashBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(appHashBox, Priority.ALWAYS);


        Text appExecText = new Text(String.format("%-14s", "Parameters"));
        appExecText.setFill(getPrimaryColor());
        appExecText.setFont(App.txtFont);


        MenuButton viewExecutionStringBtn = new MenuButton("(View)");
        viewExecutionStringBtn.setId("menuBtn");
        viewExecutionStringBtn.setPopupSide(Side.TOP);



        
        TextField appParamsField = new TextField(m_execParams);
        appParamsField.setId("formField");
        appParamsField.setPromptText("Enter additional parameters (Advanced)");
        HBox.setHgrow(appParamsField, Priority.ALWAYS);

        
        Runnable updateCmdString = ()->{
            File appFile = getAppFile();
            File configFile = m_nodeConfigData.getConfigFile();

            String cmdString = getExecCmd(appFile, configFile);
            MenuItem executionStringItem = new MenuItem( "\"" + cmdString + "\"  (Click to copy)");
            executionStringItem.setOnAction(e->{
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(cmdString);
                clipboard.setContent(content);
               
            });
            viewExecutionStringBtn.getItems().clear();
            viewExecutionStringBtn.getItems().add(executionStringItem);
        };



        appParamsField.focusedProperty().addListener((obs, oldVal, newVal)->{
            if(!newVal){
                String prevParams = m_execParams;
                String newParams = appParamsField.getText();

                if(!prevParams.equals(newParams)){
                    m_execParams = appParamsField.getText();
                    lastUpdated.set(LocalDateTime.now());
                    updateCmdString.run();
                }
                
            }
        });
        updateCmdString.run();

        HBox appExecBox = new HBox(appExecText, viewExecutionStringBtn, appParamsField);
        appExecBox.setAlignment(Pos.CENTER_LEFT);
        appExecBox.minHeightProperty().bind(rowHeight);


 





        SimpleBooleanProperty showAppFile = new SimpleBooleanProperty(false);

        BufferedButton appFileHeadingBtn = new BufferedButton(m_downArrowUrlString, App.MENU_BAR_IMAGE_WIDTH);
        appFileHeadingBtn.setText("File");
        appFileHeadingBtn.setId("titleBtn");
        appFileHeadingBtn.setPadding(new Insets(0, 0, 0, 0));
        appFileHeadingBtn.setGraphicTextGap(15);
        appFileHeadingBtn.setContentDisplay(ContentDisplay.RIGHT);
        appFileHeadingBtn.setOnAction(e->{
            showAppFile.set(!showAppFile.get());
        });
         Tooltip openAppBtnTip = new Tooltip("Select new file");
        openAppBtnTip.setShowDelay(new Duration(100));

        BufferedButton openAppFileBtn = new BufferedButton(getInstallImgUrl(), 20);
        openAppFileBtn.setTooltip(openAppBtnTip);
      
        Tooltip downloadAppBtnTip = new Tooltip("Get latest version");
        downloadAppBtnTip.setShowDelay(new Duration(100));

        Text isLatestText = new Text("");
        isLatestText.setFont(App.titleFont);
        isLatestText.setFill(getSecondaryColor());
        

        BufferedButton downloadAppBtn = new BufferedButton("/assets/cloud-download-30.png", 20);
        downloadAppBtn.setTooltip(downloadAppBtnTip);
        
        Region appFileRegion = new Region();
        HBox.setHgrow(appFileRegion, Priority.ALWAYS);


        HBox appFileHeadingBox = new HBox( appFileHeadingBtn, appFileRegion,isLatestText,  openAppFileBtn, downloadAppBtn);

        appFileHeadingBox.setMinHeight(40);
        appFileHeadingBox.setAlignment(Pos.CENTER_LEFT);


        Text appFileVersionText = new Text(String.format("%-14s", "Version"));
        appFileVersionText.setFill(getPrimaryColor());
        appFileVersionText.setFont(App.txtFont);

          

        TextField appFileVersionField = new TextField();
        appFileVersionField.setId("formField");
        appFileVersionField.setEditable(false);
        HBox.setHgrow(appFileVersionField, Priority.ALWAYS);

        HBox appFileVersionBox = new HBox(appFileVersionText, appFileVersionField);
        appFileVersionBox.setAlignment(Pos.CENTER_LEFT);
        appFileVersionBox.minHeightProperty().bind(rowHeight);


        Runnable updateAppFileText = () ->{
           // String fileVersion = m_appVersion.get().get();

            appNameField.setText(m_appFileName);
            appFileField.setText(getAppFile().getAbsolutePath());  
            appHashName.setText("(" + m_appFileHashData.getHashName()+")");
            appHashField.setText(m_appFileHashData.getHashStringHex());
           
        };

        appFileVersionField.textProperty().bind(m_appVersion.asString());

        updateAppFileText.run();

        openAppFileBtn.setOnAction(e->{
            openAppFile(stage, updateAppFileText, ()->{});
        });


        downloadAppBtn.setOnAction(e->{
            updateAppFile(stage, settingsScene, ()->{
                isLatestText.setText("Updated: " + m_appVersion.get().get());
                updateAppFileText.run();
            }, ()->{
                isLatestText.setText("Already latest: " + m_appVersion.get().get());
            });
        });

        VBox appFileBodyVBox = new VBox(appNameBox, appFileVersionBox,  appHashBox, appFileBox);
        appFileBodyVBox.setId("bodyBox");
        appFileBodyVBox.setPadding(new Insets(0, 0, 0, 30));
        HBox.setHgrow(appFileBodyVBox,Priority.ALWAYS);

         VBox appBodyBox = new VBox(appNetworkTypeBox, appRunOnStartBox,  appExecBox, appFileHeadingBox  );
        appBodyBox.setPadding(new Insets(10, 20,10, 30));
        appBodyBox.setId("bodyBox");
        HBox.setHgrow(appBodyBox, Priority.ALWAYS);

        Runnable updateAppFileInfo = () ->{
            boolean isShowing = showAppFile.get();
            if(isShowing){
                if(!appBodyBox.getChildren().contains( appFileBodyVBox)){
                    int index = appBodyBox.getChildren().indexOf(appFileHeadingBox) + 1;
                    appBodyBox.getChildren().add(index, appFileBodyVBox);
                }
                appFileHeadingBtn.setImage(new Image(m_upArrowUrlString));
            }else{
                if(appBodyBox.getChildren().contains( appFileBodyVBox)){
                    appBodyBox.getChildren().remove(appFileBodyVBox);
                }
                appFileHeadingBtn.setImage(new Image(m_downArrowUrlString));
            }
        };
        updateAppFileInfo.run();
        showAppFile.addListener((obs, oldVal, newVal)->updateAppFileInfo.run());



        VBox appVBox = new VBox(appHeadingBox, appBodyBox);
        appVBox.setPadding(new Insets(0, 0, 15, 0));
   

        VBox bodyBox = new VBox( appVBox, configVBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));
        
        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox();
        
        ScrollPane bodyScroll = new ScrollPane(bodyPaddingBox);
        bodyScroll.prefViewportWidthProperty().bind(stage.widthProperty());
        bodyScroll.setPadding(new Insets(0));
        layoutBox.getChildren().addAll(titleBox, headerBox, menuBarBox, bodyScroll, footerBox);
        settingsScene.getStylesheets().add("/css/startWindow.css");

        SimpleDoubleProperty scrollBarWidth = new SimpleDoubleProperty(0);

        bodyPaddingBox.prefWidthProperty().bind(stage.widthProperty().subtract(scrollBarWidth).subtract(5));

        Runnable closeStage = () -> {
            stage.close();
            m_stage = null;
        };

        Rectangle screenRectangle = getErgoNodesList().getErgoNodes().getNetworksData().getMaximumWindowBounds();

        Runnable updateBodySize = () ->{
            //
            double screenHeight = screenRectangle.getHeight();
            double bodyHeight = bodyPaddingBox.heightProperty().get() + 5;
            double restOfStageHeight = titleBox.heightProperty().doubleValue() +  headerBox.heightProperty().get() + menuBarBox.heightProperty().get() + footerBox.heightProperty().get();
            double totalHeight = bodyHeight + restOfStageHeight;
            
            if(totalHeight <= screenHeight){
                stage.setHeight(totalHeight);
                bodyScroll.setPrefViewportHeight(bodyHeight);
                if(stage.getY() + totalHeight > screenHeight){
                    stage.setY(screenHeight - totalHeight);
                }
            }else{
                scrollBarWidth.set(20);
                stage.setHeight(screenHeight);
                bodyScroll.setPrefViewportHeight(screenHeight - restOfStageHeight);
                stage.setY(0);
            }
        };

        bodyPaddingBox.heightProperty().addListener((obs, oldVal, newVal)->updateBodySize.run());

        stage.sceneProperty().addListener((obs, oldval, newval)->updateBodySize.run());

        closeBtn.setOnAction(e -> closeStage.run());
        m_stage.setOnCloseRequest(e -> closeStage.run());
        return settingsScene;
    }

    @Override
    public void openSettings() {
        if (checkValidSetup()) {
            if (m_stage == null) {
                
              

                m_stage = new Stage();
                m_stage.getIcons().add(getIcon());
                m_stage.setResizable(false);
                m_stage.initStyle(StageStyle.UNDECORATED);

              
                m_stage.setScene(settingsScene(m_stage));

                m_stage.show();

          

            } else {
                if (m_stage.isIconified()) {
                    m_stage.setIconified(false);
                }
                m_stage.show();
                m_stage.toFront();
            }
        } else {
     
            setup();
        }

    }

    @Override
    public HBox getMenuBar() {
        Tooltip settingsTip = new Tooltip("Settings");
        settingsTip.setShowDelay(new Duration(100));
        BufferedButton settingsBtn = new BufferedButton("/assets/settings-outline-white-120.png", 20);
        settingsBtn.setTooltip(settingsTip);

        Tooltip installTooltip = new Tooltip("Setup");
        installTooltip.setShowDelay(new Duration(100));

        BufferedButton installBtn = new BufferedButton(getInstallImgUrl(), 20);
        installBtn.setTooltip(installTooltip);
        installBtn.setOnAction(e->{
            setup();
        });

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        HBox menuBar = new HBox(menuSpacer);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        Runnable checkInstalled = () ->{
            boolean isSetup = isSetupProperty.get();

            if(isSetup){
                if(!menuBar.getChildren().contains(settingsBtn)){
                    menuBar.getChildren().add(settingsBtn);
                }
                if(menuBar.getChildren().contains(installBtn)){
                    menuBar.getChildren().remove(installBtn);
                }
            }else{
                if(menuBar.getChildren().contains(settingsBtn)){
                    menuBar.getChildren().remove(settingsBtn);
                }
                if(!menuBar.getChildren().contains(installBtn)){
                    menuBar.getChildren().add(installBtn);
                }
            }
        };

        isSetupProperty.addListener((obs,oldval,newval)->checkInstalled.run());
        checkInstalled.run();
        settingsBtn.setOnAction(e -> openSettings());
        return menuBar;
    }

    public void remove(){
        removeUpdateListener();
    }
}
