package com.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Security;

import javafx.application.Application;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rfksystems.blake2b.security.Blake2bProvider;

public class Launcher {

    private static File logFile = new File("launcher-log.txt");

    public static final String currentAppJarEquals = "currentAppJar=";

    public static final String currentLauncherData = "currentLauncherData=";
    public static final String currentJavaVersionEquals = "currentJavaVersion=";

    public static final String setupUpdates = "setupUpdates";
    public static final String visitGitHub = "visitGitHub";
    public static final String firstRun = "FirstRun";

    public static final String latestReleaseURLstring = "https://github.com/networkspore/Netnotes/releases/latest/download/releaseInfo.json";

    public static final String currentDirectory = System.getProperty("user.dir");
    public static final String settingsFileName = "settings.conf";

    public static final String CMD_SHOW_APPSTAGE = "SHOW_APPSTAGE";
    public static final long EXECUTION_TIME = 100;

    public static final String NOTES_ID = "launcher";

    public static void main(String[] args) throws IOException {
        launch();
    }

    public static void launch() throws IOException {
        Security.addProvider(new Blake2bProvider());
        JsonObject launcherData = null;

        Version javaVersion = checkJava();

        // Path appDataPath = Paths.get(appDataDirectory);
        Path settingsPath = Paths.get(currentDirectory + "\\" + settingsFileName);

        File notesDir = new File(currentDirectory + "\\notes");
        File outDir = new File(currentDirectory + "\\out");

        if (!notesDir.isDirectory()) {
            Files.createDirectories(notesDir.toPath());
        }
        if (!outDir.isDirectory()) {
            Files.createDirectories(outDir.toPath());
        }

        File writeFile = new File(currentDirectory + "\\notes\\" + NOTES_ID + "#" + CMD_SHOW_APPSTAGE + ".in");

        File watchFile = new File(currentDirectory + "\\out\\" + NOTES_ID + ".out");

        boolean isSettings = Files.isRegularFile(settingsPath);
        boolean updates = true;
        if (isSettings) {
            try {
                String jsonString = Files.readString(settingsPath);
                launcherData = new JsonParser().parse(jsonString).getAsJsonObject();
                JsonElement appKeyElement = launcherData.get("appKey");
                JsonElement updatesElement = launcherData.get("updates");
                if (appKeyElement != null) {
                    String appKey = appKeyElement.getAsString();
                    if (updatesElement != null) {
                        updates = updatesElement.getAsBoolean();

                    }
                    if (!appKey.startsWith("$2a$15$")) {
                        launcherData = null;
                    }
                } else {
                    launcherData = null;
                }

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
                showIfNotRunning(watchFile, writeFile, () -> {

                    try {
                        openJar(currentDirectoyJar);
                    } catch (IOException e) {
                        openSetup(visitGitHub, javaVersionArg, appJarArg);
                    }
                });

            } else {
                openSetup(visitGitHub, javaVersionArg, appJarArg);
            }

        }

    }

    public static String getShowCmdObject() {

        return "{\"id\": \"" + NOTES_ID + "\", \"type\": \"CMD\", \"cmd\": \"" + CMD_SHOW_APPSTAGE + "\", \"timeStamp\": " + System.currentTimeMillis() + "}";

        /*JsonObject showJson = new JsonObject();
        showJson.addProperty("id", "launcher");
        showJson.addProperty("type", "CMD");
        showJson.addProperty("cmd", CMD_SHOW_APPSTAGE);
        showJson.addProperty("timeStamp", System.currentTimeMillis());

        return showJson; */
    }

    public static void showIfNotRunning(File watchFile, File writeFile, Runnable notRunning) throws IOException {

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        if (watchFile.isFile()) {
            Files.delete(watchFile.toPath());
        }

        try {
            Files.writeString(writeFile.toPath(), getShowCmdObject());
        } catch (IOException ex) {
            try {
                Files.writeString(logFile.toPath(), "\nIO exception " + ex.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
        }

        executor.schedule(() -> {
            if (watchFile.isFile()) {
                Platform.exit();
                System.exit(0);
            } else {
                notRunning.run();
            }

        }, EXECUTION_TIME, TimeUnit.MILLISECONDS);

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

        Platform.exit();
        System.exit(0);
    }

}
