package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class TimerNetwork extends Network implements NoteInterface {

    public static String DESCRIPTION = "Timer schedules updates.";
    public static String SUMMARY = "By installing the timer you may schedule regular data updates in Apps, at the selected interval. This allows requesting regular updates, such as regular wallet balance updates.";
    public static String NAME = "Timer";

    private File logFile = new File("timer-log.txt");

    private ArrayList<TimerData> m_timersList = new ArrayList<TimerData>();

    public TimerNetwork(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.TIMER_NETWORK, networksData);

        m_timersList.add(new TimerData(null, this));

    }

    public TimerNetwork(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.TIMER_NETWORK, networksData);

    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/stopwatch-30.png");
    }

    public static Image getAppIcon() {
        return new Image("/assets/stopwatch.png");
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjectElement = note.get("subject");
        JsonElement fullNetworkIdElement = note.get("fullNetworkId");

        if (subjectElement != null && subjectElement.isJsonPrimitive() && fullNetworkIdElement != null && fullNetworkIdElement.isJsonPrimitive()) {
            String fullNetworkID = fullNetworkIdElement.getAsString();

            switch (subjectElement.getAsString()) {
                case "SUBSCRIBE":
                        try {
                    Files.writeString(logFile.toPath(), "\n" + note.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {

                }

                JsonElement timerIdElement = note.get("timerId");

                if (timerIdElement != null && timerIdElement.isJsonPrimitive()) {

                    String timerId = timerIdElement.getAsString();

                    for (int i = 0; i < m_timersList.size(); i++) {
                        TimerData timerData = m_timersList.get(i);

                        if (timerData.getTimerId().equals(timerId)) {
                            JsonObject subscriber = new JsonObject();
                            subscriber.addProperty("fullNetworkId", fullNetworkID);

                            timerData.subscribe(subscriber);
                            return true;
                        }

                    }
                }
                break;
                case "GET_TIMERS":
                    JsonObject timers = new JsonObject();

                    timers.addProperty("subject", "TIMERS");
                    timers.addProperty("networkId", getNetworkId());
                    timers.add("availableTimers", getTimersJsonArray());

                    getNetworksData().sendNoteToFullNetworkId(timers, fullNetworkID, null, null);

                    break;

            }

        }

        return false;
    }

    public JsonArray getTimersJsonArray() {
        JsonArray timers = new JsonArray();

        for (TimerData timerData : m_timersList) {
            timers.add(timerData.getJsonObject());
        }

        return timers;
    }

}
