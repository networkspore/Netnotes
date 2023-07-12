package com.netnotes;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;

public class BufferedButton extends Button {

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

    public BufferedButton(String name, String urlString) {
        super(name);
        m_imgBufView = new BufferedImageView(new Image(urlString), 30);
        setGraphic(m_imgBufView);

        setId("menuBtn");
        setOnMousePressed((pressedEvent) -> m_imgBufView.applyInvertEffect(.6));
        setOnMouseReleased((pressedEvent) -> m_imgBufView.clearEffects());

    }

    public BufferedImageView getBufferedImageView() {
        return m_imgBufView;
    }
}
