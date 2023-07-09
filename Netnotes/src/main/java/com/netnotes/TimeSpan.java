package com.netnotes;

public class TimeSpan {

    private long m_seconds;
    private String m_name;
    private String m_id;

    public TimeSpan(String id) {
        setup(id);
    }

    public TimeSpan(String name, String id, long seconds) {
        m_seconds = seconds;
        m_id = id;
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public long getSeconds() {
        return m_seconds;
    }

    public String getId() {
        return m_id;
    }

    private void setup(String id) {
        m_id = id;

        switch (id) {
            case "1min":
                m_name = "1 min";
                m_seconds = 60;
                break;
            case "3min":
                m_name = "3 min";
                m_seconds = 60 * 3;
                break;
            case "15min":
                m_name = "15 min";
                m_seconds = 60 * 15;
                break;
            case "30min":
                m_name = "30 min";
                m_seconds = 60 * 30;
                break;
            case "1hour":
                m_name = "1 hour";
                m_seconds = 60 * 60;
                break;
            case "2hour":
                m_name = "2 hour";
                m_seconds = 60 * 60 * 2;
                break;
            case "4hour":
                m_name = "4 hour";
                m_seconds = 60 * 60 * 4;
                break;
            case "6hour":
                m_name = "6 hour";
                m_seconds = 60 * 60 * 6;
                break;
            case "8hour":
                m_name = "8 hour";
                m_seconds = 60 * 60 * 8;
                break;
            case "12hour":
                m_name = "12 hour";
                m_seconds = 60 * 60 * 12;
                break;
            case "1day":
                m_name = "1 day";
                m_seconds = 60 * 60 * 24;
                break;
            case "1week":
                m_name = "1 week";
                m_seconds = 60 * 60 * 24 * 7;
                break;

        }
    }
}
