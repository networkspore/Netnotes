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
        jarHash = Utils.digestFileToHex(jarFile, Blake2b.BLAKE2_B_256);
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
            return new Version(versionStr);
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
                return new Version(versionStr);
            }
        }

        return new Version("0.0.0");
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
