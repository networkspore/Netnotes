package com.netnotes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class InvertEffect extends Effects {

    private static File logFile = new File("InvertEffect-log.txt");
    public static String NAME = "INVERT";

    public InvertEffect() {
        super(NAME);
    }

    public InvertEffect(String id) {
        super(id, NAME);
    }

    @Override
    public void applyEffect(BufferedImage img) {
        invertRGB(img);
    }

    public static void invertRGB(BufferedImage img) {
        try {
            Files.writeString(logFile.toPath(), "inverting", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = img.getRGB(x, y);

                int a = (rgba >> 24) & 0xff;
                int r = (rgba >> 16) & 0xff;
                int g = (rgba >> 8) & 0xff;
                int b = rgba & 0xff;

                r = (0xFF - r);
                g = (0xFF - g);
                b = (0xFF - b);

                int p = (a << 24) | (r << 16) | (g << 8) | b;

                img.setRGB(x, y, p);
            }
        }
    }

}
