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

import javafx.scene.control.TextField;
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
import java.util.List;
import java.util.Optional;

import org.bouncycastle.util.encoders.Hex;
import org.ergoplatform.appkit.*;
import org.reactfx.util.FxTimer;

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

    public static final String settingsFileName = "settings.conf";
    public static final String homeString = System.getProperty("user.home");

    public static JsonObject appData;

    public File currentDir = null;
    public File launcherFile = null;
    public File currentJar = null;

    @Override
    public void start(Stage appStage) {
        Platform.setImplicitExit(true);

        appStage.setResizable(false);
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Netnotes");
        appStage.getIcons().add(logo);

        Parameters params = getParameters();
        List<String> list = params.getRaw();

        parseArgs(list, appStage);

        if (currentDir != null) {
            startApp(appStage);
        } else {
            shutdownNow();
        }

    }

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
                String launcherDirString = launcherDir.getAbsolutePath();
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

                            Files.move(launcherFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            Utils.createLink(destinationFile, launcherDir, "Net Notes.lnk");

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

    private void startApp(Stage appStage) {

        File settingsFile = new File(currentDir.getAbsolutePath() + "\\" + settingsFileName);

        if (!settingsFile.isFile()) {

            Alert a = new Alert(AlertType.NONE, "Unable to access user app data. Ensure you have access to:\n\nLocation: " + currentDir.getAbsolutePath() + "\n" + launcherFile.getAbsolutePath(), ButtonType.CLOSE);
            a.initOwner(appStage);
            a.showAndWait();
            shutdownNow();

        } else {

            String passwordHash = null;
            try {

                String jsonString = Files.readString(settingsFile.toPath());
                appData = new JsonParser().parse(jsonString).getAsJsonObject();
                passwordHash = appData.get("appKey").getAsString();
            } catch (Exception e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.initOwner(appStage);
                a.showAndWait();
            }

            if (passwordHash != null) {
                String password = null;
                byte[] hashBytes = passwordHash.getBytes();
                boolean tryAgain = true;
                while (tryAgain) {
                    password = getPasswordStage("Net Notes", logo, "Net Notes");

                    if (password == null) {
                        break;
                    }

                    BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), hashBytes);

                    tryAgain = result.verified;
                }

                if (password != null) {
                    openNetnotes(appStage);
                } else {
                    shutdownNow();
                }
            } else {
                shutdownNow();
            }

        }
    }

    public static void setStatusStage(Stage appStage, String title, String statusMessage) {

        appStage.setResizable(false);
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Net Notes");
        appStage.getIcons().add(logo);

        HBox topBar = createTopBar(icon, title, null, appStage);

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

        VBox layoutVBox = new VBox(topBar, bodyVBox);

        Scene statusScene = new Scene(layoutVBox, 420, 220);
        statusScene.getStylesheets().add("/css/startWindow.css");

        appStage.setScene(statusScene);

    }

    static class Delta {

        double x, y;
    }

    // private static int createTries = 0;
    private static void openNetnotes(Stage appStage) {

        Button closeBtn = new Button();
        Button addBtn = new Button();

        HBox titleBox = createTopBar(icon, "Net Notes", closeBtn, appStage);

        HBox menuBox = createMenu(addBtn);

        VBox layout = new VBox(titleBox, menuBox);

        Scene appScene = new Scene(layout, 800, 450);
        appScene.getStylesheets().add("/css/startWindow.css");
        appStage.setScene(appScene);

        closeBtn.setOnAction(e -> {
            appStage.close();
        }
        );

        addBtn.setOnAction(e -> {
            addNetwork();
        });

        appStage.show();
        String networksString = "";
        try {
            networksString = appData.get("networks").getAsString();
        } catch (Exception e) {

        }

        if (networksString.equals("")) {
            addNetwork();
        }
    }

    private static HBox createMenu(Button addButton) {
        ImageView addImageView = highlightedImageView(new Image("/assets/add-outline-white-40.png"));
        addImageView.setFitHeight(15);
        addImageView.setPreserveRatio(true);

        addButton = new Button("Add");
        addButton.setGraphic(addImageView);
        addButton.setId("toolBtn");
        addButton.setPadding(new Insets(2, 15, 2, 5));

        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                addNetwork();
            }
        });

        HBox hbox = new HBox(addButton);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setId("menuBox");
        hbox.setPadding(new Insets(3, 0, 3, 15));

        return hbox;
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

    private static Wallet selectWallet(Stage callingStage) {
        File ergFile = getFile("Ergo wallet", callingStage, new FileChooser.ExtensionFilter("Ergo wallet", "*.erg"));

        if (ergFile == null) {
            return null;
        } else {
            String password = getPasswordStage("Open wallet", ergoLogo, "Ergo wallet");

            try {
                return Wallet.load(ergFile.toPath(), password);

            } catch (Exception e) {
                return null;
            }
        }

        //    new com.satergo.Wallet()
    }

    private static void restoreWallet(Stage callingStage) {

    }

    public static void addNetwork() {
        Stage networkStage = new Stage();
        networkStage.setTitle("Add Ergo Network");
        networkStage.getIcons().add(logo);
        networkStage.setResizable(false);
        networkStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

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

        Text locationTxt = new Text("> Network URL:");
        locationTxt.setFill(txtColor);
        locationTxt.setFont(txtFont);

        TextField locationField = new TextField("localhost (default)"); //127.0.0.1:9503
        locationField.setFont(txtFont);
        locationField.setId("formField");
        HBox.setHgrow(locationField, Priority.ALWAYS);

        locationField.focusedProperty().addListener((obs, oldVal, newVal)
                -> {
            if (newVal) {
                if (locationField.getText().equals("localhost (default)")) {
                    locationField.setText("");
                }
            } else {
                if (locationField.getText().equals("")) {
                    locationField.setText("localhost (default)");
                }
            }
        });

        HBox locationBox = new HBox(locationTxt, locationField);
        locationBox.setAlignment(Pos.CENTER_LEFT);

        Text walletTxt = new Text("> Wallet (*.erg):");
        walletTxt.setFill(txtColor);
        walletTxt.setFont(txtFont);

        Button existingWalletBtn = new Button("(select)");
        existingWalletBtn.setId("toolBtn");
        existingWalletBtn.setPadding(new Insets(2, 10, 2, 10));
        existingWalletBtn.setFont(txtFont);

        existingWalletBtn.setOnAction(e -> {
            Wallet existingWallet = selectWallet(networkStage);

            Alert a = new Alert(AlertType.NONE, existingWallet != null ? "not null" : "is null", ButtonType.OK);
            a.initOwner(networkStage);
            a.show();
        });

        HBox.setHgrow(existingWalletBtn, Priority.ALWAYS);

        Button newWalletBtn = new Button("(new)");
        newWalletBtn.setId("toolBtn");
        newWalletBtn.setPadding(new Insets(2, 15, 2, 15));
        newWalletBtn.setFont(txtFont);
        newWalletBtn.setOnAction(newWalletEvent -> {
            String walletPassword = createPassword(networkStage, "Create password", ergoLogo, "Ergo wallet");
            Alert a = new Alert(AlertType.NONE, walletPassword, ButtonType.OK);
            a.initOwner(networkStage);
            a.show();
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

        Scene networkScene = new Scene(networkVBox, 400, 500);
        networkScene.getStylesheets().add("/css/startWindow.css");
        networkStage.setScene(networkScene);

        networkStage.show();
        networkStage.setX(networkStage.getX() - 50);
        networkStage.setY(networkStage.getY() + 50);

        closeBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                networkStage.close();
            }
        });

    }

    private static HBox createTopBar(Image iconImage, String titleString, Button closeBtn, Stage theStage) {

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
        ImageView closeImage = highlightedImageView(new Image("/assets/close-outline-white.png"));
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");
        closeBtn.setOnAction(e -> {
            theStage.close();
        }
        );

        Button minimizeBtn = new Button("_");
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setPadding(new Insets(0, 5, 0, 3));
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

    public static String confirmErgoTransactionStage(String information) {
        return getPasswordStage("Confirm transaction", ergoLogo, "Confirm transaction");
    }

    public static String getPasswordStage(String topTitle, Image windowLogo, String windowSubTitle) {

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
