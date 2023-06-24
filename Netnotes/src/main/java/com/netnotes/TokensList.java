package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.netnotes.Network.NetworkID;
import com.satergo.extra.AESEncryption;

import javafx.application.Platform;

import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

public class TokensList extends Network {

    private File logFile = new File("tokensList-log.txt");

    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private VBox m_buttonGrid = null;
    // private NoteInterface m_parent = null;

    public TokensList(NetworkType networkType, NoteInterface noteInterface) {
        super(null, "Ergo Tokens - List (" + networkType.toString() + ")", "TOKENS_LIST", noteInterface);

        try {
            Files.writeString(logFile.toPath(), "\n" + getName(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        getFile(networkType);

    }

    public void getFile(NetworkType networkType) {
        if (getParentInterface().getNetworksData().getNoteInterface(NetworkID.ERGO_TOKENS) != null) {
            getParentInterface().getNetworksData().getNoteInterface(NetworkID.ERGO_TOKENS).sendNote(getDataFileLocation(networkType), onSuccess -> {
                WorkerStateEvent successEvent = onSuccess;

                Object sourceObject = successEvent.getSource().getValue();

                if (sourceObject != null && sourceObject instanceof String) {

                    String fileString = (String) sourceObject;
                    File dataFile = new File(fileString);
                    if (dataFile.isFile()) {
                        if (networkType == NetworkType.MAINNET) {

                            readFile(getParentInterface().getNetworksData().getAppKey(), dataFile.toPath());

                        } else {
                            openTestnetFile(dataFile.toPath());
                        }
                    }
                }
                Platform.runLater(() -> updateGrid());
            }, onFailed -> {

            });

        }
    }

    @Override
    public ArrayList<NoteInterface> getTunnelNoteInterfaces() {
        return m_noteInterfaceList;
    }

    public JsonObject getDataFileLocation(NetworkType networkType) {
        JsonObject getTokensFileObject = new JsonObject();
        getTokensFileObject.addProperty("subject", "GET_DATAFILE_LOCATION");
        getTokensFileObject.addProperty("networkType", networkType.toString());
        return getTokensFileObject;
    }

    public JsonObject getShutdownObject() {
        JsonObject shutdownObject = new JsonObject();
        shutdownObject.addProperty("subject", "SHUTDOWN_NOW");
        shutdownObject.addProperty("caller", getParentInterface().getNetworkId());
        return shutdownObject;
    }

    public void closeAll() {
        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            noteInterface.sendNote(getShutdownObject(), null, null);
        }
    }

    public void setNetworkType(NetworkType networkType) {
        closeAll();
        m_noteInterfaceList.clear();

        getFile(networkType);

    }

    public void openTestnetFile(Path filePath) {
        m_noteInterfaceList.clear();

        if (filePath != null) {
            try {
                JsonElement jsonElement = new JsonParser().parse(Files.readString(filePath));

                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject());
                }
            } catch (JsonParseException | IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nInvalid testnet file: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }
        }
    }

    public VBox getButtonGrid() {
        if (m_buttonGrid == null) {
            m_buttonGrid = new VBox();
        }

        updateGrid();

        return m_buttonGrid;
    }

    public void updateGrid() {
        if (m_buttonGrid != null) {
            int numCells = m_noteInterfaceList.size();

            m_buttonGrid.getChildren().clear();
            // VBox.setVgrow(m_buttonGrid, Priority.ALWAYS);

            for (int i = 0; i < numCells; i++) {
                NoteInterface noteInterface = m_noteInterfaceList.get(i);

                IconButton rowButton = noteInterface.getButton();

                m_buttonGrid.getChildren().add(rowButton);
                rowButton.prefWidthProperty().bind(m_buttonGrid.widthProperty());
            }
        }
    }

    public NoteInterface getErgoToken(String tokenid) {

        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            if (noteInterface.getNetworkId().equals(tokenid)) {
                return noteInterface;
            }
        }
        return null;
    }

    public void addToken(NoteInterface noteInterface) {

        if (noteInterface != null) {
            if (getErgoToken(noteInterface.getNetworkId()) == null) {
                m_noteInterfaceList.add(noteInterface);
                noteInterface.addUpdateListener((obs, old, newVal) -> {
                    getLastUpdated().set(LocalDateTime.now());
                });
            }
        }

    }

    public void removeToken(String networkId) {
        if (networkId != null) {
            NoteInterface noteInterface = getErgoToken(networkId);
            if (noteInterface != null) {
                noteInterface.removeUpdateListener();
                m_noteInterfaceList.remove(noteInterface);

            }
        }
    }

    private void readFile(SecretKey appKey, Path filePath) {
        try {
            Files.writeString(logFile.toPath(), "\nReading file:" + filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(filePath);

            byte[] iv = new byte[]{
                fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
            };

            ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

            try {
                JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, encryptedData)));
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject());
                }
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                Alert a = new Alert(AlertType.NONE, "Decryption error:\n\n" + e.toString(), ButtonType.CLOSE);
                Platform.runLater(() -> a.show());
            }

        } catch (IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

    private void openJson(JsonObject json) {
        m_noteInterfaceList.clear();

        try {
            Files.writeString(logFile.toPath(), "\nopening json:\n" + json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray()) {

            JsonArray dataArray = dataElement.getAsJsonArray();

            //  if (m_ergoTokens.getNetworkType().toString().equals(networkType)) {
            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement tokenIdElement = objJson.get("networkId");

                    if (nameElement != null && nameElement.isJsonPrimitive() && tokenIdElement != null && tokenIdElement.isJsonPrimitive()) {
                        addToken(new ErgoNetworkToken(nameElement.getAsString(), tokenIdElement.getAsString(), objJson, getParentInterface()));
                    }

                }
            }
            //   }
        }
    }

}
