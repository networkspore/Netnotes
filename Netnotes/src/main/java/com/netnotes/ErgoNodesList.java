package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;

import com.utils.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoNodesList {

    //options
    public final static String DEFAULT = "DEFAULT";
    public final static String PUBLIC = "PUBLIC";
    public final static String CUSTOM = "CUSTOM";

    private File logFile = new File("ergoNodesList-log.txt");
    private SimpleStringProperty m_defaultId = new SimpleStringProperty(null);
    private ArrayList<ErgoNodeData> m_dataList = new ArrayList<>();
    private ErgoNodes m_ergoNodes;
    private double MIN_ADD_STAGE_WIDTH = 400;
    private double MIN_ADD_STAGE_HEIGHT = 400;
    private double DEFAULT_ADD_STAGE_WIDTH = 700;
    private double DEFAULT_ADD_STAGE_HEIGHT = 400;
    private double m_addStageWidth = DEFAULT_ADD_STAGE_WIDTH - 100;
    private double m_addStageHeight = DEFAULT_ADD_STAGE_HEIGHT;
    private SimpleObjectProperty<LocalDateTime> m_doGridUpdate = new SimpleObjectProperty<LocalDateTime>(null);

    private JsonObject m_nodeListOptions = null;

    public ErgoNodesList(SecretKey secretKey, ErgoNodes ergoNodes) {
        m_ergoNodes = ergoNodes;
        getFile(secretKey);
    }

    private void getFile(SecretKey secretKey) {
        File dataFile = m_ergoNodes.getDataFile();
        if (dataFile != null && dataFile.isFile()) {
            try {
                JsonObject json = Utils.readJsonFile(secretKey, m_ergoNodes.getDataFile().toPath());
                openJson(json);
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
                    | IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.setHeaderText("Loading Error");
                a.setTitle("Ergo Nodes: Loading Error");
                a.show();
            }
        } else {

            showAddNodeStage();
        }

    }

    private void openJson(JsonObject json) {
        JsonElement nodesElement = json.get("nodes");
        JsonElement defaultIdElement = json.get("defaultId");
        JsonElement nodeListOptionsElement = json.get("nodeListOptions");

        String defaultId = defaultIdElement != null ? defaultIdElement.getAsString() : null;
        m_defaultId.set(defaultId);

        if (nodesElement != null && nodesElement.isJsonArray()) {
            JsonArray jsonArray = nodesElement.getAsJsonArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement nodeElement = jsonArray.get(i);

                if (nodeElement != null && nodeElement.isJsonObject()) {
                    m_dataList.add(new ErgoNodeData(this, nodeElement.getAsJsonObject()));
                }
            }
        }

        if (nodeListOptionsElement != null && nodeListOptionsElement.isJsonObject()) {
            m_nodeListOptions = nodeListOptionsElement.getAsJsonObject();
        }
    }

    public JsonArray getNodesJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < m_dataList.size(); i++) {
            ErgoNodeData ergNodeData = m_dataList.get(i);

            jsonArray.add(ergNodeData.getJsonObject());
        }

        return jsonArray;
    }

    public JsonObject getDataObject() {
        JsonObject json = new JsonObject();
        String defaultId = m_defaultId.get();
        if (defaultId != null) {
            json.addProperty("defaultId", defaultId);
        }
        json.add("nodes", getNodesJsonArray());
        return json;
    }

    public JsonObject getAddStageJson() {
        JsonObject json = new JsonObject();
        json.addProperty("width", m_addStageWidth);
        json.addProperty("height", m_addStageHeight);
        return json;
    }

    public void save() {
        JsonObject fileJson = getDataObject();
        fileJson.add("addStage", getAddStageJson());
        String jsonString = fileJson.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {
            Utils.writeEncryptedString(m_ergoNodes.getNetworksData().appKeyProperty().get(), m_ergoNodes.getDataFile(), jsonString);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
                | IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\n" + e.toString());
            } catch (IOException e1) {

            }
        }
    }

    public VBox getGridBox(SimpleDoubleProperty width, SimpleDoubleProperty scrollWidth) {
        VBox gridBox = new VBox();

        Runnable updateGrid = () -> {
            gridBox.getChildren().clear();

            int numCells = m_dataList.size();

            for (int i = 0; i < numCells; i++) {
                ErgoNodeData nodeData = m_dataList.get(i);
                HBox rowItem = nodeData.getRowItem();
                rowItem.prefWidthProperty().bind(width.subtract(scrollWidth));
                gridBox.getChildren().add(rowItem);
            }

        };

        updateGrid.run();

        m_doGridUpdate.addListener((obs, oldval, newval) -> updateGrid.run());

        return gridBox;
    }

    public ErgoNodeData getErgoNodeData(String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < m_dataList.size(); i++) {
            ErgoNodeData ergoNodeData = m_dataList.get(i);
            if (ergoNodeData.getId().equals(id)) {
                return ergoNodeData;
            }
        }
        return null;
    }

    public SimpleStringProperty defaultIdProperty() {
        return m_defaultId;
    }

    public void shutdown() {

    }
    private Stage m_addStage = null;

    public void showAddNodeStage() {
        if (m_addStage == null) {
            String friendlyId = FriendlyId.createFriendlyId();

            SimpleStringProperty nodeOption = new SimpleStringProperty(PUBLIC);
            boolean updatesEnabled = true;
            if (m_nodeListOptions == null) {
                updatesEnabled = m_ergoNodes.getNetworksData().getAppData().getUpdates();
            }
            NamedNodesList nodesList = new NamedNodesList(updatesEnabled, m_nodeListOptions);

            //private
            SimpleObjectProperty<NetworkType> networkTypeOption = new SimpleObjectProperty<NetworkType>(NetworkType.MAINNET);
            SimpleStringProperty clientTypeOption = new SimpleStringProperty(NamedNodeUrl.LIGHT_CLIENT);

            Image icon = ErgoNodes.getSmallAppIcon();
            String name = m_ergoNodes.getName();

            VBox layoutBox = new VBox();

            m_addStage = new Stage();
            m_addStage.getIcons().add(icon);
            m_addStage.setResizable(false);
            m_addStage.initStyle(StageStyle.UNDECORATED);

            double initWidth = 600;
            double initHeight = 500;

            Scene addNodeScene = new Scene(layoutBox, initWidth, initHeight);

            String heading = "Add";
            Button closeBtn = new Button();

            String titleString = heading + " - " + name;
            m_addStage.setTitle(titleString);

            HBox titleBox = App.createTopBar(icon, titleString, closeBtn, m_addStage);

            Text headingText = new Text(heading);
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            HBox headingBox = new HBox(headingText);
            headingBox.prefHeight(40);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 15, 10, 15));
            headingBox.setId("headingBox");

            HBox headingPaddingBox = new HBox(headingBox);

            headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

            VBox headerBox = new VBox(titleBox, headingPaddingBox);

            headerBox.setPadding(new Insets(0, 5, 0, 5));

            SimpleDoubleProperty rowHeight = new SimpleDoubleProperty(40);

            Text nodeTypeText = new Text("Type ");
            nodeTypeText.setFill(App.txtColor);
            nodeTypeText.setFont(App.txtFont);

            MenuButton typeBtn = new MenuButton("Public node (Light client)");
            typeBtn.setAlignment(Pos.CENTER_LEFT);

            MenuItem defaultClientItem = new MenuItem("Public node (Light client)");
            defaultClientItem.setOnAction((e) -> {
                typeBtn.setText(defaultClientItem.getText());
                nodeOption.set(DEFAULT);
            });

            MenuItem configureItem = new MenuItem("Configure");
            configureItem.setOnAction((e) -> {
                typeBtn.setText(configureItem.getText());
                nodeOption.set(CUSTOM);
            });

            typeBtn.getItems().addAll(defaultClientItem, configureItem);

            Text publicNodesText = new Text("Public Nodes ");
            publicNodesText.setFill(App.txtColor);
            publicNodesText.setFont(App.txtFont);

            HBox publicNodesBox = new HBox(publicNodesText);
            publicNodesBox.setAlignment(Pos.CENTER_LEFT);
            publicNodesBox.minHeightProperty().bind(rowHeight);

            HBox nodeTypeBox = new HBox(nodeTypeText, typeBtn);
            nodeTypeBox.setAlignment(Pos.CENTER_LEFT);
            nodeTypeBox.setPadding(new Insets(0));
            nodeTypeBox.minHeightProperty().bind(rowHeight);

            typeBtn.prefWidthProperty().bind(nodeTypeBox.widthProperty().subtract(nodeTypeText.layoutBoundsProperty().get().getWidth()));
            VBox namedNodesGridBox = nodesList.getGridBox();

            ScrollPane namedNodesScroll = new ScrollPane(namedNodesGridBox);
            namedNodesScroll.setId("darkBox");
            namedNodesScroll.setPadding(new Insets(10));

            HBox nodeScrollBox = new HBox(namedNodesScroll);
            nodeScrollBox.setPadding(new Insets(0, 15, 0, 0));

            Text namedNodeText = new Text("Node ");
            namedNodeText.setFill(App.altColor);
            namedNodeText.setFont(App.txtFont);

            Button namedNodeBtn = new Button();
            namedNodeBtn.setId("darkBox");
            namedNodeBtn.setAlignment(Pos.CENTER_LEFT);
            namedNodeBtn.setPadding(new Insets(5, 5, 5, 10));

            Runnable updateSelectedBtn = () -> {
                String selectedId = nodesList.selectedNamedNodeIdProperty().get();
                if (selectedId == null) {
                    namedNodeBtn.setText("(select node)");
                } else {
                    NamedNodeUrl namedNodeUrl = nodesList.getNamedNodeUrl(selectedId);
                    if (namedNodeUrl != null) {
                        namedNodeBtn.setText(namedNodeUrl.getName());
                    } else {
                        namedNodeBtn.setText("(select node)");
                    }
                }
            };

            updateSelectedBtn.run();

            nodesList.selectedNamedNodeIdProperty().addListener((obs, oldval, newVal) -> updateSelectedBtn.run());

            HBox nodesBox = new HBox(namedNodeText, namedNodeBtn);
            nodesBox.setAlignment(Pos.CENTER_LEFT);
            nodesBox.minHeightProperty().bind(rowHeight);
            nodesBox.setPadding(new Insets(10, 0, 0, 0));

            namedNodeBtn.prefWidthProperty().bind(nodesBox.widthProperty().subtract(namedNodeText.layoutBoundsProperty().get().getWidth()).subtract(15));
            namedNodesScroll.prefViewportWidthProperty().bind(nodesBox.widthProperty());

            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);

            namedNodesGridBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                double scrollViewPortHeight = namedNodesScroll.prefViewportHeightProperty().doubleValue();
                double gridBoxHeight = newVal.doubleValue();

                if (gridBoxHeight > scrollViewPortHeight) {
                    scrollWidth.set(40);
                }

            });

            nodesList.gridWidthProperty().bind(nodesBox.widthProperty().subtract(30).subtract(scrollWidth));

            VBox lightClientOptions = new VBox(publicNodesBox, nodeScrollBox, nodesBox);
            lightClientOptions.setPadding(new Insets(0, 0, 5, 15));
            Text nodeName = new Text(String.format("%-10s", "Name"));
            nodeName.setFill(App.txtColor);
            nodeName.setFont(App.txtFont);

            TextField nodeNameField = new TextField("Node #" + friendlyId);
            nodeNameField.setFont(App.txtFont);
            nodeNameField.setId("formField");
            HBox.setHgrow(nodeNameField, Priority.ALWAYS);

            HBox nodeNameBox = new HBox(nodeName, nodeNameField);
            nodeNameBox.setAlignment(Pos.CENTER_LEFT);
            nodeNameBox.minHeightProperty().bind(rowHeight);

            Text networkTypeText = new Text(String.format("%-10s", "Network Type"));
            networkTypeText.setFill(App.txtColor);
            networkTypeText.setFont(App.txtFont);

            MenuButton networkTypeBtn = new MenuButton("MAINNET");
            networkTypeBtn.setFont(App.txtFont);
            networkTypeBtn.setId("formField");
            HBox.setHgrow(networkTypeBtn, Priority.ALWAYS);

            MenuItem mainnetItem = new MenuItem("MAINNET");
            mainnetItem.setOnAction((e) -> {
                networkTypeBtn.setText(mainnetItem.getText());
                networkTypeOption.set(NetworkType.MAINNET);

            });

            MenuItem testnetItem = new MenuItem("TESTNET");
            testnetItem.setOnAction((e) -> {
                networkTypeBtn.setText(testnetItem.getText());
                networkTypeOption.set(NetworkType.TESTNET);

            });

            HBox networkTypeBox = new HBox(networkTypeText, networkTypeBtn);
            networkTypeBox.setAlignment(Pos.CENTER_LEFT);
            networkTypeBox.minHeightProperty().bind(rowHeight);

            Text apiKeyText = new Text(String.format("%-10s", "API Key"));
            apiKeyText.setFill(App.txtColor);
            apiKeyText.setFont(App.txtFont);

            TextField apiKeyField = new TextField("");
            apiKeyField.setFont(App.txtFont);
            apiKeyField.setId("formField");
            HBox.setHgrow(apiKeyField, Priority.ALWAYS);

            HBox apiKeyBox = new HBox(nodeName, nodeNameField);
            apiKeyBox.setAlignment(Pos.CENTER_LEFT);
            apiKeyBox.minHeightProperty().bind(rowHeight);

            Text clientType = new Text(String.format("%-10s", "Client type"));
            clientType.setFill(App.txtColor);
            clientType.setFont(App.txtFont);

            MenuButton clientTypeBtn = new MenuButton("Light client");
            clientTypeBtn.setFont(App.txtFont);
            clientTypeBtn.setId("formField");
            HBox.setHgrow(clientTypeBtn, Priority.ALWAYS);

            MenuItem lightClientItem = new MenuItem("Light client");
            lightClientItem.setOnAction((e) -> {
                clientTypeBtn.setText(lightClientItem.getText());
                clientTypeOption.set(ErgoNodeData.LIGHT_CLIENT);
            });

            HBox clientTypeBox = new HBox(clientType, clientTypeBtn);
            clientTypeBox.setAlignment(Pos.CENTER_LEFT);
            clientTypeBox.minHeightProperty().bind(rowHeight);

            Text nodePortText = new Text(String.format("%-10s", "Port"));
            nodePortText.setFill(App.txtColor);
            nodePortText.setFont(App.txtFont);

            TextField nodePortField = new TextField("9053");
            nodePortField.setId("formField");
            HBox.setHgrow(nodePortField, Priority.ALWAYS);

            HBox nodePortBox = new HBox(nodePortText, nodePortField);
            nodePortBox.setAlignment(Pos.CENTER_LEFT);
            nodePortBox.minHeightProperty().bind(rowHeight);

            Text nodeUrlText = new Text(String.format("%-10s", "URL"));
            nodeUrlText.setFill(App.txtColor);
            nodeUrlText.setFont(App.txtFont);

            TextField nodeUrlField = new TextField("127.0.0.1");
            nodeUrlField.setFont(App.txtFont);
            nodeUrlField.setId("formField");
            HBox.setHgrow(nodeUrlField, Priority.ALWAYS);

            HBox nodeUrlBox = new HBox(nodeUrlText, nodeUrlField);
            nodeUrlBox.setAlignment(Pos.CENTER_LEFT);
            nodeUrlBox.minHeightProperty().bind(rowHeight);

            Region urlSpaceRegion = new Region();
            urlSpaceRegion.setMinHeight(40);

            Region namedNodeBoxSpacer = new Region();
            namedNodeBoxSpacer.setMinHeight(40);

            VBox bodyOptionBox = new VBox(lightClientOptions);
            bodyOptionBox.setId("bodyBox");
            bodyOptionBox.setPadding(new Insets(0, 0, 5, 0));

            VBox customClientOptionsBox = new VBox(nodeNameBox, networkTypeBox, clientTypeBox, nodeUrlBox, nodePortBox);

            Button nextBtn = new Button("Next");
            nextBtn.setPadding(new Insets(5, 15, 5, 15));

            HBox nextBox = new HBox(nextBtn);
            nextBox.setPadding(new Insets(0, 0, 0, 0));
            nextBox.setMinHeight(50);
            nextBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(nextBox, Priority.ALWAYS);

            VBox bodyBox = new VBox(nodeTypeBox, bodyOptionBox, nextBox);
            bodyBox.setId("bodyBox");
            bodyBox.setPadding(new Insets(0, 10, 0, 10));

            VBox bodyPaddingBox = new VBox(bodyBox);
            bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

            Region footerSpacer = new Region();
            footerSpacer.setMinHeight(10);

            VBox footerBox = new VBox(footerSpacer);

            layoutBox.getChildren().addAll(headerBox, bodyPaddingBox, footerBox);

            namedNodesScroll.prefViewportHeightProperty().bind(m_addStage.heightProperty().subtract(headerBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(publicNodesBox.heightProperty()).subtract(nodesBox.heightProperty()));

            addNodeScene.getStylesheets().add("/css/startWindow.css");
            m_addStage.setScene(addNodeScene);
            m_addStage.show();

            nodeOption.addListener((obs, oldVal, newVal) -> {
                switch (newVal) {
                    case DEFAULT:
                        if (bodyOptionBox.getChildren().contains(customClientOptionsBox)) {
                            bodyOptionBox.getChildren().remove(customClientOptionsBox);
                        }
                        if (!bodyOptionBox.getChildren().contains(lightClientOptions)) {
                            bodyOptionBox.getChildren().add(lightClientOptions);
                        }

                        break;
                    case CUSTOM:

                        break;
                }
            });

            Runnable doClose = () -> {
                m_addStage.close();
                m_addStage = null;
            };

            m_ergoNodes.shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
                doClose.run();
            });

            m_addStage.setOnCloseRequest((e) -> doClose.run());

            closeBtn.setOnAction((e) -> doClose.run());
        } else {
            if (m_addStage.isIconified()) {
                m_addStage.setIconified(false);
            }
            m_addStage.show();
            m_addStage.toFront();
        }
    }
}
