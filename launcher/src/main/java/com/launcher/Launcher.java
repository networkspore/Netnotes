package com.launcher;

import java.io.BufferedReader;
import java.io.File;

import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.Files;

import java.nio.file.StandardOpenOption;
import java.security.Security;

import javafx.application.Application;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;

import com.google.gson.JsonObject;


public class Launcher {

    private static File logFile = new File("launcher-log.txt");


    
    public static final long EXECUTION_TIME = 100;
    
    public static final String CMD_SHOW_APPSTAGE = "SHOW_APPSTAGE";
    public static final String NOTES_ID = "launcher";

    public static void main(String[] args) throws IOException {
        
        Application.launch(Setup.class, args);
           
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


    private static void openSetup(boolean firstRun, String javaVersion, String appFilePath, String currentDirectory) {

        JsonObject json = new JsonObject();
        json.addProperty("firstRun", firstRun);
        
        if(javaVersion != null){
            json.addProperty("javaVersion", javaVersion);
        }

        json.addProperty("appFile", appFilePath);
        json.addProperty("currentDirectory", currentDirectory);

        Application.launch(Setup.class, json.toString());

    }

   /* public static void openUpdater(boolean updates, String javaVersion, File appFile)  {
        JsonObject json = new JsonObject();
        json.addProperty("updates", updates);
        json.addProperty("javaVersion", javaVersion);
        json.addProperty("appFile", appFile.getAbsolutePath());

        byte[] jsonStringBytes = json.toString().getBytes();

        
        Application.launch(Updater.class, Hex.encodeHexString(jsonStringBytes));
    }*/

   

    
}
