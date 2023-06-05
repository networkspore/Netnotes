package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.netnotes.IconButton.IconStyle;
import com.netnotes.Network.NetworkID;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworksData {

    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private File m_networksFile;
    private String m_selectedId;
    private VBox m_networksBox;
    private double m_width;

    //  public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private File logFile = new File("networkData-log.txt");

    public NetworksData(JsonObject networksObject, File networksFile) {

        JsonElement jsonArrayElement = networksObject == null ? null : networksObject.get("networks");
        m_networksFile = networksFile;
        m_networksBox = new VBox();

        /* try {
            Files.writeString(logFile.toPath(), "\nnetworks data", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        } */
        if (jsonArrayElement != null) {

            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");
                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();
                    NoteInterface noteInterface;
                    switch (networkId) {
                        case "ERGO_NETWORK":
                            noteInterface = new ErgoNetwork(jsonObject, this);
                            m_noteInterfaceList.add(noteInterface);
                            noteInterface.getLastUpdated().addListener(e -> save());
                            break;
                        case "ERGO_WALLET":
                            noteInterface = new ErgoWallet(jsonObject, this);
                            m_noteInterfaceList.add(noteInterface);
                            noteInterface.getLastUpdated().addListener(e -> save());
                            break;
                        case "ERGO_EXPLORER":
                            noteInterface = new ErgoExplorer(jsonObject, this);
                            m_noteInterfaceList.add(noteInterface);
                            noteInterface.getLastUpdated().addListener(e -> save());
                            break;
                        case "KUCOIN_EXCHANGE":
                            noteInterface = new KucoinExchange(jsonObject, this);
                            m_noteInterfaceList.add(noteInterface);
                            noteInterface.getLastUpdated().addListener(e -> save());
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
                return false;
            }
        }
        m_noteInterfaceList.add(noteInterface);
        noteInterface.getLastUpdated().addListener(updated -> {
            save();
        });
        try {
            save();
        } catch (Exception e) {

        }
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
    public static String[] networkIds = new String[]{
        NetworkID.ERGO_EXPLORER,
        NetworkID.ERGO_NETWORK,
        NetworkID.ERGO_WALLET,
        NetworkID.KUKOIN_EXCHANGE
    };

    private VBox m_installedList = null;
    private VBox m_notInstalledList = null;
    //private double m_installedListWidth = 150;
    private Stage m_addNetworkStage = null;

    public void showManageNetworkStage() {

        if (m_addNetworkStage == null) {
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

            m_installedList = new VBox();
            m_notInstalledList = new VBox();

            Button installBtn = new Button("Install");

            installBtn.setId("menuBarBtn");
            installBtn.setOnAction(e -> {
                m_notInstalledList.getChildren().forEach(notInstalledButton -> {
                    if (notInstalledButton.isFocused()) {
                        IconButton iconButton = (IconButton) notInstalledButton;
                        switch (iconButton.getName()) {
                            case "Ergo Explorer":
                                addNoteInterface(new ErgoExplorer(this));
                                break;
                            case "Ergo Network":
                                addNoteInterface(new ErgoNetwork(this));
                                break;
                            case "Ergo Wallet":
                                addNoteInterface(new ErgoWallet(this));
                                break;
                            case "KuCoin Exchange":
                                addNoteInterface(new KucoinExchange(this));
                                break;
                        }
                        Platform.runLater(() -> installBtn.requestFocus());
                        updateAvailableLists();
                    }
                });
            });

            Button removeBtn = new Button("Remove");
            removeBtn.setPrefWidth(175);
            removeBtn.setId("menuBarBtn");
            removeBtn.setOnAction(e -> {

                m_installedList.getChildren().forEach(installedButton -> {
                    if (installedButton.isFocused()) {
                        IconButton iconButton = (IconButton) installedButton;
                        switch (iconButton.getName()) {
                            case "Ergo Explorer":
                                removeNoteInterface(NetworkID.ERGO_EXPLORER);
                                break;
                            case "Ergo Network":
                                removeNoteInterface(NetworkID.ERGO_NETWORK);
                                break;
                            case "Ergo Wallet":
                                removeNoteInterface(NetworkID.ERGO_WALLET);
                                break;
                            case "KuCoin Exchange":
                                removeNoteInterface(NetworkID.KUKOIN_EXCHANGE);
                                break;
                        }
                        updateAvailableLists();
                    }
                });
            });

            Region vSpacerOne = new Region();
            VBox.setVgrow(vSpacerOne, Priority.ALWAYS);
            HBox.setHgrow(m_installedList, Priority.ALWAYS);

            VBox installedVBox = new VBox(m_installedList, vSpacerOne, removeBtn);

            Region leftSpacer = new Region();
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);

            Region topSpacer = new Region();
            VBox.setVgrow(topSpacer, Priority.ALWAYS);

            HBox addBox = new HBox(leftSpacer, installBtn);
            HBox.setHgrow(m_notInstalledList, Priority.ALWAYS);
            VBox notInstalledVBox = new VBox(m_notInstalledList, topSpacer, addBox);
            VBox.setVgrow(notInstalledVBox, Priority.ALWAYS);
            HBox.setHgrow(notInstalledVBox, Priority.ALWAYS);
            notInstalledVBox.setId("bodyBox");

            HBox columnsHBox = new HBox(installedVBox, notInstalledVBox);
            VBox.setVgrow(columnsHBox, Priority.ALWAYS);

            columnsHBox.setPadding(new Insets(10, 10, 10, 10));
            VBox layoutVBox = new VBox(titleBox, columnsHBox);

            VBox.setVgrow(m_installedList, Priority.ALWAYS);
            Scene addNetworkScene = new Scene(layoutVBox, 700, 400);
            addNetworkScene.getStylesheets().add("/css/startWindow.css");
            m_addNetworkStage.setScene(addNetworkScene);
            m_addNetworkStage.show();

            updateAvailableLists();
        } else {
            m_addNetworkStage.show();
        }
    }

    public void closeNetworksStage() {
        m_addNetworkStage.close();
        m_addNetworkStage = null;
        m_notInstalledList = null;
        m_installedList = null;
    }

    public void updateAvailableLists() {
        if (m_installedList != null && m_notInstalledList != null) {
            m_installedList.getChildren().clear();
            m_notInstalledList.getChildren().clear();

            double listImageWidth = 30;
            double listImagePadding = 5;

            ItemIterator grid = new ItemIterator();

            double imageWidth = new IconButton().getImageWidth();
            double cellPadding = new IconButton().getPadding().getLeft();
            double cellWidth = imageWidth + (cellPadding * 2);
            double numCells = networkIds.length - m_noteInterfaceList.size();
            double boxWidth = m_addNetworkStage.getWidth() - 150;

            int floor = (int) Math.floor(boxWidth / (cellWidth + 20));
            int numCol = floor == 0 ? 1 : floor;
            int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            HBox[] rowsBoxes = new HBox[numRows];
            for (int i = 0; i < numRows; i++) {
                rowsBoxes[i] = new HBox();
                HBox.setHgrow(rowsBoxes[i], Priority.ALWAYS);

                m_notInstalledList.getChildren().add(rowsBoxes[i]);
            }

            for (String networkId : networkIds) {
                NoteInterface noteInterface = getNoteInterface(networkId);

                if (noteInterface == null) {

                    HBox rowBox = rowsBoxes[grid.getJ()];

                    try {
                        Files.writeString(logFile.toPath(), "\nRow: " + grid.getJ() + " Col: " + grid.getI(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    if (grid.getI() < numCol) {
                        grid.setI(grid.getI() + 1);
                    } else {
                        grid.setI(0);
                        grid.setJ(grid.getJ() + 1);
                    }

                    InstallableIcon installableIcon = new InstallableIcon(this, networkId);

                    rowBox.getChildren().add(installableIcon);

                } else {

                    InstallableIcon installableIcon = new InstallableIcon(noteInterface);

                    m_installedList.getChildren().add(installableIcon);

                }
            }
        }
    }

    public void updateNetworksGrid() {
        m_networksBox.getChildren().clear();

        int numCells = m_noteInterfaceList.size();

        if (numCells == 0) {
            IconButton addNetworkBtn = new IconButton(App.globeImg, "Add network") {
                @Override
                public void open() {

                    showManageNetworkStage();
                    super.open();
                }
            };
            addNetworkBtn.setImageWidth(80);
            addNetworkBtn.prefHeight(200);
            addNetworkBtn.prefWidth(200);

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
        // int i = 0;
        for (NoteInterface checkInterface : m_noteInterfaceList) {
            if (checkInterface.getNetworkId().equals(networkId)) {
                boolean success = m_noteInterfaceList.remove(checkInterface);
                if (success) {
                    try {
                        save();
                    } catch (Exception e) {

                    }
                }
                return success;
            }
        }
        updateNetworksGrid();

        return false;
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

            int lastIndex = tunnelId.indexOf(".", index + 1);
            while (lastIndex != -1 && networkInterface != null) {
                networkId = tunnelId.substring(index + 1, lastIndex);
                index = lastIndex;
                networkInterface = getTunnelInterface(networkInterface, networkId);
                lastIndex = tunnelId.indexOf(".", index + 1);
            }

            networkId = tunnelId.substring(index + 1, tunnelId.length());
            networkInterface = getTunnelInterface(networkInterface, networkId);

            if (networkInterface != null) {
                return networkInterface.sendNote(note, onSucceeded, onFailed);
            }

        }
        return false;
    }

    public static NoteInterface getTunnelInterface(NoteInterface networkInterface, String networkId) {
        for (NoteInterface noteInterface : networkInterface.getTunnelNoteInterfaces()) {

            if (noteInterface.getNetworkId().equals(networkId)) {
                return noteInterface;
            }
            break;
        }
        return null;
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
