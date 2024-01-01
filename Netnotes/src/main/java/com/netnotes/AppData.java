package com.netnotes;


import java.io.File;
import java.awt.Desktop;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.satergo.extra.AESEncryption;
import com.utils.Utils;
import com.utils.Version;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class AppData {
    private File logFile;
    public static final String SETTINGS_FILE_NAME = "settings.conf";
    public final static String SHORTCUT_NAME = "Netnotes.lnk";

    public static final String HOME_DIRECTORY = System.getProperty("user.home");
    public static final File DESKTOP_DIRECTORY = new File(HOME_DIRECTORY + "/Desktop");
    public static final File STARTUP_DIRECTORY = new File(HOME_DIRECTORY + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup");
    public static final File PROGRAMS_DIRECTORY = new File(HOME_DIRECTORY + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs");

    

    public File m_appDir = null;
    public File m_settingsFile = null;

    private String m_appKey;
    private boolean m_isDaemon = false;
    private boolean m_isFirstRun = false;

    private boolean m_updates = true;
    private boolean m_autoUpdate = false;
    
    private File m_appFile = null;
    
    private HashData m_appHashData = null;

    private Version m_javaVersion = null;
    private SimpleObjectProperty<File> m_launcherFile = new SimpleObjectProperty<>();
    private SimpleObjectProperty<HashData> m_launcherHashData = new SimpleObjectProperty<>();

    private SimpleObjectProperty<SecretKey> m_secretKey = new SimpleObjectProperty<SecretKey>(null);

    private File m_autoRunFile = null;
    private boolean m_setupAutoRun = false;

    private Stage m_persistenceStage = null;

   

    public AppData()throws Exception {

        URL classLocation = Utils.getLocation(getClass());
        m_appFile = Utils.urlToFile(classLocation);
        m_appHashData = new HashData(m_appFile);
        m_appDir = m_appFile.getParentFile();

        logFile = new File(m_appDir.getAbsolutePath() + "/" +"netnotes-log.txt");
        m_settingsFile = new File(m_appDir.getAbsolutePath() + "/" + SETTINGS_FILE_NAME);

       


        readFile();
        
        m_persistenceStage = new Stage(StageStyle.UTILITY);
        m_persistenceStage.setHeight(0);
        m_persistenceStage.setWidth(0);
        m_persistenceStage.setX(java.lang.Double.MAX_VALUE);
        m_persistenceStage.show();

      
    }

    private void readFile()throws Exception{

        String settingsString = Files.readString(m_settingsFile.toPath());
        JsonObject json = new JsonParser().parse(settingsString).getAsJsonObject();
      
        openJson(json);

    
    }

    private void openJson(JsonObject dataObject) throws Exception {
        

        JsonElement autoRunKeyFileElement = dataObject.get("autoRunFile");
        JsonElement autoUpdateElement = dataObject.get("autoUpdate");
        JsonElement updatesElement = dataObject.get("updates");
        JsonElement appkeyElement = dataObject.get("appKey");

        boolean updates = updatesElement != null && updatesElement.isJsonPrimitive() ? updatesElement.getAsBoolean() : true;
        boolean autoUpdate = autoUpdateElement != null && autoUpdateElement.isJsonPrimitive() ? autoUpdateElement.getAsBoolean() : false;

        if (appkeyElement != null && appkeyElement.isJsonPrimitive()) {

            m_appKey = appkeyElement.getAsString();
            m_updates = updates;
            m_autoUpdate = autoUpdate;
            if(autoRunKeyFileElement != null && autoRunKeyFileElement.isJsonPrimitive()){
                File autoRunFile = new File(autoRunKeyFileElement.getAsString());
                if(autoRunFile.isFile()){
                    m_autoRunFile = autoRunFile;
                }else{
                    m_autoRunFile = null;
                    try{
                        disableAutoRun();
                    }catch(Exception e){
                        Files.writeString(logFile.toPath(), "\nAutorun unavailable during load. Error disabling autorun", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                }
            }else{
                m_autoRunFile = null;
            }
         

        } else {
            throw new Exception("Null appKey");
        }
     
    }
   
    public void parseArgs(String argString, Runnable complete) throws Exception{

        byte[] bytes = Hex.decode(argString);
        String jsonString = new String(bytes);
        JsonElement jsonStringElement = new JsonParser().parse(jsonString);
        JsonObject argsJson = jsonStringElement != null && jsonStringElement.isJsonObject() ? jsonStringElement.getAsJsonObject() : null;

        if(argsJson == null){
            try {
                Files.writeString(logFile.toPath(), "\nAppdata: Launcher parameters unavailable.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
            
            }
            throw new Exception("Appdata: Launcher parameters unavailable.");
        }


        JsonElement javaVersionElement =  argsJson.get("javaVersion");
        JsonElement launcherFileElement = argsJson.get("launcherFile");
        JsonElement launcherHashDataElement = argsJson.get("launcherHashData");
        JsonElement isDaemonElement = argsJson.get("isDaemon");
        JsonElement firstRunElement = argsJson.get("firstRun");

        JsonElement launcherUpdateFileElement = argsJson.get("launcherUpdateFile");
        JsonElement launcherUpdateHashDataElement = argsJson.get("launcherUpdateHashData");
        
        

        String javaVersionString =  javaVersionElement != null && javaVersionElement.isJsonPrimitive() ? javaVersionElement.getAsString() : null;
        String launcherFileString = launcherFileElement != null && launcherFileElement.isJsonPrimitive() ? launcherFileElement.getAsString() : null;
        JsonObject launcherHashDataObject = launcherHashDataElement != null && launcherHashDataElement.isJsonObject() ? launcherHashDataElement.getAsJsonObject() : null;


        if(javaVersionString == null || launcherFileString == null || launcherHashDataObject == null){
            throw new Exception("launcher did not send args");
        }

        m_isDaemon = isDaemonElement != null && isDaemonElement.isJsonPrimitive() ?  isDaemonElement.getAsBoolean() : false;
        m_javaVersion = new Version(javaVersionString);
        m_launcherFile.set(new File(launcherFileString));
        m_launcherHashData.set(new HashData(launcherHashDataObject));

       
        m_isFirstRun = firstRunElement != null && firstRunElement.isJsonPrimitive() ? firstRunElement.getAsBoolean() : false;
        
        File launcherUpdateFile = launcherUpdateFileElement != null && launcherUpdateFileElement.isJsonPrimitive() ? new File(launcherUpdateFileElement.getAsString()) : null;
        HashData launcherUpdateHashData = launcherUpdateHashDataElement != null && launcherUpdateHashDataElement.isJsonObject() ? new HashData(launcherUpdateHashDataElement.getAsJsonObject()) : null;

        File destinationFile = new File(m_appDir.getAbsolutePath() + "/Netnotes.exe");

        boolean isLauncherUpdated = launcherUpdateFile != null && launcherUpdateFile.isFile() && launcherUpdateHashData != null;
        boolean isLauncherDirectory = !m_launcherFile.get().getAbsolutePath().equals(destinationFile.getAbsolutePath());

        if(isLauncherUpdated || isLauncherDirectory || m_isFirstRun){

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    
                    if(isLauncherUpdated && launcherUpdateFile != null){
                        try {
                            Files.move(launcherUpdateFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            m_launcherFile.set(destinationFile);
                            m_launcherHashData.set(launcherUpdateHashData);
                        
                        } catch (IOException e) {
                            
                        }
                    
                    }else{
                    
                        if (isLauncherDirectory) {
                            try {
                                Files.move(m_launcherFile.get().toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                m_launcherFile.set( destinationFile);
                    
                            } catch (IOException e) {
                        
                            }
                        }
                        
                    }
                

                    if(m_isFirstRun){
                        JsonElement desktopShortcutElement = argsJson.get("desktopShortcut");
                        JsonElement startMenuShortcutElement = argsJson.get("startMenuShortcut");
                        JsonElement autoRunElement = argsJson.get("autoRun");
                        
                        boolean isDesktopShortcut = desktopShortcutElement != null && desktopShortcutElement.isJsonPrimitive() ? desktopShortcutElement.getAsBoolean() : false;
                        boolean isStartMenuShortcut = startMenuShortcutElement != null && startMenuShortcutElement.isJsonPrimitive() ? startMenuShortcutElement.getAsBoolean() : false;
                        

                        if(isDesktopShortcut){
                            try {
                                createDesktopLink();
                            } catch (Exception e) {
                                try {
                                    Files.writeString(logFile.toPath(), "\nAppdata: createDestkopShortcut failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {
                                
                                }
                            }
                        }
                        if(isStartMenuShortcut){
                        
                            try {
                                createStartMenuShortcut();
                            } catch (Exception e) {
                            try {
                                    Files.writeString(logFile.toPath(), "\nAppdata: createStartMenuShortcut failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {
                                
                                }
                            }
                        
                        }
                        

                        m_setupAutoRun = autoRunElement != null && autoRunElement.isJsonPrimitive() && m_autoRunFile != null ? autoRunElement.getAsBoolean() : false;

                  
                        Platform.runLater(()->complete.run());
                
                    }else{
                         Platform.runLater(()->complete.run());
                    }

                }
            }).start();

        }else{
           
            complete.run();
            
        }
        
     
       
    }

    public boolean isFirstRun(){
        return m_isFirstRun;
    }

    public boolean isSetupAutoRun(){
        return m_setupAutoRun;
    }

    public File getAppDir(){
        return m_appDir;
    }

  

    public boolean isDesktopLink(){
        return (new File(DESKTOP_DIRECTORY.getAbsolutePath() + "/" + SHORTCUT_NAME)).isFile();
    }

    public void createDesktopLink() throws Exception{
        File oldLink = new File(DESKTOP_DIRECTORY.getAbsolutePath() + "/" + SHORTCUT_NAME);
        if(oldLink.isFile()){
            oldLink.delete();
        }
        

        Utils.createLink(m_launcherFile.get(), DESKTOP_DIRECTORY, SHORTCUT_NAME);
       
    }

    public void removeDesktopLink(){
        File desktopLinkFile = new File(DESKTOP_DIRECTORY.getAbsolutePath() + "/" + SHORTCUT_NAME);
        if(desktopLinkFile.isFile()){
            desktopLinkFile.delete();
        }
    }

    public void removeStartMenuShortcut() throws IOException {
        File startMenuDirectory= new File(PROGRAMS_DIRECTORY  .getAbsolutePath() + "/" + "Netnotes");
     
        FileUtils.deleteDirectory(startMenuDirectory);
      
    }

    public boolean isStartMenuShortcut(){
        
        File startMenuDirectory= new File(PROGRAMS_DIRECTORY.getAbsolutePath() + "/" + "Netnotes");
        if(!startMenuDirectory.isDirectory()){
            File programShortcut = new File(startMenuDirectory.getAbsolutePath() + "/" + SHORTCUT_NAME);
            return programShortcut.isFile();
        }else{
            return false;
        }     
    }

    public void createStartMenuShortcut() throws Exception{
        
        File startMenuDirectory= new File(PROGRAMS_DIRECTORY  .getAbsolutePath() + "/" + "Netnotes");
        if(!startMenuDirectory.isDirectory()){
          
            Files.createDirectory(startMenuDirectory.toPath());
           
        }
        
        File programShortcut = new File(startMenuDirectory.getAbsolutePath() + "/" + SHORTCUT_NAME);
        if(programShortcut.isFile()){
            programShortcut.delete();
        }

      
        Utils.createLink(m_launcherFile.get(), startMenuDirectory, SHORTCUT_NAME);
     

    }

    public void removeStartupLink(){
        File oldLink = new File(STARTUP_DIRECTORY.getAbsolutePath() + "/" + SHORTCUT_NAME);
        if(oldLink.isFile()){
            try{
                oldLink.delete();
            }catch(SecurityException e){

            }
        }
    }

    public void createStartupLink() throws Exception{
        
        removeStartupLink();
        
        Utils.createLink(m_launcherFile.get(),"--daemon", STARTUP_DIRECTORY, SHORTCUT_NAME);
          
       
    }

    public boolean isStartupShortcut(){
        if(STARTUP_DIRECTORY.isDirectory()){
            File startupFile = new File(STARTUP_DIRECTORY.getAbsolutePath() + "/" + SHORTCUT_NAME);
            if(startupFile.isFile()){
                return true;
            } 
        }
        return false;
    }

    public void loadAppKey(Runnable success, Runnable failed) throws Exception  {
        
        byte[] fileBytes = isAutorun() && !isFirstRun() ? Files.readAllBytes(m_autoRunFile.toPath()) : null;

        if(fileBytes != null){
             Utils.getWin32_BiosHashData((onBiosData)->{
                    Object biosDataObject = onBiosData.getSource().getValue();

                    if(biosDataObject != null && biosDataObject instanceof HashData){
                        HashData biosHashData = (HashData) biosDataObject;
                        Utils.getWin32_BaseboardHashData((onBaseboardData)->{
                        Object baseboardDataObject = onBaseboardData.getSource().getValue();

                        if(baseboardDataObject != null && baseboardDataObject instanceof HashData){
                            HashData baseboardHashData = (HashData) baseboardDataObject;
                            try{

                                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                                
                                    KeySpec idKeySpec = new PBEKeySpec(biosHashData.getHashStringHex().toCharArray(), baseboardHashData.getHashBytes(), 65536, 256);
                                    SecretKey idKeyTmpKey = factory.generateSecret(idKeySpec);
                
                                    byte[] iv = new byte[]{
                                        fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                                        fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                                        fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
                                    };

                                    ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);


                                    byte[] keyBytes = AESEncryption.decryptData(iv, new SecretKeySpec(idKeyTmpKey.getEncoded(), "AES"), encryptedData);
                                    

                                    m_secretKey.set(new SecretKeySpec(keyBytes, "AES"));
                                    success.run();
                                }catch(Exception e){
                                    failed.run();
                                }
                            }else{
                                failed.run();
                            }
                        },onBaseboardFailed ->failed.run());
                    }else{
                        failed.run();
                    }
            },(onbiosfailed)->failed.run());
            
        }else{
            failed.run();
        }
           
        

    }

    
    private void saveAppKey(String password ){
     
        Utils.getWin32_BiosHashData((onBiosData)->{
            Object biosDataObject = onBiosData.getSource().getValue();

            if(biosDataObject != null && biosDataObject instanceof HashData){
                HashData biosHashData = (HashData) biosDataObject;
                Utils.getWin32_BaseboardHashData((onBaseboardData)->{
                    Object baseboardDataObject = onBaseboardData.getSource().getValue();

                    if(baseboardDataObject != null && baseboardDataObject instanceof HashData){
                        HashData baseboardHashData = (HashData) baseboardDataObject;
                    
                         try {
                            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                        
                            KeySpec idKeySpec = new PBEKeySpec(biosHashData.getHashStringHex().toCharArray(), baseboardHashData.getHashBytes(), 65536, 256);
                            SecretKey idKeyTmpKey = factory.generateSecret(idKeySpec);

                            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
                            byte[] iV = new byte[12];
                            secureRandom.nextBytes(iV);

                            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);
                            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(idKeyTmpKey.getEncoded(), "AES"), parameterSpec);

                            
                            byte[] encryptedData = cipher.doFinal(createKeyBytes(password));
                            
                            Path autorunFilePath = m_autoRunFile.toPath();
                           
                            Files.write(autorunFilePath, iV);
                           
                            Files.write(autorunFilePath, encryptedData, StandardOpenOption.APPEND);

                        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException  e) {
                            try {
                                Files.writeString(logFile.toPath(), "Create autorunkey failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                       
                            }
                        }
                        
                    }else{
                        try {
                            Files.writeString(logFile.toPath(), "Create autorunkey failed: baseboard returned null", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {
                    
                        }
                    }
                }, (onBaseboardFailed)->{
                    try {
                        Files.writeString(logFile.toPath(), "bios hashdata faield: " + onBaseboardFailed.getSource().getMessage(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                
                    }
                });
            }else{
                try {
                    Files.writeString(logFile.toPath(), "Create autorunkey failed: bios returned null", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {
            
                }
            }
        }, (onNoBiosData)->{});
    
    }


    
     public void createKey(String password) throws Exception {


        m_secretKey.set( new SecretKeySpec(createKeyBytes(password), "AES"));

    }

    public byte[] createKeyBytes(String password) throws NoSuchAlgorithmException, InvalidKeySpecException  {

        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);

    

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(password.toCharArray(), bytes, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();

    }


    public boolean isAutorun(){
        return m_autoRunFile != null && m_autoRunFile.isFile() && m_isDaemon;
    }

    public File getAutoRunFile(){
        return m_autoRunFile;
    }
    public void setAutoRunFile(File file){
        m_autoRunFile = file;
        try {
            save();
        } catch (IOException e) {
  
        }
    }

    public void enableAutoRun(String password) throws Exception{
   
        saveAppKey(password);
        createStartupLink();
 
    }
    

    public void disableAutoRun() throws Exception{
        if(m_autoRunFile != null && m_autoRunFile.isFile()){
            Desktop.getDesktop().open(m_autoRunFile.getParentFile());
        }
        m_autoRunFile = null;
        removeStartupLink();
        save();
    }

    public String getAppKey() {
        return m_appKey;
    }

    public byte[] getAppKeyBytes() {
        return m_appKey.getBytes();
    }

    public void setAppKey(String keyHash) throws IOException {
        m_appKey = keyHash;
        save();
    }

    public boolean getUpdates() {
        return m_updates;
    }

    public void setUpdates(boolean updates) throws IOException {
        m_updates = updates;
        save();
    }


    public void setAutoUpdate(boolean value) throws IOException{
        m_autoUpdate = value;
        save();
    }

    public Version getJavaVersion(){
        return m_javaVersion;
    }

    public HashData appHashData(){
        return m_appHashData;
    }

    public File appFile(){
        return m_appFile;
    }

    public SimpleObjectProperty<SecretKey> appKeyProperty() {
        return m_secretKey;
    }

    public void setAppKey(SecretKey secretKey) {
        m_secretKey.set(secretKey);
    }

    public JsonObject getJson() {
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("appKey", m_appKey);
        dataObject.addProperty("updates", m_updates);
        dataObject.addProperty("autoUpdate", m_autoUpdate);

        if(m_autoRunFile != null){
            dataObject.addProperty("autoRunFile", m_autoRunFile.getAbsolutePath());
        }

        return dataObject;
    }

    public void save() throws IOException {
        String jsonString = getJson().toString();
        Files.writeString(m_settingsFile.toPath(), jsonString);
    }
}
