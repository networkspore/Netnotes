package com.netnotes;

import java.awt.Rectangle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.naming.Binding;

import org.ergoplatform.appkit.NetworkType;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
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
import com.netnotes.NodeSettingsData.BlockchainMode;
import com.netnotes.NodeSettingsData.DigestAccess;
import com.netnotes.NodeSettingsData.SettingsMode;
import com.rfksystems.blake2b.Blake2b;
import com.google.gson.JsonElement;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;

public class FullErgoNode extends ErgoNodeData {

    public final static String DEFAULT_NODE_NAME = "Local Node";

    private String m_githubLatestJson = "https://api.github.com/repos/ergoplatform/ergo/releases/latest";

    private String m_setupImgUrl = "/assets/open-outline-white-20.png";

    private boolean m_setuped = false;
    private boolean m_runOnStart = false;

    private File m_nodeDir = null;
    private File m_nodeJar = null;
    private File m_configFile = null;
    private HashData m_configFileHashData = null;
    private HashData m_coreFileHashData = null;

    private long m_spaceRequired = 50L * (1024L * 1024L * 1024L);
    private Stage m_setupStage = null;
    private Stage m_settingsStage = null;
    public final static long EXECUTION_TIME = 500;
    private ScheduledFuture<?> m_lastExecution = null;

    public double SETUP_STAGE_WIDTH = 700;
    public double SETUP_STAGE_HEIGHT = 580;

    public double CORE_SETUP_STAGE_WIDTH = 700;
    public double CORE_SETUP_STAGE_HEIGHT = 395;

    private NodeSettingsData m_nodeSettingsData = null;

    public double SETTINGS_STAGE_MIN_WIDTH = 500;
    public double SETTINGS_STAGE_MIN_HEIGHT = 850;
    private double m_settingsStageWidth = SETTINGS_STAGE_MIN_WIDTH;
    private double m_settingsStageHeight = SETTINGS_STAGE_MIN_HEIGHT;
    private double m_settingsStagePrevWidth = SETTINGS_STAGE_MIN_WIDTH;
    private double m_settingsStagePrevHeight = SETTINGS_STAGE_MIN_HEIGHT;
    private boolean m_settingsStageMaximized = false;

    public FullErgoNode(String id, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, FULL_NODE, new NamedNodeUrl(id, DEFAULT_NODE_NAME, "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));
    }

    public FullErgoNode(ErgoNodesList ergoNodesList, JsonObject json) {
        super(ergoNodesList, json);
    }

    @Override
    public void openJson(JsonObject jsonObj) {

        JsonElement idElement = jsonObj == null ? null : jsonObj.get("id");
        JsonElement setupedElement = jsonObj == null ? null : jsonObj.get("setuped");
        JsonElement runOnStartElement = jsonObj == null ? null : jsonObj.get("runOnStart");
        JsonElement namedNodeElement = jsonObj == null ? null : jsonObj.get("namedNode");
        JsonElement nodeDirElement = jsonObj == null ? null : jsonObj.get("nodeDir");
        JsonElement nodeJarElement = jsonObj == null ? null : jsonObj.get("nodeJar");

        setId(idElement == null ? FriendlyId.createFriendlyId() : idElement.getAsString());

        m_setuped = setupedElement != null && setupedElement.isJsonPrimitive() ? setupedElement.getAsBoolean() : false;
        m_runOnStart = runOnStartElement != null && runOnStartElement.isJsonPrimitive() ? runOnStartElement.getAsBoolean() : false;
        m_nodeDir = nodeDirElement != null && nodeDirElement.isJsonPrimitive() ? new File(nodeDirElement.getAsString()) : null;
        m_nodeJar = nodeJarElement != null && nodeJarElement.isJsonPrimitive() ? new File(nodeJarElement.getAsString()) : null;

        if (jsonObj != null && namedNodeElement != null && namedNodeElement.isJsonObject() && m_nodeDir != null && m_nodeJar != null) {
            namedNodeUrlProperty().set(new NamedNodeUrl(namedNodeElement.getAsJsonObject()));
        } else {
            m_setuped = false;
            m_runOnStart = false;
            namedNodeUrlProperty().set(new NamedNodeUrl(FriendlyId.createFriendlyId(), DEFAULT_NODE_NAME, "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));
        }

    }

    public long getSpaceRequired() {
        return m_spaceRequired;
    }

    public boolean getIsSetup() {
        return m_setuped;
    }

    public boolean getRunOnStart() {
        return m_runOnStart;

    }

    public String getInstallImgUrl() {
        return m_setupImgUrl;
    }

    public File getNodeDir() {
        return m_nodeDir;
    }

    public File getNodeJar() {
        return m_nodeJar;
    }

    public String getGitHubLatestJson() {
        return m_githubLatestJson;
    }

    @Override
    public void start() {
        /*NamedNodeUrl namedNodeUrl = m_namedNodeUrlProperty.get();
        if (namedNodeUrl != null && namedNodeUrl.getIP() != null) {
            Runnable r = () -> {
                Platform.runLater(() -> m_statusProperty.set(MarketsData.STARTED));
                m_statusString.set("Contacting...");
                pingIP(namedNodeUrl.getIP());
                Platform.runLater(() -> m_statusProperty.set(MarketsData.STOPPED));
            };
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.start();
        }*/

    }

    private Scene initialSetupScene(Button nextBtn, MenuButton settingsModeBtn, MenuButton digestAccessBtn, MenuButton blockchainModeBtn, SimpleStringProperty apiKey, SimpleObjectProperty<File> configFile, SimpleObjectProperty<File> directoryRoot, TextField directoryNameField, Stage stage) {
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

        Text settingsText = new Text("Settings");
        settingsText.setFill(App.txtColor);
        settingsText.setFont(App.txtFont);

        Text apiKeyText = new Text(String.format("%-13s", "API Key"));
        apiKeyText.setFill(App.txtColor);
        apiKeyText.setFont((App.txtFont));

        Button apiKeyBtn = new Button("(Click to set)");
        apiKeyBtn.setId("menuBtn");
        apiKeyBtn.setFont(App.txtFont);
        HBox.setHgrow(apiKeyBtn, Priority.ALWAYS);

        HBox apiKeyBox = new HBox(apiKeyText, apiKeyBtn);
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);
        apiKeyBox.setPadding(new Insets(0, 0, 0, 15));
        apiKeyBox.minHeightProperty().bind(rowHeight);
        HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

        MenuItem simpleItem = new MenuItem(SettingsMode.BASIC);
        simpleItem.setOnAction(e -> {
            settingsModeBtn.setText(simpleItem.getText());
        });

        MenuItem advancedItem = new MenuItem(SettingsMode.ADVANCED);
        advancedItem.setOnAction(e -> {
            settingsModeBtn.setText(advancedItem.getText());
        });

        settingsModeBtn.getItems().addAll(simpleItem, advancedItem);
        settingsModeBtn.setFont(App.txtFont);

        HBox settingsBox = new HBox(settingsText);
        settingsBox.setAlignment(Pos.CENTER_LEFT);
        settingsBox.setMinHeight(40);
        settingsBox.setId("headingBox");
        settingsBox.setPadding(new Insets(0, 0, 0, 15));

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
        Text modeText = new Text(String.format("%-13s", "Mode"));
        modeText.setFill(App.txtColor);
        modeText.setFont((App.txtFont));

        HBox settingsModeBox = new HBox(modeText, settingsModeBtn);
        settingsModeBox.setAlignment(Pos.CENTER_LEFT);
        settingsModeBox.setPadding(new Insets(0, 0, 0, 15));
        settingsModeBox.setMinHeight(40);

        VBox modeOptionsBodyBox = new VBox(digestModeBox, blockchainModeBox);
        modeOptionsBodyBox.setPadding(new Insets(0, 0, 0, 45));

        Text advFileModeText = new Text(String.format("%-15s", "Config file"));
        advFileModeText.setFill(getPrimaryColor());
        advFileModeText.setFont(App.txtFont);

        Button advFileModeBtn = new Button("Browse...");
        advFileModeBtn.setFont(App.txtFont);
        advFileModeBtn.setId("rowBtn");
        HBox.setHgrow(advFileModeBtn, Priority.ALWAYS);

        advFileModeBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select location");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("ergo.conf", "*.conf"));
            File settingsFile = chooser.showOpenDialog(stage);
            if (settingsFile != null && settingsFile.isFile()) {
                configFile.set(settingsFile);
                advFileModeBtn.setText(settingsFile.getAbsolutePath());
            }
        });

        HBox advFileModeBox = new HBox(advFileModeText, advFileModeBtn);
        advFileModeBox.setAlignment(Pos.CENTER_LEFT);
        advFileModeBox.setPadding(new Insets(0, 0, 0, 45));
        advFileModeBox.minHeightProperty().bind(rowHeight);

        VBox modeBodyBox = new VBox(apiKeyBox, settingsModeBox, modeOptionsBodyBox);
        modeBodyBox.setPadding(new Insets(15));
        modeBodyBox.setId("bodyBox");
        HBox.setHgrow(modeBodyBox, Priority.ALWAYS);

        VBox modeBox = new VBox(settingsBox, modeBodyBox);
        modeBox.setPadding(new Insets(0, 0, 15, 0));

        //settingsModeBtn.prefWidthProperty().bind(settingsModeBox.widthProperty().subtract(settingsModeText.layoutBoundsProperty().get().getWidth()));
        Text directoryText = new Text("Directory");
        directoryText.setFill(App.txtColor);
        directoryText.setFont(App.txtFont);

        HBox directoryBox = new HBox(directoryText);
        directoryBox.setAlignment(Pos.CENTER_LEFT);
        directoryBox.setMinHeight(40);;
        directoryBox.setId("headingBox");
        directoryBox.setPadding(new Insets(0, 0, 0, 15));

        Text directoryRootText = new Text(String.format("%-13s", "Location"));
        directoryRootText.setFill(App.txtColor);
        directoryRootText.setFont(App.txtFont);

        Text directoryNameText = new Text(String.format("%-13s", "Folder"));
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

        TextField requiredField = new TextField("~500 Mb");
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
                    requiredField.setText("~500 Mb");
                    break;
                case BlockchainMode.FULL:
                    requiredField.setText(">50 Gb");
                    break;
            }
        };
        blockchainModeBtn.textProperty().addListener((obs, oldval, newval) -> {
            estimateSpaceRequired.run();
        });

        settingsModeBtn.textProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case SettingsMode.ADVANCED:
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
        directorySpaceBox.setPadding(new Insets(0, 0, 0, 45));

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

        Text nodeJarText = new Text("Core File");
        nodeJarText.setFill(App.txtColor);
        nodeJarText.setFont(App.txtFont);

        HBox nodeJarBox = new HBox(nodeJarText);
        nodeJarBox.setPadding(new Insets(0, 0, 0, 15));
        nodeJarBox.setAlignment(Pos.CENTER_LEFT);
        nodeJarBox.setId("headingBox");
        nodeJarBox.setMinHeight(defaultRowHeight);

        BufferedButton latestJarRadio = new BufferedButton(getRadioOnUrl(), 15);

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

            Utils.getUrlJson(getGitHubLatestJson(), (onSucceeded) -> {
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

        Runnable downloadBtnEffect = () -> {
            if (!getErgoNodesList().getErgoNodes().getNetworksData().getAppData().updatesProperty().get()) {
                if (downloadBtn.getBufferedImageView().getEffect("updateEffectId") == null) {
                    downloadBtn.getBufferedImageView().applyEffect(new InvertEffect("updateEffectId", 0.7));
                }
            } else {
                downloadBtn.getBufferedImageView().removeEffect("updateEffectId");
            }
        };

        getErgoNodesList().getErgoNodes().getNetworksData().getAppData().updatesProperty().addListener((obs, oldVal, newVal) -> {
            downloadBtnEffect.run();
            if (newVal.booleanValue()) {
                getLatestUrl.run();
            }

        });

        if (getErgoNodesList().getErgoNodes().getNetworksData().getAppData().updatesProperty().get()) {
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

        BufferedButton selectJarRadio = new BufferedButton(getRadioOffUrl(), 15);
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

            File nodeJar = chooser.showOpenDialog(m_setupStage);
            if (nodeJar != null) {
                if (Utils.checkJar(nodeJar)) {
                    jarFileBtn.setText(nodeJar.getAbsolutePath());
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
                latestJarRadio.setImage(new Image(getRadioOnUrl()));
                selectJarRadio.setImage(new Image(getRadioOffUrl()));
            } else {
                latestJarRadio.setImage(new Image(getRadioOffUrl()));
                selectJarRadio.setImage(new Image(getRadioOnUrl()));
            }
        });

        VBox jarBodyBox = new VBox(latestJarBox, latestJarNameBox, latestJarUrlBox, customBox, jarFileBox);
        jarBodyBox.setPadding(new Insets(15));
        jarBodyBox.setId("bodyBox");
        HBox.setHgrow(jarBodyBox, Priority.ALWAYS);

        HBox jarbodyPadBox = new HBox(jarBodyBox);
        jarbodyPadBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(jarbodyPadBox, Priority.ALWAYS);

        VBox jarBox = new VBox(nodeJarBox, jarbodyPadBox);
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

    public void settings() {
        if (m_setupStage != null) {
            m_setupStage.close();
            m_setupStage = null;
        }

        if (m_settingsStage == null) {
            Button okBtn = new Button();

            m_settingsStage = new Stage();
            m_settingsStage.getIcons().add(getIcon());
            m_settingsStage.setResizable(false);
            m_settingsStage.initStyle(StageStyle.UNDECORATED);

            SimpleObjectProperty<NamedNodeUrl> namedNode = new SimpleObjectProperty<>(namedNodeUrlProperty().get() != null ? namedNodeUrlProperty().get() : new NamedNodeUrl(FriendlyId.createFriendlyId(), "Local Node", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));

            Scene settingsScene = new Scene(new VBox(), 400, 400);//getSettingsScene(namedNode, okBtn, m_settingsStage);

            m_settingsStage.setScene(settingsScene);

            m_settingsStage.show();

        } else {

            if (m_settingsStage.isIconified()) {
                m_settingsStage.setIconified(false);
            }
            m_settingsStage.show();
            m_settingsStage.toFront();
        }
    }

    public void initalSetup() {
        if (m_setupStage == null) {

            SimpleObjectProperty<File> directory = new SimpleObjectProperty<File>(getErgoNodesList().getErgoNodes().getAppDir());
            TextField folderNameField = new TextField(DEFAULT_NODE_NAME);

            MenuButton settingsModeBtn = new MenuButton(SettingsMode.BASIC);
            MenuButton settingsDigestAccess = new MenuButton(DigestAccess.LOCAL);
            MenuButton settingsBlockchainMode = new MenuButton(BlockchainMode.PRUNED);
            SimpleStringProperty settingsApiKey = new SimpleStringProperty(null);

            SimpleObjectProperty<File> configFile = new SimpleObjectProperty<>(null);

            Button nextBtn = new Button("Next");

            m_setupStage = new Stage();
            m_setupStage.getIcons().add(getIcon());
            m_setupStage.setResizable(false);
            m_setupStage.initStyle(StageStyle.UNDECORATED);

            Scene initialScene = initialSetupScene(nextBtn, settingsModeBtn, settingsDigestAccess, settingsBlockchainMode, settingsApiKey, configFile, directory, folderNameField, m_setupStage);
            m_setupStage.setScene(initialScene);

            m_setupStage.show();

            nextBtn.setOnAction(e -> {
                final String settingsMode = settingsModeBtn.getText();
                final String digestMode = settingsDigestAccess.getText();
                final String blockchainMode = settingsBlockchainMode.getText();
                final String apiKey = settingsApiKey.get();
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
                    install(settingsMode, digestMode, blockchainMode, apiKey, directory, folderNameField, initialScene);
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

    public static HashData updateConfigFile(String apiKeyString, String digestMode, String blockchainMode, File configFile) throws Exception {

        final byte[] apiHashbytes = Utils.digestBytesToBytes(apiKeyString.getBytes(), Blake2b.BLAKE2_B_256);
        final String apiKeyHashString = new String(apiHashbytes);

        String configFileString = "ergo {";
        configFileString += "\n  directory = ${ergo.directory}\"/.ergo\"";
        configFileString += "\n  node {\n";
        configFileString += "\n    stateType = \"digest\"";
        configFileString += "\n    mining = false";

        switch (blockchainMode) {
            case BlockchainMode.RECENT_ONLY:
                configFileString += "\n    blocksToKeep = 1440";
            case BlockchainMode.PRUNED:
                configFileString += "\n    utxo {";
                configFileString += "\n        utxoBootstrap = true";
                configFileString += "\n        storingUtxoSnapshots = 0";
                configFileString += "\n        p2pUtxoSnapshots = 2";
                configFileString += "\n    }";
                configFileString += "\n";
                configFileString += "\n    nipopow {";
                configFileString += "\n        nipopowBootstrap = true";
                configFileString += "\n        p2pNipopows = 2";
                configFileString += "\n    }\n";
                configFileString += "\n  }";
                configFileString += "\n}";
                break;
            case BlockchainMode.FULL:
            default:
                configFileString += "\n    blocksToKeep = -1";
                configFileString += "\n    utxo {";
                configFileString += "\n        utxoBootstrap = false";
                configFileString += "\n        storingUtxoSnapshots = 0";
                configFileString += "\n        p2pUtxoSnapshots = 2";
                configFileString += "\n    }";
                configFileString += "\n";
                configFileString += "\n    nipopow {";
                configFileString += "\n        nipopowBootstrap = false";
                configFileString += "\n        p2pNipopows = 2";
                configFileString += "\n    }\n";
                configFileString += "\n  }";
                configFileString += "\n}";
        }

        configFileString += "\nscorex {\n";
        configFileString += "\n  restApi {";
        configFileString += "\n    bindAddress = \"0.0.0.0:" + ErgoNodes.MAINNET_PORT + "\"";
        configFileString += "\n    apiKeyHash = \"" + apiKeyHashString + "\"";
        configFileString += "\n  }";
        configFileString += "\n}";

        Files.writeString(configFile.toPath(), configFileString);

        byte[] fileBytes = Utils.digestFile(configFile, Blake2b.BLAKE2_B_256);

        return new HashData(fileBytes);
    }

    public void install(String settingsMode, String digestMode, String blockchainMode, String apiKeyString, SimpleObjectProperty<File> directory, TextField folderNameField, Scene initialScene) {
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
                    a.initOwner(m_settingsStage);
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
                        if (settingsMode.equals(SettingsMode.BASIC)) {
                            File configFile = new File(installDir + "/" + "ergo.conf");
                            HashData configHashData = null;
                            try {
                                configHashData = updateConfigFile(apiKeyString, digestMode, blockchainMode, configFile);
                            } catch (Exception e1) {
                                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                                a.initOwner(m_settingsStage);
                                a.setHeaderText("Config File Error");
                                a.setTitle("Config File Error - Setup - Ergo Nodes");
                                a.show();
                            }
                            if (configHashData != null) {
                                m_configFile = configFile;
                                m_configFileHashData = configHashData;
                                m_setuped = true;
                            }

                        }
                    };

                    if (installDir.isDirectory()) {

                        if (isDownload) {
                            File coreFile = new File(installDir + "/" + downloadFileName.get());

                            Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, downloadFileName, progressBar, m_setupStage);
                            m_setupStage.setScene(progressScene);
                            Utils.getUrlFileHash(downloadUrl.get(), coreFile, (onSucceeded) -> {
                                Object sourceObject = onSucceeded.getSource().getValue();
                                if (sourceObject != null && sourceObject instanceof HashData) {
                                    m_nodeDir = installDir;
                                    m_nodeJar = coreFile;
                                    m_coreFileHashData = (HashData) sourceObject;

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
                                File coreFile = new File(installDir.getAbsolutePath() + "/" + fileNameProperty.get());

                                Scene progressScene = App.getProgressScene(ErgoNodes.getSmallAppIcon(), "Downloading", "Setup - " + ErgoNodes.NAME, fileNameProperty, progressBar, m_setupStage);
                                m_setupStage.setScene(progressScene);

                                if (customFile.getAbsolutePath().equals(coreFile.getAbsolutePath())) {
                                    String errorString = null;
                                    byte[] bytes = null;
                                    try {
                                        bytes = Utils.digestFile(coreFile, null);
                                    } catch (Exception e1) {
                                        errorString = e1.toString();
                                    }
                                    if (bytes != null) {
                                        m_nodeDir = installDir;
                                        m_nodeJar = coreFile;
                                        m_coreFileHashData = new HashData(bytes);

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

                                    Utils.moveFileAndHash(customFile, coreFile, onSucceeded -> {

                                        Object sourceObject = onSucceeded.getSource().getValue();
                                        if (sourceObject != null && sourceObject instanceof HashData) {
                                            m_nodeDir = installDir;
                                            m_nodeJar = coreFile;
                                            m_coreFileHashData = (HashData) sourceObject;

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
    public HBox getRowItem() {
        String initialDefaultId = getErgoNodesList().defaultIdProperty().get();
        boolean isDefault = initialDefaultId != null && getId().equals(initialDefaultId);
        BufferedButton defaultBtn = new BufferedButton(isDefault ? getRadioOnUrl() : getRadioOffUrl(), 15);
        defaultBtn.setOnAction(e -> {
            getErgoNodesList().setDefaultId(getId());
        });
        statusStringProperty().set(getIsSetup() ? "Offline" : "(Not Installed)");

        Text topInfoStringText = new Text((namedNodeUrlProperty().get() != null ? (getName() == null ? "INVALID" : getName()) : "INVALID"));
        topInfoStringText.setFont(getFont());
        topInfoStringText.setFill(getPrimaryColor());

        Text topRightText = new Text(getClientTypeName());
        topRightText.setFont(getSmallFont());
        topRightText.setFill(getSecondaryColor());

        Text botTimeText = new Text();
        botTimeText.setFont(getSmallFont());
        botTimeText.setFill(getSecondaryColor());
        botTimeText.textProperty().bind(cmdStatusUpdatedProperty());

        TextField centerField = new TextField();
        centerField.setFont(getLargeFont());
        centerField.setId(isDefault ? "textField" : "formField");
        centerField.setEditable(false);
        centerField.setAlignment(Pos.CENTER);
        centerField.setPadding(new Insets(0, 10, 0, 0));

        centerField.textProperty().bind(statusStringProperty());

        getErgoNodesList().defaultIdProperty().addListener((obs, oldval, newVal) -> {
            defaultBtn.getBufferedImageView().setDefaultImage(new Image(newVal != null && getId().equals(newVal) ? getRadioOnUrl() : getRadioOffUrl()));
            if (newVal != null && getId().equals(newVal)) {
                centerField.setId("textField");
                centerField.setFont(getLargeFont());
            } else {
                centerField.setId("formField");
            }

        });

        Text middleTopRightText = new Text();
        middleTopRightText.setFont(getFont());
        middleTopRightText.setFill(getSecondaryColor());

        middleTopRightText.textProperty().bind(cmdProperty());

        Text middleBottomRightText = new Text(getNetworkTypeString());
        middleBottomRightText.setFont(getFont());
        middleBottomRightText.setFill(getPrimaryColor());

        VBox centerRightBox = new VBox(middleTopRightText, middleBottomRightText);
        centerRightBox.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(centerRightBox, Priority.ALWAYS);

        Tooltip statusBtnTip = new Tooltip(statusProperty().get().equals(MarketsData.STOPPED) ? (getIsSetup() ? "Start" : "Install") : "Stop");
        statusBtnTip.setShowDelay(new Duration(100));
        //m_startImgUrl : m_stopImgUrl
        BufferedButton statusBtn = new BufferedButton(statusProperty().get().equals(MarketsData.STOPPED) ? (getIsSetup() ? getStartImgUrl() : getInstallImgUrl()) : getStopImgUrl(), 15);
        statusBtn.setId("statusBtn");
        statusBtn.setPadding(new Insets(0, 10, 0, 10));
        statusBtn.setTooltip(statusBtnTip);
        statusBtn.setOnAction(action -> {
            if (statusProperty().get().equals(MarketsData.STOPPED)) {
                if (getIsSetup()) {
                    start();
                } else {
                    initalSetup();
                }
            } else {
                shutdownNowProperty().set(LocalDateTime.now());
            }
        });

        statusProperty().addListener((obs, oldVal, newVal) -> {
            switch (statusProperty().get()) {
                case MarketsData.STOPPED:
                    statusBtnTip.setText(getIsSetup() ? "Start" : "Install");
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getIsSetup() ? getStartImgUrl() : getInstallImgUrl()), 15);
                    break;
                default:
                    statusBtnTip.setText("Stop");
                    statusBtn.getBufferedImageView().setDefaultImage(new Image(getStopImgUrl()), 15);
                    break;
            }
        });

        HBox leftBox = new HBox(defaultBtn);
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

        Text ipText = new Text(namedNodeUrlProperty().get() != null ? (namedNodeUrlProperty().get().getIP() == null ? "-" : namedNodeUrlProperty().get().getIP()) : "-");
        ipText.setFill(getPrimaryColor());
        ipText.setFont(getSmallFont());

        Region bottomMiddleRegion = new Region();
        HBox.setHgrow(bottomMiddleRegion, Priority.ALWAYS);

        HBox bottomBox = new HBox(ipText, bottomMiddleRegion, botTimeText);
        bottomBox.setId("darkBox");
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(bottomBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(topSpacer, topBox, centerBox, bottomBox, bottomSpacer);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox rowBox = new HBox(leftBox, bodyBox, rightBox);
        rowBox.setPadding(new Insets(0, 0, 5, 0));
        rowBox.setAlignment(Pos.CENTER_RIGHT);
        rowBox.setId("rowBox");

        return rowBox;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();

        json.addProperty("setuped", m_setuped);
        json.addProperty("runOnStart", m_runOnStart);
        if (m_nodeDir != null && m_nodeDir.isDirectory()) {
            json.addProperty("nodeDir", m_nodeDir.getAbsolutePath());
        }
        if (m_nodeJar != null && m_nodeJar.isFile()) {
            json.addProperty("nodeJar", m_nodeJar.getAbsolutePath());
        }
        return json;

    }
}
