package com.netnotes;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;

import java.util.ArrayList;

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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
            String titleString = getName() + " - " + m_address.toString() + " - (" + getNetworkType().toString() + ")";
            m_addressStage = new Stage();
            m_addressStage.getIcons().add(ErgoWallets.getAppIcon());
            m_addressStage.setResizable(false);
            m_addressStage.initStyle(StageStyle.UNDECORATED);
            m_addressStage.setTitle(titleString);

            double shrunkHeight = 106;

            Button closeBtn = new Button();

            addShutdownListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> closeBtn.fire());
            });



            HBox titleBox = App.createTopBar(ErgoWallets.getSmallAppIcon(), titleString, closeBtn, m_addressStage);
 

            TextField totalField = new TextField();
            totalField.setId("priceField");
            totalField.setEditable(false);
            HBox.setHgrow(totalField, Priority.ALWAYS);
            
            
            HBox summaryBox = new HBox(totalField);
            HBox.setHgrow(summaryBox, Priority.ALWAYS);
            summaryBox.setPadding(new Insets(0, 0, 0, 0));
            summaryBox.setAlignment(Pos.CENTER_LEFT);

            
            Text updatedTxt = new Text("Updated:");
            updatedTxt.setFill(App.altColor);
            updatedTxt.setFont(Font.font("OCR A Extended", 10));

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setPrefWidth(190);

            lastUpdatedField.setId("smallPrimaryColor");

            HBox updateBox = new HBox(updatedTxt, lastUpdatedField);
            updateBox.setPadding(new Insets(2,2,2,0));
            updateBox.setAlignment(Pos.CENTER_RIGHT);


            VBox layoutVBox = new VBox(titleBox, summaryBox, updateBox);
            
            Scene addressScene = new Scene(layoutVBox, 700, shrunkHeight);

            addressScene.getStylesheets().add("/css/startWindow.css");

            m_addressStage.setScene(addressScene);
            m_addressStage.show();
            m_addressStage.setAlwaysOnTop(true);
            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_addressStage, ErgoWalletData.MIN_WIDTH, shrunkHeight, rect.getWidth(), shrunkHeight);

            Runnable updateTotal = ()->{
                ErgoAmount ergoAmount = m_ergoAmountProperty.get();
                PriceQuote priceQuote = m_addressesData.currentPriceQuoteProperty().get(); 

                String totalString = ergoAmount == null ? "Σ-" : ergoAmount.toString();

                double ergoAmountDouble = (ergoAmount != null ? ergoAmount.getDoubleAmount() : 0);
                double totalPrice = priceQuote != null ? priceQuote.getDoubleAmount() * ergoAmountDouble : 0;
                String quoteString = (priceQuote != null ? ": " + Utils.formatCryptoString( totalPrice , priceQuote.getQuoteCurrency(),priceQuote.getFractionalPrecision(),  ergoAmount != null) +" (" + priceQuote.toString() + ")" : "" );

                String text = totalString  + quoteString;

                Platform.runLater(() -> totalField.setText(text));
                Platform.runLater(() -> lastUpdatedField.setText(Utils.formatDateTimeString(LocalDateTime.now())));
            };

            updateTotal.run();

            m_addressesData.currentPriceQuoteProperty().addListener((obs, oldval, newval)->updateTotal.run());
            m_ergoAmountProperty.addListener((obs, oldval, newval)->updateTotal.run());

            closeBtn.setOnAction(closeEvent -> {
                removeShutdownListener();

                m_addressStage.close();
                m_addressStage = null;
            });

            m_addressStage.setOnCloseRequest((closeRequest) -> {

                closeBtn.fire();
            });
         
        } else {
            if(m_addressStage.isIconified()){
                m_addressStage.setIconified(false);
                m_addressStage.show();
                m_addressStage.setAlwaysOnTop(true);
            }
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

    public SimpleObjectProperty<ErgoAmount> ergoAmountProperty(){
        return m_ergoAmountProperty;
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

        return getValid() ? m_addressesData.selectedMarketData().get().priceQuoteProperty().get().getDoubleAmount() : 0.0;
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

       /* try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

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
