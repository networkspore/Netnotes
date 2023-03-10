package com.launcher;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Security;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rfksystems.blake2b.security.Blake2bProvider;

public class Main {

    public static final String currentAppJarEquals = "currentAppJar=";

    public static final String currentLauncherData = "currentLauncherData=";
    public static final String currentJavaVersionEquals = "currentJavaVersion=";

    public static final String setupUpdates = "setupUpdates";
    public static final String visitGitHub = "visitGitHub";
    public static final String firstRun = "FirstRun";

    public static final String latestReleaseURLstring = "https://github.com/networkspore/Netnotes/releases/latest/download/releaseInfo.json";

    public static final String currentDirectory = System.getProperty("user.dir");
    public static final String settingsFileName = "settings.conf";

    public static void main(String[] args) throws IOException {
        launch();
    }

    public static void launch() throws IOException {
        Security.addProvider(new Blake2bProvider());
        JsonObject launcherData = null;

        Version javaVersion = checkJava();

        // Path appDataPath = Paths.get(appDataDirectory);
        Path settingsPath = Paths.get(currentDirectory + "\\" + settingsFileName);

        boolean isSettings = Files.isRegularFile(settingsPath);

        if (isSettings) {
            try {
                String jsonString = Files.readString(settingsPath);
                launcherData = new JsonParser().parse(jsonString).getAsJsonObject();
                String appKey = launcherData.get("appKey").getAsString();;
                Boolean updates = launcherData.get("updates").getAsBoolean();;
                if (!appKey.startsWith("$2a$15$")) {
                    launcherData = null;
                }

                launcherData.get("networks").getAsString();
            } catch (Exception e) {
                launcherData = null;
            }
        }

        String currentDirectoyJar = Utils.getLatestFileString(currentDirectory);
        String appJarArg = currentAppJarEquals + currentDirectoyJar;
        String javaVersionArg = currentJavaVersionEquals + (javaVersion == null ? "" : javaVersion.get());

        if (launcherData == null) {

            openSetup(firstRun, appJarArg, javaVersionArg);

        } else {

            boolean isJar = !currentDirectoyJar.equals("");

            if (javaVersion != null && isJar) {

                try {
                    openJar(currentDirectoyJar);
                } catch (IOException e) {
                    openSetup(visitGitHub, javaVersionArg, appJarArg);
                }

            } else {
                openSetup(visitGitHub, javaVersionArg, appJarArg);
            }

        }

    }

    public static Version checkJava() {

        String[] cmd = {"java", "--version"};

        try {
            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            List<String> javaOutputList = new ArrayList<String>();

            String s = null;
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

    private static void openSetup(String... args) {

        Application.launch(Setup.class, args);

    }

    public static void openJar(String jarFilePathString) throws IOException {
        String[] cmdString;

        cmdString = new String[]{"cmd", "/c", "javaw", "-jar", jarFilePathString};

        Runtime.getRuntime().exec(cmdString);

    }

}
