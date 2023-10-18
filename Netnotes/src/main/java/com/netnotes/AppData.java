package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.Utils;
import com.utils.Version;

import javafx.beans.property.SimpleObjectProperty;


public class AppData {
    private File logFile;
    public static final String SETTINGS_FILE_NAME = "settings.conf";
    public final static String SHORTCUT_NAME = "Netnotes.lnk";

    public static final String HOME_DIRECTORY = System.getProperty("user.home");
    public static final File DESKTOP_DIRECTORY = new File(HOME_DIRECTORY + "/Desktop");
    public static final File STARTUP_DIRECTORY = new File(HOME_DIRECTORY + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup");
    public static final File PROGRAMS_DIRECTORY = new File(HOME_DIRECTORY + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs");

    

    public File m_currentDirectory = null;
    public File m_settingsFile = null;

    private String m_appKey;
    private boolean m_isDaemon = false;

    private boolean m_updatesProperty = true;
    private boolean m_autoUpdateProperty = false;
    
    private File m_appFile = null;
    
    private HashData m_appHashData = null;

    private Version m_javaVersion = null;
    private SimpleObjectProperty<File> m_launcherFile = new SimpleObjectProperty<>();
    private SimpleObjectProperty<HashData> m_launcherHashData = new SimpleObjectProperty<>();

    private SimpleObjectProperty<SecretKey> m_secretKey = new SimpleObjectProperty<SecretKey>(null);

    private File m_autoRunFile = null;

    public AppData(String argString) throws Exception {

        URL classLocation = Utils.getLocation(getClass());
        m_appFile = Utils.urlToFile(classLocation);
        m_appHashData = new HashData(m_appFile);
        m_currentDirectory = m_appFile.getParentFile();

        logFile = new File(m_currentDirectory.getAbsolutePath() + "/" +"appdata-log.txt");
        m_settingsFile = new File(m_currentDirectory.getAbsolutePath() + "/" + SETTINGS_FILE_NAME);

        byte[] bytes = Hex.decode(argString);
        String jsonString = new String(bytes);
        JsonElement jsonStringElement = new JsonParser().parse(jsonString);
        JsonObject argsJson = jsonStringElement != null && jsonStringElement.isJsonObject() ? jsonStringElement.getAsJsonObject() : null;

        if(argsJson == null){
            try {
                Files.writeString(logFile.toPath(), "\nLauncher parameters unavailable.", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
            
            }
            throw new Exception("Launcher parameters unavailable.");
        }

        try {
            Files.writeString(logFile.toPath(), m_appHashData.getJsonObject().toString() + "\n" + argsJson.toString());
        } catch (IOException e) {
        
        }


        readFile();
        parseArgs(argsJson);
        if(m_autoRunFile != null && m_autoRunFile.isFile() && isDaemon()){
            Files.writeString(logFile.toPath(), "\nautorun enabled", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            loadAppKey();
        }else{
            Files.writeString(logFile.toPath(), "\nautorun disabled", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private void readFile() throws Exception{

        Files.writeString(logFile.toPath(), "\nopening: " + m_settingsFile.getAbsolutePath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        String settingsString = Files.readString(m_settingsFile.toPath());

        JsonObject json = new JsonParser().parse(settingsString).getAsJsonObject();
      
        Files.writeString(logFile.toPath(), "\n" + json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        openJson(json);

    
    }

    private void openJson(JsonObject dataObject) throws Exception{
        

        JsonElement autoRunKeyFileElement = dataObject.get("autoRunFile");
        JsonElement autoUpdateElement = dataObject.get("autoUpdate");
        JsonElement updatesElement = dataObject.get("updates");
        JsonElement appkeyElement = dataObject.get("appKey");

        boolean updates = updatesElement != null && updatesElement.isJsonPrimitive() ? updatesElement.getAsBoolean() : true;
        boolean autoUpdate = autoUpdateElement != null && autoUpdateElement.isJsonPrimitive() ? autoUpdateElement.getAsBoolean() : false;

        if (appkeyElement != null && appkeyElement.isJsonPrimitive()) {

            m_appKey = appkeyElement.getAsString();
            m_updatesProperty = updates;
            m_autoUpdateProperty = autoUpdate;
            m_autoRunFile = autoRunKeyFileElement != null && autoRunKeyFileElement.isJsonPrimitive() ? new File(autoRunKeyFileElement.getAsString()) : null;
         

        } else {
            throw new Exception("Null appKey");
        }
     
    }

    private void parseArgs(JsonObject argsJson) throws Exception{

        JsonElement javaVersionElement =  argsJson.get("javaVersion");
        JsonElement launcherFileElement = argsJson.get("launcherFile");
        JsonElement launcherHashDataElement = argsJson.get("launcherHashData");
        JsonElement isDaemonElement = argsJson.get("isDaemon");


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

        JsonElement launcherUpdateFileElement = argsJson.get("launcherUpdateFile");
        JsonElement launcherUpdateHashDataElement = argsJson.get("launcherUpdateHashData");
        JsonElement firstRunElement = argsJson.get("firstRun");
        
        boolean firstRun = firstRunElement != null && firstRunElement.isJsonPrimitive() ? firstRunElement.getAsBoolean() : false;
        
        File launcherUpdateFile = launcherUpdateFileElement != null && launcherUpdateFileElement.isJsonPrimitive() ? new File(launcherUpdateFileElement.getAsString()) : null;
        HashData launcherUpdateHashData = launcherUpdateHashDataElement != null && launcherUpdateHashDataElement.isJsonObject() ? new HashData(launcherUpdateHashDataElement.getAsJsonObject()) : null;

  

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

               
                File destinationFile = new File(m_currentDirectory.getAbsolutePath() + "/Netnotes.exe");
               
                
                if(launcherUpdateFile != null && launcherUpdateFile.isFile() && launcherUpdateHashData != null){
                    try {
                        Files.move(launcherUpdateFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        m_launcherFile.set(destinationFile);
                        m_launcherHashData.set(launcherUpdateHashData);
                     
                    } catch (IOException e) {
                        
                    }
                  
                }else{
                 
                    if (!m_launcherFile.get().getAbsolutePath().equals(destinationFile.getAbsolutePath())) {
                        try {
                            Files.move(m_launcherFile.get().toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            m_launcherFile.set( destinationFile);
                   
                        } catch (IOException e) {
                    
                        }
                    }
                    
                }
              

                if(firstRun){
                    JsonElement desktopShortcutElement = argsJson.get("desktopShortcut");
                    JsonElement startMenuShortcutElement = argsJson.get("startMenuShortcut");
                    JsonElement autoRunElement = argsJson.get("autoRun");
                    
                    boolean isDesktopShortcut = desktopShortcutElement != null && desktopShortcutElement.isJsonPrimitive() ? desktopShortcutElement.getAsBoolean() : false;
                    boolean isStartMenuShortcut = startMenuShortcutElement != null && startMenuShortcutElement.isJsonPrimitive() ? startMenuShortcutElement.getAsBoolean() : false;
                    boolean isAutoRun = autoRunElement != null && autoRunElement.isJsonPrimitive() ? autoRunElement.getAsBoolean() : false;

                    if(isDesktopShortcut){
                        try {
                            createDesktopLink();
                        } catch (Exception e) {
                            try {
                                Files.writeString(logFile.toPath(), "\ncreateDestkopShortcut failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                            
                            }
                        }
                    }
                    if(isStartMenuShortcut){
                      
                        try {
                            createStartMenuShortcut();
                        } catch (Exception e) {
                           try {
                                Files.writeString(logFile.toPath(), "\ncreateStartMenuShortcut failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                            
                            }
                        }
                     
                    }

                    if(isAutoRun){
                       
                        try {
                            createStartupLink();
                        } catch (Exception e) {
                            try {
                                Files.writeString(logFile.toPath(), "\ncreateStartupLink failed: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e1) {
                            
                            }
                        }
                    }
                   
                }

                 
            
                 try {
                    Files.writeString(logFile.toPath(), "\n" + "args parsed", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
               
                }
            }
        }).start();
                

       
    }

    public File getAppDir(){
        return m_currentDirectory;
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

    public void removeStartupLink() throws Exception{
        File oldLink = new File(STARTUP_DIRECTORY.getAbsolutePath() + "/" + SHORTCUT_NAME);
        if(oldLink.isFile()){
            oldLink.delete();
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

    private void loadAppKey() throws Exception  {
        if(m_autoRunFile != null && m_autoRunFile.isFile()){
            Files.writeString(logFile.toPath(), "\nLoading appkey: " + m_autoRunFile.length() ,StandardOpenOption.CREATE, StandardOpenOption.APPEND );
            byte[] fileBytes = Files.readAllBytes(m_autoRunFile.toPath());

            

            m_secretKey.set(new SecretKeySpec(fileBytes, "AES"));

             Files.writeString(logFile.toPath(), "\nsecret key loaded: " +m_secretKey.get().toString() ,StandardOpenOption.CREATE, StandardOpenOption.APPEND );
        }

    }

    
     public void createKey(char[] chars) throws Exception {

        byte[] charBytes = Utils.charsToBytes(chars);

        charBytes = Utils.digestBytesToBytes(charBytes);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(chars, charBytes, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        m_secretKey.set( new SecretKeySpec(tmp.getEncoded(), "AES"));

    }


    private void saveAppKey( char[] chars, File keyFile) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException{
        if(m_secretKey != null && m_secretKey.get() != null){

            byte[] charBytes = Utils.charsToBytes(chars);

            charBytes = Utils.digestBytesToBytes(charBytes);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            KeySpec spec = new PBEKeySpec(chars, charBytes, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            
            Files.write(keyFile.toPath(), tmp.getEncoded());
        }
    }

    public boolean isDaemon(){
        return m_isDaemon;
    }

    public File getAutoRunFile(){
        return m_autoRunFile;
    }


    public void enableAutoRun(String keyString, File autoRunFile) throws Exception{
       
        m_autoRunFile = autoRunFile;
        saveAppKey(keyString.toCharArray(), autoRunFile);
        createStartupLink();
        save();
    }
    

    public void disableAutoRun() throws Exception{
        if(m_autoRunFile != null && m_autoRunFile.isFile()){
            m_autoRunFile.delete();
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
        return m_updatesProperty;
    }

    public void setUpdates(boolean updates) throws IOException {
        m_updatesProperty = updates;
        save();
    }

    public boolean getAutoUpdateProperty(){
        return m_autoUpdateProperty;
    }

    public void setAutoUpdate(boolean value) throws IOException{
        m_autoUpdateProperty = value;
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
        dataObject.addProperty("updates", m_updatesProperty);
        dataObject.addProperty("autoUpdate", m_autoUpdateProperty);

        if(m_autoRunFile != null && m_autoRunFile.isFile()){
            dataObject.addProperty("autoRunFile", m_autoRunFile.getAbsolutePath());
        }

        return dataObject;
    }

    public void save() throws IOException {
        String jsonString = getJson().toString();
        Files.writeString(m_settingsFile.toPath(), jsonString);
    }
}
