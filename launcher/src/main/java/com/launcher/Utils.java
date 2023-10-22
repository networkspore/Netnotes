package com.launcher;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.text.Font;
import ove.crypto.digest.Blake2b;

import java.io.FilenameFilter;

public class Utils {

    public static Font txtFont = Font.font("OCR A Extended", 15);

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

    public static byte[] digestFile(File file) throws  IOException {

        return digestFileBlake2b(file,32);
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
    
    public static Version checkJava() {

        String[] cmd = {"java", "--version"};

        try {
            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            List<String> javaOutputList = new ArrayList<String>();

            String s = null;

            while (stdErr.readLine() != null) {
                return null;
            }

            while ((s = stdInput.readLine()) != null) {
                javaOutputList.add(s);
            }

            String[] splitStr = javaOutputList.get(0).trim().split("\\s+");

            Version jV = new Version(splitStr[1].replaceAll("/[^0-9.]/g", ""));

            return jV;

        } catch (Exception e) {
            return null;
        }

    }
    
     public static boolean checkJar(File jarFile) {
     
        if (jarFile != null && jarFile.isFile()) {
            try {
                ZipFile zip = new ZipFile(jarFile);
                zip.close();
                return true;
            } catch (ZipException  zE) {
           
            } catch (IOException ioE){
              
            }
        }
        return false;
    }

    public static File getLatestFile(File directory) {

        if (!Files.isDirectory(directory.toPath())) {
            return null;
        }

        String fileFormat = "netnotes-0.0.0.jar";
        int fileLength = fileFormat.length();

     

        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("netnotes") && name.endsWith(".jar");
            }
        });

        if (matchingFiles == null) {
            return null;
        }

        int start = 7;

        String latestString = "";
        Version versionA = null;

        try {
            versionA = new Version("0.0.0");
        } catch (Exception e) {

        }

        for (File file : matchingFiles) {

            String fileName = file.getName();

            if (fileName.equals("netnotes.jar")) {
                if (versionA.get().equals("0.0.0")) {
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

                String subString = fileName.substring(i + 1, end);

                if (subString.matches("[0-9]+(\\.[0-9]+)*")) {
                    Version versionB = null;
                    try {
                        versionB = new Version(subString);
                    } catch (Exception e) {

                    }
                    if (versionB != null && versionA.compareTo(versionB) == -1) {
                        versionA = versionB;
                        latestString = fileName;
                    } else if (latestString.equals("")) {
                        latestString = fileName;
                    }
                }

            }

        }

        return latestString.equals("") ? null : new File(directory.getAbsolutePath() + "/" + latestString);
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

    public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

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
    public static byte[] digestBytesToBytes(byte[] bytes) {
        final Blake2b digest = Blake2b.Digest.newInstance(32);

        digest.update(bytes);

        return digest.digest();
    }
    
    public static byte[] charsToBytes(char[] chars) {

        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
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

     public static void getUrlFileHash(String urlString, File outputFile, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<HashData> task = new Task<HashData>() {
            @Override
            public HashData call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                if (outputFile == null) {
                    return null;
                }
                Files.deleteIfExists(outputFile.toPath());

                InputStream inputStream = null;
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                final Blake2b digest = Blake2b.Digest.newInstance(32);

                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();
                inputStream = con.getInputStream();

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

    public static void getUrlFile(String urlString, File outputFile, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed, ProgressIndicator progressIndicator) {

        Task<Boolean> task = new Task<Boolean>() {
            @Override
            public Boolean call() throws NoSuchAlgorithmException, MalformedURLException, IOException {
                if (outputFile == null) {
                    return null;
                }
                Files.deleteIfExists(outputFile.toPath());

                InputStream inputStream = null;
                FileOutputStream outputStream = new FileOutputStream(outputFile);


                URL url = new URL(urlString);

                URLConnection con = url.openConnection();

                con.setRequestProperty("User-Agent", USER_AGENT);

                long contentLength = con.getContentLengthLong();
                inputStream = con.getInputStream();

                byte[] buffer = new byte[8 * 1024];

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

                return contentLength == downloaded ? true : false;

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


    

}
