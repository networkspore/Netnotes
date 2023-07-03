package com.netnotes;

import com.google.gson.JsonObject;

import javafx.beans.value.ChangeListener;

public interface MessageInterface {

    String getId();

    String getName();

    ChangeListener<JsonObject> getSocketChangeListener();

}
