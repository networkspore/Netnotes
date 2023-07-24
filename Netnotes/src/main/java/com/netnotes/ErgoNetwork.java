package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.scene.Scene;

import javafx.scene.control.Button;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;

import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.application.Platform;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ErgoNetwork extends Network implements NoteInterface {

    public final static String NAME = "Ergo Network";
    public final static String DESCRIPTION = "Ergo Network is the gateway to the ergo universe of apps, including the node, explorer, wallet and token manager on the Ergo Network";
    public final static String SUMMARY = "";
    public final static String NETWORK_ID = "ERGO_NETWORK";
    public final static PriceCurrency NATIVE_CURRENCY = new ErgoCurrency();

    public final static File ERGO_NETWORK_DIR = new File(System.getProperty("user.dir") + "/Ergo Network");

    private final static long EXECUTION_TIME = 500;
    private ScheduledFuture<?> m_lastExecution = null;

    private NetworkType m_networkType = NetworkType.MAINNET;

    private File logFile = new File("ergoNetwork-log.txt");

    //private SimpleBooleanProperty m_shuttingdown = new SimpleBooleanProperty(false);
    public ErgoNetwork(NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);
        setStageWidth(700);
        setStageHeight(500);

        getLastUpdated().set(LocalDateTime.now());
    }

    public ErgoNetwork(JsonObject json, NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);

        JsonElement networkTypeElement = json.get("networkType");
        JsonElement stageElement = json.get("stage");
        if (networkTypeElement != null && networkTypeElement.isJsonPrimitive()) {

            m_networkType = networkTypeElement.getAsString().equals(NetworkType.TESTNET.toString()) ? NetworkType.TESTNET : NetworkType.MAINNET;

        }
        if (stageElement != null && stageElement.isJsonObject()) {

            JsonObject stageObject = stageElement.getAsJsonObject();
            JsonElement stageWidthElement = stageObject.get("width");
            JsonElement stageHeightElement = stageObject.get("height");
            JsonElement iconStyleElement = stageObject.get("iconStyle");

            setStageIconStyle(iconStyleElement.getAsString());
            setStageWidth(stageWidthElement.getAsDouble());
            setStageHeight(stageHeightElement.getAsDouble());
        }

    }

    public static Image getAppIcon() {
        return App.ergoLogo;
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergo-network-30.png");
    }

    @Override
    public JsonObject getJsonObject() {

        JsonObject networkObj = super.getJsonObject();
        networkObj.addProperty("networkType", m_networkType.toString());
        networkObj.add("stage", getStageJson());
        return networkObj;

    }

    @Override
    public void open() {
        showStage();
    }
    private Stage m_stage = null;

    public void showStage() {
        if (m_stage == null) {
            double stageWidth = getStageWidth();
            double stageHeight = getStageHeight();

            ErgoNetworkData ergNetData = new ErgoNetworkData(getStageIconStyle(), stageWidth - 60, this);
            m_stage = new Stage();
            m_stage.setTitle("Ergo Network");
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            Button maximizeBtn = new Button();

            HBox titleBar = App.createTopBar(getSmallAppIcon(), maximizeBtn, closeBtn, m_stage);

            Tooltip manageTip = new Tooltip("Manage");
            manageTip.setShowDelay(new Duration(50));
            manageTip.setHideDelay(new Duration(200));

            BufferedButton menuButton = new BufferedButton("/assets/filter.png", 15);
            menuButton.setTooltip(manageTip);
            menuButton.setPadding(new Insets(5, 5, 5, 5));
            menuButton.setOnAction(e -> {
                ergNetData.showwManageStage();
            });

            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            Tooltip gridTypeToolTip = new Tooltip("Toggle: List view");
            gridTypeToolTip.setShowDelay(new Duration(50));
            gridTypeToolTip.setHideDelay(new Duration(200));

            BufferedButton toggleGridTypeButton = new BufferedButton("/assets/list-outline-white-25.png", 25);
            toggleGridTypeButton.setTooltip(gridTypeToolTip);
            toggleGridTypeButton.setPadding(new Insets(0, 0, 0, 0));

            HBox menuBar = new HBox(menuButton, menuSpacer, toggleGridTypeButton);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setPadding(SMALL_INSETS);
            menuBarPadding.setId("bodyBox");
            VBox headerBox = new VBox(titleBar, menuBarPadding);

            VBox gridBox = ergNetData.getGridBox();
            gridBox.setPadding(SMALL_INSETS);

            ScrollPane scrollPane = new ScrollPane(gridBox);

            VBox bodyBox = new VBox(scrollPane);
            bodyBox.setId("bodyBox");
            bodyBox.setPadding(SMALL_INSETS);

            VBox layoutBox = new VBox(headerBox, bodyBox);
            layoutBox.setPadding(new Insets(0, 2, 5, 2));
            Scene scene = new Scene(layoutBox, stageWidth, stageHeight);
            scene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(scene);

            scrollPane.prefViewportHeightProperty().bind(m_stage.heightProperty().subtract(headerBox.heightProperty()).subtract(20));
            scrollPane.prefViewportWidthProperty().bind(m_stage.widthProperty());
            ergNetData.gridWidthProperty().bind(m_stage.widthProperty().subtract(60));

            m_stage.show();
            Runnable setUpdated = () -> {
                getLastUpdated().set(LocalDateTime.now());

            };

            toggleGridTypeButton.setOnAction(e -> {

                if (getStageIconStyle().equals(IconStyle.ICON)) {
                    setStageIconStyle(IconStyle.ROW);
                    ergNetData.iconStyleProperty().set(IconStyle.ROW);
                } else {
                    setStageIconStyle(IconStyle.ICON);
                    ergNetData.iconStyleProperty().set(IconStyle.ICON);
                }
                setUpdated.run();
            });

            if (ergNetData.isEmpty()) {
                ergNetData.showwManageStage();
            }
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            m_stage.widthProperty().addListener((obs, oldVal, newVal) -> {
                setStageWidth(newVal.doubleValue());

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            m_stage.heightProperty().addListener((obs, oldVal, newVal) -> {
                setStageHeight(newVal.doubleValue());

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            ResizeHelper.addResizeListener(m_stage, 200, 200, rect.getWidth(), rect.getHeight());

            closeBtn.setOnAction(e -> {
                executor.shutdown();
                ergNetData.shutdown();
                m_stage.close();
                m_stage = null;

            });

            m_stage.setOnCloseRequest((closing) -> {
                executor.shutdown();
                ergNetData.shutdown();
                m_stage = null;
            });

            shutdownNowProperty().addListener((obs, oldVal, newVal) -> {

                closeBtn.fire();
            });
        } else {
            m_stage.show();
        }
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");
        JsonElement networkTypeElement = note.get("networkType");

        if (subjecElement != null) {
            String subject = subjecElement.getAsString();
            switch (subject) {
                default:
            }
        }

        return false;
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }

    /*
    public void showStage() {

     
        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {

            networkStage.close();
        });

        HBox titleBox = App.createTopBar(getIcon(), "Network", closeBtn, networkStage);

        Button imageButton = App.createImageButton(App.globeImg, "Network");
        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);

        Text networkTypeTxt = new Text("> Name:  Ergo");
        networkTypeTxt.setFill(App.txtColor);
        networkTypeTxt.setFont(App.txtFont);

        HBox networkTypeBox = new HBox(networkTypeTxt);
        networkTypeBox.setPadding(new Insets(3, 0, 5, 0));
        HBox.setHgrow(networkTypeBox, Priority.ALWAYS);

        Text applicationTxt = new Text("> Application:");
        applicationTxt.setFill(App.txtColor);
        applicationTxt.setFont(App.txtFont);

        Text addressTxt = new Text("URL:");
        addressTxt.setFont(App.txtFont);
        addressTxt.setFill(App.altColor);
        addressTxt.setId("textField");

        ImageView arrowRightImage = App.highlightedImageView(App.arrowRightImg);
        arrowRightImage.setFitHeight(15);
        arrowRightImage.setPreserveRatio(true);

        Button addressBtn = new Button();
        addressBtn.setGraphic(arrowRightImage);
        addressBtn.setPadding(new Insets(2, 15, 2, 15));
        addressBtn.setFont(App.txtFont);
        addressBtn.setVisible(false);

        TextField addressField = new TextField("Enter address or click manage...");
        addressField.setId("formField");
        HBox.setHgrow(addressField, Priority.ALWAYS);
        addressField.setOnKeyPressed(key -> {
            KeyCode keyCode = key.getCode();

            if (keyCode == KeyCode.ENTER) {
                String addressFieldText = addressField.getText();

                try {
                    setUrl(addressFieldText);
                    String currentHost = getHost();
                    if (currentHost == null) {

                        addressField.setText("Enter address or click manage...");
                    } else {
                        int currentPort = getPort();

                        addressField.setText(currentHost + ":" + currentPort);

                    }
                    addressBtn.setVisible(false);

                } catch (MalformedURLException e) {

                    setHost(null);

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
                        setUrl(addressFieldText);
                        String currentHost = getHost();
                        if (currentHost == null) {

                            addressField.setText("Enter address or click manage...");
                        } else {
                            int currentPort = getPort();

                            addressField.setText(currentHost + ":" + currentPort);

                        }
                        addressBtn.setVisible(false);

                    } catch (MalformedURLException e) {

                        setHost(null);

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
        applicationBtn.setGraphic(App.highlightedImageView(new Image("/assets/server-outline-white-20.png")));
        applicationBtn.setFont(App.txtFont);
        applicationBtn.setPadding(new Insets(2, 10, 2, 10));

        HBox manageBox = new HBox(applicationBtn);
        manageBox.setAlignment(Pos.CENTER_LEFT);
        manageBox.setPadding(new Insets(10, 0, 10, 20));

        HBox applicationBox = new HBox(applicationTxt);
        applicationBox.setAlignment(Pos.CENTER_LEFT);
        applicationBox.setPadding(new Insets(3, 0, 0, 0));

        Text walletTxt = new Text("> Wallet:");
        walletTxt.setFill(App.txtColor);
        walletTxt.setFont(App.txtFont);

        Region hBar = null;
        HBox gBox = null;
        HBox addBox = null;

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
        addBtn.setFont(App.txtFont);
        addBtn.setOnAction(openEvent -> {

        });

        addBox = new HBox(addBtn);
        addBox.setAlignment(Pos.CENTER);
        addBox.setPadding(new Insets(25, 0, 0, 0));

        VBox bodyBox = new VBox(imageBox, networkTypeBox, applicationBox, appLocationBox, manageBox, gBox, addBox);

        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);

        VBox networkVBox = new VBox(titleBox, bodyBox);
        HBox.setHgrow(networkVBox, Priority.ALWAYS);

        Scene networkScene = new Scene(networkVBox, 400, 525);
        networkScene.getStylesheets().add("/css/startWindow.css");
        networkStage.setScene(networkScene);

        networkStage.show();

    }*/
}
