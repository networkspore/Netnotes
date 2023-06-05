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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
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

public class AddressData extends Network implements NoteInterface {

    public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<>();

    private boolean m_valid = false;
    private String m_name = "";
    private int m_index;
    private Address m_address;
    private NetworkType m_networkType;

    private long m_confirmedNanoErgs = 0;
    private long m_unconfirmedNanoErgs = 0;

    private String m_priceBaseCurrency = "ERG";
    private String m_priceTargetCurrency = "USDT";

    private ArrayList<TokenData> m_confirmedTokensList = new ArrayList<>();
    private ArrayList<TokenData> m_unconfirmedTokensList = new ArrayList<>();

    File logFile = new File("log.txt");

    private double m_price = 0;

    public AddressData(String name, int index, Address address, NetworkType networktype, NoteInterface noteInterface) {
        super(null, name, address.toString(), noteInterface);
        m_name = name;
        m_index = index;
        m_networkType = networktype;
        m_address = address;

        updateBalance();

        Tooltip addressTip = new Tooltip(getName());

        //  HBox.setHgrow(rowBtn, Priority.ALWAYS);
        setPrefHeight(40);
        // setPrefWidth(width);
        setAlignment(Pos.CENTER_LEFT);
        setContentDisplay(ContentDisplay.LEFT);
        setTooltip(addressTip);
        setPadding(new Insets(0, 20, 0, 20));
        setId("rowBtn");

    }

    @Override
    public void open() {
        showAddressStage();
    }

    private void showAddressStage() {

        String title = "Ergo Wallet - " + getName() + " (" + m_networkType + "): " + getAddressMinimal(16);

        Stage addressStage = new Stage();
        addressStage.setTitle(title);
        addressStage.getIcons().add(getIcon());
        addressStage.setResizable(false);
        addressStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeEvent -> {
            addressStage.close();
        });

        HBox titleBox = App.createTopBar(getIcon(), title, closeBtn, addressStage);

        ImageView addImage = App.highlightedImageView(App.addImg);
        addImage.setFitHeight(10);
        addImage.setPreserveRatio(true);

        Tooltip selectMarketTip = new Tooltip("Select Market");
        selectMarketTip.setShowDelay(new javafx.util.Duration(100));
        selectMarketTip.setFont(App.txtFont);
        ImageView arrow = App.highlightedImageView(new Image("/assets/navigate-outline-white-30.png"));
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
        locationUrlTip.setFont(App.txtFont);

        TextField locationUrlField = new TextField();
        locationUrlField.setId("urlField");

        locationUrlField.setEditable(false);
        locationUrlField.setTooltip(locationUrlTip);
        locationUrlField.setAlignment(Pos.CENTER_LEFT);
        locationUrlField.setFont(App.txtFont);
        /*
        ArrayList<UrlMenuItem> urlMenuList = new ArrayList<>();

        urlMenuList.forEach(item -> {
            item.setId("urlMenuItem");
            item.setOnAction(e -> {

                locationUrlField.setText("");
            });

            changeMarketButton.getItems().add(item);

        }); */

        HBox.setHgrow(locationUrlField, Priority.ALWAYS);

        HBox menuBar = new HBox(changeMarketButton, locationUrlField);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(5, 5, 5, 5));

        HBox paddingBox = new HBox(menuBar);
        paddingBox.setPadding(new Insets(2, 5, 2, 5));

        Text addressNameTxt = new Text("> Ergo wallet - " + getName() + " (" + m_networkType + "):");
        addressNameTxt.setFill(App.txtColor);
        addressNameTxt.setFont(App.txtFont);

        HBox addressNameBox = new HBox(addressNameTxt);
        addressNameBox.setPadding(new Insets(3, 0, 5, 0));

        Text addressTxt = new Text("  Address:");
        addressTxt.setFont(App.txtFont);
        addressTxt.setFill(App.formFieldColor);

        TextField addressField = new TextField(getAddressString());
        addressField.setEditable(false);

        addressField.setFont(App.txtFont);
        addressField.setId("formField");
        HBox.setHgrow(addressField, Priority.ALWAYS);

        HBox addressBox = new HBox(addressTxt, addressField);
        addressBox.setAlignment(Pos.CENTER_LEFT);

        Text ergQuantityTxt = new Text("  Balance:");
        ergQuantityTxt.setFont(App.txtFont);
        ergQuantityTxt.setFill(App.formFieldColor);
        double unconfirmed = getFullAmountUnconfirmed();
        String ergQuantityString = getFullAmountDouble() + " ERG" + (unconfirmed != 0 ? (" (" + unconfirmed + " unconfirmed)") : "");
        /*
        try {
            Files.writeString(logFile.toPath(), "erg quantity:" + ergQuantityString + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {

        } */

        TextField ergQuantityField = new TextField(ergQuantityString);
        ergQuantityField.setEditable(false);
        ergQuantityField.setFont(App.txtFont);
        ergQuantityField.setId("formField");
        HBox.setHgrow(ergQuantityField, Priority.ALWAYS);

        HBox ergQuantityBox = new HBox(ergQuantityTxt, ergQuantityField);
        ergQuantityBox.setAlignment(Pos.CENTER_LEFT);

        Text priceTxt = new Text("  Price: ");
        priceTxt.setFont(App.txtFont);
        priceTxt.setFill(App.formFieldColor);
        String priceString = getPriceString();

        TextField priceField = new TextField(priceString);
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

        Text balanceTxt = new Text("  Total:");
        balanceTxt.setFont(App.txtFont);
        balanceTxt.setFill(App.formFieldColor);
        String balanceString = getTotalAmountPriceString();
        TextField balanceField = new TextField(balanceString);
        balanceField.setEditable(false);
        /*
        try {
            Files.writeString(logFile.toPath(), "balance :" + balanceString + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {

        } */

        balanceField.setFont(App.txtFont);
        balanceField.setId("formField");
        HBox.setHgrow(balanceField, Priority.ALWAYS);

        HBox balanceBox = new HBox(balanceTxt, balanceField);
        balanceBox.setAlignment(Pos.CENTER_LEFT);

        Text lastUpdatedTxt = new Text("  Updated:");
        lastUpdatedTxt.setFill(App.formFieldColor);
        lastUpdatedTxt.setFont(App.txtFont);

        TextField lastUpdatedField = new TextField(getLastUpdatedString());
        lastUpdatedField.setEditable(false);
        lastUpdatedField.setId("formField");
        lastUpdatedField.setFont(App.txtFont);
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
        //chartButton.setGraphic(chartView);
        chartButton.setContentDisplay(ContentDisplay.BOTTOM);
        chartButton.setId("iconBtn");
        chartButton.setFont(App.txtFont);
        /*
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
        }); */
 /*
        chartButton.setOnMouseExited(e -> {
            chartView.setUserData(null);

            if (addressData.getPriceChart().getValid()) {
                chartView.setImage(SwingFXUtils.toFXImage(Utils.greyScaleImage(addressData.getPriceChart().zoomLatest(48)), null));
            } else {
                chartView.setImage(null);
                chartButton.setText("Price unavailable");
            }
        }); */

        HBox chartBox = new HBox(chartButton);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPadding(new Insets(5, 0, 20, 0));

        VBox bodyVBox = new VBox(chartBox, addressNameBox, addressBox, ergQuantityBox, priceBox, balanceBox, lastUpdatedBox);
        bodyVBox.setPadding(new Insets(0, 20, 0, 20));
        VBox layoutVBox = new VBox(titleBox, paddingBox, bodyVBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        try {
            Files.writeString(logFile.toPath(), "chartbox :" + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {

        }

        Scene addressScene = new Scene(layoutVBox, 650, 500);

        addressScene.getStylesheets().add("/css/startWindow.css");

        addressStage.setScene(addressScene);
        addressStage.show();

        lastUpdated.addListener(changed -> {

            double unconfirmedUpdate = getFullAmountUnconfirmed();
            ergQuantityField.setText(getFullAmountDouble() + " ERG" + (unconfirmedUpdate != 0 ? (" (" + unconfirmedUpdate + " unconfirmed)") : ""));
            double priceUpdate = getPrice();
            priceField.setText(getPriceString());
            balanceField.setText(getTotalAmountPriceString() + (unconfirmedUpdate != 0 ? (" (" + (unconfirmedUpdate * priceUpdate) + " unconfirmed)") : ""));
            lastUpdatedField.setText(getLastUpdatedString());

        });
        try {
            Files.writeString(logFile.toPath(), "end :" + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {

        }
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
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFailed) {
        return false;
    }

    private JsonObject getBalanceNote() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("subject", "GET_BALANCE");
        jsonObject.addProperty("address", m_address.toString());
        jsonObject.addProperty("networkType", m_networkType.toString());
        return jsonObject;
    }

    public void updateBalance() {

        getParentInterface().sendNote(getBalanceNote(),
                success -> {

                    ByteArrayOutputStream outputStream = (ByteArrayOutputStream) success.getSource().getValue();
                    String jsonString = outputStream.toString();

                    JsonElement jsonTest = null;
                    try {
                        jsonTest = new JsonParser().parse(jsonString);
                    } catch (JsonParseException e) {

                    }
                    JsonObject jsonObject = jsonTest != null ? jsonTest.getAsJsonObject() : null;
                    JsonElement confirmedElement = jsonObject != null ? jsonObject.get("confirmed") : null;

                    if (confirmedElement == null || jsonObject == null) {
                        m_valid = false;
                    } else {

                        JsonObject confirmedObject = confirmedElement.getAsJsonObject();
                        JsonObject unconfirmedObject = jsonObject.get("unconfirmed").getAsJsonObject();

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

                        m_confirmedTokensList = cTokensList;
                        m_unconfirmedTokensList = uTokensList;

                        m_valid = true;

                    }

                    updateBufferedImage();
                },
                failed -> {
                    m_valid = false;
                    updateBufferedImage();
                });

    }

    public String getName() {
        return m_name;
    }

    public int getIndex() {
        return m_index;
    }

    public boolean getValid() {
        return m_valid;
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
        return m_networkType;
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

    public boolean getPriceValid() {
        return false;
    }

    public double getTotalAmountPrice() {
        return getFullAmountDouble() * getPrice();
    }

    public String getLastUpdatedString() {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a");

        return formater.format(lastUpdated.get());
    }

    public static String formatCryptoString(double price, String target, boolean valid) {
        String priceTotal = (valid ? String.format("%.2f", price) : "-.--");

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
                priceTotal = "₿" + (valid ? String.format("%.8f", price) : "-.--");
                break;
        }

        return priceTotal;
    }

    public String getTotalAmountPriceString() {
        return formatCryptoString(getTotalAmountPrice(), getPriceTargetCurrency(), getPriceValid());

    }

    public String getPriceString() {

        return formatCryptoString(getPrice(), getPriceTargetCurrency(), getPriceValid());

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
        String AmountString = String.format("%d", integers);
        String decs = String.format("%." + decimalPlaces + "f", decimals);

        decs = decs.substring(1, decs.length());
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

        g2d.dispose();

        setIcon(SwingFXUtils.toFXImage(img, null));

    }

    public void setBalance(JsonObject jsonObject) {
        if (jsonObject != null) {
            JsonElement confirmedElement = jsonObject != null ? jsonObject.get("confirmed") : null;

            if (confirmedElement == null) {
                m_valid = false;
            } else {

                JsonObject confirmedObject = confirmedElement.getAsJsonObject();
                JsonObject unconfirmedObject = jsonObject.get("unconfirmed").getAsJsonObject();

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

                m_valid = true;
                m_confirmedTokensList = cTokensList;
                m_unconfirmedTokensList = uTokensList;
                updateBufferedImage();

            }
        } else {
            m_valid = false;
            updateBufferedImage();
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

        jsonObj.addProperty("valid", m_valid);

        if (m_valid) {
            jsonObj.addProperty("name", m_name);
            jsonObj.addProperty("address", m_address.toString());
            jsonObj.addProperty("networkType", m_networkType.toString());

            JsonObject confirmedObj = new JsonObject();
            confirmedObj.addProperty("nanoErgs", m_confirmedNanoErgs);
            confirmedObj.add("tokens", getConfirmedTokenJsonArray());

            JsonObject unconfirmedObj = new JsonObject();
            unconfirmedObj.addProperty("nanoErgs", m_unconfirmedNanoErgs);
            unconfirmedObj.add("tokens", getUnconfirmedTokenJsonArray());

            jsonObj.add("confirmed", confirmedObj);
            jsonObj.add("unconfirmed", unconfirmedObj);

        }

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
