package com.netnotes;

import java.io.File;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletsDataList {

    private File logFile = new File("walletsDataBox-log.txt");
    private ArrayList<NoteInterface> m_noteInterfaceList = new ArrayList<>();
    private String m_selectedId;
    private VBox m_buttonGrid = null;
    private double m_width = 400;
    private String m_direction = "column";

    public SimpleObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());

    public WalletsDataList(JsonArray jsonArray, NoteInterface noteInterface) {

        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            JsonElement nameElement = jsonObject.get("name");
            JsonElement idElement = jsonObject.get("uuid");
            JsonElement fileLocationElement = jsonObject.get("walletFile");
            JsonElement networkTypeElement = jsonObject.get("networkType");

            if (nameElement != null && idElement != null && fileLocationElement != null) {
                String name = nameElement.getAsString();
                String id = idElement.getAsString();
                File walletFile = new File(fileLocationElement.getAsString());
                NetworkType networkType = NetworkType.fromValue(networkTypeElement.getAsString());

                m_noteInterfaceList.add(new WalletData(name, id, walletFile, networkType, noteInterface));
            }
        }

    }

    public void addOpen(WalletData walletData) {
        m_noteInterfaceList.add(walletData);
        updateGrid();
        walletData.open();
        lastUpdated.set(LocalDateTime.now());
    }

    public void remove(String id) {
        for (NoteInterface noteInterface : m_noteInterfaceList) {
            if (noteInterface.getNetworkId().equals(id)) {
                m_noteInterfaceList.remove(noteInterface);
                break;
            }
        }
        updateGrid();
        lastUpdated.set(LocalDateTime.now());
    }

    public void setButtonGridNull() {
        if (m_buttonGrid != null) {
            Pane parent = (Pane) m_buttonGrid.getParent();
            if (parent == null) {
                m_buttonGrid.getChildren().clear();
                m_buttonGrid = null;
            } else {
                parent.getChildren().remove(m_buttonGrid);
                m_buttonGrid.getChildren().clear();
                m_buttonGrid = null;
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

    public void sendNoteToTunnelInterface(JsonObject note, String tunnelId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int index = tunnelId.indexOf(":");

        String networkId = index == -1 ? tunnelId : tunnelId.substring(0, index);

        NoteInterface networkInterface = getNoteInterface(networkId);

        for (NoteInterface noteInterface : networkInterface.getTunnelNoteInterfaces()) {

            if (noteInterface.getNetworkId().equals(tunnelId)) {
                noteInterface.sendNoteToTunnelInterface(note, tunnelId, onSucceeded, onFailed);
            }
        }

    }

    public int size() {
        return m_noteInterfaceList.size();
    }

    public JsonArray getJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_noteInterfaceList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        return jsonArray;
    }

    public double getWidth() {
        return m_width;
    }

    public void setWidth(double width) {
        m_width = width;
        updateGrid();
    }

    public VBox getButtonGrid() {
        if (m_buttonGrid == null) {
            m_buttonGrid = new VBox();
            HBox.setHgrow(m_buttonGrid, Priority.ALWAYS);
        }
        updateGrid();
        return m_buttonGrid;
    }

    public void updateGrid() {

        int numCells = m_noteInterfaceList.size();

        m_buttonGrid.getChildren().clear();

        for (int i = 0; i < numCells; i++) {
            NoteInterface noteInterface = m_noteInterfaceList.get(i);

            IconButton rowButton = noteInterface.getButton();
            HBox.setHgrow(rowButton, Priority.ALWAYS);
            m_buttonGrid.getChildren().add(rowButton);
        }
        /*
            try {
                Files.writeString(logFile.toPath(), "networks: " + numCells);
            } catch (IOException e) {

            } 
           
                double imageWidth = 100;
                double cellPadding = 15;
                double cellWidth = imageWidth + (cellPadding * 2);

                int floor = (int) Math.floor(m_width / (cellWidth + 20));

                int numCol = floor == 0 ? 1 : floor;

                int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1; //  (int) ((numCells > 0) && (numCol != 0) ? Math.ceil(numCells / numCol) : 1);

                HBox[] rowsBoxes = new HBox[numRows];
                for (int i = 0; i < numRows; i++) {
                    rowsBoxes[i] = new HBox();
                    m_buttonGrid.getChildren().add(rowsBoxes[i]);
                }

                //Image iconImage = ergoNetworkImg;
                ItemIterator grid = new ItemIterator();

                for (NoteInterface noteInterface : m_noteInterfaceList) {
                    // gridBox.getChildren().add(network.getButton());
                    HBox rowBox = rowsBoxes[grid.getJ()];
                    rowBox.getChildren().add(noteInterface.getButton());

                    if (grid.getI() < numCol) {
                        grid.setI(grid.getI() + 1);
                    } else {
                        grid.setI(0);
                        grid.setJ(0);
                    }
                }*/

    }

}
