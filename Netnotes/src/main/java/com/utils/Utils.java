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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

import java.io.FilenameFilter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import mslinks.ShellLinkException;
import mslinks.ShellLinkHelper;

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
}
