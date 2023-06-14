package com.netnotes;

import java.io.File;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import javafx.scene.image.Image;

public class Network extends IconButton {

    private File logFile = new File("Network-log");
    private String m_networkId;
    private NetworksData m_networksData;
    private ArrayList<NoteInterface> m_tunnelInterfaceList = new ArrayList<>();
    private NoteInterface m_parentInterface = null;
    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private ChangeListener<LocalDateTime> m_changeListener = null;

    public static class NetworkID {

        public static String ERGO_NETWORK = "ERGO_NETWORK";
        public static String ERGO_WALLET = "ERGO_WALLET";
        public static String KUKOIN_EXCHANGE = "KUCOIN_EXCHANGE";
        public static String ERGO_EXPLORER = "ERGO_EXPLORER";
        public static String NETWORK_TIMER = "NETWORK_TIMER";
    }

    public Network(Image icon, String name, String id, NetworksData networksData) {
        super(icon);
        setName(name);
        setIconStyle(IconStyle.ICON);
        m_networkId = id;
        m_networksData = networksData;
        m_parentInterface = null;
    }

    public Network(Image icon, String name, String id, NoteInterface parentInterface) {
        this(icon, name, id, parentInterface.getNetworksData());
        m_parentInterface = parentInterface;

    }

    public String getFullNetworkId() {
        String fullNetworkId = m_networkId;
        NoteInterface parent = m_parentInterface;
        while (parent != null) {
            fullNetworkId = parent.getNetworkId() + "." + fullNetworkId;
            parent = parent.getParentInterface();
        }
        return fullNetworkId;
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        return false;
    }

    public boolean sendNoteToFullNetworkId(JsonObject note, String fullNetworkId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        int indexOfNetworkID = fullNetworkId.indexOf(getNetworkId());

        int indexOfperiod = fullNetworkId.indexOf(".", indexOfNetworkID);

        if (indexOfperiod == -1) {
            return sendNote(note, onSucceeded, onFailed);
        } else {
            int indexOfSecondPeriod = fullNetworkId.indexOf(".", indexOfperiod + 1);
            String tunnelID;

            if (indexOfSecondPeriod == -1) {
                tunnelID = fullNetworkId.substring(indexOfperiod);
            } else {
                tunnelID = fullNetworkId.substring(indexOfperiod, indexOfSecondPeriod);
            }

            NoteInterface tunnelInterface = getTunnelNoteInterface(tunnelID);
            if (tunnelInterface != null) {
                return tunnelInterface.sendNoteToFullNetworkId(note, fullNetworkId, onSucceeded, onFailed);
            }
        }

        return false;

    }

    public NoteInterface getParentInterface() {
        return m_parentInterface;
    }

    public void setNetworkId(String id) {
        m_networkId = id;
    }

    public String getNetworkId() {
        return m_networkId;
    }

    public JsonObject getJsonObject() {
        JsonObject networkObj = new JsonObject();
        networkObj.addProperty("name", getName());
        networkObj.addProperty("networkId", getNetworkId());
        return networkObj;

    }

    public NetworksData getNetworksData() {
        return m_networksData;
    }

    public NoteInterface getTunnelNoteInterface(String networkId) {

        for (NoteInterface noteInterface : m_tunnelInterfaceList) {
            if (noteInterface.getNetworkId().equals(networkId)) {
                return noteInterface;
            }
        }
        return null;
    }

    public ArrayList<NoteInterface> getTunnelNoteInterfaces() {
        return m_tunnelInterfaceList;
    }

    public void addTunnelNoteInterface(NoteInterface noteInterface) {
        if (getTunnelNoteInterface(noteInterface.getNetworkId()) == null) {
            m_tunnelInterfaceList.add(noteInterface);
        }
    }

    public void removeTunnelNoteInterface(String id) {
        for (int i = 0; i < m_tunnelInterfaceList.size(); i++) {
            NoteInterface tunnel = m_tunnelInterfaceList.get(i);

            if (tunnel.getNetworkId().equals(id)) {
                m_tunnelInterfaceList.remove(tunnel);
                break;
            }

        }
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_lastUpdated;
    }

    public void addUpdateListener(ChangeListener<LocalDateTime> changeListener) {
        m_changeListener = changeListener;
        if (m_changeListener != null) {
            m_lastUpdated.addListener(m_changeListener);

        }
        // m_lastUpdated.addListener();
    }

    public void removeUpdateListener() {
        if (m_changeListener != null) {
            m_lastUpdated.removeListener(m_changeListener);
            m_changeListener = null;
        }
    }

    public void remove() {
        removeUpdateListener();
    }

}
