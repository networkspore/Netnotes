package com.netnotes;

/**
 * Netnotes
 *
 */
import javafx.application.Application;
import javafx.application.Platform;
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
import mslinks.ShellLinkException;
import javafx.scene.input.KeyCode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.ergoplatform.appkit.*;
import org.reactfx.util.FxTimer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.satergo.Wallet;
import com.utils.Utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

public class App extends Application {

    //public members
    public static Font mainFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    public static Font txtFont = Font.font("OCR A Extended", 15);
    public static Font titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
    public static Color txtColor = Color.web("#cdd4da");

    public static Image icon = new Image("/assets/icon20.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image ergoLogo = new Image("/assets/ergo-black-350.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");
    public static Image closeImg = new Image("/assets/close-outline-white.png");
    public static Image minimizeImg = new Image("/assets/minimize-white-20.png");
    public static Image globeImg = new Image("/assets/globe-outline-white-120.png");
    public static Image settingsImg = new Image("/assets/settings-outline-white-120.png");

    public static final String settingsFileName = "settings.conf";
    public static final String networksFileName = "networks.dat";

    public static final String homeString = System.getProperty("user.home");

    private AppData appData;

    private File settingsFile = null;
    private File networksFile;
    private File currentDir = null;
    private File launcherFile = null;
    private File currentJar = null;

    private ArrayList<Network> networks = new ArrayList<>();

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

    private void loadNetworks() throws Exception {
        if (networksFile.isFile()) {

            String fileHexString = Files.readString(networksFile.toPath());
            byte[] bytes = Hex.decode(fileHexString);
            String jsonArrayString = new String(bytes, StandardCharsets.UTF_8);
            JsonArray jsonArray = new JsonParser().parse(jsonArrayString).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject arrayObject = element.getAsJsonObject();
                Network savedNetwork = new Network(arrayObject);
                networks.add(savedNetwork);
            }

        }

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

        VBox.setVgrow(menuBox, Priority.ALWAYS);
        VBox bodyVBox = new VBox();
        HBox.setHgrow(bodyVBox, Priority.ALWAYS);
        VBox.setVgrow(bodyVBox, Priority.ALWAYS);

        HBox mainHbox = new HBox(menuBox, bodyVBox);

        VBox layout = new VBox(titleBox, mainHbox);

        Scene appScene = new Scene(layout, 800, 450);
        appScene.getStylesheets().add("/css/startWindow.css");
        appStage.setScene(appScene);

        closeBtn.setOnAction(e -> {
            appStage.close();
        });

        settingsBtn.setOnAction(e -> {
            showSettings(appStage, bodyVBox);
        });

        networksBtn.setOnAction(e -> {
            showNetworks(bodyVBox);
        });

        appStage.show();

        try {
            loadNetworks();
        } catch (Exception e1) {

            Alert a = new Alert(AlertType.NONE, "Fatal error: Error unable to load networks.\n\n" + e1.toString(), ButtonType.CLOSE);
            a.setTitle("Fatal error: Unable to load networks.");
            a.initOwner(appStage);
            a.showAndWait();
            shutdownNow();
        }

    }

    private void showSettings(Stage appStage, VBox bodyVBox) {
        bodyVBox.getChildren().clear();

        boolean isUpdates = appData.getUpdates();

        Button settingsButton = createImageButton(logo, "Settings");

        HBox settingsBtnBox = new HBox(settingsButton);
        settingsBtnBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Update password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        Button passwordBtn = new Button("(click to update)");
        passwordBtn.setFont(txtFont);
        passwordBtn.setId("formField");
        passwordBtn.setOnAction(e -> {
            Stage newPasswordStage = new Stage();
            final String newPassword = createPassword(newPasswordStage, "Net Notes - Update password", logo, "Update password...");

            Stage statusStage = new Stage();
            setStatusStage(statusStage, "Net Notes - Saving...", "Saving...");
            statusStage.show();
            FxTimer.runLater(Duration.ofMillis(100), () -> {
                String hash = Utils.getBcryptHashString(newPassword);

                try {

                    appData.setAppKey(hash);
                } catch (IOException e1) {
                    Alert a = new Alert(AlertType.NONE, "Error:\n\n" + e1.toString(), ButtonType.CLOSE);
                    a.initOwner(appStage);
                    a.show();
                }

                statusStage.close();

            });
        });

        HBox passwordBox = new HBox(passwordTxt, passwordBtn);
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        Text updatesTxt = new Text("> Updates:");
        updatesTxt.setFill(txtColor);
        updatesTxt.setFont(txtFont);

        Button updatesBtn = new Button(isUpdates ? "Enabled" : "Disabled");
        updatesBtn.setFont(txtFont);
        updatesBtn.setId("formField");
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
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        bodyVBox.getChildren().addAll(settingsBtnBox, passwordBox, updatesBox);
    }

    private void showNetworks(VBox bodyVBox) {
        bodyVBox.getChildren().clear();
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

                if (passwordField.getText().length() > 6) {

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
            }
        });
        passwordStage.showAndWait();

        return passwordField.getText();
    }

    /*
    private static Wallet selectWallet(Stage callingStage) {
        File ergFile = getFile("Ergo wallet", callingStage, new FileChooser.ExtensionFilter("Ergo wallet", "*.erg"));

        if (ergFile == null) {
            return null;
        } else {

            String password = confirmPassword("Ergo - Wallet password", "Wallet password", "");
            try {

            } catch (Exception e) {
                return null;
            }
        }

        //    new com.satergo.Wallet()
    } */
    private static void restoreWallet(Stage callingStage) {

    }

    public void addNetwork() {
        Stage networkStage = new Stage();
        networkStage.setTitle("Add Ergo Network");
        networkStage.getIcons().add(ergoLogo);
        networkStage.setResizable(false);
        networkStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {
            networkStage.close();
        });

        HBox titleBox = createTopBar(icon, "Add Network", closeBtn, networkStage);

        Button imageButton = createImageButton(ergoLogo, "Add Network");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text networkTypeTxt = new Text("> Type:  Ergo");
        networkTypeTxt.setFill(txtColor);
        networkTypeTxt.setFont(txtFont);

        HBox networkTypeBox = new HBox(networkTypeTxt);
        networkTypeBox.setPadding(new Insets(3, 0, 5, 0));
        HBox.setHgrow(networkTypeBox, Priority.ALWAYS);

        Text locationTxt = new Text("> Location:");
        locationTxt.setFill(txtColor);
        locationTxt.setFont(txtFont);

        Button locationBtn = new Button("(select)"); //127.0.0.1:9503
        locationBtn.setFont(txtFont);
        locationBtn.setId("formField");

        HBox locationBox = new HBox(locationTxt, locationBtn);
        locationBox.setAlignment(Pos.CENTER_LEFT);

        Text walletTxt = new Text("> Wallet (*.erg):");
        walletTxt.setFill(txtColor);
        walletTxt.setFont(txtFont);

        Button existingWalletBtn = new Button("(select)");
        existingWalletBtn.setId("formField");
        existingWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        existingWalletBtn.setFont(txtFont);

        existingWalletBtn.setOnAction(e -> {
            //Wallet existingWallet = selectWallet(networkStage);

            /*  Alert a = new Alert(AlertType.NONE, existingWallet != null ? "not null" : "is null", ButtonType.OK);
            a.initOwner(networkStage);
            a.show(); */
        });

        HBox.setHgrow(existingWalletBtn, Priority.ALWAYS);

        Button newWalletBtn = new Button("(new)");
        newWalletBtn.setId("toolBtn");
        newWalletBtn.setPadding(new Insets(2, 15, 2, 15));
        newWalletBtn.setFont(txtFont);
        Network newNetwork = new Network();

        newWalletBtn.setOnAction(newWalletEvent -> {

            String password = createPassword(networkStage, "Ergo - New wallet: Password", ergoLogo, "New Wallet");
            if (!password.equals("")) {
                String seedPhrase = createMnemonicStage();

                if (!seedPhrase.equals("")) {
                    Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase), SecretString.create(password));

                    try {
                        newNetwork.setWallet(Wallet.create(mnemonic, password.toCharArray(), false));
                        newNetwork.save(networksFile);
                    } catch (Exception e1) {
                        Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                        a.initOwner(networkStage);
                        a.show();
                    }
                }
            }
        });

        Button restoreWalletBtn = new Button("(restore)");
        restoreWalletBtn.setId("toolBtn");
        restoreWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        restoreWalletBtn.setFont(txtFont);

        HBox walletBox = new HBox(walletTxt, existingWalletBtn, newWalletBtn, restoreWalletBtn);
        walletBox.setAlignment(Pos.CENTER_LEFT);

        VBox bodyBox = new VBox(networkTypeBox, locationBox, walletBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox networkVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene networkScene = new Scene(networkVBox, 450, 500);
        networkScene.getStylesheets().add("/css/startWindow.css");
        networkStage.setScene(networkScene);

        networkStage.show();

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
        nextBtn.setId("toolSelected");
        nextBtn.setFont(txtFont);
        nextBtn.setOnAction(nxtEvent -> {
            Alert nextAlert = new Alert(AlertType.NONE, "User Agreement:\n\nI have written this phrase down and stored it in a secure\nlocation.\n\nI understand that this phrase is the only way to recover my wallet if the password is lost.\n\nI understand and accept that I am solely responsible for\nkeeping my mnemonic phrase secret and secure.\n ", ButtonType.NO, ButtonType.YES);
            nextAlert.initOwner(mnemonicStage);
            nextAlert.setTitle("User Agreement");
            Optional<ButtonType> result = nextAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                mnemonicStage.close();
            } else {
                Alert terminateAlert = new Alert(AlertType.NONE, "Wallet creation:\n\nTerminated.", ButtonType.CLOSE);
                terminateAlert.initOwner(mnemonicStage);
                terminateAlert.setTitle("Terminated");
                terminateAlert.showAndWait();
                mnemonicStage.close();
                mnemonicField.setText("");
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
        barIconView.setFitHeight(20);
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
        minimizeImage.setFitHeight(15);
        minimizeImage.setFitWidth(15);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(5, 5, 0, 5));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(10, 8, 10, 10));
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

}
