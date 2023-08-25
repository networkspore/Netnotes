package com.netnotes;

import java.awt.Rectangle;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.ergoplatform.appkit.NetworkType;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import com.google.gson.JsonElement;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;

public class FullErgoNode extends ErgoNodeData {

    private String m_githubLatestJson = "https://api.github.com/repos/ergoplatform/ergo/releases/latest";

    private String m_setupImgUrl = "/assets/open-outline-white-20.png";

    private boolean m_setuped = false;
    private boolean m_runOnStart = false;

    private File m_nodeDir = null;
    private File m_nodeJar = null;

    private long m_spaceRequired = 50L * (1024L * 1024L * 1024L);
    private Stage m_setupStage = null;

    public double SETUP_STAGE_MIN_WIDTH = 500;
    public double SETUP_STAGE_MIN_HEIGHT = 655;
    public final static long EXECUTION_TIME = 500;

    private ScheduledFuture<?> m_lastExecution = null;
    private double m_setupStageWidth = SETUP_STAGE_MIN_WIDTH;
    private double m_setupStageHeight = SETUP_STAGE_MIN_HEIGHT;
    private double m_setupStagePrevWidth = SETUP_STAGE_MIN_WIDTH;
    private double m_setupStagePrevHeight = SETUP_STAGE_MIN_HEIGHT;
    private boolean m_setupStageMaximized = false;

    public FullErgoNode(String id, ErgoNodesList ergoNodesList) {
        super(ergoNodesList, FULL_NODE, new NamedNodeUrl(id, "Full Node - Setup", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));
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
        JsonElement setupStageElement = jsonObj == null ? null : jsonObj.get("setupStage");

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
            namedNodeUrlProperty().set(new NamedNodeUrl(FriendlyId.createFriendlyId(), "Full Node - Setup", "127.0.0.1", ErgoNodes.MAINNET_PORT, "", NetworkType.MAINNET));
        }

        if (setupStageElement != null && setupStageElement.isJsonObject()) {
            JsonObject stageObject = setupStageElement.getAsJsonObject();

            JsonElement widthElement = stageObject.get("width");
            JsonElement heightElement = stageObject.get("height");
            JsonElement stagePrevWidthElement = stageObject.get("prevWidth");
            JsonElement stagePrevHeightElement = stageObject.get("prevHeight");
            JsonElement stageMaximizedElement = stageObject.get("maximized");

            boolean maximized = stageMaximizedElement != null && stageMaximizedElement.isJsonPrimitive() ? stageMaximizedElement.getAsBoolean() : false;

            if (!maximized) {
                setSetupStageWidth(widthElement != null && widthElement.isJsonPrimitive() ? widthElement.getAsDouble() : SETUP_STAGE_MIN_WIDTH);
                setSetupStageHeight(heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : SETUP_STAGE_MIN_HEIGHT);
            } else {
                double prevWidth = stagePrevWidthElement != null && stagePrevWidthElement.isJsonPrimitive() ? stagePrevWidthElement.getAsDouble() : SETUP_STAGE_MIN_WIDTH;
                double prevHeight = stagePrevHeightElement != null && stagePrevHeightElement.isJsonPrimitive() ? stagePrevHeightElement.getAsDouble() : SETUP_STAGE_MIN_HEIGHT;

                setSetupStageWidth(prevWidth);
                setSetupStageHeight(prevHeight);

                setSetupStagePrevWidth(prevWidth);
                setSetupStagePrevHeight(prevHeight);

            }
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

    public double getSetupStageWidth() {
        return m_setupStageWidth;
    }

    public void setSetupStageWidth(double width) {
        m_setupStageWidth = width;

    }

    public void setSetupStageHeight(double height) {
        m_setupStageHeight = height;
    }

    public double getSetupStageHeight() {
        return m_setupStageHeight;
    }

    public boolean getSetupStageMaximized() {
        return m_setupStageMaximized;
    }

    public void setSetupStageMaximized(boolean value) {
        m_setupStageMaximized = value;
    }

    public double getSetupStagePrevWidth() {
        return m_setupStagePrevWidth;
    }

    public void setSetupStagePrevWidth(double width) {
        m_setupStagePrevWidth = width;

    }

    public void setSetupStagePrevHeight(double height) {
        m_setupStagePrevHeight = height;
    }

    public double getSetupStagePrevHeight() {
        return m_setupStagePrevHeight;
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

    private Scene initialSetupScene(Button nextBtn, SimpleObjectProperty<File> directoryRoot, TextField directoryNameField, SimpleObjectProperty<File> jarFile, SimpleBooleanProperty getLatestBoolean, SimpleStringProperty downloadUrlProperty, Stage stage) {
        String title = "Setup - Full Node - " + ErgoNodes.NAME;
        m_setupStage.setTitle(title);

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 40;
        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, stage);
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

        VBox headerBox = new VBox(titleBox, headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text directoryText = new Text("Directory");
        directoryText.setFill(App.txtColor);
        directoryText.setFont(App.txtFont);

        HBox directoryBox = new HBox(directoryText);
        directoryBox.setAlignment(Pos.CENTER_LEFT);
        directoryBox.setMinHeight(40);;
        directoryBox.setId("headingBox");
        directoryBox.setPadding(new Insets(0, 0, 0, 15));

        Text directoryRootText = new Text(String.format("%-10s", "Location"));
        directoryRootText.setFill(App.txtColor);
        directoryRootText.setFont(App.txtFont);

        Text directoryNameText = new Text(String.format("%-10s", "Folder"));
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

        Text useableText = new Text("Available Space ");
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
        HBox.setHgrow(useableBox, Priority.ALWAYS);
        useableBox.setPadding(new Insets(0, 0, 0, 15));
        useableBox.setAlignment(Pos.CENTER_LEFT);

        Text requiredText = new Text("Required Space  ");
        requiredText.setFill(getPrimaryColor());
        requiredText.setFont(App.txtFont);

        TextField requiredField = new TextField(Utils.formatedBytes(getSpaceRequired(), 2));
        requiredField.setFont(App.txtFont);
        requiredField.setId("formField");
        requiredField.setEditable(false);
        HBox.setHgrow(requiredField, Priority.ALWAYS);

        HBox requiredBox = new HBox(requiredText, requiredField);
        HBox.setHgrow(requiredBox, Priority.ALWAYS);
        requiredBox.setPadding(new Insets(0, 0, 0, 15));
        requiredBox.setAlignment(Pos.CENTER_LEFT);

        VBox directorySpaceBox = new VBox(useableBox, requiredBox);
        directorySpaceBox.setPadding(new Insets(0, 0, 0, 100));

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

        VBox directoryBodyBox = new VBox(directoryNameBox, directoryRootBox, directorySpaceBox);
        directoryBodyBox.setPadding(new Insets(15));
        directoryBodyBox.setId("bodyBox");
        HBox.setHgrow(directoryBodyBox, Priority.ALWAYS);

        HBox padBox = new HBox(directoryBodyBox);
        padBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(padBox, Priority.ALWAYS);

        VBox directoryPaddingBox = new VBox(directoryBox, padBox);
        HBox.setHgrow(directoryPaddingBox, Priority.ALWAYS);

        VBox jarBodyBox = new VBox(latestJarBox, latestJarNameBox, latestJarUrlBox, customBox, jarFileBox);
        jarBodyBox.setPadding(new Insets(15));
        jarBodyBox.setId("bodyBox");
        HBox.setHgrow(jarBodyBox, Priority.ALWAYS);

        HBox jarbodyPadBox = new HBox(jarBodyBox);
        jarbodyPadBox.setPadding(new Insets(2, 0, 15, 0));
        HBox.setHgrow(jarbodyPadBox, Priority.ALWAYS);

        VBox jarBox = new VBox(nodeJarBox, jarbodyPadBox);
        HBox.setHgrow(jarBox, Priority.ALWAYS);

        nextBtn.setPadding(new Insets(5, 15, 5, 15));
        HBox nextBox = new HBox(nextBtn);
        nextBox.setMinHeight(35);
        nextBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(nextBox, Priority.ALWAYS);

        VBox bodyBox = new VBox(directoryPaddingBox, jarBox, nextBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);

        VBox layoutBox = new VBox(headerBox, bodyPaddingBox, footerBox);
        Scene setupNodeScene = new Scene(layoutBox, m_setupStageWidth, m_setupStageHeight);
        setupNodeScene.getStylesheets().add("/css/startWindow.css");

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });

        Runnable setUpdated = () -> {
            getLastUpdated().set(LocalDateTime.now());
        };

        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            setSetupStageWidth(newVal.doubleValue());

            if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                m_lastExecution.cancel(false);
            }

            m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            setSetupStageHeight(newVal.doubleValue());

            if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                m_lastExecution.cancel(false);
            }

            m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
        });

        rowHeight.bind(Bindings.add(defaultRowHeight, stage.heightProperty().subtract(SETUP_STAGE_MIN_HEIGHT).divide(5)));

        maximizeBtn.setOnAction(maxEvent -> {
            boolean maximized = stage.isMaximized();

            setSetupStageMaximized(!maximized);

            if (!maximized) {
                setSetupStagePrevWidth(stage.getWidth());
                setSetupStagePrevHeight(stage.getHeight());
            }
            setUpdated.run();
            stage.setMaximized(!maximized);
        });

        Runnable closeStage = () -> {
            stage.close();
            m_setupStage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        stage.setOnCloseRequest(e -> closeStage.run());
        return setupNodeScene;
    }

    public void install() {
        if (m_setupStage == null) {
            SimpleBooleanProperty getLatestBoolean = new SimpleBooleanProperty(true);
            SimpleObjectProperty<File> directory = new SimpleObjectProperty<File>(getErgoNodesList().getErgoNodes().getAppDir());
            TextField folderNameField = new TextField("Full Node");
            SimpleObjectProperty<File> jarFile = new SimpleObjectProperty<File>(null);
            SimpleStringProperty downloadUrl = new SimpleStringProperty("https://github.com/ergoplatform/ergo/releases/download/v5.0.13/ergo-5.0.13.jar");

            Button nextBtn = new Button("Next");

            m_setupStage = new Stage();
            m_setupStage.getIcons().add(getIcon());
            m_setupStage.setResizable(false);
            m_setupStage.initStyle(StageStyle.UNDECORATED);

            Scene initialScene = initialSetupScene(nextBtn, directory, folderNameField, jarFile, getLatestBoolean, downloadUrl, m_setupStage);
            m_setupStage.setScene(initialScene);

            Rectangle rect = getErgoNodesList().getErgoNodes().getNetworksData().getMaximumWindowBounds();
            ResizeHelper.addResizeListener(m_setupStage, SETUP_STAGE_MIN_WIDTH, SETUP_STAGE_MIN_HEIGHT, rect.getWidth(), rect.getHeight());

            nextBtn.setOnAction(e -> {
                File dir = directory.get();
                long useableSpace = dir.getUsableSpace();

                if (dir != null && useableSpace > getSpaceRequired()) {

                } else {
                    Alert a = new Alert(AlertType.NONE, "Space required: " + Utils.formatedBytes(getSpaceRequired(), 2) + "\nUseable space: " + Utils.formatedBytes(useableSpace, 2));
                    a.initOwner(m_setupStage);
                    a.setTitle("Insufficient Disk Space - Setup - Ergo Nodes");
                    a.setHeaderText("Insufficient Disk Space");
                    a.show();
                }

            });

            m_setupStage.show();

        } else {
            if (m_setupStage.isIconified()) {
                m_setupStage.setIconified(false);
            }
            m_setupStage.show();
            m_setupStage.toFront();
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
                    install();
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

    public JsonObject getSetupStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("maximized", getSetupStageMaximized());
        json.addProperty("width", getSetupStageWidth());
        json.addProperty("height", getSetupStageHeight());
        json.addProperty("prevWidth", getSetupStagePrevWidth());
        json.addProperty("prevHeight", getSetupStagePrevHeight());
        return json;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.add("setupStage", getSetupStageJson());
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
