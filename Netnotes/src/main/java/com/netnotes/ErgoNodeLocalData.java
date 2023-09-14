package com.netnotes;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.Binding;

import org.ergoplatform.appkit.NetworkType;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.google.gson.JsonObject;
import com.netnotes.ErgoNodeConfig.BlockchainMode;
import com.netnotes.ErgoNodeConfig.DigestAccess;
import com.netnotes.ErgoNodeConfig.ConfigMode;
import com.rfksystems.blake2b.Blake2b;
import com.google.gson.JsonElement;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;

public class ErgoNodeLocalData extends ErgoNodeData {

    public final static int MAX_INPUT_BUFFER_SIZE = 30;

    final private List<ErgoNodeMsg> m_nodeMsgBuffer = Collections.synchronizedList(new ArrayList<ErgoNodeMsg>());

    private File logFile = new File("ergoLocalNode-log.txt");
    public final static int MAX_CONSOLE_ROWS = 200;

    //  public SimpleStringProperty nodeApiAddress;
    public final static String DEFAULT_NODE_NAME = "Local Node";

    private String GitHub_LATEST_URL = "https://api.github.com/repos/ergoplatform/ergo/releases/latest";

    private String m_setupImgUrl = "/assets/open-outline-white-20.png";

    private boolean m_runOnStart = false;

    private File m_appDir = null;
    private String m_appFileName = null;
    private HashData m_appFileHashData = null;

    private long m_spaceRequired = 50L * (1024L * 1024L * 1024L);
    private Stage m_setupStage = null;
    private Stage m_configStage = null;
    public final static long EXECUTION_TIME = 500;
    private ScheduledFuture<?> m_lastExecution = null;

    public double SETUP_STAGE_WIDTH = 700;
    public double SETUP_STAGE_HEIGHT = 580;

    public double CORE_SETUP_STAGE_WIDTH = 700;
    public double CORE_SETUP_STAGE_HEIGHT = 395;

    private ErgoNodeConfig m_nodeConfigData = null;
    private ExecutorService m_executor = null;
    private Future<?> m_future = null;
    private ScheduledFuture<?> m_scheduledFuture = null;
    private long m_pid = -1;
    private SimpleStringProperty m_consoleOutputProperty = new SimpleStringProperty("");

    public final SimpleBooleanProperty syncedProperty = new SimpleBooleanProperty(false);
    public final SimpleLongProperty nodeBlockHeightProperty = new SimpleLongProperty(-1);
    public final SimpleLongProperty networkBlockHeightProperty = new SimpleLongProperty(-1);
    private AtomicReference<String> m_lastNodeMsgId = new AtomicReference<>(null);
    private AtomicInteger m_inputCycleIndex = new AtomicInteger(0);
    public long INPUT_CYCLE_PERIOD = 100;

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);

            return t;
        }
    });

    public ErgoNodeLocalData(String id, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, LOCAL_NODE, new NamedNodeUrl(id, DEFAULT_NODE_NAME, "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));

        setListeners();

    }

    public ErgoNodeLocalData(JsonObject json, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, json);

        setListeners();
    }

    private void setListeners() {
        getErgoNodesList().getErgoNodes().getNetworksData().shutdownNowProperty().addListener((obs, oldVal, newVal) -> stop());
        syncedProperty.bind(Bindings.greaterThanOrEqual(nodeBlockHeightProperty, networkBlockHeightProperty).and(networkBlockHeightProperty.isNotEqualTo(-1L).and(statusProperty.isNotEqualTo(MarketsData.STOPPED))));
        statusProperty.addListener((obs, oldval, newVal) -> {
            if (newVal != null && newVal.equals(MarketsData.STOPPED)) {
                cmdProperty.set("");
            }
        });
    }

    public void coreFileError() {
        Alert a = new Alert(AlertType.WARNING, "Local node core file has been altered." + "\n\nThe node can not be started.", ButtonType.OK);
        a.setHeaderText("Error: Config Mismatch");
        a.setTitle("Error: Core File Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    public void configFileError() {
        Alert a = new Alert(AlertType.WARNING, "Local node config file has been altered." + "\n\nThe node can not be started.", ButtonType.OK);
        a.setHeaderText("Error: Config Mismatch");
        a.setTitle("Error: Config Mistmatch - Local Node - Ergo Nodes");
        a.show();

    }

    @Override
    public void openJson(JsonObject jsonObj) {
        super.openJson(jsonObj);
        if (jsonObj != null) {

            JsonElement isSetupElement = jsonObj.get("isSetup");
            JsonElement runOnStartElement = jsonObj.get("runOnStart");
            JsonElement appDirElement = jsonObj.get("appDir");
            JsonElement appFileNameElement = jsonObj.get("appFileName");
            JsonElement appFileHashDataElement = jsonObj.get("appFileHashData");
            JsonElement configElement = jsonObj.get("config");

            File appDir = appDirElement != null && appDirElement.isJsonPrimitive() ? new File(appDirElement.getAsString()) : null;

            m_appDir = appDir != null && appDir.isDirectory() ? appDir : null;
            isSetupProperty.set(isSetupElement != null && isSetupElement.isJsonPrimitive() ? isSetupElement.getAsBoolean() : false);
            m_runOnStart = runOnStartElement != null && runOnStartElement.isJsonPrimitive() ? runOnStartElement.getAsBoolean() : false;
            m_appFileHashData = null;
            m_nodeConfigData = null;

            String appFileName = m_appDir != null && appFileNameElement != null && appFileNameElement.isJsonPrimitive() ? appFileNameElement.getAsString() : null;
            File appFile = appFileName != null ? new File(m_appDir.getAbsolutePath() + "/" + appFileName) : null;

            if (isSetupProperty.get() && appFile != null && appFile.isFile() && appFileHashDataElement != null && appFileHashDataElement.isJsonObject()) {
                m_appFileName = appFileName;
                boolean isCorrectHash = false;

                try {
                    m_appFileHashData = new HashData(appFileHashDataElement.getAsJsonObject());
                    byte[] appFileBytes = Utils.digestFile(appFile, null);
                    String appFileHashString = appFileBytes != null ? new String(appFileBytes) : null;
                    isCorrectHash = m_appFileHashData.getHashString() != null && appFileHashString != null && m_appFileHashData.getHashString().equals(appFileHashString);

                } catch (Exception e) {

                }
                if (isCorrectHash) {

                    if (m_nodeConfigData != null) {

                        File configFile = m_nodeConfigData.getConfigFile();
                        String configFileHashString = null;
                        try {
                            configFileHashString = configFile != null && configFile.isFile() ? new String(Utils.digestFile(configFile, null)) : null;
                        } catch (Exception e) {

                        }
                        String nodeConfigHash = m_nodeConfigData.getConfigFileHashData() != null ? m_nodeConfigData.getConfigFileHashData().getHashString() : null;
                        if (configFileHashString != null && nodeConfigHash != null && nodeConfigHash.equals(configFileHashString)) {

                            if (m_runOnStart) {
                                runNode(configFile, appFile);
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

            if (m_appDir != null && isSetupProperty.get()) {
                try {
                    m_nodeConfigData = configElement != null && configElement.isJsonObject() ? new ErgoNodeConfig(configElement.getAsJsonObject(), m_appDir) : null;
                } catch (Exception e) {

                }
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
        Utils.getUrlJson(localApiString, (onSucceeded) -> {
            Object sourceObject = onSucceeded.getSource().getValue();
            if (sourceObject != null && sourceObject instanceof JsonObject) {
                JsonObject json = (JsonObject) sourceObject;
                //Status(int blockHeight, int headerHeight, int networkHeight, int peerCount) {}
                //ew Status(o.getInt("fullHeight"), o.getInt("headersHeight"), o.getInt("maxPeerHeight"), o.getInt("peersCount"));
                JsonElement fullHeightElement = json.get("fullHeight");
                JsonElement maxPeerHeightElement = json.get("maxPeerHeight");
                JsonElement peerCountElement = json.get("peersCount");
                //  JsonElement headerHeightElement = json.get("headerHeight");

                // long headerHeight = headerHeightElement != null && headerHeightElement.isJsonPrimitive() ? headerHeightElement.getAsLong() : -1;
                //long fullHeight = fullHeightElement != null && fullHeightElement.isJsonPrimitive() ? fullHeightElement.getAsLong() : -1;
                long networkHeight = maxPeerHeightElement != null && maxPeerHeightElement.isJsonPrimitive() ? maxPeerHeightElement.getAsLong() : -1;
                int peerCount = peerCountElement != null && peerCountElement.isJsonPrimitive() ? peerCountElement.getAsInt() : -1;

                cmdStatusUpdated.set(String.format("%29s", Utils.formatDateTimeString(LocalDateTime.now())));
                Platform.runLater(() -> {
                    peerCountProperty.set(peerCount);
                    networkBlockHeightProperty.set(networkHeight);

                });

            }
        }, (onFailed) -> {
            //String errMsg = onFailed.getSource().getException().toString();
            Platform.runLater(() -> {
                networkBlockHeightProperty.set(-1);
                peerCountProperty.set(0);
            });

        }, null);

    }

    final private Runnable m_readNodeInput = () -> {
        int mod = m_inputCycleIndex.incrementAndGet();
        if (mod % 150 == 0) {
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

    private void runNode(File appFile, File configFile) {
        if (m_executor == null) {
            m_executor = Executors.newSingleThreadExecutor();

        }

        if ((m_future == null || m_future != null && m_future.isDone()) && m_pid == -1) {
            String[] cmd = {"cmd", "/c", "java", "-jar", appFile.getName(), "--mainnet", "-c", configFile.getName()};
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
                            statusProperty.set(MarketsData.STARTED);
                            if (m_scheduledFuture == null || (m_scheduledFuture != null && m_scheduledFuture.isDone())) {
                                m_scheduledFuture = scheduledExecutor.scheduleAtFixedRate(m_readNodeInput, 50, INPUT_CYCLE_PERIOD, TimeUnit.MILLISECONDS);
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
                isSetupProperty.set(false);
                statusProperty.set(MarketsData.STOPPED);
            };

            statusProperty.set(MarketsData.STARTING);
            File appFile = getAppFile();
            File configFile = m_nodeConfigData != null ? m_nodeConfigData.getConfigFile() : null;
            String appFileHash = m_appFileHashData != null ? m_appFileHashData.getHashString() : null;
            String configFileHash = m_nodeConfigData != null && m_nodeConfigData.getConfigFileHashData() != null ? m_nodeConfigData.getConfigFileHashData().getHashString() : null;

            if (appFile != null && appFile.isFile() && configFile != null && configFile.isFile() && appFileHash != null && configFileHash != null) {
                byte[] appFileHashBytes = null;
                byte[] configFileHashBytes = null;
                try {
                    appFileHashBytes = Utils.digestFile(appFile, null);
                    configFileHashBytes = Utils.digestFile(configFile, null);
                } catch (Exception e) {

                }
                if (appFileHashBytes != null && configFileHashBytes != null) {
                    String appFileHashBytesString = new String(appFileHashBytes);
                    String configFileHashBytesString = new String(configFileHashBytes);

                    if (appFileHash.equals(appFileHashBytesString)) {
                        if (configFileHash.equals(configFileHashBytesString)) {
                            runNode(appFile, configFile);
                        } else {
                            runError.run();
                            configFileError();
                        }

                    } else {

                        runError.run();
                        coreFileError();
                    }

                } else {
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

        Text advFileModeText = new Text(String.format("%-15s", "File"));
        advFileModeText.setFill(getPrimaryColor());
        advFileModeText.setFont(App.txtFont);

        Button advFileModeBtn = new Button("Browse...");
        advFileModeBtn.setFont(App.txtFont);
        advFileModeBtn.setId("rowBtn");
        HBox.setHgrow(advFileModeBtn, Priority.ALWAYS);

        advFileModeBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select location");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Node Config", "*.conf"));
            File configFile = chooser.showOpenDialog(stage);
            if (configFile != null && configFile.isFile()) {
                configFileOption.set(configFile);
                advFileModeBtn.setText(configFile.getAbsolutePath());
            }
        });

        HBox advFileModeBox = new HBox(advFileModeText, advFileModeBtn);
        advFileModeBox.setAlignment(Pos.CENTER_LEFT);
        advFileModeBox.setPadding(new Insets(0, 0, 0, 15));
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

        VBox layoutBox = new VBox(titleBox, headerBox, bodyPaddingBox, footerBox);
        Scene setupNodeScene = new Scene(layoutBox, SETUP_STAGE_WIDTH, SETUP_STAGE_HEIGHT);
        setupNodeScene.getStylesheets().add("/css/startWindow.css");

        Runnable closeStage = () -> {
            stage.close();
            m_setupStage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        m_setupStage.setOnCloseRequest(e -> closeStage.run());
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
            if (!ergoNodes.getNetworksData().getAppData().updatesProperty().get()) {
                if (downloadBtn.getBufferedImageView().getEffect("updateEffectId") == null) {
                    downloadBtn.getBufferedImageView().applyEffect(new InvertEffect("updateEffectId", 0.7));
                }
            } else {
                downloadBtn.getBufferedImageView().removeEffect("updateEffectId");
            }
        };

        ergoNodes.getNetworksData().getAppData().updatesProperty().addListener((obs, oldVal, newVal) -> {
            downloadBtnEffect.run();
            if (newVal.booleanValue()) {
                getLatestUrl.run();
            }

        });

        if (ergoNodes.getNetworksData().getAppData().updatesProperty().get()) {
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

            File appFile = chooser.showOpenDialog(m_setupStage);
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
            m_setupStage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        m_setupStage.setOnCloseRequest(e -> closeStage.run());
        return setupNodeScene;
    }

    public void config() {
        if (m_setupStage != null) {
            m_setupStage.close();
            m_setupStage = null;
        }

        if (m_configStage == null) {
            Button okBtn = new Button();

            m_configStage = new Stage();
            m_configStage.getIcons().add(getIcon());
            m_configStage.setResizable(false);
            m_configStage.initStyle(StageStyle.UNDECORATED);

            SimpleObjectProperty<NamedNodeUrl> namedNode = new SimpleObjectProperty<>(namedNodeUrlProperty.get() != null ? namedNodeUrlProperty.get() : new NamedNodeUrl(FriendlyId.createFriendlyId(), "Local Node", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));

            Scene configScene = new Scene(new VBox(), 400, 400);//getConfigScene(namedNode, okBtn, m_configStage);

            m_configStage.setScene(configScene);

            m_configStage.show();

        } else {

            if (m_configStage.isIconified()) {
                m_configStage.setIconified(false);
            }
            m_configStage.show();
            m_configStage.toFront();
        }
    }

    public void initalSetup() {
        if (m_setupStage == null) {

            SimpleObjectProperty<File> directory = new SimpleObjectProperty<File>(getErgoNodesList().getErgoNodes().getAppDir());
            TextField folderNameField = new TextField(DEFAULT_NODE_NAME);

            TextField configApiKey = new TextField();
            MenuButton configModeBtn = new MenuButton(ConfigMode.BASIC);
            MenuButton configDigestAccess = new MenuButton(DigestAccess.LOCAL);
            MenuButton configBlockchainMode = new MenuButton(BlockchainMode.PRUNED);

            SimpleObjectProperty<File> configFile = new SimpleObjectProperty<>(null);

            Button nextBtn = new Button("Next");

            m_setupStage = new Stage();
            m_setupStage.getIcons().add(getIcon());
            m_setupStage.setResizable(false);
            m_setupStage.initStyle(StageStyle.UNDECORATED);

            Scene initialScene = initialSetupScene(nextBtn, configModeBtn, configDigestAccess, configBlockchainMode, configApiKey, configFile, directory, folderNameField, m_setupStage);
            m_setupStage.setScene(initialScene);

            m_setupStage.show();

            nextBtn.setOnAction(e -> {
                final String configMode = configModeBtn.getText();
                final String digestMode = configDigestAccess.getText();
                final String blockchainMode = configBlockchainMode.getText();
                final String apiKey = configApiKey.getText();
                long useableSpace = directory.get().getUsableSpace();
                long requiredSpace;

                switch (blockchainMode) {
                    case BlockchainMode.RECENT_ONLY:
                        requiredSpace = 100L * 1024L * 1024L;
                        break;
                    case BlockchainMode.PRUNED:
                        requiredSpace = 500L * 1024L * 1024L;
                        break;
                    case BlockchainMode.FULL:
                        requiredSpace = 50L * 1024L * 1024L * 1024L;
                        break;
                    default:
                        requiredSpace = 50L * 1024L * 1024L * 1024L;
                }
                if (requiredSpace > useableSpace) {
                    Alert a = new Alert(AlertType.NONE, "The selected directory does not meet the space requirements.\n\nUseable space: " + Utils.formatedBytes(useableSpace, 2) + "\nRequired space: " + Utils.formatedBytes(requiredSpace, 2), ButtonType.OK);
                    a.initOwner(m_setupStage);
                    a.setHeaderText("Required Space");
                    a.setTitle("Required Space - Setup - Local Node - Ergo Nodes");
                    a.show();
                } else {
                    install(configMode, digestMode, blockchainMode, apiKey, directory, folderNameField, initialScene);
                }

            });

        } else {
            if (m_setupStage.isIconified()) {
                m_setupStage.setIconified(false);
            }
            m_setupStage.show();
            m_setupStage.toFront();
        }
    }

    public SimpleStringProperty consoleOutputProperty() {
        return m_consoleOutputProperty;
    }

    public void install(String configMode, String digestMode, String blockchainMode, String apiKeyString, SimpleObjectProperty<File> directory, TextField folderNameField, Scene initialScene) {
        if (m_setupStage == null) {
            initalSetup();
        } else {
            Button installBtn = new Button("Install");
            Button backBtn = new Button("Back");

            File installDir = new File(directory.get().getAbsolutePath() + "/" + folderNameField.getText());

            SimpleBooleanProperty getLatestBoolean = new SimpleBooleanProperty(true);
            SimpleStringProperty downloadUrl = new SimpleStringProperty("https://github.com/ergoplatform/ergo/releases/download/v5.0.14/ergo-5.0.14.jar");
            SimpleStringProperty downloadFileName = new SimpleStringProperty("ergo-5.0.14.jar");
            SimpleObjectProperty<File> jarFile = new SimpleObjectProperty<File>(null);

            Scene finalSetupScene = getFinalSetupScene(installBtn, backBtn, jarFile, getLatestBoolean, downloadUrl, downloadFileName, m_setupStage);

            m_setupStage.setScene(finalSetupScene);

            backBtn.setOnAction(e -> {
                m_setupStage.setScene(initialScene);
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

                    if (!installDir.isDirectory()) {
                        try {
                            Files.createDirectory(installDir.toPath());

                        } catch (IOException e1) {
                        }
                    }
                    boolean isDownload = getLatestBoolean.get();

                    Runnable installComplete = () -> {

                        isSetupProperty.set(true);
                        m_setupStage.close();
                        m_setupStage = null;

                        if (configMode.equals(ConfigMode.BASIC)) {

                            try {
                                m_nodeConfigData = new ErgoNodeConfig(apiKeyString, configMode, digestMode, blockchainMode, m_appDir);

                                namedNodeUrlProperty.set(new NamedNodeUrl(getId(), blockchainMode, "127.0.0.1", ErgoNodes.MAINNET_PORT, apiKeyString, NetworkType.MAINNET));

                                start();

                            } catch (Exception e1) {
                                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                                a.initOwner(m_setupStage);
                                a.setTitle("Config Creation Error - Setup - Ergo Nodes");
                                a.setHeaderText("Config Creation Error");
                                a.show();
                                m_setupStage.setScene(initialScene);

                            }

                        } else {
                            String configFileName = "ergo.conf";
                            try {
                                m_nodeConfigData = new ErgoNodeConfig(apiKeyString, configMode, digestMode, blockchainMode, configFileName, m_appDir);
                                namedNodeUrlProperty.set(new NamedNodeUrl(getId(), ConfigMode.ADVANCED, "127.0.0.1", ErgoNodes.MAINNET_PORT, apiKeyString, NetworkType.MAINNET));
                                start();
                            } catch (Exception e1) {
                                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                                a.initOwner(m_setupStage);
                                a.setTitle("Config Creation Error - Setup - Ergo Nodes");
                                a.setHeaderText("Config Creation Error");
                                a.show();
                                m_setupStage.setScene(initialScene);

                            }

                        }

                        lastUpdated.set(LocalDateTime.now());

                    };

                    if (installDir.isDirectory()) {

                        if (isDownload) {
                            File appFile = new File(installDir + "/" + downloadFileName.get());

                            Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, downloadFileName, progressBar, m_setupStage);
                            m_setupStage.setScene(progressScene);
                            Utils.getUrlFileHash(downloadUrl.get(), appFile, (onSucceeded) -> {
                                Object sourceObject = onSucceeded.getSource().getValue();
                                if (sourceObject != null && sourceObject instanceof HashData) {
                                    m_appDir = installDir;
                                    m_appFileName = downloadFileName.get();
                                    m_appFileHashData = (HashData) sourceObject;

                                    installComplete.run();
                                } else {
                                    Alert a = new Alert(AlertType.NONE, "Check the download URL and destination path and then try again.", ButtonType.OK);
                                    a.initOwner(m_setupStage);
                                    a.setTitle("Download Failed - Setup - Ergo Nodes");
                                    a.setHeaderText("Download Failed");
                                    a.show();
                                    m_setupStage.setScene(initialScene);
                                }
                            }, (onFailed) -> {
                                String errorString = onFailed.getSource().getException().toString();

                                Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                a.initOwner(m_setupStage);
                                a.setTitle("Error - Setup - Ergo Nodes");
                                a.setHeaderText("Error");
                                a.show();
                                m_setupStage.setScene(initialScene);
                            }, progressBar);
                        } else {
                            File customFile = jarFile.get();

                            if (Utils.checkJar(customFile)) {

                                SimpleStringProperty fileNameProperty = new SimpleStringProperty(customFile.getName());
                                File appFile = new File(installDir.getAbsolutePath() + "/" + fileNameProperty.get());

                                Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, fileNameProperty, progressBar, m_setupStage);
                                m_setupStage.setScene(progressScene);

                                if (customFile.getAbsolutePath().equals(appFile.getAbsolutePath())) {
                                    String errorString = null;
                                    byte[] bytes = null;
                                    try {
                                        bytes = Utils.digestFile(appFile, null);
                                    } catch (Exception e1) {
                                        errorString = e1.toString();
                                    }
                                    if (bytes != null) {
                                        m_appDir = installDir;
                                        m_appFileName = fileNameProperty.get();
                                        m_appFileHashData = new HashData(bytes);

                                        installComplete.run();
                                    } else {

                                        Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                        a.initOwner(m_setupStage);
                                        a.setTitle("Error - Setup - Ergo Nodes");
                                        a.setHeaderText("Error");
                                        a.show();

                                        m_setupStage.setScene(initialScene);
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
                                            a.initOwner(m_setupStage);
                                            a.setTitle("Download Failed - Setup - Ergo Nodes");
                                            a.setHeaderText("Download Failed");
                                            a.show();
                                            m_setupStage.setScene(initialScene);
                                        }

                                    }, onFailed -> {

                                        String errorString = onFailed.getSource().getException().toString();

                                        Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
                                        a.initOwner(m_setupStage);
                                        a.setTitle("Error - Setup - Ergo Nodes");
                                        a.setHeaderText("Error");
                                        a.show();
                                        m_setupStage.setScene(initialScene);

                                    }, progressBar);

                                }

                            } else {
                                Alert a = new Alert(AlertType.NONE, "Select a valid Ergo core file. (ergo-<Version>.jar)", ButtonType.OK);
                                a.initOwner(m_setupStage);
                                a.setTitle("Invalid Core File - Setup - Ergo Nodes");
                                a.setHeaderText("Invalid Core File");
                                a.show();

                                m_setupStage.setScene(initialScene);
                            }

                        }

                    } else {
                        Alert a = new Alert(AlertType.NONE, "File system cannote be accessed.", ButtonType.OK);
                        a.initOwner(m_setupStage);
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
        if (!statusProperty.get().equals(MarketsData.STOPPED)) {

            Utils.wmicTerminate(m_appFileName);

            statusProperty.set(MarketsData.STOPPED);

        }

    }

    public boolean getIsSetup() {
        return isSetupProperty.get();
    }

    @Override
    public HBox getRowItem() {

        Button powerBtn = new Button();
        powerBtn.setGraphic(IconButton.getIconView(new Image(syncedProperty.get() ? getPowerOnUrl() : (statusProperty.get().equals(MarketsData.STOPPED) ? getPowerOffUrl() : getPowerInitUrl())), 15));
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
        BufferedButton statusBtn = new BufferedButton(statusProperty.get().equals(MarketsData.STOPPED) ? (getIsSetup() ? getStartImgUrl() : getInstallImgUrl()) : getStopImgUrl(), 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (statusProperty.get().equals(MarketsData.STOPPED)) {
                if (getIsSetup()) {
                    start();
                } else {
                    initalSetup();
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

            String value = statusProperty.get() == null ? MarketsData.STOPPED : statusProperty.get();

            topRightText.setText(getAppFile() != null ? m_appFileName : "");

            if (value.equals(MarketsData.STOPPED)) {
                String stoppedString = getIsSetup() ? "Start" : "Setup";
                if (!statusBtnTip.getText().equals(stoppedString)) {

                    statusBtnTip.setText(stoppedString);
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getIsSetup() ? getStartImgUrl() : getInstallImgUrl()), 15);
                    centerField.setAlignment(Pos.CENTER);
                    statusString.set("Offline");
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

        TextField syncField = new TextField();

        syncField.setId(syncedProperty.get() ? "smallPrimaryColor" : "smallSecondaryColor");
        syncField.setAlignment(Pos.CENTER);
        syncField.setEditable(false);
        syncField.setPadding(new Insets(0));
        syncField.setFont(getSmallFont());
        HBox.setHgrow(syncField, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, syncField, botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        //syncField.prefWidthProperty().bind(bottomBox.widthProperty().subtract(ipText.layoutBoundsProperty().get().getWidth()).subtract(botTimeText.layoutBoundsProperty().get().getWidth()));
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
            String status = statusProperty.get() == null ? MarketsData.STOPPED : statusProperty.get();
            if (!status.equals(MarketsData.STOPPED)) {
                boolean synced = syncedProperty.get();
                int peerCount = peerCountProperty.get();
                long networkBlockHeight = networkBlockHeightProperty.get();
                long nodeBlockHeight = nodeBlockHeightProperty.get();

                if (!synced) {

                    //  double p = (networkBlockHeight / nodeBlockHeight);
                    //if (networkBlockHeight == -1 || nodeBlockHeight == -1) {
                    //    syncField.setText("Updating sync status...");
                    //  } else {
                    syncField.setText((nodeBlockHeight == -1 ? "Getting block height..." : nodeBlockHeight) + " / " + (networkBlockHeight == -1 ? "Getting: Network height..." : networkBlockHeight) + (peerCount > 0 ? ",  Peers: " + peerCount : ""));

                    //+ " (" + String.format("%.1f", p * 100) + ")");
                    // }
                } else {
                    syncField.setText("Synchronized: " + nodeBlockHeight + (peerCount > 0 ? ",  Peers: " + peerCount : ""));
                }

            } else {
                syncField.setText("");
            }

        };

        try {
            Files.writeString(logFile.toPath(), "\nrow Item test", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        nodeBlockHeightProperty.addListener((obs, oldVal, newVal) -> updateSynced.run());
        networkBlockHeightProperty.addListener((obs, oldval, newVal) -> updateSynced.run());
        statusProperty.addListener((obs, oldval, newval) -> updateSynced.run());
        syncedProperty.addListener((obs, oldVal, newVal) -> {

            syncField.setId(newVal ? "smallPrimaryColor" : "smallSecondaryColor");
            powerBtn.setGraphic(IconButton.getIconView(new Image(newVal ? getPowerOnUrl() : (statusProperty.get().equals(MarketsData.STOPPED) ? getPowerOffUrl() : getPowerInitUrl())), 15));

        });
        updateSynced.run();

        rowBox.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            String currentId = getErgoNodesList().selectedIdProperty().get();
            if (currentId == null) {
                getErgoNodesList().selectedIdProperty().set(getId());
            } else {
                if (!currentId.equals(getId())) {
                    getErgoNodesList().selectedIdProperty().set(getId());
                }
            }
        });

        Runnable updateSelected = () -> {
            String selectedId = getErgoNodesList().selectedIdProperty().get();
            boolean isSelected = selectedId != null && getId().equals(selectedId);

            centerField.setId(isSelected ? "selectedField" : "formField");
            rowBox.setId(isSelected ? "selected" : "unSelected");
        };

        getErgoNodesList().selectedIdProperty().addListener((obs, oldval, newVal) -> updateSelected.run());
        updateSelected.run();

        return rowBox;
    }

    @Override
    public JsonObject getJsonObject() {
        NamedNodeUrl namedNodeUrl = namedNodeUrlProperty.get();

        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
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

        return json;

    }
}
