package com.netnotes;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;

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

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworksData implements InstallerInterface {

    public final static long WATCH_INTERVAL = 50;
    public final String INPUT_EXT = ".in";
    public final String OUT_EXT = ".out";

    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
        ErgoNetwork.NETWORK_ID,
        KucoinExchange.NETWORK_ID
    };

    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private File m_networksFile;
    private String m_selectedId;
    private VBox m_networksBox;

    private SimpleObjectProperty<SecretKey> m_secretKey = new SimpleObjectProperty<SecretKey>(null);

    private double m_leftColumnWidth = 175;

    private VBox m_installedVBox = null;
    private VBox m_notInstalledVBox = null;
    private ArrayList<InstallableIcon> m_installables = new ArrayList<>();

    private Stage m_addNetworkStage = null;

    private InstallableIcon m_focusedInstallable = null;

    private Rectangle m_rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    private HostServices m_hostServices;
    private File m_appDir;

    private File m_notesDir;
    private File m_outDir;
    private SimpleObjectProperty<com.grack.nanojson.JsonObject> m_cmdSwitch = new SimpleObjectProperty<com.grack.nanojson.JsonObject>(null);

    private NoteWatcher m_noteWatcher = null;

    private File logFile = new File("networkData-log.txt");
    private SimpleStringProperty m_stageIconStyle = new SimpleStringProperty(IconStyle.ICON);

    private double m_stageWidth = 700;
    private double m_stageHeight = 500;
    private double m_stagePrevWidth = 310;
    private double m_stagePrevHeight = 500;
    private boolean m_stageMaximized = false;
    private AppData m_appData;

    public NetworksData(AppData appData, SecretKey secretKey, HostServices hostServices, File networksFile, boolean isFile) {
        m_appData = appData;
        m_secretKey.set(secretKey);
        m_networksFile = networksFile;
        m_networksBox = new VBox();
        m_hostServices = hostServices;
        m_appDir = new File(System.getProperty("user.dir"));

        if (isFile) {
            readFile(appKeyProperty().get(), networksFile.toPath());
        }

        m_notesDir = new File(m_appDir.getAbsolutePath() + "/notes");
        m_outDir = new File(m_appDir.getAbsolutePath() + "/out");
        if (!m_notesDir.isDirectory()) {
            try {
                Files.createDirectory(m_notesDir.toPath());
            } catch (IOException e) {

            }
        }
        if (!m_outDir.isDirectory()) {
            try {
                Files.createDirectory(m_outDir.toPath());
            } catch (IOException e) {

            }
        }

        try {
            m_noteWatcher = new NoteWatcher(m_notesDir, new NoteListener() {
                public void onNoteChange(String fileString) {
                    checkFile(new File(fileString));
                }
            });
        } catch (IOException e) {

        }

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

    public AppData getAppData() {
        return m_appData;
    }

    private void openJson(JsonObject networksObject) {
        if (networksObject != null) {

            JsonElement jsonArrayElement = networksObject == null ? null : networksObject.get("networks");
            JsonElement stageElement = networksObject.get("stage");
            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");

                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();

                    switch (networkId) {
                        case "ERGO_NETWORK":
                            addNoteInterface(new ErgoNetwork(jsonObject, this), false);
                            break;

                        case "KUCOIN_EXCHANGE":
                            addNoteInterface(new KucoinExchange(jsonObject, this), false);
                            break;

                    }

                }

            }
            if (stageElement != null && stageElement.isJsonObject()) {

                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement stageWidthElement = stageObject.get("width");
                JsonElement stageHeightElement = stageObject.get("height");
                JsonElement stagePrevWidthElement = stageObject.get("prevWidth");
                JsonElement stagePrevHeightElement = stageObject.get("prevHeight");

                JsonElement iconStyleElement = stageObject.get("iconStyle");
                JsonElement stageMaximizedElement = stageObject.get("maximized");

                boolean maximized = stageMaximizedElement == null ? false : stageMaximizedElement.getAsBoolean();
                String iconStyle = iconStyleElement != null ? iconStyleElement.getAsString() : IconStyle.ICON;

                m_stageIconStyle.set(iconStyle);
                setStagePrevWidth(Network.DEFAULT_STAGE_WIDTH);
                setStagePrevHeight(Network.DEFAULT_STAGE_HEIGHT);
                if (!maximized) {

                    setStageWidth(stageWidthElement.getAsDouble());
                    setStageHeight(stageHeightElement.getAsDouble());
                } else {
                    double prevWidth = stagePrevWidthElement != null && stagePrevWidthElement.isJsonPrimitive() ? stagePrevWidthElement.getAsDouble() : Network.DEFAULT_STAGE_WIDTH;
                    double prevHeight = stagePrevHeightElement != null && stagePrevHeightElement.isJsonPrimitive() ? stagePrevHeightElement.getAsDouble() : Network.DEFAULT_STAGE_HEIGHT;
                    setStageWidth(prevWidth);
                    setStageHeight(prevHeight);
                    setStagePrevWidth(prevWidth);
                    setStagePrevHeight(prevHeight);
                }
                setStageMaximized(maximized);
            }
            updateNetworksGrid();

        }
    }

    public double getStageWidth() {
        return m_stageWidth;
    }

    public void setStageWidth(double width) {
        m_stageWidth = width;

    }

    public void setStageHeight(double height) {
        m_stageHeight = height;
    }

    public double getStageHeight() {
        return m_stageHeight;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_stageIconStyle;
    }

    public boolean getStageMaximized() {
        return m_stageMaximized;
    }

    public void setStageMaximized(boolean value) {
        m_stageMaximized = value;
    }

    public double getStagePrevWidth() {
        return m_stagePrevWidth;
    }

    public void setStagePrevWidth(double width) {
        m_stagePrevWidth = width;

    }

    public void setStagePrevHeight(double height) {
        m_stagePrevHeight = height;
    }

    public double getStagePrevHeight() {
        return m_stagePrevHeight;
    }

    public JsonObject getStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("maximized", getStageMaximized());
        json.addProperty("width", getStageWidth());
        json.addProperty("height", getStageHeight());
        json.addProperty("prevWidth", getStagePrevWidth());
        json.addProperty("prevHeight", getStagePrevHeight());
        json.addProperty("iconStyle", m_stageIconStyle.get());
        return json;
    }

    public SimpleObjectProperty<com.grack.nanojson.JsonObject> cmdSwitchProperty() {
        return m_cmdSwitch;
    }

    public File getAppDir() {
        return m_appDir;
    }

    public HostServices getHostServices() {
        return m_hostServices;
    }

    public Rectangle getMaximumWindowBounds() {
        return m_rect;
    }

    public void clear() {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            m_noteInterfaceList.remove(noteInterface);
        }

        try {
            save();
        } catch (Exception e) {

        }
        updateNetworksGrid();

    }

    public boolean addNoteInterface(NoteInterface noteInterface) {
        return addNoteInterface(noteInterface, true);
    }

    public boolean addNoteInterface(NoteInterface noteInterface, boolean update) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNoteInterface(networkId) == null) {
            m_noteInterfaceList.add(noteInterface);
            noteInterface.addUpdateListener((obs, oldValue, newValue) -> save());

            if (update) {
                updateNetworksGrid();
            }
            return true;
        }
        return false;
    }

    public VBox getNetworksBox() {

        updateNetworksGrid();
        m_stageIconStyle.addListener((obs, oldVal, newVal) -> {
            updateNetworksGrid();
            save();
        });

        return m_networksBox;
    }

    private SimpleObjectProperty<LocalDateTime> m_shutdownNow = new SimpleObjectProperty<>(null);

    public SimpleObjectProperty<LocalDateTime> shutdownNowProperty() {
        return m_shutdownNow;
    }

    public void shutdown() {
        m_shutdownNow.set(LocalDateTime.now());

        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            noteInterface.shutdown();

            m_noteInterfaceList.remove(i);
        }

        if (m_noteWatcher != null) {
            m_noteWatcher.shutdown();
        }
        closeNetworksStage();
    }

    public void show() {
        com.grack.nanojson.JsonObject showJson = new com.grack.nanojson.JsonObject();

        showJson.put("type", "CMD");
        showJson.put("cmd", App.CMD_SHOW_APPSTAGE);
        showJson.put("timeStamp", System.currentTimeMillis());

        m_cmdSwitch.set(showJson);
    }

    public void showManageNetworkStage() {

        if (m_addNetworkStage == null) {
            updateInstallables();
            m_installedVBox = new VBox();
            m_installedVBox.prefWidth(m_leftColumnWidth);
            m_notInstalledVBox = new VBox();
            VBox.setVgrow(m_notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);

            String topTitle = "NetNotes: Select networks";
            m_addNetworkStage = new Stage();
            m_addNetworkStage.setTitle(topTitle);
            m_addNetworkStage.getIcons().add(App.logo);
            m_addNetworkStage.setResizable(false);
            m_addNetworkStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            closeBtn.setOnAction(e -> {
                closeNetworksStage();
            });

            HBox titleBox = App.createTopBar(App.icon, topTitle, closeBtn, m_addNetworkStage);

            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            BufferedButton checkBtn = new BufferedButton("/assets/checkmark-25.png", 15);

            HBox menuBar = new HBox(menuSpacer, checkBtn);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 0));

            VBox headerBox = new VBox(menuBar);
            headerBox.setPadding(new Insets(0, 5, 2, 5));
            headerBox.setId("bodyBox");

            Button installBtn = new Button("Install");
            installBtn.setFont(App.txtFont);

            Button removeBtn = new Button("Remove");
            removeBtn.setPrefWidth(m_leftColumnWidth);
            removeBtn.setId("menuBarBtn");

            Region vSpacerOne = new Region();
            VBox.setVgrow(vSpacerOne, Priority.ALWAYS);
            HBox.setHgrow(m_installedVBox, Priority.ALWAYS);

            VBox installedVBox = new VBox(m_installedVBox, vSpacerOne);

            installedVBox.setId("bodyBox");
            VBox.setVgrow(installedVBox, Priority.ALWAYS);

            Region leftSpacer = new Region();
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);

            Region topSpacer = new Region();
            VBox.setVgrow(topSpacer, Priority.ALWAYS);

            HBox addBox = new HBox(leftSpacer, installBtn);
            addBox.setPadding(new Insets(15));
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);

            VBox notInstalledVBox = new VBox(m_notInstalledVBox, topSpacer);
            notInstalledVBox.setId("bodyRight");

            VBox.setVgrow(notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(notInstalledVBox, Priority.ALWAYS);

            VBox.setVgrow(m_installedVBox, Priority.ALWAYS);
            HBox rmvBtnBox = new HBox(removeBtn);
            rmvBtnBox.setPadding(new Insets(5, 5, 5, 5));
            VBox leftSide = new VBox(installedVBox, rmvBtnBox);

            leftSide.setPadding(new Insets(5));
            leftSide.prefWidth(m_leftColumnWidth);
            VBox.setVgrow(leftSide, Priority.ALWAYS);
            VBox rightSide = new VBox(notInstalledVBox, addBox);
            rightSide.setPadding(new Insets(5));
            HBox.setHgrow(rightSide, Priority.ALWAYS);

            HBox columnsHBox = new HBox(leftSide, rightSide);
            VBox.setVgrow(columnsHBox, Priority.ALWAYS);

            columnsHBox.setPadding(new Insets(10, 10, 10, 10));
            VBox layoutVBox = new VBox(titleBox, headerBox, columnsHBox);

            Scene addNetworkScene = new Scene(layoutVBox, 700, 400);
            addNetworkScene.getStylesheets().add("/css/startWindow.css");
            m_addNetworkStage.setScene(addNetworkScene);

            addNetworkScene.focusOwnerProperty().addListener((e) -> {
                if (addNetworkScene.focusOwnerProperty().get() instanceof InstallableIcon) {
                    InstallableIcon installable = (InstallableIcon) addNetworkScene.focusOwnerProperty().get();

                    m_focusedInstallable = installable;
                } else {
                    if (addNetworkScene.focusOwnerProperty().get() instanceof Button) {
                        Button focusedButton = (Button) addNetworkScene.focusOwnerProperty().get();
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

            m_addNetworkStage.show();
            m_addNetworkStage.toFront();
            updateAvailableLists();
        } else {
            m_addNetworkStage.show();
            m_addNetworkStage.toFront();
        }
    }

    public void closeNetworksStage() {
        if (m_addNetworkStage != null) {
            m_addNetworkStage.close();
        }
        m_addNetworkStage = null;
        m_notInstalledVBox = null;
        m_installedVBox = null;
        m_installables = null;
        m_focusedInstallable = null;
    }

    public void updateAvailableLists() {
        if (m_installables != null && m_installedVBox != null && m_notInstalledVBox != null) {
            m_installedVBox.getChildren().clear();

            m_notInstalledVBox.getChildren().clear();

            //  double listImageWidth = 30;
            //    double listImagePadding = 5;
            ItemIterator grid = new ItemIterator();

            double imageWidth = new IconButton().getImageWidth();
            double cellPadding = new IconButton().getPadding().getLeft();
            double cellWidth = imageWidth + (cellPadding * 2);
            double numCells = INTALLABLE_NETWORK_IDS.length - m_noteInterfaceList.size();
            double boxWidth = m_addNetworkStage.getWidth() - 150;

            int floor = (int) Math.floor(boxWidth / (cellWidth + 20));
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

    public void updateInstallables() {
        m_installables = new ArrayList<>();
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            NoteInterface noteInterface = getNoteInterface(networkId);
            boolean installed = !(noteInterface == null);
            InstallableIcon installableIcon = new InstallableIcon(this, networkId, installed);

            m_installables.add(installableIcon);
        }
    }

    public void updateNetworksGrid() {
        m_networksBox.getChildren().clear();

        int numCells = m_noteInterfaceList.size();
        double width = m_stageWidth - 80;
        String currentIconStyle = m_stageIconStyle.get();

        if (numCells != 0) {

            m_networksBox.setAlignment(Pos.TOP_LEFT);
            if (currentIconStyle.equals(IconStyle.ROW)) {
                for (int i = 0; i < numCells; i++) {
                    NoteInterface network = m_noteInterfaceList.get(i);
                    IconButton iconButton = network.getButton(currentIconStyle);
                    iconButton.setPrefWidth(width);
                    m_networksBox.getChildren().add(iconButton);
                }
            } else {

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
                    m_networksBox.getChildren().add(rowsBoxes[i]);
                }

                ItemIterator grid = new ItemIterator();

                for (NoteInterface noteInterface : m_noteInterfaceList) {

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
    }

    public void installNetwork(String networkId) {

        switch (networkId) {

            case "ERGO_NETWORK":
                addNoteInterface(new ErgoNetwork(this));
                break;
            case "KUCOIN_EXCHANGE":
                addNoteInterface(new KucoinExchange(this));
                break;

        }
        m_installedVBox.getChildren().clear();
        m_notInstalledVBox.getChildren().clear();
        updateInstallables();
        updateAvailableLists();
        save();
    }

    public void removeNetwork(String networkId) {
        removeNoteInterface(networkId);

        m_installedVBox.getChildren().clear();
        m_notInstalledVBox.getChildren().clear();
        updateInstallables();
        updateAvailableLists();

        save();
    }

    public void setSelected(String networkId) {
        if (m_selectedId != null) {
            NoteInterface prevInterface = getNoteInterface(m_selectedId);
            if (prevInterface != null) {
                prevInterface.getButton().setCurrent(false);
            }
        }
        m_selectedId = networkId;
        if (networkId != null) {
            NoteInterface currentInterface = getNoteInterface(networkId);
            if (currentInterface != null) {
                currentInterface.getButton().setCurrent(true);
            }
        }
    }

    public String getSelected() {
        return m_selectedId;
    }

    public boolean removeNoteInterface(String networkId) {
        return removeNoteInterface(networkId, true);
    }

    public boolean removeNoteInterface(String networkId, boolean update) {
        boolean success = false;
        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            if (networkId.equals(noteInterface.getNetworkId())) {
                m_noteInterfaceList.remove(i);
                noteInterface.remove();

                success = true;
                break;
            }
        }
        if (success && update) {
            updateNetworksGrid();
        }

        return success;
    }

    public void broadcastNote(JsonObject note) {

        m_noteInterfaceList.forEach(noteInterface -> {

            noteInterface.sendNote(note, null, null);

        });

    }

    public void broadcastNoteToNetworkIds(JsonObject note, ArrayList<String> networkIds) {

        networkIds.forEach(id -> {
            m_noteInterfaceList.forEach(noteInterface -> {

                int index = id.indexOf(":");
                String networkId = index == -1 ? id : id.substring(0, index);
                if (noteInterface.getNetworkId().equals(networkId)) {

                    note.addProperty("uuid", id);
                    noteInterface.sendNote(note, null, null);
                }
            });
        });
    }

    public NoteInterface getNoteInterface(String networkId) {
        if (networkId != null) {
            for (int i = 0; i < m_noteInterfaceList.size(); i++) {
                NoteInterface noteInterface = m_noteInterfaceList.get(i);

                if (noteInterface.getNetworkId().equals(networkId)) {
                    return noteInterface;
                }
            }
        }
        return null;
    }

    public void sendNoteToNetworkId(JsonObject note, String networkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        m_noteInterfaceList.forEach(noteInterface -> {
            if (noteInterface.getNetworkId().equals(networkId)) {

                noteInterface.sendNote(note, onSucceeded, onFailed);
            }
        });
    }

    public SimpleObjectProperty<SecretKey> appKeyProperty() {
        return m_secretKey;
    }

    public void setAppKey(SecretKey secretKey) {
        m_secretKey.set(secretKey);
    }

    public void save() {
        JsonObject fileObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        fileObject.add("networks", jsonArray);
        fileObject.add("stage", getStageJson());

        String jsonString = fileObject.toString();

        try {
            Files.writeString(logFile.toPath(), jsonString);
        } catch (IOException e1) {

        }

        try {

            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iV = new byte[12];
            secureRandom.nextBytes(iV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, appKeyProperty().get(), parameterSpec);

            byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

            try {

                if (m_networksFile.isFile()) {
                    Files.delete(m_networksFile.toPath());
                }

                FileOutputStream outputStream = new FileOutputStream(m_networksFile);
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

    public String getAckString(String id, long timeStamp) {
        return "{\"id\": \"" + id + "\", \"type\": \"ack\", \"timeStamp\": " + timeStamp + "}";
    }

    private void noteIn(String id, String body, File inFile) {
        switch (body) {
            case App.CMD_SHOW_APPSTAGE:
                show();

                break;

            default:
                break;
        }
    }

    private void sendAck(String senderId) {

        File ackFile = new File(m_outDir.getAbsolutePath() + "\\" + senderId + OUT_EXT);
        try {
            Files.writeString(ackFile.toPath(), getAckString(senderId, System.currentTimeMillis()));
        } catch (IOException e) {

        }

    }

    private void checkFile(File file) {

        String fileName = file.getName();
        int fileNameLength = fileName.length();
        int extIndex = fileNameLength - INPUT_EXT.length();

        if (fileName.substring(extIndex, fileNameLength).equals(INPUT_EXT)) {
            int hashIndex = fileName.indexOf("#");
            String senderId = hashIndex == -1 ? fileName.substring(0, extIndex) : fileName.substring(0, hashIndex);
            String bodyString = hashIndex == -1 ? null : fileName.substring(hashIndex + 1, extIndex);

            if (bodyString != null) {
                sendAck(senderId);
                //  Platform.runLater(() -> );
                noteIn(senderId, bodyString, file);
            }
        }

    }

}
