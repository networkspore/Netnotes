package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import org.reactfx.util.FxTimer;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netnotes.IconButton.IconStyle;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class KucoinMarketItem {

    private File logFile;
    private String m_id;
    private String m_symbol;
    private String m_name;
    private String m_timeSpan = "30min";
    private KuCoinDataList m_dataList = null;
    private SimpleObjectProperty<KucoinTickerData> m_tickerDataProperty = new SimpleObjectProperty<>(null);
    private Stage m_stage = null;
    private SimpleBooleanProperty m_isFavorite = new SimpleBooleanProperty(false);
    private ChangeListener<JsonObject> m_socketMsgListener;

    private NoteInterface m_parentInterface;

    public KucoinMarketItem(NoteInterface parentInterface, String id, String symbol, String name, boolean favorite, KucoinTickerData tickerData, KuCoinDataList dataList) {
        m_parentInterface = parentInterface;
        m_id = id;
        m_symbol = symbol;
        m_name = name;
        m_dataList = dataList;
        m_tickerDataProperty.set(tickerData);
        m_isFavorite.set(favorite);

    }

    public ChangeListener<JsonObject> getSocketChangeListener() {
        return m_socketMsgListener;
    }

    public SimpleBooleanProperty isFavoriteProperty() {
        return m_isFavorite;
    }

    public String getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getTimeSpan() {
        return m_timeSpan;
    }

    public SimpleObjectProperty<KucoinTickerData> tickerDataProperty() {
        return m_tickerDataProperty;
    }

    public HBox getRowBox() {

        KucoinTickerData data = m_tickerDataProperty.get();

        Button favoriteBtn = new Button();
        favoriteBtn.setId("menuBtn");
        favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
        favoriteBtn.setOnAction(e -> {
            boolean newVal = !m_isFavorite.get();
            m_isFavorite.set(newVal);
            if (newVal) {
                m_dataList.addFavorite(m_symbol, true);
            } else {
                m_dataList.removeFavorite(m_symbol, true);
            }
        });

        m_isFavorite.addListener((obs, oldVal, newVal) -> {
            favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
        });
        Image buttonImage = data == null ? null : getButtonImage(data);

        Button rowButton = new IconButton() {
            @Override
            public void open() {
                showStage();
            }
        };
        rowButton.setContentDisplay(ContentDisplay.LEFT);
        rowButton.setAlignment(Pos.CENTER_LEFT);
        rowButton.setGraphic(buttonImage == null ? null : IconButton.getIconView(buttonImage, buttonImage.getWidth()));
        rowButton.setId("rowBtn");

        HBox rowBox = new HBox(favoriteBtn, rowButton);

        rowButton.prefWidthProperty().bind(rowBox.widthProperty().subtract(favoriteBtn.widthProperty()));

        rowBox.setAlignment(Pos.CENTER_LEFT);

        m_tickerDataProperty.addListener((obs, oldVal, newVal) -> {
            Image img = newVal == null ? null : getButtonImage(newVal);
            rowButton.setGraphic(img == null ? null : IconButton.getIconView(img, img.getWidth()));
        });

        return rowBox;
    }

    private Image getButtonImage(KucoinTickerData data) {
        if (data == null) {
            return null;
        }
        int height = 30;

        String symbolString = String.format("%-18s", data.getSymbol());
        String lastString = data.getLastString();

        boolean positive = data.getChangeRate() > 0;
        boolean neutral = data.getChangeRate() == 0;

        //    java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 15);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();

        int symbolWidth = fm.stringWidth(symbolString);
        int lastWidth = fm.stringWidth(lastString);
        int fontAscent = fm.getAscent();
        int fontHeight = fm.getHeight();
        int stringY = ((height - fontHeight) / 2) + fontAscent;
        int colPadding = 5;

        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        img = new BufferedImage(symbolWidth + colPadding + lastWidth, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // g2d.drawImage(unitImage, 0, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);
        g2d.setFont(font);
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(symbolString, 0, stringY);

        if (neutral) {

            g2d.setColor(App.NEUTRAL_COLOR);
        } else {
            g2d.setColor(positive ? App.POSITIVE_COLOR : App.NEGATIVE_COLOR);
        }

        g2d.drawString(lastString, symbolWidth + colPadding, stringY);

        g2d.dispose();

        return SwingFXUtils.toFXImage(img, null);
    }

    public void showStage() {
        if (m_stage == null) {
            logFile = new File("marketItem-" + m_symbol + ".txt");
            double sceneWidth = 800;
            double sceneHeight = 800;

            KucoinExchange exchange = m_dataList.getKucoinExchange();

            m_stage = new Stage();
            m_stage.getIcons().add(KucoinExchange.getAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(exchange.getName() + " - " + m_name + (m_tickerDataProperty.get() != null ? " - " + m_tickerDataProperty.get().getLastString() + "" : ""));

            Button maximizeBtn = new Button();
            Button closeBtn = new Button();
            Button fillRightBtn = new Button();

            HBox titleBox = App.createTopBar(KucoinExchange.getSmallAppIcon(), fillRightBtn, maximizeBtn, closeBtn, m_stage);

            Button favoriteBtn = new Button();
            favoriteBtn.setId("menuBtn");
            favoriteBtn.setContentDisplay(ContentDisplay.LEFT);
            favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
            favoriteBtn.setOnAction(e -> {
                boolean newVal = !m_isFavorite.get();
                m_isFavorite.set(newVal);
                if (newVal) {
                    m_dataList.addFavorite(m_symbol, true);
                } else {
                    m_dataList.removeFavorite(m_symbol, true);
                }
            });

            m_isFavorite.addListener((obs, oldVal, newVal) -> {
                favoriteBtn.setGraphic(IconButton.getIconView(new Image(m_isFavorite.get() ? "/assets/star-30.png" : "/assets/star-outline-30.png"), 30));
            });

            Text headingText = new Text(m_name);
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            Region headingSpacerL = new Region();

            HBox headingBox = new HBox(favoriteBtn, headingSpacerL, headingText);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 0, 10, 0));
            headingBox.setId("headingBox");

            headingSpacerL.prefWidthProperty().bind(headingBox.widthProperty().subtract(favoriteBtn.widthProperty()).subtract(headingText.layoutBoundsProperty().get().getWidth()).divide(2));

            TextArea informationTextArea = new TextArea();

            Label emissionLbl = new Label();
            TextField emissionAmountField = new TextField();

            ChartView chartView = new ChartView(sceneWidth - 100, sceneHeight - 200);

            HBox chartBox = new HBox();
            chartBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(chartBox, Priority.ALWAYS);

            ChangeListener<JsonObject> socketMsgListener = (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        Files.writeString(logFile.toPath(), "\nnewMsg" + newVal.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e1) {

                    }
                    JsonElement subjectElement = newVal.get("subject");

                    String subject = subjectElement != null && subjectElement.isJsonPrimitive() ? subjectElement.getAsString() : null;

                    if (subject != null) {
                        switch (subject) {
                            case "message":

                                break;
                        }
                    }
                }
            };

            MessageInterface msgInterface = new MessageInterface() {
                @Override
                public String getId() {
                    return getId();
                }

                @Override
                public ChangeListener<JsonObject> getSocketChangeListener() {
                    return socketMsgListener;
                }

            };

            exchange.addMsgListenr(msgInterface);

            exchange.getCandlesDataset(m_symbol, m_timeSpan, onSuccess -> {
                WorkerStateEvent worker = onSuccess;
                Object sourceObject = worker.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof JsonObject) {

                    JsonObject sourceJson = (JsonObject) sourceObject;

                    JsonElement msgElement = sourceJson.get("msg");
                    JsonElement dataElement = sourceJson.get("data");

                    if (msgElement != null && msgElement.isJsonPrimitive()) {
                        chartView.setMsg(msgElement.toString());
                    } else {
                        if (dataElement != null && dataElement.isJsonArray()) {

                            chartView.setPriceDataList(dataElement.getAsJsonArray());
                            if (exchange.isClientReady()) {
                                Platform.runLater(() -> exchange.subscribeToCandles(m_parentInterface.getNetworkId(), m_symbol, m_timeSpan));
                            } else {
                                FxTimer.runLater(Duration.ofMillis(1000), () -> {
                                    if (exchange.isClientReady()) {
                                        Platform.runLater(() -> exchange.subscribeToCandles(m_parentInterface.getNetworkId(), m_symbol, m_timeSpan));
                                    }
                                });
                            }
                            //    if (get) {

                            //    exchange.subscribeToCandles(m_symbol, m_timeSpan);
                            //   }
                        } else {

                        }

                    }

                }
            }, onFailed -> {

            });

            VBox paddingBox = new VBox(headingBox, chartBox);

            paddingBox.setPadding(new Insets(0, 5, 5, 5));

            VBox layoutBox = new VBox(titleBox, paddingBox);

            //   Stage appStage = m_kucoinExchange.getAppStage();
            //      appStage.setX(0);
            Rectangle rect = m_dataList.getNetworksData().getMaximumWindowBounds();

            Scene mainScene = new Scene(layoutBox, sceneWidth, sceneHeight);
            mainScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(mainScene);

            chartBox.getChildren().add(chartView.getChartBox(m_stage));

            ResizeHelper.addResizeListener(m_stage, 600, 800, rect.getWidth(), rect.getHeight());
            m_stage.show();

            ChangeListener<KucoinTickerData> tickerListener = (obs, oldVal, newVal) -> {
                if (newVal != null) {

                    m_stage.setTitle(exchange.getName() + " - " + m_name + (newVal != null ? " - " + newVal.getLastString() + "" : ""));

                } else {

                }
            };

            m_tickerDataProperty.addListener(tickerListener);

            Runnable closable = () -> {
                exchange.unsubscribeToCandles(m_id, m_symbol, m_timeSpan);
                m_tickerDataProperty.removeListener(tickerListener);
                exchange.removeMsgListener(msgInterface);
                m_stage.close();
                m_stage = null;
            };

            m_stage.setOnCloseRequest(e -> closable.run());

            closeBtn.setOnAction(e -> closable.run());

            fillRightBtn.setOnAction(e -> {

                if (exchange.getAppStage() != null) {
                    Stage exStage = exchange.getAppStage();

                    exchange.cmdObjectProperty().set(Utils.getCmdObject("MAXIMIZE_STAGE_LEFT"));

                    m_stage.setX(exStage.getWidth());
                    m_stage.setY(0);
                    m_stage.setWidth(rect.getWidth() - exStage.getWidth());
                    m_stage.setHeight(rect.getHeight());
                }

            });

        } else {
            m_stage.show();
        }
    }

    public String getSymbol() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSymbol() : m_symbol;
    }

    public String getSymbolName() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSymbolName() : m_name;
    }

    public double getBuy() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getBuy() : Double.NaN;
    }

    public double getSell() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSell() : Double.NaN;
    }

    public double getChangeRate() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getChangeRate() : Double.NaN;
    }

    public double getChangePrice() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getChangePrice() : Double.NaN;
    }

    public double getHigh() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getHigh() : Double.NaN;
    }

    public double getLow() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getLow() : Double.NaN;
    }

    public double getVol() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getVol() : Double.NaN;
    }

    public double getVolValue() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getVolValue() : Double.NaN;
    }

    public double getLast() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getLast() : Double.NaN;
    }

    public double getAveragePrice() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getAveragePrice() : Double.NaN;
    }

    public double getTakerFeeRate() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getTakerFeeRate() : Double.NaN;
    }

    public double getMakerFeeRate() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getMakerFeeRate() : Double.NaN;
    }

    public double getTakerCoefficient() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getTakerCoefficient() : Double.NaN;
    }

    public double getMakerCoefficient() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getMakerCoefficient() : Double.NaN;
    }
}
