package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;

import javax.imageio.ImageIO;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.ergo.ErgoInterface;
import com.utils.Utils;

import javafx.application.Platform;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

public class AddressData extends Network {

    private static String NULL_ERG = "-.-- ERG";

    private boolean m_quantityValid = false;

    private int m_index;
    private Address m_address;

    private SimpleObjectProperty<ErgoAmount> m_ergoAmountProperty = new SimpleObjectProperty<ErgoAmount>(null);

    private long m_unconfirmedNanoErgs = 0;

    private String m_priceBaseCurrency = "ERG";
    private String m_priceTargetCurrency = "USDT";

    private ArrayList<TokenData> m_confirmedTokensList = new ArrayList<>();
    private ArrayList<TokenData> m_unconfirmedTokensList = new ArrayList<>();
    private Stage m_addressStage = null;
    private File logFile = new File("netnotes-log.txt");
    private AddressesData m_addressesData;

    
    
    public AddressData(String name, int index, Address address, NetworkType networktype, AddressesData addressesData) {
        super(null, name, address.toString(), addressesData.getWalletData());
        

        m_addressesData = addressesData;
        m_index = index;
        m_address = address;

        Tooltip addressTip = new Tooltip(getName());
        addressTip.setShowDelay(new javafx.util.Duration(100));
        addressTip.setFont(App.txtFont);

        setTooltip(addressTip);
        setPadding(new Insets(0, 10, 0, 10));
        setId("rowBtn");
        setText(getButtonText());

        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);
        setTextAlignment(TextAlignment.LEFT);
        updateBalance();
        update();
        
       // getQuote();

        ChangeListener<? super PriceQuote> quoteChangeListener = (obs, oldVal, newVal) -> update();

        m_addressesData.selectedMarketData().addListener((obs, oldval, newVal) -> {
            if (oldval != null) {
                oldval.priceQuoteProperty().removeListener(quoteChangeListener);
                oldval.shutdown();
            }
            if (newVal != null) {
                newVal.priceQuoteProperty().addListener(quoteChangeListener);
                newVal.start();
            }
        });

        if (m_addressesData.selectedMarketData().get() != null) {
            m_addressesData.selectedMarketData().get().priceQuoteProperty().addListener(quoteChangeListener);
        }

        m_addressesData.timeCycleProperty().addListener((obs, oldval, newval)->{
            updateBalance();
        });
        
    }

    public void setQuote(PriceQuote oldVal, PriceQuote newVal){

    }

    public String getButtonText() {
        return "  " + getName() + "\n   " + getAddressString();
    }

    /*public boolean donate(){
        BigDecimal amountFullErg = dialog.showForResult().orElse(null);
		if (amountFullErg == null) return;
		try {
			Wallet wallet = Main.get().getWallet();
			UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(Utils.createErgoClient(),
					wallet.addressStream().toList(),
					DONATION_ADDRESS, ErgoInterface.toNanoErg(amountFullErg), Parameters.MinFee, Main.get().getWallet().publicAddress(0));
			String txId = wallet.transact(Utils.createErgoClient().execute(ctx -> {
				try {
					return wallet.key().sign(ctx, unsignedTx, wallet.myAddresses.keySet());
				} catch (WalletKey.Failure ex) {
					return null;
				}
			}));
			if (txId != null) Utils.textDialogWithCopy(Main.lang("transactionId"), txId);
		} catch (WalletKey.Failure ignored) {
			// user already informed
		}
    }*/
 /* return ;
        }); */
    public String getNodesId() {
        return "";
    }

    private void update() {

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

    @Override
    public void open() {
        super.open();
        showAddressStage();

    }

    private void showAddressStage() {
        if (m_addressStage == null) {

            m_addressStage = new Stage();
            // 
            m_addressStage.getIcons().add(ErgoWallets.getAppIcon());
            m_addressStage.setResizable(false);
            m_addressStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();

            addShutdownListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> closeBtn.fire());
            });

            Button maximizeBtn = new Button();

            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), maximizeBtn, closeBtn, m_addressStage);

            Tooltip sendTip = new Tooltip("Send");
            sendTip.setShowDelay(new javafx.util.Duration(100));
            sendTip.setFont(App.txtFont);

            m_addressStage.setTitle(getName() + " - " + getAddressMinimal(16) + " - (" + getNetworkType().toString() + ")");

            Button sendButton = new Button();
            sendButton.setGraphic(IconButton.getIconView(new Image("/assets/arrow-send-white-30.png"), 30));
            sendButton.setId("menuBtn");
            sendButton.setTooltip(sendTip);
            sendButton.setOnAction(e -> {

                cmdProperty().set(Utils.getCmdObject("SEND"));
                // ResizeHelper.addResizeListener(parentStage, WalletData.MIN_WIDTH, WalletData.MIN_HEIGHT, m_walletData.getMaxWidth(), m_walletData.getMaxHeight());
            });

            Tooltip nodeTip = new Tooltip("Ergo Nodes");
            nodeTip.setShowDelay(new javafx.util.Duration(100));
            nodeTip.setFont(App.txtFont);

            MenuButton nodeMenuBtn = new MenuButton();
            nodeMenuBtn.setPadding(new Insets(2, 0, 0, 0));
            nodeMenuBtn.setTooltip(nodeTip);

            ErgoExplorerData explorerData = null;

            Tooltip explorerTip = new Tooltip(explorerData == null ? "Explorer unavailable" : explorerData.getName());
            explorerTip.setShowDelay(new javafx.util.Duration(100));
            explorerTip.setFont(App.txtFont);

            MenuButton explorerBtn = new MenuButton();
            explorerBtn.setPadding(new Insets(2, 0, 0, 0));
            explorerBtn.setTooltip(explorerTip);

            Tooltip marketsTip = new Tooltip(m_addressesData.selectedMarketData().get() == null ? "Market unavailable" : ErgoMarketsData.getFriendlyUpdateTypeName(m_addressesData.selectedMarketData().get().getUpdateType()) + ": " + m_addressesData.selectedMarketData().get().getUpdateValue());
            marketsTip.setShowDelay(new javafx.util.Duration(100));
            marketsTip.setFont(App.txtFont);

            MenuButton marketsBtn = new MenuButton();
            marketsBtn.setGraphic(m_addressesData.selectedMarketData().get() == null ? IconButton.getIconView(new Image("/assets/exchange-30.png"), 30) : IconButton.getIconView(new InstallableIcon(m_addressesData.getWalletData().getNetworksData(), m_addressesData.selectedMarketData().get().getMarketId(), true).getIcon(), 30));
            explorerBtn.setPadding(new Insets(2, 0, 0, 0));
            explorerBtn.setTooltip(explorerTip);

            HBox rightSideMenu = new HBox(nodeMenuBtn, explorerBtn, marketsBtn);
            rightSideMenu.setId("rightSideMenuBar");
            rightSideMenu.setPadding(new Insets(0, 10, 0, 20));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox menuBar = new HBox(sendButton, spacer, rightSideMenu);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            HBox paddingBox = new HBox(menuBar);
            paddingBox.setPadding(new Insets(2, 5, 2, 5));

            //////////////////////////////////////////////////////////********************************** *///////////////////////////////////
            TextField addressField = new TextField(getAddressString());
            addressField.setId("formField");
            addressField.setFont(App.txtFont);
            addressField.setEditable(false);
            HBox.setHgrow(addressField, Priority.ALWAYS);
            addressField.setPadding(new Insets(0, 0, 0, 0));

            TextField nameField = new TextField(getName());
            nameField.setId("textField");
            nameField.setFont(App.txtFont);
            nameField.setEditable(false);
            nameField.setPadding(new Insets(5, 10, 5, 0));
            nameField.setPrefWidth(105);

            HBox nameBox = new HBox(nameField);
            nameBox.setPrefHeight(40);
            nameBox.setAlignment(Pos.TOP_RIGHT);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            ImageView imgView = new ImageView();
            imgView.setFitHeight(40);
            imgView.setPreserveRatio(true);
            imgView.setImage(m_imgBuffer.get());
            imgView.imageProperty().bind(m_imgBuffer);

            HBox headingBox = new HBox(imgView, addressField, nameBox);
            headingBox.setMinHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 15, 10, 15));
            headingBox.setId("headingBox");

            VBox bodyVBox = new VBox();// m_amountsList.getGridBox();
            bodyVBox.setPadding(new Insets(0, 20, 20, 20));
            HBox.setHgrow(bodyVBox, Priority.ALWAYS);
            bodyVBox.setId("bodyBox");

            ScrollPane scrollPane = new ScrollPane(bodyVBox);

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setPrefWidth(60);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);

            VBox bodyPaddingBox = new VBox(nameBox, scrollPane, footerVBox);
            HBox.setHgrow(bodyPaddingBox, Priority.ALWAYS);
            bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

            VBox layoutVBox = new VBox(titleBox, paddingBox, bodyPaddingBox);
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene addressScene = new Scene(layoutVBox, 650, 500);

            addressScene.getStylesheets().add("/css/startWindow.css");

            m_addressStage.setScene(addressScene);
            m_addressStage.show();

            scrollPane.prefViewportWidthProperty().bind(addressScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(addressScene.heightProperty().subtract(titleBox.heightProperty()).subtract(nameBox.heightProperty()).subtract(footerVBox.heightProperty()).subtract(10));

            //ergQuantityField.textProperty().bind(m_formattedQuantity);
            //  lastUpdatedField.textProperty().bind(Bindings.concat(getLastUpdated().asString("%1$TH:%1$TM:%1$TS")));
            //  priceField.textProperty().bind(m_formattedPrice);
            //  balanceField.textProperty().bind(m_formattedTotal);
            closeBtn.setOnAction(closeEvent -> {
                removeShutdownListener();

                m_addressStage.close();
                m_addressStage = null;
            });

            m_addressStage.setOnCloseRequest((closeRequest) -> {

                removeShutdownListener();
                m_addressStage = null;
            });
            //  

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

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        /* JsonElement subjectElement = note.get("subject");
        if (subjectElement != null) {
            String subject = subjectElement.getAsString();
            switch (subject) {
                case "GET_EXPLORER_BALANCE_UPDATE":
                    return updateBalance();

            }
        }*/
        return false;
    }

    public int getIndex() {
        return m_index;
    }

    public boolean getValid() {
        return m_addressesData.selectedMarketData().get() != null && m_addressesData.selectedMarketData().get().priceQuoteProperty().get() != null && (m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getTimeStamp() - System.currentTimeMillis() < 1000 * 60 * 2);
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
        return ErgoInterface.toFullErg(getConfirmedNanoErgs());
    }

    public NetworkType getNetworkType() {
        return m_address.getNetworkType();
    }

    public long getConfirmedNanoErgs() {
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        return ergoAmount == null ? 0 : ergoAmount.getLongAmount();
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

        return getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getAmount() : 0.0;
    }

    public double getTotalAmountPrice() {
        return getFullAmountDouble() * getPrice();
    }

    public String getTotalAmountPriceString() {
        return Utils.formatCryptoString(getTotalAmountPrice(), getPriceTargetCurrency(), getValid() && m_quantityValid);

    }

    public String getPriceString() {

        return Utils.formatCryptoString(getPrice(), getPriceTargetCurrency(), getValid());

    }

    public int getAmountInt() {
        return (int) getFullAmountDouble();
    }

    public double getAmountDecimalPosition() {
        return getFullAmountDouble() - getAmountInt();
    }

    public Image getUnitImage() {
        ErgoAmount ergoAmount = m_ergoAmountProperty.get();
        if (ergoAmount == null) {
            return new Image("/assets/unknown-unit.png");
        } else {
            return ergoAmount.getCurrency().getUnitImage();
        }
    }

    private SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);

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

        try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }

        setImageBuffer(SwingFXUtils.toFXImage(img, null));

    }

    public SimpleObjectProperty<Image> getImageProperty() {
        return m_imgBuffer;
    }

    private void setImageBuffer(Image image) {
        m_imgBuffer.set(image == null ? null : image);

        Platform.runLater(() -> setGraphic(m_imgBuffer.get() == null ? null : getIconView(m_imgBuffer.get(), m_imgBuffer.get().getWidth())));
        Platform.runLater(() -> getLastUpdated().set(LocalDateTime.now()));
    }

    

    public void updateBalance() {

       ErgoExplorerData explorerData =  m_addressesData.selectedExplorerData().get();
   
        if (explorerData != null) {
            
                    explorerData.getBalance(m_address.toString(),
                    success -> {
                        Object sourceObject = success.getSource().getValue();

                        if (sourceObject != null) {
                            JsonObject jsonObject = (JsonObject) sourceObject;
                       
                            setBalance(jsonObject);
                            update();
                        } else {
                            m_quantityValid = false;
                        }
                    },
                    failed -> {
                          try {
                                Files.writeString(logFile.toPath(), "\nAddressData, Explorer failed update: " + failed.getSource().getException().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

            try {
                Utils.writeString(logFile.toPath(), jsonObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {

            }

            JsonElement confirmedElement = jsonObject != null ? jsonObject.get("confirmed") : null;
            JsonElement unconfirmedElement = jsonObject.get("unconfirmed");
            if (confirmedElement != null && unconfirmedElement != null) {

                JsonObject confirmedObject = confirmedElement.getAsJsonObject();
                JsonObject unconfirmedObject = unconfirmedElement.getAsJsonObject();

                JsonElement nanoErgElement = confirmedObject.get("nanoErgs");

                if (nanoErgElement != null && nanoErgElement.isJsonPrimitive()) {
                    m_ergoAmountProperty.set(new ErgoAmount(nanoErgElement.getAsLong()));
                }

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
        jsonObj.addProperty("name", getName());
        jsonObj.addProperty("address", m_address.toString());
        jsonObj.addProperty("networkType", m_address.getNetworkType().toString());
        jsonObj.addProperty("explorerValidated", m_quantityValid);
        jsonObj.addProperty("marketValidated", getValid());

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

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return getText();
    }
}
