package io.netnotes.system;

import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.TextStyle.LineStyle;
import io.netnotes.terminal.components.install.InstallStep;
import io.netnotes.terminal.components.install.TerminalInstallWizard;
import io.netnotes.terminal.components.panels.TerminalDivider;
import io.netnotes.terminal.components.panels.TerminalPanel;
import io.netnotes.terminal.components.panels.TerminalVStack;
import io.netnotes.terminal.components.text.TerminalLabel;
import io.netnotes.terminal.input.TerminalTextInput;
import io.netnotes.terminal.menus.MenuContext;
import io.netnotes.terminal.menus.MenuNavigator;
import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.daemon.DiscoveredDeviceRegistry.DeviceDescriptorWithCapabilities;
import io.netnotes.engine.messaging.NoteMessaging.ItemTypes;
import io.netnotes.engine.ui.LayoutOverflowStrategy;
import io.netnotes.engine.ui.Orientation;
import io.netnotes.engine.ui.SizePreference;
import io.netnotes.engine.ui.TextAlignment;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
import io.netnotes.engine.virtualExecutors.SerializedScheduledVirtualExecutor;
import io.netnotes.engine.virtualExecutors.VirtualExecutors;
import io.netnotes.noteBytes.NoteBytes;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SystemSetupWizardScreen - Sequential setup wizard using TerminalInstallWizard.
 *
 * Replaces the menu-driven SystemSetupScreen with a linear, step-based flow.
 * The only compulsory step is password creation (first-run only); all hardware
 * setup steps are optional and skippable.
 *
 * STEPS
 *   1. Configure Security Hardware  — optional; user may skip entirely
 *        a. Scan for IODaemon
 *        b. Install IODaemon       — only if not detected
 *        c. Select USB Keyboard    — only if IODaemon available and keyboards found
 *   2. Set System Password         — required on first-run; skipped on reconfigure
 *
 * LAYOUT
 * ┌──────────────────────────────────────────────────────────┐
 * │  ◈  NETNOTES                             appTitleLabel   │
 * │  Initial System Configuration            appTaglineLabel │
 * │  ──────────────────────────────────────  div1            │
 * │  ╔══════════════════════════════════════╗                │
 * │  ║  Netnotes Setup       0% [░░░░░░░]  ║ installWizard  │
 * │  ╠══════════════════════════════════════╣                │
 * │  ║  ◉ 1. Scan for Security Hardware    ║                │
 * │  ║  ○ 2. Install IODaemon   [optional] ║                │
 * │  ║  ○ 3. Configure USB Keyboard        ║                │
 * │  ║  ○ 4. Set System Password           ║                │
 * │  ╠══════════════════════════════════════╣                │
 * │  ║  Step 1 of 4  ·  Elapsed: 00:03     ║                │
 * │  ╚══════════════════════════════════════╝                │
 * │  ──────────────────────────────────────  div2            │
 * │  [menuNavigator | inputStack]            interactive     │
 * └──────────────────────────────────────────────────────────┘
 */
public class SystemSetupWizardScreen extends TerminalPanel implements SystemUIInterface {

    private static final LogLevel LOG_LEVEL = LogLevel.IMPORTANT;


    // ── Step identifiers
    private static final String STEP_HARDWARE = "hardware";
    private static final String STEP_PASSWORD = "password";

    // ── Outer card
    private final TerminalPanel wizardCard;
    private SerializedScheduledVirtualExecutor scheduler = VirtualExecutors.getSerializedScheduledVirtualExecutor();

    // ── Branding heade
    private final TerminalLabel   appTitleLabel;
    private final TerminalLabel   appTaglineLabel;
    private final TerminalDivider div1;

    // ── Progress wizard
    private final TerminalInstallWizard installWizard;

    // ── Interactive area (user choices / installer UI)
    private final TerminalDivider div2;
    private final MenuNavigator   menuNavigator;
    private final TerminalVStack  inputStack;

    // ── Data
    private final SystemApplication application;
    private final boolean isFirstRun;
    private volatile String socketPath;
    private volatile boolean ioDaemonAvailable = false;
    private volatile List<DeviceDescriptorWithCapabilities> availableKeyboards = null;

    private IODaemonInstaller   installer;
    private TerminalTextInput socketInput;
    private CompletableFuture<Void>  tickFuture;
    private volatile boolean    wizardDone = false;

    private Consumer<Void>              onComplete;
    private Consumer<SystemUIInterface> onDisposed;


    // CONSTRUCTION

    public SystemSetupWizardScreen(SystemApplication application) {
        super("system-setup-wizard");
        this.application = application;
        this.isFirstRun  = !application.isAuthenticated();
        String configured = application.getIODaemonSocketPath();
        this.socketPath   = configured != null
            ? configured
            : SystemApplication.DEFAULT_IO_DAEMON_SOCKET_PATH;

        // ── Outer shell: fills terminal, centres card
        setAxis(Axis.VERTICAL);
        setWidthPreference(SizePreference.FILL);
        setHeightPreference(SizePreference.FILL);
        setAlignment(Alignment.CENTER);
        setCrossAlignment(Alignment.CENTER);

        // ── Wizard card
        wizardCard = new TerminalPanel("setup-wiz-card");
        wizardCard.setAxis(Axis.VERTICAL);
        wizardCard.setWidthPreference(SizePreference.PERCENT);
        wizardCard.setPercentWidth(0.7f);
        wizardCard.setMaxWidth(80);
        wizardCard.setMinWidth(62);
        wizardCard.setHeightPreference(SizePreference.FILL);
        wizardCard.setMinHeight(15);
        wizardCard.setSpacing(1);
        wizardCard.setPadding(1, 2);
        wizardCard.setEnableBorder(true);
        wizardCard.setBorderStyle(LineStyle.SINGLE);
        wizardCard.setBorderTextStyle(SystemStyles.CARD_BORDER_STYLE);
        wizardCard.setFocusedBorderTextStyle(SystemStyles.CARD_FOCUSED_BORDER_STYLE);
        wizardCard.setFillStyle(SystemStyles.CARD_FILL_STYLE);

        // ── Branding
        appTitleLabel = new TerminalLabel("setup-wiz-title");
        appTitleLabel.setText("  \u25C8  NETNOTES");
        appTitleLabel.setWidthPreference(SizePreference.FILL);
        appTitleLabel.setHeightPreference(SizePreference.FIT_CONTENT);
        appTitleLabel.setTextStyle(SystemStyles.WIZARD_TITLE_STYLE);

        appTaglineLabel = new TerminalLabel("setup-wiz-tagline");
        appTaglineLabel.setText(isFirstRun
            ? "  Initial System Configuration"
            : "  System Reconfiguration");
        appTaglineLabel.setWidthPreference(SizePreference.FILL);
        appTaglineLabel.setHeightPreference(SizePreference.FIT_CONTENT);
        appTaglineLabel.setTextStyle(SystemStyles.WIZARD_TAGLINE_STYLE);

        div1 = makeDivider("setup-wiz-div1");

        // ── Install wizard
        installWizard = TerminalInstallWizard.builder("netnotes-setup-builder")
            .showFooter(true)
            .showBorder(true)
            .title("Netnotes Setup")
            .borderStyle(LineStyle.DOUBLE)
            .build();

        installWizard.setOverflowStrategy(LayoutOverflowStrategy.SHRINK_FILL);

        // ── Interactive area
        div2 = makeDivider("setup-wiz-div2");

        menuNavigator = new MenuNavigator("setup-wiz-menu");
        menuNavigator.setWidthPreference(SizePreference.FILL);
        menuNavigator.setHeightPreference(SizePreference.FIT_CONTENT);

        inputStack = new TerminalVStack("setup-wiz-input");
        inputStack.setWidthPreference(SizePreference.FILL);
        inputStack.setHeightPreference(SizePreference.FIT_CONTENT);
        inputStack.setSpacing(0);

        div2.hide();
        menuNavigator.hide();
        inputStack.hide();

        // ── Assemble card
        wizardCard.addChild(appTitleLabel);
        wizardCard.addChild(appTaglineLabel);
        wizardCard.addChild(div1);
        wizardCard.addChild(installWizard);
        wizardCard.addChild(div2);
        wizardCard.addChild(menuNavigator);
        wizardCard.addChild(inputStack);

        addChild(wizardCard);

        buildWizardSteps();


        Log.logMsg("[SystemSetupWizardScreen] instantiated, firstRun=" + isFirstRun, LOG_LEVEL);
    }

    @Override
    protected void onStarted() {

        beginHardwareStep();
    }


    // ── Pre-populate all steps at construction time
    private void buildWizardSteps() {
        InstallStep hw = new InstallStep(STEP_HARDWARE, "Configure Security Hardware");
        hw.setOptional(true);

        if (isFirstRun) {
            InstallStep pwd = new InstallStep(STEP_PASSWORD, "Set System Password");
            pwd.setOptional(false);
            installWizard.addSteps(List.of(hw, pwd));
        } else {
            installWizard.addSteps(List.of(hw));
        }
    }

    // DIVIDER FACTORIES
    private TerminalDivider makeDivider(String name) {
        TerminalDivider div = new TerminalDivider(name, Orientation.HORIZONTAL);
        div.setLineStyle(LineStyle.SINGLE);
        div.setLineTextStyle(SystemStyles.WIZARD_DIVIDER_STYLE);
        return div;
    }

    private TerminalDivider makeLabelledDivider(String name, String label) {
        TerminalDivider div = makeDivider(name);
        div.setLabel(label);
        div.setLabelAlignment(TextAlignment.LEFT);
        div.setLabelTextStyle(SystemStyles.WIZARD_TAGLINE_STYLE);
        return div;
    }

    // SPINNER TICK
    private void startTick() {
        if (tickFuture != null && !tickFuture.isDone()) return;
        tickFuture = scheduler.scheduleAtFixedRate(this::runTick, 150, 150, TimeUnit.MILLISECONDS);
    }

    private void runTick(){
        if (wizardDone) {
            stopTick();
            return;
        }
        installWizard.tick();
    }

    private void stopTick() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
            tickFuture = null;
        }
    }

    // INTERACTIVE AREA HELPERS
    private void showMenuArea(MenuContext menu) {
        inputStack.hide();
        menuNavigator.showMenu(menu);
        menuNavigator.show();
        menuNavigator.requestFocus();
        div2.show();
    }

    private void showInputArea() {
        menuNavigator.hide();
        inputStack.show();
        div2.show();
    }

    private void hideInteractiveArea() {
        clearSocketInput();
        menuNavigator.hide();
        inputStack.hide();
        div2.hide();
    }



    // STEP 1: SCAN
    // Automatically detects IODaemon and USB keyboards. No user interaction.
    private void beginHardwareStep(){
        
        Log.logMsg("[SystemSetupWizardScreen] beginHardwareSetupInternal started", LOG_LEVEL);
        ContextPath path = application.getContextPath().append("setup-hardware");
        MenuContext menu = new MenuContext(path, null, null, null);

        menu.addItem("configure",
            "Configure security hardware  \u2192 enables hardware-secured password entry",
            () -> {
                hideInteractiveArea();
                installWizard.beginStep(STEP_HARDWARE);
                startTick();
                runHardwareScan();
            });

        menu.addSeparator("");
        menu.addItem("skip",
            "Skip  \u2192 use on-screen keyboard only",
            () -> {
                hideInteractiveArea();
                startTick();   // mirror the configure path so elapsed time tracks the password step
                application.completeBootstrap(null)
                    .thenRun(() -> {
                        installWizard.skipStep(STEP_HARDWARE);
                        // beginPasswordStep touches component state — must run on UI thread.
                        uiExecutor.runLater(this::beginPasswordStep);
                    })
                    .exceptionally(ex -> {
                        installWizard.skipStep(STEP_HARDWARE);
                        uiExecutor.runLater(this::beginPasswordStep);
                        return null;
                    });
            });

        showMenuArea(menu);
    }
    private void runHardwareScan() {
        installWizard.logLine(STEP_HARDWARE, "Checking IODaemon service...");

        application.getIoDaemonManager().detect()
            .thenAccept(result -> {
                ioDaemonAvailable = result.isFullyOperational();
                if (ioDaemonAvailable) {
                    installWizard.logLine(STEP_HARDWARE, "IODaemon: operational");
                    runKeyboardScan();
                } else {
                    installWizard.logLine(STEP_HARDWARE, "IODaemon: not found");
                    offerIODaemonInstall();
                }
            })
            .exceptionally(ex -> {
                Log.logError("[Setup] Scan failed: " + ex.getMessage());
                installWizard.logLine(STEP_HARDWARE, "Detection error — continuing");
                offerIODaemonInstall();
                return null;
            });
    }

    private void runKeyboardScan() {
        installWizard.logLine(STEP_HARDWARE, "Scanning for USB keyboards...");
        application.getIoDaemonManager().discoverDevicesOfType(ItemTypes.KEYBOARD)
            .thenAccept(keyboards -> {
                availableKeyboards = keyboards;
                int count = keyboards != null ? keyboards.size() : 0;
                installWizard.logLine(STEP_HARDWARE, "Found " + count + " USB keyboard(s)");
                offerKeyboardSelection();
            })
            .exceptionally(ex -> {
                installWizard.logLine(STEP_HARDWARE, "Keyboard scan failed — skipping keyboard setup");
                availableKeyboards = null;
                hardwareStepComplete(null);
                return null;
            });
    }

    // ── Sub-step B: IODaemon install (only when not detected)

    private void offerIODaemonInstall() {
        if(!uiExecutor.isCurrentThread()){
            uiExecutor.runLater(this::offerIODaemonInstall);
            return;
        }
        ContextPath path = application.getContextPath().append("setup-iodaemon");
        MenuContext menu = new MenuContext(path, null, null, null);

        menu.addItem("install",
            "Install IODaemon",
            this::launchIODaemonInstaller);

        menu.addSeparator("");
        menu.addItem("socket",
            "Change socket path  (current: " + socketPath + ")",
            this::showSocketConfigInline);

        menu.addSeparator("");
        menu.addItem("skip",
            "Skip IODaemon  \u2192 use on-screen keyboard only",
            () -> {
                hideInteractiveArea();
                hardwareStepComplete(null);
            });

        showMenuArea(menu);
        
    }


     // ── Sub-step C: keyboard selection (only when keyboards found)

    private void offerKeyboardSelection() {
        if (availableKeyboards == null || availableKeyboards.isEmpty()) {
            installWizard.logLine(STEP_HARDWARE, "No keyboards found — skipping keyboard setup");
            hardwareStepComplete(null);
            return;
        }

        offerKeyboarSelectionInternal();
       
    }

    private void offerKeyboarSelectionInternal(){
        if(!uiExecutor.isCurrentThread()){
            uiExecutor.runLater(this::offerKeyboarSelectionInternal);
            return;
        }
        ContextPath path = application.getContextPath().append("setup-keyboard");
        MenuContext menu = new MenuContext(path, null, null, null);

        for (DeviceDescriptorWithCapabilities device : availableKeyboards) {
            NoteBytes deviceId  = device.usbDevice().getDeviceId();
            String manufacturer = device.usbDevice().manufacturer;
            String product      = device.usbDevice().product;

            String displayName = product != null ? product : "USB Keyboard";
            if (manufacturer != null && !manufacturer.isBlank()) {
                displayName = manufacturer + " " + displayName;
            }
            String badge = device.claimed() ? "IN USE" : null;

            menu.addItem(deviceId.getAsString(), displayName, badge,
                () -> claimKeyboard(deviceId));
        }

        menu.addSeparator("");
        menu.addItem("gui",
            "Use on-screen keyboard instead",
            () -> {
                hideInteractiveArea();
                hardwareStepComplete(null);
            });

        showMenuArea(menu);
    }
  

    private void launchIODaemonInstaller() {
        hideInteractiveArea();
        launchIODaemonInstallerInternal();
    }

    private void launchIODaemonInstallerInternal(){
        if(!uiExecutor.isCurrentThread()){
            uiExecutor.runLater(this::launchIODaemonInstallerInternal);
            return;
        }
    
        installer = new IODaemonInstaller("setup-iod-installer", application);
        installer.setWidthPreference(SizePreference.FILL);
        installer.setHeightPreference(SizePreference.FIT_CONTENT);
        installer.setOnComplete(() -> {
            inputStack.clearChildren();
            installer = null;
            hideInteractiveArea();
            ioDaemonAvailable = true;
            installWizard.logLine(STEP_HARDWARE, "IODaemon installed — scanning keyboards...");
            runKeyboardScan();
        });
        inputStack.clearChildren();
        inputStack.addChild(installer);
        showInputArea();
    }



    private void showSocketConfigInline() {
        clearSocketInput();
        inputStack.clearChildren();
        inputStack.addChild(makeLabelledDivider("socket-divider", "Socket Path"));

        socketInput = new TerminalTextInput("socket-path-input", 0, 0, 60);
        socketInput.setText(socketPath);

        socketInput.setOnComplete(newPath -> {
            clearSocketInput();
            hideInteractiveArea();

            if (newPath == null || newPath.trim().isEmpty() || !newPath.startsWith("/")) {
                // Invalid or cancelled — just go back to the IODaemon offer menu
                offerIODaemonInstall();
                return;
            }

            String oldPath = socketPath;
            socketPath = newPath.trim();
            application.getIoDaemonManager().setIODaemonSocketPath(socketPath);

            // Re-probe with the new path before deciding what to show
            installWizard.logLine(STEP_HARDWARE, "Testing socket path: " + socketPath);

            application.getIoDaemonManager().detect()
                .thenAccept(result -> {
                    if (result.isFullyOperational()) {
                        installWizard.logLine(STEP_HARDWARE, "IODaemon found at new path");
                        ioDaemonAvailable = true;
                        runKeyboardScan();
                    } else {
                        installWizard.logLine(STEP_HARDWARE, "IODaemon not found at new path");
                        offerIODaemonInstall();
                    }
                })
                .exceptionally(ex -> {
                    // Roll back on error
                    socketPath = oldPath;
                    application.getIoDaemonManager().setIODaemonSocketPath(oldPath);
                    installWizard.logLine(STEP_HARDWARE, "Path test failed — reverted to: " + oldPath);
                    offerIODaemonInstall();
                    return null;
                });
        });

        socketInput.setOnEscape(ignored -> {
            clearSocketInput();
            hideInteractiveArea();
            offerIODaemonInstall();
        });

        inputStack.addChild(socketInput);
        showInputArea();
        socketInput.requestFocus();
    }

    private void clearSocketInput() {
        if (socketInput != null) {
            inputStack.removeChild(socketInput);
            socketInput.close();
            socketInput = null;
        }
    }

 
    private void claimKeyboard(NoteBytes deviceId) {
        hideInteractiveArea();
        installWizard.updateProgress(STEP_HARDWARE, 0.8f, "Claiming keyboard…");
        application.completeBootstrap(deviceId)
            .thenRun(() -> hardwareStepComplete(deviceId))
            .exceptionally(ex -> {
                installWizard.logLine(STEP_HARDWARE, "Could not claim device: " + ex.getMessage());
                hardwareStepComplete(null);
                return null;
            });
    }

    // ── Hardware step terminal point.
    // May arrive from async completion threads (thenRun / exceptionally).
    // Dispatches to the UI executor when not already there, matching beginHardwareStep.

    private void hardwareStepComplete(NoteBytes chosenKeyboardId) {
        if (!uiExecutor.isCurrentThread()) {
            uiExecutor.runLater(() -> hardwareStepComplete(chosenKeyboardId));
            return;
        }
        if (chosenKeyboardId == null) {
            application.completeBootstrap(null);
        }
        installWizard.completeStep(STEP_HARDWARE);
        beginPasswordStep();
    }

    // STEP 2: PASSWORD 
    private void beginPasswordStep() {
        if (!isFirstRun) {
            // Reconfiguration complete — no password change needed
            wizardDone = true;
            stopTick();
            complete();
            return;
        }

        installWizard.beginStep(STEP_PASSWORD);
        hideInteractiveArea();

        application.getPasswordService()
            .requestCreation(
                "Create System Password",
                "Enter new password:",
                "Confirm password:",
                pw -> application.createNewSystem(pw).thenApply(v -> true),
                5
            )
            .thenAccept(result -> {
                if (result.success()) {
                    installWizard.completeStep(STEP_PASSWORD);
                    wizardDone = true;
                    stopTick();
                    complete();
                } else {
                    installWizard.failStep(STEP_PASSWORD, result.message());
                    wizardDone = true;
                    stopTick();
                }
            });
    }

    // =========================================================================
    // COMPLETION
    // =========================================================================

    private void complete() {
        if (onComplete != null) {
            onComplete.accept(null);
        }
    }

    // =========================================================================
    // SystemUIInterface
    // =========================================================================

    public void setOnComplete(Consumer<Void> onComplete) {
        this.onComplete = onComplete;
    }

    @Override
    public void setOnDisposed(Consumer<SystemUIInterface> onDisposed) {
        this.onDisposed = onDisposed;
    }

    @Override
    public TerminalRenderable getUI() {
        return this;
    }

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void setupStateTransitions() {
        // No separate state machine — InstallStep statuses drive the flow.
    }

    @Override
    protected void onRemovedFromLayout(){
        destroy();
    }

    @Override
    protected void onDestroying() {
        super.onDestroying();
        wizardDone = true;
        stopTick();
        clearSocketInput();
        if (installer != null) {
            inputStack.removeChild(installer);
            installer = null;
        }
        if (onDisposed != null) {
            onDisposed.accept(this);
        }
    }
}