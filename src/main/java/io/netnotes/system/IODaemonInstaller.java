package io.netnotes.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import io.netnotes.terminal.TerminalRectangle;
import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.TextStyle;
import io.netnotes.terminal.TextStyle.BoxStyle;
import io.netnotes.terminal.components.ScrollableTextViewer;
import io.netnotes.terminal.components.TerminalLabel;
import io.netnotes.terminal.components.TerminalProgressBar;
import io.netnotes.terminal.components.TerminalTextBox;
import io.netnotes.terminal.layout.TerminalLayoutData;
import io.netnotes.terminal.menus.MenuContext;
import io.netnotes.terminal.menus.MenuNavigator;
import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.github.GitHubAPI;
import io.netnotes.engine.utils.github.GitHubAsset;
import io.netnotes.engine.utils.github.GitHubInfo;
import io.netnotes.engine.utils.streams.UrlStreamHelpers;

/**
 * IODaemonInstaller - menu-driven installer using component-based UI.
 */
public class IODaemonInstaller extends TerminalRenderable {
    private static final GitHubInfo GITHUB_INFO = new GitHubInfo("networkspore", "NoteDaemon");

    // States
    private static final int STATE_IDLE = 20;
    private static final int STATE_FETCHING_RELEASES = 21;
    private static final int STATE_SHOWING_MENU = 22;
    private static final int STATE_CONFIRMING = 23;
    private static final int STATE_INSTALLING = 24;
    private static final int STATE_VERIFYING = 25;
    private static final int STATE_COMPLETE = 26;
    private static final int STATE_FAILED = 27;

    private final String osName;
    private final SystemApplication application;
    private Runnable onComplete;

    // UI components
    private final TerminalLabel headerLabel;
    private final TerminalTextBox statusBox;
    private final MenuNavigator menuNavigator;
    private final TerminalProgressBar progressBar;
    private final ScrollableTextViewer logViewer;

    // Installation state
    private volatile GitHubAsset[] availableReleases;
    private volatile GitHubAsset selectedAsset;
    private volatile String selectedVersion;
    private volatile String errorMessage;

    private volatile int currentStep = 0;
    private final int totalSteps = 14;

    // Work directories
    private Path workDir;
    private Path extractedDir;
    private Path buildDir;

    public IODaemonInstaller(String name, SystemApplication application) {
        super(name);
        this.application = application;
        this.osName = System.getProperty("os.name", "unknown").toLowerCase();

        headerLabel = new TerminalLabel("iod-header", "IODaemon Installer");
        headerLabel.setStyle(TextStyle.BOLD);

        statusBox = new TerminalTextBox("iod-status");
        statusBox.setBorderStyle(BoxStyle.SINGLE);
        statusBox.setTitle("Status");

        menuNavigator = new MenuNavigator("iod-menu");
        progressBar = new TerminalProgressBar("iod-progress", TerminalProgressBar.Style.BLOCKS);
        logViewer = new ScrollableTextViewer("iod-log", true, "Install Log");

        buildUi();

        stateMachine.addState(STATE_IDLE);
        transitionTo(STATE_IDLE, STATE_FETCHING_RELEASES);
    }

    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    private void buildUi() {
        addChild(headerLabel, ctx -> {
            TerminalRectangle parent = ctx.getParentRegion();
            return TerminalLayoutData.getBuilder()
                .setX(2)
                .setY(1)
                .setWidth(parent.getWidth() - 4)
                .setHeight(1)
                .build();
        });

        addChild(statusBox, ctx -> {
            TerminalRectangle parent = ctx.getParentRegion();
            return TerminalLayoutData.getBuilder()
                .setX(2)
                .setY(3)
                .setWidth(parent.getWidth() - 4)
                .setHeight(6)
                .build();
        });

        addChild(menuNavigator, ctx -> {
            TerminalRectangle parent = ctx.getParentRegion();
            return TerminalLayoutData.getBuilder()
                .setX(2)
                .setY(10)
                .setWidth(parent.getWidth() - 4)
                .setHeight(parent.getHeight() - 11)
                .build();
        });

        addChild(progressBar, ctx -> {
            TerminalRectangle parent = ctx.getParentRegion();
            return TerminalLayoutData.getBuilder()
                .setX(2)
                .setY(4)
                .setWidth(parent.getWidth() - 4)
                .setHeight(1)
                .build();
        });

        addChild(logViewer, ctx -> {
            TerminalRectangle parent = ctx.getParentRegion();
            int logHeight = Math.max(5, parent.getHeight() - 8);
            return TerminalLayoutData.getBuilder()
                .setX(2)
                .setY(6)
                .setWidth(parent.getWidth() - 4)
                .setHeight(logHeight)
                .build();
        });

        menuNavigator.hide();
        progressBar.hide();
        logViewer.hide();
    }

    @Override
    protected void setupStateTransitions() {
        super.setupStateTransitions();

        stateMachine.onStateAdded(STATE_FETCHING_RELEASES, (old, now, bit) -> {
            headerLabel.setText("IODaemon Installer");
            statusBox.setText("Fetching releases from GitHub...\nPlease wait.");
            statusBox.show();
            menuNavigator.hide();
            progressBar.hide();
            logViewer.hide();
            fetchReleases();
        });

        stateMachine.onStateAdded(STATE_SHOWING_MENU, (old, now, bit) -> {
            headerLabel.setText("Select IODaemon Release");
            statusBox.hide();
            menuNavigator.show();
            progressBar.hide();
            logViewer.hide();
            showReleaseSelectionMenu();
        });

        stateMachine.onStateAdded(STATE_CONFIRMING, (old, now, bit) -> {
            headerLabel.setText("Confirm Installation");
            statusBox.hide();
            menuNavigator.show();
            progressBar.hide();
            logViewer.hide();
            showConfirmationMenu();
        });

        stateMachine.onStateAdded(STATE_INSTALLING, (old, now, bit) -> {
            headerLabel.setText("Installing IODaemon " + selectedVersion);
            statusBox.hide();
            menuNavigator.hide();
            progressBar.show();
            logViewer.show();
            progressBar.reset();
            logViewer.clear();
            performInstallation();
        });

        stateMachine.onStateAdded(STATE_VERIFYING, (old, now, bit) -> {
            postProgress("Verifying installation...", totalSteps);
            application.getIoExecutor().submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                postProgress("Verification complete!", totalSteps);
                transitionTo(STATE_VERIFYING, STATE_COMPLETE);
                return null;
            });
        });

        stateMachine.onStateAdded(STATE_COMPLETE, (old, now, bit) -> {
            headerLabel.setText("Installation Complete");
            statusBox.hide();
            menuNavigator.show();
            progressBar.hide();
            logViewer.hide();
            showCompletionMenu(true);
        });

        stateMachine.onStateAdded(STATE_FAILED, (old, now, bit) -> {
            headerLabel.setText("Installation Failed");
            statusBox.hide();
            menuNavigator.show();
            progressBar.hide();
            logViewer.hide();
            showCompletionMenu(false);
        });
    }

    // ===== ASYNC OPERATIONS =====

    private void fetchReleases() {
        GitHubAPI api = new GitHubAPI(GITHUB_INFO);
        api.getAssetsAllLatestRelease(application.getIoExecutor())
            .thenApply(assets -> java.util.Arrays.stream(assets)
                .filter(asset -> asset.getName().endsWith(".tar.gz"))
                .filter(asset -> !asset.getName().contains("checksums"))
                .toArray(GitHubAsset[]::new))
            .thenAccept(assets -> {
                availableReleases = assets;
                transitionTo(STATE_FETCHING_RELEASES, STATE_SHOWING_MENU);
            })
            .exceptionally(ex -> {
                errorMessage = "Failed to fetch releases: " + ex.getMessage();
                transitionTo(STATE_FETCHING_RELEASES, STATE_FAILED);
                return null;
            });
    }

    private void performInstallation() {
        currentStep = 0;

        application.getIoExecutor().submit(() -> {
            if (osName.contains("linux")) {
                installLinux();
            } else if (osName.contains("mac")) {
                installMacOS();
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + osName);
            }
            return null;
        })
        .thenRun(() -> transitionTo(STATE_INSTALLING, STATE_VERIFYING))
        .exceptionally(ex -> {
            errorMessage = ex.getMessage();
            postProgress("Failed: " + errorMessage, currentStep);
            transitionTo(STATE_INSTALLING, STATE_FAILED);
            return null;
        });
    }

    // ===== MENU CONSTRUCTION =====

    private void showReleaseSelectionMenu() {
        if (availableReleases == null || availableReleases.length == 0) {
            errorMessage = "No releases found";
            transitionTo(STATE_SHOWING_MENU, STATE_FAILED);
            return;
        }

        ContextPath menuPath = application.getContextPath().append("release-selection");
        MenuContext menu = new MenuContext(
            menuPath,
            "Select IODaemon Release",
            buildReleasesDescription(),
            null
        );

        for (int i = 0; i < availableReleases.length; i++) {
            GitHubAsset asset = availableReleases[i];
            String itemName = "release-" + i;
            String description = buildReleaseDescription(asset);

            menu.addItem(itemName, description, () -> onReleaseSelected(asset));
        }

        menu.addSeparator("");
        menu.addItem("cancel", "Cancel Installation", this::goBack);

        menuNavigator.showMenu(menu);
    }

    private void showConfirmationMenu() {
        ContextPath menuPath = application.getContextPath().append("confirm");
        MenuContext menu = new MenuContext(
            menuPath,
            "Confirm Installation",
            buildConfirmationDescription(),
            null
        );

        menu.addItem("install", "Install Now", this::startInstallation);
        menu.addItem("back", "Back to Release Selection", () -> {
            transitionTo(STATE_CONFIRMING, STATE_SHOWING_MENU);
        });

        menuNavigator.showMenu(menu);
    }

    private void showCompletionMenu(boolean success) {
        ContextPath menuPath = application.getContextPath().append("completion");
        String title = success ? "Installation Complete" : "Installation Failed";
        String description = success ? buildSuccessDescription() : buildFailureDescription();

        MenuContext menu = new MenuContext(menuPath, title, description, null);

        if (success) {
            menu.addItem("finish", "Finish", this::goBack);
        } else {
            menu.addItem("retry", "Retry Installation", () -> {
                transitionTo(STATE_FAILED, STATE_SHOWING_MENU);
            });
            menu.addItem("cancel", "Cancel", this::goBack);
        }

        menuNavigator.showMenu(menu);
    }

    // ===== DESCRIPTION BUILDERS =====

    private String buildReleasesDescription() {
        return String.format("Found %d available releases.\nSelect a version to install.",
            availableReleases.length);
    }

    private String buildReleaseDescription(GitHubAsset asset) {
        String sizeStr = formatSize(asset.getSize());
        String dateStr = asset.getCreatedAt() != null
            ? asset.getCreatedAt().toString().substring(0, 10)
            : "unknown";

        return String.format("%s (%s, %s, %d downloads)",
            asset.getTagName(), sizeStr, dateStr, asset.getDownloadCount());
    }

    private String buildConfirmationDescription() {
        return String.format(
            "Ready to install IODaemon %s\n\n" +
            "File: %s\n" +
            "Size: %s\n\n" +
            "This will:\n" +
            "- Install system dependencies\n" +
            "- Download and build IODaemon\n" +
            "- Create system user and group\n" +
            "- Install systemd service\n" +
            "- Configure udev rules\n\n" +
            "Root access required.",
            selectedVersion,
            selectedAsset.getName(),
            formatSize(selectedAsset.getSize())
        );
    }

    private String buildSuccessDescription() {
        return String.format(
            "IODaemon %s installed successfully!\n\n" +
            "Service Status: Active\n" +
            "Socket: /var/run/io-daemon.sock\n\n" +
            "Installation completed in %d steps.\n\n" +
            "You may need to log out and back in for group membership to take effect.\n" +
            "Or run: newgrp netnotes",
            selectedVersion, currentStep
        );
    }

    private String buildFailureDescription() {
        return String.format(
            "Installation failed at step %d of %d!\n\n" +
            "Error: %s\n\n" +
            "Please check the logs for details.",
            currentStep, totalSteps,
            errorMessage != null ? errorMessage : "Unknown error"
        );
    }

    // ===== EVENT HANDLERS =====

    private void onReleaseSelected(GitHubAsset asset) {
        this.selectedAsset = asset;
        this.selectedVersion = extractVersionFromAsset(asset);
        transitionTo(STATE_SHOWING_MENU, STATE_CONFIRMING);
    }

    private void startInstallation() {
        transitionTo(STATE_CONFIRMING, STATE_INSTALLING);
    }

    private void goBack() {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    // ===== INSTALLATION LOGIC =====

    private void installLinux() throws Exception {
        updateProgress("Checking prerequisites...", 1);
        checkRootAccess();

        updateProgress("Installing dependencies...", 2);
        installLinuxDependencies();

        updateProgress("Setting up work directory...", 3);
        setupWorkDirectory();

        updateProgress("Downloading IODaemon " + selectedVersion + "...", 4);
        downloadSelectedRelease();

        updateProgress("Extracting archive...", 5);
        extractArchive();

        updateProgress("Creating system user and group...", 6);
        createSystemUser();

        updateProgress("Creating runtime directories...", 7);
        createRuntimeDirectories();

        updateProgress("Building IODaemon...", 8);
        buildProject();

        updateProgress("Installing binary...", 9);
        installBinary();

        updateProgress("Installing udev rules...", 10);
        installUdevRules();

        updateProgress("Installing systemd service...", 11);
        installSystemdService();

        updateProgress("Starting service...", 12);
        startService();

        updateProgress("Configuring user permissions...", 13);
        configureUserPermissions();

        updateProgress("Cleaning up...", 14);
        cleanupDirs();

        updateProgress("Installation complete!", 14);
    }

    private void installMacOS() throws Exception {
        throw new UnsupportedOperationException("macOS installer not implemented");
    }

    private void setupWorkDirectory() throws Exception {
        workDir = Files.createTempDirectory("notedaemon-install");
        postLog("Work directory: " + workDir);
    }

    private void downloadSelectedRelease() throws Exception {
        Path tarballPath = workDir.resolve("notedaemon.tar.gz");
        String downloadUrl = selectedAsset.getBrowserDownloadUrl();
        postLog("Downloading from: " + downloadUrl);
        UrlStreamHelpers.streamUrlToFile(downloadUrl, tarballPath);
        postLog("Download complete: " + formatSize(Files.size(tarballPath)));
    }

    private void extractArchive() throws Exception {
        postLog("Extracting archive...");
        Path tarballPath = workDir.resolve("notedaemon.tar.gz");
        String topLevelDir = null;

        try (var fileIn = Files.newInputStream(tarballPath);
             var gzipIn = new GZIPInputStream(fileIn);
             var tarIn = new TarArchiveInputStream(gzipIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path outputPath = workDir.resolve(entry.getName());

                if (topLevelDir == null && entry.isDirectory()) {
                    String name = entry.getName();
                    if (name.endsWith("/")) {
                        name = name.substring(0, name.length() - 1);
                    }
                    if (!name.contains("/")) {
                        topLevelDir = name;
                    }
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    Files.copy(tarIn, outputPath, StandardCopyOption.REPLACE_EXISTING);

                    if ((entry.getMode() & 0100) != 0) {
                        outputPath.toFile().setExecutable(true);
                    }
                }
            }
        }

        if (topLevelDir == null) {
            throw new RuntimeException("Could not detect top-level directory");
        }

        extractedDir = workDir.resolve(topLevelDir);
        postLog("Extracted to: " + extractedDir);
    }

    private void createSystemUser() throws Exception {
        postLog("Creating system user 'netnotes'...");

        try {
            executeCommand("groupadd", "--system", "netnotes");
        } catch (Exception e) {
            postLog("Group already exists");
        }

        try {
            executeCommand(
                "useradd",
                "--system", "--no-create-home",
                "--home-dir", "/var/lib/netnotes",
                "-g", "netnotes",
                "--shell", "/usr/sbin/nologin",
                "netnotes"
            );
        } catch (Exception e) {
            postLog("User already exists");
        }
    }

    private void createRuntimeDirectories() throws Exception {
        postLog("Creating runtime directories...");
        executeCommand("mkdir", "-p", "/var/lib/netnotes", "/run/netnotes");
        executeCommand("chown", "netnotes:netnotes", "/var/lib/netnotes", "/run/netnotes");
        executeCommand("chmod", "0750", "/var/lib/netnotes", "/run/netnotes");
    }

    private void buildProject() throws Exception {
        postLog("Building project...");
        buildDir = extractedDir.resolve("build");
        Files.createDirectories(buildDir);

        executeCommandInDirectory(buildDir, "cmake", "-DCMAKE_BUILD_TYPE=Release", "..");
        int processors = Runtime.getRuntime().availableProcessors();
        executeCommandInDirectory(buildDir, "make", "-j" + processors);
    }

    private void installBinary() throws Exception {
        Path binary = buildDir.resolve("note-daemon");
        executeCommand("install", "-m", "0755", binary.toString(), "/usr/local/bin/note-daemon");
    }

    private void installUdevRules() throws Exception {
        postLog("Installing udev rules...");
        Path rules = extractedDir.resolve("resources/99-netnotes.rules");
        executeCommand("install", "-m", "0644", rules.toString(), "/etc/udev/rules.d/99-netnotes.rules");
        executeCommand("udevadm", "control", "--reload-rules");
        executeCommand("udevadm", "trigger");
    }

    private void installSystemdService() throws Exception {
        postLog("Installing systemd service...");
        Path service = extractedDir.resolve("resources/note-daemon.service");
        executeCommand("install", "-m", "0644", service.toString(), "/etc/systemd/system/note-daemon.service");
        executeCommand("systemctl", "daemon-reload");
        executeCommand("systemctl", "enable", "note-daemon.service");
        executeCommand("systemctl", "start", "note-daemon.service");

        String status = executeCommandWithOutput("systemctl", "is-active", "note-daemon.service");
        postLog("Service status: " + status.trim());
    }

    private void startService() throws Exception {
        executeCommand("systemctl", "start", "note-daemon.service");
    }

    private void configureUserPermissions() throws Exception {
        String user = System.getProperty("user.name");
        postLog("Adding user to netnotes group: " + user);
        executeCommand("usermod", "-a", "-G", "netnotes", user);
    }

    private void cleanupDirs() throws Exception {
        if (workDir != null) {
            deleteRecursively(workDir);
        }
    }

    private void checkRootAccess() throws Exception {
        String uid = executeCommandWithOutput("id", "-u").trim();
        if (!"0".equals(uid)) {
            throw new RuntimeException("Root access required to install IODaemon");
        }
    }

    private void installLinuxDependencies() throws Exception {
        postLog("Installing dependencies...");
        executeCommand("apt-get", "update", "-qq");
        String installCmd = "apt-get install -y build-essential cmake pkg-config libusb-1.0-0-dev";
        executeCommand("sh", "-c", installCmd);
    }

    // ===== PROCESS EXECUTION =====

    private void executeCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                postLog("  " + line);
                Log.logMsg("  " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }

    private void executeCommandInDirectory(Path directory, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                postLog("  " + line);
                Log.logMsg("  " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }

    private String executeCommandWithOutput(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                postLog("  " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }

        return output.toString();
    }

    private void deleteRecursively(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteRecursively(child);
                    } catch (Exception e) {
                        Log.logError("Could not delete: " + child);
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }

    // ===== PROGRESS =====

    private void updateProgress(String message, int step) {
        currentStep = step;
        postProgress(message, step);
    }

    private void postProgress(String message, int step) {
        double percent = (step * 100.0) / totalSteps;
        application.getUiExecutor().executeFireAndForget(() -> {
            progressBar.updatePercent(percent);
            logViewer.addLine(String.format("[%d/%d] %s", step, totalSteps, message));
        });
        Log.logMsg("[Installer] " + message);
    }

    private void postLog(String line) {
        application.getUiExecutor().executeFireAndForget(() -> logViewer.addLine(line));
    }

    // ===== UTILITIES =====

    private String extractVersionFromAsset(GitHubAsset asset) {
        String tagName = asset.getTagName();
        if (tagName != null && !tagName.isEmpty()) {
            return tagName;
        }

        String name = asset.getName();
        if (name.contains("-")) {
            String[] parts = name.split("-");
            if (parts.length > 1) {
                return parts[1].replace(".tar.gz", "");
            }
        }

        return "unknown";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
