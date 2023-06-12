package com.netnotes;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.NavigableMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.netnotes.Network.NetworkID;
import com.satergo.WalletKey.Local;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

public class AddressData extends IconButton {

    private static String NULL_ERG = "-.-- ERG";

    private SimpleStringProperty m_lastUpdated = new SimpleStringProperty(Utils.formatDateTimeString(LocalDateTime.now()));
    private boolean m_valid = false;
    private boolean m_quantityValid = false;
    private SimpleStringProperty m_name = new SimpleStringProperty();
    private int m_index;
    private Address m_address;

    private SimpleStringProperty m_formattedQuantity = new SimpleStringProperty();
    private SimpleStringProperty m_formattedPrice = new SimpleStringProperty();
    private SimpleStringProperty m_formattedTotal = new SimpleStringProperty();

    private long m_confirmedNanoErgs = 0;
    private long m_unconfirmedNanoErgs = 0;

    private String m_priceBaseCurrency = "ERG";
    private String m_priceTargetCurrency = "USDT";

    private ArrayList<TokenData> m_confirmedTokensList = new ArrayList<>();
    private ArrayList<TokenData> m_unconfirmedTokensList = new ArrayList<>();
    private Stage m_addressStage = null;
    private WalletData m_walletData;
    File logFile;

    private double m_price = 0;
    // private WalletData m_WalletData;

    public AddressData(String name, int index, Address address, NetworkType networktype, WalletData walletData) {
        super();

        logFile = new File("address - " + address.toString() + "-log.txt");
        //    m_WalletData = walletData;
        m_walletData = walletData;
        m_name.set(name);
        m_index = index;
        m_address = address;

        Tooltip addressTip = new Tooltip(getName());

        //  HBox.setHgrow(this, Priority.ALWAYS);
        setPrefHeight(40);
        // setPrefWidth(width);
        // setImageWidth(150);

        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);

        setTooltip(addressTip);
        setPadding(new Insets(0, 10, 0, 10));
        setId("rowBtn");

        textProperty().bind(Bindings.concat("> ", m_name, ":\n  ", getAddressMinimal(12)));

        update();
        getUpdates();
    }

    private void getUpdates() {
        updateBalance();
    }

    private void update() {
        setFormattedQuantity();
        setFormattedPrice();
        setFormattedTotal();
        setLastUpdatedStringNow();

        updateBufferedImage();

        //  double remainingSpace = getBoundsInParent().getWidth();
        //  int factor = (int) (remainingSpace / 24);
        //  String addressMinimal = factor < 10 ? getAddressMinimal(10) : getAddressMinimal(factor);
        //  setText("> " + m_name.get() + ":\n  " + addressMinimal);
        //  explorerInterface.sendNote, null, null)
    }

    public String getQuantityString() {
        if (m_quantityValid) {
            double unconfirmed = getFullAmountUnconfirmed();
            return getFullAmountDouble() + " ERG" + (unconfirmed != 0 ? (" (" + unconfirmed + " unconfirmed)") : "");
        } else {
            return NULL_ERG;
        }
    }

    public String getFormmatedTotal() {
        if (m_quantityValid) {
            double unconfirmedTotal = getFullAmountUnconfirmed();
            return getTotalAmountPriceString() + (unconfirmedTotal != 0 ? (" (" + (unconfirmedTotal * getPrice()) + " unconfirmed)") : "");
        } else {
            return "-.--";
        }
    }

    public void setFormattedQuantity() {

        m_formattedQuantity.set(getQuantityString());
    }

    public void setFormattedPrice() {
        m_formattedPrice.set(getPriceString());
    }

    public void setFormattedTotal() {
        m_formattedTotal.set(getFormmatedTotal());
    }

    public SimpleStringProperty getLastUpdated() {
        return m_lastUpdated;
    }

    @Override
    public void open() {
        showAddressStage();

    }

    /* public void updateBalance(long nanoErgs, ArrayList<TokenData> confirmedTokenList, long unconfirmedNanoErgs, ArrayList<TokenData> unconfirmedTokenList) {
        m_valid = true;
        m_confirmedNanoErgs = nanoErgs;
        m_unconfirmedNanoErgs = unconfirmedNanoErgs;
        m_confirmedTokensList = confirmedTokenList;
        m_unconfirmedTokensList = unconfirmedTokenList;
        lastUpdated.set(LocalDateTime.now());
    } */
    private void showAddressStage() {
        if (m_addressStage == null) {
            // String title = "Ergo Wallet - " + m_name.get();
            String infoString = ": " + getAddressMinimal(16) + " - (";

            m_addressStage = new Stage();
            m_addressStage.titleProperty().bind(Bindings.concat("Ergo Wallet - ", m_name, infoString, m_address.getNetworkType().toString(), ")"));
            // m_addressStage.setTitle(title);
            m_addressStage.getIcons().add(ErgoWallet.getAppIcon());
            m_addressStage.setResizable(false);
            m_addressStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_addressStage.close();
                m_addressStage = null;
            });

            Label titleLbl = new Label();

            HBox titleBox = App.createLabeledTopBar(ErgoWallet.getSmallAppIcon(), titleLbl, closeBtn, m_addressStage);
            titleLbl.textProperty().bind(Bindings.concat("Ergo Wallet - ", m_name, " (", m_address.getNetworkType().toString(), "): ", getAddressMinimal(10)));

            HBox menuBar = new HBox();
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(5, 5, 5, 5));

            HBox paddingBox = new HBox(menuBar);
            paddingBox.setPadding(new Insets(2, 5, 2, 5));

            TextField addressNameTxt = new TextField();
            addressNameTxt.setId("textField");
            addressNameTxt.setFont(App.txtFont);
            addressNameTxt.setEditable(false);
            addressNameTxt.textProperty().bind(Bindings.concat("> ", m_name, " (", m_address.getNetworkType().toString(), "):"));
            HBox.setHgrow(addressNameTxt, Priority.ALWAYS);

            HBox addressNameBox = new HBox(addressNameTxt);
            addressNameBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(addressNameBox, Priority.ALWAYS);

            Text addressTxt = new Text("   Address:");
            addressTxt.setFont(App.txtFont);
            addressTxt.setFill(App.formFieldColor);

            TextField addressField = new TextField(getAddressString());
            addressField.setEditable(false);

            addressField.setFont(App.txtFont);
            addressField.setId("formField");
            HBox.setHgrow(addressField, Priority.ALWAYS);

            HBox addressBox = new HBox(addressTxt, addressField);
            addressBox.setAlignment(Pos.CENTER_LEFT);

            Text ergQuantityTxt = new Text("   Balance:");
            ergQuantityTxt.setFont(App.txtFont);
            ergQuantityTxt.setFill(App.formFieldColor);

            /*
            try {
                Files.writeString(logFile.toPath(), "erg quantity:" + ergQuantityString + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            } catch (IOException e) {

            } */
            TextField ergQuantityField = new TextField();
            ergQuantityField.setEditable(false);
            ergQuantityField.setFont(App.txtFont);
            ergQuantityField.setId("formField");
            HBox.setHgrow(ergQuantityField, Priority.ALWAYS);

            HBox ergQuantityBox = new HBox(ergQuantityTxt, ergQuantityField);
            ergQuantityBox.setAlignment(Pos.CENTER_LEFT);

            Text priceTxt = new Text("   Price: ");
            priceTxt.setFont(App.txtFont);
            priceTxt.setFill(App.formFieldColor);

            TextField priceField = new TextField();
            priceField.setEditable(false);
            /*
            try {
                Files.writeString(logFile.toPath(), "price :" + priceString + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            } catch (IOException e) {

            } */

            priceField.setFont(App.txtFont);
            priceField.setId("formField");
            HBox.setHgrow(priceField, Priority.ALWAYS);

            HBox priceBox = new HBox(priceTxt, priceField);
            priceBox.setAlignment(Pos.CENTER_LEFT);

            Text balanceTxt = new Text("   Total:");
            balanceTxt.setFont(App.txtFont);
            balanceTxt.setFill(App.formFieldColor);

            TextField balanceField = new TextField();
            balanceField.setEditable(false);
            balanceField.setFont(App.txtFont);
            balanceField.setId("formField");

            HBox.setHgrow(balanceField, Priority.ALWAYS);

            HBox balanceBox = new HBox(balanceTxt, balanceField);
            balanceBox.setAlignment(Pos.CENTER_LEFT);

            Text lastUpdatedTxt = new Text("   Updated:");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(App.txtFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(App.txtFont);
            HBox.setHgrow(lastUpdatedField, Priority.ALWAYS);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_LEFT);

            VBox bodyVBox = new VBox(addressNameBox, addressBox, ergQuantityBox, priceBox, balanceBox, lastUpdatedBox);
            bodyVBox.setPadding(new Insets(0, 20, 0, 20));
            VBox layoutVBox = new VBox(titleBox, paddingBox, bodyVBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene addressScene = new Scene(layoutVBox, 650, 500);

            addressScene.getStylesheets().add("/css/startWindow.css");

            m_addressStage.setScene(addressScene);
            m_addressStage.show();

            /**
             * **** BINDINGS ****
             */
            ergQuantityField.textProperty().bind(m_formattedQuantity);
            lastUpdatedField.textProperty().bind(m_lastUpdated);
            priceField.textProperty().bind(m_formattedPrice);
            balanceField.textProperty().bind(m_formattedTotal);

            /* 
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
            });*/
        } else {
            m_addressStage.show();
        }
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {
        return false;
    }

    @Override
    public String getName() {
        return m_name.get();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        m_name.set(name);
    }

    public int getIndex() {
        return m_index;
    }

    public boolean getValid() {
        return m_valid;
    }

    public void setValid(boolean valid) {
        m_valid = valid;
        // updateBufferedImage();
    }

    public Address getAddress() {
        return m_address;
    }

    public String getAddressString() {
        return m_address.toString();
    }

    public String getAddressMinimal(int show) {
        String adr = m_address.toString();
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    public BigDecimal getConfirmedAmount() {
        return ErgoInterface.toFullErg(m_confirmedNanoErgs);
    }

    public NetworkType getNetworkType() {
        return m_address.getNetworkType();
    }

    public long getConfirmedNanoErgs() {
        return m_confirmedNanoErgs;
    }

    public long getUnconfirmedNanoErgs() {
        return m_unconfirmedNanoErgs;
    }

    public ArrayList<TokenData> getConfirmedTokenList() {
        return m_confirmedTokensList;
    }

    public ArrayList<TokenData> getUnconfirmedTokenList() {
        return m_unconfirmedTokensList;
    }

    public double getFullAmountDouble() {
        return (double) getConfirmedNanoErgs() / 1000000000;
    }

    public double getFullAmountUnconfirmed() {
        return (double) getUnconfirmedNanoErgs() / 1000000000;
    }

    public double getPrice() {
        return m_price;
    }

    public double getTotalAmountPrice() {
        return getFullAmountDouble() * getPrice();
    }

    public void setLastUpdatedStringNow() {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a");

        m_lastUpdated.set(formater.format(now));
    }

    public String formatCryptoString(double price, String target) {
        String priceTotal = (m_valid && m_quantityValid ? String.format("%.2f", price) : "-.--");

        switch (target) {
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = "$" + priceTotal;
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal = "₿" + (m_valid && m_quantityValid ? String.format("%.8f", price) : "-.--");
                break;
        }

        return priceTotal;
    }

    public String getTotalAmountPriceString() {
        return formatCryptoString(getTotalAmountPrice(), getPriceTargetCurrency());

    }

    public String getPriceString() {

        return formatCryptoString(getPrice(), getPriceTargetCurrency());

    }

    public int getAmountInt() {
        return (int) getFullAmountDouble();
    }

    public double getAmountDecimalPosition() {
        return getFullAmountDouble() - getAmountInt();
    }

    public Image getUnitImage() {
        return new Image("/assets/unitErgo.png");
    }

    public void updateBufferedImage() {

        String totalPrice = getTotalAmountPriceString();
        int integers = getAmountInt();
        double decimals = getAmountDecimalPosition();
        int decimalPlaces = 9;
        String cryptoName = getPriceBaseCurrency();
        String currencyPrice = getPriceString();

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        String AmountString = m_quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = m_quantityValid ? decs.substring(1, decs.length()) : "";
        totalPrice = totalPrice + "   ";
        currencyPrice = "(" + currencyPrice + ")   ";

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 5;
        int stringWidth = fm.stringWidth(AmountString);
        int width = stringWidth;
        int height = fm.getHeight() + 10;

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();
        int priceWidth = fm.stringWidth(totalPrice);
        int currencyWidth = fm.stringWidth(currencyPrice);
        int priceLength = (priceWidth > currencyWidth ? priceWidth : currencyWidth);

        //  int priceAscent = fm.getAscent();
        width = priceLength + width + 58 + (padding * 2);

        int cryptoNameStringX = width - fm.stringWidth(cryptoName) - 3;

        g2d.dispose();

        BufferedImage unitImage = SwingFXUtils.fromFXImage(getUnitImage(), null);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        //   g2d.setComposite(AlphaComposite.Clear);

        /* for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(adrBuchImg.getRGB(x, y), true);

                Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 35);

                img.setRGB(x, y, c2.getRGB());
            }
        }
         */
        g2d.drawImage(unitImage, 0, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

        g2d.setFont(smallFont);
        g2d.setColor(java.awt.Color.WHITE);
        fm = g2d.getFontMetrics();
        g2d.drawString(totalPrice, 0, fm.getHeight() + 2);

        g2d.setColor(new java.awt.Color(.6f, .6f, .6f, .9f));
        g2d.drawString(currencyPrice, 0, height - 10);

        g2d.drawString(cryptoName, cryptoNameStringX, height - 10);

        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString(AmountString, priceLength, fm.getAscent() + 5);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

        g2d.drawString(decs, priceLength + stringWidth + 1, fm.getHeight() + 2);
        /*try {
            Files.writeString(logFile.toPath(), AmountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();
        /*
        try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        } */

        setGraphic(getIconView(SwingFXUtils.toFXImage(img, null), width));

    }

    private JsonObject getBalanceNote() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("subject", "GET_BALANCE");
        jsonObject.addProperty("address", m_address.toString());
        jsonObject.addProperty("networkType", getNetworkType().toString());
        return jsonObject;
    }

    public void updateBalance() {
        NoteInterface explorerInterface = m_walletData.getExplorerInterface();

        if (explorerInterface != null) {
            explorerInterface.sendNote(
                    getBalanceNote(),
                    success -> {
                        JsonObject jsonObject = null;
                        try {
                            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) success.getSource().getValue();
                            String jsonString = outputStream.toString();

                            JsonElement jsonTest = new JsonParser().parse(jsonString);
                            jsonObject = jsonTest != null ? jsonTest.getAsJsonObject() : null;

                        } catch (Exception e) {
                            try {
                                Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {

                            }
                        }

                        if (jsonObject != null) {

                            setBalance(jsonObject);
                        } else {

                            m_quantityValid = false;
                        }
                        update();
                    },
                    failed -> {
                        try {
                            Files.writeString(logFile.toPath(), "\nUpdateBalance: failed " + "\n" + failed.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }

                        m_quantityValid = false;
                        update();
                    }
            );
        }

    }

    public void setBalance(JsonObject jsonObject) {
        if (jsonObject != null) {
            JsonElement confirmedElement = jsonObject != null ? jsonObject.get("confirmed") : null;
            JsonElement unconfirmedElement = jsonObject.get("unconfirmed");
            if (confirmedElement != null && unconfirmedElement != null) {

                JsonObject confirmedObject = confirmedElement.getAsJsonObject();
                JsonObject unconfirmedObject = unconfirmedElement.getAsJsonObject();

                m_confirmedNanoErgs = confirmedObject.get("nanoErgs").getAsLong();

                m_unconfirmedNanoErgs = unconfirmedObject.get("nanoErgs").getAsLong();

                JsonArray confirmedTokenArray = confirmedObject.get("tokens").getAsJsonArray();
                JsonArray unconfirmedTokenArray = unconfirmedObject.get("tokens").getAsJsonArray();

                ArrayList<TokenData> cTokensList = new ArrayList<>();

                int confirmedSize = confirmedTokenArray.size();
                for (int i = 0; i < confirmedSize; i++) {
                    JsonObject token = confirmedTokenArray.get(i).getAsJsonObject();
                    TokenData data = new TokenData(token);
                    cTokensList.add(data);
                }

                ArrayList<TokenData> uTokensList = new ArrayList<>();
                int unconfirmedSize = unconfirmedTokenArray.size();

                for (int i = 0; i < unconfirmedSize; i++) {
                    JsonObject token = unconfirmedTokenArray.get(i).getAsJsonObject();
                    TokenData data = new TokenData(token);
                    uTokensList.add(data);

                }

                m_quantityValid = true;
                m_confirmedTokensList = cTokensList;
                m_unconfirmedTokensList = uTokensList;

            } else {
                m_quantityValid = false;

            }
        } else {
            m_quantityValid = false;

        }

    }

    public JsonArray getConfirmedTokenJsonArray() {
        JsonArray confirmedTokenArray = new JsonArray();
        m_confirmedTokensList.forEach(token -> {
            confirmedTokenArray.add(token.getJsonObject());
        });
        return confirmedTokenArray;
    }

    public JsonArray getUnconfirmedTokenJsonArray() {
        JsonArray unconfirmedTokenArray = new JsonArray();
        m_unconfirmedTokensList.forEach(token -> {
            unconfirmedTokenArray.add(token.getJsonObject());
        });
        return unconfirmedTokenArray;
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("id", m_address.toString());
        jsonObj.addProperty("tickerName", m_priceBaseCurrency);
        jsonObj.addProperty("name", m_name.get());
        jsonObj.addProperty("address", m_address.toString());
        jsonObj.addProperty("networkType", m_address.getNetworkType().toString());
        jsonObj.addProperty("explorerValidated", m_quantityValid);
        jsonObj.addProperty("marketValidated", m_valid);

        JsonObject formattedData = new JsonObject();
        formattedData.addProperty("quantity", m_formattedQuantity.get());
        formattedData.addProperty("price", m_formattedPrice.get());
        formattedData.addProperty("total", m_formattedTotal.get());

        JsonObject priceObj = new JsonObject();
        priceObj.addProperty("price", getPrice());
        priceObj.addProperty("quoteCurrency", getPriceTargetCurrency());
        priceObj.addProperty("total", getTotalAmountPrice());
        priceObj.add("formattedData", formattedData);

        jsonObj.add("priceData", priceObj);

        JsonObject confirmedObj = new JsonObject();
        confirmedObj.addProperty("nanoErgs", m_confirmedNanoErgs);
        confirmedObj.add("tokens", getConfirmedTokenJsonArray());

        JsonObject unconfirmedObj = new JsonObject();
        unconfirmedObj.addProperty("nanoErgs", m_unconfirmedNanoErgs);
        unconfirmedObj.add("tokens", getUnconfirmedTokenJsonArray());

        jsonObj.add("confirmed", confirmedObj);
        jsonObj.add("unconfirmed", unconfirmedObj);

        return jsonObj;

    }

    private int m_apiIndex = 0;

    public int getApiIndex() {
        return m_apiIndex;
    }

    public String getPriceBaseCurrency() {
        return m_priceBaseCurrency;
    }

    public String getPriceTargetCurrency() {
        return m_priceTargetCurrency;
    }

}
