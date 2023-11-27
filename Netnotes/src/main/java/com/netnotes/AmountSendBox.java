package com.netnotes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;

import java.text.DecimalFormat;


import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AmountSendBox extends AmountBox {

     private SimpleObjectProperty<PriceAmount> m_balanceAmount = new SimpleObjectProperty<>();
     private SimpleObjectProperty<Image> m_balanceAmountImage = new SimpleObjectProperty<>();

    public AmountSendBox(PriceAmount priceAmount, Scene scene, boolean editable) {
        super();
        setId("darkBox");
        setMinHeight(40);
        priceAmountProperty().set(priceAmount);
        setAlignment(Pos.CENTER_LEFT);
        
        Button amountBtn = new Button();
        amountBtn.setId("amountBtn");
       // amountBtn.textProperty().bind(m_currentAmount.asString());
        amountBtn.setContentDisplay(ContentDisplay.LEFT);
        amountBtn.setAlignment(Pos.CENTER_LEFT);
        amountBtn.setPadding(new Insets(2, 5, 2, 10));
        amountBtn.setGraphicTextGap(25);
        imageBufferProperty().addListener((obs,oldval,newval)-> {
            if(newval != null){    
                amountBtn.setGraphic(IconButton.getIconView(newval, newval.getWidth()));
            }
        });
        
        HBox amountsBox = new HBox(amountBtn);
        amountsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(amountsBox,Priority.ALWAYS);

 

        Button balanceAmountBtn = new Button();
        balanceAmountBtn.setId("rowBox");
        balanceAmountBtn.setContentDisplay(ContentDisplay.RIGHT);
        balanceAmountBtn.setAlignment(Pos.CENTER_RIGHT);
        balanceAmountBtn.setPadding(new Insets(0, 0, 0, 0));
        balanceAmountBtn.setGraphic(IconButton.getIconView(new Image("/assets/selectAddress.png"), 172));
        m_balanceAmountImage.addListener((obs,oldval,newval)->{
            Image newImage = newval;
            if(newImage != null){
                balanceAmountBtn.setGraphic(IconButton.getIconView(newImage,newImage.getWidth()));
            }else{
                balanceAmountBtn.setGraphic(IconButton.getIconView(new Image("/assets/selectAddress.png"), 172));
            }
        });

        String textFieldId = getBoxId() +"TextField";

        int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);



     

        TextField amountField = new TextField(df.format(priceAmount.getDoubleAmount()));
        //  amountField.setMaxHeight(20);
        amountField.setId("amountField");
        amountField.setAlignment(Pos.CENTER_LEFT);
        amountField.setEditable(editable);
        amountField.setPadding(new Insets(3, 10, 3, 10));
        amountField.setUserData(textFieldId);
        amountField.textProperty().addListener((obs, oldval, newval)->{
           
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > priceAmount.getCurrency().getDecimals() ? rightSide.substring(0, priceAmount.getCurrency().getDecimals()) : rightSide;
        
            amountField.setText(leftSide +  rightSide);
        });
      

        Button enterButton = new Button("[ ENTER ]");
        enterButton.setFont(App.txtFont);
        enterButton.setId("toolBtn");


   

       
        SimpleBooleanProperty isFieldFocused = new SimpleBooleanProperty(false);
        
        scene.focusOwnerProperty().addListener((obs, old, newPropertyValue) -> {
            if (newPropertyValue != null && newPropertyValue instanceof TextField) {
                TextField focusedField = (TextField) newPropertyValue;
                Object userData = focusedField.getUserData();
                if(userData != null && userData instanceof String){
                    String userDataString = (String) userData;
                    if(userDataString.equals(textFieldId)){
                        isFieldFocused.set(true);
                    }else{
                        if(isFieldFocused.get()){
                            isFieldFocused.set(false);
                            enterButton.fire();
                        }
                    }
                }else{
                    if(isFieldFocused.get()){
                        isFieldFocused.set(false);
                        enterButton.fire();
                    }
                }
            }else{
                if(isFieldFocused.get()){
                    isFieldFocused.set(false);
                    enterButton.fire();
                }
            }
        });

        HBox.setHgrow(amountField, Priority.ALWAYS);

        ImageView textViewImage = IconButton.getIconView( new Image( priceAmount.getCurrency().getImageString()),35);

        VBox imgPaddingBox = new VBox(textViewImage);
        imgPaddingBox.setPadding(new Insets(0,15,0,10)); 
        imgPaddingBox.setMinHeight(40);
        imgPaddingBox.setAlignment(Pos.CENTER_LEFT);

        amountBtn.setOnAction(actionEvent -> {
            amountsBox.getChildren().remove(amountBtn);
            if(editable){
                amountsBox.getChildren().add(0, imgPaddingBox);
                amountsBox.getChildren().add(1, amountField);
                amountsBox.getChildren().add(2, enterButton);
            }else{
                
                amountsBox.getChildren().add(0, imgPaddingBox);
                amountsBox.getChildren().add(1, amountField);
      
            }

            Platform.runLater(()-> {
                amountField.requestFocus();

            });
            
        });

       

    
        getChildren().addAll(amountsBox, balanceAmountBtn);

        amountBtn.prefWidthProperty().bind(this.widthProperty());

        Runnable setNotFocused = () ->{
            if (amountsBox.getChildren().contains(enterButton)) {
                amountsBox.getChildren().remove(enterButton);
            }

            if (amountsBox.getChildren().contains(amountField)) {
                amountsBox.getChildren().remove(amountField);

            }

            if (amountsBox.getChildren().contains( imgPaddingBox)) {
                amountsBox.getChildren().remove( imgPaddingBox);
            }

            if (!(amountsBox.getChildren().contains(amountBtn))) {
                amountsBox.getChildren().add(amountBtn);
            }

            
        };
        enterButton.setOnAction(e->{
            if(editable){
                String text = amountField.getText();
                PriceAmount newAmount = new PriceAmount(Double.parseDouble(text.equals("") ? "0" : text), priceAmountProperty().get().getCurrency());
                priceAmountProperty().set(newAmount);
            }
            setNotFocused.run();
        });

        amountField.setOnAction(e->{
            enterButton.fire();
        });

        balanceAmountBtn.setOnAction(e->{
            PriceAmount balanceAmount = m_balanceAmount.get();
            if(balanceAmount != null){
                amountField.textProperty().set(balanceAmount.getAmountString());
                enterButton.fire();
            }
        });
        
        priceQuoteProperty().addListener((obs, oldval, newval)->updateBufferedImage());

        priceAmountProperty().addListener((obs,oldval, newval)-> updateBufferedImage());

        m_balanceAmount.addListener((obs, oldval, newVal)->{
            updateAmountImage();
        });
  
        updateBufferedImage();
    }

    public SimpleObjectProperty<PriceAmount> balanceAmountProperty(){
        return m_balanceAmount;
    }
   

    public void updateAmountImage() {
        final int padding = 5;
        

       
        PriceAmount balanceAmount = m_balanceAmount.get() != null ? m_balanceAmount.get() : new PriceAmount(0, (priceAmountProperty().get() != null ? priceAmountProperty().get().getCurrency() : new PriceCurrency("", "unknonw", "unknown", 0, "","unknowmn", "/assets/unknown-unit.png", "unkown", "")));
        boolean quantityValid = balanceAmount != null && balanceAmount.getAmountValid();
  
    
        BigInteger integers = balanceAmount != null ? balanceAmount.getBigDecimalAmount().toBigInteger() : BigInteger.ZERO;
        BigDecimal decimals = balanceAmount != null ? balanceAmount.getBigDecimalAmount().subtract(new BigDecimal(integers)) : BigDecimal.ZERO;
        int decimalPlaces = balanceAmount != null ? balanceAmount.getCurrency().getFractionalPrecision() : 0;
        String currencyName = balanceAmount != null ? balanceAmount.getCurrency().getSymbol() : "UKNOWN";
        int space = currencyName.indexOf(" ");
        currencyName = space != -1 ? currencyName.substring(0, space) : currencyName;

     

        java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        //   Image ergoBlack25 = new Image("/assets/ergo-black-25.png");
        //   SwingFXUtils.fromFXImage(ergoBlack25, null);
        
        String amountString = quantityValid ? String.format("%d", integers) : " -";
        String decs = String.format("%." + decimalPlaces + "f", decimals);
        String maxString = "MAX";

        decs = quantityValid ? decs.substring(1, decs.length()) : "";
   
    
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
 
        int stringWidth = fm.stringWidth(amountString);

        int height = fm.getHeight() + 10;

        g2d.setFont(smallFont);

        fm = g2d.getFontMetrics();
        int maxWidth = fm.stringWidth(maxString);


        //  int priceAscent = fm.getAscent();
        int integersX = padding;
      
        int decimalsX = integersX + stringWidth + 1;

       // int currencyNameStringWidth = fm.stringWidth(currencyName);
        int decsWidth = decs.equals("") ? 0 : fm.stringWidth(decs);
        int currencyNameWidth = fm.stringWidth(currencyName);

        int width = decimalsX + stringWidth + (decsWidth < currencyNameWidth ? currencyNameWidth : decsWidth) + (padding * 2) + padding + maxWidth;
  

        int currencyNameStringX = decimalsX + 2;

        g2d.dispose();
        
        BufferedImage unitImage = SwingFXUtils.fromFXImage(balanceAmount != null ? balanceAmount.getCurrency().getIcon() : new Image("/assets/unknown-unit.png"), null);
        Drawing.setImageAlpha(unitImage, 0x40);
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
         #ffffff05, #66666680, #ffffff05*/
         Drawing.fillArea(img, 0xff000000, 0, 0, width,height);
        Drawing.drawBar(1, 0x30ffffff, 0x60666666,img, 0, 0, width, height/2);

  

        g2d.drawImage(unitImage, width - unitImage.getWidth() - (maxWidth /2) , (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

       



        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(java.awt.Color.WHITE);

        

        g2d.drawString(amountString, integersX, fm.getAscent() + 5);

        g2d.setFont(smallFont);
        fm = g2d.getFontMetrics();
        g2d.setColor(new java.awt.Color(.9f, .9f, .9f, .9f));

       
        if(decimalPlaces > 0){
            //decimalsX = widthIncrease > 0 ? decimalsX + widthIncrease : decimalsX;
            g2d.drawString(decs, decimalsX , fm.getHeight() + 2);
        }

        
        g2d.drawString(currencyName, currencyNameStringX, height - 10);
        // ((height - fm.getHeight()) / 2) + fm.getAscent())
        g2d.setColor(Color.WHITE);
        g2d.drawString(maxString, width - ((padding*2) + maxWidth),  ((height - fm.getHeight()) / 2) + fm.getAscent());
       // g2d.setFont(smallFont);
   
     //   fm = g2d.getFontMetrics();


        
        /*try {
            Files.writeString(logFile.toPath(), amountString + decs);
        } catch (IOException e) {

        }*/
        g2d.dispose();

       /* try {
            ImageIO.write(img, "png", new File("outputImage.png"));
        } catch (IOException e) {

        }*/

        m_balanceAmountImage.set(SwingFXUtils.toFXImage(img, null));
        
    }

   

}
