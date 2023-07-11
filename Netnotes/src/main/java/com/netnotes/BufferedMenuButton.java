package com.netnotes;

import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;

public class BufferedMenuButton extends MenuButton {

    private BufferedImageView m_imgBufView;

    public BufferedMenuButton() {
        this("/assets/menu-outline-30.png");
    }

    public BufferedMenuButton(String urlString) {
        this("", urlString);
    }

    public BufferedMenuButton(String name, String urlString) {
        super(name);
        m_imgBufView = new BufferedImageView(new Image(urlString), 30);
        setGraphic(m_imgBufView);

        /*setId("menuBtn");*/
        setOnMousePressed((event) -> {
            m_imgBufView.applyInvertEffect(.6);
            show();
        });
        setOnMouseReleased((event) -> {
            m_imgBufView.clearEffects();
        });

    }

    public BufferedImageView getBufferedImageView() {
        return m_imgBufView;
    }
}
