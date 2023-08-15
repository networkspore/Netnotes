package com.netnotes;

/**
 * Netnotes
 *
 */
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.input.KeyCode;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.StandardCopyOption;
import java.security.Security;
import java.security.spec.KeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netnotes.IconButton.IconStyle;
import com.rfksystems.blake2b.security.Blake2bProvider;

import com.utils.Utils;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

public class App extends Application {

    public static final String CMD_SHOW_APPSTAGE = "SHOW_APPSTAGE";
    public static final long NOTE_EXECUTION_TIME = 100;
    public static final String notesFileName = "notes.dat";

    public static final String GET_DATA = "GET_DATA";

    private File logFile = new File("log.txt");
    //public members
    public static Font mainFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    public static Font txtFont = Font.font("OCR A Extended", 15);
    public static Font titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
    public static Color txtColor = Color.web("#cdd4da");
    public static Color altColor = Color.web("#777777");
    public static Color formFieldColor = new Color(.8, .8, .8, .9);

    public static Image icon = new Image("/assets/icon20.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image ergoLogo = new Image("/assets/ergo-network.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");
    public static Image addImg = new Image("/assets/add-outline-white-40.png");
    public static Image closeImg = new Image("/assets/close-outline-white.png");
    public static Image minimizeImg = new Image("/assets/minimize-white-20.png");
    public static Image globeImg = new Image("/assets/globe-outline-white-120.png");
    public static Image settingsImg = new Image("/assets/settings-outline-white-120.png");
    public static Image lockDocumentImg = new Image("/assets/document-lock.png");
    public static Image arrowRightImg = new Image("/assets/arrow-forward-outline-white-20.png");
    public static Image ergoWallet = new Image("/assets/ergo-wallet.png");
    public static Image atImage = new Image("/assets/at-white-240.png");
    public static Image branchImg = new Image("/assets/git-branch-outline-white-240.png");
    public static Image ergoExplorerImg = new Image("/assets/ergo-explorer.png");
    public static Image kucoinImg = new Image("/assets/kucoin-100.png");

    public static Image walletLockImg20 = new Image("/assets/wallet-locked-outline-white-20.png");

    public static Image openImg = new Image("/assets/open-outline-white-20.png");
    public static Image diskImg = new Image("/assets/save-outline-white-20.png");

    public static String settingsFileName = "settings.conf";
    public static String networksFileName = "networks.dat";

    public static final String homeString = System.getProperty("user.home");

    private File settingsFile = null;

    private File currentDir = null;
    private File launcherFile = null;
    private File currentJar = null;

    private NetworksData m_networksData;

    private HostServices m_networkServices = getHostServices();
    private java.awt.SystemTray m_tray;
    private java.awt.TrayIcon m_trayIcon;
    private final static long EXECUTION_TIME = 500;
    private Stage m_stage;

    private void parseArgs(List<String> args, Stage appStage) {

        if (args.size() > 0) {

            String argString = args.get(0);

            byte[] bytes = Hex.decode(argString);

            String jsonString = new String(bytes, StandardCharsets.UTF_8);

            JsonObject obj = new JsonParser().parse(jsonString).getAsJsonObject();
            JsonElement jarFileElement = obj.get("jarFilePath");
            JsonElement launcherFiElement = obj.get("launcher");

            if (jarFileElement != null && launcherFiElement != null) {
                String jarFilePathString = jarFileElement.getAsString();
                String launcherFilePathString = launcherFiElement.getAsString();

                currentJar = new File(jarFilePathString);
                currentDir = currentJar.getParentFile();
                launcherFile = new File(launcherFilePathString);
                File launcherDir = launcherFile.getParentFile();

                File destinationFile = new File(currentDir.getAbsolutePath() + "/" + launcherFile.getName());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {

                        }

                        try {
                            File desktopFile = new File(homeString + "/Desktop");

                            if (!launcherFile.getAbsolutePath().equals(destinationFile.toString())) {

                                Files.move(launcherFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                Utils.createLink(destinationFile, launcherDir, "Net Notes.lnk");

                            }

                            Utils.createLink(destinationFile, desktopFile, "Net Notes.lnk");

                        } catch (Exception e) {

                        }

                    }
                }).start();

            }

        } else {
            currentDir = new File(System.getProperty("user.dir"));
        }

    }

    @Override
    public void start(Stage appStage) {
        //  Platform.setImplicitExit(true);
        AppData appData = null;
        appStage.setResizable(false);
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Netnotes");
        appStage.getIcons().add(logo);

        Parameters params = getParameters();
        List<String> list = params.getRaw();

        parseArgs(list, appStage);

        if (currentDir != null) {
            String currentDirString = currentDir.getAbsolutePath();

            settingsFile = new File(currentDirString + "/" + settingsFileName);

            if (!settingsFile.isFile()) {

                Alert a = new Alert(AlertType.NONE, "Unable to access user app data. Ensure you have access to:\n\nLocation: " + currentDir.getAbsolutePath() + "\n" + launcherFile.getAbsolutePath(), ButtonType.CLOSE);
                a.showAndWait();
                shutdownNow();

            } else {

                try {
                    appData = new AppData(settingsFile);

                } catch (Exception e) {
                    Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                    a.showAndWait();
                }

                if (appData != null && appData.getAppKey() != null) {

                    startApp(appData, appStage);
                } else {
                    //init?
                    shutdownNow();
                }
            }

        } else {
            Alert a = new Alert(AlertType.NONE, "Unable to open Net Notes.", ButtonType.CLOSE);
            a.initOwner(appStage);
            a.showAndWait();
            shutdownNow();
        }

    }

    public void startApp(AppData appData, Stage appStage) {
        if (Security.getProvider("BLAKE2B") == null) {
            Security.addProvider(new Blake2bProvider());

        }
        appStage.setTitle("Net Notes - Enter Password");

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, "Net Notes - Enter Password", closeBtn, appStage);

        Button imageButton = createImageButton(logo, "Net Notes");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(20, 0, 0, 0));

        Button clickRegion = new Button();
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, 600, 300);

        passwordScene.getStylesheets().add("/css/startWindow.css");
        appStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            shutdownNow();
        });

        Stage statusStage = getStatusStage("Net Notes - Verifying...", "Verifying..");

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {

                    statusStage.show();

                    FxTimer.runLater(Duration.ofMillis(100), () -> {
                        char[] chars = passwordField.getText().toCharArray();
                        Platform.runLater(() -> passwordField.setText(""));
                        byte[] hashBytes = appData.getAppKeyBytes();
                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(chars, hashBytes);
                        statusStage.close();
                        if (result.verified) {

                            try {
                                openNetnotes(appData, createKey(chars), appStage);
                                chars = null;
                            } catch (Exception e1) {
                                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CLOSE);
                                a.initOwner(appStage);
                                a.show();
                            }

                        }
                    });
                }
            }
        });
        appStage.show();
    }

    private static SecretKey createKey(char[] chars) throws Exception {

        byte[] charBytes = Utils.charsToBytes(chars);

        charBytes = Utils.digestBytesToBytes(charBytes);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(chars, charBytes, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        return secret;
    }

    public static Stage getStatusStage(String title, String statusMessage) {
        Stage statusStage = new Stage();
        statusStage.setResizable(false);
        statusStage.initStyle(StageStyle.UNDECORATED);
        statusStage.setTitle("Net Notes - Verifying");
        statusStage.getIcons().add(logo);

        statusStage.setTitle(title);

        Label newTitleLbl = new Label(title);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        ImageView barIconView = new ImageView(icon);
        barIconView.setFitHeight(20);
        barIconView.setPreserveRatio(true);

        HBox newTopBar = new HBox(barIconView, newTitleLbl);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(10, 8, 10, 10));
        newTopBar.setId("topBar");

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);

        Text statusTxt = new Text(statusMessage);
        statusTxt.setFill(txtColor);
        statusTxt.setFont(txtFont);

        VBox bodyVBox = new VBox(imageBox, statusTxt);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(newTopBar, bodyVBox);

        Scene statusScene = new Scene(layoutVBox, 420, 220);
        statusScene.getStylesheets().add("/css/startWindow.css");

        statusStage.setScene(statusScene);
        return statusStage;
    }

    static class Delta {

        double x, y;
    }

    // private static int createTries = 0;
    private void openNetnotes(AppData appData, SecretKey appKey, Stage appStage) {
        File networksFile = new File(networksFileName);

        boolean isNetworksFile = networksFile.isFile();

        m_networksData = new NetworksData(appData, appKey, m_networkServices, networksFile, isNetworksFile);

        m_stage = new Stage();
        m_stage.initStyle(StageStyle.UTILITY);
        m_stage.setOpacity(0);
        m_stage.setHeight(0);
        m_stage.setWidth(0);
        m_stage.show();

        m_networksData.cmdSwitchProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                com.grack.nanojson.JsonObject cmdObject = m_networksData.cmdSwitchProperty().get();
                String type = cmdObject.getString("type");

                if (type != null) {
                    if (type.equals("CMD")) {
                        String cmd = cmdObject.getString("cmd");
                        Platform.runLater(() -> {
                            switch (cmd) {

                                case CMD_SHOW_APPSTAGE:
                                    //  Alert a = new Alert(AlertType.NONE, "msg", ButtonType.CLOSE);
                                    //  a.show();
                                    if (appStage.isIconified()) {
                                        appStage.requestFocus();
                                        appStage.setIconified(false);
                                        appStage.show();
                                        appStage.toFront();

                                    } else {
                                        if (appStage.isShowing()) {

                                            appStage.show();
                                            appStage.toFront();
                                            appStage.requestFocus();

                                        } else {

                                            verifyAppKey(() -> {

                                                appStage.show();

                                            });

                                        }
                                    }

                                    break;
                            }
                        });
                    }
                }
            }
        });

        if (java.awt.SystemTray.isSupported()) {

            javax.swing.SwingUtilities.invokeLater(this::addAppToTray);
        }

        showMainStage(appStage, isNetworksFile);

    }

    public void verifyAppKey(Runnable runnable) {

        String title = "Net Notes - Enter Password";

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, title, closeBtn, passwordStage);

        Button imageButton = createImageButton(logo, "Net Notes");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(20, 0, 0, 0));

        Button clickRegion = new Button();
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, 600, 320);

        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        Stage statusStage = getStatusStage("Net Notes - Verifying...", "Verifying..");

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {

                    statusStage.show();

                    FxTimer.runLater(Duration.ofMillis(100), () -> {

                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(passwordField.getText().toCharArray(), m_networksData.getAppData().getAppKeyBytes());
                        Platform.runLater(() -> passwordField.setText(""));
                        statusStage.close();
                        if (result.verified) {
                            passwordStage.close();

                            runnable.run();

                        }

                    });
                }
            }
        });

        closeBtn.setOnAction(e -> {
            passwordStage.close();

        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });

        passwordStage.show();

    }
    private ScheduledFuture<?> m_lastExecution = null;

    private void showMainStage(Stage appStage, boolean isNetworksFile) {

        Button closeBtn = new Button();
        Button settingsBtn = new Button();
        Button networksBtn = new Button();
        Button maximizeBtn = new Button();

        appStage.setTitle("Net Notes: Networks");

        HBox titleBox = createTopBar(icon, maximizeBtn, closeBtn, appStage);

        VBox menuBox = createMenu(settingsBtn, networksBtn);
        networksBtn.setId("activeMenuBtn");
        VBox.setVgrow(menuBox, Priority.ALWAYS);

        VBox bodyVBox = new VBox();
        HBox.setHgrow(bodyVBox, Priority.ALWAYS);
        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        bodyVBox.setId("bodyBox");
        bodyVBox.setPadding(new Insets(0, 5, 5, 5));

        Region vBar = new Region();
        VBox.setVgrow(vBar, Priority.ALWAYS);
        vBar.setPrefWidth(2);
        vBar.setId("vGradient");

        VBox headerBox = new VBox();
        headerBox.setPadding(new Insets(0, 2, 5, 2));
        headerBox.setId("bodyBox");

        VBox bodyBox = new VBox(headerBox, bodyVBox);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        HBox mainHbox = new HBox(menuBox, vBar, bodyBox);
        VBox.setVgrow(mainHbox, Priority.ALWAYS);

        VBox layout = new VBox(titleBox, mainHbox);
        VBox.setVgrow(layout, Priority.ALWAYS);
        layout.setPadding(new Insets(0, 2, 2, 2));

        Scene appScene = new Scene(layout, m_networksData.getStageWidth(), m_networksData.getStageHeight());
        appScene.getStylesheets().add("/css/startWindow.css");
        // appStage.setScene(appScene);

        settingsBtn.setOnAction(e -> {
            networksBtn.setId("menuBtn");
            settingsBtn.setId("activeMenuBtn");
            headerBox.getChildren().clear();
            showSettings(appStage, bodyVBox);
        });

        networksBtn.setOnAction(e -> {
            networksBtn.setId("activeMenuBtn");
            settingsBtn.setId("menuBtn");
            showNetworks(appScene, headerBox, bodyVBox);
        });

        appStage.setScene(appScene);
        showNetworks(appScene, headerBox, bodyVBox);

        Rectangle rect = m_networksData.getMaximumWindowBounds();
        ResizeHelper.addResizeListener(appStage, 400, 200, rect.getWidth(), rect.getHeight());

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });
        Runnable save = () -> {
            m_networksData.save();

        };

        appStage.widthProperty().addListener((obs, oldval, newVal) -> {
            m_networksData.setStageWidth(newVal.doubleValue());
            m_networksData.updateNetworksGrid();
            if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                m_lastExecution.cancel(false);
            }

            m_lastExecution = executor.schedule(save, EXECUTION_TIME, TimeUnit.MILLISECONDS);
        });
        appStage.heightProperty().addListener((obs, oldval, newVal) -> {
            m_networksData.setStageHeight(newVal.doubleValue());

            if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                m_lastExecution.cancel(false);
            }

            m_lastExecution = executor.schedule(save, EXECUTION_TIME, TimeUnit.MILLISECONDS);
        });

        maximizeBtn.setOnAction(maxEvent -> {
            boolean maximized = appStage.isMaximized();
            m_networksData.setStageMaximized(!maximized);

            if (!maximized) {
                m_networksData.setStagePrevWidth(appStage.getWidth());
                m_networksData.setStagePrevHeight(appStage.getHeight());
            }

            appStage.setMaximized(!maximized);
        });

        closeBtn.setOnAction(e -> {
            m_tray.remove(m_trayIcon);
            m_networksData.shutdown();
            appStage.close();
            shutdownNow();
        });

        appStage.setOnCloseRequest(e -> {
            m_tray.remove(m_trayIcon);
            m_networksData.shutdown();
            shutdownNow();
        });

        if (java.awt.SystemTray.isSupported()) {
            BufferedButton sleepBtn = new BufferedButton("/assets/moon-15.png", 15);

            sleepBtn.setOnAction(e -> {
                appStage.hide();
            });
            titleBox.getChildren().add(sleepBtn);
        }

        if (m_networksData.getStageMaximized()) {

            appStage.setMaximized(true);
        }
        if (!isNetworksFile) {

            m_networksData.showManageNetworkStage();
        }
    }

    private void showNetworks(Scene appScene, VBox header, VBox bodyVBox) {

        bodyVBox.getChildren().clear();

        Tooltip addTip = new Tooltip("Networks");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        IconButton manageButton = new IconButton(new Image("/assets/filter.png"), "", IconStyle.ICON);
        manageButton.setImageWidth(15);
        manageButton.setPadding(new Insets(3, 6, 3, 6));
        manageButton.setTooltip(addTip);
        manageButton.setOnAction(e -> m_networksData.showManageNetworkStage());

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        Tooltip gridTypeToolTip = new Tooltip("Toggle: List view");
        gridTypeToolTip.setShowDelay(new javafx.util.Duration(50));
        gridTypeToolTip.setHideDelay(new javafx.util.Duration(200));

        BufferedButton toggleGridTypeButton = new BufferedButton("/assets/list-outline-white-25.png", 25);
        toggleGridTypeButton.setTooltip(gridTypeToolTip);
        toggleGridTypeButton.setPadding(new Insets(0, 0, 0, 0));

        HBox menuBar = new HBox(manageButton, menuSpacer, toggleGridTypeButton);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 5, 1, 5));

        header.getChildren().clear();
        header.getChildren().add(menuBar);

        VBox gridBox = m_networksData.getNetworksBox();

        ScrollPane scrollPane = new ScrollPane(gridBox);
        scrollPane.setPadding(new Insets(5, 0, 5, 0));
        scrollPane.prefViewportWidthProperty().bind(appScene.widthProperty().subtract(90));
        scrollPane.prefViewportHeightProperty().bind(appScene.heightProperty().subtract(menuBar.heightProperty().get()).subtract(50));

        bodyVBox.getChildren().addAll(scrollPane);

        toggleGridTypeButton.setOnAction(e -> {

            m_networksData.iconStyleProperty().set(m_networksData.iconStyleProperty().get().equals(IconStyle.ROW) ? IconStyle.ICON : IconStyle.ROW);

        });

        /*
        addButton.setOnAction(clickEvent -> {
            Network newNetwork = showNetworkStage(null);

            refreshNetworksGrid(gridBox);
        });*/
    }

    private void showSettings(Stage appStage, VBox bodyVBox) {
        bodyVBox.getChildren().clear();

        boolean isUpdates = m_networksData.getAppData().getUpdates();

        Button settingsButton = createImageButton(logo, "Settings");

        HBox settingsBtnBox = new HBox(settingsButton);
        settingsBtnBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text(String.format("%-12s", "  Password:"));
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        Button passwordBtn = new Button("(click to update)");
        passwordBtn.setFont(txtFont);
        passwordBtn.setId("toolBtn");
        passwordBtn.setOnAction(e -> {
            Stage passwordStage = new Stage();

            createPassword("Net Notes - Password", logo, logo, passwordStage, (onSuccess) -> {
                Object sourceObject = onSuccess.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof String) {
                    String newPassword = (String) sourceObject;

                    if (!newPassword.equals("")) {

                        Stage statusStage = getStatusStage("Net Notes - Saving...", "Saving...");
                        statusStage.show();
                        FxTimer.runLater(Duration.ofMillis(100), () -> {
                            String hash = Utils.getBcryptHashString(newPassword);

                            try {

                                m_networksData.getAppData().setAppKey(hash);
                            } catch (IOException e1) {
                                Alert a = new Alert(AlertType.NONE, "Error: Password not changed.\n\n" + e1.toString(), ButtonType.CLOSE);
                                a.setTitle("Error: Password not changed.");
                                a.initOwner(appStage);
                                a.show();
                            }

                            statusStage.close();

                        });
                    } else {
                        Alert a = new Alert(AlertType.NONE, "Net Notes: Passwod not change.\n\nCanceled by user.", ButtonType.CLOSE);
                        a.setTitle("Net Notes: Password not changed");
                        a.initOwner(appStage);
                        a.show();
                    }
                }
                passwordStage.close();
            });

        });

        HBox passwordBox = new HBox(passwordTxt, passwordBtn);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(10, 0, 0, 20));

        Text updatesTxt = new Text(String.format("%-12s", "  Updates:"));
        updatesTxt.setFill(txtColor);
        updatesTxt.setFont(txtFont);

        Button updatesBtn = new Button(isUpdates ? "Enabled" : "Disabled");
        updatesBtn.setFont(txtFont);
        updatesBtn.setId("toolBtn");
        updatesBtn.setOnAction(e -> {
            try {
                if (updatesBtn.getText().equals("Enabled")) {
                    updatesBtn.setText("Disabled");
                    m_networksData.getAppData().setUpdates(false);
                } else {
                    updatesBtn.setText("Enabled");
                    m_networksData.getAppData().setUpdates(true);
                }
            } catch (IOException e1) {
                Alert a = new Alert(AlertType.NONE, "Error:\n\n" + e1.toString(), ButtonType.CLOSE);
                a.initOwner(appStage);
                a.show();
            }

        });

        HBox updatesBox = new HBox(updatesTxt, updatesBtn);
        updatesBox.setAlignment(Pos.CENTER_LEFT);
        updatesBox.setPadding(new Insets(0, 0, 0, 20));

        bodyVBox.getChildren().addAll(settingsBtnBox, passwordBox, updatesBox);
    }

    private VBox createMenu(Button settingsBtn, Button networksBtn) {
        double menuSize = 35;

        ImageView networkImageView = highlightedImageView(globeImg);
        networkImageView.setFitHeight(menuSize);
        networkImageView.setPreserveRatio(true);

        Tooltip networkToolTip = new Tooltip("Networks");
        networkToolTip.setShowDelay(new javafx.util.Duration(100));
        networksBtn.setGraphic(networkImageView);
        networksBtn.setId("menuBtn");
        networksBtn.setTooltip(networkToolTip);

        ImageView settingsImageView = highlightedImageView(settingsImg);
        settingsImageView.setFitHeight(menuSize);
        settingsImageView.setPreserveRatio(true);

        Tooltip settingsTooltip = new Tooltip("Settings");
        settingsTooltip.setShowDelay(new javafx.util.Duration(100));
        settingsBtn.setGraphic(settingsImageView);
        settingsBtn.setId("menuBtn");
        settingsBtn.setTooltip(settingsTooltip);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox menuBox = new VBox(networksBtn, spacer, settingsBtn);
        VBox.setVgrow(menuBox, Priority.ALWAYS);
        menuBox.setId("menuBox");
        menuBox.setPadding(new Insets(2, 0, 2, 2));

        return menuBox;
    }

    public static File getFile(String title, Stage owner, FileChooser.ExtensionFilter... extensionFilters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        //  fileChooser.setInitialFileName(initialFileName);

        fileChooser.getExtensionFilters().addAll(extensionFilters);
        File file = fileChooser.showOpenDialog(owner);
        return file;
    }

    public static void createPassword(String topTitle, Image windowLogo, Image mainLogo, Stage passwordStage, EventHandler<WorkerStateEvent> onSucceeded) {

        passwordStage.setTitle(topTitle);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, topTitle, closeBtn, passwordStage);

        Button imageBtn = App.createImageButton(mainLogo, "Password");
        imageBtn.setGraphicTextGap(20);
        HBox imageBox = new HBox(imageBtn);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();

        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        PasswordField createPassField2 = new PasswordField();
        HBox.setHgrow(createPassField2, Priority.ALWAYS);
        createPassField2.setId("passField");

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        Button clickRegion = new Button();
        clickRegion.setMaxWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(Double.MAX_VALUE);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();
        });

        VBox bodyBox = new VBox(passwordBox, clickRegion);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        VBox passwordVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene passwordScene = new Scene(passwordVBox, 600, 300);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordField.setText("");
            passwordStage.close();
        });

        passwordField.setOnKeyPressed(e1 -> {

            KeyCode keyCode = e1.getCode();

            if ((keyCode == KeyCode.ENTER || keyCode == KeyCode.TAB)) {

                String passStr = passwordField.getText();
                // createPassField.setText("");

                bodyBox.getChildren().removeAll(passwordBox, clickRegion);

                Text reenterTxt = new Text("> Confirm password:");
                reenterTxt.setFill(txtColor);
                reenterTxt.setFont(txtFont);

                Platform.runLater(() -> createPassField2.requestFocus());

                HBox secondPassBox = new HBox(reenterTxt, createPassField2);
                secondPassBox.setAlignment(Pos.CENTER_LEFT);

                bodyBox.getChildren().addAll(secondPassBox, clickRegion);

                clickRegion.setOnAction(regionEvent -> {
                    createPassField2.requestFocus();
                });

                createPassField2.setOnKeyPressed(pressEvent -> {

                    KeyCode keyCode2 = pressEvent.getCode();

                    if ((keyCode2 == KeyCode.ENTER)) {

                        if (passStr.equals(createPassField2.getText())) {

                            Utils.returnObject(passStr, onSucceeded, e -> {
                                closeBtn.fire();
                            });
                        } else {
                            bodyBox.getChildren().clear();
                            createPassField2.setText("");
                            passwordField.setText("");

                            secondPassBox.getChildren().clear();
                            bodyBox.getChildren().addAll(passwordBox, clickRegion);

                        }
                    }
                });

            }
        });

    }

    public String getNowTimeString() {
        LocalTime time = LocalTime.now();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(time);
    }

    public static String showGetTextInput(String prompt, String title, Image img) {

        Stage textInputStage = new Stage();
        textInputStage.setTitle(title);
        textInputStage.getIcons().add(logo);
        textInputStage.setResizable(false);
        textInputStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, title, closeBtn, textInputStage);

        Button imageButton = createImageButton(img, title);

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text promptTxt = new Text("> " + prompt + ":");
        promptTxt.setFill(txtColor);
        promptTxt.setFont(txtFont);

        TextField textField = new TextField();
        textField.setFont(txtFont);
        textField.setId("textField");

        closeBtn.setOnAction(event -> {
            textField.setText("");
            textInputStage.close();
        });

        HBox.setHgrow(textField, Priority.ALWAYS);

        Platform.runLater(() -> textField.requestFocus());

        HBox passwordBox = new HBox(promptTxt, textField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        Button clickRegion = new Button();
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            textField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene textInputScene = new Scene(layoutVBox, 600, 425);

        textInputScene.getStylesheets().add("/css/startWindow.css");

        textInputStage.setScene(textInputScene);

        textField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                textInputStage.close();

            }
        });
        textInputStage.showAndWait();
        String returnValue = textField.getText();

        return returnValue.equals("") ? null : returnValue;

    }

    public static HBox createTopBar(Image iconImage, Button fillRightBtn, Button maximizeBtn, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(25);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(theStage.titleProperty().get());
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));
        newTitleLbl.textProperty().bind(theStage.titleProperty());

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        fillRightBtn.setId("toolBtn");
        fillRightBtn.setGraphic(IconButton.getIconView(new Image("/assets/fillRight.png"), 20));
        fillRightBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, fillRightBtn, minimizeBtn, maximizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, Button maximizeBtn, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(25);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(theStage.titleProperty().get());
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));
        newTitleLbl.textProperty().bind(theStage.titleProperty());

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, maximizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, String titleString, Button maximizeBtn, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(25);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(titleString);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, maximizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, String titleString, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(25);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(titleString);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, Label newTitleLbl, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(25);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static Button createImageButton(Image image, String name) {
        ImageView btnImageView = new ImageView(image);
        btnImageView.setFitHeight(135);
        btnImageView.setPreserveRatio(true);

        Button imageBtn = new Button(name);
        imageBtn.setGraphic(btnImageView);
        imageBtn.setId("startImageBtn");
        imageBtn.setFont(mainFont);
        imageBtn.setContentDisplay(ContentDisplay.TOP);

        return imageBtn;
    }

    private static void shutdownNow() {

        Platform.exit();
        System.exit(0);
    }

    public static ImageView highlightedImageView(Image image) {

        ImageView imageView = new ImageView(image);

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {

            imageView.setEffect(colorAdjust);

        });
        imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setEffect(null);
        });

        return imageView;
    }

    public static String confirmPassword(String topTitle, String windowSubTitle, String information) {

        Image windowLogo = ergoLogo;

        Stage passwordStage = new Stage();

        passwordStage.setTitle(topTitle);

        passwordStage.getIcons().add(logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, topTitle, closeBtn, passwordStage);

        Button imageButton = createImageButton(windowLogo, windowSubTitle);

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        Button clickRegion = new Button();
        clickRegion.setMaxWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(Double.MAX_VALUE);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();
        });

        VBox bodyBox = new VBox(passwordBox, clickRegion);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene passwordScene = new Scene(layoutVBox, 600, 330);

        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordBox.getChildren().remove(passwordField);
            passwordField.setDisable(true);
            passwordStage.close();
        });

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();
            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {

                    passwordStage.close();
                }
            }
        });

        passwordStage.showAndWait();

        return passwordField.getText().equals("") ? null : passwordField.getText();
    }

    private void addAppToTray() {
        try {

            java.awt.Toolkit.getDefaultToolkit();

            BufferedImage imgBuf = SwingFXUtils.fromFXImage(new Image("/assets/icon15.png"), null);

            m_tray = java.awt.SystemTray.getSystemTray();

            m_trayIcon = new java.awt.TrayIcon((java.awt.Image) imgBuf, "Net Notes");
            m_trayIcon.setActionCommand("show");

            m_trayIcon.addActionListener(event -> Platform.runLater(() -> {
                if (event.getActionCommand().equals("show")) {
                    m_networksData.show();
                }

            }));

            java.awt.MenuItem openItem = new java.awt.MenuItem("Show Net Notes");
            openItem.addActionListener(event -> Platform.runLater(() -> m_networksData.show()));

            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Close");
            exitItem.addActionListener(event -> Platform.runLater(() -> {
                m_networksData.shutdown();
                m_tray.remove(m_trayIcon);
                shutdownNow();
            }));

            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            m_trayIcon.setPopupMenu(popup);

            m_tray.add(m_trayIcon);

        } catch (java.awt.AWTException e) {

        }

    }
}
