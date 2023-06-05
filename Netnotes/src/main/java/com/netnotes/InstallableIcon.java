package com.netnotes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class InstallableIcon extends IconButton {

    public static double DEFAULT_SCENE_WIDTH = 500;
    public static double DEFAULT_SCENE_HEIGHT = 500;
    public static Image INSTALLED_IMAGE = new Image("/assets/installed.png");

    private String m_networkId;
    private boolean m_installed;
    private Stage m_stage;
    private String m_description;
    private String m_summary;
    private double m_sceneWidth = DEFAULT_SCENE_WIDTH;
    private double m_sceneHeight = DEFAULT_SCENE_HEIGHT;
    private NetworksData m_networksData;

    public InstallableIcon(NoteInterface noteInterface) {
        super(noteInterface.getButton().getIcon(), noteInterface.getButton().getName(), IconStyle.ROW);

        setNetworkId(noteInterface.getNetworkId());
        m_installed = true;
        m_networksData = noteInterface.getNetworksData();
        enableActions();
    }

    public InstallableIcon(NetworksData networksData, String networkId) {
        super();
        m_installed = false;
        m_networksData = networksData;

        setNetworkId(networkId);
        setIconStyle(IconStyle.ICON);
        setId("iconBtn");
        setFont(App.txtFont);
        enableActions();

    }

    @Override
    public void open() {

        if (m_stage == null) {
            m_stage = new Stage();
            setScene();

        }
        m_stage.show();
        super.open();

    }

    public void setNetworkId(String networkId) {
        m_networkId = networkId;
        switch (m_networkId) {
            case "ERGO_EXPLORER":
                setIcon(ErgoExplorer.getAppIcon());
                setName(ErgoExplorer.NAME);
                setDescription(ErgoExplorer.DESCRIPTION);
                setSummary(ErgoExplorer.SUMMARY);
                break;
            case "ERGO_WALLET":
                setIcon(ErgoWallet.getAppIcon());
                setName(ErgoWallet.NAME);
                setDescription(ErgoWallet.DESCRIPTION);
                setSummary(ErgoWallet.SUMMARY);
                break;
            case "ERGO_NETWORK":
                setIcon(ErgoNetwork.getAppIcon());
                setName(ErgoNetwork.NAME);
                setDescription(ErgoNetwork.DESCRIPTION);
                setSummary(ErgoNetwork.SUMMARY);
                break;
            case "KUCOIN_EXCHANGE":
                setIcon(KucoinExchange.getAppIcon());
                setName(KucoinExchange.NAME);
                setDescription(KucoinExchange.DESCRIPTION);
                setSummary(KucoinExchange.SUMMARY);
                break;
        }

    }

    private void setScene() {
        m_stage.setScene(m_installed ? uninstallScene() : installScene());
    }

    public Scene installScene() {
        String topTitle = getName() + " - Install";
        m_stage.setTitle(topTitle);
        m_stage.getIcons().add(getIcon());
        m_stage.setResizable(false);
        m_stage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(getIcon(), topTitle, closeBtn, m_stage);
        closeBtn.setOnAction(e -> close());

        Button imageButton = App.createImageButton(getIcon(), getName());
        imageButton.setGraphicTextGap(15);
        HBox logoBox = new HBox(imageButton);
        logoBox.setPadding(new Insets(15));
        logoBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(logoBox, Priority.ALWAYS);

        Text description = new Text(m_description);
        description.setFill(App.txtColor);
        description.setFont(App.txtFont);

        Text summary = new Text("\n\n" + m_summary);
        summary.setFill(App.altColor);
        summary.setFont(App.txtFont);

        TextFlow bodyText = new TextFlow(description, summary);

        HBox bodyHBox = new HBox(bodyText);
        bodyHBox.setPadding(new Insets(0, 30, 30, 30));
        HBox.setHgrow(bodyHBox, Priority.ALWAYS);
        HBox.setHgrow(bodyText, Priority.ALWAYS);

        Region footerRegionVspacer = new Region();
        VBox.setVgrow(footerRegionVspacer, Priority.ALWAYS);

        Region leftfooterRegion = new Region();
        HBox.setHgrow(leftfooterRegion, Priority.ALWAYS);

        Button footerBtn = new Button("Install");
        footerBtn.setFont(App.titleFont);
        footerBtn.setId("menuBarBtn");
        footerBtn.setOnAction(e -> {
            switch (m_networkId) {
                case "ERGO_EXPLORER":
                    m_networksData.addNoteInterface(new ErgoExplorer(m_networksData));
                    break;
                case "ERGO_WALLET":
                    m_networksData.addNoteInterface(new ErgoWallet(m_networksData));
                    break;
                case "ERGO_NETWORK":
                    m_networksData.addNoteInterface(new ErgoNetwork(m_networksData));
                    break;
                case "KUCOIN_EXCHANGE":
                    m_networksData.addNoteInterface(new KucoinExchange(m_networksData));
                    break;
            }
        });

        HBox footerHBox = new HBox(leftfooterRegion, footerBtn);
        footerHBox.setPadding(new Insets(0, 40, 30, 40));

        VBox layoutVBox = new VBox(titleBox, logoBox, bodyHBox, footerRegionVspacer, footerHBox);
        Scene installScene = new Scene(layoutVBox, m_sceneWidth, m_sceneHeight);
        description.wrappingWidthProperty().bind(bodyText.widthProperty());

        installScene.getStylesheets().add("/css/startWindow.css");
        return installScene;
    }

    public Scene uninstallScene() {
        String topTitle = getName() + " - Uninstall";
        m_stage.setTitle(topTitle);
        m_stage.getIcons().add(getIcon());
        m_stage.setResizable(false);
        m_stage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(getIcon(), topTitle, closeBtn, m_stage);
        closeBtn.setOnAction(e -> close());

        Button imageButton = App.createImageButton(getIcon(), getName());
        imageButton.setGraphicTextGap(15);

        HBox logoBox = new HBox(imageButton);
        logoBox.setPadding(new Insets(15));
        logoBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(logoBox, Priority.ALWAYS);

        Text description = new Text(m_description);
        description.setFont(App.txtFont);
        description.setFill(App.txtColor);

        Text summary = new Text("\n\n" + m_summary);
        summary.setFill(App.altColor);
        summary.setFont(App.txtFont);

        TextFlow bodyText = new TextFlow(description, summary);

        HBox bodyHBox = new HBox(bodyText);
        bodyHBox.setPadding(new Insets(30, 40, 30, 40));
        HBox.setHgrow(bodyHBox, Priority.ALWAYS);
        HBox.setHgrow(bodyText, Priority.ALWAYS);

        Region footerRegionVspacer = new Region();
        VBox.setVgrow(footerRegionVspacer, Priority.ALWAYS);

        Region leftfooterRegion = new Region();
        HBox.setHgrow(leftfooterRegion, Priority.ALWAYS);

        Button footerBtn = new Button("Uninstall");
        footerBtn.setFont(App.titleFont);
        footerBtn.setId("menuBarBtn");
        footerBtn.setOnAction(e -> {
            m_networksData.removeNoteInterface(m_networkId);
            close();
        });

        HBox footerHBox = new HBox(leftfooterRegion, footerBtn);
        footerHBox.setPadding(new Insets(0, 40, 30, 40));

        VBox layoutVBox = new VBox(titleBox, logoBox, bodyHBox, footerRegionVspacer, footerHBox);

        Scene installScene = new Scene(layoutVBox, m_sceneWidth, m_sceneHeight);
        installScene.getStylesheets().add("/css/startWindow.css");
        return installScene;
    }

    @Override
    public void close() {
        if (m_stage != null) {
            m_stage.close();
            m_stage = null;
            super.close();
        }
    }

    public boolean getInstalled() {
        return m_installed;
    }

    public void setInstalled(boolean installed) {
        m_installed = installed;
        if (isOpen()) {
            setScene();
        }
        setIconStyle(m_installed ? IconStyle.ROW : IconStyle.ICON);
    }

    public void setDescription(String description) {
        m_description = description;
    }

    public String getDescription() {
        return m_description;
    }

    public void setSummary(String summary) {
        m_summary = summary;
    }

    public String getSummary() {
        return m_summary;
    }
}
