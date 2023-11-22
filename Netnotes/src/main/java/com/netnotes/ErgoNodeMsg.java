package com.netnotes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;


public class ErgoNodeMsg {

    private final static DateTimeFormatter TIME_FORMATER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final static String NEW_HEIGHT = ". New height ";
    private final static String SYNC_MSG = "Send message MessageSpec(65";
    private final static String REQUEST_DATA = "Send message MessageSpec(22";

    public class MsgTypes {

        public final static String SYNC_MSG = "Syncing";
        public final static String REQUEST_DATA = "Polling";
        public final static String NEW_HEIGHT = "New height";
    }
    private String m_line = "";

    private String m_id = FriendlyId.createFriendlyId();
    private String m_type = "";

    private String m_timeStamp = "";

    private String m_typeString = "";
    private String m_source = "";
    private String m_sourceCtx = "";
    private String m_body = "";
    private long m_height = -1;

    private Exception m_exception = null;

    public ErgoNodeMsg(String line) {
     
        m_line = line;
        try {
            parseLine();
        } catch (Exception e) {
            m_exception = e;
        }
    }

    public void parseLine() throws Exception {

        int firstSpace = m_line.indexOf(" ");
        int firstDoubleSpace = m_line.indexOf("  ", firstSpace);
        int nextSpace = m_line.indexOf(" ", firstDoubleSpace + 2);
        int spaceDashSpace = m_line.indexOf(" - ", nextSpace);

        m_timeStamp = m_line.substring(0, firstSpace);

        m_typeString = m_line.substring(firstSpace + 1, firstDoubleSpace);
        m_source = m_line.substring(firstDoubleSpace + 2, nextSpace);
        m_sourceCtx = m_line.substring(nextSpace + 1, spaceDashSpace);

        m_body = m_line.substring(spaceDashSpace + 3);
        //LocalDateTime ldt = null;

        int syncMsgIndex = m_body.indexOf(SYNC_MSG);

        if (syncMsgIndex != -1) {
            m_type = MsgTypes.SYNC_MSG;
        } else {
            int requestDataIndex = m_body.indexOf(REQUEST_DATA);
            if (requestDataIndex != -1) {
                m_type = MsgTypes.REQUEST_DATA;
            } else {
                int newHeightIndex = m_body.indexOf(NEW_HEIGHT);
                if (newHeightIndex > 90) {
                    m_type = MsgTypes.NEW_HEIGHT;
                    int heightEndComma = m_body.indexOf(",", newHeightIndex + NEW_HEIGHT.length());
                    if (heightEndComma > newHeightIndex) {
                        String heightString = m_body.substring(newHeightIndex + NEW_HEIGHT.length(), heightEndComma);
                        m_height = Long.parseLong(heightString);
                    }
                }
            }
        }

    }

    public String getLine() {
        return m_line;
    }

    public String getId() {
        return m_id;
    }

    public String getType() {
        return m_type;
    }

    public String getTimeStamp() {
        return m_timeStamp;
    }

    public String getDateTimeString() {
        return Utils.formatDateTimeString(getLocalDateTime());

    }

    public LocalTime getLocalTime() {
        return LocalTime.parse(m_timeStamp, TIME_FORMATER);
    }

    public LocalDateTime getLocalDateTime() {
        return getLocalTime().atDate(LocalDate.now());
    }

    public String getBody() {
        return m_body;
    }

    public String gettypeString() {
        return m_typeString;
    }

    public String getSource() {
        return m_source;
    }

    public String getSourceContext() {
        return m_sourceCtx;
    }

    public long getHeight() {
        return m_height;
    }

    public Exception getException() {
        return m_exception;
    }
}
