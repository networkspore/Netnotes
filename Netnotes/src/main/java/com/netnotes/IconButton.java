package com.netnotes;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.effect.ColorAdjust;

public class IconButton extends Button {

    private ImageView m_icon;
    private String m_uuid;

    public IconButton(Image icon, String title, String uuid) {
        super(title);
        setIconButton(icon, title, uuid, 75, 15);
    }

    public void setIconButton(Image icon, String title, String uuid, double imageWidth, double padding) {

        m_uuid = uuid;

        m_icon = highlightedImageView(icon);
        m_icon.setFitHeight(imageWidth);
        m_icon.setPreserveRatio(true);

        setGraphic(m_icon);
        setId("iconBtn");
        setFont(App.txtFont);
        setContentDisplay(ContentDisplay.TOP);
        // setPrefWidth(imageWidth + (padding * 2));
        setPadding(new Insets(10 + padding, padding, padding, padding));

    }

    private static ImageView highlightedImageView(Image image) {

        ImageView imageView = new ImageView(image);

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {

            imageView.setEffect(colorAdjust);

        });
        imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setEffect(null);
        });

        return imageView;
    }

    public void setUUID(String uuid) {
        m_uuid = uuid;
    }

    public String getUUID() {
        return m_uuid;
    }
}
