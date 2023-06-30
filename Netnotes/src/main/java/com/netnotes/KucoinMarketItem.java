package com.netnotes;

import java.awt.Rectangle;

import com.netnotes.IconButton.IconStyle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class KucoinMarketItem {

    private String m_id;
    private String m_name;
    private KucoinExchange m_kucoinExchange = null;
    private SimpleObjectProperty<KucoinTickerData> m_tickerDataProperty = new SimpleObjectProperty<>(null);
    private Stage m_stage = null;

    public KucoinMarketItem(String id, String name, KucoinTickerData tickerData, KucoinExchange kucoinExchange) {
        m_id = id;
        m_name = name;
        m_kucoinExchange = kucoinExchange;
        m_tickerDataProperty.set(tickerData);

    }

    public String getId() {
        return m_id;
    }

    public SimpleObjectProperty<KucoinTickerData> tickerDataProperty() {
        return m_tickerDataProperty;
    }

    public HBox getRowBox() {

        KucoinTickerData data = m_tickerDataProperty.get();

        TextField symbolField = new TextField(m_id);
        symbolField.setFont(App.txtFont);
        symbolField.setId("rowField");
        symbolField.setEditable(false);

        TextField priceField = new TextField(data != null ? data.toString() : "-.--");
        priceField.setFont(App.txtFont);
        priceField.setId("rowField");
        priceField.setEditable(false);
        priceField.textProperty().bind(m_tickerDataProperty.asString());

        HBox rowBox = new HBox(symbolField, priceField);
        rowBox.setId("rowBtn");

        if (data != null) {
            double changePrice = data.getChangePrice();
            if (changePrice > 0) {
                priceField.setId("rowFieldGreen");
            } else {
                if (changePrice == 0) {
                    priceField.setId("rowField");
                } else {
                    priceField.setId("rowFieldRed");
                }
            }
        }

        m_tickerDataProperty.addListener((obs, oldVal, newVal) -> {
            if (data != null) {
                double changePrice = data.getChangePrice();
                if (changePrice > 0) {
                    priceField.setId("rowFieldGreen");
                } else {
                    if (changePrice == 0) {
                        priceField.setId("rowField");
                    } else {
                        priceField.setId("rowFieldRed");
                    }
                }
            }
        });

        symbolField.prefWidthProperty().bind(rowBox.widthProperty().divide(2));
        priceField.prefWidthProperty().bind(rowBox.widthProperty().divide(2));
        return rowBox;
    }

    public void showStage() {
        if (m_stage == null) {

            m_stage = new Stage();
            m_stage.getIcons().add(ErgoWallet.getSmallAppIcon());
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(m_kucoinExchange.getName() + " - " + m_name);
            m_stage.titleProperty().bind(Bindings.concat(m_kucoinExchange.getName(), " - ", m_name));

            Text promptText = new Text("");
            TextArea descriptionTextArea = new TextArea();

            Label emissionLbl = new Label();
            TextField emissionAmountField = new TextField();
            Button closeBtn = new Button();
            Button maximizeBtn = new Button();

            //    if (m_tickerDataProperty.get() != null) {
            //   } else {
            //   }
            ChangeListener<KucoinTickerData> tickerListener = (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    KucoinTickerData tickerData = newVal;

                } else {

                }
            };

            HBox titleBox = App.createTopBar(KucoinExchange.getSmallAppIcon(), maximizeBtn, closeBtn, m_stage);

            VBox layoutBox = new VBox(titleBox);

            //   Stage appStage = m_kucoinExchange.getAppStage();
            //      appStage.setX(0);
            Rectangle rect = m_kucoinExchange.getNetworksData().getMaximumWindowBounds();

            double sceneWidth = 800;
            double sceneHeight = 800;

            Scene mainScene = new Scene(layoutBox, sceneWidth, sceneHeight);

            m_stage.setScene(mainScene);

            ResizeHelper.addResizeListener(m_stage, 600, 800, rect.getWidth(), rect.getHeight());
            m_stage.show();
        } else {
            m_stage.show();
        }
    }

    public String getSymbol() {
        return m_tickerDataProperty.get() != null ? m_tickerDataProperty.get().getSymbol() : m_id;
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
