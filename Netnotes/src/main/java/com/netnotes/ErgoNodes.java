package com.netnotes;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.text.TextAlignment;

public class ErgoNodes extends Network implements NoteInterface {

    public final static String NAME = "Ergo Nodes";
    public final static String DESCRIPTION = "Ergo Nodes is the gateway to yoru wallet, node, explorer and token manager on the Ergo Network";
    public final static String NETWORK_ID = "ERGO_NODES";
    public final static String SUMMARY = "";

    public final static int MAINNET_PORT = 9053;
    public final static int TESTNET_PORT = 9052;
    public final static int EXTERNAL_PORT = 9030;

    private String m_explorerId = null;

    private String m_mainnetNodeId = null;

    private ArrayList<ErgoNodeData> m_nodesDataList = new ArrayList<>();

    public ErgoNodes(ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);

    }

    public ErgoNodes(JsonObject jsonObject, ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);

    }

    public static Image getAppIcon() {
        return new Image("/assets/ergoNodes-100.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergoNodes-30.png");
    }

    @Override
    public void open() {
        /*
        if (nodesJson != null) {
            JsonElement nodesElement = ergoNetworkJson.get("nodes");
            JsonElement mainnetNodeIdElement = ergoNetworkJson.get("mainnetNodeId");
            JsonElement supportedCurrenciesElement = ergoNetworkJson.get("supportedCurrencies");

            m_mainnetNodeId = mainnetNodeIdElement == null ? null : mainnetNodeIdElement.getAsString();

            if (nodesElement != null && nodesElement.isJsonArray()) {
                JsonArray nodesArray = nodesElement.getAsJsonArray();

                for (JsonElement clientElement : nodesArray) {
                    if (clientElement.isJsonObject()) {
                        m_nodesDataList.add(new ErgoNodeData(clientElement.getAsJsonObject(), this));
                    }
                }
            } */
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");
        JsonElement networkTypeElement = note.get("networkType");
        JsonElement nodeIdElement = note.get("nodeId");
        if (subjecElement != null) {
            String subject = subjecElement.getAsString();
            switch (subject) {
                case "GET_CLIENT":

                    String nodeId = nodeIdElement == null ? null : nodeIdElement.getAsString();
                    String networkType = networkTypeElement == null ? null : networkTypeElement.toString();

                    if (nodeId == null) {
                        if (m_mainnetNodeId == null) {
                            return false;
                        } else {
                            ErgoNodeData nodeData = getErgoNodeData(m_mainnetNodeId);
                            if (nodeData != null) {
                                if (networkType != null) {
                                    if (nodeData.getNetworkTypeString().equals(networkType)) {
                                        return nodeData.getClient(onSucceeded, onFailed);
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return nodeData.getClient(onSucceeded, onFailed);
                                }
                            } else {
                                return false;
                            }

                        }
                    } else {
                        ErgoNodeData nodeData = getErgoNodeData(nodeId);
                        if (nodeData != null) {
                            if (networkType == null) {
                                return nodeData.getClient(onSucceeded, onFailed);
                            } else {
                                if (nodeData.getNetworkTypeString().equals(networkType)) {
                                    return nodeData.getClient(onSucceeded, onFailed);
                                } else {
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }

                    }

                // NetworkType.MAINNET.toString().equals(networkType);
            }
        }

        return false;
    }

    private ErgoNodeData getErgoNodeData(String networkId) {
        if (networkId == null) {
            return null;
        }
        for (ErgoNodeData ergoNodeData : m_nodesDataList) {
            if (ergoNodeData.getNetworkId().equals(m_mainnetNodeId)) {
                return ergoNodeData;
            }
        }
        return null;
    }

    public NoteInterface getExplorerInterface() {
        if (m_explorerId != null) {
            return getNetworksData().getNoteInterface(m_explorerId);
        } else {
            return null;
        }
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getText(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        if (iconStyle.equals(IconStyle.ROW)) {
            iconButton.setContentDisplay(ContentDisplay.LEFT);
            iconButton.setImageWidth(30);
        } else {
            iconButton.setContentDisplay(ContentDisplay.TOP);
            iconButton.setTextAlignment(TextAlignment.CENTER);
        }

        return iconButton;
    }

}
