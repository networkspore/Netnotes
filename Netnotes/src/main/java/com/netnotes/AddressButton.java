package com.netnotes;

import scala.util.Try;

import java.time.LocalDateTime;

import org.ergoplatform.appkit.Address;
import org.ergoplatform.appkit.NetworkType;

import com.rfksystems.blake2b.Blake2b;

import com.utils.Utils;

import javafx.application.Platform;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;

import javafx.scene.control.Button;

import javafx.scene.image.Image;

import scorex.util.encode.Base58;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class AddressButton extends Button {

    public static String DEFAULT_TEXT = "Enter address";

    private SimpleObjectProperty<Address> m_address = new SimpleObjectProperty<>(null);
    private NetworkType m_networkType = NetworkType.MAINNET;
    // private boolean m_valid = false;
    private boolean m_ergValid = false;
    private String m_addressType;
    private String m_name = DEFAULT_TEXT;

    private SimpleObjectProperty<Image> m_imgBuffer = new SimpleObjectProperty<Image>(null);

    private SimpleObjectProperty<LocalDateTime> m_LastUpdated = new SimpleObjectProperty<LocalDateTime>(LocalDateTime.now());

    //private File logFile;
    private byte[] m_addressBytes;
    private SimpleStringProperty m_addressString = new SimpleStringProperty("");

//String explorerId, NetworksData networksData
    public AddressButton(String addressString, NetworkType networkType) {
        super();

        setPadding(new Insets(0));
        m_networkType = networkType;
        // m_explorerId = explorerId;
        //  m_networksData = networksData;

        //   setIconStyle(IconStyle.ROW);
        //  HBox.setHgrow(this, Priority.ALWAYS);
        setPrefHeight(40);
        // setPrefWidth(width);
        // setImageWidth(150);

        // setIconStyle(IconStyle.ROW);
        // 
        setText(getButtonText());

        setAddressByString(addressString, e -> {
            //success?
        });

        updateBufferedImage();

    }

    public String getButtonText() {
        return "> " + m_name + "\n  " + m_addressString;
    }

    /*
    public NoteInterface getExplorerInterface() {
        return m_explorerId == null ? null : m_networksData.getNoteInterface(m_explorerId);
    } */
    public void setAddressByString(String addressString, EventHandler<WorkerStateEvent> onSucceeded) {

        if (addressString.length() > 5) {
            //^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$

            checkAddress(addressString, successEvent -> {
                WorkerStateEvent workerEvent = successEvent;
                Object workerObject = workerEvent.getSource().getValue();
                if (workerObject != null && workerObject instanceof byte[]) {

                    byte[] addressBytes = (byte[]) workerObject;

                    Utils.returnObject(updateAddress(addressBytes, addressString), onSucceeded, null);

                } else {
                    setDefault("");
                }
            }, failure -> {
                setDefault("");
            });

        } else {
            setDefault("");
        }

    }

    private boolean updateAddress(byte[] addressBytes, String addressString) {

        NetworkType networkType = null;
        String type = null;

        switch (addressBytes[0]) {
            case 0x01:
                type = "P2PK";
                networkType = NetworkType.MAINNET;
                break;
            case 0x02:
                type = "P2SH";
                networkType = NetworkType.MAINNET;
                break;
            case 0x03:
                type = "P2S";
                networkType = NetworkType.MAINNET;
                break;
            case 0x11:
                type = "P2PK";
                networkType = NetworkType.TESTNET;
                break;
            case 0x12:
                type = "P2SH";
                networkType = NetworkType.TESTNET;
                break;
            case 0x13:
                type = "P2S";
                networkType = NetworkType.TESTNET;
                break;
        }
        if (networkType != null && type != null) {
            m_addressBytes = addressBytes;

            if (networkType != m_networkType) {
                m_name = ErgoNetwork.NAME + " - (" + networkType + ") [" + type + "]";
                setText(getButtonText());
                m_addressString.set("");
                updateBufferedImage();
                return false;
            }

            m_addressType = type;
            m_ergValid = true;

            m_name = ErgoNetwork.NAME + " - (" + networkType + ") [" + type + "]";
            setText(getButtonText());

            m_addressString.set(addressString);
            m_address.set(Address.create(addressString));
            updateBufferedImage();
            return true;
        }

        setDefault("");
        return false;

    }

    public boolean getAddressValid() {
        return m_ergValid;
    }

    public String getAddressTypeString() {
        return m_addressType;
    }

    public void checkAddress(String addressString, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<byte[]> task = new Task<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                Try<byte[]> bytes = Base58.decode(addressString);

                byte[] addressBytes = bytes.get();

                byte[] checksumBytes = new byte[]{addressBytes[addressBytes.length - 4], addressBytes[addressBytes.length - 3], addressBytes[addressBytes.length - 2], addressBytes[addressBytes.length - 1]};

                byte[] testBytes = new byte[addressBytes.length - 4];

                for (int i = 0; i < addressBytes.length - 4; i++) {
                    testBytes[i] = addressBytes[i];
                }

                byte[] hashBytes = Utils.digestBytesToBytes(testBytes, Blake2b.BLAKE2_B_256);

                byte[] resultBytes = new byte[]{hashBytes[0], hashBytes[1], hashBytes[2], hashBytes[3]};

                boolean valid = (checksumBytes[0] == resultBytes[0]
                        && checksumBytes[1] == resultBytes[1]
                        && checksumBytes[2] == resultBytes[2]
                        && checksumBytes[3] == resultBytes[3]);

                return valid ? addressBytes : null;
            }
        };

        task.setOnSucceeded(onSucceeded);

        task.setOnFailed(onFailed);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void setDefault(String addressString) {
        m_addressBytes = null;
        m_ergValid = false;
        m_name = DEFAULT_TEXT;
        setText(getButtonText());
        m_addressString.set(addressString);
        updateBufferedImage();
    }

    public byte[] getAddressBytes() {
        return m_addressBytes;
    }

    public SimpleObjectProperty<Address> addressProperty() {
        return m_address;
    }

    public Address getAddress() {
        return m_address.get() == null ? null : m_address.get();
    }

    public String getAddressString() {
        return getAddress() == null ? "" : getAddress().toString();
    }

    public String getAddressMinimal(int show) {
        if (m_address.get() == null) {
            return "";
        }

        String adr = m_address.get().toString();
        int len = adr.length();

        return (show * 2) > len ? adr : adr.substring(0, show) + "..." + adr.substring(len - show, len);
    }

    public SimpleObjectProperty<LocalDateTime> getLastUpdatedObject() {
        return m_LastUpdated;
    }

    public void setLastUpdatedNow() {

        //   DateTimeFormatter formater = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss.SSSSS a");
        LocalDateTime now = LocalDateTime.now();

        m_LastUpdated.set(now);

    }

    public Image getUnknownUnitImage() {
        return new Image("/assets/unknown-unit.png");
    }

    public Image getUnitImage() {
        return new Image("/assets/unitErgo.png");
    }

    public NetworkType getNetworkType() {
        return m_address.get() == null ? null : m_address.get().getNetworkType();
    }

    public void updateBufferedImage() {

        int height = 40;
        int width = 80;

        //    java.awt.Font font = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 30);
        java.awt.Font smallFont = new java.awt.Font("SANS-SERIF", java.awt.Font.PLAIN, 12);

        // BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage unitImage = m_ergValid ? SwingFXUtils.fromFXImage(getUnitImage(), null) : SwingFXUtils.fromFXImage(getUnknownUnitImage(), null);
        //  adrBuchImg.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.drawImage(unitImage, 0, (height / 2) - (unitImage.getHeight() / 2), unitImage.getWidth(), unitImage.getHeight(), null);

        g2d.setFont(smallFont);
        g2d.setColor(java.awt.Color.WHITE);

        g2d.dispose();

        setImageBuffer(SwingFXUtils.toFXImage(img, null));
    }

    private void setImageBuffer(Image image) {
        m_imgBuffer.set(image == null ? null : image);

        Platform.runLater(() -> setGraphic(m_imgBuffer.get() == null ? null : IconButton.getIconView(m_imgBuffer.get(), m_imgBuffer.get().getWidth())));
        Platform.runLater(() -> setLastUpdatedNow());
    }

    @Override
    public String toString() {;
        return getAddressString();
    }
}
