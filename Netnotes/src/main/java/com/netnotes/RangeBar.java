package com.netnotes;

import java.awt.image.BufferedImage;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.image.Image;

public class RangeBar extends BufferedImageView {

    private SimpleDoubleProperty m_height = new SimpleDoubleProperty(100);
    private SimpleDoubleProperty m_width = new SimpleDoubleProperty(20);
    private SimpleDoubleProperty m_scale = new SimpleDoubleProperty(0);
    private SimpleDoubleProperty m_topVvalue = new SimpleDoubleProperty(0);
    private SimpleDoubleProperty m_bottomVvalue = new SimpleDoubleProperty(0);

    public RangeBar() {
        super(getBarImage(20, 20));
        m_height.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBarImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()));
        });
        m_width.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBarImage(m_width.get() < 1 ? 1 : m_width.get(), m_width.get() < 1 ? 1 : m_width.get()));
        });

    }

    public SimpleDoubleProperty widthProperty() {
        return m_width;
    }

    public SimpleDoubleProperty heightProperty() {
        return m_height;
    }

    public static Image getBarImage(double width, double height) {
        return getBarImage((int) Math.ceil(width), (int) Math.ceil(height));
    }

    public static Image getBarImage(int width, int height) {
        BufferedImage barImageBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Drawing.fillArea(barImageBuf, 0x00010101, 0, 0, width, height);
        return SwingFXUtils.toFXImage(barImageBuf, null);
    }
}
