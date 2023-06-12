package com.netnotes;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.scene.control.MenuItem;

public class TimersList {

    private ArrayList<JsonObject> m_availableTimers = new ArrayList<>();
    private ArrayList<NetMenuButton> m_timerMenuBtns = new ArrayList<NetMenuButton>();

    public TimersList() {

    }

    public void setAvailableTimers(JsonArray availableTimers) {
        m_availableTimers.clear();

        for (JsonElement timerElement : availableTimers) {
            if (timerElement.isJsonObject()) {
                m_availableTimers.add(timerElement.getAsJsonObject());
            }
        }

        updateAllTimerButtons();
    }

    public NetMenuButton getMenuButton(String networkId, String text, Object userData) {
        NetMenuButton netMenuButton = new NetMenuButton(networkId, text, userData);
        m_timerMenuBtns.add(netMenuButton);

        return netMenuButton;
    }

    public void updateTimerButtons(NetMenuButton netMenuButton) {
        netMenuButton.getItems().clear();

        for (JsonObject availableTimer : m_availableTimers) {
            String timerId = availableTimer.get("timerId").getAsString();
            long interval = availableTimer.get("interval").getAsLong();
            String timeUnit = availableTimer.get("timeUnit").getAsString();

            netMenuButton.getItems().add(createMenuItem(netMenuButton, timerId, interval, timeUnit));
        }
    }

    public void updateAllTimerButtons() {
        for (NetMenuButton netMenuButton : m_timerMenuBtns) {
            updateTimerButtons(netMenuButton);
        }
    }

    public MenuItem createMenuItem(NetMenuButton menuButton, String timerId, long interval, String timeUnit) {
        MenuItem menuItem = new MenuItem(interval + " " + timeUnit);
        String newTimerId = timerId;
        menuItem.setOnAction(itemAction -> {
            menuButton.setText(menuItem.getText());
            menuButton.setUserData(newTimerId);
        });
        return menuItem;
    }
}
