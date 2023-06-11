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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class TimerNetwork extends Network implements NoteInterface {

    public static String DESCRIPTION = "Timer schedules updates.";
    public static String SUMMARY = "**Required for obtaining network updates**";
    public static String NAME = "Timer";

    private long m_interval = 5000;
    private ArrayList<String> m_subscribers = new ArrayList<>();

    private File logFile = new File("timer-log.txt");

    private ScheduledExecutorService m_executorService = null;

    private final Runnable m_task = new Runnable() {
        @Override
        public void run() {
            for (String networkId : m_subscribers) {
                getNetworksData().sendNoteToFullNetworkId(getJsonObject(), networkId, null, null);
            }
        }
    };

    public TimerNetwork(NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.TIMER_NETWORK, networksData);

    }

    public TimerNetwork(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NetworkID.TIMER_NETWORK, networksData);

        JsonElement intervalElement = jsonObject.get("interval");
        m_interval = intervalElement == null ? 5000 : intervalElement.getAsLong();

    }

    private void startTimer() {

        m_executorService.scheduleAtFixedRate(new Runnable() {
            private final ExecutorService executor = Executors.newFixedThreadPool(3,
                    new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            private Future<?> lastExecution;

            @Override
            public void run() {
                if (lastExecution != null && !lastExecution.isDone()) {
                    try {
                        Files.writeString(logFile.toPath(), "\nPrevious execution not complete: " + lastExecution.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }

                }
                lastExecution = executor.submit(m_task);
            }
        }, m_interval, m_interval, TimeUnit.MILLISECONDS);
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/stopwatch-30.png");
    }

    public static Image getAppIcon() {
        return new Image("/assets/stopwatch.png");
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjecElement = note.get("subject");
        if (subjecElement != null) {
            switch (subjecElement.getAsString()) {
                case "SUBSCRIBE":
                    JsonElement fullNetworkIdElement = note.get("fullNetworkId");

                    if (fullNetworkIdElement != null) {
                        String fullNetworkID = fullNetworkIdElement.getAsString();

                        for (String networkId : m_subscribers) {
                            if (fullNetworkID.equals(networkId)) {
                                return false;
                            }
                        }

                        m_subscribers.add(fullNetworkID);
                        if (m_subscribers.size() > 0) {
                            if (m_executorService == null) {
                                Executors.newScheduledThreadPool(1);
                            }
                        }
                    }

                    break;
            }
        }

        return false;
    }

}
