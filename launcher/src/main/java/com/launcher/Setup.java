package com.launcher;

import javafx.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Optional;



import org.apache.commons.codec.binary.Hex;
import org.reactfx.util.FxTimer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.HostServices;

public class Setup extends Application {

    private File logFile = new File("setup-log.txt");
    
    
   
    public static final String SETTINGS_FILE_NAME = "settings.conf";
    public static final String KEYFILE_WARNING = "The autorun feature requires an encrypted version of your private key to be saved to disk.\n\nNotice: In order to minimize the security risk the key will be encrypted using platform specific information. Updating or changing base system hardware, such as your motherboard or bios, may invalidate the key and require the autorun feature to be re-enabled in Netnotes settings.\n\n";
    
    
    public static final String HOME_DIRECTORY = System.getProperty("user.home");
    public static final File DESKTOP_DIRECTORY = new File(HOME_DIRECTORY + "/Desktop");
    public static final File STARTUP_DIRECTORY = new File(HOME_DIRECTORY + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup");
    public static final File PROGRAMS_DIRECTORY = new File(HOME_DIRECTORY + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs");

    public static final String APP_DATA_DIR = System.getenv("LOCALAPPDATA") + "\\Netnotes";

    private final static String GitHub_ALL_RELEASES_URL = "https://api.github.com/repos/networkspore/Netnotes/releases";

    private final static String GitHub_USERDL_URL = "https://github.com/networkspore/Netnotes/releases";

    public final static String JAVA_URL = "https://www.java.com/en/download/";

    

    public final static Font mainFont = Font.font("OCR A Extended", FontWeight.BOLD, 25);
    public final static Font txtFont = Font.font("OCR A Extended", 15);
    public final static Font smallFont = Font.font("OCR A Extended", 11);
    public final static Font titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
    public final static Color txtColor = Color.web("#cdd4da");

    public final static Image icon = new Image("/assets/icon20.png");
    public final static Image logo = new Image("/assets/icon256.png");
    public final static Image ergoLogo = new Image("/assets/ergo-black-350.png");
    public final static Image waitingImage = new Image("/assets/spinning.gif");
    public final static Image closeImg = new Image("/assets/close-outline-white.png");
    public final static Image minimizeImg = new Image("/assets/minimize-white-20.png");



    private HostServices services = getHostServices();
    private File m_notesDir = null;
    private File m_outDir = null;
    private File m_writeFile = null;
    private File m_watchFile = null;

    private File m_appFile = null;
    private File m_appDir = null;
    private File m_launcherFile = null;
    private File m_settingsFile = null;
    private File m_autoRunFile = null;
 

    private Version m_javaVersion = null;
    private HashData m_appHashData = null;
    private HashData m_launcherHashData = null;
    private boolean m_updates = true;
    private boolean m_autoUpdate = false;
    private boolean m_autoRun = false;
    

    private boolean m_desktopShortcut = true;
    private boolean m_startMenuShortcut = true;

    private boolean m_isDaemon = false;
    
    private File m_launcherUpdateFile = null;
    private HashData m_launcherUpdateHashData = null;
   

    @Override
    public void start(Stage appStage) {
        Parameters params = getParameters();
        List<String> list = params.getRaw();

        for(String arg : list){
            if(arg.equals("--daemon")){
                m_isDaemon = true;
            }
        }

        appStage.getIcons().add(logo);
        appStage.setResizable(false);
        appStage.initStyle(StageStyle.UNDECORATED);

        showStatusStage(appStage, "Opening", "Opening...");
   
       
        try {
            
            URL classLocation = Utils.getLocation(getClass());
            m_launcherFile = Utils.urlToFile(classLocation);
            
            m_launcherHashData = new HashData(m_launcherFile);
        
            m_javaVersion = Utils.checkJava();

            m_appDir  = new File(m_launcherFile.getParentFile().getAbsolutePath());
           
            m_appFile = Utils.getLatestFile(m_appDir);

         
            
            
        
            m_settingsFile = new File(m_appDir.getAbsolutePath() + "/" + SETTINGS_FILE_NAME);
            
            String jsonString = m_settingsFile.isFile() ? Files.readString(m_settingsFile.toPath()) : null;
            JsonObject launcherData = jsonString != null ? new JsonParser().parse(jsonString).getAsJsonObject() : null;
            JsonElement appKeyElement = launcherData != null ? launcherData.get("appKey") : null;
            JsonElement updatesElement = launcherData != null ? launcherData.get("updates") : null;
            JsonElement autoUpdateElement = launcherData != null ? launcherData.get("autoUpdate") : null;
            JsonElement autoRunFileElement = launcherData != null ? launcherData.get("autoRunFile") : null;
     
            m_autoRunFile = autoRunFileElement != null && autoRunFileElement.isJsonPrimitive() ? new File(autoRunFileElement.getAsString()) : null;

            m_autoUpdate = autoUpdateElement != null && autoUpdateElement.isJsonPrimitive() ? autoUpdateElement.getAsBoolean() : false;
            m_updates = updatesElement != null && updatesElement.isJsonPrimitive() ? updatesElement.getAsBoolean() : true;

            String appKey = appKeyElement != null && appKeyElement.isJsonPrimitive() ? appKeyElement.getAsString() : null;
            

            if(!(appKey != null && appKey.startsWith("$2a$15$"))){
                m_settingsFile = null;
            }

            if(!Utils.checkJar(m_appFile)){
                m_appFile = null;
               
            }
            m_appHashData = m_appFile != null ? new HashData(m_appFile) : null;   

            if (m_javaVersion == null || m_appHashData == null || m_settingsFile == null) { 
                if(!m_isDaemon){
                    if(m_settingsFile == null){
                        firstRun(appStage);
                    }else{
                        checkSetup(appStage);
                    }
                }else{
                    shutdownNow();
                }
            }else{
             
                startApp(appStage);
            }
        
        } catch (Exception e) {

        
            try{
                Files.writeString(logFile.toPath(), "\nSetup: err" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }catch(IOException e1){
               //  visitWebsites(new File(CURRENT_DIRECTORY), appStage);
            }
        
        }        

    }

    public static void showStatusStage(Stage statusStage, String title, String statusMessage) {
       
        statusStage.setTitle(title);

        Label newTitleLbl = new Label(title);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        ImageView barIconView = new ImageView(icon);
        barIconView.setFitHeight(20);
        barIconView.setPreserveRatio(true);

        HBox newTopBar = new HBox(barIconView, newTitleLbl);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(10, 8, 10, 10));
        newTopBar.setId("topBar");

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);

        Text statusTxt = new Text(statusMessage);
        statusTxt.setFill(txtColor);
        statusTxt.setFont(txtFont);

        VBox bodyVBox = new VBox(imageBox, statusTxt);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(newTopBar, bodyVBox);

        Scene statusScene = new Scene(layoutVBox, 400, 215);
        statusScene.getStylesheets().add("/css/startWindow.css");

        statusStage.setScene(statusScene);
        
        statusStage.show();
    }

    public void startApp(Stage appStage) throws IOException{
        

            m_notesDir = new File(m_appDir.getCanonicalPath() + "/notes");
            m_outDir = new File(m_appDir.getCanonicalPath() + "/out");

            if(!m_notesDir.isDirectory()){
                Files.createDirectory(m_notesDir.toPath());
            }
            if(!m_outDir.isDirectory()){
                Files.createDirectory(m_outDir.toPath());
            }
          
            m_writeFile = new File(m_notesDir.getCanonicalPath() + "/" + Launcher.NOTES_ID + "#" + Launcher.CMD_SHOW_APPSTAGE + ".in");

            m_watchFile = new File(m_outDir.getCanonicalPath() + "/" + Launcher.NOTES_ID + ".out");

      

            Launcher.showIfNotRunning(m_watchFile, m_writeFile, () -> {
          

                if((m_updates && !m_isDaemon) || (m_updates && m_autoUpdate && m_isDaemon)){
                    checkForAllUpdates(appStage, ()->{
                       
                        try{
                            openJar();
                        }catch(IOException e){
                            visitWebsites(appStage);
                        }
                        
                       
                    });
                }else{
                    try{
                        openJar();
                    }catch(IOException e){
                        visitWebsites(appStage);
                    }
                    
                  
                    
                }
    

            });
    }

    private void getApp(ProgressBar progressBar, Text statusText, Runnable complete, Runnable failed){
      

     
        Utils.getUrlJsonArray(GitHub_ALL_RELEASES_URL, (onSucceeded)->{
              Object sourceObject = onSucceeded.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonArray) {
                    JsonArray allReleases = (JsonArray) sourceObject;
                   
                    int length = allReleases.size();
              
                    int j = length -1;

                    SimpleObjectProperty<JsonObject> releaseInfoObject = new SimpleObjectProperty<>(null);
                    SimpleObjectProperty<JsonObject> appAsset = new SimpleObjectProperty<>(null);
                    SimpleObjectProperty<JsonObject> launcherAsset = new SimpleObjectProperty<>(null);
                    SimpleBooleanProperty foundRelease = new SimpleBooleanProperty(false);
                    
                    while(j < allReleases.size() && !foundRelease.get()){
                        JsonObject gitHubApiJson = allReleases.get(j).getAsJsonObject();

                        JsonElement assetsElement = gitHubApiJson.get("assets");
                        if (assetsElement != null && assetsElement.isJsonArray()) {
                            JsonArray assetsArray = assetsElement.getAsJsonArray();
                            
                

                            for(int i = 0; i < assetsArray.size(); i++){
                                
                                JsonElement assetElement = assetsArray.get(i);

                                if (assetElement != null && assetElement.isJsonObject()) {
                                    
                                    JsonObject assetObject = assetElement.getAsJsonObject();

                                    JsonElement downloadUrlElement = assetObject.get("browser_download_url");
                                    JsonElement nameElement = assetObject.get("name");

                                    if (nameElement != null && nameElement.isJsonPrimitive() && downloadUrlElement != null && downloadUrlElement.isJsonPrimitive()) {
                                        String name = nameElement.getAsString();
                                        
                                        if(name.startsWith("releaseInfo")){
                                            
                                            releaseInfoObject.set(assetObject);
                                         
                                        }else{
                                            if(name.endsWith("exe")){
                                                launcherAsset.set(assetObject);
                                               
                                            }else{
                                                if(name.endsWith("jar")){
                                                    appAsset.set(assetObject);
                                                 
                                                }
                                            }
                                        }


                                    }
                                }
                            }
                            
                            if(launcherAsset.get() != null && appAsset.get() != null && releaseInfoObject.get() != null){
                                foundRelease.set(true);
                            }else{
                                launcherAsset.set(null);
                                appAsset.set(null);
                                releaseInfoObject.set(null);
                                
                            }
                        }
                        j++;
                    }

                    if(foundRelease.get()){
                        JsonObject releaseInfoAssetObject = releaseInfoObject.get();

                        JsonElement releaseInfoDownloadUrlElement = releaseInfoAssetObject.get("browser_download_url");
       
                        if(releaseInfoDownloadUrlElement != null && releaseInfoDownloadUrlElement.isJsonPrimitive()){
                            Utils.getUrlJson(releaseInfoDownloadUrlElement.getAsString(), (onReleaseInfoSucceeded)->{
                                Object jsonObjectSourceObject = onReleaseInfoSucceeded.getSource().getValue();
                                if (jsonObjectSourceObject != null && jsonObjectSourceObject instanceof JsonObject) {
                                    JsonObject releaseInfo = (JsonObject) jsonObjectSourceObject;
                                    JsonElement applicationElement = releaseInfo.get("application");
          

                                    if(applicationElement != null && applicationElement.isJsonObject()){
                                        JsonObject applicationInfoObject = applicationElement.getAsJsonObject();
                                        JsonElement appHashDataElement = applicationInfoObject.get("hashData");
                                  
                                        HashData appObjectHashData = appHashDataElement != null && appHashDataElement.isJsonObject() ? new HashData(appHashDataElement.getAsJsonObject()) : null;

                                  

                                        boolean isGetApp =  appObjectHashData != null;
                                   

                                        if(isGetApp){    

                                            JsonObject appAssetObject = appAsset.get();
                                            JsonElement appAssetDownloadUrlElement = appAssetObject.get("browser_download_url");
                                            JsonElement appAssetNameElement = appAssetObject.get("name");

                                        

                                            Stage getAppStage = new Stage();
                                            getAppStage.getIcons().add(logo);
                                            getAppStage.setResizable(false);
                                            getAppStage.initStyle(StageStyle.UNDECORATED);
                                            
                                            String appFileName = appAssetNameElement.getAsString();


                                            File appFile = new File(m_appDir.getAbsolutePath() + "/" + appFileName);

                                             progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
                                                if(newVal.doubleValue() != 1){
                                                    statusText.setText(appFileName + " (" + String.format("%.1f", newVal.doubleValue() * 100) + "%)");
                                                }else{
                                                    statusText.setText("Checking...");
                                                }
                                            });
                                  
                                            Utils.getUrlFileHash(appAssetDownloadUrlElement.getAsString(), appFile, onHashData->{
                                                Object onHashDataObject = onHashData.getSource().getValue();
                                                if(onHashDataObject != null && onHashDataObject instanceof HashData){
                                                    m_appHashData = (HashData) onHashDataObject;
                                                    m_appFile = appFile;
                                                    complete.run();
                                                }else{
                                                    failed.run();
                                                }
                                            }, onFailedDownload->{
                                                try {
                                                    Files.writeString(logFile.toPath(), "\nSetup: Failed getting app: " + onFailedDownload.getSource().getMessage(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                                } catch (IOException e) {

                                                }
                                                failed.run();
                                            }, progressBar);

                                        }else{
                                            failed.run();
                                        }
                                    }else{
                                        failed.run();
                                    }
                                }
                            }, (onFailed)->{
                                failed.run();
                            }, null);
                        }else{
                            failed.run();
                        }
                    }else{
                        failed.run();
                    }
                }else{
                    failed.run();
                }
            }, onFailed->{
                failed.run();
            }, null);


                                            
    }                   
    

  


    private void checkForAllUpdates(Stage appStage, Runnable complete){

 
        Utils.getUrlJsonArray(GitHub_ALL_RELEASES_URL, (onSucceeded)->{
              Object sourceObject = onSucceeded.getSource().getValue();
                if (sourceObject != null && sourceObject instanceof JsonArray) {
                    JsonArray allReleases = (JsonArray) sourceObject;
            
                    int length = allReleases.size();
              
                    int j = 0;
                    SimpleObjectProperty<JsonObject> releaseInfoObject = new SimpleObjectProperty<>(null);
                    SimpleObjectProperty<JsonObject> appAsset = new SimpleObjectProperty<>(null);
                    SimpleObjectProperty<JsonObject> launcherAsset = new SimpleObjectProperty<>(null);

                    SimpleBooleanProperty foundRelease = new SimpleBooleanProperty(false);
                    
                    while(j < length && !foundRelease.get()){
                        JsonObject gitHubApiJson = allReleases.get(j).getAsJsonObject();

                        JsonElement assetsElement = gitHubApiJson.get("assets");
                        if (assetsElement != null && assetsElement.isJsonArray()) {
                            JsonArray assetsArray = assetsElement.getAsJsonArray();
                            
                            

                            for(int i = 0; i < assetsArray.size(); i++){
                                
                                JsonElement assetElement = assetsArray.get(i);

                                if (assetElement != null && assetElement.isJsonObject()) {
                                    
                                    JsonObject assetObject = assetElement.getAsJsonObject();

                                    JsonElement downloadUrlElement = assetObject.get("browser_download_url");
                                    JsonElement nameElement = assetObject.get("name");

                                    if (nameElement != null && nameElement.isJsonPrimitive() && downloadUrlElement != null && downloadUrlElement.isJsonPrimitive()) {
                                        String name = nameElement.getAsString();
                                        
                                        if(name.startsWith("releaseInfo")){
                                            
                                            releaseInfoObject.set(assetObject);
                                       
                                        }else{
                                            if(name.endsWith("exe")){
                                                launcherAsset.set(assetObject);
                                           
                                            }else{
                                                if(name.endsWith("jar")){
                                                    appAsset.set(assetObject);
                                                   
                                                }
                                            }
                                        }


                                    }
                                }
                                
                            }
                            
                            if(launcherAsset.get() != null && appAsset.get() != null && releaseInfoObject.get() != null){
                                foundRelease.set(true);
                            }else{
                                launcherAsset.set(null);
                                appAsset.set(null);
                                releaseInfoObject.set(null);
                                
                            }
                        }
                        j++;
                    }

                    if(foundRelease.get()){
                        JsonObject releaseInfoAssetObject = releaseInfoObject.get();

                        JsonElement releaseInfoDownloadUrlElement = releaseInfoAssetObject.get("browser_download_url");
       
                        if(releaseInfoDownloadUrlElement != null && releaseInfoDownloadUrlElement.isJsonPrimitive()){
                            Utils.getUrlJson(releaseInfoDownloadUrlElement.getAsString(), (onReleaseInfoSucceeded)->{
                                Object jsonObjectSourceObject = onReleaseInfoSucceeded.getSource().getValue();
                                if (jsonObjectSourceObject != null && jsonObjectSourceObject instanceof JsonObject) {
                                    JsonObject releaseInfo = (JsonObject) jsonObjectSourceObject;
                                    JsonElement applicationElement = releaseInfo.get("application");
                                    JsonElement launcherElement = releaseInfo.get("launcher");

                                    if(applicationElement != null && applicationElement.isJsonObject() && launcherElement != null && launcherElement.isJsonObject()){
                                        JsonObject applicationInfoObject = applicationElement.getAsJsonObject();
                                        JsonElement appHashDataElement = applicationInfoObject.get("hashData");
                                  
                                        JsonObject launcherInfoObject = launcherElement.getAsJsonObject();
                                        JsonElement launcherHashDataElement = launcherInfoObject.get("hashData");
                                        JsonElement launcherVersionElement = launcherInfoObject.get("version");

                                        HashData launcherReleaseHashData = launcherHashDataElement != null && launcherHashDataElement.isJsonObject() ? new HashData(launcherHashDataElement.getAsJsonObject()) : null;

                                        HashData appObjectHashData = appHashDataElement != null && appHashDataElement.isJsonObject() ? new HashData(appHashDataElement.getAsJsonObject()) : null;

                                        //String appName = appNameElement.getAsString();
                                        //Version appVersion = new Version(appVersionElement.getAsString());
                                        String appReleaseHashHex = appObjectHashData.getHashStringHex();
                                        String launcherReleaseHashHex = launcherReleaseHashData.getHashStringHex();
                                        String launcherReleaseVersionString = launcherVersionElement.getAsString();

                                        boolean isGetApp =  appObjectHashData != null && m_appFile == null || (m_appFile != null && !m_appFile.isFile()) || (m_appHashData != null && !m_appHashData.getHashStringHex().equals(appReleaseHashHex));
                                        
                                        boolean isGetLauncher =  launcherReleaseHashData != null  && m_launcherHashData != null && !m_launcherHashData.getHashStringHex().equals(launcherReleaseHashHex);

                                        JsonObject appAssetObject = appAsset.get();
                                        JsonElement appAssetDownloadUrlElement = appAssetObject.get("browser_download_url");
                                        JsonElement appAssetNameElement = appAssetObject.get("name");

                                        String appAssetName = appAssetNameElement.getAsString();
                                        String appAssetUrl = appAssetDownloadUrlElement.getAsString();

                                        JsonObject launcherAssetObject = launcherAsset.get();
                                        JsonElement launcherAssetDownloadUrlElement = launcherAssetObject.get("browser_download_url");
                                        JsonElement launcherAssetNameElement = launcherAssetObject.get("name");
                                        String launcherName = launcherAssetNameElement.getAsString();
                                        String launcherUrl = launcherAssetDownloadUrlElement.getAsString();

                                        Runnable doUpdates = () ->{
                                            if(isGetApp){    

                                                getUrlApp(appAssetUrl, appAssetName, appReleaseHashHex, appStage, ()->{
                                                    if(isGetLauncher){
                                                        getUrlLauncher(launcherUrl, launcherName, launcherReleaseHashHex, launcherReleaseVersionString, appStage, complete);
                                                    }else{
                                                        complete.run();
                                                    }
                                                });
                                           
                                            }else{
                                              
                                               
                                                if(isGetLauncher){
                                                
                                                    getUrlLauncher(launcherUrl, launcherName, launcherReleaseHashHex, launcherReleaseVersionString, appStage, complete);
                                                    
                                                }else{
                                                  
                                                    complete.run();
                                                }
                                            }
                                        };
                                        if(isGetApp || isGetLauncher){
                                            if(!m_autoUpdate){
                                            
                                                    Button closeBtn = new Button();
                                                    TextField promptField = new TextField(); 
                                                    showGetTextInput(appStage, "Update? ("+ (isGetApp ? appAssetName : "") + (isGetApp && isGetLauncher ? ", ":"")+ (isGetLauncher ? launcherName : "") +") (Y/n)", "Update Available - Netnotes", "Update available...", logo, closeBtn, promptField);
                                                    closeBtn.setOnAction(e->{
                                                        complete.run();
                                                    });
                                                    appStage.setOnCloseRequest(e->complete.run());

                                                    promptField.setOnKeyPressed(e1 -> {
                                                        KeyCode keyCode = e1.getCode();
                                                        if(keyCode == KeyCode.ENTER || keyCode == KeyCode.Y){
                                                            doUpdates.run();
                                                        }else{
                                                            if(keyCode == KeyCode.N){
                                                                complete.run();
                                                            }else{
                                                                promptField.setText("");
                                                            }
                                                        }
                                                    });
                                                

                                            }else{
                                                doUpdates.run();
                                            }
                                        }else{
                                            complete.run();
                                        }
                                        


                                    }else{
                                        try {
                                            Files.writeString(logFile.toPath(), "\nSetup: received invalid releaseInfo, no application object", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                        } catch (IOException e2) {
                                        
                                        }
                                        complete.run();
                                    }

                                }else{
                                    try {
                                        Files.writeString(logFile.toPath(), "\nSetup: received releaseInfo which is not a Json", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e2) {
                                    
                                    }
                                    complete.run();
                                }
                            }, (failed)->{
                                try {
                                    Files.writeString(logFile.toPath(), "\nSetup: getting releaseInfo failed: " + failed.getSource().getMessage(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e2) {
                                
                                }
                                complete.run();
                            }, null);
                        }else{
                            complete.run();
                        }

                    }else{
                        try {
                            Files.writeString(logFile.toPath(), "\nSetup: no releases found", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e2) {
                        
                        }
                        complete.run();
                    }
                }else{
                    try {
                        Files.writeString(logFile.toPath(), "\nSetup: Received invalid release info (Null or not a Json)", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e2) {
                    
                    }
                    complete.run();
                }

        }, (onFailed)->{
            try {
                Files.writeString(logFile.toPath(), "\nSetup: Failed to download release update info: " + onFailed.getSource().getMessage(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e2) {
            
            }
            complete.run();
        }, null);
        
    }

    public void getUrlApp(String url, String name, String hashHex, Stage appStage, Runnable complete){
        ProgressBar progressBar = new ProgressBar();
    
        
        Scene progressScene = getProgressScene(icon, "Downloading", "Setup - Netnotes", name, progressBar, appStage);
        appStage.setScene(progressScene);

        File appUpdateFile = new File(m_appDir.getAbsolutePath() + "/" + name);
        Utils.getUrlFileHash(url, appUpdateFile, (appAssetSucceded)->{
      
            Object appAssetHashDataObject = appAssetSucceded.getSource().getValue();
            if(appAssetHashDataObject != null && appAssetHashDataObject instanceof HashData){
                HashData appUpdateHashData = (HashData) appAssetHashDataObject;
         
                if(appUpdateHashData.getHashStringHex().equals(hashHex)){
                    
                    if(m_appFile != null && m_appFile.isFile() && m_appFile.getParentFile().getAbsolutePath().equals(m_appDir.getAbsolutePath())){
                        try{
                            Files.deleteIfExists(m_appFile.toPath());
                        }catch(IOException e){
                            try {
                                Files.writeString(logFile.toPath(), "\nSetup: Error deleting old app file:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e2) {
                            
                            }
        
                        }
                    }
                    m_appFile = appUpdateFile;
                    m_appHashData = appUpdateHashData;
                }else{
                    try {
                        Files.deleteIfExists(m_appFile.toPath());
                        Files.writeString(logFile.toPath(),"\nSetup: Download does not match expected hash: " + appUpdateHashData.getHashStringHex(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                
                }
            }
            complete.run();
            
        }, (appAssetFailed)->{
            try {
                Files.writeString(logFile.toPath(), "\nSetup: " + appAssetFailed.getSource().getMessage(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                
            }
            complete.run();
        }, progressBar);
    }

    
    public void getUrlLauncher(String url, String name, String hashHex, String version, Stage appStage, Runnable complete){
        

        ProgressBar progressBar = new ProgressBar();
   
        Scene progressScene = getProgressScene(icon, "Downloading", "Setup - Netnotes", name, progressBar, appStage);
        appStage.setScene(progressScene);

        File launcherUpdateFile = new File(m_appDir.getAbsolutePath() + "/launcher-"+version +".tmp");

        Utils.getUrlFileHash(url, launcherUpdateFile, (onSucceded)->{
        
            Object launcherAssetHashDataObject = onSucceded.getSource().getValue();
            if(launcherAssetHashDataObject != null && launcherAssetHashDataObject instanceof HashData){
                HashData launcherUpdateHashData = (HashData) launcherAssetHashDataObject;
           
                m_launcherUpdateFile = launcherUpdateFile;
                m_launcherUpdateHashData  = launcherUpdateHashData;
                
            }
            complete.run();
            
        }, (onFailed)->{
       
            complete.run();
        }, progressBar);
    }
    private boolean m_firstRun = false;
 
    private void firstRun(Stage appStage) {
        VBox bodyVBox = new VBox();
        m_firstRun = true;
        bodyVBox.setPadding(new Insets(0, 15, 0, 0));
        setSetupStage(appStage, "Netnotes - Setup", "Setup...", bodyVBox);
        appStage.setHeight(540);
        Text directoryTxt = new Text("> Location:");
        directoryTxt.setFill(txtColor);
        directoryTxt.setFont(txtFont);

    

        Button directoryBtn = new Button(m_appDir.getAbsolutePath());
        directoryBtn.setFont(txtFont);
        directoryBtn.setId("toolBtn");
        directoryBtn.setAlignment(Pos.CENTER_LEFT);

        File appData = new File(APP_DATA_DIR);
        boolean isAppdata = new File(System.getenv("LOCALAPPDATA")).isDirectory();
        final String useAppDataString = "AppData";
        final String useDefaultString = "Default";
        Button defaultBtn = new Button(isAppdata ? useAppDataString : useDefaultString);
        defaultBtn.setFont(txtFont);
        // defaultBtn.setId("toolSelected");
        defaultBtn.setMinWidth(120);
        defaultBtn.setOnAction(btnEvent -> {
            if (defaultBtn.getText().equals(useAppDataString)) {
                m_appDir = appData;
                defaultBtn.setText(useDefaultString);
            } else {
                m_appDir = new File(m_appFile.getParentFile().getAbsolutePath());
                defaultBtn.setText(isAppdata ? useAppDataString : useDefaultString);
            }
            directoryBtn.setText(m_appDir.getAbsolutePath());
        });

        directoryBtn.setOnAction(btnEvent -> {

            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setInitialDirectory(m_appDir);
            File chosenDir = dirChooser.showDialog(appStage);
            if (chosenDir != null) {
                m_appDir = chosenDir;
                directoryBtn.setText(m_appDir.getAbsolutePath());
            }
        });

        HBox directoryBox = null;

        directoryBox = new HBox(directoryTxt, directoryBtn, defaultBtn);
        directoryBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(directoryBox, Priority.ALWAYS);

        directoryBtn.prefWidthProperty().bind(directoryBox.widthProperty().subtract(directoryTxt.layoutBoundsProperty().get().getWidth()).subtract(defaultBtn.widthProperty()));

        Text updatesTxt = new Text("> Updates:");
        updatesTxt.setFill(txtColor);
        updatesTxt.setFont(txtFont);



        Button updatesBtn = new Button("Enabled");
        updatesBtn.setId("inactiveMainImageBtn");
        updatesBtn.setFont(txtFont);
        updatesBtn.setOnAction(btnEvent -> {
            if (updatesBtn.getText().equals("Enabled")) {
                updatesBtn.setText("Disabled");
            } else {
                updatesBtn.setText("Enabled");
            }
        });
       
        HBox updatesBox = new HBox(updatesTxt, updatesBtn);
        updatesBox.setAlignment(Pos.CENTER_LEFT);
        updatesBox.setPadding(new Insets(3, 0, 0, 0));


        Text autoUpdateTxt = new Text("> Auto-update:");
        autoUpdateTxt.setFill(txtColor);
        autoUpdateTxt.setFont(txtFont);

        Button autoUpdateBtn = new Button("Disabled");
        autoUpdateBtn.setId("inactiveMainImageBtn");
        autoUpdateBtn.setFont(txtFont);


        autoUpdateBtn.setOnAction(btnEvent -> {
            if (autoUpdateBtn.getText().equals("Enabled")) {
                autoUpdateBtn.setText("Disabled");
            } else {
                autoUpdateBtn.setText("Enabled");
            }
        });

        HBox autoUpdateBox = new HBox(autoUpdateTxt, autoUpdateBtn);
        autoUpdateBox.setAlignment(Pos.CENTER_LEFT);
        autoUpdateBox.setPadding(new Insets(3, 0, 0, 0));


        Text autoRunTxt = new Text("> Auto-run:");
        autoRunTxt.setFill(txtColor);
        autoRunTxt.setFont(txtFont);



        Button  autoRunBtn = new Button("Disabled");
         autoRunBtn.setId("inactiveMainImageBtn");
        autoRunBtn.setFont(txtFont);
     
  
        
        File documentsDir = new File(System.getProperty("user.home") + "/Documents");

        FileChooser keyFileChooser = new FileChooser();
        keyFileChooser.setTitle("Create key file");
        keyFileChooser.setInitialDirectory(documentsDir);
        keyFileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Key File (Encrypted file)", "*.*"));

        Button autoRunFileBtn = new Button("...");
        autoRunFileBtn.setId("inactiveMainImageBtn");
        autoRunFileBtn.setFont(txtFont);
        autoRunFileBtn.setAlignment(Pos.CENTER_LEFT);
        
 
        HBox autoRunBox = new HBox(autoRunTxt, autoRunBtn);
        autoRunBox.setAlignment(Pos.CENTER_LEFT);
        autoRunBox.setPadding(new Insets(3, 0, 0, 0));
        HBox.setHgrow(autoRunBox, Priority.ALWAYS);
        autoRunFileBtn.prefWidthProperty().bind(autoRunBox.widthProperty().subtract(autoRunTxt.getLayoutBounds().getWidth()).subtract(autoRunBtn.widthProperty()));
        autoRunBtn.setOnAction(btnEvent -> {
           
            if (autoRunBtn.getText().equals("Enabled")) {
                autoRunBtn.setText("Disabled");
                if(autoRunBox.getChildren().contains(autoRunFileBtn)){
                    autoRunBox.getChildren().remove(autoRunFileBtn);
                }
                autoRunFileBtn.setText("...");
            } else {
                Alert a = new Alert(AlertType.NONE, KEYFILE_WARNING, ButtonType.CANCEL, ButtonType.OK);
                a.setTitle("Autorun Key");
                a.setHeaderText("Autorun Key");
                a.initOwner(appStage);
                Optional<ButtonType> result = a.showAndWait();
                if(result.isPresent() && result.get() == ButtonType.OK){
                    File keyFile = keyFileChooser.showSaveDialog(appStage);
                    try {
                        
                        if(keyFile != null){
                   
                            if(!autoRunBox.getChildren().contains(autoRunFileBtn)){
                                autoRunBox.getChildren().add(1, autoRunFileBtn);
                            }
                            autoRunFileBtn.setText(keyFile.getCanonicalPath());
                            autoRunBtn.setText("Enabled");
                            
                        }
                    } catch (Exception e1) {
                        Alert err = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                        err.setTitle(e1.getCause().toString());
                        err.setHeaderText(e1.getCause().toString());
                        err.initOwner(appStage);
                        err.show();
                    }
                }
            }
        });

        autoRunFileBtn.setOnAction(e->{
       
            File keyFile = keyFileChooser.showSaveDialog(appStage);
            try {
               
                
                if(keyFile != null){
                    Files.writeString(keyFile.toPath(), "keyFile");
                    Files.delete(keyFile.toPath());
                    if(!autoRunBox.getChildren().contains(autoRunFileBtn)){
                        autoRunBox.getChildren().add(1, autoRunFileBtn);
                    }
                    autoRunFileBtn.setText(keyFile.getCanonicalPath());
                    autoRunBtn.setText("Enabled");
                    
                }
            } catch (Exception e1) {
                Alert err = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                err.setTitle(e1.getCause().toString());
                err.setHeaderText(e1.getCause().toString());
                err.initOwner(appStage);
                err.show();
                autoRunBtn.setText("Disabled");
                if(autoRunBox.getChildren().contains(autoRunFileBtn)){
                    autoRunBox.getChildren().remove(autoRunFileBtn);
                }
                autoRunFileBtn.setText("...");
            }
        });

        Text desktopShortcutTxt = new Text("> Desktop shortcut:");
        desktopShortcutTxt.setFill(txtColor);
        desktopShortcutTxt.setFont(txtFont);

        Button desktopShortcutBtn = new Button("Enabled");
        desktopShortcutBtn.setId("inactiveMainImageBtn");
        desktopShortcutBtn.setFont(txtFont);


        desktopShortcutBtn.setOnAction(btnEvent -> {
            if (desktopShortcutBtn.getText().equals("Enabled")) {
                desktopShortcutBtn.setText("Disabled");
            } else {
                desktopShortcutBtn.setText("Enabled");
            }
        });

        HBox desktopShortcutBox = new HBox(desktopShortcutTxt, desktopShortcutBtn);
        desktopShortcutBox.setAlignment(Pos.CENTER_LEFT);
        desktopShortcutBox.setPadding(new Insets(3, 0, 0, 0));

        Text startMenuShortcutTxt = new Text("> Start Menu shortcut:");
        startMenuShortcutTxt.setFill(txtColor);
        startMenuShortcutTxt.setFont(txtFont);

        Button startMenuShortcutBtn = new Button("Enabled");
        startMenuShortcutBtn.setId("inactiveMainImageBtn");
        startMenuShortcutBtn.setFont(txtFont);


        startMenuShortcutBtn.setOnAction(btnEvent -> {
            if (startMenuShortcutBtn.getText().equals("Enabled")) {
                startMenuShortcutBtn.setText("Disabled");
            } else {
                startMenuShortcutBtn.setText("Enabled");
            }
        });

        HBox startMenuShortcutBox = new HBox(startMenuShortcutTxt, startMenuShortcutBtn);
        startMenuShortcutBox.setAlignment(Pos.CENTER_LEFT);
        startMenuShortcutBox.setPadding(new Insets(3, 0, 0, 0));


        Button nextBtn = new Button("Next");
        nextBtn.setId("toolSelected");
        nextBtn.setFont(txtFont);

        Region hBar = new Region();
        hBar.setPrefWidth(400);
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

        HBox gBox = new HBox(hBar);
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(25, 0, 0, 0));

        HBox nextBox = new HBox(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));

        bodyVBox.getChildren().addAll(directoryBox, updatesBox, autoUpdateBox,desktopShortcutBox, startMenuShortcutBox, autoRunBox, gBox, nextBox);

        nextBtn.setOnAction(btnEvent -> {

            m_updates = updatesBtn.getText().equals("Enabled");
            m_autoUpdate = autoUpdateBtn.getText().equals("Enabled");
            m_autoRun = autoRunBtn.getText().equals("Enabled");
            m_startMenuShortcut = startMenuShortcutBtn.getText().equals("Enabled");
            m_desktopShortcut = desktopShortcutBtn.getText().equals("Enabled");

            if(m_autoRun){
                m_autoRunFile = new File(autoRunFileBtn.getText());
            }

            String directoryString = directoryBtn.getText();
        

            if (!m_appDir.isDirectory()) {

                Alert a = new Alert(AlertType.NONE, "This will create the directory:\n\n" + directoryString + "\n\n ", ButtonType.OK, ButtonType.CANCEL);
                a.initOwner(appStage);
                a.setTitle("Create Directory - Setup - Netnotes");
                a.setHeaderText("Setup");
                Optional<ButtonType> result = a.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {

                    if (m_appDir.mkdir()) {
                        createPassword(appStage);
                    }else{

                    }

                }
            } else {
                createPassword(appStage);
            }

        });
        appStage.setX(appStage.getX() - (appStage.getWidth()-400)/2);

        appStage.show();
    }

    private void createPassword(Stage appStage) {
        VBox bodyVBox = new VBox();
        setSetupStage(appStage, "Create Password - Netnotes", "Create password...", bodyVBox);
     
        appStage.setHeight(360);

        Text passwordTxt = new Text("> Password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

  

        PasswordField passwordField = new PasswordField();

        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        PasswordField createPassField2 = new PasswordField();
        HBox.setHgrow(createPassField2, Priority.ALWAYS);
        createPassField2.setId("passField");

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        Insets defaultInsets = passwordBox.getInsets();
        Insets inputInsets = new Insets(defaultInsets.getTop(), 30, defaultInsets.getBottom(), defaultInsets.getLeft());
        passwordBox.setPadding(inputInsets);

        Button clickRegion = new Button();
        clickRegion.setMaxWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(Double.MAX_VALUE);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();
        });

        bodyVBox.getChildren().addAll(passwordBox, clickRegion);

        Platform.runLater(() -> passwordField.requestFocus());

        passwordField.setOnKeyPressed(e1 -> {

            KeyCode keyCode = e1.getCode();

            if ((keyCode == KeyCode.ENTER || keyCode == KeyCode.TAB)) {

                if (passwordField.getText().length() > 6) {

                    String passStr = passwordField.getText();
                    // createPassField.setText("");
                    bodyVBox.getChildren().remove(clickRegion);

                    passwordField.setVisible(false);
     

                    Text reenterTxt = new Text("> Re-enter password:");
                    reenterTxt.setFill(txtColor);
                    reenterTxt.setFont(txtFont);

                    Platform.runLater(() -> createPassField2.requestFocus());

                    HBox secondPassBox = new HBox(reenterTxt, createPassField2);
                    secondPassBox.setPadding(inputInsets);
                    secondPassBox.setAlignment(Pos.CENTER_LEFT);

                    bodyVBox.getChildren().addAll(secondPassBox, clickRegion);

                    clickRegion.setOnAction(regionEvent -> {
                        createPassField2.requestFocus();
                    });

                    createPassField2.setOnKeyPressed(pressEvent -> {

                        KeyCode keyCode2 = pressEvent.getCode();

                        if (keyCode2 == KeyCode.ENTER) {

                            if (passStr.equals(createPassField2.getText())) {
                                bodyVBox.getChildren().clear();
                                setSetupStage(appStage, "Creating Settings - Netnotes", "Creating settings...", bodyVBox);
                             
                                Text savingFileTxt = new Text("> Location:  " + m_appDir.getAbsolutePath());
                                savingFileTxt.setFill(txtColor);
                                savingFileTxt.setFont(txtFont);

                                bodyVBox.getChildren().add(savingFileTxt);
                                FxTimer.runLater(Duration.ofMillis(100), () -> {
                                    try {

                                  
                                           
                                        createSettings(passStr, appStage);
                                        

                                       
                                    } catch (Exception e) {
                                        Alert err = new Alert(AlertType.ERROR, e.toString(), ButtonType.CLOSE);
                                        err.initOwner(appStage);
                                        err.showAndWait();
                                        shutdownNow();
                                    }
                                });
                            } else {
                    
                                bodyVBox.getChildren().remove(secondPassBox);
                                createPassField2.setText("");
                                passwordField.setText("");
                                passwordField.setVisible(true);
                                secondPassBox.getChildren().clear();
                      
                                Platform.runLater(() -> passwordField.requestFocus());
                            }
                        }
                    });
                }
            }
        });

    }

    

    private void createSettings(String password,Stage appStage) throws Exception {

        File settingsFile = new File( m_appDir.getCanonicalPath() + "\\" + SETTINGS_FILE_NAME);

        String hash = getBcryptHashString(password);

   
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("appKey", hash);
        jsonObj.addProperty("updates", m_updates);
        jsonObj.addProperty("autoUpdate", m_autoUpdate);
        if(m_autoRunFile != null && m_autoRun){
            jsonObj.addProperty("autoRunFile", m_autoRunFile.getCanonicalPath());
          
        }
        String jsonString = jsonObj.toString();

        Files.writeString(settingsFile.toPath(), jsonString);
        
        checkSetup(appStage);
   
    }

    public void checkSetup(Stage appStage) {
      
        try{
            boolean validJar = Utils.checkJar(m_appFile);
            m_appHashData = validJar ? new HashData(m_appFile) : null;  
            boolean validJava = m_javaVersion != null && (m_javaVersion.compareTo(new Version("17.0.0")) > -1);


             if((validJava && validJar) || (validJava && m_updates)){
               
                startApp(appStage);
            }else{
               
                visitWebsites(appStage);
            }
        }catch(IOException e){
            try {
                Files.writeString(logFile.toPath(), "\nSetup: Checking setup io error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
  
            }
            
            visitWebsites(appStage);
        }
       
        
    }

    public static String getBcryptHashString(String password) {
        SecureRandom sr;

        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {

            sr = new SecureRandom();
        }

        return BCrypt.with(BCrypt.Version.VERSION_2A, sr, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).hashToString(15, password.toCharArray());
    }


    private void visitWebsites(Stage appStage) {

        VBox bodyVBox = new VBox();
        setSetupStage(appStage, "Setup - Netnotes", "Setup", bodyVBox);
        appStage.show();
        boolean validJava = (m_javaVersion != null && (m_javaVersion.compareTo(new Version("17.0.0")) > -1));
        boolean validJar = Utils.checkJar(m_appFile);

        if(!validJar || !validJava){
            String errorString = !validJava ? "Environment: Java 17+\n" : "";
            errorString += (!validJar ? "Application: netnotes-x.x.x.jar" : "");

            Alert a = new Alert(AlertType.NONE, errorString, ButtonType.OK);
            a.setHeaderText("Requirements:");
            a.setTitle("Requirements - Setup - Netnotes");;
            a.initOwner(appStage);
            a.show();
        }


        Text getJavaTxt = new Text("> Java URL:");
        getJavaTxt.setFill(txtColor);
        getJavaTxt.setFont(txtFont);

        Button javaURLBtn = new Button(JAVA_URL);
        javaURLBtn.setFont(txtFont);
        javaURLBtn.setId("toolBtn");
        javaURLBtn.setAlignment(Pos.CENTER_LEFT);

        javaURLBtn.setOnAction(btnEvent -> {
            services.showDocument(JAVA_URL);
        });

        Button javaRequiredBtn = new Button(validJava ? "Enabled": "Disabled");
        javaRequiredBtn.setId("inactiveMainImageBtn");
        javaRequiredBtn.setFont(txtFont);
        javaRequiredBtn.setPrefWidth(30);
        javaRequiredBtn.setOnAction(e->{
            m_javaVersion = Utils.checkJava();
            if(m_javaVersion != null && (m_javaVersion.compareTo(new Version("17.0.0")) > -1)){
                javaRequiredBtn.setText("Enabled");
            }else{
                javaRequiredBtn.setText("Disabled");
            }
        });

        HBox javaUrlHbox = new HBox(getJavaTxt, javaURLBtn, javaRequiredBtn);
        javaUrlHbox.setAlignment(Pos.CENTER_LEFT);
        javaURLBtn.prefWidthProperty().bind(javaUrlHbox.widthProperty().subtract(getJavaTxt.getLayoutBounds().getWidth()).subtract(javaRequiredBtn.widthProperty()));
        HBox.setHgrow(javaUrlHbox, Priority.ALWAYS);

        bodyVBox.getChildren().addAll(javaUrlHbox);

        Text applicationTxt = new Text("> Application");
        applicationTxt.setFill(txtColor);
        applicationTxt.setFont(txtFont);

        TextField applicationTxtField = new TextField();
        applicationTxtField.setDisable(true);
        HBox.setHgrow(applicationTxtField,Priority.ALWAYS);

        HBox applicationTxtBox = new HBox(applicationTxt, applicationTxtField);
        applicationTxtBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(applicationTxtBox,Priority.ALWAYS);
        

        Text getJarTxt = new Text("  URL:");
        getJarTxt.setFill(txtColor);
        getJarTxt.setFont(txtFont);


        Button latestURLBtn = new Button(GitHub_USERDL_URL);
        latestURLBtn.setFont(txtFont);
        latestURLBtn.setId("inactiveMainImageBtn");
        latestURLBtn.setAlignment(Pos.CENTER_LEFT);
        latestURLBtn.setOnAction(btnEvent -> {
            services.showDocument(GitHub_USERDL_URL);
        });

        Image dlImage = new Image("/assets/cloud-download-30.png");
        
        ImageView dlImgView = new ImageView(dlImage);
        dlImgView.setPreserveRatio(true);
        dlImgView.setFitWidth(20);

        Tooltip getLatestAppTip = new Tooltip("Get latest: " + GitHub_ALL_RELEASES_URL);

        Button getLatestAppBtn = new Button();
        getLatestAppBtn.setTooltip(getLatestAppTip);
        getLatestAppBtn.setGraphic(dlImgView);
        getLatestAppBtn.setContentDisplay(ContentDisplay.CENTER);
        
        ProgressBar progressBar = new ProgressBar();
        HBox.setHgrow(progressBar,Priority.ALWAYS);

        HBox getJarBox = new HBox(getJarTxt, latestURLBtn, getLatestAppBtn);
        getJarBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox progressBox = new HBox(progressBar);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(60, 0,20,0));
        HBox.setHgrow(progressBox, Priority.ALWAYS);

        Text dlLabelText = new Text();
        dlLabelText.setFill(txtColor);
        dlLabelText.setFont(txtFont);

        HBox dlLabelBox = new HBox(dlLabelText);
        dlLabelBox.setAlignment(Pos.CENTER);

        progressBar.prefWidthProperty().bind(progressBox.widthProperty().subtract(120));

        HBox selectAppBox = new HBox();
     
        HBox gBox = new HBox();
        HBox nextBox = new HBox();

        getLatestAppBtn.setOnAction(e->{
      
            bodyVBox.getChildren().removeAll(selectAppBox, getJarBox, gBox, nextBox);
            

            bodyVBox.getChildren().addAll( progressBox, dlLabelBox);

            getApp(progressBar,dlLabelText, ()->{
        
                checkSetup(appStage);
            }, ()->{
         
                bodyVBox.getChildren().removeAll(progressBox, dlLabelBox);
                bodyVBox.getChildren().addAll(getJarBox, selectAppBox, gBox, nextBox );

            });

        });




        HBox.setHgrow(getJarBox, Priority.ALWAYS);
        latestURLBtn.prefWidthProperty().bind(getJarBox.widthProperty().subtract(getJarTxt.getLayoutBounds().getWidth()).subtract(getLatestAppBtn.widthProperty()));
    
        Region  jarBodySpacerRegion = new Region();
        jarBodySpacerRegion.setMinHeight(5);

         bodyVBox.getChildren().addAll(applicationTxtBox, getJarBox);


        Text appJarTxt = new Text("  File:");
        appJarTxt.setFill(txtColor);
        appJarTxt.setFont(txtFont);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select 'netnotes-x.x.x.jar'");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("netnotes-x.x.x", "*.jar"));

        SimpleObjectProperty<File> selectedJar = new SimpleObjectProperty<>(m_appFile);

        Button selectBtn = new Button(m_appFile != null ? m_appFile.getAbsolutePath() : "Select 'netnotes-x.x.x.jar'");
        selectBtn.setFont(txtFont);
        selectBtn.setId("toolBtn");
        selectBtn.setOnAction(btnEvent -> {
            File chosenFile = chooser.showOpenDialog(appStage);

            if(chosenFile != null){
                if (!Utils.checkJar(chosenFile)) {
                    Alert a = new Alert(AlertType.NONE, "Invalid file.\n\nPlease visit gitHub for the latest release.", ButtonType.CLOSE);
                    a.initOwner(appStage);
                    a.show();
                } else {
                    selectBtn.setText(chosenFile.getAbsolutePath());
                    selectedJar.set(chosenFile);
                }
            }
        });
       
        selectAppBox.getChildren().addAll(appJarTxt, selectBtn);
        selectAppBox.setAlignment(Pos.CENTER_LEFT);

        
        bodyVBox.getChildren().add(selectAppBox);

        Button nextBtn = new Button("Next");
        nextBtn.setId("toolSelected");
        nextBtn.setFont(txtFont);

        nextBtn.setOnAction(e->{
            
            if(selectedJar.get() != null){
                File chosenFile = selectedJar.get();
                String chosenFileName = chosenFile.getName();

                Version jarVersion = Utils.getFileNameVersion(chosenFileName);
                String unknownName = "netnotes-0.0.0.jar";

                String fileName = "";

                int versionTest = jarVersion.compareTo(new Version("0.0.0"));

                if (versionTest > 0) {
                    fileName = chosenFileName;
                } else {
                    fileName = unknownName;
                }
                if(!chosenFile.getParentFile().getAbsolutePath().equals(m_appDir.getAbsolutePath()) || !chosenFile.getName().equals(fileName)){
                    File newFile = new File(m_appDir.getAbsolutePath() + "/" + fileName);   

                    try {
                        Files.move(chosenFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        m_appFile = newFile;
                        
                    } catch (Exception e1) {

                        Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                        a.initOwner(appStage);
                        a.showAndWait();
                
                    }
                }
            }
            
            
                checkSetup(appStage);    
        });

        
     
        Region hBar = new Region();
        hBar.setPrefHeight(2);
        hBar.setId("hGradient");

       
        gBox.setAlignment(Pos.CENTER);
        gBox.setPadding(new Insets(25, 0, 0, 0));
        gBox.prefWidthProperty().bind(appStage.widthProperty());
        hBar.prefWidthProperty().bind(gBox.widthProperty().subtract(80));

        gBox.getChildren().add(hBar);

        nextBox.getChildren().add(nextBtn);
        nextBox.setAlignment(Pos.CENTER);
        nextBox.setPadding(new Insets(25, 0, 0, 0));
        
        HBox.setHgrow(nextBox,Priority.ALWAYS);

        bodyVBox.getChildren().addAll(gBox, nextBox);


 
        appStage.setHeight(480);
    }

   


    private static ImageView highlightedImageView(Image image) {

        ImageView imageView = new ImageView(image);

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {

            imageView.setEffect(colorAdjust);

        });
        imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setEffect(null);
        });

        return imageView;
    }

    public static void setSetupStage(Stage appStage, String title, String setupMessage, VBox bodyVBox) {

        appStage.setResizable(false);

        appStage.setTitle(title);
        appStage.getIcons().add(logo);

        Button closeBtn = new Button();
        closeBtn.setOnAction(closeClick -> {
            shutdownNow();
        });

        HBox topBar = createTopBar(icon, title, closeBtn, appStage);

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(20, 0, 20, 0));

        Text setupTxt = new Text("> " + setupMessage);
        setupTxt.setFill(txtColor);
        setupTxt.setFont(txtFont);

        Text spacerTxt = new Text(">");
        spacerTxt.setFill(txtColor);
        spacerTxt.setFont(txtFont);

        HBox line2 = new HBox(spacerTxt);
        line2.setAlignment(Pos.CENTER_LEFT);
        line2.setPadding(new Insets(10, 0, 6, 0));

        bodyVBox.getChildren().addAll(setupTxt, line2);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(topBar, imageBox, bodyVBox);

        Scene setupScene = new Scene(layoutVBox, 700, 425);
        setupScene.getStylesheets().add("/css/startWindow.css");

        appStage.setScene(setupScene);

    }

    private static void shutdownNow() {

        Platform.exit();
        System.exit(0);
    }

    public static HBox createTopBar(Image iconImage, String titleString, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitHeight(18);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(titleString);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(5, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    static class Delta {

        double x, y;
    }

    public void openJar() throws IOException  {
  
        if(!Utils.checkJar(m_appFile)){
            throw new IOException("Invalid jar file.");
        }
        
        JsonObject obj = new JsonObject();
        obj.addProperty("javaVersion", m_javaVersion.get());
        obj.addProperty("launcherFile", m_launcherFile.getAbsolutePath());
        obj.add("launcherHashData", m_launcherHashData.getJsonObject());

        if(m_updates && m_launcherUpdateFile != null && m_launcherUpdateFile.isFile()){
            obj.addProperty("launcherUpdateFile", m_launcherUpdateFile.getAbsolutePath());
            obj.add("launcherUpdateHashData", m_launcherUpdateHashData.getJsonObject());
        }
        if(m_firstRun){
            obj.addProperty("firstRun", true);
            obj.addProperty("autoRun", m_autoRun);
            obj.addProperty("desktopShortcut", m_desktopShortcut);
            obj.addProperty("startMenuShortcut", m_startMenuShortcut);
        }

        if(m_isDaemon && m_autoRunFile != null && m_autoRunFile.isFile()){
            obj.addProperty("isDaemon", true);
        }


        String hexJson = Hex.encodeHexString(obj.toString().getBytes());

        String[] cmdString = new String[]{"cmd", "/c", "javaw", "-jar", m_appFile.getCanonicalPath(), hexJson};

        Runtime.getRuntime().exec(cmdString);
        
   
          
        Platform.exit();
        System.exit(0);
    

    }
    
    public static Button createImageButton(Image image, String name) {
        ImageView btnImageView = new ImageView(image);
        btnImageView.setFitHeight(135);
        btnImageView.setPreserveRatio(true);

        Button imageBtn = new Button(name);
        imageBtn.setGraphic(btnImageView);
        imageBtn.setId("startImageBtn");
        imageBtn.setFont(mainFont);
        imageBtn.setContentDisplay(ContentDisplay.TOP);

        return imageBtn;
    }
    
    public static void showGetTextInput(Stage appStage, String prompt, String title, String heading, Image img, Button closeBtn,  TextField inputField) {

        appStage.setTitle(title);
        HBox titleBox = createTopBar(icon, title, closeBtn, appStage);

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPadding(new Insets(20, 0, 20, 0));

        Text setupTxt = new Text("> " + heading);
        setupTxt.setFill(txtColor);
        setupTxt.setFont(txtFont);

        Text spacerTxt = new Text(">");
        spacerTxt.setFill(txtColor);
        spacerTxt.setFont(txtFont);

        HBox line2 = new HBox(spacerTxt);
        line2.setAlignment(Pos.CENTER_LEFT);
        line2.setPadding(new Insets(10, 0, 6, 0));

        
        Text promptTxt = new Text("> " + prompt + ":");
        promptTxt.setFill(txtColor);
        promptTxt.setFont(txtFont);


        
        inputField.setFont(txtFont);
        inputField.setId("textField");

  

        HBox.setHgrow(inputField, Priority.ALWAYS);

        Platform.runLater(() -> inputField.requestFocus());

        HBox inputBox = new HBox(promptTxt, inputField);
        inputBox.setAlignment(Pos.CENTER_LEFT);

    
        VBox bodyBox = new VBox(setupTxt, line2, inputBox);
        bodyBox.setPadding(new Insets(5,10,0,20));

        VBox layoutVBox = new VBox(titleBox, imageBox,bodyBox);
        
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);
        
        Scene textInputScene = new Scene(layoutVBox, 625, 350);

        textInputScene.getStylesheets().add("/css/startWindow.css");

        appStage.setScene(textInputScene);

        appStage.setHeight(330);
    
        if(appStage.isIconified()){
            appStage.setIconified(false);
        }
        appStage.show();
  

        inputField.focusedProperty().addListener((obs, oldval, newVal)->{
            if(newVal == false){
                Platform.runLater(()->inputField.requestFocus());
            }            
        });

   

    }

    
    public static HBox createTopBar(Image iconImage, Label newTitleLbl, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(25);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }
    
    public static Scene getProgressScene(Image icon, String headingString, String titleContextString, String fileName, ProgressBar progressBar, Stage stage) {

        double defaultRowHeight = 40;
        Button closeBtn = new Button();
        

        Text fileNameProgressText = new Text(fileName + " (" + String.format("%.1f", progressBar.getProgress() * 100) + "%)");
        fileNameProgressText.setFill(txtColor);
        fileNameProgressText.setFont(txtFont);

        Label titleBoxLabel = new Label();
        titleBoxLabel.setTextFill(txtColor);
        titleBoxLabel.setFont(txtFont);
        titleBoxLabel.textProperty().bind(fileNameProgressText.textProperty());

        HBox titleBox = createTopBar(icon, titleBoxLabel, closeBtn, stage);

        closeBtn.setOnAction(e->shutdownNow());

        Text headingText = new Text(headingString);
        headingText.setFont(txtFont);
        headingText.setFill(txtColor);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(titleBox, headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        //Region progressLeftRegion = new Region();
        //progressLeftRegion.minWidthProperty().bind(stage.widthProperty().multiply(0.15));
        progressBar.prefWidthProperty().bind(stage.widthProperty().multiply(0.7));

        //  Region bodyTopRegion = new Region();
        HBox progressAlignmentBox = new HBox(progressBar);
        //  HBox.setHgrow(progressAlignmentBox, Priority.ALWAYS);
        progressAlignmentBox.setAlignment(Pos.CENTER);

       

        progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            fileNameProgressText.setText(fileName + " (" + String.format("%.1f", newVal.doubleValue() * 100) + "%)");
        });

        stage.titleProperty().bind(Bindings.concat(fileNameProgressText.textProperty(), " - ", titleContextString));

        HBox fileNameProgressBox = new HBox(fileNameProgressText);
        fileNameProgressBox.setAlignment(Pos.CENTER);
        fileNameProgressBox.setPadding(new Insets(20, 0, 0, 0));

        VBox colorBox = new VBox(progressAlignmentBox, fileNameProgressBox);
        colorBox.setId("bodyBox");
        HBox.setHgrow(colorBox, Priority.ALWAYS);
        colorBox.setPadding(new Insets(40, 0, 15, 0));

        VBox bodyBox = new VBox(colorBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));
        bodyBox.setAlignment(Pos.CENTER);

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);
        VBox layoutBox = new VBox(headerBox, bodyPaddingBox, footerBox);
        Scene coreFileProgressScene = new Scene(layoutBox, 600, 260);
        coreFileProgressScene.getStylesheets().add("/css/startWindow.css");

        // bodyTopRegion.minHeightProperty().bind(stage.heightProperty().subtract(30).divide(2).subtract(progressAlignmentBox.heightProperty()).subtract(fileNameProgressBox.heightProperty().divide(2)));
        bodyBox.prefHeightProperty().bind(stage.heightProperty().subtract(headerBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(10));
        return coreFileProgressScene;
    }

}
