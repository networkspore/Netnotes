package com.netnotes;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;

public class BufferedButton extends Button {

    public final static String ON_MOUSE_PRESSED_EFFECT_ID = "onMousePressed";

    private BufferedImageView m_imgBufView;

    public BufferedButton() {
        this("/assets/menu-outline-30.png");
    }

    public BufferedButton(String urlString) {
        this("", urlString);
    }

    public BufferedButton(Image image) {
        super("");
        m_imgBufView = new BufferedImageView(image);
        setGraphic(m_imgBufView);

    }

    public BufferedButton(String urlString, double imageWidth) {
        super();
        m_imgBufView = new BufferedImageView(new Image(urlString), imageWidth);
        setGraphic(m_imgBufView);

        setId("menuBtn");
        setOnMousePressed((pressedEvent) -> m_imgBufView.applyInvertEffect(ON_MOUSE_PRESSED_EFFECT_ID, .6));
        setOnMouseReleased((pressedEvent) -> m_imgBufView.removeEffect(ON_MOUSE_PRESSED_EFFECT_ID));

    }

    public BufferedButton(String name, String urlString) {
        super(name);
        m_imgBufView = new BufferedImageView(new Image(urlString), 30);
        setGraphic(m_imgBufView);

        setId("menuBtn");
        setOnMousePressed((pressedEvent) -> m_imgBufView.applyInvertEffect(ON_MOUSE_PRESSED_EFFECT_ID, .6));
        setOnMouseReleased((pressedEvent) -> m_imgBufView.removeEffect(ON_MOUSE_PRESSED_EFFECT_ID));

    }

    public BufferedImageView getBufferedImageView() {
        return m_imgBufView;
    }

    public void setImage(Image image) {

        m_imgBufView.setDefaultImage(image);

    }
}
