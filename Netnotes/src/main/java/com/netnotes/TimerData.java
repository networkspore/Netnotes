package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.devskiller.friendly_id.FriendlyId;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

public class TimerData {

    private File logFile;
    private String m_timerID;
    private long m_interval = 5000;
    private TimeUnit m_timeUnit = TimeUnit.MILLISECONDS;

    private boolean m_started = false;

    private NetworkTimer m_timerNetwork;
    private ScheduledExecutorService m_executorService = Executors.newScheduledThreadPool(1);

    private ArrayList<String> m_subscribers = new ArrayList<>();

    public JsonObject getTimeObject() {
        JsonObject timeObject = getJsonObject();
        timeObject.addProperty("subject", "TIME");
        timeObject.addProperty("networkId", m_timerNetwork.getNetworkId());
        timeObject.addProperty("timerId", m_timerID);
        timeObject.addProperty("localDateTime", Utils.formatDateTimeString(LocalDateTime.now()));
        return timeObject;
    }

    private final Runnable m_task = new Runnable() {
        @Override
        public void run() {
            ArrayList<String> itemsToRemove = new ArrayList<>();

            ArrayList<String> subscribers = new ArrayList<>(m_subscribers);
            for (int i = 0; i < subscribers.size(); i++) {
                String fullNetworkId = subscribers.get(i);

                boolean succeeded = m_timerNetwork.getNetworksData().sendNoteToFullNetworkId(getTimeObject(), fullNetworkId, null, null);
                if (!succeeded) {
                    try {
                        Files.writeString(logFile.toPath(), "\nfullNetworkId not found: " + fullNetworkId + " unsubscribe:\n" + fullNetworkId, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {

                    }
                    itemsToRemove.add(fullNetworkId);
                }
            }
            for (String itemId : itemsToRemove) {
                unsubscribe(itemId);
            }
        }
    };

    public TimerData(JsonObject jsonObject, NetworkTimer timerNetwork) {

        m_timerNetwork = timerNetwork;
        if (jsonObject == null) {
            m_timerID = FriendlyId.createFriendlyId();
        } else {
            JsonElement timerIdElement = jsonObject.get("timerId");
            m_timerID = timerIdElement != null ? timerIdElement.getAsString() : FriendlyId.createFriendlyId();

            JsonElement intervalElement = jsonObject.get("interval");
            JsonElement timeUnitElement = jsonObject.get("timeUnit");

            m_interval = intervalElement == null ? 5000 : intervalElement.getAsLong();
            if (timeUnitElement != null) {
                TimeUnit timeUnit = Utils.stringToTimeUnit(timeUnitElement.getAsString());
                m_timeUnit = timeUnit == null ? TimeUnit.MILLISECONDS : timeUnit;
            } else {
                m_timeUnit = TimeUnit.MILLISECONDS;
            }

        }

    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("timerId", m_timerID);
        jsonObject.addProperty("interval", m_interval);
        jsonObject.addProperty("timeUnit", Utils.timeUnitToString(m_timeUnit));

        return jsonObject;
    }

    public String findSubscriber(String fullNetworkId) {
        for (String subscriber : m_subscribers) {
            if (subscriber.equals(fullNetworkId)) {
                return subscriber;
            }
        }
        return null;
    }

    public boolean subscribe(String fullNetworkId) {

        if (findSubscriber(fullNetworkId) != null) {
            return false;
        }

        m_subscribers.add(fullNetworkId);

        if (!m_started) {
            startTimer();
        }
        return true;
    }

    public boolean unsubscribe(String fullNetworkId) {
        boolean found = false;
        for (int i = 0; i < m_subscribers.size(); i++) {
            String subscriber = m_subscribers.get(i);

            if (fullNetworkId.equals(subscriber)) {
                m_subscribers.remove(i);
                found = true;
                break;
            }

        }

        if (m_subscribers.size() == 0) {

            m_executorService.shutdown();
            try {
                if (!m_executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    m_executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                m_executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            m_started = false;
        }
        return found;
    }

    private void startTimer() {

        m_executorService.scheduleAtFixedRate(new Runnable() {
            private final ExecutorService executor = Executors.newFixedThreadPool(1,
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
                    return;
                }
                lastExecution = executor.submit(m_task);
            }
        }, 0, m_interval, m_timeUnit);
    }

    public String getTimerId() {
        return m_timerID;
    }
}
