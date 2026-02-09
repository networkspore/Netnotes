package io.netnotes.system;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.crypto.SecretKey;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

import io.netnotes.noteFiles.FileStreamUtils;

import io.netnotes.engine.crypto.AsymmetricPairs;
import io.netnotes.engine.crypto.CryptoService;
import io.netnotes.engine.crypto.HashServices;

import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteRandom;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteBytes.processing.RandomService;
import io.netnotes.engine.utils.JarHelpers;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.virtualExecutors.SerializedVirtualExecutor;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

public class SettingsData {
    public static final NoteBytes BCRYPT_KEY = new NoteBytes("bcrypt_key");
    public static final NoteBytes SALT_KEY = new NoteBytes("salt_key"); 
    public static final NoteBytes OLD_BCRYPT_KEY = new NoteBytes("old_bcrypt");
    public static final NoteBytes OLD_SALT_KEY = new NoteBytes("old_salt"); 

    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException(String msg) { super(msg); }
    }

    private static final String SETTINGS_FILE_NAME = "settings.dat";
    private static final String SYSTEM_CONFIG_FILE_NAME = "syscfg.dat";

     
    private static File m_appDir = null;
    private static File m_appFile = null;
    
    static {
        try {
            URL classLocation = JarHelpers.getLocation(SettingsData.class);
            m_appFile = JarHelpers.urlToFile(classLocation);
            m_appDir = m_appFile.getParentFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static File getSystemConfigFile() {
        File dataDir = getAppDataDir();
        return new File(dataDir, SYSTEM_CONFIG_FILE_NAME);
    }
    
    public static File getAppDataDir() {
        File dataDir = new File(m_appDir, "data");
        if (!dataDir.isDirectory()) {
            try {
                Files.createDirectory(dataDir.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Cannot create data directory", e);
            }
        }
        return dataDir;
    }
    
    public static File getAppDir() {
        return m_appDir;
    }
    
    public static File getAppFile() {
        return m_appFile;
    }
    
    public static File getDataDir() {
        return getAppDataDir();
    }

    public static File getIdDataFile(){
        File dataDir = SettingsData.getDataDir();
        File idDataFile = new File(dataDir.getAbsolutePath() + "/data.dat");
        return idDataFile;
    }

    public static boolean isIdDataFile(){
        File dataFile = getIdDataFile();
        return dataFile.isFile() && dataFile.exists();
    }
    


    private SecretKey m_oldKey = null;
    private SecretKey m_secretKey = null;

    private AsymmetricPairs m_asymPairs = new AsymmetricPairs();
    private AsymmetricPairs m_oldAsymPairs = null;
    
    private NoteBytes m_oldSalt = null;
    private NoteBytes m_oldBcrypt = null;
    private NoteBytes m_salt = null;
    private NoteBytes m_bcryptKey = null;


    public SettingsData(SecretKey secretKey, NoteBytes salt, NoteBytes bcrypt, NoteBytes... oldValues){
        m_salt = salt;
        m_secretKey = secretKey;
        m_bcryptKey = bcrypt;
        if(oldValues != null && oldValues.length > 0){
            m_oldSalt = oldValues[0];
            if(oldValues.length > 1){
                m_oldBcrypt = oldValues[1];
            }
        }
    }

    public boolean hasOldKey() {
        return m_oldKey != null && m_oldSalt != null;
    }

   
    public SecretKey getOldKey(){
        return m_oldKey;
    }

    public NoteBytes oldSalt(){
        return m_oldSalt;
    }

    /**
     * Clear old key/salt (after successful password change completion)
     */
    public void clearOldKey() {
        m_oldKey = null;
        m_oldSalt = null;
        m_oldBcrypt = null;
        try{
            save();
            Log.logMsg("[SettingsData] Old key/salt cleared");
        }catch(IOException e){
          
            Log.logError("[SettingsData] Old key/salt cleared, but not saved:\n" + e.toString());
            e.printStackTrace();
        }
       
    }

    public CompletableFuture<Boolean> verifyPassword(NoteBytesEphemeral password){
        NoteBytesEphemeral copy = password.copy();
        return CompletableFuture.supplyAsync(()->{
            try(copy){
                
                return HashServices.verifyBCryptPassword(password, m_bcryptKey);
            }
        },  VirtualExecutors.getVirtualExecutor());
    }

    public CompletableFuture<Boolean> verifyPassword(
        SerializedVirtualExecutor exec,
        NoteBytesEphemeral password
    ){
        NoteBytesEphemeral copy = password.copy();
        return exec.submit(() -> {
            try (copy) {
                return HashServices.verifyBCryptPassword(password, m_bcryptKey);
            }
        });
    }

    public static CompletableFuture<Boolean> verifyPassword(NoteBytesEphemeral password, NoteBytesMap map){
        NoteBytesEphemeral copy = password.copy();
        return CompletableFuture.supplyAsync(()->{
            try(copy){
                NoteBytes bcrypt = map.get(BCRYPT_KEY);
                return HashServices.verifyBCryptPassword(password, bcrypt);
            }
        }, VirtualExecutors.getVirtualExecutor());
    }

    public static CompletableFuture<Boolean> verifyPassword(
        SerializedVirtualExecutor exec,
        NoteBytesEphemeral password,
        NoteBytesMap map
    ){
        NoteBytesEphemeral copy = password.copy();
        return exec.submit(() -> {
            try (copy) {
                NoteBytes bcrypt = map.get(BCRYPT_KEY);
                return HashServices.verifyBCryptPassword(password, bcrypt);
            }
        });
    }



    public CompletableFuture<Boolean> verifyOldPassword(NoteBytesEphemeral password){
        NoteBytesEphemeral copy = password.copy();
        return CompletableFuture.supplyAsync(()->{
            try(copy){
                if(HashServices.verifyBCryptPassword(copy, m_oldBcrypt)){
                    m_oldKey = CryptoService.createKey(copy, m_oldSalt);
                    return true;
                }
                return false;
            }catch(Exception e){
                throw new CompletionException("Crypto exception", e);
            }
        }, VirtualExecutors.getVirtualExecutor());
    }

    public CompletableFuture<Boolean> verifyOldPassword(
        SerializedVirtualExecutor exec,
        NoteBytesEphemeral password
    ){
        NoteBytesEphemeral copy = password.copy();
        return exec.submit(() -> {
            try (copy) {
                if (HashServices.verifyBCryptPassword(copy, m_oldBcrypt)) {
                    m_oldKey = CryptoService.createKey(copy, m_oldSalt);
                    return true;
                }
                return false;
            } catch (Exception e) {
                throw new CompletionException("Crypto exception", e);
            }
        });
    }



    public void updatePassword(NoteBytesEphemeral oldPassword, NoteBytesEphemeral newPassword) throws InvalidPasswordException, InvalidKeySpecException, NoSuchAlgorithmException, IOException{
        if(HashServices.verifyBCryptPassword(oldPassword, m_bcryptKey)){

            m_oldKey = m_secretKey;
            m_oldSalt = m_salt;
            m_oldBcrypt = m_bcryptKey;

            NoteBytes salt = new NoteBytes(RandomService.getRandomBytes(16));

            NoteBytes bcrypt = HashServices.getBcryptHash(newPassword);
            SecretKey secretKey = CryptoService.createKey(newPassword, salt);

            m_salt = salt;
            m_bcryptKey = bcrypt;
            m_secretKey = secretKey;

            save();
        }else{
            throw new InvalidPasswordException("Invalid password");
        }
    }



    /**
     * Rollback to old password state
     * Swaps current key/salt with old key/salt and saves to disk
     * 
     * This is used during recovery when user wants to restore the previous password
     * after a failed password change operation.
     * 
     * Requirements:
     * - m_oldKey and m_oldSalt must be available (non-null)
     * - System must not have been restarted since password change
     * 
     * Process:
     * 1. Verify old key/salt exist
     * 2. Swap: current ↔ old
     * 3. Save to disk (old password becomes active again)
     * 
     * @throws IllegalStateException if old key/salt not available
     * @throws IOException if save fails
     */
    public void rollbackToOldPassword() throws IllegalStateException, IOException {
        if (m_oldKey == null || m_oldSalt == null || m_oldBcrypt == null) {
            throw new IllegalStateException(
                "Cannot rollback: old key and salt not available. " +
                "Rollback is only possible if system hasn't restarted since password change.");
        }
        
        Log.logMsg("[SettingsData] Rolling back to old password");
        
        // Save current as temporary
        SecretKey tempKey = m_secretKey;
        NoteBytes tempSalt = m_salt;
        NoteBytes tempBcrypt = m_bcryptKey;
        
        // Restore old as current
        m_secretKey = m_oldKey;
        m_salt = m_oldSalt;
        m_bcryptKey = m_oldBcrypt;

        m_oldBcrypt = tempBcrypt;
        m_oldSalt = tempSalt;
        m_oldKey = tempKey;
        save();
    }


    public NoteBytes getOldBCrypt(){
        return m_oldBcrypt;
    }

    public SecretKey getSecretKey(){
        return m_secretKey;
    }

    public CompletableFuture<AsymmetricPairs> getAsymmetricPairs(NoteBytesEphemeral password){
        return verifyPassword(password)
            .thenApply(verified->{
                if(verified){
                    return m_asymPairs;
                }else{
                    throw new CompletionException("Verification required", new InvalidPasswordException("Invalid password"));
                }

            });
    }

    

    public CompletableFuture<AsymmetricPairs> rotateKeys(NoteBytesEphemeral password){
        return verifyPassword(password)
            .thenApply(verified->{
                if(verified){
                    m_oldAsymPairs = m_asymPairs;
                    m_asymPairs = new AsymmetricPairs();

                    return m_asymPairs;
                }else{
                    throw new CompletionException("Verification required", new InvalidPasswordException("Invalid password"));
                }

            });
    }

    public CompletableFuture<AsymmetricPairs> undoRotateKeys(NoteBytesEphemeral password){
        if(m_oldAsymPairs != null){
            return verifyPassword(password)
                .thenApply(verified->{
                    if(verified){
                    
                        m_asymPairs = m_oldAsymPairs;
                        m_oldAsymPairs = m_asymPairs;

                        return m_asymPairs;
                    }else{
                        throw new CompletionException("Verification required", new InvalidPasswordException("Invalid password"));
                    }

                });
        }else{
            return CompletableFuture.failedFuture(new NullPointerException("Asym pairs are null"));
        }
    }

    public void clearOldAsymmetricPairs(){
        m_oldAsymPairs = null;
    }

    public Ed25519PublicKeyParameters getSigningPublicKey(){
        return m_asymPairs.getSigningPublicKey();
    }

    public X25519PublicKeyParameters getExchangePublicKey(){
        return m_asymPairs.getExchangePublicKey();
    }

    public void setSecretKey(SecretKey secretKey){
        m_secretKey = secretKey;
    }


    public NoteBytes getBCryptKey() {
        return m_bcryptKey;
    }

    public byte[] getBCryptKeyBytes() {
        return m_bcryptKey.getBytes();
    }

    public void setBCryptKey(NoteBytes hash) throws IOException {
        m_bcryptKey = hash;
    }

    public static File getSettingsFile(){
        File dataDir = getDataDir();
        
        return new File(dataDir.getAbsolutePath() + "/" + SETTINGS_FILE_NAME);
        
    }

    public void save() throws IOException {
        if(m_oldBcrypt != null && m_oldSalt != null){
            save( 
                new NoteBytesPair(BCRYPT_KEY, m_bcryptKey),
                new NoteBytesPair(SALT_KEY, m_salt),
                new NoteBytesPair(OLD_BCRYPT_KEY, m_oldBcrypt),
                new NoteBytesPair(OLD_SALT_KEY, m_oldSalt)
            );
        }else{
            save( 
                new NoteBytesPair(BCRYPT_KEY, m_bcryptKey),
                new NoteBytesPair(SALT_KEY, m_salt)
            );
        }
        
    }

    private static void save( NoteBytesPair... pairs) throws IOException{
        FileStreamUtils.writeFileNoteBytes( getSettingsFile(), new NoteBytesObject(pairs));
    }

    public void shutdown(){

    }

    public static boolean isSettingsData(){
        File settingsFile = getSettingsFile();
        if(settingsFile.exists() && settingsFile.isFile()){
            return true;
        }

        return false;
    }

    public static boolean isSystemConfigData(){
        File sysCfgFile = getSystemConfigFile();
        if(sysCfgFile.exists() && sysCfgFile.isFile()){
            return true;
        }

        return false;
    }

    public static CompletableFuture<Void> saveSystemConfig( NoteBytesMap map){
        return CompletableFuture.runAsync(()->{
            try {
                saveSystemConfig( map.toNoteBytes());
            } catch (IOException e) {
                throw new CompletionException("Failed to save", e);
            }

        }, VirtualExecutors.getVirtualExecutor());
    }

    public static CompletableFuture<Void> saveSystemConfig(
        SerializedVirtualExecutor exec,
        NoteBytesMap map
    ){
        return exec.submit(() -> {
            try {
                saveSystemConfig(map.toNoteBytes());
            } catch (IOException e) {
                throw new CompletionException("Failed to save", e);
            }
            return null;
        });
    }
    
    public static void saveSystemConfig( NoteBytesObject nbo) throws IOException{
        File file = getSystemConfigFile();

        FileStreamUtils.writeFileNoteBytes(file, nbo);
    }
    

    public static CompletableFuture<SettingsData> createSettings(NoteBytesEphemeral password){
        NoteBytesEphemeral pass = password.copy();

        return CompletableFuture.supplyAsync(()->{
            try{
                if(pass.byteLength() < 6){
                    throw new InvalidPasswordException("Password must be at least 6 characters long");
                }
                NoteBytes bcrypt = HashServices.getBcryptHash(pass);
                NoteBytes salt = new NoteRandom(16);
                SettingsData settingsData = new SettingsData(CryptoService.createKey(pass, salt), salt,  bcrypt);
                settingsData.save();
                return settingsData;
            }catch(Exception e){
                throw new CompletionException("Could not create settings", e);
            }finally{
                pass.close();
            }
        }, VirtualExecutors.getVirtualExecutor());
    }

    public static CompletableFuture<SettingsData> createSettings(
        SerializedVirtualExecutor exec,
        NoteBytesEphemeral password
    ){
        NoteBytesEphemeral pass = password.copy();

        return exec.submit(() -> {
            try {
                if (pass.byteLength() < 6) {
                    throw new InvalidPasswordException("Password must be at least 6 characters long");
                }
                NoteBytes bcrypt = HashServices.getBcryptHash(pass);
                NoteBytes salt = new NoteRandom(16);
                SettingsData settingsData = new SettingsData(
                    CryptoService.createKey(pass, salt), salt, bcrypt
                );
                settingsData.save();
                return settingsData;
            } catch (Exception e) {
                throw new CompletionException("Could not create settings", e);
            } finally {
                pass.close();
            }
        });
    }

    public static CompletableFuture<NoteBytesMap> loadSettingsMap(){

        return CompletableFuture.supplyAsync(()->{
            try{
                File settingsFile = getSettingsFile();
                NoteBytes noteBytes = FileStreamUtils.readFileNextNoteBytes(settingsFile);
                if(noteBytes != null && noteBytes.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
                    return noteBytes.getAsMap();
                }
                throw new IllegalStateException("File is invalid");
            }catch(Exception e){
                throw new CompletionException("Settings could not be read", e);
            }

        }, VirtualExecutors.getVirtualExecutor());
    }

    public static CompletableFuture<NoteBytesMap> loadSettingsMap(SerializedVirtualExecutor exec){

        return exec.submit(()->{
            try{
                File settingsFile = getSettingsFile();
                NoteBytes noteBytes = FileStreamUtils.readFileNextNoteBytes(settingsFile);
                if(noteBytes != null && noteBytes.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
                    return noteBytes.getAsMap();
                }
                throw new IllegalStateException("File is invalid");
            }catch(Exception e){
                throw new CompletionException("Settings could not be read", e);
            }
        });
    }

    public static CompletableFuture<NoteBytesMap> loadSystemConfig(SerializedVirtualExecutor exec){

        return exec.submit(()->{
            try{
                File syscfgFile = getSystemConfigFile();
                NoteBytes noteBytes = FileStreamUtils.readFileNextNoteBytes(syscfgFile);
                if(noteBytes != null && noteBytes.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
                    return noteBytes.getAsMap();
                }
                throw new IllegalStateException("File is invalid");
            }catch(Exception e){
                throw new CompletionException("Settings could not be read", e);
            }

        });
    }




    public static CompletableFuture<SettingsData> loadSettingsData(NoteBytesEphemeral pass, NoteBytesMap map){
        final NoteBytesEphemeral password = pass.copy();
        return CompletableFuture.supplyAsync(()->{
            try(password){
                NoteBytes bcryptKey = map.get(BCRYPT_KEY);
                NoteBytes salt = map.get(SALT_KEY);
                NoteBytes oldSalt = map.get(OLD_SALT_KEY);
                NoteBytes oldBcrypt = map.get(OLD_BCRYPT_KEY);
                if(salt != null && bcryptKey != null){
                    if(oldSalt != null){
                        if(oldBcrypt != null){
                            return new SettingsData(CryptoService.createKey(password, salt), salt, bcryptKey, oldSalt, oldBcrypt);
                        }else{
                            return new SettingsData(CryptoService.createKey(password, salt), salt, bcryptKey, oldSalt);
                        }
                    }else{
                        return new SettingsData(CryptoService.createKey(password, salt), salt, bcryptKey);
                    }
                }else{
                    String saltString = salt == null ? "Salt unavailable file is corrupt" : "";
                    String bcryptString = bcryptKey == null ? "Key is unavailable file is corrupt" : "";
                    throw new NullPointerException(bcryptString + ", " + saltString);
                }
            }catch(Exception e){
                  throw new CompletionException(e);
            }
        }, VirtualExecutors.getVirtualExecutor());
    }

    public static CompletableFuture<SettingsData> loadSettingsData(
        SerializedVirtualExecutor exec,
        NoteBytesEphemeral pass,
        NoteBytesMap map
    ){
        final NoteBytesEphemeral password = pass.copy();
        return exec.submit(() -> {
            try (password) {
                NoteBytes bcryptKey = map.get(BCRYPT_KEY);
                NoteBytes salt = map.get(SALT_KEY);
                NoteBytes oldSalt = map.get(OLD_SALT_KEY);
                NoteBytes oldBcrypt = map.get(OLD_BCRYPT_KEY);
                if (salt != null && bcryptKey != null) {
                    if (oldSalt != null) {
                        if (oldBcrypt != null) {
                            return new SettingsData(
                                CryptoService.createKey(password, salt),
                                salt,
                                bcryptKey,
                                oldSalt,
                                oldBcrypt
                            );
                        } else {
                            return new SettingsData(
                                CryptoService.createKey(password, salt),
                                salt,
                                bcryptKey,
                                oldSalt
                            );
                        }
                    } else {
                        return new SettingsData(
                            CryptoService.createKey(password, salt),
                            salt,
                            bcryptKey
                        );
                    }
                } else {
                    String saltString = salt == null ? "Salt unavailable file is corrupt" : "";
                    String bcryptString = bcryptKey == null ? "Key is unavailable file is corrupt" : "";
                    throw new NullPointerException(bcryptString + ", " + saltString);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

}
