package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public interface NoteInterface {

    String getName();

    String getNetworkId();

    String getFullNetworkId();

    SimpleObjectProperty<LocalDateTime> getLastUpdated();

    boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed);

    JsonObject getJsonObject();

    IconButton getButton();

    ArrayList<NoteInterface> getTunnelNoteInterfaces();

    NoteInterface getTunnelNoteInterface(String networkId);

    void addTunnelNoteInterface(NoteInterface noteInterface);

    void removeTunnelNoteInterface(String id);

    NetworksData getNetworksData();

    void sendNoteToTunnelInterface(JsonObject note, String tunnelId, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed);

    NoteInterface getParentInterface();

    void addUpdateListener(ChangeListener<LocalDateTime> changeListener);

    void removeUpdateListener();

    void remove();
}
