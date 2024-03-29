package com.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
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

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URLConnection;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;

import java.io.FilenameFilter;
import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import mslinks.ShellLinkHelper;
import ove.crypto.digest.Blake2b;
import scala.util.Try;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.netnotes.App;
import com.netnotes.FreeMemory;
import com.netnotes.HashData;
import com.netnotes.PriceAmount;
import com.netnotes.PriceCurrency;
import com.satergo.extra.AESEncryption;

public class Utils {

    // Security.addProvider(new Blake2bProvider());
    public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

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

    public static byte[] digestFile(File file) throws  IOException {

        return digestFileBlake2b(file,32);
        
    }

    public static boolean findPathPrefixInRoots(String filePathString){
        File roots[] = File.listRoots();

        if(roots != null && roots.length > 0 && filePathString != null && filePathString.length() > 0){

            String appDirPrefix = FilenameUtils.getPrefix(filePathString);

            for(int i = 0; i < roots.length; i++){
                String rootString = roots[i].getAbsolutePath();

                if(rootString.startsWith(appDirPrefix)){
                    return true;
                }
            }
        }

        return false;
    }

    public static byte[] digestFileBlake2b(File file, int digestLength) throws IOException {
        final Blake2b digest = Blake2b.Digest.newInstance(digestLength);

        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[8 * 1024];
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

    public static byte[] digestBytesToBytes(byte[] bytes) {
        final Blake2b digest = Blake2b.Digest.newInstance(32);

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

    public static Version getFileNameVersion(String fileName){
        int end = fileName.length() - 4;

        int start = fileName.indexOf("-");

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

        String versionString = fileName.substring(i + 1, end);

 
        if (versionString.matches("[0-9]+(\\.[0-9]+)*")) {
            Version version = null;
            try{
                version = new Version(versionString);
            }catch(IllegalArgumentException e){

            }
            return version;
        }
        return null;
    }

    public static void createLink(File targetFile, File linkDir, String linkName) throws IOException, ShellLinkException {
        //ShellLinkHelper link = new ShellLinkHelper(new ShellLink());

        ShellLinkHelper.createLink(targetFile.getAbsolutePath(), linkDir.getAbsolutePath() + "/" + linkName);

    }

    public static void createLink(File targetFile, String args, File linkDir, String linkName) throws IOException, ShellLinkException {
        //ShellLinkHelper link = new ShellLinkHelper(new ShellLink());
        String target = ShellLinkHelper.resolveEnvVariables(targetFile.getAbsolutePath());
   
        ShellLinkHelper helper = new ShellLinkHelper(new ShellLink());
        String[] parts = target.split(":");
        if (parts.length != 2) {
            throw new ShellLinkException("Wrong path '" + target + "'");
        }
        helper.setLocalTarget(parts[0], parts[1]);
        ShellLink link = helper.getLink();
        
        link.setCMDArgs(args);

       Path savingPath = new File(linkDir.getAbsolutePath() + "/" + linkName).toPath();
        if (Files.isDirectory(savingPath)) {
            throw new IOException("can't save ShellLink to \"" + savingPath + "\" because there is a directory with this name");
        }

        link.setLinkFileSource(savingPath);

        Path savingDir = savingPath.getParent();
        try {
            Path targetPath = Paths.get(link.resolveTarget());
            if (!link.getHeader().getLinkFlags().hasRelativePath()) {
                // this will always be false on linux
                if (savingDir.getRoot().equals(targetPath.getRoot())) {
                    link.setRelativePath(savingDir.relativize(targetPath).toString());
                }
            }

            if (!link.getHeader().getLinkFlags().hasWorkingDir()) {
                // this will always be false on linux
                if (Files.isRegularFile(targetPath)) {
                    link.setWorkingDir(targetPath.getParent().toString());
                }
            }
        } catch (InvalidPathException e) {
            // skip automatic relative path and working dir if path is some special folder
        }

        Files.createDirectories(savingDir);
        link.serialize(Files.newOutputStream(savingPath));
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
    
    public static String currencySymbol(String currency){
         switch (currency) {
            case "ERG":
                return "Σ";
            case "USD":
                return "$";
            case "USDT":
                return "$";
            case "EUR":
                return "€‎";
             
            case "BTC":
                return "฿";
        }
        return currency;
    }

    public static String formatCryptoString(BigDecimal price, String target, int precision, boolean valid) {
       String formatedDecimals = String.format("%."+precision+"f", price);
        String priceTotal = valid ? formatedDecimals : "-";
    
      
     
        switch (target) {
            case "ERG":
                priceTotal = priceTotal + " ERG";
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + priceTotal;
                break;
        }

        return priceTotal;
    }

    public static String formatCryptoString(double price, String target, int precision, boolean valid) {
        String formatedDecimals = String.format("%."+precision+"f", price);
        String priceTotal = valid ? formatedDecimals : "-";
    
        switch (target) {
            case "ERG":
                priceTotal = priceTotal + " ERG";
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + priceTotal;
                break;
        }

        return priceTotal;
    }


    public static String formatCryptoString(double price, String target, boolean valid) {
        String formatedDecimals = String.format("%.2f", price);
        String priceTotal = valid ? formatedDecimals : "-.--";

        

        switch (target) {
            case "ERG":
                priceTotal = (valid ? String.format("%.3f", price) : "-.--") + " ERG";
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "USDT":
                priceTotal = priceTotal + " USDT";
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + (valid ? String.format("%.8f", price) : "-.--");
                break;
        }

        return priceTotal;
    }
    public static String truncateText(String text,FontMetrics metrics, double width) {
       
        String truncatedString = text.substring(0, 5) + "..";
        if (text.length() > 3) {
            int i = text.length() - 3;
            truncatedString = text.substring(0, i) + "..";

            while (metrics.stringWidth(truncatedString) > width && i > 1) {
                i = i - 1;
                truncatedString = text.substring(0, i) + "..";

            }
        }
        return truncatedString;
    }

    public static int measureString(String str, java.awt.Font font){
        
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.dispose();
        return fm.stringWidth(str);
    }

    public static String formatCryptoString(PriceAmount priceAmount, boolean valid) {
         int precision = priceAmount.getCurrency().getFractionalPrecision();
        DecimalFormat df = new DecimalFormat("0");
        df.setMaximumFractionDigits(precision);

        String formatedDecimals = df.format(priceAmount.getDoubleAmount());
        String priceTotal = valid ? formatedDecimals : "-.--";

        

        switch (priceAmount.getCurrency().getSymbol()) {
            case "ERG":
                priceTotal = "Σ"+ priceTotal;
                break;
            case "USD":
                priceTotal = "$" + priceTotal;
                break;
            case "EUR":
                priceTotal = "€‎" + priceTotal;
                break;
            case "BTC":
                priceTotal ="฿" + priceTotal;
                break;
            default:
                priceTotal = priceTotal + " " + priceAmount.getCurrency().getSymbol();
        }

        return priceTotal;
    }

    public static long getNowEpochMillis() {

        return System.currentTimeMillis();
    }

    public static long getNowEpochMillis(LocalDateTime now) {
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public static String formatDateTimeString(LocalDateTime localDateTime) {

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss.SSS a");

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

        if (bytes == 0) {
            return "0 Bytes";
        }

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

    
    public static Image checkAndLoadImage(String imageString, HashData hashData) {
        if(imageString != null ){
            
            if(imageString.startsWith(App.ASSETS_DIRECTORY + "/")){
                return new Image(imageString);
            }
            File checkFile = new File(imageString);

            try {
                HashData checkFileHashData = new HashData(checkFile);
                /*try {
                    Files.writeString(logFile.toPath(), "\nhashString: " +checkFileHashData.getHashStringHex()+ " hashDataString: " + hashData.getHashStringHex(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }*/
                if (checkFileHashData.getHashStringHex().equals(hashData.getHashStringHex())) {
                    return getImageByFile(checkFile);
                }
            } catch (Exception e) {
                try {
                    Files.writeString(new File("netnotes-log.txt").toPath(), "\n" + e, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e2) {

                }
            }
        }

  
        return new Image("/assets/unknown-unit.png");
        

    }

    public static Image getImageByFile(File file) throws IOException {
        if (file != null && file.isFile()) {
            String contentType = null;
            
                contentType = Files.probeContentType(file.toPath());
                contentType = contentType.split("/")[0];
                if (contentType != null && contentType.equals("image")) {
                    FileInputStream fis = new FileInputStream(file);
                    Image img = new Image(fis);
                    fis.close();
                    return img;
                    
                }
              
        }
         return null;
    }

    public static String removeInvalidChars(String str)
    {
        return str.replaceAll("[^a-zA-Z0-9\\.\\-]", "");
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

                byte[] hashBytes = Utils.digestBytesToBytes(testBytes);

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

    public static int getRandomInt(int min, int max) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();

        return secureRandom.nextInt(min, max);
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
    public static JsonObject readJsonFile(SecretKey appKey, File file) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
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
    
    public static String readStringFile(SecretKey appKey, File file)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, IOException {

        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
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

        byte[] iv = new byte[] {
                fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
        };

        buff = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

        return new String(AESEncryption.decryptData(iv, appKey, buff));


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

    public static void moveFileAndHash(File inputFile, File outputFile, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                long contentLength = -1;

                if (inputFile != null && inputFile.isFile() && outputFile != null && !inputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    contentLength = Files.size(inputFile.toPath());
                } else {
                    return null;
                }

                FileOutputStream outputStream = new FileOutputStream(outputFile);

                 final Blake2b digest = Blake2b.Digest.newInstance(32);

                FileInputStream inputStream = new FileInputStream(inputFile);

                byte[] buffer = new byte[8 * 1024];

                int length;
                long copied = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    digest.update(buffer, 0, length);

                    copied += (long) length;
                    updateProgress(length, contentLength);

                }
                outputStream.close();
                inputStream.close();

                byte[] hashbytes = digest.digest();

                HashData hashData = new HashData(hashbytes);

                outputStream.close();

                return contentLength == copied ? hashData : null;

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
    
    public static void getUrlJsonArray(String urlString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<JsonArray> task = new Task<JsonArray>() {
            @Override
            public JsonArray call() throws JsonParseException, MalformedURLException, IOException {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                String outputString = null;

                URL url = new URL(urlString);

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

                JsonArray jsonArray = jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;

                return jsonArray == null ? null : jsonArray;

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

    public static void getUrlFileHash(String urlString, File outputFile, EventHandler<WorkerStateEvent> onSucceeded,
            EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator,
            SimpleBooleanProperty cancel) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                if (outputFile == null) {
                    return null;
                }
                Files.deleteIfExists(outputFile.toPath());

                FileOutputStream outputStream = new FileOutputStream(outputFile);

                final Blake2b digest = Blake2b.Digest.newInstance(32);

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();
                InputStream inputStream = con.getInputStream();

                byte[] buffer = new byte[8 * 1024];

                int length;
                long downloaded = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    outputStream.write(buffer, 0, length);
                    digest.update(buffer, 0, length);
                    if (progressIndicator != null) {
                        downloaded += (long) length;
                        updateProgress(downloaded, contentLength);
                    }
                    if (cancel.get()) {
                        inputStream.close();
                        outputStream.close();
                        return null;

                    }
                }

                byte[] hashbytes = digest.digest();

                HashData hashData = new HashData(hashbytes);

                outputStream.close();

                return contentLength == downloaded ? hashData : null;

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

    public static String[] getShellCmd(String execCmd){
        return new String[]{"cmd", "/c", execCmd};
    }

    public static void pingIP(String ip, SimpleStringProperty status, SimpleStringProperty updated, SimpleBooleanProperty available) throws Exception{
     
        String[] cmd = { "cmd", "/c", "ping", ip };

        Process proc = Runtime.getRuntime().exec(cmd);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        List<String> javaOutputList = new ArrayList<String>();

        String s = null;

        while ((s = stdInput.readLine()) != null) {
            javaOutputList.add(s);

            String timeString = "time=";
            int indexOftimeString = s.indexOf(timeString);

            if (s.indexOf("timed out") > 0) {
                Platform.runLater(()->available.set(false));
                status.set("Timed out");
                updated.set(Utils.formatDateTimeString(LocalDateTime.now()));
            }

            if (indexOftimeString > 0) {
                int lengthOftime = timeString.length();

                int indexOfms = s.indexOf("ms");

                available.set(true);

                String time = s.substring(indexOftimeString + lengthOftime, indexOfms + 2);

                status.set("Ping: " + time);
                updated.set(Utils.formatDateTimeString(LocalDateTime.now()));
            }

            String avgString = "Average = ";
            int indexOfAvgString = s.indexOf(avgString);

            if (indexOfAvgString > 0) {
                int lengthOfAvg = avgString.length();

                String avg = s.substring(indexOfAvgString + lengthOfAvg);

                status.set("Average: " + avg);

                updated.set(Utils.formatDateTimeString(LocalDateTime.now()));

            }

        }
        
           

    }

    public static void getWin32_BaseboardHashData(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws IOException, InterruptedException {
                // File psbaseboardIdLogFile = new File("psbaseboardId-log.txt");
           
                //et-CimInstance -Class Win32_BaseBoard | Format-Table Manufacturer, Product, SerialNumber, Version 
    
                String[] psCmd = {"powershell", "Get-CimInstance", "-ClassName", "Win32_BaseBoard"};
                Process psProc = Runtime.getRuntime().exec(psCmd);

                BufferedReader psStderr = new BufferedReader(new InputStreamReader(psProc.getErrorStream()));
                BufferedReader psStdInput = new BufferedReader(new InputStreamReader(psProc.getInputStream()));

                String psInput = null;
                
                String colProduct = null;
                String colManufacturer = null;
                String colSerialNumber = null;
               

                while ((psInput = psStdInput.readLine()) != null) {
                    int index = psInput.indexOf(" ");
                    if(index > 1){
                        String colName = psInput.substring(0, index);

                        switch(colName){
                            case "Product":
                                index = psInput.indexOf(":", index);
                                colProduct = psInput.substring(index+2);
                                break;
                            case "Manufacturer":
                                index = psInput.indexOf(":", index);
                                colManufacturer = psInput.substring(index+2);
                                break;
                            case "SerialNumber":
                                index = psInput.indexOf(":", index);
                                colSerialNumber = psInput.substring(index+2);
                                break;
                        
                        }
                    }
            
                }



                while ((psStderr.readLine()) != null) {

                // Files.writeString(logFile.toPath(), "\nps err: " + pserr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    return null;
                }

                psProc.waitFor();

                if(colProduct != null && colManufacturer != null  && colSerialNumber != null){
                    String idString = colProduct + colManufacturer  + colSerialNumber;
                    // Files.writeString(psbaseboardIdLogFile.toPath(), idString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    return new HashData(Utils.digestBytesToBytes(Utils.charsToBytes(idString.toCharArray())));
                    
                    
                }

              
            
                return null;
               
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public static void getWin32_BiosHashData(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws IOException, InterruptedException {
                 //File psbaseboardIdLogFile = new File("psbiosId-log.txt");

    
                String[] psCmd = {"powershell", "Get-CimInstance", "-ClassName", "Win32_Bios"};
                Process psProc = Runtime.getRuntime().exec(psCmd);

                BufferedReader psStderr = new BufferedReader(new InputStreamReader(psProc.getErrorStream()));
                BufferedReader psStdInput = new BufferedReader(new InputStreamReader(psProc.getInputStream()));

                String psInput = null;
                
                String colVersion = null;
                String colManufacturer = null;
                String colSerialNumber = null;

               // psProc.isAlive()
               

                while ((psInput = psStdInput.readLine()) != null) {
                    int index = psInput.indexOf(" ");
                    if(index > 1){
                        String colName = psInput.substring(0, index);

                        switch(colName){
                            case "Version":
                                index = psInput.indexOf(":", index);
                                colVersion = psInput.substring(index+2);
                                break;
                            case "Manufacturer":
                                index = psInput.indexOf(":", index);
                                colManufacturer = psInput.substring(index+2);
                                break;
                            case "SerialNumber":
                                index = psInput.indexOf(":", index);
                                colSerialNumber = psInput.substring(index+2);
                                break;
                        
                        }
                    }
            
                }



                while ((psStderr.readLine()) != null) {

                // Files.writeString(logFile.toPath(), "\nps err: " + pserr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    return null;
                }

                psProc.waitFor();

                if(colVersion != null && colManufacturer != null  && colSerialNumber != null){
                    String idString = colVersion + colManufacturer  + colSerialNumber;
                   
                    return new HashData(Utils.digestBytesToBytes(Utils.charsToBytes(idString.toCharArray())));
                    
                    
                }

              
            
                return null;
               
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public static void centerStage(Stage stage, Rectangle screenRectangle){
        stage.setX(screenRectangle.getWidth()/2 - stage.getWidth()/2);
        stage.setY(screenRectangle.getHeight()/2 - stage.getHeight()/2);
    }

    

    public static String[] findPIDs(String jarname){
          try {
          //  File logFile = new File("wmicTerminate-log.txt");
            //Get-Process | Where {$_.ProcessName -Like "SearchIn*"}
         //   String[] wmicCmd = {"powershell", "Get-Process", "|", "Where", "{$_.ProcessName", "-Like", "'*" +  jarname+ "*'}"};
            Process psProc = Runtime.getRuntime().exec("powershell Get-WmiObject Win32_Process | WHERE {$_.CommandLine -Like '*"+jarname+"*' } | Select ProcessId");

            BufferedReader psStderr = new BufferedReader(new InputStreamReader(psProc.getErrorStream()));
            //String pserr = null;


            ArrayList<String> pids = new ArrayList<>();

            BufferedReader psStdInput = new BufferedReader(new InputStreamReader(psProc.getInputStream()));

            String psInput = null;
           // boolean gotInput = false;
            //   int pid = -1;
               
            while ((psInput = psStdInput.readLine()) != null) {
              //  
              //  gotInput = true;
                psInput.trim();
                if(!psInput.equals("") && !psInput.startsWith("ProcessId") && !psInput.startsWith("---------")){
                    
                    pids.add(psInput);
                }
            }
            
            String  pserr = null;
            while ((pserr = psStderr.readLine()) != null) {
                try {
                    Files.writeString(new File("netnotes-log.txt").toPath(), "\npsPID err: " + pserr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
                
                }
               // Files.writeString(logFile.toPath(), "\nps err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
            }

            psProc.waitFor();
            if( pids.size() > 0){
                String[] pidArray = new String[pids.size()];

                pidArray =  pids.toArray(pidArray);
                
                return pidArray;
            }else{
                return null;
            }
            

        } catch (Exception e) {
              try {
                Files.writeString(new File("netnotes-log.txt").toPath(), "\npsPID: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
             
            }
             
            return null;
        }
   
    }

     public static boolean sendTermSig(String fileName){
          try {
          //  File logFile = new File("wmicTerminate-log.txt");
            //Get-Process | Where {$_.ProcessName -Like "SearchIn*"}
         //   String[] wmicCmd = {"powershell", "Get-Process", "|", "Where", "{$_.ProcessName", "-Like", "'*" +  jarname+ "*'}"};
            
            String[] pids = findPIDs(fileName);

            for(String pid : pids){
                Process psProc = Runtime.getRuntime().exec("powershell stop-process -id " + pid );
                psProc.waitFor();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
   
    }

    public static String getIncreseSwapUrl(){
        return "https://learn.microsoft.com/en-us/troubleshoot/windows-client/performance/how-to-determine-the-appropriate-page-file-size-for-64-bit-versions-of-windows";
    }

     public static void cmdTaskKill(String pid){
          try {

            Process psProc = Runtime.getRuntime().exec("cmd /c taskkill /PID " + pid );


            psProc.waitFor();



        } catch (Exception e) {
            
        }
   
    }

   

    public static int findMenuItemIndex(ObservableList<MenuItem> list, String id){
        if(id != null){
            for(int i = 0; i < list.size() ; i++){
                MenuItem menuItem = list.get(i);
                Object userData = menuItem.getUserData();

                if(userData != null && userData instanceof String){
                    String menuItemId = (String) userData;
                    if(menuItemId.equals(id)){
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    public static FreeMemory getFreeMemory(){
        // systeminfo | Select-String 'Total Physical Memory:', 'Available Physical Memory:', 'Virtual Memory: Max Size:', 'Virtual Memory: In Use:'
        try {

            Process psProc = Runtime.getRuntime().exec("powershell get-wmiobject win32_operatingsystem | select FreePhysicalMemory, FreeVirtualMemory, FreeSpaceInPagingFiles, TotalVirtualMemorySize, TotalVisibleMemorySize, SizeStoredInPagingFiles");

            BufferedReader psStderr = new BufferedReader(new InputStreamReader(psProc.getErrorStream()));
            // String pserr = null;


            BufferedReader psStdInput = new BufferedReader(new InputStreamReader(psProc.getInputStream()));

            String psInput = null;
            
            long swapTotal = -1;
            long swapFree = -1;
            long memFree = -1;
            long memAvailable = -1;
            long memTotal = -1;

            final String delimiter = " ";
            final String valueDelim = ": ";
            final int valueDelimSize = valueDelim.length();

            long consumedVMem = -1;

            while ((psInput = psStdInput.readLine()) != null) {
                int spaceIndex = psInput.indexOf(delimiter);
                int valueIndex = psInput.indexOf(valueDelim);
                String rowStr = psInput.substring(0, spaceIndex);
                long value = Long.parseLong(psInput.substring(valueIndex + valueDelimSize));

                switch(rowStr){
                    case "SizeStoredInPagingFiles":
                        consumedVMem = value;
                    break;
                    case "FreePhysicalMemory":
                        memFree = value;
                    break;
                    case "FreeSpaceInPagingFiles":
                        swapFree = value;
                    break;
                    case "TotalVirtualMemorySize":
                        swapTotal = value;
                        memAvailable = value;
                        
                    break;
                    case "TotalVisibleMemorySize":
                        memTotal = value;
                    break;

                }
            }

            memAvailable = memAvailable > 0 ? memAvailable - consumedVMem : memAvailable;
          
            String errStr = psStderr.readLine();
            psProc.waitFor();
          
            if(errStr == null){
                return new FreeMemory(swapTotal, swapFree, memFree, memAvailable, memTotal);
            }


        } catch (Exception e) {
            try {
                Files.writeString(new File("netnotes-log.txt").toPath(), "\ngetFreeMemory: " + e.toString(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }

            return null;
        }
        try {
            Files.writeString(new File("netnotes-log.txt").toPath(), "\ngetFreeMemory: null",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e1) {

        }

        return null;
    }

    public static boolean sendKillSig(String jarName) {
        try {
          //  File logFile = new File("wmicTerminate-log.txt");
     
            String[] wmicCmd = {"cmd", "/c", "wmic", "Path", "win32_process", "Where", "\"CommandLine", "Like", "'%" + jarName + "%'\"", "Call", "Terminate"};
            Process wmicProc = Runtime.getRuntime().exec(wmicCmd);

            BufferedReader wmicStderr = new BufferedReader(new InputStreamReader(wmicProc.getErrorStream()));
            //String wmicerr = null;


         

            BufferedReader wmicStdInput = new BufferedReader(new InputStreamReader(wmicProc.getInputStream()));

           // String wmicInput = null;
            boolean gotInput = false;

            while ((wmicStdInput.readLine()) != null) {
            
            
                gotInput = true;
            }

            while ((wmicStderr.readLine()) != null) {

               // Files.writeString(logFile.toPath(), "\nwmic err: " + wmicerr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return false;
            }

            wmicProc.waitFor();

            if (gotInput) {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static void openDir(File file) throws Exception  {

        java.awt.Desktop.getDesktop().browseFileDirectory(file);

    }
    
    public static URL getLocation(final Class<?> c) {

        if (c == null) {
            return null; // could not load the class
        }
        // try the easy way first
        try {
            final URL codeSourceLocation = c.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceLocation != null) {
                return codeSourceLocation;
            }
        } catch (final SecurityException e) {
            // NB: Cannot access protection domain.
        } catch (final NullPointerException e) {
            // NB: Protection domain or code source is null.
        }

        // NB: The easy way failed, so we try the hard way. We ask for the class
        // itself as a resource, then strip the class's path from the URL string,
        // leaving the base path.
        // get the class's raw resource path
        final URL classResource = c.getResource(c.getSimpleName() + ".class");
        if (classResource == null) {
            return null; // cannot find class resource
        }
        final String url = classResource.toString();
        final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
        if (!url.endsWith(suffix)) {
            return null; // weird URL
        }
        // strip the class's path from the URL string
        final String base = url.substring(0, url.length() - suffix.length());

        String path = base;

        // remove the "jar:" prefix and "!/" suffix, if present
        if (path.startsWith("jar:")) {
            path = path.substring(4, path.length() - 2);
        }

        try {
            return new URL(path);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the given {@link URL} to its corresponding {@link File}.
     * <p>
     * This method is similar to calling {@code new File(url.toURI())} except
     * that it also handles "jar:file:" URLs, returning the path to the JAR
     * file.
     * </p>
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a
     * file.
     */
    public static File urlToFile(final URL url) {
        return url == null ? null : urlToFile(url.toString());
    }

    /**
     * Converts the given URL string to its corresponding {@link File}.
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a
     * file.
     */
    public static File urlToFile(final String url) {
        String path = url;
        if (path.startsWith("jar:")) {
            // remove "jar:" prefix and "!/" suffix
            final int index = path.indexOf("!/");
            path = path.substring(4, index);
        }

        try {

            if (path.matches("file:[A-Za-z]:.*")) {
                path = "file:/" + path.substring(5);
            }
            return new File(new URL(path).toURI());
        } catch (final MalformedURLException e) {
            // NB: URL is not completely well-formed.

        } catch (final URISyntaxException e) {
            // NB: URL is not completely well-formed.
        }
        if (path.startsWith("file:")) {
            // pass through the URL as-is, minus "file:" prefix
            path = path.substring(5);
            return new File(path);
        }
        throw new IllegalArgumentException("Invalid URL: " + url);
    }

}
