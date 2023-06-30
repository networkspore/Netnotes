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

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class TimersList {

    private File logFile;
    public SimpleObjectProperty<LocalDateTime> lastUpdatedProperty = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());
    private String m_timerNetworkId;
    private ArrayList<JsonObject> m_availableTimers = new ArrayList<>();
    private JsonArray m_subscribedTimers = new JsonArray();

    //private String m_networkId;
    private NetworksData m_networksData = null;
    private MenuButton[] m_menuCtlButtons = null;
    private HBox m_timerMenuHBox = new HBox();

    private ArrayList<NoteInterface> m_noteInterfaces = new ArrayList<>();

    public TimersList(String timerNetworkId, JsonArray subscribedTimersArray, NetworksData networksData) {
        m_timerNetworkId = timerNetworkId;
        m_networksData = networksData;
        m_timerMenuHBox.setAlignment(Pos.CENTER_LEFT);
        logFile = new File("timerList.txt");
        if (subscribedTimersArray != null) {
            for (JsonElement jsonObjectElement : subscribedTimersArray) {
                if (jsonObjectElement.isJsonObject()) {
                    m_subscribedTimers.add(jsonObjectElement.getAsJsonObject());
                }
            }
        }
    }

    public String getTimerNetworkId() {
        return m_timerNetworkId;
    }

    public void setTimerNetworkId(String networkId) {

        m_subscribedTimers = new JsonArray();
        m_availableTimers.clear();
        m_timerNetworkId = networkId;
        if (networkId == null) {
            update();
        } else {
            getAvailableTimers();
        }
        lastUpdatedProperty.set(LocalDateTime.now());
    }

    public void addNoteInterface(NoteInterface noteInterface) {
        m_noteInterfaces.add(noteInterface);
    }

    public void removeNoteInterface(NoteInterface noteInterface) {
        m_noteInterfaces.remove(noteInterface);
    }

    public HBox createTimerMenu(MenuButton... menuButtons) {

        m_menuCtlButtons = menuButtons;
        for (int i = 0; i < m_menuCtlButtons.length; i++) {
            MenuButton menuButton = m_menuCtlButtons[i];

            menuButton.graphicProperty().addListener(changed -> {
                update();
            });
        }
        update();
        return m_timerMenuHBox;
    }

    public boolean time(JsonObject note, String timerId, String timeString) {
        JsonArray subscription = getSubscribed(timerId);
        if (subscription != null) {
            note.add("subscription", subscription);
            int anySent = 0;

            for (NoteInterface noteInterface : m_noteInterfaces) {
                boolean sent = noteInterface.sendNote(note, null, null);
                if (sent) {
                    anySent += 1;
                }
            }

            if (anySent > 0) {
                return true;
            }
        }
        return false;
    }

    public void update() {

        try {
            Files.writeString(logFile.toPath(), "\nupdating (m_timerNetworkId: " + (m_timerNetworkId == null ? "NULL" : m_timerNetworkId) + " menubutton size: " + m_menuCtlButtons.length + "):", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }
        Platform.runLater(() -> m_timerMenuHBox.getChildren().clear());
        if (m_timerNetworkId != null) {

            for (MenuButton menuBtn : m_menuCtlButtons) {

                String menuBtnId = menuBtn.getUserData() == null ? null : (String) menuBtn.getUserData();

                if (menuBtnId != null) {

                    try {
                        Files.writeString(logFile.toPath(), "\nmenuButton: " + menuBtnId, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }

                    InstallableIcon installable = new InstallableIcon(m_networksData, menuBtnId, true);

                    Tooltip menuBtnTooltip = new Tooltip(installable.getText());
                    menuBtnTooltip.setShowDelay(new javafx.util.Duration(100));
                    menuBtnTooltip.setFont(App.txtFont);

                    ImageView menuBtnView = IconButton.getIconView(installable.getIcon(), 30);
                    Label menuBtnViewLbl = new Label("", menuBtnView);
                    menuBtnViewLbl.setTooltip(menuBtnTooltip);

                    JsonObject subscribedObject = getSubscribedObject(menuBtnId);
                    String text = "(none)";
                    String selectedTimerId = null;

                    if (subscribedObject != null) {
                        selectedTimerId = subscribedObject.get("timerId").getAsString();
                        long interval = subscribedObject.get("interval").getAsLong();
                        String timeUnit = subscribedObject.get("timeUnit").getAsString();

                        text = interval + " " + timeUnit;
                        subscribe(selectedTimerId);

                    }

                    NetMenuButton netMenuButton = getMenuButton(installable.getId(), installable.getName(), text, selectedTimerId);
                    updateTimerButtons(netMenuButton);
                    if (netMenuButton.getItems().size() == 1) {
                        netMenuButton.setText("Add timer");
                        netMenuButton.getItems().clear();
                        netMenuButton.setOnMouseClicked(mouseEvent -> {
                            try {
                                Files.writeString(logFile.toPath(), netMenuButton.getName() + ": opening timers", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {

                            }
                            sendOpenTimersNote();
                        });
                    } else {
                        netMenuButton.setOnMouseClicked(null);
                    }

                    Platform.runLater(() -> m_timerMenuHBox.getChildren().addAll(menuBtnViewLbl, netMenuButton));

                }
            }

        } else {

        }
    }

    public boolean sendOpenTimersNote() {
        JsonObject openObject = getOpenObject();
        try {
            Files.writeString(logFile.toPath(), "\nsending open object:\n" + openObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

        if (m_timerNetworkId != null) {

            NoteInterface noteInterface = m_networksData.getNoteInterface(m_timerNetworkId);

            return noteInterface.sendNote(openObject, null, null);
        }
        return false;
    }

    public JsonArray getSubscribedTimers() {
        return m_subscribedTimers;
    }

    public void clearSubscribers() {
        m_subscribedTimers = new JsonArray();
        m_timerMenuHBox.getChildren().clear();

    }

    public JsonObject getSubscribedObject(String networkId) {
        for (JsonElement subscriberElement : m_subscribedTimers) {
            JsonObject subscriber = subscriberElement.getAsJsonObject();

            JsonElement networkIdElement = subscriber.get("networkId");
            if (networkIdElement != null && networkIdElement.getAsString().equals(networkId)) {
                return subscriber;
            }
        }
        return null;
    }

    public JsonArray getSubscribed(String timerId) {
        JsonArray subscription = new JsonArray();
        for (JsonElement subscriberElement : m_subscribedTimers) {
            JsonObject subscriber = subscriberElement.getAsJsonObject();

            JsonElement networkIdElement = subscriber.get("timerId");
            if (networkIdElement != null && networkIdElement.getAsString().equals(timerId)) {
                subscription.add(subscriber);
            }
        }

        return subscription;
    }

    public void setAvailableTimers(JsonArray availableTimers) {
        // m_networkId = networkId;
        if (availableTimers != null && availableTimers.size() > 0) {
            try {
                Files.writeString(logFile.toPath(), "\nSetting available timers:" + availableTimers.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }
            m_availableTimers.clear();
            if (availableTimers != null) {
                for (JsonElement timerElement : availableTimers) {
                    if (timerElement.isJsonObject()) {
                        m_availableTimers.add(timerElement.getAsJsonObject());
                    }
                }
            }
        }

        updateSubscribers();

        update();
    }

    private void updateSubscribers() {
        if (m_availableTimers.size() == 0) {
            m_subscribedTimers = new JsonArray();
        } else {
            for (JsonElement subscriberElement : m_subscribedTimers) {
                if (subscriberElement.isJsonObject()) {
                    JsonObject subscriber = subscriberElement.getAsJsonObject();
                    String subscriberTimerId = subscriber.get("timerId").getAsString();
                    boolean notFound = true;
                    for (JsonObject availableTimer : m_availableTimers) {
                        if (availableTimer.get("timerId").getAsString().equals(subscriberTimerId)) {
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound) {
                        m_subscribedTimers.remove(subscriberElement);
                    }
                } else {
                    m_subscribedTimers.remove(subscriberElement);
                }
            }
        }
    }

    private NetMenuButton getMenuButton(String id, String name, String text, Object userData) {
        NetMenuButton netMenuButton = new NetMenuButton(id, name, text, userData);

        return netMenuButton;
    }

    private void updateTimerButtons(NetMenuButton netMenuButton) {
        netMenuButton.getItems().clear();
        netMenuButton.getItems().add(createNullMenuItem(netMenuButton));

        for (JsonObject availableTimer : m_availableTimers) {
            String timerId = availableTimer.get("timerId").getAsString();
            long interval = availableTimer.get("interval").getAsLong();
            String timeUnit = availableTimer.get("timeUnit").getAsString();

            netMenuButton.getItems().add(createMenuItem(netMenuButton, timerId, interval, timeUnit));
        }
    }

    private MenuItem createNullMenuItem(NetMenuButton menuButton) {
        MenuItem menuItem = new MenuItem("(none)");
        String buttonNetworkId = menuButton.getNetworkId();

        menuItem.setOnAction(itemAction -> {
            String oldTimerId = menuButton.getUserData() == null ? null : (String) menuButton.getUserData();

            if (!(oldTimerId == null)) {
                removeSubscriber(buttonNetworkId, oldTimerId);
                menuButton.setText(menuItem.getText());
                menuButton.setUserData(null);

                if (getSubscribed(oldTimerId).size() == 0) {
                    unsubscibe(oldTimerId);
                }
            }
        });
        return menuItem;
    }

    private void removeSubscriber(String networkId, String timerId) {
        for (int i = 0; i < m_subscribedTimers.size(); i++) {
            JsonElement subscriberElement = m_subscribedTimers.get(i);
            JsonObject subscriberObject = subscriberElement.getAsJsonObject();
            String subscriberNetworkId = subscriberObject.get("networkId").getAsString();
            String subscriberTimerId = subscriberObject.get("timerId").getAsString();

            if (subscriberNetworkId.equals(networkId) && subscriberTimerId.equals(timerId)) {
                m_subscribedTimers.remove(i);
                break;
            }
        }
        lastUpdatedProperty.set(LocalDateTime.now());
    }

    private void addSubscriber(String networkId, String timerId, long interval, String timeUnit) {
        JsonObject subscriberJson = new JsonObject();
        subscriberJson.addProperty("networkId", networkId);
        subscriberJson.addProperty("timerId", timerId);
        subscriberJson.addProperty("interval", interval);
        subscriberJson.addProperty("timeUnit", timeUnit);

        m_subscribedTimers.add(subscriberJson);
        lastUpdatedProperty.set(LocalDateTime.now());
    }

    private MenuItem createMenuItem(NetMenuButton menuButton, String timerId, long interval, String timeUnit) {
        MenuItem menuItem = new MenuItem(interval + " " + timeUnit);
        String buttonNetworkId = menuButton.getNetworkId();

        String newTimerId = timerId;
        long newInterval = interval;
        String newTimeUnit = timeUnit;
        menuItem.setOnAction(itemAction -> {
            String oldTimerId = menuButton.getUserData() == null ? "NULLDATA" : (String) menuButton.getUserData();

            if (!oldTimerId.equals(newTimerId)) {

                menuButton.setText(menuItem.getText());
                menuButton.setUserData(newTimerId);

                if (!oldTimerId.equals("NULLDATA")) {

                    removeSubscriber(buttonNetworkId, oldTimerId);

                }

                addSubscriber(buttonNetworkId, newTimerId, newInterval, newTimeUnit);

                if (!oldTimerId.equals("NULLDATA") && getSubscribed(oldTimerId).size() == 0) {
                    unsubscibe(oldTimerId);
                }
                subscribe(newTimerId);
            }
        });
        return menuItem;
    }

    private boolean unsubscibe(String timerId) {
        NoteInterface timerInterface = m_networksData.getNoteInterface(m_timerNetworkId);
        if (timerInterface != null) {
            JsonObject unsubscribeJson = new JsonObject();
            unsubscribeJson.addProperty("subject", "UNSUBSCRIBE");
            unsubscribeJson.addProperty("timerId", timerId);

            return timerInterface.sendNote(unsubscribeJson, null, null);
        }
        return false;
    }

    private boolean subscribe(String timerId) {
        NoteInterface timerInterface = m_networksData.getNoteInterface(m_timerNetworkId);

        if (timerInterface != null) {
            JsonObject subscribeJson = new JsonObject();
            subscribeJson.addProperty("subject", "SUBSCRIBE");
            subscribeJson.addProperty("timerId", timerId);

            return timerInterface.sendNote(subscribeJson, null, null);
        }
        return false;
    }

    public JsonObject getOpenObject() {
        JsonObject getTimersObject = new JsonObject();
        getTimersObject.addProperty("subject", "OPEN");

        return getTimersObject;
    }

    public JsonObject getTimersObject() {
        JsonObject getTimersObject = new JsonObject();
        getTimersObject.addProperty("subject", "GET_TIMERS");

        return getTimersObject;
    }

    public boolean getAvailableTimers() {

        if (m_timerNetworkId != null) {
            NoteInterface noteInterface = m_networksData.getNoteInterface(m_timerNetworkId);
            if (noteInterface != null) {
                JsonObject note = getTimersObject();

                try {
                    Files.writeString(logFile.toPath(), "\nGet available timers: " + getTimersObject().toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }
                return noteInterface.sendNote(note,
                        (succeeded) -> {
                            Object value = succeeded.getSource().getValue();

                            JsonObject timersObject = value == null ? null : (JsonObject) value;

                            if (timersObject != null) {
                                try {
                                    Files.writeString(logFile.toPath(), "\nAvailable timers: " + timersObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e) {

                                }

                                JsonElement timersElement = timersObject.get("availableTimers");
                                if (timersElement != null && timersElement.isJsonArray()) {
                                    setAvailableTimers(timersElement.getAsJsonArray());
                                } else {
                                    setAvailableTimers(null);
                                }
                            } else {
                                setAvailableTimers(null);
                            }
                        },
                        (failed) -> {
                            setAvailableTimers(null);
                        });
            }
        }
        return false;
    }
}
