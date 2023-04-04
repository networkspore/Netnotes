package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetworksData {

    private File m_networksFile;
    private ArrayList<Network> m_networks;

    public NetworksData(File networksFile) throws Exception {

        m_networksFile = networksFile;
        m_networks = new ArrayList<>();
        if (networksFile.isFile()) {

            String fileHexString = Files.readString(networksFile.toPath());
            byte[] bytes = Hex.decodeHex(fileHexString);
            String jsonArrayString = new String(bytes, StandardCharsets.UTF_8);
            JsonArray jsonArray = new JsonParser().parse(jsonArrayString).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject arrayObject = element.getAsJsonObject();
                Network savedNetwork = new Network(arrayObject);
                m_networks.add(savedNetwork);
            }

        }
    }

    public ArrayList<Network> getNetworks() {
        return m_networks;
    }

    public void save() throws Exception {

        if (m_networks.size() > 0) {
            JsonArray jsonArray = new JsonArray();

            for (Network network : m_networks) {
                JsonObject jsonObj = network.getJsonObject();
                jsonArray.add(jsonObj);

            }
            String jsonArrayString = jsonArray.toString();

            byte[] bytes = jsonArrayString.getBytes(StandardCharsets.UTF_8);
            String fileHexString = Hex.encodeHexString(bytes);

            Files.writeString(m_networksFile.toPath(), fileHexString);
        } else {
            if (m_networksFile.isFile()) {
                Files.delete(m_networksFile.toPath());
            }
        }

    }
}
