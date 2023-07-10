package com.netnotes;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class BufferedImageView extends ImageView {

    private static File logFile = new File("BufferedImageView-log.txt");
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

    public BufferedImageView(Image img) {
        this(img, true);
    }

    public BufferedImageView(Image image, boolean fitWidth) {
        super(image);
        m_img = image;

        setPreserveRatio(true);
        if (fitWidth) {
            setFitWidth(image.getWidth());
        }

    }

    public void setDefaultImage(Image img) {
        m_img = img;
        setFitWidth(img.getWidth());
        updateImage();
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

    public void clear() {
        m_effects.clear();
        updateImage();
    }

    public void updateImage() {
        if (m_img != null) {
            if (m_effects.size() > 0) {
                BufferedImage imgBuf = SwingFXUtils.fromFXImage(m_img, null);
                try {
                    Files.writeString(logFile.toPath(), "\nGot a buffered image " + imgBuf.getWidth(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
                for (int i = 0; i < m_effects.size(); i++) {
                    Effects effect = m_effects.get(i);
                    effect.applyEffect(imgBuf);
                }
                try {
                    Files.writeString(logFile.toPath(), "\nbuffered image inverted" + imgBuf.getWidth(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }

                Image imgUpdate = SwingFXUtils.toFXImage(imgBuf, null);

                try {
                    Files.writeString(logFile.toPath(), "\nbuffered image inverted" + imgBuf.getWidth(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }

                setImage(imgUpdate);

                try {
                    Files.writeString(logFile.toPath(), "\nupdated Image", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            } else {
                Platform.runLater(() -> setImage(m_img));

                try {
                    Files.writeString(logFile.toPath(), "\nreverted Image", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
            }
        }
    }
}
