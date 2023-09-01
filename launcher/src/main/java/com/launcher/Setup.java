package com.launcher;

import javafx.event.EventHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Hex;
import org.reactfx.util.FxTimer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.HostServices;

public class Setup extends Application {

    public static String javaName = "Java 19 (x64)";
    public static String javaURL = "https://download.oracle.com/java/19/latest/jdk-19_windows-x64_bin.exe";

    public static String updateUrl = "https://github.com/networkspore/Netnotes/releases/latest/download";
    public static String javaUrl = "https://www.java.com/en/download/";

    public static final String programFilesDir = System.getenv("LOCALAPPDATA") + "\\Net Notes";

    public static Font mainFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    public static Font txtFont = Font.font("OCR A Extended", 15);
    public static Font smallFont = Font.font("OCR A Extended", 11);
    public static Font titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
    public static Color txtColor = Color.web("#cdd4da");

    public static Image icon = new Image("/assets/icon20.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image ergoLogo = new Image("/assets/ergo-black-350.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");
    public static Image closeImg = new Image("/assets/close-outline-white.png");
    public static Image minimizeImg = new Image("/assets/minimize-white-20.png");

    public static String javaFileName = "jdk-19_windows-x64_bin.exe";

    private HostServices services = getHostServices();

    @Override
    public void start(Stage appStage) {
        // HostServices hostServices;
        Platform.setImplicitExit(true);
        appStage.initStyle(StageStyle.UNDECORATED);
        Parameters params = getParameters();
        List<String> list = params.getRaw();

        Version javaVersion = null;
        JsonObject launcherData = null;

        String currentAppJar = "";

        boolean firstRun = false;
        boolean doUpdates = false;

        for (String each : list) {

            if (each.startsWith(Main.firstRun)) {

                firstRun = true;
            }
            if (each.startsWith(Main.setupUpdates)) {
                doUpdates = true;
            }

            if (each.startsWith(Main.currentJavaVersionEquals)) {

                if (each.length() > Main.currentJavaVersionEquals.length()) {

                    javaVersion = new Version(each.substring(Main.currentJavaVersionEquals.length(), each.length()));
                } else {
                    javaVersion = null;
                }
            }

            if (each.startsWith(Main.currentAppJarEquals)) {

                if (each.length() > Main.currentAppJarEquals.length()) {
                    currentAppJar = each.substring(Main.currentAppJarEquals.length(), each.length());
                } else {
                    currentAppJar = "";
                }

            }

        }

        VBox bodyVBox = new VBox();
        if (firstRun) {
            firstRun(appStage, javaVersion, currentAppJar, bodyVBox);
        } else {
            if (doUpdates) {
                try {
                    //  getReleaseInfo(javaVersion, currentAppJar);
                } catch (Exception e) {

                }
            } else {

            }
        }

    }

    private void firstRun(Stage appStage, Version javaVersion, String currentAppJar, VBox bodyVBox) {
        bodyVBox.getChildren().clear();
        setSetupStage(appStage, "Netnotes - Setup", "Setup...", bodyVBox);

        Text directoryTxt = new Text("> Location:");
        directoryTxt.setFill(txtColor);
        directoryTxt.setFont(txtFont);

        Button directoryBtn = new Button(programFilesDir);
        directoryBtn.setFont(txtFont);
        directoryBtn.setId("toolBtn");

        Button defaultBtn = new Button("(default)");
        defaultBtn.setFont(txtFont);
        defaultBtn.setId("toolBtn");
        defaultBtn.setVisible(false);
        defaultBtn.setOnAction(btnEvent -> {
            directoryBtn.setText(programFilesDir);
            defaultBtn.setVisible(false);
        });

        directoryBtn.setOnAction(btnEvent -> {

            DirectoryChooser dirChooser = new DirectoryChooser();

            File chosenDir = dirChooser.showDialog(appStage);
            if (chosenDir != null) {
                directoryBtn.setText(chosenDir.getAbsolutePath());
                defaultBtn.setVisible(true);
            }
        });

        HBox.setHgrow(directoryBtn, Priority.ALWAYS);

        HBox directoryBox = new HBox(directoryTxt, directoryBtn, defaultBtn);
        directoryBox.setAlignment(Pos.CENTER_LEFT);

        Text currentDirTxt = new Text("> Use current directory:");
        currentDirTxt.setFill(txtColor);
        currentDirTxt.setFont(txtFont);

        Button currentDirBtn = new Button("Disabled");
        currentDirBtn.setId("toolBtn");
        currentDirBtn.setFont(txtFont);
        currentDirBtn.setOnAction(btnEvent -> {
            if (currentDirBtn.getText().equals("Enabled")) {
                currentDirBtn.setText("Disabled");
                directoryBtn.setVisible(true);
                if (!directoryBtn.getText().equals(programFilesDir)) {
                    defaultBtn.setVisible(true);
                }
            } else {
                currentDirBtn.setText("Enabled");
                directoryBtn.setVisible(false);
                defaultBtn.setVisible(false);
            }
        });

        HBox currentDirBox = new HBox(currentDirTxt, currentDirBtn);
        currentDirBox.setAlignment(Pos.CENTER_LEFT);

        Text updatesTxt = new Text("> Updates:");
        updatesTxt.setFill(txtColor);
        updatesTxt.setFont(txtFont);

        Button updatesBtn = new Button("Enabled");
        updatesBtn.setId("inactiveMainImageBtn");
        updatesBtn.setFont(txtFont);
        updatesBtn.setOnAction(btnEvent -> {
            if (updatesBtn.getText().equals("Enabled")) {
                updatesBtn.setText("Disabled");
            } else {
                updatesBtn.setText("Enabled");
            }
        });

        HBox updatesBox = new HBox(updatesTxt, updatesBtn);
        updatesBox.setAlignment(Pos.CENTER_LEFT);
        updatesBox.setPadding(new Insets(3, 0, 0, 0));

        Button nextBtn = new Button("Next");
        nextBtn.setId("toolSelected");
        nextBtn.setFont(txtFont);

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(25, 0, 0, 0));

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        bodyVBox.getChildren().addAll(directoryBox, currentDirBox, updatesBox, gBox, nextBox);

        nextBtn.setOnAction(btnEvent -> {

            boolean updates = updatesBtn.getText().equals("Enabled");

            String directoryString = currentDirBtn.getText().equals("Enabled") ? Main.currentDirectory : directoryBtn.getText();
            File directoryFile = new File(directoryString);

            if (!directoryFile.isDirectory()) {

                Alert a = new Alert(AlertType.NONE, "This will create the directory:\n\n" + directoryString + "\n\n ", ButtonType.OK, ButtonType.CANCEL);
                a.initOwner(appStage);

                Optional<ButtonType> result = a.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {

                    if (directoryFile.mkdir()) {
                        createPassword(javaVersion, currentAppJar, updates, directoryFile, bodyVBox, appStage);
                    }

                }
            } else {
                createPassword(javaVersion, currentAppJar, updates, directoryFile, bodyVBox, appStage);
            }

        });

        appStage.show();
    }

    private void createPassword(Version javaVersion, String appJar, boolean updates, File installDir, VBox bodyVBox, Stage appStage) {
        bodyVBox.getChildren().clear();
        setSetupStage(appStage, "Netnotes - Security", "Security...", bodyVBox);

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

        bodyVBox.getChildren().addAll(passwordBox, clickRegion);

        Platform.runLater(() -> passwordField.requestFocus());

        passwordField.setOnKeyPressed(e1 -> {

            KeyCode keyCode = e1.getCode();

            if ((keyCode == KeyCode.ENTER || keyCode == KeyCode.TAB)) {

                if (passwordField.getText().length() > 6) {

                    String passStr = passwordField.getText();
                    // createPassField.setText("");
                    bodyVBox.getChildren().remove(clickRegion);

                    passwordField.setVisible(false);

                    Text reenterTxt = new Text("> Re-enter password:");
                    reenterTxt.setFill(txtColor);
                    reenterTxt.setFont(txtFont);

                    Platform.runLater(() -> createPassField2.requestFocus());

                    HBox secondPassBox = new HBox(reenterTxt, createPassField2);
                    secondPassBox.setAlignment(Pos.CENTER_LEFT);

                    bodyVBox.getChildren().addAll(secondPassBox, clickRegion);

                    clickRegion.setOnAction(regionEvent -> {
                        createPassField2.requestFocus();
                    });

                    createPassField2.setOnKeyPressed(pressEvent -> {

                        KeyCode keyCode2 = pressEvent.getCode();

                        if ((keyCode2 == KeyCode.ENTER)) {

                            if (passStr.equals(createPassField2.getText())) {
                                bodyVBox.getChildren().clear();
                                setSetupStage(appStage, "Netnotes - Saving Settings", "Saving...", bodyVBox);
                                Text savingFileTxt = new Text("> Creating:  " + installDir.getAbsolutePath());
                                savingFileTxt.setFill(txtColor);
                                savingFileTxt.setFont(txtFont);

                                bodyVBox.getChildren().add(savingFileTxt);
                                FxTimer.runLater(Duration.ofMillis(100), () -> {
                                    try {
                                        createSettings(javaVersion, appJar, updates, installDir, passStr, bodyVBox, appStage);
                                    } catch (Exception e) {
                                        Alert err = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                                        err.initOwner(appStage);
                                        err.show();
                                        firstRun(appStage, javaVersion, appJar, bodyVBox);
                                    }
                                });
                            } else {
                                bodyVBox.getChildren().remove(secondPassBox);
                                createPassField2.setText("");
                                passwordField.setText("");
                                passwordField.setVisible(true);
                                secondPassBox.getChildren().clear();
                                Platform.runLater(() -> passwordField.requestFocus());
                            }
                        }
                    });
                }
            }
        });

    }

    private void createSettings(Version javaVersion, String appJar, boolean updates, File installDir, String password, VBox bodyVBox, Stage appStage) throws IOException {

        String installDirString = installDir.getAbsolutePath();
        File settingsFile = new File(installDirString + "\\" + Main.settingsFileName);

        String hash = getBcryptHashString(password);

        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("appKey", hash);
        jsonObj.addProperty("updates", updates);
        jsonObj.addProperty("networks", "");
        String jsonString = jsonObj.toString();

        Files.writeString(settingsFile.toPath(), jsonString);

        Path newPath = null;
        boolean validJar = appJar != "";
        File jarFile = null;

        if (validJar) {
            jarFile = new File(appJar);
            validJar = checkJar(jarFile);
            if (validJar) {
                newPath = Paths.get(installDirString + "\\" + jarFile.getName());
            }
        }

        boolean validJava = javaVersion != null && (javaVersion.compareTo(new Version("17.0.3")) > -1);

        boolean moveFiles = !installDir.getAbsolutePath().equals(Main.currentDirectory);

        if (validJar && validJava) {

            File launcherFile = null;

            try {
                if (moveFiles) {
                    URL classLocation = Utils.getLocation(getClass());
                    launcherFile = Utils.urlToFile(classLocation);

                    Files.move(jarFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING);

                    openJar(installDirString + "\\" + jarFile.getName(), launcherFile);
                } else {
                    Main.openJar(jarFile.getAbsolutePath());
                }
                shutdownNow();
            } catch (Exception e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                a.initOwner(appStage);
                a.showAndWait();
                shutdownNow();
            }

        } else {
            if (validJar && moveFiles) {

                Files.move(jarFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
            }

            getSetupFiles(validJar, validJava, jarFile, installDir, bodyVBox, appStage);

        }

    }

    public void openJar(String jarFilePathString, File launcher) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("jarFilePath", jarFilePathString);
        obj.addProperty("launcher", launcher.getAbsolutePath());

        byte[] byteString = obj.toString().getBytes(StandardCharsets.UTF_8);

        String hexJson = Hex.encodeHexString(byteString);

        String[] cmdString;

        cmdString = new String[]{"cmd", "/c", "javaw", "-jar", jarFilePathString, hexJson};

        Runtime.getRuntime().exec(cmdString);

        Platform.exit();
        System.exit(0);
    }

    public static String getBcryptHashString(String password) {
        SecureRandom sr;

        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {

            sr = new SecureRandom();
        }

        return BCrypt.with(BCrypt.Version.VERSION_2A, sr, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).hashToString(15, password.toCharArray());
    }

    private void getSetupFiles(boolean validJar, boolean validJava, File jarFile, File installDir, VBox bodyVBox, Stage appStage) {
        bodyVBox.getChildren().clear();
        setSetupStage(appStage, "Netnotes - Download files", "Download files...", bodyVBox);

        appStage.show();

        Text getJavaTxt = new Text("> Setup files required, would you like to download? (Y/n):");
        getJavaTxt.setFill(txtColor);
        getJavaTxt.setFont(txtFont);

        TextField getJavaField = new TextField();
        getJavaField.setFont(txtFont);
        getJavaField.setId("formField");

        Platform.runLater(() -> getJavaField.requestFocus());

        HBox getJavaBox = new HBox(getJavaTxt, getJavaField);
        getJavaBox.setAlignment(Pos.CENTER_LEFT);

        Button clickRegion = new Button();
        clickRegion.setMaxWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(Double.MAX_VALUE);

        clickRegion.setOnAction(e -> {
            getJavaField.requestFocus();
        });

        bodyVBox.getChildren().addAll(getJavaBox, clickRegion);

        getJavaField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();
            if (keyCode == KeyCode.Y || keyCode == KeyCode.ENTER) {
                getJavaField.setDisable(true);

                ProgressBar progressBar = new ProgressBar(0);

                setupDownloadField(bodyVBox, progressBar);
                FxTimer.runLater(Duration.ofMillis(100), () -> {
                    try {

                        if (!validJar) {
                            //To do: update name
                            String jarFileName = "netnotes.jar";
                            File file = new File(installDir + "\\" + javaFileName);
                            String jarURL = "";
                            downloadJar(validJava, installDir, file, jarURL, progressBar);
                        } else {
                            File file = new File(installDir.getAbsolutePath() + "\\" + javaFileName);

                            if (!validJava) {
                                downloadJava(jarFile, file, installDir, javaURL, progressBar);
                            }
                        }

                    } catch (Exception dlException) {
                        Alert a = new Alert(AlertType.NONE, dlException.toString(), ButtonType.OK);
                        a.initOwner(appStage);
                        a.show();
                        visitWebsites(validJava, validJar, installDir, bodyVBox, appStage);
                    }
                });
            } else {
                visitWebsites(validJava, validJar, installDir, bodyVBox, appStage);
            }
        }
        );

    }

    private void visitWebsites(boolean validJava, boolean validJar, File installDir, VBox bodyVBox, Stage appStage) {
        bodyVBox.getChildren().clear();
        setSetupStage(appStage, "Netnotes - Get latest release", "Get the latest release...", bodyVBox);
        appStage.show();

        Text getJavaTxt = new Text("> Java URL:");
        getJavaTxt.setFill(txtColor);
        getJavaTxt.setFont(txtFont);

        TextField javaURLField = new TextField(javaUrl);
        javaURLField.setFont(txtFont);
        javaURLField.setId("formField");
        javaURLField.setEditable(false);
        HBox.setHgrow(javaURLField, Priority.ALWAYS);

        HBox javaUrlHbox = new HBox(getJavaTxt, javaURLField);
        javaUrlHbox.setAlignment(Pos.CENTER_LEFT);

        bodyVBox.getChildren().addAll(javaUrlHbox);

        Text getJarTxt = new Text("> Update URL:");
        getJarTxt.setFill(txtColor);
        getJarTxt.setFont(txtFont);

        TextField latestURLField = new TextField(updateUrl);
        latestURLField.setFont(txtFont);
        latestURLField.setId("formField");
        latestURLField.setEditable(false);
        HBox.setHgrow(latestURLField, Priority.ALWAYS);

        HBox getJarBox = new HBox(getJarTxt, latestURLField);
        getJarBox.setAlignment(Pos.CENTER_LEFT);

        Button latestBtn = new Button("Get program files");
        latestBtn.setPadding(new Insets(10, 40, 10, 40));
        latestBtn.setFont(txtFont);
        latestBtn.setId("toolBtn");
        latestBtn.setOnAction(btnEvent -> {
            services.showDocument(updateUrl);
        });

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select 'Jar'");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Netnotes", "*.jar"));

        Button selectBtn = new Button("Select 'netnotes-x.x.x.jar'");
        selectBtn.setPadding(new Insets(10, 10, 10, 10));
        selectBtn.setFont(txtFont);
        selectBtn.setId("toolBtn");
        selectBtn.setOnAction(btnEvent -> handleChooser(installDir, appStage, chooser));

        Region spacer = new Region();

        spacer.setPrefWidth(20);

        HBox handleJarBox = new HBox(latestBtn, spacer, selectBtn);
        handleJarBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(handleJarBox, Priority.ALWAYS);
        handleJarBox.setPadding(new Insets(15, 0, 0, 0));

        bodyVBox.getChildren().addAll(getJarBox, handleJarBox);

        Button javaBtn = new Button("Get Java");
        javaBtn.setPadding(new Insets(10, 10, 10, 10));
        javaBtn.setFont(txtFont);
        javaBtn.setId("toolBtn");
        javaBtn.setOnAction(btnEvent -> {
            services.showDocument(javaUrl);
        });
        HBox javaBtnBox = new HBox(javaBtn);
        javaBtnBox.setAlignment(Pos.CENTER);
        javaBtnBox.setPadding(new Insets(5, 0, 0, 0));
        bodyVBox.getChildren().add(javaBtnBox);

        appStage.setWidth(1000);
        appStage.setHeight(500);
    }

    public static boolean checkJar(File jarFile) {
        ZipFile zip = null;
        boolean isJar = false;
        try {
            zip = new ZipFile(jarFile);
            isJar = true;
        } catch (Exception zipException) {

        } finally {
            try {
                zip.close();
            } catch (IOException e) {

            }
        }

        return isJar;
    }

    private void handleChooser(File installDir, Stage appStage, FileChooser chooser) {

        File chosenFile = chooser.showOpenDialog(appStage);

        boolean isJar = checkJar(chosenFile);

        if (!isJar) {
            Alert a = new Alert(AlertType.NONE, "Invalid file.\n\nPlease visit gitHub for the latest release.", ButtonType.CLOSE);
            a.initOwner(appStage);
            a.show();
        } else {
            String chosenFileName = chosenFile.getName();
            Version jarVersion = AppJar.getVersionFromFileName(chosenFileName);
            String unknownName = "netnotes-0.0.0.jar";

            String fileName = "";

            int versionTest = jarVersion.compareTo(new Version("0.0.0"));

            if (versionTest > 0) {
                fileName = chosenFileName;
            } else {
                fileName = unknownName;
            }

            String newJarString = installDir.getAbsolutePath() + "\\" + fileName;

            try {
                Path newJarPath = Paths.get(newJarString);
                Files.move(chosenFile.toPath(), newJarPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {

                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.initOwner(appStage);
                a.showAndWait();
                shutdownNow();
            }

            try {
                URL classLocation = Utils.getLocation(getClass());
                File launcherFile = Utils.urlToFile(classLocation);

                openJar(newJarString, launcherFile);
                shutdownNow();
            } catch (Exception e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.initOwner(appStage);
                a.show();

            }

        }

    }

    private static void downloadJar(boolean validJava, File installDir, File file, String urlString, ProgressBar progressBar) throws Exception {

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {

                URI uri = URI.create(urlString);

                InputStream inputStream = null;
                OutputStream outputStream = null;

                URL url = uri.toURL();

                String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();

                inputStream = con.getInputStream();

                outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[2048];

                int length;
                long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    downloaded += (long) length;

                    updateProgress(downloaded, contentLength);

                }

                outputStream.close();
                inputStream.close();

                return null;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(evt -> {

        });

        Thread t = new Thread(task);
        t.start();

    }

    private void downloadJava(File jarFile, File file, File installDir, String urlString, ProgressBar progressBar) throws Exception {

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {

                URI uri = URI.create(urlString);

                InputStream inputStream = null;
                OutputStream outputStream = null;

                URL url = uri.toURL();

                String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();

                inputStream = con.getInputStream();

                outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[2048];

                int length;
                long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    downloaded += (long) length;

                    updateProgress(downloaded, contentLength);

                }

                outputStream.close();
                inputStream.close();

                return null;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(evt -> {
            runJavaSetup(jarFile, file, installDir);
        });

        Thread t = new Thread(task);
        t.start();

    }

    private void runJavaSetup(File jarFile, File setupFile, File installDir) {

        String cmdString = "cmd /c " + setupFile.getAbsolutePath();

        try {
            Process p = Runtime.getRuntime().exec(cmdString);

            int exitCode = p.waitFor();

            if (exitCode == 0) {

                Path javaFilePath = setupFile.toPath();
                Files.deleteIfExists(javaFilePath);

                //ToDo: Next Setup Step
            } else {
                Alert a = new Alert(AlertType.NONE, "The installation did not complete.\n\nTry again?", ButtonType.YES, ButtonType.NO);
                a.initOwner(null);
                Optional<ButtonType> result = a.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.YES) {
                    runJavaSetup(jarFile, setupFile, installDir);
                } else {
                    boolean validJar = checkJar(jarFile);
                    //     visitWebsites(false, validJar, setupFile, null, null);
                }

            }

        } catch (Exception e) {
            Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);

            a.showAndWait();
            //  visitWebsites(false, false, setupFile, null, null);
        }

    }

    public static void getReleaseInfo(Version javaVersion, String currentAppJar) throws Exception {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        URI uri = new URI(Main.latestReleaseURLstring);

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {

                //File file = new File(appDataDirectory + "\\" + );
                InputStream inputStream = null;

                URL url = uri.toURL();

                String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                //  long contentLength = con.getContentLengthLong();
                inputStream = con.getInputStream();

                byte[] buffer = new byte[2048];

                int length;
                // long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    //    downloaded += (long) length;

                    //updateProgress(downloaded, contentLength); for small files
                }

                inputStream.close();

                return null;
            }
        };
        // progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(evt -> {
            SetupData latestSetup = null;
            String json = outputStream.toString();

            JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

            try {
                latestSetup = new SetupData(jsonObject);
            } catch (Exception e) {

            }
            if (latestSetup != null) {
                //checkLatestSetup(latestSetup, javaVersion, launcherData);
            } else {

            }
        });

        Thread t = new Thread(task);
        t.start();

    }

    private static void setupDownloadField(VBox bodyVBox, ProgressBar progressBar) {
        bodyVBox.getChildren().clear();

        Text downloadingTxt = new Text("> Downloading " + javaName + "...");
        downloadingTxt.setFill(txtColor);
        downloadingTxt.setFont(txtFont);

        progressBar.setPrefWidth(400);

        HBox progressBox = new HBox(progressBar);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(50, 0, 0, 0));

        HBox downloadBox = new HBox(downloadingTxt);
        downloadBox.setAlignment(Pos.CENTER_LEFT);

        bodyVBox.getChildren().addAll(downloadBox, progressBox);

    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;

        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

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

    public static void setSetupStage(Stage appStage, String title, String setupMessage, VBox bodyVBox) {

        appStage.setResizable(false);

        appStage.setTitle(title);
        appStage.getIcons().add(logo);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeClick -> {
            shutdownNow();
        });

        HBox topBar = createTopBar(icon, title, closeBtn, appStage);

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(20, 0, 20, 0));

        Text setupTxt = new Text("> " + setupMessage);
        setupTxt.setFill(txtColor);
        setupTxt.setFont(txtFont);

        Text spacerTxt = new Text(">");
        spacerTxt.setFill(txtColor);
        spacerTxt.setFont(txtFont);

        HBox line2 = new HBox(spacerTxt);
        line2.setAlignment(Pos.CENTER_LEFT);
        line2.setPadding(new Insets(10, 0, 6, 0));

        bodyVBox.getChildren().addAll(setupTxt, line2);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(topBar, imageBox, bodyVBox);

        Scene setupScene = new Scene(layoutVBox, 625, 450);
        setupScene.getStylesheets().add("/css/startWindow.css");

        appStage.setScene(setupScene);

    }

    private static void shutdownNow() {

        Platform.exit();
        System.exit(0);
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

    static class Delta {

        double x, y;
    }

}
