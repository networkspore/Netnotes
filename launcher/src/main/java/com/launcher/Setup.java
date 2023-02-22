package com.launcher;

import javafx.event.EventHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.reactfx.util.FxTimer;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Setup extends Application {

    public static Font mainFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    public static Font txtFont = Font.font("OCR A Extended", 15);
    public static Font titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
    public static Color txtColor = Color.web("#cdd4da");

    public static Image icon = new Image("/assets/icon20.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image ergoLogo = new Image("/assets/ergo-black-350.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");

    public static String javaFileName = "jdk-19_windows-x64_bin.exe";
    public static String javaURL = "https://download.oracle.com/java/19/latest/jdk-19_windows-x64_bin.exe";

    public static String downloadsDir = System.getProperty("user.home") + "\\Downloads";

    @Override
    public void start(Stage appStage) {
        Platform.setImplicitExit(false);

        Parameters params = getParameters();
        List<String> list = params.getRaw();

        boolean isJava = true;

        for (String each : list) {
            if (each.startsWith("noJava")) {
                isJava = false;
            }
        }
        VBox bodyVBox = new VBox();

        setSetupStage(appStage, "Net Notes - Setup", "Setup...", bodyVBox);

        appStage.show();

        if (!isJava) {

            Text getJavaTxt = new Text("> Java is required. Would you like to download? (Y/n):");
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
                            downloadJava(bodyVBox, progressBar);
                        } catch (Exception dlException) {
                            Alert a = new Alert(AlertType.NONE, dlException.toString(), ButtonType.OK);
                            a.show();
                        }
                    });
                } else {
                    if (keyCode == KeyCode.N || keyCode == KeyCode.ESCAPE) {
                        shutdownNow();
                    } else {
                        getJavaField.setText("");
                    }
                }
            }
            );
        } else {

        }

    }

    private static void setupJava(VBox bodyVBox) {
        bodyVBox.getChildren().clear();
        FxTimer.runLater(Duration.ofMillis(100), () -> runJavaSetup());
    }

    private static void runJavaSetup() {

        String cmdString = "cmd /c " + downloadsDir + "\\" + javaFileName;

        try {
            Process p = Runtime.getRuntime().exec(cmdString);

            int exitCode = p.waitFor();

            if (exitCode == 0) {

                Path javaFilePath = Paths.get(downloadsDir + "\\" + javaFileName);
                Files.deleteIfExists(javaFilePath);
                Main.launch();
                shutdownNow();
            } else {
                Alert a = new Alert(AlertType.NONE, "The installation did not complete.\n\nTry again?", ButtonType.YES, ButtonType.NO);
                a.showAndWait();

                if (a.getResult() == ButtonType.YES) {
                    runJavaSetup();
                } else {
                    shutdownNow();
                }

            }

        } catch (Exception e) {
            Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
            a.showAndWait();
            shutdownNow();
        }

    }

    private static void setupDownloadField(VBox bodyVBox, ProgressBar progressBar) {
        bodyVBox.getChildren().clear();

        Text downloadingTxt = new Text("> Downloading Java 19 (x64)...");
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

    private static void downloadJava(VBox bodyVBox, ProgressBar progressBar) throws Exception {

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {

                File file = new File(downloadsDir + "\\" + javaFileName);

                URI uri = URI.create(javaURL);

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
            setupJava(bodyVBox);
        });

        Thread t = new Thread(task);
        t.start();

    }

    public static void downloadComplete() {

        Alert a = new Alert(AlertType.NONE, "complete");
        a.showAndWait();

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
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Net Notes - Setup");
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

        Text caretTxt = new Text(">");
        caretTxt.setFill(txtColor);
        caretTxt.setFont(txtFont);

        TextField hiddenField = new TextField();
        hiddenField.setVisible(false);
        hiddenField.setFont(txtFont);
        hiddenField.setId("formField");

        HBox line2 = new HBox(caretTxt, hiddenField);
        line2.setAlignment(Pos.CENTER_LEFT);

        bodyVBox.getChildren().addAll(setupTxt, line2);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(topBar, imageBox, bodyVBox);

        Scene setupScene = new Scene(layoutVBox, 600, 425);
        setupScene.getStylesheets().add("/css/startWindow.css");

        appStage.setScene(setupScene);

    }

    private static void shutdownNow() {

        Platform.exit();
        System.exit(0);
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

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer);

        if (closeBtn != null) {
            closeBtn.setGraphic(closeImage);
            closeBtn.setPadding(new Insets(0, 5, 0, 3));
            closeBtn.setId("closeBtn");
            newTopBar.getChildren().add(closeBtn);
        }

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

    static class Delta {

        double x, y;
    }

}
