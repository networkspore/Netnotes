package com.netnotes;

import java.awt.Color;
import java.awt.image.BufferedImage;


public class Drawing {


    public static void drawBarFillColor(int direction, boolean fillInverse, int fillColor, int RGB1, int RGB2, BufferedImage img, int x1, int y1, int x2, int y2) {
        /*cancel draw if out of bounds?
            if(!(y1 < img.getHeight() && y2 <= img.getHeight() && x1 < img.getWidth() && x2 <= img.getWidth())){
            return;
        }*/
        int a1 = (RGB1 >> 24) & 0xff;
        int r1 = (RGB1 >> 16) & 0xff;
        int g1 = (RGB1 >> 8) & 0xff;
        int b1 = RGB1 & 0xff;

        int a2 = (RGB1 >> 24) & 0xff;
        int r2 = (RGB2 >> 16) & 0xff;
        int g2 = (RGB2 >> 8) & 0xff;
        int b2 = RGB2 & 0xff;

        int i = 0;
        int width;
        int height;
        double scaleA;
        double scaleR;
        double scaleG;
        double scaleB;

        switch (direction) {
            case 1:
                height = (y2 - y1) - 1;
                height = height < 1 ? 1 : height;
                // double middle = height / 2;
                //(0.6d * (double) height) / High

                scaleA = (double) (a2 - a1) / (double) height;
                scaleR = (double) (r2 - r1) / (double) height;
                scaleG = (double) (g2 - g1) / (double) height;
                scaleB = (double) (b2 - b1) / (double) height;

                /*try {
                    Files.writeString(logFile.toPath(), "\nscaleA: " + scaleA + " R " + scaleR + " G " + scaleG + " B " + scaleB, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/
                i = 0;
                for (int x = x1; x < x2; x++) {
                    for (int y = y1; y < y2; y++) {
                        int oldRGB = img.getRGB(x, y);

                        if (oldRGB == fillColor || (oldRGB != fillColor && fillInverse)) {
                            int a = (int) (a1 + (i * scaleA));
                            int r = (int) (r1 + (i * scaleR));
                            int g = (int) (g1 + (i * scaleG));
                            int b = (int) (b1 + (i * scaleB));

                            /*
                            try {
                                Files.writeString(logFile.toPath(), "\ni: " + i + " a: " + a + " r: " + r + " g: " + g + " b: " + b, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {

                            }*/
                            int p = (a << 24) | (r << 16) | (g << 8) | b;

                            img.setRGB(x, y, blendRGBA(oldRGB, p));
                        }
                        i++;
                    }
                    i = 0;
                }
                break;
            default:
                width = (x2 - x1) - 1;
                width = width < 1 ? 1 : width;
                // double middle = width / 2;
                //(0.6d * (double) height) / High

                scaleA = (double) (a2 - a1) / (double) width;
                scaleR = (double) (r2 - r1) / (double) width;
                scaleG = (double) (g2 - g1) / (double) width;
                scaleB = (double) (b2 - b1) / (double) width;

                i = 0;
                for (int x = x1; x < x2; x++) {
                    for (int y = y1; y < y2; y++) {
                        int oldRGB = img.getRGB(x, y);

                        if (oldRGB == fillColor || (oldRGB != fillColor && fillInverse)) {

                            int a = (int) (a1 + (i * scaleA));
                            int r = (int) (r1 + (i * scaleR));
                            int g = (int) (g1 + (i * scaleG));
                            int b = (int) (b1 + (i * scaleB));

                            int p = (a << 24) | (r << 16) | (g << 8) | b;

                            img.setRGB(x, y, blendRGBA(oldRGB, p));
                        }

                    }
                    i++;
                }
                break;
        }
    }

    public static void drawBar(int rgb1, int rgb2, BufferedImage img, int x1, int y1, int x2, int y2) {
        drawBar(0, rgb1, rgb2, img, x1, y1, x2, y2);
    }

    public static void drawBar(Color color1, Color color2, BufferedImage img, int x1, int y1, int x2, int y2) {
        drawBar(0, color1, color2, img, x1, y1, x2, y2);
    }

    public static void drawBar(int direction, Color color1, Color color2, BufferedImage img, int x1, int y1, int x2, int y2) {
        drawBar(direction, color1.getRGB(), color2.getRGB(), img, x1, y1, x2, y2);
    }

    public static void drawBar(int direction, int RGB1, int RGB2, BufferedImage img, int x1, int y1, int x2, int y2) {

        int a1 = (RGB1 >> 24) & 0xff;
        int r1 = (RGB1 >> 16) & 0xff;
        int g1 = (RGB1 >> 8) & 0xff;
        int b1 = RGB1 & 0xff;

        int a2 = (RGB1 >> 24) & 0xff;
        int r2 = (RGB2 >> 16) & 0xff;
        int g2 = (RGB2 >> 8) & 0xff;
        int b2 = RGB2 & 0xff;

        int i = 0;
        int width;
        int height;
        double scaleA;
        double scaleR;
        double scaleG;
        double scaleB;

        switch (direction) {
            case 1:
                height = (y2 - y1) - 1;
                height = height < 1 ? 1 : height;
                // double middle = height / 2;
                //(0.6d * (double) height) / High

                scaleA = (double) (a2 - a1) / (double) height;
                scaleR = (double) (r2 - r1) / (double) height;
                scaleG = (double) (g2 - g1) / (double) height;
                scaleB = (double) (b2 - b1) / (double) height;

                i = 0;
                for (int x = x1; x < x2; x++) {
                    for (int y = y1; y < y2; y++) {
                        int oldRGB = img.getRGB(x, y);
                        int a = (int) (a1 + (i * scaleA));
                        int r = (int) (r1 + (i * scaleR));
                        int g = (int) (g1 + (i * scaleG));
                        int b = (int) (b1 + (i * scaleB));

                        int p = (a << 24) | (r << 16) | (g << 8) | b;
                        img.setRGB(x, y, blendRGBA(oldRGB, p));
                        i++;
                    }
                    i = 0;
                }
                break;
            default:
                width = (x2 - x1) - 1;
                width = width < 1 ? 1 : width;
                // double middle = width / 2;
                //(0.6d * (double) height) / High

                scaleA = (double) (a2 - a1) / (double) width;
                scaleR = (double) (r2 - r1) / (double) width;
                scaleG = (double) (g2 - g1) / (double) width;
                scaleB = (double) (b2 - b1) / (double) width;

                i = 0;
                for (int x = x1; x < x2; x++) {
                    for (int y = y1; y < y2; y++) {
                        int oldRGB = img.getRGB(x, y);
                        int a = (int) (a1 + (i * scaleA));
                        int r = (int) (r1 + (i * scaleR));
                        int g = (int) (g1 + (i * scaleG));
                        int b = (int) (b1 + (i * scaleB));

                        int p = (a << 24) | (r << 16) | (g << 8) | b;

                        img.setRGB(x, y, blendRGBA(oldRGB, p));

                    }
                    i++;
                }
                break;
        }
    }

    public static void fillArea(BufferedImage img, int RGB, int x1, int y1, int x2, int y2) {
        fillArea(img, RGB, x1, y1, x2, y2, true);
    }

    /*int avg = (r + g + b) / 3;*/
    public static void fillArea(BufferedImage img, int RGB, int x1, int y1, int x2, int y2, boolean blend) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                if (blend) {
                    int oldRGB = img.getRGB(x, y);
                    img.setRGB(x, y, blendRGBA(oldRGB, RGB));
                } else {
                    img.setRGB(x, y, RGB);
                }
            }
        }
    }

    public static void fillAreaDotted(int size, BufferedImage img, int RGB, int x1, int y1, int x2, int y2) {

        int j = 0;

        for (int x = x1; x < x2; x += size) {
            for (int y = y1; y < y2; y += size) {

                if (j % 2 == 0) {
                    // int oldRGB = img.getRGB(x, y);
                    //  img.setRGB(x, y, blendRGBA(oldRGB, RGB));
                    fillArea(img, RGB, x, y, x + size, y + size);
                }
                j++;
            }

        }

    }

    public static int blendRGBA(int RGB1, int RGB2) {

        int aA = (RGB1 >> 24) & 0xff;
        int rA = (RGB1 >> 16) & 0xff;
        int gA = (RGB1 >> 8) & 0xff;
        int bA = RGB1 & 0xff;

        int aB = (RGB2 >> 24) & 0xff;
        int rB = (RGB2 >> 16) & 0xff;
        int gB = (RGB2 >> 8) & 0xff;
        int bB = RGB2 & 0xff;

        int r = (int) ((rA * (255 - aB) + rB * aB) / 255);
        int g = (int) ((gA * (255 - aB) + gB * aB) / 255);
        int b = (int) ((bA * (255 - aB) + bB * aB) / 255);
        int a = (int) (0xff - ((0xff - aA) * (0xff - aB) / 0xff));

        return (a << 24) | (r << 16) | (g << 8) | b;

    }

    //topleft to bottomRight
    public static void dr1wLineRect(BufferedImage img, Color color, int lineSize, int x1, int y1, int x2, int y2) {
        int RGB = color.getRGB();
        fillArea(img, RGB, x1, y1, x1 + lineSize, y2);
        fillArea(img, RGB, x2 - lineSize, y1, x2, y2);

        fillArea(img, RGB, x1, y1, x2, y1 + lineSize);
        fillArea(img, RGB, x1, y2 - lineSize, x1, y2 - lineSize);
    }

}
