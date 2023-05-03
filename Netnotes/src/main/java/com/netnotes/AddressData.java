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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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

public class AddressData {

    public String m_mainnetExplorerUrl = Network.ErgoMainnet_EXPLORER_URL;
    public String m_testnetExplorerUrl = Network.ErgoTestnet_EXPLORER_URL;

    public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<>();

    private boolean m_valid = false;
    private String m_name = "";
    private int m_index;
    private Address m_address;
    private NetworkType m_networkType;

    private long m_confirmedNanoErgs = 0;
    private long m_unconfirmedNanoErgs = 0;

    private String m_priceBaseCurrency = "USD";
    private String m_priceTargetCurrency = "ERG";

    private PriceChart m_priceChart;

    private ArrayList<TokenData> m_confirmedTokensList = new ArrayList<>();
    private ArrayList<TokenData> m_unconfirmedTokensList = new ArrayList<>();

    private ArrayList<UrlMenuItem> m_urlMenuItems = new ArrayList<>();

    File logFile = new File("log.txt");

    public AddressData(String name, int index, Address address, PriceChart priceChart, NetworkType networktype) {

        m_name = name;
        m_index = index;
        m_networkType = networktype;
        m_address = address;
        m_apiIndex = 0;

        m_urlMenuItems.add(new UrlMenuItem("Kucoin", "ERG-USDT", "https://api.kucoin.com", "ERG", "USDT", 0));
        m_urlMenuItems.add(new UrlMenuItem("Kucoin", "ERG-BTC", "https://api.kucoin.com", "ERG", "BTC", 1));
        m_priceChart = priceChart;
        m_priceChart.lastUpdated.addListener(e -> {
            updateBalance();
        });

        updateBalance();

        Timer timer = new Timer("updateClock:" + getName(), true);

        TimerTask updateTask = new TimerTask() {

            @Override
            public void run() {
                updateBalance();

            }
        };
        timer.schedule(updateTask, 0, 5000);
    }

    public PriceChart getPriceChart() {
        return m_priceChart;
    }

    public String getMainnetExplorerUrlString() {
        return m_mainnetExplorerUrl;
    }

    public void setMainnetExplorerUrlString(String url) {
        m_mainnetExplorerUrl = url;
    }

    public String getTestnetExplorerUrlString() {
        return m_testnetExplorerUrl;
    }

    public void setTestnetExplorerUrlString(String url) {
        m_testnetExplorerUrl = url;
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
    private BufferedImage m_image = null;

    public double getFullAmountDouble() {
        return (double) getConfirmedNanoErgs() / 1000000000;
    }

    public double getFullAmountUnconfirmed() {
        return (double) getUnconfirmedNanoErgs() / 1000000000;
    }

    public double getPrice() {
        return m_priceChart.getCurrentPrice();
    }

    public boolean getPriceValid() {
        return m_priceChart.getValid();
    }

    public double getTotalAmountPrice() {
        return getFullAmountDouble() * getPrice();
    }

    public String getLastUpdatedString() {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a");

        return formater.format(lastUpdated.get());
    }

    public String getTotalAmountPriceString() {
        String priceTotal = (getPriceValid() ? String.format("%.2f", getTotalAmountPrice()) : "-.--");

        switch (m_priceTargetCurrency) {
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal = "₿" + (getPriceValid() ? String.format("%.8f", getTotalAmountPrice()) : "-.--");
                break;
        }

        return priceTotal;
    }

    public String getPriceString() {
        String price = (getPriceValid() ? String.format("%.3f", getPrice()) : "-.--");
        switch (m_priceTargetCurrency) {
            case "USD":
                price = "$" + price;
                break;
            case "USDT":
                price = price + " USDT";
                break;
            case "EUR":
                price = "€‎" + price;// priceTotal;
                break;
            case "BTC":
                price = "₿" + (getPriceValid() ? String.format("%.8f", getPrice()) : "-.--");
                break;
        }
        return price;
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

    public BufferedImage getBufferedImage() {
        if (m_image == null) {
            updateBufferedImage();
        }
        return m_image;
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
        m_image = img;

        lastUpdated.set(LocalDateTime.now());
    }

    public void updateBalance() {

        String urlString = "/api/v1/addresses/" + m_address + "/balance/total";
        Utils.getUrlData((m_networkType == NetworkType.MAINNET ? m_mainnetExplorerUrl : m_testnetExplorerUrl) + urlString,
                e -> {

                    ByteArrayOutputStream outputStream = (ByteArrayOutputStream) e.getSource().getValue();
                    String jsonString = outputStream.toString();

                    JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
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

                        m_confirmedTokensList = cTokensList;
                        m_unconfirmedTokensList = uTokensList;

                        m_valid = true;

                    }

                    updateBufferedImage();

                },
                e -> {

                    m_valid = false;
                    updateBufferedImage();
                },
                null
        );

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

    public void setApiIndex(int index) {
        m_apiIndex = index;

    }

    public String getCurrentPriceApiUrl() {
        return m_urlMenuItems.get(m_apiIndex).getUrlString();
    }

    public ArrayList<UrlMenuItem> getUrlMenuItems() {
        return m_urlMenuItems;
    }

    public String getPriceBaseCurrency() {
        return m_priceBaseCurrency;
    }

    public String getPriceTargetCurrency() {
        return m_priceTargetCurrency;
    }

}
