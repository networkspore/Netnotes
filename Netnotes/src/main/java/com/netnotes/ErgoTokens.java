package com.netnotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;

public class ErgoTokens extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Ergo Tokens allows you to manage your interactions with the tokens on the Ergo Network.";
    public final static String SUMMARY = "Mange your tokens with Ergo Tokens.";
    public final static String NAME = "Ergo Tokens";

    private File logFile = new File("ErgoTokens-log.txt");
    private File m_appDir = null;
    private File m_tokensDir = null;

    private ArrayList<ErgoToken> m_tokensList = new ArrayList<>();

    public ErgoTokens(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.ERGO_TOKENS, networksData);
    }

    public ErgoTokens(JsonObject jsonObject, NetworksData networksData) {

        super(getAppIcon(), NAME, NetworkID.ERGO_TOKENS, networksData);

        JsonElement directoriesElement = jsonObject.get("directories");

        if (directoriesElement != null && directoriesElement.isJsonObject()) {
            JsonObject directoriesObject = directoriesElement.getAsJsonObject();
            if (directoriesObject != null) {
                JsonElement appDirElement = directoriesObject.get("app");
                JsonElement tokensDirElement = directoriesObject.get("tokens");
                m_appDir = appDirElement == null ? null : new File(appDirElement.getAsString());
                m_tokensDir = tokensDirElement == null ? null : new File(tokensDirElement.getAsString());
            }
        }

    }

    @Override
    public void open() {
        try {
            Files.writeString(logFile.toPath(), "\nOPEN " + getNetworkId(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        if (m_appDir == null || m_tokensDir == null) {
            setupErgoTokens();
        } else {
            if ((!m_appDir.isDirectory()) || (!m_tokensDir.isDirectory())) {
                setupErgoTokens();
            }
        }
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        return false;
    }

    public static Image getAppIcon() {
        return new Image("/assets/diamond-150.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/diamond-30.png");
    }

    public void setupErgoTokens() {

        m_appDir = m_appDir == null ? new File(System.getProperty("user.dir") + "/" + NAME) : m_appDir;

        m_tokensDir = m_tokensDir == null ? new File(System.getProperty("user.dir") + "/" + NAME + "/tokens") : m_tokensDir;

        try {
            Files.writeString(logFile.toPath(), "\nSETUP \n appDir:" + m_appDir.getAbsolutePath() + "\ntokensDir: " + m_tokensDir.getAbsolutePath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
        boolean createdtokensDirectory = false;
        if (!m_tokensDir.isDirectory()) {
            try {
                Files.createDirectories(m_tokensDir.toPath());
                createdtokensDirectory = true;

            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }

        if (m_tokensDir.isDirectory() && createdtokensDirectory) {
            try {
                Files.writeString(logFile.toPath(), "\nUnzipping", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            //    ClassLoader classloader = Thread.currentThread().getContextClassLoader();

            // new InputStreamReader();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/currencyIcons.zip");
            try {
                Files.writeString(logFile.toPath(), "\n" + is.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }

            ZipInputStream zipStream = null;
            try {
                zipStream = new ZipInputStream(is);

                String tokensPathString = m_tokensDir.getAbsolutePath() + "\\";
                if (zipStream != null) {
                    // Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {

                        String entryName = entry.getName();

                        try {
                            Files.writeString(logFile.toPath(), "\n" + entryName, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {

                        }

                        int indexOfDir = entryName.lastIndexOf("/");

                        if (indexOfDir != entryName.length() - 1) {

                            int indexOfExt = entryName.lastIndexOf(".");

                            String fileName = entryName.substring(0, indexOfExt);

                            File newDirFile = new File(tokensPathString + "\\" + fileName);
                            if (!newDirFile.isDirectory()) {
                                Files.createDirectory(newDirFile.toPath());
                            }

                            File entryFile = new File(tokensPathString + "\\" + fileName + "\\" + entryName);
                            OutputStream outStream = null;
                            try {

                                outStream = new FileOutputStream(entryFile);
                                //outStream.write(buffer);

                                byte[] buffer = new byte[8 * 1024];
                                int bytesRead;
                                while ((bytesRead = zipStream.read(buffer)) != -1) {
                                    outStream.write(buffer, 0, bytesRead);
                                }

                                addToken(fileName, entryFile);

                            } catch (IOException ex) {
                                try {
                                    Files.writeString(logFile.toPath(), "\n" + ex.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e1) {

                                }
                            } finally {
                                if (outStream != null) {
                                    outStream.close();
                                }
                            }
                        }

                    }
                }
            } catch (IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            } finally {
                if (zipStream != null) {

                    try {
                        zipStream.close();
                    } catch (IOException e2) {
                        try {
                            Files.writeString(logFile.toPath(), "\n" + e2.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e1) {

                        }
                    }

                }
            }

        }
        getLastUpdated().set(LocalDateTime.now());
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.add("directories", getDirectoriesJson());
        json.add("tokens", getTokensJsonArray());
        return json;
    }

    public JsonArray getTokensJsonArray() {
        JsonArray jsonArray = new JsonArray();
        for (ErgoToken ergoToken : m_tokensList) {
            jsonArray.add(ergoToken.getJsonObject());
        }
        return jsonArray;
    }

    public ErgoToken getErgoToken(String tokenid) {

        for (int i = 0; i < m_tokensList.size(); i++) {
            ErgoToken ergToken = m_tokensList.get(i);
            if (ergToken.getTokenId().equals(tokenid)) {
                return ergToken;
            }
        }
        return null;
    }

    public void addToken(String key, File imageFile) {
        ErgoToken tokenIcon = new ErgoToken(key, imageFile);
        if (tokenIcon != null) {
            m_tokensList.add(tokenIcon);
        }

    }

    public JsonObject getDirectoriesJson() {

        m_appDir = m_appDir == null ? new File(System.getProperty("user.dir") + "/" + NAME) : m_appDir;

        m_tokensDir = m_tokensDir == null ? new File(System.getProperty("user.dir") + "/" + NAME + "/tokens") : m_tokensDir;

        JsonObject dirJsonObject = new JsonObject();
        dirJsonObject.addProperty("app", m_appDir.getAbsolutePath());
        dirJsonObject.addProperty("tokens", m_tokensDir.getAbsolutePath());
        return dirJsonObject;
    }

}
