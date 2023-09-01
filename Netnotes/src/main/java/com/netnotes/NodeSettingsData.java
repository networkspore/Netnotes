package com.netnotes;

import java.io.File;

public class NodeSettingsData {

    public static class SettingsMode {

        public final static String ADVANCED = "Advanced";
        public final static String BASIC = "Basic";
    }

    public static class DigestAccess {

        public final static String LOCAL = "Local Only";
        public final static String ALL = "All";
    }

    public static class BlockchainMode {

        public final static String PRUNED = "Pruned (Fast-Sync)";
        public final static String RECENT_ONLY = "Recent Only";
        public final static String FULL = "Full";
    }

    private File m_appDir;
    private String m_settingsMode = SettingsMode.BASIC;
    private String m_blockchainMode = BlockchainMode.RECENT_ONLY;
    private String m_digestAccess = DigestAccess.LOCAL;
    private String m_apiKeyHash = null;

    public NodeSettingsData(String apiKeyHash, File appDir) {
        this(SettingsMode.BASIC, DigestAccess.LOCAL, BlockchainMode.RECENT_ONLY, apiKeyHash, appDir);
    }

    public NodeSettingsData(String settingsMode, String digestAccess, String blockchainMode, String apiKeyHash, File appDir) {
        m_settingsMode = settingsMode;
        m_digestAccess = digestAccess;
        m_blockchainMode = blockchainMode;
        m_apiKeyHash = apiKeyHash;
        m_appDir = appDir;
    }

    public File getAppDir() {
        return m_appDir;
    }

    public String getSettingsMode() {
        return m_settingsMode;
    }

    public void setSettingsMode(String settingsMode) {
        m_settingsMode = settingsMode;
        save();
    }

    public String getDigestAccess() {
        return m_digestAccess;
    }

    public void setDigestAccess(String digestAccess) {
        m_digestAccess = digestAccess;
        save();
    }

    public void blockchainMode(String blockchainMode) {
        m_blockchainMode = blockchainMode;
        save();
    }

    public String blockchainMode() {
        return m_blockchainMode;
    }

    public void setApiKeyHash(String hash) {
        m_apiKeyHash = hash;
        save();
    }

    public String getApiKeyHash() {
        return m_apiKeyHash;
    }

    public void save() {

    }

    /*
      public Scene getSettingsScene(SimpleObjectProperty<NamedNodeUrl> namedNode, Button okBtn, Stage stage) {
        final String headingString = "Settings";
        final String nodeId = namedNode.get().getId();

        SimpleStringProperty apiKeySting = new SimpleStringProperty(namedNode.get().getApiKey());

        stage.titleProperty().bind(Bindings.concat(headingString, " - ", namedNode.asString(), " - ", ErgoNodes.NAME));

        Image icon = ErgoNodes.getSmallAppIcon();
        double defaultRowHeight = 40;
        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        okBtn.setText("Ok");
        okBtn.setPadding(new Insets(5, 15, 5, 15));

        HBox titleBox = App.createTopBar(icon, maximizeBtn, closeBtn, stage);
        Text headingText = new Text(headingString);
        headingText.setFont(App.txtFont);
        headingText.setFill(Color.WHITE);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(titleBox, headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(defaultRowHeight);

        Text settingsText = new Text(headingString);
        settingsText.setFill(App.txtColor);
        settingsText.setFont(App.txtFont);

        HBox settingsBox = new HBox(settingsText);
        settingsBox.setAlignment(Pos.CENTER_LEFT);
        settingsBox.setMinHeight(40);;
        settingsBox.setId("headingBox");
        settingsBox.setPadding(new Insets(0, 0, 0, 15));

        HBox okBox = new HBox(okBtn);
        okBox.setMinHeight(35);
        okBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(okBox, Priority.ALWAYS);

        Text apiKeyText = new Text(String.format("%-13s", "API Key"));
        apiKeyText.setFill(App.txtColor);
        apiKeyText.setFont(App.txtFont);

        Button apiKeyBtn = new Button(apiKeySting.get() != null && apiKeySting.get() != "" ? "(Click to update)" : "(Click to set)");
        apiKeyBtn.setId("rowBtn");
        apiKeyBtn.setOnAction(e -> {

        });

        HBox apiKeyBox = new HBox(apiKeyText, apiKeyBtn);
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);
        apiKeyBox.setPadding(new Insets(0, 0, 0, 15));
        HBox.setHgrow(apiKeyBox, Priority.ALWAYS);

        apiKeyBtn.prefWidthProperty().addListener((obs, oldval, newVal) -> {

        });

        VBox settingsPaddingBox = new VBox(apiKeyBox);

        VBox advPaddingBox = new VBox();

        VBox bodyBox = new VBox(settingsPaddingBox, advPaddingBox, okBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);

        VBox layoutBox = new VBox(headerBox, bodyPaddingBox, footerBox);
        Scene settingsScene = new Scene(layoutBox, m_settingsStageWidth, m_settingsStageHeight);
        settingsScene.getStylesheets().add("/css/startWindow.css");

        Runnable closeStage = () -> {
            stage.close();
            m_settingsStage = null;
        };

        closeBtn.setOnAction(e -> closeStage.run());
        stage.setOnCloseRequest(e -> closeStage.run());

        Runnable updateNamedNode = () -> {

            //String id, String name, String ip, int port, String apiKey, NetworkType networkType
            namedNode.set(new NamedNodeUrl(nodeId, nameField.getText(), "127.0.0.1", Integer.parseInt(portField.getText()), apiKeySting.get(), NetworkType.MAINNET));
        };

        apiKeySting.addListener((obs, oldVal, newval) -> {
            updateNamedNode.run();
        });

        namedNode.addListener((obs, oldVal, newVal) -> {
            apiKeyBtn.setText(namedNode.get().getApiKey() != null && namedNode.get().getApiKey() != "" ? "(Click to update)" : "(Click to set)");
        });

        return settingsScene;
    }
     */
}
