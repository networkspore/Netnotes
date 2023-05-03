package com.netnotes;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
/**
 * Netnotes
 *
 */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.scene.input.MouseEvent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser.ExtensionFilter;
import mslinks.ShellLinkException;
import javafx.scene.input.KeyCode;

import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.ergoplatform.ErgoAddress;
import org.ergoplatform.appkit.*;
import org.ergoplatform.restapi.client.WalletBox;
import org.reactfx.util.FxTimer;
import org.whispersystems.curve25519.java.open;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netnotes.Network.NetworkName;
import com.satergo.Wallet;
import com.satergo.WalletKey.Failure;
import com.satergo.ergo.ErgoInterface;
import com.satergo.ergo.ErgoNodeAccess;
import com.utils.Utils;
import javafx.embed.swing.SwingFXUtils;
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

    public static Image icon = new Image("/assets/icon20.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image ergoLogo = new Image("/assets/ergo-black-350.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");
    public static Image addImg = new Image("/assets/add-outline-white-40.png");
    public static Image closeImg = new Image("/assets/close-outline-white.png");
    public static Image minimizeImg = new Image("/assets/minimize-white-20.png");
    public static Image globeImg = new Image("/assets/globe-outline-white-120.png");
    public static Image settingsImg = new Image("/assets/settings-outline-white-120.png");
    public static Image lockDocumentImg = new Image("/assets/document-lock.png");
    public static Image ergoNetworkImg = new Image("/assets/globe-outline-ergo-150.png");
    public static Image arrowRightImg = new Image("/assets/arrow-forward-outline-white-20.png");
    public static Image walletImg20 = new Image("/assets/wallet-outline-white-20.png");
    public static Image walletImg240 = new Image("/assets/wallet-outline-white-240.png");
    public static Image atImage = new Image("/assets/at-white-240.png");
    public static Image branchImg = new Image("/assets/git-branch-outline-white-240.png");

    public static Image walletLockImg20 = new Image("/assets/wallet-locked-outline-white-20.png");

    public static Image openImg = new Image("/assets/open-outline-white-20.png");
    public static Image diskImg = new Image("/assets/save-outline-white-20.png");

    public static ExtensionFilter ergExt = new ExtensionFilter("Ergo wallet", "*.erg");

    public static final String settingsFileName = "settings.conf";
    public static final String networksFileName = "networks.dat";

    public static final String homeString = System.getProperty("user.home");

    private AppData appData;
    private NetworksData networksData;

    private File walletsDir;
    private File settingsFile = null;
    private File networksFile;
    private File currentDir = null;
    private File launcherFile = null;
    private File currentJar = null;

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

        walletsDir = new File(currentDir.getAbsolutePath() + "/wallets");
        if (!walletsDir.isDirectory()) {
            try {
                Files.createDirectories(walletsDir.toPath());
            } catch (IOException e) {
                walletsDir = null;
            }
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

            networksFile = new File(currentDirString + "/" + networksFileName);
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

        Scene passwordScene = new Scene(layoutVBox, 600, 425);

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
                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(passwordField.getText().toCharArray(), hashBytes);
                        statusStage.close();
                        if (result.verified) {
                            openNetnotes(appStage);

                        } else {
                            passwordField.setText("");
                        }
                    });
                }
            }
        });
        appStage.show();
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
    private void openNetnotes(Stage appStage) {

        Button closeBtn = new Button();
        Button settingsBtn = new Button();
        Button networksBtn = new Button();

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
        appStage.setScene(appScene);

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
            showNetworks(appStage, bodyVBox);
        });

        appStage.show();

        try {
            networksData = new NetworksData(networksFile);
        } catch (Exception e1) {

            Alert a = new Alert(AlertType.NONE, "Fatal error: Error unable to load networks.\n\n" + e1.toString(), ButtonType.CLOSE);
            a.setTitle("Fatal error: Unable to load networks.");
            a.initOwner(appStage);
            a.showAndWait();
            shutdownNow();
        }

        showNetworks(appStage, bodyVBox);
    }

    private void showNetworks(Stage appStage, VBox bodyVBox) {
        bodyVBox.getChildren().clear();

        ImageView addImage = highlightedImageView(addImg);
        addImage.setFitHeight(10);
        addImage.setPreserveRatio(true);

        Tooltip addTip = new Tooltip("Add network");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(txtFont);

        Button addButton = new Button();
        addButton.setGraphic(addImage);
        addButton.setId("menuBarBtn");
        addButton.setPadding(new Insets(2, 6, 2, 6));
        addButton.setTooltip(addTip);

        /*  Region lRegion = new Region();
        Region rRegion = new Region();

        HBox.setHgrow(lRegion, Priority.ALWAYS);
        HBox.setHgrow(rRegion, Priority.ALWAYS); */
        HBox menuBar = new HBox(addButton);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(5, 5, 5, 5));
        bodyVBox.getChildren().add(menuBar);
        bodyVBox.setPadding(new Insets(0, 5, 0, 5));
        /*Alert a = new Alert(AlertType.NONE, layoutBounds.getWidth() + " " + layoutBounds.getHeight(), ButtonType.CLOSE);
        a.initOwner(appStage);
        a.show();*/

        VBox gridBox = new VBox();
        VBox.setVgrow(gridBox, Priority.ALWAYS);
        HBox.setHgrow(gridBox, Priority.ALWAYS);

        bodyVBox.getChildren().add(gridBox);

        refreshNetworksGrid(gridBox);

        addButton.setOnAction(clickEvent -> {
            Network newNetwork = showNetworkStage(null);

            refreshNetworksGrid(gridBox);
        });

    }

    public void refreshNetworksGrid(VBox gridBox) {
        gridBox.getChildren().clear();
        int numCells = networksData.getNetworks().size();

        if (numCells == 0) {
            IconButton addNetworkBtn = new IconButton(globeImg, "Add Network", "empty");

            gridBox.setAlignment(Pos.CENTER);
            gridBox.getChildren().add(addNetworkBtn);

            addNetworkBtn.setOnAction(addEvent -> {
                Network newNetwork = showNetworkStage(null);
            });
        } else {

            Bounds layoutBounds = gridBox.getLayoutBounds();
            double imageWidth = 100;
            double cellPadding = 15;
            double cellWidth = imageWidth + (cellPadding * 2);

            int numCol = (int) Math.floor(cellWidth / layoutBounds.getWidth());
            int numRows = (int) ((numCells > 0) && (numCol != 0) ? Math.ceil(numCells / numCol) : 1);

            HBox[] rowsBoxes = new HBox[numRows];

            Image iconImage = ergoNetworkImg;

            ItemIterator grid = new ItemIterator();

            networksData.getNetworks().forEach(network -> {

                IconButton iconButton = new IconButton(iconImage, network.getId(), network.getUUID()); //createIconButton(imageWidth, cellPadding, iconImage, network.getName());

                rowsBoxes[grid.getJ()].getChildren().add(iconButton);

                if (grid.getI() < numCol) {
                    grid.setI(grid.getI() + 1);
                } else {
                    grid.setI(0);
                    grid.setJ(0);
                }
            });
        }
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

    public Button createIconButton(double imageWidth, double padding, Image image, String title) {

        ImageView btnImageView = highlightedImageView(image);
        btnImageView.setFitHeight(imageWidth);
        btnImageView.setPreserveRatio(true);

        Button imageBtn = new Button(title);
        imageBtn.setGraphic(btnImageView);
        imageBtn.setId("iconBtn");
        imageBtn.setFont(txtFont);
        imageBtn.setContentDisplay(ContentDisplay.TOP);
        imageBtn.setPrefWidth(imageWidth);
        imageBtn.setPadding(new Insets(padding, padding, padding, padding));

        return imageBtn;

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

    public ArrayList<AddressData> getWalletAddressDataList(Wallet wallet, PriceChart priceChart, NetworkType networkType) {

        // ErgoClient ergoClient = RestApiErgoClient.create(nodeApiAddress, networkType, "", networkType == NetworkType.MAINNET ? defaultMainnetExplorerUrl : defaultTestnetExplorerUrl);
        ArrayList<AddressData> addressList = new ArrayList<>();
        wallet.myAddresses.forEach((index, name) -> {

            Address address = null;
            try {
                address = wallet.publicAddress(networkType, index);
            } catch (Failure e) {

            }
            addressList.add(new AddressData(name, index, address, priceChart, networkType));

        });

        return addressList;
    }

    public void showOpenWalletStage(Wallet wallet, Network network) {

        NetworkType networkType = network.getType() == Network.NetworkType.MAINNET ? NetworkType.MAINNET : NetworkType.TESTNET;

        String name = network.getName() == Network.NetworkName.ERGO ? "Ergo" : "unknown";

        String title = name + " wallet - (" + (network.getType() == Network.NetworkType.MAINNET ? "MAINNET" : "TESTNET") + ")";

        PriceChart priceChart = new PriceChart();

        double width = 450;

        Stage openWalletStage = new Stage();
        openWalletStage.setTitle(title);
        openWalletStage.getIcons().add(walletImg240);
        openWalletStage.setResizable(false);
        openWalletStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {

            openWalletStage.close();
        });

        HBox titleBox = createTopBar(icon, title, closeBtn, openWalletStage);

        /*Button imageButton = createImageButton(walletImg240, title + "\n" + wallet.name.get());
        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);*/
        ImageView addImage = highlightedImageView(addImg);
        addImage.setFitHeight(10);
        addImage.setPreserveRatio(true);

        Tooltip addTip = new Tooltip("Add address");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(txtFont);

        Button addButton = new Button();
        addButton.setGraphic(highlightedImageView(new Image("/assets/git-branch-outline-white-30.png")));
        addButton.setId("menuBarBtn");
        addButton.setPadding(new Insets(2, 6, 2, 6));
        addButton.setTooltip(addTip);

        Tooltip explorerUrlTip = new Tooltip("Explorer url");
        explorerUrlTip.setShowDelay(new javafx.util.Duration(100));
        explorerUrlTip.setFont(txtFont);

        Button explorerBtn = new Button();
        explorerBtn.setGraphic(highlightedImageView(new Image("/assets/search-outline-white-30.png")));
        explorerBtn.setId("menuBarBtn");
        explorerBtn.setPadding(new Insets(2, 6, 2, 6));
        explorerBtn.setTooltip(explorerUrlTip);
        explorerBtn.setDisable(true);

        TextField explorerURLField = new TextField();
        explorerURLField.setId("urlField");
        explorerURLField.setText(network.getCurrentExplorerURL());
        explorerURLField.setEditable(false);
        explorerURLField.setPrefWidth(350);
        explorerURLField.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox menuBar = new HBox(addButton, spacer, explorerBtn, explorerURLField);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(5, 5, 5, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        VBox layoutBox = new VBox();
        Font smallerFont = Font.font("OCR A Extended", 10);

        Text updatedTxt = new Text("Updated:");
        updatedTxt.setFill(altColor);
        updatedTxt.setFont(smallerFont);

        TextField lastUpdatedField = new TextField();
        lastUpdatedField.setFont(smallerFont);
        lastUpdatedField.setId("formField");

        HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
        updateBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacerRegion = new Region();
        VBox.setVgrow(spacerRegion, Priority.ALWAYS);

        VBox bodyVBox = new VBox(titleBox, paddingBox, layoutBox, spacerRegion, updateBox);

        ArrayList<AddressData> addressDataList = getWalletAddressDataList(wallet, priceChart, networkType);

        Scene openWalletScene = new Scene(bodyVBox, width, 525);
        openWalletScene.getStylesheets().add("/css/startWindow.css");
        openWalletStage.setScene(openWalletScene);
        openWalletStage.show();

        addButton.setOnAction(e -> {
            String addressName = showGetTextInput("Address name", "Address name", branchImg);
            if (addressName != null) {
                int nextAddressIndex = wallet.nextAddressIndex();
                wallet.myAddresses.put(nextAddressIndex, name);
                try {
                    Address address = wallet.publicAddress(networkType, nextAddressIndex);
                    AddressData addressData = new AddressData(addressName, nextAddressIndex, address, priceChart, networkType);
                    Button newButton = getAddressDataButton(openWalletScene.getWidth(), addressData, wallet, networkType);

                    addressDataList.add(addressData);
                    layoutBox.getChildren().add(newButton);
                } catch (Failure e1) {
                    Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                    a.show();
                }

            }
        });

        addressDataList.forEach(addressData -> {

            Button rowBtn = getAddressDataButton(width, addressData, wallet, networkType);

            HBox rowBox = new HBox(rowBtn);
            HBox.setHgrow(rowBox, Priority.ALWAYS);
            layoutBox.getChildren().add(rowBox);
            HBox.setHgrow(layoutBox, Priority.ALWAYS);
        });

    }

    public String getNowTimeString() {
        LocalTime time = LocalTime.now();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(time);
    }

    private Button getAddressDataButton(double width, AddressData addressData, Wallet wallet, NetworkType networkType) {

        Tooltip addressTip = new Tooltip(addressData.getName());

        Button rowBtn = new Button();
        //  HBox.setHgrow(rowBtn, Priority.ALWAYS);
        rowBtn.setPrefHeight(40);
        rowBtn.setPrefWidth(width);
        rowBtn.setAlignment(Pos.CENTER_LEFT);
        rowBtn.setContentDisplay(ContentDisplay.LEFT);
        rowBtn.setTooltip(addressTip);
        rowBtn.setPadding(new Insets(0, 20, 0, 20));
        rowBtn.setId("rowBtn");

        updateAddressBtn(width, rowBtn, addressData);

        rowBtn.setOnAction(e -> {
            showAddressStage(wallet, networkType, addressData);
        });

        addressData.lastUpdated.addListener(e -> {
            updateAddressBtn(width, rowBtn, addressData);
        });

        return rowBtn;
    }

    private void updateAddressBtn(double width, Button rowBtn, AddressData addressData) {

        BufferedImage imageBuffer = addressData.getBufferedImage();

        int remainingSpace = imageBuffer.getWidth();

        String addressMinimal = addressData.getAddressMinimal((int) (remainingSpace / 24));

        ImageView btnImageView = new ImageView();
        if (imageBuffer != null) {
            btnImageView.setImage(SwingFXUtils.toFXImage(imageBuffer, null));
        }
        String text = "> " + addressData.getName() + ": \n  " + addressMinimal;
        Tooltip addressTip = new Tooltip(addressData.getName());

        rowBtn.setGraphic(btnImageView);
        rowBtn.setText(text);
        rowBtn.setTooltip(addressTip);

    }

    private void showAddressStage(Wallet wallet, NetworkType networkType, AddressData addressData) {

        String title = "Ergo Wallet - " + addressData.getName() + " (" + (networkType.toString()) + "): " + addressData.getAddressMinimal(16);

        Stage addressStage = new Stage();
        addressStage.setTitle(title);
        addressStage.getIcons().add(walletImg240);
        addressStage.setResizable(false);
        addressStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {
            addressStage.close();
        });

        HBox titleBox = createTopBar(icon, title, closeBtn, addressStage);

        ImageView addImage = highlightedImageView(addImg);
        addImage.setFitHeight(10);
        addImage.setPreserveRatio(true);

        Tooltip selectMarketTip = new Tooltip("Select Market");
        selectMarketTip.setShowDelay(new javafx.util.Duration(100));
        selectMarketTip.setFont(txtFont);
        ImageView arrow = highlightedImageView(new Image("/assets/navigate-outline-white-30.png"));
        arrow.setFitWidth(25);
        arrow.setPreserveRatio(true);

        MenuButton changeMarketButton = new MenuButton();
        changeMarketButton.setGraphic(arrow);
        changeMarketButton.setId("menuBarBtn");
        // changeMarketButton.setMaxWidth(30);
        //  changeMarketButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        changeMarketButton.setTooltip(selectMarketTip);

        Tooltip locationUrlTip = new Tooltip("Market url");
        locationUrlTip.setShowDelay(new javafx.util.Duration(100));
        locationUrlTip.setFont(txtFont);

        TextField locationUrlField = new TextField();
        locationUrlField.setId("urlField");
        locationUrlField.setText(addressData.getCurrentPriceApiUrl());
        locationUrlField.setEditable(false);
        locationUrlField.setTooltip(locationUrlTip);
        locationUrlField.setAlignment(Pos.CENTER_LEFT);
        locationUrlField.setFont(txtFont);

        addressData.getUrlMenuItems().forEach(item -> {
            item.setId("urlMenuItem");
            item.setOnAction(e -> {
                addressData.setApiIndex(item.getIndex());
                locationUrlField.setText(addressData.getCurrentPriceApiUrl());
            });

            changeMarketButton.getItems().add(item);

        });

        HBox.setHgrow(locationUrlField, Priority.ALWAYS);

        HBox menuBar = new HBox(changeMarketButton, locationUrlField);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(5, 5, 5, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        Text addressNameTxt = new Text("> Ergo wallet - " + addressData.getName() + " (" + networkType.toString() + "):");
        addressNameTxt.setFill(Color.WHITE);
        addressNameTxt.setFont(txtFont);

        HBox addressNameBox = new HBox(addressNameTxt);
        addressNameBox.setPadding(new Insets(3, 0, 5, 0));

        Color formFieldColor = new Color(.8, .8, .8, .9);

        Text addressTxt = new Text("  Address:");
        addressTxt.setFont(txtFont);
        addressTxt.setFill(formFieldColor);

        TextField addressField = new TextField(addressData.getAddressString());
        addressField.setEditable(false);

        addressField.setFont(txtFont);
        addressField.setId("formField");
        HBox.setHgrow(addressField, Priority.ALWAYS);

        HBox addressBox = new HBox(addressTxt, addressField);
        addressBox.setAlignment(Pos.CENTER_LEFT);

        Text ergQuantityTxt = new Text("  Balance:");
        ergQuantityTxt.setFont(txtFont);
        ergQuantityTxt.setFill(formFieldColor);
        double unconfirmed = addressData.getFullAmountUnconfirmed();
        TextField ergQuantityField = new TextField(addressData.getFullAmountDouble() + " ERG" + (unconfirmed != 0 ? (" (" + unconfirmed + " unconfirmed)") : ""));
        ergQuantityField.setEditable(false);
        ergQuantityField.setFont(txtFont);
        ergQuantityField.setId("formField");
        HBox.setHgrow(ergQuantityField, Priority.ALWAYS);

        HBox ergQuantityBox = new HBox(ergQuantityTxt, ergQuantityField);
        ergQuantityBox.setAlignment(Pos.CENTER_LEFT);

        Text priceTxt = new Text("  Price: ");
        priceTxt.setFont(txtFont);
        priceTxt.setFill(formFieldColor);

        TextField priceField = new TextField(addressData.getPriceString());
        priceField.setEditable(false);

        priceField.setFont(txtFont);
        priceField.setId("formField");
        HBox.setHgrow(priceField, Priority.ALWAYS);

        HBox priceBox = new HBox(priceTxt, priceField);
        priceBox.setAlignment(Pos.CENTER_LEFT);

        Text balanceTxt = new Text("  Total:");
        balanceTxt.setFont(txtFont);
        balanceTxt.setFill(formFieldColor);

        TextField balanceField = new TextField(addressData.getTotalAmountPriceString());
        balanceField.setEditable(false);

        balanceField.setFont(txtFont);
        balanceField.setId("formField");
        HBox.setHgrow(balanceField, Priority.ALWAYS);

        HBox balanceBox = new HBox(balanceTxt, balanceField);
        balanceBox.setAlignment(Pos.CENTER_LEFT);

        Text lastUpdatedTxt = new Text("  Updated:");
        lastUpdatedTxt.setFill(formFieldColor);
        lastUpdatedTxt.setFont(txtFont);

        TextField lastUpdatedField = new TextField(addressData.getLastUpdatedString());
        lastUpdatedField.setEditable(false);
        lastUpdatedField.setId("formField");
        lastUpdatedField.setFont(txtFont);
        HBox.setHgrow(lastUpdatedField, Priority.ALWAYS);

        HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
        lastUpdatedBox.setAlignment(Pos.CENTER_LEFT);

        ImageView chartView = new ImageView();
        chartView.setUserData(null);
        // chartView.setPreserveRatio(true);
        chartView.setFitWidth(400);
        chartView.setFitHeight(150);
        // chartView.setPreserveRatio(true);
        Button chartButton = new Button("Getting price information");
        chartButton.setGraphic(chartView);
        chartButton.setContentDisplay(ContentDisplay.BOTTOM);
        chartButton.setId("iconBtn");
        chartButton.setFont(txtFont);

        chartButton.setOnMouseEntered(e -> {
            chartView.setUserData("mouseOver");
            PriceChart pc = addressData.getPriceChart();
            BufferedImage imgBuf = pc.getChartBufferedImage();
            if (imgBuf != null) {
                chartView.setImage(SwingFXUtils.toFXImage(pc.zoomLatest(48), null));
            } else {
                chartView.setImage(null);
                chartButton.setText("Price unavailable");
            }
        });

        chartButton.setOnMouseExited(e -> {
            chartView.setUserData(null);

            if (addressData.getPriceChart().getValid()) {
                chartView.setImage(SwingFXUtils.toFXImage(Utils.greyScaleImage(addressData.getPriceChart().zoomLatest(48)), null));
            } else {
                chartView.setImage(null);
                chartButton.setText("Price unavailable");
            }
        });

        HBox chartBox = new HBox(chartButton);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPadding(new Insets(5, 0, 20, 0));

        VBox bodyVBox = new VBox(chartBox, addressNameBox, addressBox, ergQuantityBox, priceBox, balanceBox, lastUpdatedBox);
        bodyVBox.setPadding(new Insets(0, 20, 0, 20));
        VBox layoutVBox = new VBox(titleBox, paddingBox, bodyVBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene addressScene = new Scene(layoutVBox, 650, 500);

        addressScene.getStylesheets().add("/css/startWindow.css");

        addressStage.setScene(addressScene);
        addressStage.show();

        addressData.lastUpdated.addListener(changed -> {

            double unconfirmedUpdate = addressData.getFullAmountUnconfirmed();
            ergQuantityField.setText(addressData.getFullAmountDouble() + " ERG" + (unconfirmedUpdate != 0 ? (" (" + unconfirmedUpdate + " unconfirmed)") : ""));
            double priceUpdate = addressData.getPrice();
            priceField.setText(addressData.getPriceString());
            balanceField.setText(addressData.getTotalAmountPriceString() + (unconfirmedUpdate != 0 ? (" (" + (unconfirmedUpdate * priceUpdate) + " unconfirmed)") : ""));
            lastUpdatedField.setText(addressData.getLastUpdatedString());

        });

        addressData.getPriceChart().lastUpdated.addListener(updated -> {
            PriceChart priceChart = addressData.getPriceChart();
            if (priceChart.getValid()) {

                chartButton.setText(priceChart.getSymbol() + " - " + priceChart.getTimespan() + " (" + priceChart.getTimeStampString() + ")");
                if (chartView.getUserData() == null) {
                    chartView.setImage(SwingFXUtils.toFXImage(Utils.greyScaleImage(addressData.getPriceChart().zoomLatest(48)), null));
                } else {
                    chartView.setImage(SwingFXUtils.toFXImage(addressData.getPriceChart().zoomLatest(48), null));
                }
            } else {
                chartButton.setText("Price unavailable");
                chartView.setImage(null);
            }
        });

    }

    public String showGetTextInput(String prompt, String title, Image img) {

        Stage textInputStage = new Stage();
        textInputStage.setTitle(title);
        textInputStage.getIcons().add(walletImg240);
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

    public Network showNetworkStage(Network network) {

        Network currentNetwork = network == null ? new Network(Network.NetworkName.ERGO, Network.NetworkType.MAINNET) : network;

        Stage networkStage = new Stage();
        networkStage.setTitle("Net Notes - Network");
        networkStage.getIcons().add(ergoLogo);
        networkStage.setResizable(false);
        networkStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {
            if (network == null) {

                currentNetwork.setId(null);

            }
            networkStage.close();
        });

        HBox titleBox = createTopBar(icon, "Network", closeBtn, networkStage);

        Button imageButton = createImageButton(globeImg, "Network");
        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);

        Text networkTypeTxt = new Text("> Name:  Ergo");
        networkTypeTxt.setFill(txtColor);
        networkTypeTxt.setFont(txtFont);

        HBox networkTypeBox = new HBox(networkTypeTxt);
        networkTypeBox.setPadding(new Insets(3, 0, 5, 0));
        HBox.setHgrow(networkTypeBox, Priority.ALWAYS);

        Text applicationTxt = new Text("> Application:");
        applicationTxt.setFill(txtColor);
        applicationTxt.setFont(txtFont);

        Text addressTxt = new Text("URL:");
        addressTxt.setFont(txtFont);
        addressTxt.setFill(altColor);
        addressTxt.setId("textField");

        ImageView arrowRightImage = highlightedImageView(arrowRightImg);
        arrowRightImage.setFitHeight(15);
        arrowRightImage.setPreserveRatio(true);

        Button addressBtn = new Button();
        addressBtn.setGraphic(arrowRightImage);
        addressBtn.setPadding(new Insets(2, 15, 2, 15));
        addressBtn.setFont(txtFont);
        addressBtn.setVisible(false);

        TextField addressField = new TextField("Enter address or click manage...");
        addressField.setId("formField");
        HBox.setHgrow(addressField, Priority.ALWAYS);
        addressField.setOnKeyPressed(key -> {
            KeyCode keyCode = key.getCode();

            if (keyCode == KeyCode.ENTER) {
                String addressFieldText = addressField.getText();

                try {
                    currentNetwork.setUrl(addressFieldText);
                    String currentHost = currentNetwork.getHost();
                    if (currentHost == null) {

                        addressField.setText("Enter address or click manage...");
                    } else {
                        int currentPort = currentNetwork.getPort();

                        addressField.setText(currentHost + ":" + currentPort);

                    }
                    addressBtn.setVisible(false);

                } catch (MalformedURLException e) {

                    currentNetwork.setHost(null);

                    addressBtn.setVisible(false);
                    addressField.setText("Enter address or click manage...");

                }
            }
        });

        addressField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                String addressFieldText = addressField.getText();

                if (newPropertyValue) {
                    if (addressFieldText.equals("Enter address or click manage...")) {
                        addressField.setText("");
                    }
                    addressBtn.setVisible(true);
                } else {

                    try {
                        currentNetwork.setUrl(addressFieldText);
                        String currentHost = currentNetwork.getHost();
                        if (currentHost == null) {

                            addressField.setText("Enter address or click manage...");
                        } else {
                            int currentPort = currentNetwork.getPort();

                            addressField.setText(currentHost + ":" + currentPort);

                        }
                        addressBtn.setVisible(false);

                    } catch (MalformedURLException e) {

                        currentNetwork.setHost(null);

                        addressBtn.setVisible(false);
                        addressField.setText("Enter address or click manage...");

                    }

                }
            }
        });

        HBox appLocationBox = new HBox(addressTxt, addressField, addressBtn);
        appLocationBox.setPadding(new Insets(5, 0, 5, 20));
        appLocationBox.setAlignment(Pos.CENTER_LEFT);

        Button applicationBtn = new Button("Manage"); //127.0.0.1:9503
        applicationBtn.setGraphic(highlightedImageView(new Image("/assets/server-outline-white-20.png")));
        applicationBtn.setFont(txtFont);
        applicationBtn.setPadding(new Insets(2, 10, 2, 10));

        HBox manageBox = new HBox(applicationBtn);
        manageBox.setAlignment(Pos.CENTER_LEFT);
        manageBox.setPadding(new Insets(10, 0, 10, 20));

        HBox applicationBox = new HBox(applicationTxt);
        applicationBox.setAlignment(Pos.CENTER_LEFT);
        applicationBox.setPadding(new Insets(3, 0, 0, 0));

        Text walletTxt = new Text("> Wallet:");
        walletTxt.setFill(txtColor);
        walletTxt.setFont(txtFont);

        Button openWalletBtn = new Button("Open");
        openWalletBtn.setGraphic(highlightedImageView(walletLockImg20));
        openWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        openWalletBtn.setFont(txtFont);
        openWalletBtn.setDisable(true);
        openWalletBtn.setOnAction(openEvent -> {

            try {
                WalletContainer walletContainer = confirmWalletPassword(currentNetwork.getWalletFile());
                Wallet wallet = walletContainer.getWallet();

                if (wallet != null) {
                    showOpenWalletStage(wallet, currentNetwork);
                }
            } catch (Exception e) {
                Alert noWallet = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                noWallet.showAndWait();
            }

        });

        TextField fileTxtField = new TextField("Select file...");
        fileTxtField.setId("formField");
        fileTxtField.setEditable(false);
        HBox.setHgrow(fileTxtField, Priority.ALWAYS);

        Tooltip selectTooltip = new Tooltip("Select File");
        selectTooltip.setShowDelay(new javafx.util.Duration(100));

        Button selectWalletBtn = new Button();
        selectWalletBtn.setGraphic(highlightedImageView(diskImg));
        selectWalletBtn.setAlignment(Pos.CENTER);
        selectWalletBtn.setTooltip(selectTooltip);

        File defaultFile = null;
        boolean isWallet = currentNetwork.isWallet();

        if (isWallet) {
            try {
                defaultFile = currentNetwork.getWalletFile();
            } catch (Exception e) {

            }
        }

        if (defaultFile != null) {
            fileTxtField.setText(defaultFile.getName());
            openWalletBtn.setDisable(false);
        }

        TextField walletLocationField = new TextField("");
        walletLocationField.setEditable(false);
        walletLocationField.setId("formField");
        HBox.setHgrow(walletLocationField, Priority.ALWAYS);

        selectWalletBtn.setOnAction(e -> {

            SelectedFile selectedWalletFile = showSelectWalletStage(currentNetwork);
            File walletFile = selectedWalletFile.getFile();

            if (walletFile != null) {
                currentNetwork.setWalletFile(walletFile);
                fileTxtField.setText(walletFile.getName());
                openWalletBtn.setDisable(false);

            } else {
                if (currentNetwork.isWallet()) {

                    try {
                        fileTxtField.setText(currentNetwork.getWalletFile().getName());
                        openWalletBtn.setDisable(false);
                    } catch (Exception e1) {

                        fileTxtField.setText("Select file...");
                        openWalletBtn.setDisable(true);
                    }
                } else {

                    fileTxtField.setText("Select file...");
                    openWalletBtn.setDisable(true);
                }
            }
        });

        HBox walletBox = new HBox(walletTxt);
        walletBox.setAlignment(Pos.CENTER_LEFT);
        walletBox.setPadding(new Insets(5, 0, 3, 0));

        Text fileTxt = new Text("File (*.erg):");
        fileTxt.setFont(txtFont);
        fileTxt.setFill(altColor);
        fileTxt.setId("textField");

        HBox fileBox = new HBox(fileTxt, fileTxtField, selectWalletBtn);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setPadding(new Insets(0, 10, 0, 20));
        fileBox.setPrefWidth(Double.MAX_VALUE);

        Region hBar = null;
        HBox gBox = null;
        HBox addBox = null;

        if (network == null) {

            hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setId("hGradient");

            gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(15, 0, 0, 0));
            HBox.setHgrow(gBox, Priority.ALWAYS);

            Button addBtn = new Button("Add");
            addBtn.setPadding(new Insets(2, 10, 2, 10));
            addBtn.setFont(txtFont);
            addBtn.setOnAction(openEvent -> {

            });

            addBox = new HBox(addBtn);
            addBox.setAlignment(Pos.CENTER);
            addBox.setPadding(new Insets(25, 0, 0, 0));
        }

        HBox walletManageBox = new HBox(openWalletBtn);
        walletManageBox.setPadding(new Insets(15, 0, 0, 20));

        VBox bodyBox = null;
        if (network == null) {
            bodyBox = new VBox(imageBox, networkTypeBox, applicationBox, appLocationBox, manageBox, walletBox, fileBox, walletManageBox, gBox, addBox);
        } else {
            bodyBox = new VBox(imageBox, networkTypeBox, applicationBox, appLocationBox, manageBox, walletBox, fileBox, walletManageBox);
        }
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        VBox networkVBox = new VBox(titleBox, bodyBox);
        HBox.setHgrow(networkVBox, Priority.ALWAYS);

        Scene networkScene = new Scene(networkVBox, 400, 525);
        networkScene.getStylesheets().add("/css/startWindow.css");
        networkStage.setScene(networkScene);

        if (!isWallet) {
            Platform.runLater(() -> selectWalletBtn.fire());
        }
        networkStage.showAndWait();
        if (currentNetwork.getId() == null) {
            return null;
        } else {
            return currentNetwork;
        }

    }

    class SelectedFile {

        private File m_file;

        public SelectedFile(File file) {
        }

        public File getFile() {
            return m_file;
        }

        public void setFile(File file) {
            m_file = file;
        }
    }

    public SelectedFile showSelectWalletStage(Network network) {

        NetworkType networkType = network.getType() == Network.NetworkType.MAINNET ? NetworkType.MAINNET : NetworkType.TESTNET;

        SelectedFile selectedFile = new SelectedFile(null);

        Stage walletStage = new Stage();
        walletStage.getIcons().add(logo);
        walletStage.setResizable(false);
        walletStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {
            walletStage.close();
        });

        HBox titleBox = createTopBar(icon, "Network: Wallet file - " + (networkType == NetworkType.MAINNET ? "Mainnet" : "Testnet"), closeBtn, walletStage);

        Button lockDocBtn = createImageButton(lockDocumentImg, "Wallet File");
        HBox imageBox = new HBox(lockDocBtn);
        imageBox.setAlignment(Pos.CENTER);

        Text walletTxt = new Text("> Select wallet file:");
        walletTxt.setFont(txtFont);
        walletTxt.setFill(txtColor);

        HBox textWalletBox = new HBox(walletTxt);
        textWalletBox.setAlignment(Pos.CENTER_LEFT);
        textWalletBox.setPadding(new Insets(10, 0, 0, 0));

        Button newWalletBtn = new Button("Create");

        newWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        newWalletBtn.setFont(txtFont);
        newWalletBtn.setPrefWidth(120);

        newWalletBtn.setOnAction(newWalletEvent -> {

            String seedPhrase = createMnemonicStage();
            if (!seedPhrase.equals("")) {
                String password = createPassword(walletStage, "Ergo - New wallet: Password", ergoLogo, "New Wallet");

                if (!password.equals("")) {
                    Alert nextAlert = new Alert(AlertType.NONE, "Notice:\n\nThis password is required along with the mnemonic phrase in order to restore this wallet.\n\nPlease be aware that you may change the password to access your wallet, but you will always need this password in order to restore this wallet.\n\nIf it is possible for you to forget this password write it down and keep it in a secure location.\n\n", ButtonType.OK);
                    nextAlert.initOwner(walletStage);
                    nextAlert.setTitle("Password: Notice");
                    nextAlert.showAndWait();

                    Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(password));

                    FileChooser saveFileChooser = new FileChooser();
                    saveFileChooser.setInitialDirectory(walletsDir);
                    saveFileChooser.setTitle("Save: Wallet file");
                    saveFileChooser.getExtensionFilters().add(ergExt);
                    saveFileChooser.setSelectedExtensionFilter(ergExt);

                    File walletFile = saveFileChooser.showSaveDialog(walletStage);

                    if (walletFile == null) {
                        Alert a = new Alert(AlertType.NONE, "Wallet creation:\n\nCanceled by user.\n\n", ButtonType.CLOSE);
                        a.initOwner(walletStage);
                        a.setTitle("Wallet creation: Canceled");
                        a.showAndWait();

                    } else {
                        try {
                            Wallet wallet = Wallet.create(walletFile.toPath(), mnemonic, walletFile.getName(), password.toCharArray());
                            selectedFile.setFile(walletFile);

                            showOpenWalletStage(wallet, network);
                            walletStage.close();
                        } catch (Exception e1) {
                            Alert a = new Alert(AlertType.NONE, "Wallet creation:\n\n" + e1.toString() + ". Creation process terminated.\n\n" + e1.toString(), ButtonType.OK);
                            a.initOwner(walletStage);
                            a.show();
                        }
                    }

                }
            }

        });

        Button existingWalletBtn = new Button("Open");
        existingWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        existingWalletBtn.setPrefWidth(120);
        existingWalletBtn.setFont(txtFont);
        existingWalletBtn.setOnAction(clickEvent -> {
            FileChooser openFileChooser = new FileChooser();
            openFileChooser.setInitialDirectory(walletsDir);
            openFileChooser.setTitle("Open: Wallet file");
            openFileChooser.getExtensionFilters().add(ergExt);
            openFileChooser.setSelectedExtensionFilter(ergExt);

            File walletFile = openFileChooser.showOpenDialog(walletStage);

            if (walletFile != null) {

                WalletContainer walletContainer = confirmWalletPassword(walletFile);
                Wallet wallet = walletContainer.getWallet();
                if (wallet != null) {
                    selectedFile.setFile(walletFile);
                    showOpenWalletStage(wallet, network);
                    walletStage.close();

                }

            }
        });

        Button restoreWalletBtn = new Button("Restore");
        restoreWalletBtn.setPadding(new Insets(2, 5, 2, 5));
        restoreWalletBtn.setFont(txtFont);
        restoreWalletBtn.setPrefWidth(120);
        restoreWalletBtn.setOnAction(clickEvent -> {
            String seedPhrase = restoreMnemonicStage();
            if (!seedPhrase.equals("")) {
                String password = createPassword(walletStage, "Ergo - Restore wallet: Password", ergoLogo, "Restore Wallet");

                if (!password.equals("")) {
                    Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(password));

                    FileChooser saveFileChooser = new FileChooser();
                    saveFileChooser.setInitialDirectory(walletsDir);
                    saveFileChooser.setTitle("Save: Wallet file");
                    saveFileChooser.getExtensionFilters().add(ergExt);
                    saveFileChooser.setSelectedExtensionFilter(ergExt);

                    File walletFile = saveFileChooser.showSaveDialog(walletStage);

                    if (walletFile == null) {
                        Alert a = new Alert(AlertType.NONE, "Wallet restoration: Canceled", ButtonType.CLOSE);
                        a.initOwner(walletStage);
                        a.setTitle("Wallet restoration: Canceled");
                        a.showAndWait();

                    } else {
                        try {
                            Wallet newWallet = Wallet.create(walletFile.toPath(), mnemonic, seedPhrase, password.toCharArray());

                            //  Files.write(walletFile.toPath(), newWallet.serializeEncrypted());
                            selectedFile.setFile(walletFile);
                            showOpenWalletStage(newWallet, network);
                            walletStage.close();

                        } catch (Exception e1) {
                            Alert a = new Alert(AlertType.NONE, "Wallet creation: Cannot be saved.\n\n" + e1.toString(), ButtonType.OK);
                            a.initOwner(walletStage);
                            a.show();
                        }
                    }

                }
            }

        });

        Region lRegion = new Region();
        lRegion.setPrefWidth(20);

        Region rRegion = new Region();
        rRegion.setPrefWidth(20);

        HBox newWalletBox = new HBox(newWalletBtn, lRegion, existingWalletBtn, rRegion, restoreWalletBtn);
        newWalletBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(newWalletBox, Priority.ALWAYS);
        newWalletBox.setPadding(new Insets(45, 0, 0, 0));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(45, 0, 0, 0));
        HBox.setHgrow(gBox, Priority.ALWAYS);

        Button okBtn = new Button("Close");
        okBtn.setPadding(new Insets(2, 5, 2, 5));
        okBtn.setFont(txtFont);
        okBtn.setPrefWidth(120);
        okBtn.setOnAction(clickEvent -> {
            walletStage.close();
        });

        HBox okBox = new HBox(okBtn);
        okBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(okBox, Priority.ALWAYS);
        okBox.setPadding(new Insets(20, 0, 0, 0));

        VBox bodyBox = new VBox(textWalletBox, newWalletBox, gBox, okBox);
        bodyBox.setPadding(new Insets(0, 20, 0, 20));

        VBox layoutBox = new VBox(titleBox, imageBox, bodyBox);

        Scene walletScene = new Scene(layoutBox, 600, 425);
        walletScene.getStylesheets().add("/css/startWindow.css");
        walletStage.setScene(walletScene);

        walletStage.showAndWait();

        return selectedFile;
    }

    public WalletContainer confirmWalletPassword(File file) {
        Stage walletPasswordStage = new Stage();
        walletPasswordStage.setResizable(false);
        walletPasswordStage.initStyle(StageStyle.UNDECORATED);
        walletPasswordStage.setTitle("Wallet file: Security");

        WalletContainer walletContainer = new WalletContainer();

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, "Wallet file - Security", closeBtn, walletPasswordStage);
        closeBtn.setOnAction(event -> {
            walletPasswordStage.close();
        });
        Button imageButton = createImageButton(lockDocumentImg, "Wallet File");

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
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, 600, 425);

        passwordScene.getStylesheets().add("/css/startWindow.css");
        walletPasswordStage.setScene(passwordScene);

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                try {

                    walletContainer.setWallet(Wallet.load(file.toPath(), passwordField.getText()));;

                    walletPasswordStage.close();
                } catch (Exception e1) {

                    passwordField.setText("");
                }

            }
        });
        walletPasswordStage.showAndWait();
        return walletContainer;
    }

    public static String restoreMnemonicStage() {
        String titleStr = "Ergo - Restore wallet: Mnemonic phrase";

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(logo);
        mnemonicStage.setResizable(false);
        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, titleStr, closeBtn, mnemonicStage);

        Button imageButton = createImageButton(ergoLogo, "Restore wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase - Required to recover wallet:");
        subTitleTxt.setFill(txtColor);
        subTitleTxt.setFont(txtFont);

        HBox subTitleBox = new HBox(subTitleTxt);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);

        TextArea mnemonicField = new TextArea();
        mnemonicField.setFont(txtFont);
        mnemonicField.setId("formField");

        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicBox = new HBox(mnemonicField);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Words left: 15");
        nextBtn.setId("toolBtn");
        nextBtn.setFont(txtFont);
        nextBtn.setDisable(true);
        nextBtn.setOnAction(nxtEvent -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");

            List<String> mnemonicList = Arrays.asList(words);
            try {
                Mnemonic.checkEnglishMnemonic(mnemonicList);
                mnemonicStage.close();
            } catch (MnemonicValidationException e) {
                Alert a = new Alert(AlertType.NONE, "Error: Mnemonic invalid\n\nPlease correct the mnemonic phrase and try again.", ButtonType.CLOSE);
                a.initOwner(mnemonicStage);
                a.setTitle("Error: Mnemonic invalid.");
            }

        });

        mnemonicField.setOnKeyPressed(e1 -> {
            String mnemonicString = mnemonicField.getText();;

            String[] words = mnemonicString.split("\\s+");
            int numWords = words.length;
            if (numWords == 15) {
                nextBtn.setText("Ok");
                List<String> mnemonicList = Arrays.asList(words);
                try {
                    Mnemonic.checkEnglishMnemonic(mnemonicList);
                    nextBtn.setDisable(false);

                } catch (MnemonicValidationException e) {
                    nextBtn.setText("Invalid");
                    nextBtn.setId("toolBtn");
                    nextBtn.setDisable(true);
                }

            } else {
                if (nextBtn.getText().equals("")) {
                    nextBtn.setText("Words left: 15");
                } else {
                    nextBtn.setText("Words left: " + (15 - numWords));
                }

                nextBtn.setId("toolBtn");
                nextBtn.setDisable(true);
            }

        });

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(subTitleBox, mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);

        mnemonicScene.getStylesheets().add("/css/startWindow.css");
        mnemonicStage.setScene(mnemonicScene);

        closeBtn.setOnAction(e -> {
            Alert terminateAlert = new Alert(AlertType.NONE, "Wallet creation:\n\nTerminated.", ButtonType.CLOSE);
            terminateAlert.initOwner(mnemonicStage);
            terminateAlert.setTitle("Wallet creation: Terminated");
            terminateAlert.showAndWait();

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();

    }

    public static String createMnemonicStage() {
        String titleStr = "Ergo - New wallet: Mnemonic phrase";

        String mnemonic = Mnemonic.generateEnglishMnemonic();

        Stage mnemonicStage = new Stage();

        mnemonicStage.setTitle(titleStr);

        mnemonicStage.getIcons().add(logo);
        mnemonicStage.setResizable(false);
        mnemonicStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, titleStr, closeBtn, mnemonicStage);

        Button imageButton = createImageButton(ergoLogo, "New Wallet");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text subTitleTxt = new Text("> Mnemonic phrase - Required to recover wallet:");
        subTitleTxt.setFill(txtColor);
        subTitleTxt.setFont(txtFont);

        HBox subTitleBox = new HBox(subTitleTxt);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);

        TextArea mnemonicField = new TextArea(mnemonic);
        mnemonicField.setFont(txtFont);
        mnemonicField.setId("textField");
        mnemonicField.setEditable(false);
        mnemonicField.setWrapText(true);
        mnemonicField.setPrefRowCount(2);
        HBox.setHgrow(mnemonicField, Priority.ALWAYS);

        Platform.runLater(() -> mnemonicField.requestFocus());

        HBox mnemonicBox = new HBox(mnemonicField);
        mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(15, 0, 0, 0));

        Button nextBtn = new Button("Next");

        nextBtn.setFont(txtFont);
        nextBtn.setOnAction(nxtEvent -> {
            Alert nextAlert = new Alert(AlertType.NONE, "User Agreement:\n\nI have written the mnemonic phrase down and will store it in a secure location.", ButtonType.NO, ButtonType.YES);
            nextAlert.initOwner(mnemonicStage);
            nextAlert.setTitle("User Agreement");
            Optional<ButtonType> result = nextAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                mnemonicStage.close();
            }
        });

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(subTitleBox, mnemonicBox, gBox, nextBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene mnemonicScene = new Scene(layoutVBox, 600, 425);

        mnemonicScene.getStylesheets().add("/css/startWindow.css");
        mnemonicStage.setScene(mnemonicScene);

        closeBtn.setOnAction(e -> {
            Alert terminateAlert = new Alert(AlertType.NONE, "Wallet creation:\n\nTerminated.", ButtonType.CLOSE);
            terminateAlert.initOwner(mnemonicStage);
            terminateAlert.setTitle("Wallet creation: Terminated");
            terminateAlert.showAndWait();

            mnemonicStage.close();
            mnemonicField.setText("");

        });

        mnemonicStage.showAndWait();

        return mnemonicField.getText();
    }

    public static HBox createTopBar(Image iconImage, String titleString, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitHeight(18);
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
        newTopBar.setPadding(new Insets(5, 8, 10, 10));
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

    private static Button createImageButton(Image image, String name) {
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

    /* 
    private static void updateMainLayout(){
        if(defaultWallet == null)
        {
            imageView.setImage(new Image("/assets/add-circle-outline-white-256.png"));

        }else{
            imageView.setImage(new Image("/assets/ergo-white-256.png"));
        }
    }*/
    private static ImageView highlightedImageView(Image image) {

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

        Scene passwordScene = new Scene(layoutVBox, 600, 425);

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

    public class ItemIterator {

        private int m_i;
        private int m_j;

        public ItemIterator() {
            m_i = 0;
            m_j = 0;
        }

        public void setI(int i) {
            m_i = i;
        }

        public int getI() {
            return m_i;
        }

        public int getJ() {
            return m_j;
        }

        public void setJ(int j) {
            m_j = j;
        }
    }
}
