package com.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;

import javafx.beans.property.DoubleProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.FilenameFilter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import mslinks.ShellLinkException;
import mslinks.ShellLinkHelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.rfksystems.blake2b.Blake2b;

public class Utils {
    // Security.addProvider(new Blake2bProvider());

    public static String getBcryptHashString(String password) {
        SecureRandom sr;

        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {

            sr = new SecureRandom();
        }

        return BCrypt.with(BCrypt.Version.VERSION_2A, sr, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).hashToString(15, password.toCharArray());
    }

    public static boolean verifyBCryptPassword(String password, String hash) {
        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), hash.getBytes());

        return result.verified;
    }

    public static String digestFileToHex(File file, String... instance) throws Exception, FileNotFoundException, IOException {
        String digestInstance = instance == null ? Blake2b.BLAKE2_B_256 : instance[0];

        final MessageDigest digest = MessageDigest.getInstance(digestInstance);

        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        byte[] hashBytes = digest.digest();

        return Hex.encodeHexString(hashBytes);

    }

    public static Map<String, List<String>> parseArgs(String args[]) {

        final Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        for (int i = 0; i < args.length; i++) {
            final String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return null;
                }

                options = new ArrayList<>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                return null;
            }
        }

        return params;
    }

    public static String getLatestFileString(String directoryString) {

        if (!Files.isDirectory(Paths.get(directoryString))) {
            return "";
        }

        String fileFormat = "netnotes-0.0.0.jar";
        int fileLength = fileFormat.length();

        File f = new File(directoryString);

        File[] matchingFiles = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("netnotes") && name.endsWith(".jar");
            }
        });

        if (matchingFiles == null) {
            return "";
        }

        int start = 7;

        String latestString = "";

        String versionA = "0.0.0";

        for (File file : matchingFiles) {

            String fileName = file.getName();

            if (fileName.equals("netnotes.jar")) {
                if (versionA.equals("0.0.0")) {
                    latestString = "netnotes.jar";
                }
            } else if (fileName.length() == fileLength) {

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

                String versionB = fileName.substring(i + 1, end);

                if (versionB.matches("[0-9]+(\\.[0-9]+)*")) {

                    Version vA = new Version(versionA);
                    Version vB = new Version(versionB);

                    if (vA.compareTo(vB) == -1) {
                        versionA = versionB;
                        latestString = fileName;
                    } else if (latestString.equals("")) {
                        latestString = fileName;
                    }
                }

            }

        }

        return latestString;
    }

    public static void createLink(File target, File linkDir, String linkName) throws IOException, ShellLinkException {
        //ShellLinkHelper link = new ShellLinkHelper(new ShellLink());

        ShellLinkHelper.createLink(target.getAbsolutePath(), linkDir.getAbsolutePath() + "/" + linkName);

    }

    public static String formatDateTimeString(LocalDateTime localDateTime) {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a");

        return formater.format(localDateTime);
    }

    public static String readHexDecodeString(File file) {
        String fileHexString = null;

        try {
            fileHexString = file != null && file.isFile() ? Files.readString(file.toPath()) : null;
        } catch (IOException e) {

        }
        byte[] bytes = null;

        try {
            bytes = fileHexString != null ? Hex.decodeHex(fileHexString) : null;
        } catch (DecoderException e) {

        }

        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    public static void getUrlData(String urlString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<ByteArrayOutputStream> task = new Task<ByteArrayOutputStream>() {
            @Override
            public ByteArrayOutputStream call() {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    URL url = new URL(urlString);

                    String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

                    URLConnection con = url.openConnection();

                    con.setRequestProperty("User-Agent", USER_AGENT);

                    long contentLength = con.getContentLengthLong();
                    inputStream = con.getInputStream();

                    byte[] buffer = new byte[2048];

                    int length;
                    long downloaded = 0;

                    while ((length = inputStream.read(buffer)) != -1) {

                        outputStream.write(buffer, 0, length);
                        downloaded += (long) length;
                        updateProgress(downloaded, contentLength);
                    }

                    return outputStream;
                } catch (IOException e) {
                    return null;
                }

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.start();
    }

    public static BufferedImage greyScaleImage(BufferedImage img) {

        int height = img.getHeight();
        int width = img.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);

                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                //calculate average
                int avg = (r + g + b) / 3;

                //replace RGB value with avg
                p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                img.setRGB(x, y, p);
            }
        }

        return img;
    }

}
