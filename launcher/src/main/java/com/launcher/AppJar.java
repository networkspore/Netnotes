package com.launcher;

import java.io.File;

import com.rfksystems.blake2b.Blake2b;

public class AppJar {

    private String jarLocation;
    private Version jarVersion;
    private String jarHash;
    private File jarFile;

    public AppJar(String appJarLocation, Version appJarVersion, String appJarhash) {
        jarLocation = appJarLocation;
        jarVersion = appJarVersion;
        jarHash = appJarhash;
        jarFile = new File(appJarLocation);
    }

    public AppJar(File jarFile) {

        jarVersion = getVersionFromFileName(jarFile.getName());

        jarLocation = jarFile.getAbsolutePath();
        try {
            jarHash = Utils.digestFileToHex(jarFile);
        } catch (Exception e) {

        }
    }

    public File getFile() {
        return jarFile;
    }

    public static Version getVersionFromFileName(String fileName) {

        String fileFormat = "netnotes-0.0.0.jar";
        int fileLength = fileFormat.length();
        int start = 7;

        String versionStr = "0.0.0";

        if (fileName.equals("netnotes.jar")) {
            try {
                return new Version(versionStr);
            } catch (Exception e) {

            }
        }

        if (fileName.length() == fileLength) {

            int end = fileName.length() - 4;

            int i = end;
            char p = '.';

            while (i > start) {
                char c = fileName.charAt(i);
                if (Character.isDigit(c) || Character.compare(c, p) == 0) {
                    i--;
                } else {
                    break;
                }

            }

            versionStr = fileName.substring(i + 1, end);

            if (versionStr.matches("[0-9]+(\\.[0-9]+)*")) {
                try {
                    return new Version(versionStr);
                } catch (Exception e) {

                }
            }
        }
        Version zero = null;
        try {
            zero = new Version("0.0.0");
        } catch (Exception e) {

        }
        return zero;
    }

    public String getLocation() {
        return jarLocation;
    }

    public Version getVersion() {
        return jarVersion;
    }

    public String getHash() {
        return jarHash;
    }
}
