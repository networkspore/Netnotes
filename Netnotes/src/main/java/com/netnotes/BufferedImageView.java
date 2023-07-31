package com.netnotes;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.awt.image.BufferedImage;

import java.util.ArrayList;

public class BufferedImageView extends ImageView {

    private Image m_img;
    private ArrayList<Effects> m_effects = new ArrayList<Effects>();

    public BufferedImageView() {
        super();
        m_img = null;
    }

    public BufferedImageView(Image image, double imageWidth) {
        this(image, false);
        setFitWidth(imageWidth);

    }

    public BufferedImageView(Image image, boolean fitWidth) {
        super(image);
        m_img = image;

        setPreserveRatio(true);
        if (fitWidth) {
            setFitWidth(image.getWidth());
        }

    }

    public BufferedImageView(Image image) {
        super(image);
        m_img = image;

    }

    public void setDefaultImage(Image img) {
        m_img = img;
        updateImage();
    }

    public void setDefaultImage(Image img, double fitWidth) {
        m_img = img;
        updateImage();
        setFitWidth(fitWidth);
    }

    public Effects getEffect(String id) {
        for (int i = 0; i < m_effects.size(); i++) {
            Effects effect = m_effects.get(i);
            if (effect.getId().equals(id)) {
                return effect;
            }

        }
        return null;
    }

    public Effects getFirstNameEffect(String name) {
        if (m_effects.size() == 0) {
            return null;
        }

        for (int i = 0; i < m_effects.size(); i++) {
            Effects effect = m_effects.get(i);
            if (effect.getName().equals(name)) {
                return effect;
            }

        }

        return null;
    }

    public void applyInvertEffect(double amount) {

        m_effects.add(new InvertEffect(amount));
        updateImage();

    }

    public void addEffect(Effects effect) {
        m_effects.add(effect);
    }

    public void clearEffects() {
        m_effects.clear();
        updateImage();
    }

    public void updateImage() {
        if (m_img != null) {
            if (m_effects.size() > 0) {
                BufferedImage imgBuf = SwingFXUtils.fromFXImage(m_img, null);

                for (int i = 0; i < m_effects.size(); i++) {
                    Effects effect = m_effects.get(i);
                    effect.applyEffect(imgBuf);
                }

                Image imgUpdate = SwingFXUtils.toFXImage(imgBuf, null);

                setImage(imgUpdate);

            } else {
                Platform.runLater(() -> setImage(m_img));

            }
        } else {
            setImage(null);
        }
    }

    public Image getBaseImage() {
        return m_img;
    }

    public void updateImage(BufferedImage imgBuf) {
        if (m_img != null) {
            if (m_effects.size() > 0) {

                for (int i = 0; i < m_effects.size(); i++) {
                    Effects effect = m_effects.get(i);
                    effect.applyEffect(imgBuf);
                }

                Image imgUpdate = SwingFXUtils.toFXImage(imgBuf, null);

                setImage(imgUpdate);

            } else {
                Platform.runLater(() -> setImage(SwingFXUtils.toFXImage(imgBuf, null)));

            }
        } else {
            Platform.runLater(() -> setImage(SwingFXUtils.toFXImage(imgBuf, null)));
        }
    }
}
