package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.satergo.WalletKey.Local;
import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoMarkets extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Get updates from suported markets.";
    public final static String SUMMARY = "";
    public final static String NAME = "Ergo Markets";
    public final static String NETWORK_ID = "ERGO_MARKETS";

    private File logFile = new File("netnotes-log.txt");

    private File m_dataFile = null;
    private File m_appDir = null;
    private Stage m_stage = null;

    private boolean m_tunnelOpen = false;

    public ErgoMarkets(NoteInterface noteInterface) {
        super(getAppIcon(), NAME, NETWORK_ID, noteInterface);
        setStageWidth(300);
        setStageHeight(500);
        setup(null);
        getLastUpdated().set(LocalDateTime.now());
    }

    public ErgoMarkets(JsonObject json, NoteInterface noteInterface) {
        super(getAppIcon(), NAME, NETWORK_ID, noteInterface);
        setup(json);
    }

    private void setup(JsonObject json) {
        File ergoNetworkDir = new File(getNetworksData().getAppData().getAppDir().getAbsolutePath() + "/" + "Ergo Network");

        JsonElement directoriesElement = json.get("directories");

        if (directoriesElement != null && directoriesElement.isJsonObject()) {
            JsonObject directoriesObject = directoriesElement.getAsJsonObject();
            if (directoriesObject != null) {
                JsonElement appDirElement = directoriesObject.get("app");
               

                m_appDir = appDirElement == null ? new File(ergoNetworkDir.getAbsolutePath() + "/" + NAME) : new File(appDirElement.getAsString());

            }
        } else {
            m_appDir = new File(ergoNetworkDir.getAbsolutePath() + "/" + NAME);
        }

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

            m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");
            String newMarketDataId = FriendlyId.createFriendlyId();

            ErgoMarketsList dataList = new ErgoMarketsList(getNetworksData().getAppData().appKeyProperty().get(), this);

            dataList.add(new MarketsData(newMarketDataId, KucoinExchange.NETWORK_ID, "ERG", "USDT", MarketsData.REALTIME, MarketsData.TICKER, dataList));
            dataList.defaultIdProperty().set(newMarketDataId);
            dataList.save();
            dataList = null;

        } else {
            m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");

        }

        getNetworksData().getAppData().appKeyProperty().addListener((obs, oldVal, newVal) -> {
            ErgoMarketsList dataList = new ErgoMarketsList(oldVal, this);
            dataList.save();
            dataList = null;
        });
    }

    @Override
    public void open() {
        showStage();
    }

    public static Image getAppIcon() {
        return new Image("/assets/ergoChart.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergoChart-30.png");
    }

    public File getDataFile() {
        return m_dataFile;
    }

    public void showStage() {
        if (m_stage == null) {
            String title = getName();

            ErgoMarketsList marketsList = new ErgoMarketsList(getNetworksData().getAppData().appKeyProperty().get(), this);
            double buttonHeight = 100;

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(title);

            Button closeBtn = new Button();

            Button maximizeButton = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maximizeButton, closeBtn, m_stage);
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuSpacer);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setId("bodyBox");

            HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setPadding(new Insets(0, 2, 5, 2));
            menuBarPadding.setId("bodyBox");

            Button addButton = new Button("Add");
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setPrefWidth(getStageWidth() / 2);
            addButton.setPrefHeight(buttonHeight);

            Button removeButton = new Button("Remove");
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));

            removeButton.setDisable(true);
            removeButton.setPrefWidth(getStageWidth() / 2);
            removeButton.setPrefHeight(buttonHeight);

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            VBox layoutBox = new VBox(titleBox, menuBar, scrollPane, menuBox);

            Scene mainScene = new Scene(layoutBox, getStageWidth(), getStageHeight());
            mainScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(mainScene);

            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            scrollPane.prefViewportWidthProperty().bind(mainScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(mainScene.heightProperty().subtract(140));
            scrollPane.setPadding(new Insets(5, 5, 5, 5));

            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(m_stage.getWidth());
            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);
            gridWidth.bind(m_stage.widthProperty().subtract(15));

            VBox gridBox = marketsList.getGridBox(gridWidth, scrollWidth);

            scrollPane.setContent(gridBox);

            ResizeHelper.addResizeListener(m_stage, 300, 300, rect.getWidth(), rect.getHeight());

            m_stage.setOnCloseRequest(e -> {
                marketsList.shutdown();
                m_stage = null;
            });

            closeBtn.setOnAction(closeEvent -> {
                marketsList.shutdown();
                m_stage.close();
                m_stage = null;
            });
            shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
                marketsList.shutdown();
                m_stage.close();
                m_stage = null;
            });

            gridBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();
                if (val > scrollPane.prefViewportHeightProperty().doubleValue()) {
                    scrollWidth.set(40);
                } else {
                    scrollWidth.set(0);
                }
            });

            addButton.prefWidthProperty().bind(m_stage.widthProperty().divide(2));
            removeButton.prefWidthProperty().bind(m_stage.widthProperty().divide(2));
            m_stage.show();
        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            m_stage.show();
            m_stage.toFront();
        }

    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getText(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");

        if (subjecElement != null) {
            switch (subjecElement.getAsString()) {
                case App.GET_DATA:
                    getData(onSucceeded, onFailed);
                    break;
            }
        }
        return false;
    }

    public void getData(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        ErgoMarketsList dataList = new ErgoMarketsList(getNetworksData().getAppData().appKeyProperty().get(), this);

        Utils.returnObject(dataList.getDataObject(), onSucceeded, onFailed);
    }
}
