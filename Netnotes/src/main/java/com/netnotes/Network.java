package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.text.FontWeight;

public class Network extends IconButton {

    private String m_networkId;
    private NetworksData m_networksData;
    private ArrayList<NoteInterface> m_tunnelInterfaceList = new ArrayList<>();
    private NoteInterface m_parentInterface = null;
    private SimpleObjectProperty<LocalDateTime> m_lastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());

    public static class NetworkID {

        public static String ERGO_NETWORK = "ERGO_NETWORK";
        public static String ERGO_WALLET = "ERGO_WALLET";
        public static String KUKOIN_EXCHANGE = "KUCOIN_EXCHANGE";
        public static String ERGO_EXPLORER = "ERGO_EXPLORER";
    }

    public Network(Image icon, String name, String id, NetworksData networksData) {
        super(icon);
        setName(name);
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
        networkObj.addProperty("name", getText());
        networkObj.addProperty("networkId", m_networkId);

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
        m_tunnelInterfaceList.forEach(tunnel -> {
            if (tunnel.getNetworkId().equals(id)) {
                m_tunnelInterfaceList.remove(tunnel);
            }
        });
    }

    public void sendNoteToTunnelInterface(JsonObject note, String tunnelId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        for (NoteInterface tunnelInterface : m_tunnelInterfaceList) {
            if (tunnelInterface.getNetworkId().equals(tunnelId)) {
                tunnelInterface.sendNote(note, onSucceeded, onFailed);
            }
        }
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdated() {
        return m_lastUpdated;
    }
}
