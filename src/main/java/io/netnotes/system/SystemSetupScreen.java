package io.netnotes.system;

import io.netnotes.terminal.*;
import io.netnotes.terminal.components.*;
import io.netnotes.terminal.components.panels.TerminalBorderPanel;
import io.netnotes.terminal.components.panels.TerminalVStack;
import io.netnotes.terminal.input.TerminalTextInput;
import io.netnotes.terminal.menus.*;
import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.daemon.DiscoveredDeviceRegistry.DeviceDescriptorWithCapabilities;
import io.netnotes.engine.io.daemon.IODaemonDetection;
import io.netnotes.engine.io.input.events.keyboardEvents.KeyDownEvent;
import io.netnotes.engine.messaging.NoteMessaging.ItemTypes;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.engine.utils.LoggingHelpers.Log;

import java.util.List;
import java.util.function.Consumer;

/**
 * SystemSetupScreen - Modal setup overlay for FIRST_RUN + post-auth reconfiguration
 * 
 * Flow: Welcome → Detect → Menu → [Installer] → Password → Complete
 * Installer: Nested overlay, returns to setup on completion
 */
public class SystemSetupScreen extends TerminalRenderable implements SystemUIInterface {
    
    private static final int STATE_WELCOME = 20;
    private static final int STATE_DETECTING = 21;
    private static final int STATE_MAIN_MENU = 22;
    private static final int STATE_KEYBOARD_SELECTION = 23;
    private static final int STATE_SOCKET_CONFIG = 24;
    private static final int STATE_INSTALLER = 25;
    private static final int STATE_PASSWORD = 26;
    private static final int STATE_ERROR = 27;
    
    private final TerminalBorderPanel layout;
    private final TerminalLabel headerLabel;
    private final TerminalTextBox statusBox;
    private final MenuNavigator menuNavigator;
    private final TerminalVStack inputStack;
    private final SystemApplication application;
    
    private TerminalTextInput socketInput;
    private IODaemonInstaller installer;
    private PasswordPrompt passwordPrompt;

    
    private final boolean isFirstRun;
    private volatile boolean ioDaemonAvailable = false;
    private volatile String socketPath;
    private volatile List<DeviceDescriptorWithCapabilities> availableKeyboards = null;
    private volatile String errorMessage = null;
    
    private Consumer<Void> onComplete;
    private Consumer<SystemUIInterface> onDisposed;
    
    public SystemSetupScreen(SystemApplication application) {
        super("system-setup");
        this.application = application;
        this.isFirstRun = !application.isAuthenticated();
        String configuredPath = application.getIODaemonSocketPath();
        this.socketPath = configuredPath != null
            ? configuredPath
            : SystemApplication.DEFAULT_IO_DAEMON_SOCKET_PATH;
        
        this.layout = new TerminalBorderPanel("setup-layout");
        this.headerLabel = new TerminalLabel("header", "");
        this.statusBox = new TerminalTextBox("status");
        this.menuNavigator = new MenuNavigator("menu");
        this.inputStack = new TerminalVStack("input");
        
        buildLayout();
        
        stateMachine.addState(isFirstRun ? STATE_WELCOME : STATE_DETECTING);
    }
    
    private void buildLayout() {
        addChild(layout);
        
        TerminalVStack topStack = new TerminalVStack("top");
        topStack.setSpacing(1);
        topStack.setPadding(2);
        topStack.addChild(headerLabel);
        topStack.addChild(statusBox);
        
        layout.setPanel(TerminalBorderPanel.Panel.TOP, topStack);
        layout.setPanel(TerminalBorderPanel.Panel.CENTER, menuNavigator);
        
        menuNavigator.hide();
        inputStack.hide();
    }
    
    @Override
    protected void setupStateTransitions() {
        stateMachine.onStateAdded(STATE_WELCOME, (o,n,b) -> {
            headerLabel.setText("═══ System Setup ═══");
            statusBox.setText(
                "Welcome to NoteNexus!\n\n" +
                "Configure secure input devices for password entry.\n\n" +
                "Press any key to begin..."
            );
            statusBox.show();
            menuNavigator.hide();
            inputStack.hide();
            
            addKeyDownHandler(e -> {
                if (e instanceof KeyDownEvent) {
                    transitionTo(STATE_WELCOME, STATE_DETECTING);
                }
            });
        });
        
        stateMachine.onStateAdded(STATE_DETECTING, (o,n,b) -> {
            headerLabel.setText("═══ Detecting Devices ═══");
            statusBox.setText("Scanning for IODaemon and USB devices...\n\nPlease wait...");
            statusBox.show();
            menuNavigator.hide();
            detectIODaemon();
        });
        
        stateMachine.onStateAdded(STATE_MAIN_MENU, (o,n,b) -> {
            headerLabel.setText("═══ Setup Menu ═══");
            statusBox.setText(buildStatusText());
            statusBox.show();
            menuNavigator.show();
            buildMainMenu();
        });
        
        stateMachine.onStateAdded(STATE_KEYBOARD_SELECTION, (o,n,b) -> {
            headerLabel.setText("═══ Keyboard Selection ═══");
            statusBox.setText("Select a USB keyboard for password entry");
            statusBox.show();
            menuNavigator.show();
            buildKeyboardMenu();
        });
        
        stateMachine.onStateAdded(STATE_SOCKET_CONFIG, (o,n,b) -> {
            headerLabel.setText("═══ Socket Configuration ═══");
            statusBox.setText("Enter IODaemon socket path:\n(Default: " + 
                SystemApplication.DEFAULT_IO_DAEMON_SOCKET_PATH + ")");
            statusBox.show();
            menuNavigator.hide();
            showSocketInput();
        });
        
        stateMachine.onStateRemoved(STATE_SOCKET_CONFIG, (o,n,b) -> {
            hideSocketInput();
        });
        
        stateMachine.onStateAdded(STATE_INSTALLER, (o,n,b) -> {
            showInstaller();
        });
        
        stateMachine.onStateRemoved(STATE_INSTALLER, (o,n,b) -> {
            hideInstaller();
        });
        
        stateMachine.onStateAdded(STATE_PASSWORD, (o,n,b) -> {
            showPasswordPrompt();
        });
        
        stateMachine.onStateRemoved(STATE_PASSWORD, (o,n,b) -> {
            hidePasswordPrompt();
        });
        
        stateMachine.onStateAdded(STATE_ERROR, (o,n,b) -> {
            headerLabel.setText("═══ Error ═══");
            statusBox.setText(errorMessage != null ? errorMessage : "Unknown error");
            statusBox.show();
            menuNavigator.hide();
            
            addKeyDownHandler(e -> {
                if (e instanceof KeyDownEvent) {
                    transitionTo(STATE_ERROR, STATE_MAIN_MENU);
                }
            });
        });
    }
    
    // ===== DETECTION =====
    
    private void detectIODaemon() {
        application.getIoDaemonManager().detect()
            .thenAccept(result -> {
                ioDaemonAvailable = result.isFullyOperational();
                if (ioDaemonAvailable) {
                    discoverKeyboards();
                } else {
                    transitionTo(STATE_DETECTING, STATE_MAIN_MENU);
                }
            })
            .exceptionally(ex -> {
                Log.logError("[Setup] Detection failed: " + ex.getMessage());
                ioDaemonAvailable = false;
                transitionTo(STATE_DETECTING, STATE_MAIN_MENU);
                return null;
            });
    }



    
    private void discoverKeyboards() {
        application.getIoDaemonManager().discoverDevicesOfType(ItemTypes.KEYBOARD)
            .thenAccept(keyboards -> {
                availableKeyboards = keyboards;
                transitionTo(STATE_DETECTING, STATE_MAIN_MENU);
            })
            .exceptionally(ex -> {
                Log.logError("[Setup] Keyboard discovery failed: " + ex.getMessage());
                availableKeyboards = null;
                transitionTo(STATE_DETECTING, STATE_MAIN_MENU);
                return null;
            });
    }
    
    // ===== MENU BUILDING =====
    
    private void buildMainMenu() {
        ContextPath menuPath = application.getContextPath().append("setup-main");
        MenuContext menu = new MenuContext(menuPath, null, null, null);
        
        if (ioDaemonAvailable && availableKeyboards != null && !availableKeyboards.isEmpty()) {
            menu.addItem("keyboard-select", 
                "Configure USB Keyboard", 
                () -> transitionTo(STATE_MAIN_MENU, STATE_KEYBOARD_SELECTION));
        }
        
        if (ioDaemonAvailable) {
            menu.addItem("gui-keyboard", 
                "Use GUI Keyboard", 
                this::configureGUIOnly);
        } else {
            menu.addItem("gui-keyboard", 
                "Continue with GUI Keyboard", 
                this::configureGUIOnly);
        }
        
        if (!ioDaemonAvailable) {
            menu.addSeparator("");
            menu.addItem("install-iodaemon", 
                "Install IODaemon", 
                () -> transitionTo(STATE_MAIN_MENU, STATE_INSTALLER));
        }
        
        if (ioDaemonAvailable) {
            menu.addSeparator("");
            menu.addItem("refresh", 
                "Refresh Device List", 
                () -> transitionTo(STATE_MAIN_MENU, STATE_DETECTING));
        }
        
        menu.addSeparator("");
        menu.addItem("socket-path", 
            "Configure Socket Path: " + socketPath, 
            () -> transitionTo(STATE_MAIN_MENU, STATE_SOCKET_CONFIG));
        
        if (isFirstRun) {
            menu.addSeparator("");
            menu.addItem("continue", 
                "Continue to Password Setup →", 
                () -> transitionTo(STATE_MAIN_MENU, STATE_PASSWORD));
        } else {
            menu.addSeparator("");
            menu.addItem("back", 
                "← Back", 
                this::complete);
        }
        
        menuNavigator.showMenu(menu);
    }
    
    private void buildKeyboardMenu() {
        ContextPath menuPath = application.getContextPath().append("keyboard-selection");
        MenuContext menu = new MenuContext(menuPath, null, null, null);
        
        if (availableKeyboards == null || availableKeyboards.isEmpty()) {
            menu.addInfoItem("no-keyboards", "No keyboards detected");
            menu.addSeparator("");
            menu.addItem("back", "← Back", 
                () -> transitionTo(STATE_KEYBOARD_SELECTION, STATE_MAIN_MENU));
        } else {
            for (DeviceDescriptorWithCapabilities device : availableKeyboards) {
                String deviceId = device.usbDevice().getDeviceId();
                String manufacturer = device.usbDevice().manufacturer;
                String product = device.usbDevice().product;
                
                String displayName = product != null ? product : "USB Keyboard";
                if (manufacturer != null && !manufacturer.isEmpty()) {
                    displayName = manufacturer + " " + displayName;
                }
                
                String badge = device.claimed() ? "IN USE" : null;
                
                menu.addItem(deviceId, displayName, badge, () -> selectKeyboard(deviceId));
            }
            
            menu.addSeparator("");
            menu.addItem("refresh", "↻ Refresh", this::refreshKeyboards);
            menu.addItem("back", "← Back", 
                () -> transitionTo(STATE_KEYBOARD_SELECTION, STATE_MAIN_MENU));
        }
        
        menuNavigator.showMenu(menu);
    }
    
    private String buildStatusText() {
        StringBuilder sb = new StringBuilder();
        
        if (ioDaemonAvailable) {
            sb.append("✓ IODaemon operational\n");
            
            if (availableKeyboards != null && !availableKeyboards.isEmpty()) {
                sb.append(String.format("✓ Found %d USB keyboard(s)\n\n", availableKeyboards.size()));
                sb.append("Hardware-level password protection available.\n");
            } else {
                sb.append("⚠ No USB keyboards detected\n\n");
                sb.append("Connect a USB keyboard for secure input.\n");
            }
        } else {
            sb.append("✗ IODaemon not available\n\n");
            IODaemonDetection.InstallationPaths paths = 
                application.getIoDaemonManager().getInstallationPaths();
            if (paths != null) {
                sb.append("Expected: ").append(paths.binaryPath).append("\n");
            }
            sb.append("\nContinue with GUI keyboard or install IODaemon.\n");
        }
        
        return sb.toString();
    }
    
    // ===== SOCKET CONFIG =====
    
    private void showSocketInput() {
        socketInput = new TerminalTextInput("socket-input", 0, 0, 60);
        socketInput.setText(socketPath);
        socketInput.setOnComplete(this::handleSocketPathComplete);
        socketInput.setOnEscape(text -> transitionTo(STATE_SOCKET_CONFIG, STATE_MAIN_MENU));
        
        inputStack.addChild(socketInput);
        layout.setPanel(TerminalBorderPanel.Panel.CENTER, inputStack);
        inputStack.show();
    }
    
    private void hideSocketInput() {
        if (socketInput != null) {
            inputStack.removeChild(socketInput);
            socketInput.close();
            socketInput = null;
        }
        inputStack.hide();
        layout.setPanel(TerminalBorderPanel.Panel.CENTER, menuNavigator);
    }
    
    private void handleSocketPathComplete(String newPath) {
        if (newPath == null || newPath.trim().isEmpty()) {
            transitionTo(STATE_SOCKET_CONFIG, STATE_MAIN_MENU);
            return;
        }
        
        if (!newPath.startsWith("/")) {
            errorMessage = "Socket path must be absolute (start with /)";
            transitionTo(STATE_SOCKET_CONFIG, STATE_ERROR);
            return;
        }
        
        String oldPath = socketPath;
        socketPath = newPath.trim();
        
        application.getIoDaemonManager().setIODaemonSocketPath(socketPath);
        application.completeBootstrap(application.getClaimedKeyboardId())
            .thenCompose(v -> application.getIoDaemonManager().reconfigureSocketPath(socketPath))
            .thenCompose(v -> application.getIoDaemonManager().detect())
            .thenAccept(result -> {
                if (result.isFullyOperational()) {
                    ioDaemonAvailable = true;
                    discoverKeyboards();
                } else {
                    transitionTo(STATE_SOCKET_CONFIG, STATE_MAIN_MENU);
                }
            })
            .exceptionally(ex -> {
                socketPath = oldPath;
                application.getIoDaemonManager().setIODaemonSocketPath(oldPath);
                application.completeBootstrap(application.getClaimedKeyboardId());
                
                errorMessage = "Failed to apply new path: " + ex.getMessage();
                transitionTo(STATE_SOCKET_CONFIG, STATE_ERROR);
                return null;
            });
    }
    
    // ===== KEYBOARD SELECTION =====
    
    private void selectKeyboard(String deviceId) {
        application.completeBootstrap(deviceId)
            .thenRun(() -> {
                if (isFirstRun) {
                    transitionTo(STATE_KEYBOARD_SELECTION, STATE_PASSWORD);
                } else {
                    transitionTo(STATE_KEYBOARD_SELECTION, STATE_MAIN_MENU);
                }
            })
            .exceptionally(ex -> {
                errorMessage = "Failed to configure keyboard: " + ex.getMessage();
                transitionTo(STATE_KEYBOARD_SELECTION, STATE_ERROR);
                return null;
            });
    }
    
    private void refreshKeyboards() {
        transitionTo(STATE_KEYBOARD_SELECTION, STATE_DETECTING);
    }
    
    private void configureGUIOnly() {
        application.completeBootstrap(null)
            .thenRun(() -> {
                if (isFirstRun) {
                    transitionTo(STATE_MAIN_MENU, STATE_PASSWORD);
                } else {
                    complete();
                }
            })
            .exceptionally(ex -> {
                errorMessage = "Configuration failed: " + ex.getMessage();
                transitionTo(STATE_MAIN_MENU, STATE_ERROR);
                return null;
            });
    }
    
    // ===== INSTALLER =====
    
    private void showInstaller() {
 
        application.getUiExecutor().executeFireAndForget(() -> {
            if (!stateMachine.hasState(STATE_INSTALLER)) {
                return;
            }


            installer = new IODaemonInstaller("installer", application);
            installer.setOnComplete(() -> {
                transitionTo(STATE_INSTALLER, STATE_DETECTING);
            });

            layout.setPanel(TerminalBorderPanel.Panel.CENTER, installer);
            menuNavigator.hide();
        });
     
   
    }
    
    private void hideInstaller() {
        if (installer != null) {
            layout.setPanel(TerminalBorderPanel.Panel.CENTER, menuNavigator);
            installer = null;
        }
    }

    
    // ===== PASSWORD =====
    
    private void showPasswordPrompt() {
        passwordPrompt = new PasswordPrompt("password", SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID,  application.getContainerHandle())
            .withTitle("Create System Password")
            .withPrompt("Enter new password:")
            .withConfirmPrompt("Confirm password:")
            .onPassword(this::handlePasswordCreated)
            .onCancel(this::handlePasswordCancelled);
        
        layout.setPanel(TerminalBorderPanel.Panel.CENTER, passwordPrompt);
        menuNavigator.hide();
        passwordPrompt.activate();
    }
    
    private void hidePasswordPrompt() {
        if (passwordPrompt != null) {
            passwordPrompt.deactivate();
            layout.setPanel(TerminalBorderPanel.Panel.CENTER, menuNavigator);
            passwordPrompt = null;
        }
    }
    
    private void handlePasswordCreated(NoteBytesEphemeral password) {
        application.createNewSystem(password)
            .thenRun(this::complete)
            .exceptionally(ex -> {
                errorMessage = "Failed to create system: " + ex.getMessage();
                transitionTo(STATE_PASSWORD, STATE_ERROR);
                return null;
            });
    }
    
    private void handlePasswordCancelled() {
        transitionTo(STATE_PASSWORD, STATE_MAIN_MENU);
    }
    
    // ===== COMPLETION =====
    
    private void complete() {
        if (onComplete != null) {
            onComplete.accept(null);
        }
    }
    
    public void setOnComplete(Consumer<Void> onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public TerminalRenderable getUI() {
        return this;
    }

    @Override
    public void setOnDisposed(Consumer<SystemUIInterface> onDisposed) {
        this.onDisposed = onDisposed;
    }

    @Override
    protected void onCleanup(){
        if(onDisposed != null){
            onDisposed.accept(this);
        }
    }
}
