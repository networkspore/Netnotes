package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class NamedNodesList {

    private SimpleStringProperty m_selectedNamedNodeId = new SimpleStringProperty(null);

    private ArrayList<NamedNodeUrl> m_dataList = new ArrayList<>();

    private boolean m_enableGitHubUpdates;

    private SimpleDoubleProperty m_gridWidthProperty = new SimpleDoubleProperty(200);

    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);

    private SimpleObjectProperty<LocalDateTime> m_optionsUpdated = new SimpleObjectProperty<LocalDateTime>(null);

    public NamedNodesList(boolean enableGitHubUpdates, JsonObject settingsObj) {
        if (settingsObj != null) {
            JsonElement getGitHubListElement = settingsObj.get("getGitHubList");

            if (getGitHubListElement != null && getGitHubListElement.isJsonPrimitive()) {
                m_enableGitHubUpdates = getGitHubListElement.getAsBoolean();
            }
        } else {
            NamedNodeUrl defaultNodeUrl = new NamedNodeUrl();
            m_dataList.add(defaultNodeUrl);
            m_selectedNamedNodeId.set(defaultNodeUrl.getId());
            m_enableGitHubUpdates = enableGitHubUpdates;
        }

        if (m_enableGitHubUpdates) {
            getGitHubList();
        }

    }

    public SimpleStringProperty selectedNamedNodeIdProperty() {
        return m_selectedNamedNodeId;
    }

    public void setEnableGitHubUpdates(boolean enable) {
        if (enable) {
            m_enableGitHubUpdates = enable;
            getGitHubList();
        }
        m_enableGitHubUpdates = enable;
        m_optionsUpdated.set(LocalDateTime.now());
    }

    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidthProperty;
    }

    public SimpleObjectProperty<LocalDateTime> doUpdateProperty() {
        return m_doGridUpdate;
    }

    public SimpleObjectProperty<LocalDateTime> optionsUpdatedProperty() {
        return m_optionsUpdated;
    }

    public void getGitHubList() {
        if (m_dataList.size() == 0) {
            NamedNodeUrl defaultNodeUrl = new NamedNodeUrl();
            m_dataList.add(defaultNodeUrl);
            m_selectedNamedNodeId.set(defaultNodeUrl.getId());
            m_doGridUpdate.set(LocalDateTime.now());
        }
    }

    public VBox getGridBox() {
        VBox gridBox = new VBox();

        updateGrid(gridBox);

        m_doGridUpdate.addListener((obs, oldVal, newVal) -> updateGrid(gridBox));

        return gridBox;
    }

    public void updateGrid(VBox gridBox) {
        gridBox.getChildren().clear();

        for (int i = 0; i < m_dataList.size(); i++) {
            NamedNodeUrl namedNode = m_dataList.get(i);
            IconButton namedButton = namedNode.getButton();
            namedButton.prefWidthProperty().bind(m_gridWidthProperty);

            gridBox.getChildren().add(namedButton);
        }
    }

    public JsonObject getJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("getGitHubList", m_enableGitHubUpdates);
        return json;
    }

    public NamedNodeUrl getNamedNodeUrl(String id) {
        if (id != null && m_dataList.size() > 0) {
            for (int i = 0; i < m_dataList.size(); i++) {
                NamedNodeUrl namedNodeUrl = m_dataList.get(i);

                if (namedNodeUrl.getId().equals(id)) {
                    return namedNodeUrl;
                }
            }
        }

        return null;
    }

}
