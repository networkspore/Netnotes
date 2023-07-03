package com.netnotes;

/**
 * Netnotes
 *
 */
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.StandardCopyOption;
import java.security.Security;
import java.security.spec.KeySpec;
import java.time.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

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

    private File logFile = new File("log.txt");
    //public members
    public static Font mainFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    public static Font txtFont = Font.font("OCR A Extended", 15);
    public static Font titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
    public static Color txtColor = Color.web("#cdd4da");
    public static Color altColor = Color.web("#777777");
    public static Color formFieldColor = new Color(.8, .8, .8, .9);

    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0x028A0F);
    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0x9A2A2A);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

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

    private AppData appData;

    private File settingsFile = null;

    private File currentDir = null;
    private File launcherFile = null;
    private File currentJar = null;

    private NetworksData m_networksData;

    private HostServices m_networkServices = getHostServices();

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

                String passwordHash = null;
                try {
                    appData = new AppData(settingsFile);
                    passwordHash = appData.getAppKey();
                } catch (Exception e) {
                    Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                    a.showAndWait();
                }

                if (passwordHash != null) {

                    startApp(passwordHash.getBytes(), appStage);
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

    public void startApp(byte[] hashBytes, Stage appStage) {
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

        Scene passwordScene = new Scene(layoutVBox, 600, 320);

        passwordScene.getStylesheets().add("/css/startWindow.css");
        appStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            shutdownNow();
        });

        Stage statusStage = new Stage();
        statusStage.setResizable(false);
        statusStage.initStyle(StageStyle.UNDECORATED);
        statusStage.setTitle("Net Notes - Verifying");
        statusStage.getIcons().add(logo);
        setStatusStage(statusStage, "Net Notes - Verifying...", "Verifying..");

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
                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(chars, hashBytes);
                        statusStage.close();
                        if (result.verified) {

                            try {
                                openNetnotes(createKey(chars), appStage);
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

    public static void setStatusStage(Stage appStage, String title, String statusMessage) {

        appStage.setTitle(title);

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

        Text statusTxt = new Text("> " + statusMessage);
        statusTxt.setFill(txtColor);
        statusTxt.setFont(txtFont);

        VBox bodyVBox = new VBox(imageBox, statusTxt);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(newTopBar, bodyVBox);

        Scene statusScene = new Scene(layoutVBox, 420, 220);
        statusScene.getStylesheets().add("/css/startWindow.css");

        appStage.setScene(statusScene);

    }

    static class Delta {

        double x, y;
    }

    // private static int createTries = 0;
    private void openNetnotes(SecretKey appKey, Stage appStage) {
        File networksFile = new File(networksFileName);
        JsonObject networksObject = null;
        boolean notSetup = networksFile.isFile();
        if (notSetup) {
            try {
                String fileString = Files.readString(networksFile.toPath());
                networksObject = new JsonParser().parse(fileString).getAsJsonObject();
            } catch (Exception e) {
                try {
                    Files.writeString(logFile.toPath(), "\n" + e.toString());
                } catch (IOException e1) {

                }
            }
        }

        m_networksData = new NetworksData(appKey, m_networkServices, networksObject, networksFile);

        Button closeBtn = new Button();
        Button settingsBtn = new Button();
        Button networksBtn = new Button();

        appStage.setTitle("Net Notes");

        HBox titleBox = createTopBar(icon, "Net Notes", closeBtn, appStage);

        VBox menuBox = createMenu(settingsBtn, networksBtn);
        networksBtn.setId("activeMenuBtn");
        VBox.setVgrow(menuBox, Priority.ALWAYS);

        VBox bodyVBox = new VBox();
        HBox.setHgrow(bodyVBox, Priority.ALWAYS);
        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        bodyVBox.setId("bodyBox");

        Region vBar = new Region();
        VBox.setVgrow(vBar, Priority.ALWAYS);
        vBar.setPrefWidth(2);
        vBar.setId("vGradient");

        // gBox.setPadding(new Insets(15, 0, 0, 0));
        HBox mainHbox = new HBox(menuBox, vBar, bodyVBox);
        VBox.setVgrow(mainHbox, Priority.ALWAYS);

        VBox layout = new VBox(titleBox, mainHbox);
        VBox.setVgrow(layout, Priority.ALWAYS);

        Scene appScene = new Scene(layout, 800, 450);
        appScene.getStylesheets().add("/css/startWindow.css");
        // appStage.setScene(appScene);

        closeBtn.setOnAction(e -> {

            appStage.close();
        });

        settingsBtn.setOnAction(e -> {
            networksBtn.setId("menuBtn");
            settingsBtn.setId("activeMenuBtn");
            showSettings(appStage, bodyVBox);
        });

        networksBtn.setOnAction(e -> {
            networksBtn.setId("activeMenuBtn");
            settingsBtn.setId("menuBtn");
            showNetworks(appScene, bodyVBox);
        });

        if (notSetup) {
            appStage.setScene(appScene);
        } else {
            setupOptions(appStage, appScene);
        }

        showNetworks(appScene, bodyVBox);

        appStage.show();
    }

    private void setupOptions(Stage appStage, Scene appScene) {

        String topTitle = "Setup: Options";

        Button closeBtn = new Button();
        closeBtn.setOnAction(e -> {
            appStage.setScene(appScene);
        });
        HBox titleBox = createTopBar(icon, topTitle, closeBtn, appStage);

        IconButton allNetworksBtn = new IconButton(new Image("/assets/layers.png"), "Full Install");

        allNetworksBtn.setOnAction(e -> {

            m_networksData.addNoteInterface(new ErgoWallet(m_networksData));
            m_networksData.addNoteInterface(new ErgoExplorer(m_networksData));
            m_networksData.addNoteInterface(new ErgoNetwork(m_networksData));
            m_networksData.addNoteInterface(new KucoinExchange(m_networksData));

            appStage.setScene(appScene);
        });
        Region seperatorRegion = new Region();
        seperatorRegion.setPrefHeight(20);

        IconButton cleanInstallBtn = new IconButton(new Image("/assets/snow.png"), "Clean Install");
        cleanInstallBtn.setPrefWidth(200);
        cleanInstallBtn.setPrefHeight(200);
        cleanInstallBtn.setOnAction(e -> {
            m_networksData.clear();
            appStage.setScene(appScene);
            m_networksData.showManageNetworkStage();
        });

        VBox listVBox = new VBox(allNetworksBtn, seperatorRegion, cleanInstallBtn);
        listVBox.setPadding(new Insets(35, 0, 0, 0));
        listVBox.setAlignment(Pos.CENTER);

        VBox layoutVBox = new VBox(titleBox, listVBox);

        Scene setupOptionsScene = new Scene(layoutVBox, 350, 450);
        setupOptionsScene.getStylesheets().add("/css/startWindow.css");
        appStage.setScene(setupOptionsScene);

    }

    private void showNetworks(Scene appScene, VBox bodyVBox) {

        bodyVBox.getChildren().clear();

        Tooltip addTip = new Tooltip("Networks");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        IconButton manageButton = new IconButton(new Image("/assets/filter.png"), "", IconStyle.ICON);
        manageButton.setImageWidth(15);
        manageButton.setPadding(new Insets(0, 6, 0, 6));
        manageButton.setTooltip(addTip);
        manageButton.setOnAction(e -> m_networksData.showManageNetworkStage());

        HBox menuBar = new HBox(manageButton);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 5, 1, 5));

        bodyVBox.setPadding(new Insets(0, 5, 0, 5));

        VBox gridBox = m_networksData.getNetworksBox(appScene.getWidth() - 100);
        VBox.setVgrow(gridBox, Priority.ALWAYS);
        HBox.setHgrow(gridBox, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(gridBox);
        scrollPane.setPadding(new Insets(5, 0, 5, 0));
        scrollPane.prefViewportWidthProperty().bind(appScene.widthProperty());
        scrollPane.prefViewportHeightProperty().bind(appScene.heightProperty().subtract(40).subtract(menuBar.heightProperty().get()));

        bodyVBox.getChildren().addAll(menuBar, scrollPane);

        /*
        addButton.setOnAction(clickEvent -> {
            Network newNetwork = showNetworkStage(null);

            refreshNetworksGrid(gridBox);
        });*/
    }

    private void showSettings(Stage appStage, VBox bodyVBox) {
        bodyVBox.getChildren().clear();

        boolean isUpdates = appData.getUpdates();

        Button settingsButton = createImageButton(logo, "Settings");

        HBox settingsBtnBox = new HBox(settingsButton);
        settingsBtnBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        Button passwordBtn = new Button("(click to update)");
        passwordBtn.setFont(txtFont);
        passwordBtn.setId("toolBtn");
        passwordBtn.setOnAction(e -> {
            Stage newPasswordStage = new Stage();
            final String newPassword = createPassword(newPasswordStage, "Net Notes - Security", logo, "Security");
            if (!newPassword.equals("")) {
                Stage statusStage = new Stage();
                setStatusStage(statusStage, "Net Notes - Saving...", "Saving...");
                statusStage.show();
                FxTimer.runLater(Duration.ofMillis(100), () -> {
                    String hash = Utils.getBcryptHashString(newPassword);

                    try {

                        appData.setAppKey(hash);
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
        });

        HBox passwordBox = new HBox(passwordTxt, passwordBtn);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(10, 0, 0, 20));

        Text updatesTxt = new Text("> Updates:");
        updatesTxt.setFill(txtColor);
        updatesTxt.setFont(txtFont);

        Button updatesBtn = new Button(isUpdates ? "Enabled" : "Disabled");
        updatesBtn.setFont(txtFont);
        updatesBtn.setId("toolBtn");
        updatesBtn.setOnAction(e -> {
            try {
                if (updatesBtn.getText().equals("Enabled")) {
                    updatesBtn.setText("Disabled");
                    appData.setUpdates(false);
                } else {
                    updatesBtn.setText("Enabled");
                    appData.setUpdates(true);
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

    public static String createPassword(Stage callingStage, String topTitle, Image windowLogo, String windowSubTitle) {

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

        Text passwordTxt = new Text("> Create password:");
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

        Scene passwordScene = new Scene(passwordVBox, 600, 425);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                passwordField.setText("");
                passwordStage.close();
            }
        });

        passwordField.setOnKeyPressed(e1 -> {

            KeyCode keyCode = e1.getCode();

            if ((keyCode == KeyCode.ENTER || keyCode == KeyCode.TAB)) {

                String passStr = passwordField.getText();
                // createPassField.setText("");
                bodyBox.getChildren().remove(clickRegion);

                passwordField.setVisible(false);

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
                            passwordStage.close();
                        } else {
                            bodyBox.getChildren().remove(secondPassBox);
                            createPassField2.setText("");
                            passwordField.setText("");
                            passwordField.setVisible(true);
                            secondPassBox.getChildren().clear();
                        }
                    }
                });

            }
        });
        passwordStage.showAndWait();

        return passwordField.getText();
    }

    private static void restoreWallet(Stage callingStage) {

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
        maximizeBtn.setOnAction(maxEvent -> {
            theStage.setMaximized(!theStage.isMaximized());
        });

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
        maximizeBtn.setOnAction(maxEvent -> {
            theStage.setMaximized(!theStage.isMaximized());
        });

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
        maximizeBtn.setOnAction(maxEvent -> {
            theStage.setMaximized(!theStage.isMaximized());
        });

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

    /*
    public static HBox createLabeledTopBar(Image iconImage, Label newTitleLbl, Button closeBtn, Stage theStage) {

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
    } */
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

}
