package com.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b256;
import org.ergoplatform.appkit.NetworkType;

import java.io.FilenameFilter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import mslinks.ShellLinkException;
import mslinks.ShellLinkHelper;
import scala.util.Try;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.netnotes.PriceAmount;
import com.netnotes.PriceCurrency;
import com.rfksystems.blake2b.Blake2b;
import com.satergo.extra.AESEncryption;

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

    public static byte[] digestFile(File file, String... instance) throws Exception, FileNotFoundException, IOException {
        String digestInstance = instance != null ? (instance.length == 0 ? Blake2b.BLAKE2_B_256 : instance[0]) : Blake2b.BLAKE2_B_256;

        final MessageDigest digest = MessageDigest.getInstance(digestInstance);

        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        byte[] hashBytes = digest.digest();

        return hashBytes;

    }

    public static JsonObject getNetworkTypeObject() {
        JsonObject getExplorerObject = new JsonObject();

        getExplorerObject.addProperty("subject", "GET_NETWORK_TYPE");

        return getExplorerObject;
    }

    public static JsonObject getExplorerInterfaceIdObject() {
        JsonObject getExplorerObject = new JsonObject();

        getExplorerObject.addProperty("subject", "GET_EXPLORER_INTERFACE_ID");

        return getExplorerObject;
    }

    public static byte[] digestBytesToBytes(byte[] bytes, String... instance) throws NoSuchAlgorithmException {
        String digestInstance = instance != null && instance.length > 0 ? instance[0] : Blake2b.BLAKE2_B_256;

        final MessageDigest digest = MessageDigest.getInstance(digestInstance);

        digest.update(bytes);

        return digest.digest();
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

    public static PriceAmount getAmountByString(String text, PriceCurrency priceCurrency) {
        if (text != null && priceCurrency != null) {
            text = text.replace(",", ".");

            char[] ch = text.toCharArray();

            for (int i = 0; i < ch.length; ++i) {
                if (Character.isDigit(ch[i])) {
                    ch[i] = Character.forDigit(Character.getNumericValue(ch[i]), 10);
                }
            }

            text = new String(ch);

            try {
                double parsedDouble = Double.parseDouble(text);
                return new PriceAmount(parsedDouble, priceCurrency);
            } catch (NumberFormatException ex) {

            }
        }
        return new PriceAmount(0, priceCurrency);
    }

    public static String formatCryptoString(double price, String target, boolean valid) {
        String formatedDecimals = String.format("%.2f", price);
        String priceTotal = valid ? formatedDecimals : "-.--";

        switch (target) {
            case "ERG":
                priceTotal = (valid ? String.format("%.3f", price) : "-.--") + " ERG";
                break;
            case "USD":
                priceTotal = "$ " + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎ " + priceTotal;
                break;
            case "BTC":
                priceTotal = (valid ? String.format("%.8f", price) : "-.--") + " BTC";
                break;
        }

        return priceTotal;
    }

    public static long getNowEpochMillis() {
        Instant instant = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public static long getNowEpochMillis(LocalDateTime now) {
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public static String formatDateTimeString(LocalDateTime localDateTime) {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a");

        return formater.format(localDateTime);
    }

    public static String formatTimeString(LocalDateTime localDateTime) {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(localDateTime);
    }

    public static LocalDateTime milliToLocalTime(long timestamp) {
        Instant timeInstant = Instant.ofEpochMilli(timestamp);

        return LocalDateTime.ofInstant(timeInstant, ZoneId.systemDefault());
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

    public static void returnObject(Object object, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() {

                return object;
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

    }

    public static void getUrlJson(String urlString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() throws JsonParseException, MalformedURLException, IOException {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String outputString = null;

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

                    if (progressIndicator != null) {
                        downloaded += (long) length;
                        updateProgress(downloaded, contentLength);
                    }
                }

                outputStream.close();
                outputString = outputStream.toString();

                JsonElement jsonElement = new JsonParser().parse(outputString);

                JsonObject jsonObject = jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;

                return jsonObject == null ? null : jsonObject;

            }

        };

        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(task.progressProperty());
        }

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public static String formatedBytes(long bytes, int decimals) {

        double k = 1024;
        int dm = decimals < 0 ? 0 : decimals;

        String[] sizes = new String[]{"Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

        int i = (int) Math.floor(Math.log((double) bytes) / Math.log(k));

        return String.format("%." + dm + "f", bytes / Math.pow(k, i)) + " " + sizes[i];

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

    public static TimeUnit stringToTimeUnit(String str) {
        switch (str.toLowerCase()) {
            case "μs":
            case "microsecond":
            case "microseconds":
                return TimeUnit.MICROSECONDS;
            case "ms":
            case "millisecond":
            case "milliseconds":
                return TimeUnit.MILLISECONDS;
            case "s":
            case "sec":
            case "second":
            case "seconds":
                return TimeUnit.SECONDS;
            case "min":
            case "minute":
            case "minutes":
                return TimeUnit.MINUTES;
            case "h":
            case "hour":
            case "hours":
                return TimeUnit.HOURS;
            case "day":
            case "days":
                return TimeUnit.DAYS;
            default:
                return null;
        }
    }

    public static String timeUnitToString(TimeUnit unit) {
        switch (unit) {
            case MICROSECONDS:
                return "μs";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "m";
            case HOURS:
                return "h";
            case DAYS:
                return "days";
            default:
                return "~";
        }
    }

    public static byte[] charsToBytes(char[] chars) {

        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static boolean checkJar(File jarFile) {
        boolean isJar = false;
        if (jarFile != null && jarFile.isFile()) {
            try {
                ZipFile zip = new ZipFile(jarFile);
                isJar = true;
                zip.close();
            } catch (Exception zipException) {

            }
        }
        return isJar;
    }

    public static void checkAddress(String addressString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<byte[]> task = new Task<byte[]>() {
            @Override
            public byte[] call() throws Exception {

                byte[] addressBytes = null;

                Try<byte[]> bytes = scorex.util.encode.Base58.decode(addressString);

                addressBytes = bytes.get();

                byte[] checksumBytes = new byte[]{addressBytes[addressBytes.length - 4], addressBytes[addressBytes.length - 3], addressBytes[addressBytes.length - 2], addressBytes[addressBytes.length - 1]};

                byte[] testBytes = new byte[addressBytes.length - 4];

                for (int i = 0; i < addressBytes.length - 4; i++) {
                    testBytes[i] = addressBytes[i];
                }

                byte[] hashBytes = Utils.digestBytesToBytes(testBytes, Blake2b.BLAKE2_B_256);

                if (!(checksumBytes[0] == hashBytes[0]
                        && checksumBytes[1] == hashBytes[1]
                        && checksumBytes[2] == hashBytes[2]
                        && checksumBytes[3] == hashBytes[3])) {
                    return null;
                }

                return addressBytes;
            }
        };

        task.setOnSucceeded(onSucceeded);

        task.setOnFailed(onFailed);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public static JsonObject getCmdObject(String subject) {
        JsonObject cmdObject = new JsonObject();
        cmdObject.addProperty("subject", subject);
        cmdObject.addProperty("timeStamp", getNowEpochMillis());
        return cmdObject;
    }

    public static void saveJson(SecretKey appKey, JsonObject listJson, File dataFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

        String tokenString = listJson.toString();

        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iV = new byte[12];
        secureRandom.nextBytes(iV);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

        cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

        byte[] encryptedData = cipher.doFinal(tokenString.getBytes());

        if (dataFile.isFile()) {
            Files.delete(dataFile.toPath());
        }

        FileOutputStream outputStream = new FileOutputStream(dataFile);
        FileChannel fc = outputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

        fc.write(byteBuffer);

        int written = 0;
        int bufferLength = 1024 * 8;

        while (written < encryptedData.length) {

            if (written + bufferLength > encryptedData.length) {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
            } else {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
            }

            written += fc.write(byteBuffer);
        }

        outputStream.close();

    }

    /*public static JsonObject readJsonFile(SecretKey appKey, Path filePath) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        byte[] fileBytes;

        fileBytes = Files.readAllBytes(filePath);

        byte[] iv = new byte[]{
            fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
            fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
            fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
        };

        ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

        JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, encryptedData)));
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }

        return null;
    }*/
    public static JsonObject readJsonFile(SecretKey appKey, Path filePath) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int bufferSize = 1024;
        if (bufferSize > channel.size()) {
            bufferSize = (int) channel.size();
        }
        ByteBuffer buff = ByteBuffer.allocate(bufferSize);

        while (channel.read(buff) > 0) {
            out.write(buff.array(), 0, buff.position());
            buff.clear();
        }

        channel.close();

        byte[] fileBytes = out.toByteArray();

        byte[] iv = new byte[]{
            fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
            fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
            fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
        };

        buff = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

        JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, buff)));
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }

        return null;

    }

    public static void writeEncryptedString(SecretKey secretKey, File dataFile, String jsonString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {

        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iV = new byte[12];
        secureRandom.nextBytes(iV);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

        if (dataFile.isFile()) {
            Files.delete(dataFile.toPath());
        }

        FileOutputStream outputStream = new FileOutputStream(dataFile);
        FileChannel fc = outputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

        fc.write(byteBuffer);

        int written = 0;
        int bufferLength = 1024 * 8;

        while (written < encryptedData.length) {

            if (written + bufferLength > encryptedData.length) {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
            } else {
                byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
            }

            written += fc.write(byteBuffer);
        }

        outputStream.close();

    }

    //logFile.toPath(), jsonObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND
    public static void writeString(Path filepath, String str, StandardOpenOption... openOptions) throws IOException {

        AsynchronousFileChannel asyncFile = AsynchronousFileChannel.open(filepath, openOptions);

        asyncFile.write(ByteBuffer.wrap("Some text to be written".getBytes()), 0);

        //Files.writeString(filepath, str, openOptions);
    }

}
