package com.netnotes;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;

public class IconButton extends Button {

    public class IconStyle {

        public static String ROW = "ROW";
        public static String ICON = "ICON";
    }
    public static double SMALL_PADDING = 5;
    public static double NORMAL_PADDING = 15;
    public static double NORMAL_IMAGE_WIDTH = 75;
    public static double SMALL_IMAGE_WIDTH = 30;

    public static Insets SMALL_INSETS = new Insets(SMALL_PADDING, SMALL_PADDING, SMALL_PADDING, SMALL_PADDING);
    public static Insets NORMAL_INSETS = new Insets(NORMAL_PADDING, NORMAL_PADDING, NORMAL_PADDING, NORMAL_PADDING);

    public final static String DEFAULT_CURRENT_ID = "iconBtnCurrent";
    public final static String DEFAULT_ID = "iconBtn";

    private String m_defaultId = DEFAULT_ID;
    private String m_currentId = DEFAULT_CURRENT_ID;

    private Image m_icon;
    private double m_imageWidth = 75;
    private String m_name = "";
    private String m_iconStyle = IconStyle.ICON;

    private boolean m_multipleInstances = false;
    private ChangeListener<Boolean> m_focusListener;
    //private EventHandler<MouseEvent> m_mouseEventHandler;

    public IconButton() {
        super();

        setId("iconBtn");
        setFont(App.txtFont);
        enableActions();
    }

    public IconButton(Image icon) {
        this();
        setIcon(icon);
    }

    public IconButton(Image icon, String name) {
        this();
        setIcon(icon);
        setName(name);

    }

    public IconButton(Image image, String name, String iconStyle) {
        super();
        setIcon(image);
        setName(name);
        if (iconStyle.equals(IconStyle.ROW)) {
            setPadding(SMALL_INSETS);
        } else {
            setPadding(NORMAL_INSETS);
        }

        setIconStyle(iconStyle);
        setId("iconBtn");
        setFont(App.txtFont);
        enableActions();
    }

    private void startFocusCurrent() {
        m_focusListener = (obs, oldValue, newValue) -> setCurrent(newValue.booleanValue());
        focusedProperty().addListener(m_focusListener);
    }

    private void stopFocusCurrent() {
        focusedProperty().removeListener(m_focusListener);
    }

    public void enableActions() {
        setOnMouseClicked((event) -> onClick(event));
        startFocusCurrent();
    }

    public void disableActions() {
        setOnMouseClicked(null);
        stopFocusCurrent();
    }

    public void onClick(MouseEvent e) {
        if (!isFocused()) {
            Platform.runLater(() -> requestFocus());
        }
        if (e.getClickCount() == 2) {

            open();

        }
    }

    public void open() {

    }

    public void close() {

    }

    public boolean getMultipleInstances() {
        return m_multipleInstances;
    }

    public void setMultipleInstances(boolean allowMultipleInstances) {
        m_multipleInstances = allowMultipleInstances;
    }

    public IconButton(String text) {
        this();
        setName(text);
    }

    public String getIconStyle() {
        return m_iconStyle;
    }

    public void setIconStyle(String style) {
        switch (style) {
            case "ICON":
                setImageWidth(NORMAL_IMAGE_WIDTH);

                setContentDisplay(ContentDisplay.TOP);
                setTextAlignment(TextAlignment.CENTER);

                break;
            case "ROW":
                setImageWidth(SMALL_IMAGE_WIDTH);

                setContentDisplay(ContentDisplay.LEFT);
                setAlignment(Pos.CENTER_LEFT);
                setText(m_name);

                break;
        }
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {

        m_name = name;
        name = name.replace("\n", " ");
        java.awt.Font font = new java.awt.Font(getFont().getFamily(), java.awt.Font.PLAIN, (int) getFont().getSize());

        FontMetrics metrics = getFontMetrics(font);

        int stringWidth = metrics.stringWidth(name);
        double imageWidth = getImageWidth();
        if (stringWidth > imageWidth) {
            int indexOfSpace = name.indexOf(" ");

            if (indexOfSpace > 0) {
                String firstWord = name.substring(0, indexOfSpace);

                if (metrics.stringWidth(firstWord) > imageWidth) {
                    setText(truncateName(name, metrics));
                } else {

                    String text = firstWord + "\n";
                    String secondWord = name.substring(indexOfSpace + 1, name.length());

                    if (metrics.stringWidth(secondWord) > imageWidth) {
                        secondWord = truncateName(secondWord, metrics);
                    }
                    text = text + secondWord;
                    setText(text);
                }
            } else {

                setText(truncateName(name, metrics));
            }

        } else {
            setText(name);
        }
    }

    public String truncateName(String name, FontMetrics metrics) {
        double imageWidth = getImageWidth();
        String truncatedString = name.substring(0, 3) + "...";
        if (name.length() > 3) {
            int i = name.length() - 3;
            truncatedString = name.substring(0, i) + "...";

            while (metrics.stringWidth(truncatedString) > imageWidth && i > 1) {
                i = i - 1;
                truncatedString = name.substring(0, i) + "...";

            }
        }
        return truncatedString;
    }

    public FontMetrics getFontMetrics(java.awt.Font font) {

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        return g2d.getFontMetrics();

    }

    public IconButton getButton() {
        return this;
    }

    public void setCurrent(boolean value, String... idString) {

        if (idString != null && idString.length > 0) {
            m_defaultId = idString[1];

            if (idString.length > 1) {
                m_currentId = idString[0];
            }
        }

        if (value) {
            setId(m_currentId);
        } else {
            setId(m_defaultId);
        }
    }

    public static ImageView getIconView(Image image, double imageWidth) {
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(imageWidth);


            /*
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {

            imageView.setEffect(colorAdjust);

        });
        imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setEffect(null);
        }); */
            return imageView;
        } else {
            return null;
        }
    }

    public Image getIcon() {
        return m_icon;
    }

    public void setIcon(Image icon) {
        m_icon = icon;
        setGraphic(getIconView(icon, m_imageWidth));
    }

    public double getImageWidth() {
        return m_imageWidth;
    }

    public void setImageWidth(double imageWidth) {
        m_imageWidth = imageWidth;
        setGraphic(getIconView(m_icon, m_imageWidth));
    }

}
