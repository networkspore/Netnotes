package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.netnotes.Network.NetworkID;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
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

public class NetworksData {

    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
        NetworkID.ERGO_EXPLORER,
        NetworkID.ERGO_NETWORK,
        NetworkID.ERGO_WALLET,
        NetworkID.KUKOIN_EXCHANGE,
        NetworkID.NETWORK_TIMER,
        NetworkID.ERGO_TOKENS
    };

    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private File m_networksFile;
    private String m_selectedId;
    private VBox m_networksBox;
    private double m_width;

    //  private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private double m_leftColumnWidth = 175;

    private VBox m_installedVBox = null;
    private VBox m_notInstalledVBox = null;
    private ArrayList<InstallableIcon> m_installables = new ArrayList<>();
    //private double m_installedListWidth = 150;
    private Stage m_addNetworkStage = null;

    private InstallableIcon m_focusedInstallable = null;

    //  public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private File logFile = new File("networkData-log.txt");

    public NetworksData(JsonObject networksObject, File networksFile) {

        m_networksFile = networksFile;
        m_networksBox = new VBox();

        try {
            Files.writeString(logFile.toPath(), "networks data\n");
        } catch (IOException e) {

        }
        if (networksObject != null) {

            JsonElement jsonArrayElement = networksObject == null ? null : networksObject.get("networks");

            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");
                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();
                    //   NoteInterface noteInterface;
                    try {
                        Files.writeString(logFile.toPath(), "\ninstalling: " + networkId + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    switch (networkId) {
                        case "ERGO_NETWORK":
                            addNoteInterface(new ErgoNetwork(jsonObject, this));
                            break;
                        case "ERGO_WALLET":
                            addNoteInterface(new ErgoWallet(jsonObject, this));
                            break;
                        case "ERGO_EXPLORER":
                            addNoteInterface(new ErgoExplorer(jsonObject, this));
                            break;
                        case "KUCOIN_EXCHANGE":
                            addNoteInterface(new KucoinExchange(jsonObject, this));
                            break;
                        case "NETWORK_TIMER":
                            addNoteInterface(new NetworkTimer(jsonObject, this));
                            break;
                        case "ERGO_TOKENS":
                            addNoteInterface(new ErgoTokens(jsonObject, this));
                            break;
                    }

                }

            }

        }

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
        // int i = 0;

        String networkId = noteInterface.getNetworkId();
        for (NoteInterface checkInterface : m_noteInterfaceList) {
            if (checkInterface.getNetworkId().equals(networkId)) {
                try {
                    Files.writeString(logFile.toPath(), "\n" + networkId + " Exists install cancelled");
                } catch (IOException e) {

                }
                return false;
            }
        }
        m_noteInterfaceList.add(noteInterface);
        noteInterface.addUpdateListener((obs, oldValue, newValue) -> save());

        updateNetworksGrid();

        return true;
    }

    public VBox getNetworksBox(double width) {

        m_width = width;

        updateNetworksGrid();
        //    lastUpdated.addListener(e -> {
        //      updateNetworksGrid();
        //   });
        return m_networksBox;
    }

    public double getBoxWidth() {
        return m_width;
    }

    public void setBoxWidth(double width) {
        m_width = width;
        updateNetworksGrid();
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

            VBox.setVgrow(notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(notInstalledVBox, Priority.ALWAYS);

            VBox.setVgrow(m_installedVBox, Priority.ALWAYS);

            VBox leftSide = new VBox(installedVBox, removeBtn);
            leftSide.setPadding(new Insets(5));
            leftSide.prefWidth(m_leftColumnWidth);
            VBox.setVgrow(leftSide, Priority.ALWAYS);
            VBox rightSide = new VBox(notInstalledVBox, addBox);
            HBox.setHgrow(rightSide, Priority.ALWAYS);

            HBox columnsHBox = new HBox(leftSide, rightSide);
            VBox.setVgrow(columnsHBox, Priority.ALWAYS);

            columnsHBox.setPadding(new Insets(10, 10, 10, 10));
            VBox layoutVBox = new VBox(titleBox, columnsHBox);

            Scene addNetworkScene = new Scene(layoutVBox, 700, 400);
            addNetworkScene.getStylesheets().add("/css/startWindow.css");
            m_addNetworkStage.setScene(addNetworkScene);
            m_addNetworkStage.show();

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

            installBtn.setOnAction(e -> {

                try {
                    Files.writeString(logFile.toPath(), "installing: " + (m_focusedInstallable == null ? " null " : m_focusedInstallable.getText() + " nId: " + m_focusedInstallable.getNetworkId() + " installed: " + m_focusedInstallable.getInstalled()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }

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

            updateAvailableLists();
        } else {
            m_addNetworkStage.show();
        }
    }

    public void closeNetworksStage() {
        m_addNetworkStage.close();
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
                try {
                    Files.writeString(logFile.toPath(), "\n" + installable.getName() + " installed: " + installable.getInstalled(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
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

        if (numCells == 0) {
            IconButton addNetworkBtn = new IconButton(App.globeImg, "Add Network");
            addNetworkBtn.setImageWidth(80);
            addNetworkBtn.prefHeight(200);
            addNetworkBtn.prefWidth(200);
            addNetworkBtn.setOnAction(e -> showManageNetworkStage());

            HBox rowBox = new HBox(addNetworkBtn);
            rowBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(rowBox, Priority.ALWAYS);
            rowBox.setPrefHeight(500);
            m_networksBox.getChildren().add(rowBox);
        } else {
            double imageWidth = 100;
            double cellPadding = 15;
            double cellWidth = imageWidth + (cellPadding * 2);

            int floor = (int) Math.floor(getBoxWidth() / (cellWidth + 20));
            int numCol = floor == 0 ? 1 : floor;
            int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            HBox[] rowsBoxes = new HBox[numRows];
            for (int i = 0; i < numRows; i++) {
                rowsBoxes[i] = new HBox();
                m_networksBox.getChildren().add(rowsBoxes[i]);
            }

            ItemIterator grid = new ItemIterator();

            for (NoteInterface noteInterface : m_noteInterfaceList) {

                HBox rowBox = rowsBoxes[grid.getJ()];
                rowBox.getChildren().add(noteInterface.getButton());

                if (grid.getI() < numCol) {
                    grid.setI(grid.getI() + 1);
                } else {
                    grid.setI(0);
                    grid.setJ(grid.getJ() + 1);
                }
            }
        }
    }

    public void installNetwork(String networkId) {
        try {
            Files.writeString(logFile.toPath(), "\ninstalling " + networkId + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        switch (networkId) {
            case "ERGO_EXPLORER":
                addNoteInterface(new ErgoExplorer(this));
                break;
            case "ERGO_WALLET":
                addNoteInterface(new ErgoWallet(this));
                break;
            case "ERGO_NETWORK":
                addNoteInterface(new ErgoNetwork(this));
                break;
            case "KUCOIN_EXCHANGE":
                addNoteInterface(new KucoinExchange(this));
                break;
            case "NETWORK_TIMER":
                addNoteInterface(new NetworkTimer(this));
                break;
            case "ERGO_TOKENS":
                addNoteInterface(new ErgoTokens(this));
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
        boolean success = false;
        for (int i = 0; i < m_noteInterfaceList.size(); i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);
            if (networkId.equals(noteInterface.getNetworkId())) {
                m_noteInterfaceList.remove(i);
                noteInterface.remove();
                try {
                    Files.writeString(logFile.toPath(), "\n" + noteInterface.getButton().getName() + " removed\n");
                } catch (IOException e) {

                }
                success = true;
                break;
            }
        }
        if (success) {
            save();
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

        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(networkId)) {
                return noteInterface;
            }
        }
        return null;
    }

    public NoteInterface getNoteIntefaceByName(String name) {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getName().equals(name)) {
                return noteInterface;
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

    public boolean sendNoteToFullNetworkId(JsonObject note, String tunnelId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int index = tunnelId.indexOf(".");

        NoteInterface networkInterface;
        if (index == -1) {
            networkInterface = getNoteInterface(tunnelId);
            if (networkInterface != null) {
                return networkInterface.sendNote(note, onSucceeded, onFailed);
            }
        } else {
            String networkId = tunnelId.substring(0, index);
            networkInterface = getNoteInterface(networkId);

            networkInterface.sendNoteToFullNetworkId(note, tunnelId, onSucceeded, onFailed);

            if (networkInterface != null) {
                return networkInterface.sendNote(note, onSucceeded, onFailed);
            }

        }
        return false;
    }

    public void save() {
        JsonObject fileObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        fileObject.add("networks", jsonArray);

        String jsonString = fileObject.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {
            Files.writeString(m_networksFile.toPath(), jsonString);
        } catch (IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nError saving networds data: " + e.toString() + "\n");
            } catch (IOException e1) {

            }
        }

    }

}
