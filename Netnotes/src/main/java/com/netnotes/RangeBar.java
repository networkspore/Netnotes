package com.netnotes;

import java.awt.image.BufferedImage;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class RangeBar extends BufferedImageView {

    public static int DEFAULT_BUTTON_HEIGHT = 18;

    private SimpleDoubleProperty m_height;
    private SimpleDoubleProperty m_width;

    private double m_maxTop = 1;
    private SimpleDoubleProperty m_topVvalue = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty m_bottomVvalue = new SimpleDoubleProperty(0);
    private double m_minBot = 0;

    private int m_btnHeight = DEFAULT_BUTTON_HEIGHT;

    private int m_btnTopBgColor1 = 0x504bbd94;
    private int m_btnTopBgColor2 = 0xff000000;
    private int m_btnTopBgColor3 = 0x004bbd94;
    private int m_btnTopBgColor4 = 0x104bbd94;
    private int m_btnTopBorderColor = 0xff028A0F;
    private int m_btnTopBorderColor2 = 0x50000000;

    private int m_btnBotBgColor1 = 0xff000000;
    private int m_btnBotBgColor2 = 0x50e96d71;
    private int m_btnBotBgColor3 = 0x00e96d71;
    private int m_btnBotBgColor4 = 0x10e96d71;
    private int m_btnBotBorderColor = 0xff9A2A2A;
    private int m_btnBotBorderColor2 = 0x50000000;

    private int m_bg1 = 0x50ffffff;
    private int m_bg2 = 0x50000000;

    private int m_bg3 = 0x80777777;
    private int m_bg4 = 0xcceeeeee;

    private int m_barRGB1 = 0x55333333;
    private int m_barRGB2 = 0x50ffffff;

    public RangeBar(SimpleDoubleProperty width, SimpleDoubleProperty height) {
        super(getBgImage(width.get(), height.get()), width.get());

        m_width = width;
        m_height = height;

        setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()));

        m_height.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()), m_width.get());
        });
        m_width.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_width.get() < 1 ? 1 : m_width.get()), newVal.doubleValue());
        });

        setOnMousePressed((mouseEvent) -> onMousePressed(mouseEvent));
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

        return barImageBuf;
    }
    private SimpleBooleanProperty m_settingRange = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty m_active = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty activeProperty() {
        return m_active;
    }

    private void onMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            boolean settingRange = m_settingRange.get();
            if (!settingRange) {

                m_settingRange.set(true);
            } else {

                double mouseY = event.getY();
                int height = (int) Math.ceil(getBaseImage().getHeight());

                if (mouseY <= m_btnHeight) {
                    m_active.set(true);
                    m_settingRange.set(false);
                } else {
                    if (mouseY >= height - m_btnHeight) {
                        m_settingRange.set(false);
                        m_active.set(false);
                        m_topVvalue.set(m_maxTop);
                        m_bottomVvalue.set(m_minBot);

                    } else {
                        double newVal = 1.0 - (double) (mouseY - m_btnHeight) / (height - (m_btnHeight * 2));

                        //double closeRange = 5 / (height - (m_btnHeight * 2));
                        double topValue = m_topVvalue.get();
                        double bottomValue = m_bottomVvalue.get();

                        double distanceToY1 = topValue > newVal ? topValue - newVal : topValue == newVal ? 0 : newVal - topValue;
                        double distanceToY2 = bottomValue > newVal ? bottomValue - newVal : bottomValue == newVal ? 0 : newVal - bottomValue;

                        if (distanceToY1 != distanceToY2) {
                            if (distanceToY1 < distanceToY2) {
                                if (newVal > bottomValue && newVal <= m_maxTop) {
                                    m_topVvalue.set(newVal);
                                }
                            } else {
                                if (newVal < topValue && newVal >= m_minBot) {
                                    m_bottomVvalue.set(newVal);
                                }
                            }
                        }

                    }
                }
            }
            updateImage();
        }
    }

    public double getScrollScale(int height) {
        //  (mouseY - m_btnHeight) / (height - (m_btnHeight * 2))
        return ((double) (height - (m_btnHeight * 2))) / m_maxTop;
    }

    public int getY1(int height) {
        double topValue = m_topVvalue.get();
        return (topValue == m_maxTop ? 0 : (int) Math.ceil(((height - (m_btnHeight * 2)) - (getScrollScale(height) * topValue)))) + m_btnHeight;
    }

    public int getY2(int height) {
        double botValue = m_bottomVvalue.get();
        return (botValue == m_minBot ? height - (m_btnHeight * 2) : (int) Math.ceil(((height - (m_btnHeight * 2)) - (getScrollScale(height) * botValue)))) + m_btnHeight;
    }

    @Override
    public void updateImage() {

        BufferedImage imgBuf = getBgImage(m_width, m_height);
        int height = imgBuf.getHeight();
        int width = imgBuf.getWidth();

        int btnTopX1 = 2;
        int btnTopY1 = 0;
        int btnTopX2 = width - 2;
        int btnTopY2 = m_btnHeight;

        int btnBotX1 = 2;
        int btnBotY1 = height - m_btnHeight;
        int btnBotX2 = width - 2;
        int btnBotY2 = height;

        // double imgScale = getScrollScale(height);
        int x1 = 3;
        int y1 = getY1(height);

        int x2 = width - 3;
        int y2 = getY2(height);
        boolean settingRange = m_settingRange.get();

        if (!settingRange) {
            Drawing.fillArea(imgBuf, 50333333, 0, 0, width, height);
            Drawing.drawBar(1, m_bg1, m_bg2, imgBuf, x1 + 1, y1 + 1, x2 - 1, y2 - 1);
            Drawing.drawBar(m_barRGB1, m_barRGB2, imgBuf, x1 + 1, y1 + 1, x2 - 1, y2 - 1);

        } else {
            Drawing.fillArea(imgBuf, 0x20ffffff, 0, 0, width, height);

            Drawing.drawBar(1, 0xffffffff, 0xff00ff00, imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.drawBar(1, m_btnTopBgColor1, m_btnTopBgColor2, imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.drawBar(0, m_btnTopBgColor3, m_btnTopBgColor4, imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);

            Drawing.fillArea(imgBuf, m_btnTopBorderColor2, btnTopX1, btnTopY1, btnTopX1 + 1, btnTopY2);
            Drawing.fillArea(imgBuf, m_btnTopBorderColor2, btnTopX1, btnTopY1, btnTopX2, btnTopY1 + 1);
            Drawing.fillArea(imgBuf, m_btnTopBorderColor, btnTopX2 - 1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.fillArea(imgBuf, m_btnTopBorderColor, btnTopX1 + 1, btnTopY2 - 1, btnTopX2 - 1, btnTopY2);

            Drawing.drawBar(1, 0xffff0000, m_btnBotBorderColor, imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.drawBar(1, m_btnBotBgColor2, m_btnBotBgColor1, imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.drawBar(0, m_btnBotBgColor3, m_btnBotBgColor4, imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);

            Drawing.fillArea(imgBuf, m_btnBotBorderColor2, btnBotX1, btnBotY1, btnBotX1 + 2, btnBotY2); //left
            Drawing.fillArea(imgBuf, m_btnBotBorderColor, btnBotX1, btnBotY1, btnBotX2, btnBotY1 + 1); //top
            Drawing.fillArea(imgBuf, m_btnBotBgColor2, btnBotX2 - 1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.fillArea(imgBuf, m_btnBotBorderColor2, btnBotX1 + 1, btnBotY2 - 1, btnBotX2 - 1, btnBotY2);

            Drawing.drawBar(1, m_bg4, m_bg3, imgBuf, x1 + 1, y1 + 1, x2 - 1, y2 - 1);
            Drawing.drawBar(0, m_bg2, m_bg1, imgBuf, x1, y1, x2 - 1, y2);

            Drawing.fillArea(imgBuf, 0x50111111, x1 + 1, y1, x1 + 2, y2);
            Drawing.fillArea(imgBuf, 0x50111111, x2 - 2, y1, x2 - 1, y2);
        }

        super.updateImage(imgBuf);
    }
}
