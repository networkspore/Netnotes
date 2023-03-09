package com.netnotes;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;
import com.utils.Utils;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import mslinks.ShellLinkHelper;

public class Main {

    public static void main(String[] args) {

        Application.launch(App.class, args);

    }

}
