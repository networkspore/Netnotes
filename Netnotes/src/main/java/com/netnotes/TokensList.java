package com.netnotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.satergo.extra.AESEncryption;

import io.circe.Json;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

public class TokensList {

    private File logFile = new File("tokensList-log.txt");

    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private VBox m_buttonGrid = null;
    private ErgoTokens m_ergoTokens;

    private SimpleObjectProperty<LocalDateTime> m_localDatTime = new SimpleObjectProperty<>(null);

    public TokensList(SecretKey appKey, ErgoTokens ergoTokens) {
        m_ergoTokens = ergoTokens;

        if (m_ergoTokens.getDataFile() != null && m_ergoTokens.getDataFile().isFile()) {
            readFile(appKey);
        } else {
            setupTokens(appKey);
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

    private JsonArray getTokensJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            JsonObject json = noteInterface.getJsonObject();
            jsonArray.add(json);

        }

        return jsonArray;
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
            m_noteInterfaceList.add(noteInterface);
        }

    }

    public void createToken(String key, File imageFile) {
        ErgoToken ergoToken = null;
        switch (key) {
            case "aht":
                ergoToken = new ErgoToken("Ergo Auction House", "18c938e1924fc3eadc266e75ec02d81fe73b56e4e9f4e268dffffcb30387c42d", imageFile, m_ergoTokens);

                break;
            case "comet":
                ergoToken = new ErgoToken("Comet Memecoin", "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b", imageFile, m_ergoTokens);
                break;
            case "cypx":
                ergoToken = new ErgoToken("CyberVerse", "01dce8a5632d19799950ff90bca3b5d0ca3ebfa8aaafd06f0cc6dd1e97150e7f", imageFile, m_ergoTokens);
                break;
            case "egio":
                ergoToken = new ErgoToken("ErgoGames.io", "00b1e236b60b95c2c6f8007a9d89bc460fc9e78f98b09faec9449007b40bccf3", imageFile, m_ergoTokens);
                break;
            case "epos":
                ergoToken = new ErgoToken("ErgoPOS", "00bd762484086cf560d3127eb53f0769d76244d9737636b2699d55c56cd470bf", imageFile, m_ergoTokens);
                break;
            case "erdoge":
                ergoToken = new ErgoToken("Erdoge Memecoin", "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", imageFile, m_ergoTokens);
                break;
            case "ergold":
                ergoToken = new ErgoToken("Ergold", "e91cbc48016eb390f8f872aa2962772863e2e840708517d1ab85e57451f91bed", imageFile, m_ergoTokens);
                break;
            case "ergone":
                ergoToken = new ErgoToken("ErgOne NFT", "fcfca7654fb0da57ecf9a3f489bcbeb1d43b56dce7e73b352f7bc6f2561d2a1b", imageFile, m_ergoTokens);
                break;
            case "ergopad":
                ergoToken = new ErgoToken("ErgoPad", "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", imageFile, m_ergoTokens);
                break;
            case "ermoon":
                ergoToken = new ErgoToken("ErMoon", "9dbc8dd9d7ea75e38ef43cf3c0ffde2c55fd74d58ac7fc0489ec8ffee082991b", imageFile, m_ergoTokens);
                break;
            case "exle":
                ergoToken = new ErgoToken("Ergo-Lend", "007fd64d1ee54d78dd269c8930a38286caa28d3f29d27cadcb796418ab15c283", imageFile, m_ergoTokens);
                break;
            case "flux":
                ergoToken = new ErgoToken("Flux", "e8b20745ee9d18817305f32eb21015831a48f02d40980de6e849f886dca7f807", imageFile, m_ergoTokens);
                break;
            case "getblock":
                ergoToken = new ErgoToken("GetBlock.io", "4f5c05967a2a68d5fe0cdd7a688289f5b1a8aef7d24cab71c20ab8896068e0a8", imageFile, m_ergoTokens);
                break;
            case "kushti":
                ergoToken = new ErgoToken("Kushti Memecoin", "fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40", imageFile, m_ergoTokens);
                break;
            case "love":
                ergoToken = new ErgoToken("Love NFT", "3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386", imageFile, m_ergoTokens);
                break;
            case "lunadog":
                ergoToken = new ErgoToken("LunaDog NFT", "5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e", imageFile, m_ergoTokens);
                break;
            case "migoreng":
                ergoToken = new ErgoToken("Mi Goreng", "0779ec04f2fae64e87418a1ad917639d4668f78484f45df962b0dec14a2591d2", imageFile, m_ergoTokens);
                break;
            case "neta":
                ergoToken = new ErgoToken("anetaBTC", "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8", imageFile, m_ergoTokens);
                break;
            case "obsidian":
                ergoToken = new ErgoToken("Adventurers DAO", "2a51396e09ad9eca60b1bdafd365416beae155efce64fc3deb0d1b3580127b8f", imageFile, m_ergoTokens);
                break;
            case "paideia":
                ergoToken = new ErgoToken("Paideia DAO", "1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489", imageFile, m_ergoTokens);
                break;
            case "proxie":
                ergoToken = new ErgoToken("Proxies NFT", "01ddcc3d0205c2da8a067ffe047a2ccfc3e8241bc3fcc6f6ebc96b7f7363bb36", imageFile, m_ergoTokens);
                break;
            case "quacks":
                ergoToken = new ErgoToken("duckpools.io", "089990451bb430f05a85f4ef3bcb6ebf852b3d6ee68d86d78658b9ccef20074f", imageFile, m_ergoTokens);
                break;
            case "sigrsv":
                ergoToken = new ErgoToken("Sigma Reserve", "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", imageFile, m_ergoTokens);
                break;
            case "sigusd":
                ergoToken = new ErgoToken("Sigma USD", "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", imageFile, m_ergoTokens);
                break;
            case "spf":
                ergoToken = new ErgoToken("Spectrum Finanace", "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d", imageFile, m_ergoTokens);
                break;
            case "terahertz":
                ergoToken = new ErgoToken("Swamp Audio", "02f31739e2e4937bb9afb552943753d1e3e9cdd1a5e5661949cb0cef93f907ea", imageFile, m_ergoTokens);
                break;
            case "walrus":
                ergoToken = new ErgoToken("Walrus Dao", "59ee24951ce668f0ed32bdb2e2e5731b6c36128748a3b23c28407c5f8ccbf0f6", imageFile, m_ergoTokens);
                break;
            case "woodennickels":
                ergoToken = new ErgoToken("Wooden Nickles", "4c8ac00a28b198219042af9c03937eecb422b34490d55537366dc9245e85d4e1", imageFile, m_ergoTokens);
                break;
        }

        if (ergoToken != null) {
            addToken(ergoToken);
        }
    }

    public void setupTokens(SecretKey appKey) {

        File tokensDir = new File(m_ergoTokens.getAppDir().getAbsolutePath() + "\\tokens");

        if (!m_ergoTokens.getAppDir().isDirectory()) {

            try {
                Files.createDirectories(m_ergoTokens.getAppDir().toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }
        boolean createdtokensDirectory = false;
        if (!tokensDir.isDirectory()) {
            try {
                Files.createDirectories(tokensDir.toPath());
                createdtokensDirectory = true;

            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }
        }

        if (tokensDir.isDirectory() && createdtokensDirectory) {

            try {
                Files.writeString(logFile.toPath(), "\nUnzipping", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            //    ClassLoader classloader = Thread.currentThread().getContextClassLoader();

            // new InputStreamReader();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/currencyIcons.zip");

            ZipInputStream zipStream = null;
            try {
                zipStream = new ZipInputStream(is);

                String tokensPathString = tokensDir.getAbsolutePath() + "\\";
                if (zipStream != null) {
                    // Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {

                        String entryName = entry.getName();

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
                                createToken(fileName, entryFile);

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

        save(appKey);
    }

    private void readFile(SecretKey appKey) {

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(m_ergoTokens.getDataFile().toPath());

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
        JsonElement dataElement = json.get("data");

        if (dataElement != null && dataElement.isJsonArray()) {
            JsonArray dataArray = dataElement.getAsJsonArray();

            for (JsonElement objElement : dataArray) {
                if (objElement.isJsonObject()) {
                    JsonObject objJson = objElement.getAsJsonObject();
                    JsonElement nameElement = objJson.get("name");
                    JsonElement tokenIdElement = objJson.get("networkId");
                    if (nameElement != null && nameElement.isJsonPrimitive() && tokenIdElement != null && tokenIdElement.isJsonPrimitive()) {
                        addToken(new ErgoToken(nameElement.getAsString(), tokenIdElement.getAsString(), objJson, m_ergoTokens));
                    }

                }
            }
        }
    }

    private JsonObject getListObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("data", getTokensJsonArray());

        return jsonObject;
    }

    public void save(SecretKey appKey) {

        try {

            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iV = new byte[12];
            secureRandom.nextBytes(iV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, appKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(getListObject().toString().getBytes());

            try {

                if (m_ergoTokens.getDataFile().isFile()) {
                    Files.delete(m_ergoTokens.getDataFile().toPath());
                }

                FileOutputStream outputStream = new FileOutputStream(m_ergoTokens.getDataFile());
                FileChannel fc = outputStream.getChannel();

                ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

                fc.write(byteBuffer);

                int written = 0;
                int bufferLength = 1024 * 8;

                while (written < encryptedData.length) {

                    if (written + bufferLength > encryptedData.length) {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
                    } else {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
                    }

                    written += fc.write(byteBuffer);
                }

                outputStream.close();
            } catch (IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nIO exception:" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(), "\nKey error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

}
