package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
import com.netnotes.IconButton.IconStyle;
import com.satergo.extra.AESEncryption;

import javafx.beans.property.SimpleDoubleProperty;

import javafx.beans.property.SimpleStringProperty;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoNetworkData implements InstallerInterface {

    private File logFile = new File("ErgoNetworkData-log.txt");

    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
        ErgoTokens.NETWORK_ID,
        ErgoExplorer.NETWORK_ID,
        ErgoNodes.NETWORK_ID,
        ErgoWallet.NETWORK_ID
    };

    private Stage m_manageStage = null;
    private double m_stageWidth = 700;
    private double m_stageHeight = 500;

    private InstallableIcon m_focusedInstallable = null;
    private ArrayList<NoteInterface> m_networkList = new ArrayList<>();
    private double m_leftColumnWidth = 200;
    private ErgoNetwork m_ergoNetwork;
    private File m_dataFile;

    private SimpleStringProperty m_iconStyle;
    private SimpleDoubleProperty m_gridWidth;

    private VBox m_installedVBox = new VBox();
    private VBox m_notInstalledVBox = new VBox();

    private final static long EXECUTION_TIME = 500;

    private ScheduledFuture<?> m_lastExecution = null;

    public ErgoNetworkData(String iconStyle, double gridWidth, ErgoNetwork ergoNetwork) {
        m_ergoNetwork = ergoNetwork;
        m_iconStyle = new SimpleStringProperty(iconStyle);
        m_gridWidth = new SimpleDoubleProperty(gridWidth);

        File appDir = ErgoNetwork.ERGO_NETWORK_DIR;

        if (!appDir.isDirectory()) {
            try {
                Files.createDirectory(appDir.toPath());
            } catch (IOException e) {

            }
        }

        m_dataFile = new File(appDir.getAbsolutePath() + "/" + "ergoNetworkData.dat");
        boolean isFile = m_dataFile.isFile();

        if (isFile) {
            readFile(m_ergoNetwork.getNetworksData().appKeyProperty().get(), m_dataFile.toPath());
        }

        m_iconStyle.addListener((obs, oldVal, newVal) -> updateGrid());
        m_gridWidth.addListener((obs, oldVal, newVal) -> updateGrid());
    }

    public boolean isEmpty() {
        return m_networkList.size() == 0;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_iconStyle;
    }

    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidth;
    }

    private void readFile(SecretKey appKey, Path filePath) {

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

            }

        } catch (IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\n" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

    private void openJson(JsonObject json) {
        if (json != null) {

            JsonElement jsonArrayElement = json.get("networks");
            JsonElement stageElement = json.get("stage");

            if (stageElement != null && stageElement.isJsonObject()) {
                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement widthElement = stageObject.get("width");
                JsonElement heightElement = stageObject.get("height");

                m_stageWidth = widthElement != null && widthElement.isJsonPrimitive() ? widthElement.getAsDouble() : m_stageWidth;
                m_stageHeight = heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : m_stageHeight;

            }

            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");
                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();
                    NoteInterface network = null;
                    switch (networkId) {
                        case ErgoWallet.NETWORK_ID:
                            network = new ErgoWallet(this, jsonObject, m_ergoNetwork);
                            break;
                        case ErgoTokens.NETWORK_ID:
                            network = new ErgoTokens(jsonObject, m_ergoNetwork);
                            break;
                        case ErgoExplorer.NETWORK_ID:
                            network = new ErgoExplorer(jsonObject, m_ergoNetwork);
                            break;
                        case ErgoNodes.NETWORK_ID:
                            network = new ErgoNodes(jsonObject, m_ergoNetwork);
                            break;
                    }

                    if (network != null) {
                        addNoteInterface(network);
                    }
                }

            }

        }
    }
    private VBox m_gridBox = new VBox();

    public VBox getGridBox() {
        updateGrid();
        return m_gridBox;
    }

    private void updateGrid() {
        int numCells = m_networkList.size();
        String currentIconStyle = m_iconStyle.get();
        m_gridBox.getChildren().clear();

        if (currentIconStyle.equals(IconStyle.ROW)) {
            for (int i = 0; i < numCells; i++) {
                NoteInterface network = m_networkList.get(i);
                IconButton iconButton = network.getButton(currentIconStyle);
                iconButton.prefWidthProperty().bind(m_gridWidth);
                m_gridBox.getChildren().add(iconButton);
            }
        } else {

            double width = m_gridWidth.get();
            double imageWidth = 75;
            double cellPadding = 15;
            double cellWidth = imageWidth + (cellPadding * 2);

            int floor = (int) Math.floor(width / cellWidth);
            int numCol = floor == 0 ? 1 : floor;
            // currentNumCols.set(numCol);
            int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            HBox[] rowsBoxes = new HBox[numRows];
            for (int i = 0; i < numRows; i++) {
                rowsBoxes[i] = new HBox();
                m_gridBox.getChildren().add(rowsBoxes[i]);
            }

            ItemIterator grid = new ItemIterator();

            for (NoteInterface noteInterface : m_networkList) {

                HBox rowBox = rowsBoxes[grid.getJ()];
                rowBox.getChildren().add(noteInterface.getButton(currentIconStyle));

                if (grid.getI() < numCol) {
                    grid.setI(grid.getI() + 1);
                } else {
                    grid.setI(0);
                    grid.setJ(grid.getJ() + 1);
                }
            }

        }

    }

    private ArrayList<InstallableIcon> updateInstallables() {
        ArrayList<InstallableIcon> installables = new ArrayList<>();
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            NoteInterface network = getNetwork(networkId);
            boolean installed = !(network == null);
            InstallableIcon installableIcon = new InstallableIcon(this, networkId, installed);

            installables.add(installableIcon);
        }
        return installables;
    }

    public void showwManageStage() {
        if (m_manageStage == null) {

            ArrayList<InstallableIcon> installables = updateInstallables();

            double stageWidth = m_stageWidth;
            double stageHeight = m_stageHeight;

            m_manageStage = new Stage();
            m_manageStage.setTitle("Ergo Network - Manage");
            m_manageStage.getIcons().add(ErgoNetwork.getSmallAppIcon());
            m_manageStage.setResizable(false);
            m_manageStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            Button maximizeBtn = new Button();

            HBox titleBar = App.createTopBar(ErgoNetwork.getSmallAppIcon(), maximizeBtn, closeBtn, m_manageStage);

            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            BufferedButton checkBtn = new BufferedButton("/assets/checkmark-25.png", 15);

            HBox menuBar = new HBox(menuSpacer, checkBtn);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 0));

            VBox headerBox = new VBox(menuBar);
            headerBox.setId("bodyBox");

            Button installBtn = new Button("Install");
            installBtn.setFont(App.txtFont);

            Button installAllBtn = new Button("All");
            installAllBtn.setFont(App.txtFont);

            Button removeBtn = new Button("Remove");
            removeBtn.setPrefWidth(m_leftColumnWidth);
            removeBtn.setId("menuBarBtn");

            Button removeAllBtn = new Button("All");
            removeAllBtn.setId("menuBarBtn");

            Region vSpacerOne = new Region();
            VBox.setVgrow(vSpacerOne, Priority.ALWAYS);
            HBox.setHgrow(m_installedVBox, Priority.ALWAYS);

            VBox installedPaddedVBox = new VBox(m_installedVBox, vSpacerOne);

            installedPaddedVBox.setId("bodyBox");
            VBox.setVgrow(installedPaddedVBox, Priority.ALWAYS);

            Region leftSpacer = new Region();
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);

            Region topSpacer = new Region();
            VBox.setVgrow(topSpacer, Priority.ALWAYS);

            Region installAllSpacer = new Region();
            installAllSpacer.setMinWidth(5);

            HBox addBox = new HBox(leftSpacer, installBtn, installAllSpacer, installAllBtn);
            addBox.setPadding(new Insets(15));
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);

            VBox.setVgrow(m_notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);

            VBox.setVgrow(m_installedVBox, Priority.ALWAYS);

            HBox rmvBox = new HBox(removeBtn, removeAllBtn);

            VBox leftSide = new VBox(installedPaddedVBox, rmvBox);
            leftSide.setPadding(new Insets(5));
            leftSide.prefWidth(m_leftColumnWidth);
            VBox.setVgrow(leftSide, Priority.ALWAYS);

            VBox rightSide = new VBox(m_notInstalledVBox, addBox);
            HBox.setHgrow(rightSide, Priority.ALWAYS);

            HBox columnsHBox = new HBox(leftSide, rightSide);
            VBox.setVgrow(columnsHBox, Priority.ALWAYS);
            columnsHBox.setId("bodyBox");
            columnsHBox.setPadding(new Insets(10, 10, 10, 10));

            VBox layoutBox = new VBox(titleBar, headerBox, columnsHBox);

            layoutBox.setPadding(new Insets(0, 2, 2, 2));
            Scene scene = new Scene(layoutBox, stageWidth, stageHeight);
            scene.getStylesheets().add("/css/startWindow.css");
            m_manageStage.setScene(scene);

            closeBtn.setOnAction(e -> {
                m_manageStage.close();
                m_focusedInstallable = null;
                m_manageStage = null;
            });

            scene.focusOwnerProperty().addListener((e) -> {
                if (scene.focusOwnerProperty().get() instanceof InstallableIcon) {
                    InstallableIcon installable = (InstallableIcon) scene.focusOwnerProperty().get();

                    m_focusedInstallable = installable;
                } else {
                    if (scene.focusOwnerProperty().get() instanceof Button) {
                        Button focusedButton = (Button) scene.focusOwnerProperty().get();
                        String buttonString = focusedButton.getText();
                        if (!(buttonString.equals(installBtn.getText()) || buttonString.equals(removeBtn.getText()))) {

                            m_focusedInstallable = null;

                        }
                    }
                }

            });

            checkBtn.setOnAction(e -> {
                closeBtn.fire();
            });

            installBtn.setOnAction(e -> {

                if (m_focusedInstallable != null && (!m_focusedInstallable.getInstalled())) {
                    installNetwork(m_focusedInstallable.getNetworkId());
                }
                m_focusedInstallable = null;
            });

            removeBtn.setOnAction(e -> {
                if (m_focusedInstallable != null && (m_focusedInstallable.getInstalled())) {
                    removeNetwork(m_focusedInstallable.getNetworkId());
                }
                m_focusedInstallable = null;
            });

            removeAllBtn.setOnAction(e -> {
                removeAll();
            });

            installAllBtn.setOnAction(e -> {
                addAll();
            });

            Runnable runSave = () -> {
                save();
            };

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            m_manageStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                m_stageWidth = newVal.doubleValue();
                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }
                m_lastExecution = executor.schedule(runSave, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            m_manageStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                m_stageHeight = newVal.doubleValue();
                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }
                m_lastExecution = executor.schedule(runSave, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            m_manageStage.show();

            Rectangle maxRect = m_ergoNetwork.getNetworksData().getMaximumWindowBounds();

            ResizeHelper.addResizeListener(m_manageStage, 400, 200, maxRect.getWidth(), maxRect.getHeight());

            updateAvailableLists(installables);
        } else {
            m_manageStage.show();
        }

    }

    private void updateAvailableLists(ArrayList<InstallableIcon> m_installables) {
        if (m_installables != null && m_installedVBox != null && m_notInstalledVBox != null) {
            m_installedVBox.getChildren().clear();

            m_notInstalledVBox.getChildren().clear();

            //  double listImageWidth = 30;
            //    double listImagePadding = 5;
            ItemIterator grid = new ItemIterator();

            double imageWidth = new IconButton().getImageWidth();
            double cellPadding = new IconButton().getPadding().getLeft();
            double cellWidth = imageWidth + cellPadding;
            double numCells = INTALLABLE_NETWORK_IDS.length - m_networkList.size();
            double boxWidth = m_stageWidth - (m_leftColumnWidth);

            int floor = (int) Math.floor(boxWidth / (cellWidth));
            int numCol = floor == 0 ? 1 : floor;
            int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            HBox[] rowsBoxes = new HBox[numRows];
            for (int i = 0; i < numRows; i++) {
                rowsBoxes[i] = new HBox();
                HBox.setHgrow(rowsBoxes[i], Priority.ALWAYS);

                m_notInstalledVBox.getChildren().add(rowsBoxes[i]);
            }

            for (InstallableIcon installable : m_installables) {

                if (installable.getInstalled()) {
                    installable.setPrefWidth(m_leftColumnWidth);
                    m_installedVBox.getChildren().add(installable);

                } else {

                    HBox rowBox = rowsBoxes[grid.getJ()];

                    if (grid.getI() < numCol) {
                        grid.setI(grid.getI() + 1);
                    } else {
                        grid.setI(0);
                        grid.setJ(grid.getJ() + 1);
                    }
                    //   installable.prefWidthProperty().unbind();
                    installable.setPrefWidth(IconButton.NORMAL_IMAGE_WIDTH);
                    rowBox.getChildren().add(installable);
                }
            }
        }
    }

    public void installNetwork(String networkID) {
        installNetwork(networkID, true);
    }

    public void installNetwork(String networkId, boolean update) {
        NoteInterface noteInterface = null;
        switch (networkId) {

            case ErgoTokens.NETWORK_ID:
                noteInterface = new ErgoTokens(m_ergoNetwork);
                break;
            case ErgoWallet.NETWORK_ID:
                noteInterface = new ErgoWallet(this, m_ergoNetwork);
                break;
            case ErgoExplorer.NETWORK_ID:
                noteInterface = new ErgoExplorer(m_ergoNetwork);
                break;
            case ErgoNodes.NETWORK_ID:
                noteInterface = new ErgoNodes(m_ergoNetwork);
                break;
        }
        if (noteInterface != null) {
            addNoteInterface(noteInterface);
            updateAvailableLists(updateInstallables());
            save();
            updateGrid();
        }

    }

    public boolean addNoteInterface(NoteInterface noteInterface) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNetwork(networkId) == null) {
            m_networkList.add(noteInterface);
            noteInterface.addUpdateListener((obs, oldValue, newValue) -> save());

            return true;
        }
        return false;
    }

    public void removeNetwork(String networkId) {
        removeNetwork(networkId, true);
    }

    public void removeNetwork(String networkId, boolean save) {

        removeNoteInterface(networkId);

        updateAvailableLists(updateInstallables());
        updateGrid();
        if (save) {
            save();
        }

    }

    public boolean removeNoteInterface(String networkId) {
        return removeNoteInterface(networkId, true);
    }

    public boolean removeNoteInterface(String networkId, boolean update) {
        boolean success = false;
        for (int i = 0; i < m_networkList.size(); i++) {
            NoteInterface noteInterface = m_networkList.get(i);
            if (networkId.equals(noteInterface.getNetworkId())) {
                m_networkList.remove(noteInterface);
                noteInterface.remove();

                success = true;
                break;
            }
        }

        return success;
    }

    public void shutdown() {

        for (int i = 0; i < m_networkList.size(); i++) {
            NoteInterface noteInterface = m_networkList.get(i);
            noteInterface.shutdown();
            m_networkList.remove(noteInterface);

        }
    }

    public NoteInterface getNetwork(String networkId) {
        if (networkId != null) {
            for (int i = 0; i < m_networkList.size(); i++) {
                NoteInterface network = m_networkList.get(i);

                if (network.getNetworkId().equals(networkId)) {
                    return network;
                }
            }
        }
        return null;
    }

    public void addAll() {
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            if (getNetwork(networkId) == null) {
                installNetwork(networkId, false);
            }
        }
        updateAvailableLists(updateInstallables());
        updateGrid();
        save();
    }

    public void removeAll() {

        while (m_networkList.size() > 0) {
            NoteInterface noteInterface = m_networkList.get(0);
            m_networkList.remove(noteInterface);
            noteInterface.remove();
        }
        updateAvailableLists(updateInstallables());
        updateGrid();
        save();
    }

    public JsonObject getStageObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("width", m_stageWidth);
        jsonObject.addProperty("height", m_stageHeight);
        jsonObject.addProperty("iconStyle", m_iconStyle.get());
        return jsonObject;
    }

    public void save() {
        JsonObject fileObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_networkList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        fileObject.add("networks", jsonArray);
        fileObject.add("stage", getStageObject());
        String jsonString = fileObject.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {

            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iV = new byte[12];
            secureRandom.nextBytes(iV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, m_ergoNetwork.getNetworksData().appKeyProperty().get(), parameterSpec);

            byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

            try {

                if (m_dataFile.isFile()) {
                    Files.delete(m_dataFile.toPath());
                }

                FileOutputStream outputStream = new FileOutputStream(m_dataFile);
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
