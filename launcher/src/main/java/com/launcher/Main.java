package com.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Security;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.List;

import com.rfksystems.blake2b.security.Blake2bProvider;

public class Main {

    public static final String latestReleaseURLstring = "https://github.com/networkspore/Netnotes/releases";

    public static final String appDataDirectory = System.getenv("LOCALAPPDATA") + "\\NetNotes";
    public static final String javaVersion = System.getProperty("java.version");
    public static final String currentDirectory = System.getProperty("user.dir");
    public static final Path appDataPath = Paths.get(appDataDirectory);

    public static String currentDirectoyJar = Utils.getLatestFileString(currentDirectory);
    public static String appDataJar = Utils.getLatestFileString(appDataDirectory);

    public static void main(String[] mainArgs) {

        Security.addProvider(new Blake2bProvider());

        launch();

    }

    public static void launch() {
        boolean isAppData = Files.isDirectory(appDataPath);
        boolean isJar = currentDirectoyJar.equals("") && appDataJar.equals("");
        boolean isJava = checkJava();

        List<String> launcherList = new ArrayList<String>();

        if (!isJar) {
            launcherList.add("noJar");
        }

        if (!isJava) {
            launcherList.add("noJava");
        }

        if (!isAppData) {
            try {
                Files.createDirectory(appDataPath);
            } catch (IOException e) {

            }
        }

        AppJar latestAppJar = getLatestJar();

        if (latestAppJar == null) {
            if (isJar) {
                if (currentDirectoyJar.equals("")) {
                    openJar(appDataJar);
                } else {
                    if (appDataJar.equals("")) {
                        File currentDirFile = new File(currentDirectoyJar);
                        File appDirFile = new File(appDataDirectory + "\\" + currentDirFile.getName());

                        try {
                            Files.move(currentDirFile.toPath(), appDirFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            openJar(appDirFile.getAbsolutePath());
                        } catch (IOException e) {

                        }

                        openJar(currentDirectoyJar);

                        openSetup("noInternetAndJar");
                    } else {
                        AppJar a = new AppJar(new File(currentDirectoyJar));
                        AppJar b = new AppJar(new File(appDataJar));
                        int compared = a.getVersion().compareTo(b.getVersion());

                        if (compared == -1 || compared == 0) {
                            openJar(appDataJar);
                            openSetup("noInternetAndJar");
                        } else {
                            String newAppDirFile = appDataDirectory + "\\" + a.getFile().getName();
                            try {
                                Files.move(a.getFile().toPath(), Paths.get(newAppDirFile), StandardCopyOption.REPLACE_EXISTING);
                                openJar(newAppDirFile);
                            } catch (IOException e) {

                            }
                            openJar(currentDirectoyJar);
                            openSetup("noInternetAndJar");
                        }

                    }
                }

            } else {
                openSetup("noInternetAndJar");
            }
        } else {

        }
    }

    public static AppJar getLatestJar() {

        return null;
    }

    /*  public static void checkJar() {
        if (currentDirectoyJar.equals("")) {
            if (isAppData) {

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

    }*/
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

    private static void openJar(String jarFilePathString) {
        String cmdString = "cmd /c javaw -jar " + jarFilePathString;
        boolean executed = true;

        try {
            Runtime.getRuntime().exec(cmdString);
        } catch (IOException e) {
            executed = false;
        }

        if (executed) {

            shutdownNow();
        }
    }

    private static void shutdownNow() {
        Platform.exit();
        System.exit(0);
    }
}
