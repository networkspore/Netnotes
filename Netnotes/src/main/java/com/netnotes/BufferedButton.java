package com.netnotes;

import javafx.scene.control.Button;
import javafx.scene.image.Image;

public class BufferedButton extends Button {

    private BufferedImageView m_imgBufView;

    public BufferedButton() {
        this("/assets/menu-outline-30.png");
    }

    public BufferedButton(String urlString) {
        super();
        m_imgBufView = new BufferedImageView(new Image(urlString), 30);
        setGraphic(m_imgBufView);

        setId("menuBtn");
        setOnMousePressed((pressedEvent) -> m_imgBufView.applyInvertEffect());
        setOnMouseReleased((pressedEvent) -> m_imgBufView.clear());

        focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                m_imgBufView.clear();
            }
        });
    }

    public BufferedImageView getBufferedImageView() {
        return m_imgBufView;
    }
}
