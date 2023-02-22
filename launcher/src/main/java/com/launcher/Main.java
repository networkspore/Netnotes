package com.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static final String appDataDirectory = System.getenv("LOCALAPPDATA") + "\\NetNotes";
    public static final String javaVersion = System.getProperty("java.version");
    public static final String currentDirectory = System.getProperty("user.dir");

    public static void main(String[] mainArgs) {

        if (!checkJava()) {
            openSetup("noJava");
        } else {
            launch();
        }

    }

    public static void launch() {
        String currentDirectoyJar = Utils.getLatestFileString(currentDirectory);

        Path appDataPath = Paths.get(appDataDirectory);

        boolean isAppData = Files.isDirectory(appDataPath);

        if (!isAppData) {
            try {
                Files.createDirectory(appDataPath);
            } catch (IOException e) {

            }
        }

        if (currentDirectoyJar.equals("")) {
            if (isAppData) {
                String appDataJar = Utils.getLatestFileString(appDataDirectory);

                if (appDataJar.equals("")) {
                    // openSetup("noJar");
                } else {
                    openJar(appDataDirectory + "\\" + appDataJar);
                }
            } else {
                // openSetup("noJar");
            }
        } else {
            openJar(currentDirectory + "\\" + currentDirectoyJar);
        }
    }

    public static boolean checkJava() {

        String[] cmd = {"java", "--version"};

        //String line = null;
        //List<String> list = new ArrayList<String>();
        //boolean executed = true;
        try {
            Runtime.getRuntime().exec(cmd);
            /*          
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            
            while ((line = stdInput.readLine()) != null) {
                list.add(line);
            }

            while ((line = stdError.readLine()) != null) {
                list.add(line);
            } */

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private static void openSetup(String... launcherArgs) {

        Application.launch(Setup.class, launcherArgs);
    }

    private static boolean openJar(String jarFilePathString) {
        String cmdString = "cmd /c javaw -jar " + jarFilePathString;
        boolean executed = true;

        try {
            Runtime.getRuntime().exec(cmdString);
        } catch (IOException e) {
            executed = false;
        }

        return executed;
    }
}
