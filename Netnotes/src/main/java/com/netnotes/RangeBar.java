package com.netnotes;

import java.awt.image.BufferedImage;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.image.Image;

public class RangeBar extends BufferedImageView {

    public static double DEFAULT_WIDTH = 7;
    public static double DEFAULT_HEIGHT = 100;

    private SimpleDoubleProperty m_height;
    private SimpleDoubleProperty m_width;

    private double m_maxTop = 1;
    private double m_topVvalue = 1;
    private double m_bottomVvalue = 0;
    private double m_minBot = 0;
    private int m_barRGB1 = 0x55777777;
    private int m_barRGB2 = 0x50333333;

    public RangeBar(SimpleDoubleProperty width, SimpleDoubleProperty height) {
        super(getBgImage(DEFAULT_WIDTH, DEFAULT_HEIGHT), DEFAULT_WIDTH);

        m_width = width;
        m_height = height;

        setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()));

        m_height.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()));
        });
        m_width.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_width.get() < 1 ? 1 : m_width.get()), newVal.doubleValue());
        });

    }

    public SimpleDoubleProperty widthProperty() {
        return m_width;
    }

    public SimpleDoubleProperty heightProperty() {
        return m_height;
    }

    public static Image getBgImage(double width, double height) {
        return getBgImage((int) Math.ceil(width), (int) Math.ceil(height));
    }

    public static Image getBgImage(int width, int height) {
        BufferedImage barImageBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Drawing.fillArea(barImageBuf, 0x00010101, 0, 0, width, height);
        return SwingFXUtils.toFXImage(barImageBuf, null);
    }

    public static BufferedImage getBgImage(SimpleDoubleProperty w, SimpleDoubleProperty h) {
        int width = (int) Math.ceil(w.get());
        int height = (int) Math.ceil(h.get());

        BufferedImage barImageBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Drawing.fillArea(barImageBuf, 0x50333333, 0, 0, width, height);
        return barImageBuf;
    }

    @Override
    public void updateImage() {

        BufferedImage imgBuf = getBgImage(m_width, m_height);
        int height = imgBuf.getHeight();
        int width = imgBuf.getWidth();
        double imgScale = ((double) height) / m_maxTop;

        int x1 = 0;
        int y1 = m_topVvalue == m_maxTop ? 0 : (int) Math.ceil((height - (imgScale * m_topVvalue)));

        int x2 = width;
        int y2 = m_bottomVvalue == m_minBot ? height : (int) Math.ceil((height - (imgScale * m_bottomVvalue)));

        Drawing.drawBar(1, 0x50999999, 0x50333333, imgBuf, x1, y1, x2, y2);
        Drawing.drawBar(1, m_barRGB1, m_barRGB2, imgBuf, x1, y1, x2, y2);

        super.updateImage(imgBuf);
    }
}
