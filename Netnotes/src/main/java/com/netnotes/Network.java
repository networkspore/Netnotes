package com.netnotes;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import com.google.gson.JsonObject;

public class Network {

    public static class NetworkName {

        public static int ERGO = 1;
    }

    public static class NetworkType {

        public static int MAINNET = 0;
        public static int TESTNET = 1;
    }

    private final String m_uuid;
    private String m_wallet = null;
    private String m_id;
    private int m_name;
    private String m_host = null;
    private int m_type;
    private int m_port;
    private int m_exPort;

    public Network(int networkName, int type) {
        m_uuid = UUID.randomUUID().toString();
        m_id = networkName + m_uuid.substring(0, 5);
        m_name = networkName;
        m_host = null;

        setType(type);
    }

    public Network(JsonObject networkJson) throws Exception {

        String walletValue = networkJson.get("walletFile").getAsString();
        String hostValue = networkJson.get("host").getAsString();

        m_id = networkJson.get("id").getAsString();
        m_name = networkJson.get("name").getAsInt();
        m_uuid = networkJson.get("uuid").getAsString();
        m_wallet = walletValue == "" ? null : walletValue;
        m_type = networkJson.get("type").getAsInt();
        m_host = hostValue.equals("") ? null : hostValue;
        m_port = networkJson.get("port").getAsInt();
        m_exPort = networkJson.get("externalPort").getAsInt();

    }

    public String getUUID() {
        return m_uuid;
    }

    public int getType() {
        return m_type;
    }

    public void setType(int type) {
        m_type = type;

        if (m_name == NetworkName.ERGO) {
            if (m_type == NetworkType.MAINNET) {
                m_port = 9053;
            } else {
                m_port = 9052;
            }
            m_exPort = 9030;
        }
    }

    public void setName(int name) {
        m_name = name;
    }

    public int getName() {
        return m_name;
    }

    public void setId(String id) {
        m_id = id;
    }

    public String getId() {
        return m_id;
    }

    public String getHost() {
        return m_host;
    }

    public void setHost(String host) {
        m_host = host;
    }

    public int getPort() {
        return m_port;
    }

    public void setPort(int port) {
        m_port = port;

    }

    public void setExternalPort(int port) {
        m_port = port;
    }

    public int getExternalPort() {
        return m_port;
    }

    public void setWalletFile(File walletFile) {
        m_wallet = walletFile.getAbsolutePath();
    }

    public File getWalletFile() throws Exception {
        File walletFile = new File(m_wallet);
        return walletFile;
    }

    public boolean isWallet() {
        if (m_wallet == null) {
            return false;
        } else {
            File walletFile = new File(m_wallet);
            return walletFile.isFile();
        }
    }

    public void setUrl(String url) throws MalformedURLException {
        if (url == null) {
            m_host = null;
        } else {
            if (url.equals("")) {
                m_host = null;
            } else {

                char c0 = url.charAt(0);

                URL testURL = new URL(c0 != 'h' ? "http://" + url : url);

                m_host = testURL.getHost();

                int port = testURL.getPort();

                if (port != 80 && port != -1) {

                    setPort(port);

                }

            }

        }

    }

    public JsonObject getJsonObject() throws Exception {
        if (m_wallet != null) {
            String walletValue = m_wallet == null ? "" : m_wallet;
            String hostValue = m_host == null ? "" : m_host.toString();

            JsonObject networkObj = new JsonObject();
            networkObj.addProperty("type", m_type);
            networkObj.addProperty("uuid", m_uuid);
            networkObj.addProperty("wallet", walletValue);
            networkObj.addProperty("name", m_name);
            networkObj.addProperty("host", hostValue);
            networkObj.addProperty("id", m_id);
            networkObj.addProperty("port", m_port);
            networkObj.addProperty("externalPort", m_exPort);

            return networkObj;
        } else {
            return null;
        }
    }

}
