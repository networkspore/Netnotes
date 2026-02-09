package io.netnotes.system.nodes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.StreamChannel;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.noteFiles.NoteFile;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.github.GitHubInfo;
import io.netnotes.engine.utils.streams.UrlStreamHelpers;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RepositoryManager - Manages repository list (System Service)
 * 
 * REFACTORED:
 * - No longer stores AppData reference
 * - Receives path from parent at construction
 * - Uses only AppDataInterface for file access
 * - Parent decides where repository manager lives
 * 
 * LIFECYCLE:
 * - Created by AppData during system initialization
 * - Lives for entire application lifetime
 * - Maintains NoteFile reference for efficient access
 * - Closed during shutdown
 * 
 * STORAGE:
 * - Path: {myPath}/sources (parent decides base path)
 * - Format: NoteBytesMap { repositoryId -> Repository }
 * - NoteFile cached as instance field (no repeated ledger access)
 * 
 * EXTERNAL FORMAT (from GitHub):
 * Fetches packages.json from each repository URL:
 * {
 *   "repository": "Official Netnotes",
 *   "packages": [...]
 * }
 */
public class RepositoryManager extends FlowProcess {
    public static final GitHubNodeRepository OFFICIAL_REPO = new GitHubNodeRepository("official","Official Netnotes Repository",
            GitHubInfo.NETNOTES_OFFICIAL_REPO,
            "packages/packages.json",
            "packages/key.gpg",
            true
        );
    private final ConcurrentHashMap<NoteBytesReadOnly, Repository> repositories;
    
    // Cached NoteFile - expensive to get, cheap to keep
    // should last the duration of RepositoryManager and then be closed
    private NoteFile sourcesFile;
    
    /**
     * Constructor
     * 
     * @param name Process name (e.g., "repository-manager")
     * @param repositoryFile NoteFile for repository sources
     */
    public RepositoryManager(
        String name,
        NoteFile repositoryFile
    ) {
        super(name, ProcessType.BIDIRECTIONAL);
        this.sourcesFile = repositoryFile;
        this.repositories = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Void> run() {
        return initialize()
            .thenCompose(v -> getCompletionFuture());
    }
    
    
    /**
     * Initialize - load repositories from NoteFile
     * 
     * Gets NoteFile ONCE and stores for entire lifecycle.
     */
    public CompletableFuture<Void> initialize() {
        Log.logMsg("[RepositoryManager] Initializing at: " + contextPath);
  
        return loadRepositories()
            .exceptionallyCompose(ex -> {
                // First run - add default repository
                Log.logMsg("[RepositoryManager] No existing sources found, adding defaults");
                addDefaultRepository();
                return saveRepositories();
            });
    }
    
    /**
     * Load repositories from cached NoteFile
     */
    private CompletableFuture<Void> loadRepositories() {
        return sourcesFile.readNoteBytes()
            .thenAccept(noteBytesObj -> {
                // Deserialize from NoteBytes format
                NoteBytesMap reposMap = noteBytesObj.getAsNoteBytesMap();
                
                Log.logMsg("[RepositoryManager] Found " + reposMap.size() + " repositories");
                
                for (Map.Entry<NoteBytes, NoteBytes> entry : reposMap.entrySet()) {
                    NoteBytes repoId = entry.getKey();
                    try {
                        Repository repo = Repository.fromNoteBytes(entry.getValue().getAsNoteBytesMap());
                        repositories.put(repoId.readOnly(), repo);
                        
                        Log.logMsg("[RepositoryManager] Loaded: " + 
                            repo.getName() + (repo.isEnabled() ? "" : " (disabled)"));
                            
                    } catch (Exception e) {
                        Log.logError("[RepositoryManager] Failed to load repository " + 
                            repoId + ": " + e.getMessage());
                    }
                }
            });
    }

    private void emitRepositoryEvent(String eventType, NoteBytesReadOnly repoId) {
        emit(new NoteBytesObject(new NoteBytesPair[]{
            new NoteBytesPair("event", eventType),
            new NoteBytesPair("repository_id", repoId),
            new NoteBytesPair("timestamp", System.currentTimeMillis())
        }));
    }

    /**
     * Add default official repository on first run
     */
    private void addDefaultRepository() {
        
        repositories.put(OFFICIAL_REPO.getId(), OFFICIAL_REPO);
        Log.logMsg("[RepositoryManager] Added default repository: " + 
            OFFICIAL_REPO.getName());
    }
    
    /**
     * Get all repositories
     */
    public List<Repository> getRepositories() {
        return new ArrayList<>(repositories.values());
    }
    
    /**
     * Add a new repository
     */
    public CompletableFuture<Void> addRepository(Repository repo) {
        repositories.put(repo.getId(), repo);
        Log.logMsg("[RepositoryManager] Added repository: " + repo.getName());
        emitRepositoryEvent("repository_added", repo.getId());
        return saveRepositories();
    }
    
    /**
     * Remove a repository
     */
    public CompletableFuture<Void> removeRepository(String repoId) {
        Repository removed = repositories.remove(repoId);
        if (removed != null) {
            Log.logMsg("[RepositoryManager] Removed repository: " + 
                removed.getName());
            emitRepositoryEvent("repository_removed", removed.getId());
            
            return saveRepositories();
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Enable/disable a repository
     */
    public CompletableFuture<Void> setRepositoryEnabled(String repoId, boolean enabled) {
        Repository repo = repositories.get(repoId);
        if (repo != null) {
            repo.setEnabled(enabled);
            Log.logMsg("[RepositoryManager] Repository " + repo.getName() + 
                " " + (enabled ? "enabled" : "disabled"));
            emitRepositoryEvent(
                enabled ? "repository_enabled" : "repository_disabled",
                repo.getId()
            );
            return saveRepositories();
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Update all repositories (like apt-get update)
     * Fetches package lists from all enabled repositories
     */
    public CompletableFuture<List<PackageInfo>> updateAllRepositories() {
        List<CompletableFuture<List<PackageInfo>>> futures = new ArrayList<>();
        
        Log.logMsg("[RepositoryManager] Updating package lists from " + 
            repositories.values().stream().filter(Repository::isEnabled).count() + 
            " enabled repositories");
        
        for (Repository repo : repositories.values()) {
            if (repo.isEnabled()) {
                futures.add(fetchPackagesFromRepository(repo));
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<PackageInfo> allPackages = new ArrayList<>();
                for (CompletableFuture<List<PackageInfo>> future : futures) {
                    try {
                        allPackages.addAll(future.join());
                    } catch (Exception e) {
                        Log.logError("[RepositoryManager] Failed to get packages: " + 
                            e.getMessage());
                    }
                }
                
                Log.logMsg("[RepositoryManager] Found " + allPackages.size() + 
                    " packages total");
                return allPackages;
            });
    }
    
    /**
     * Fetch packages from a single repository
     * 
     * This is where JSON comes in - repositories serve packages.json externally
     */
    private CompletableFuture<List<PackageInfo>> fetchPackagesFromRepository(Repository repo) {
        
        return UrlStreamHelpers.getUrlContentAsString(repo.getUrl())
            .thenCompose(jsonString ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Log.logMsg("[RepositoryManager] Fetching from: " + repo.getName());
                        
                        // Fetch JSON from URL (EXTERNAL format)
                        JsonObject repoData = JsonParser.parseString(jsonString).getAsJsonObject();
                        
                        // Get packages array
                        JsonArray packagesArray = repoData.getAsJsonArray("packages");
                        
                        List<PackageInfo> packages = new ArrayList<>();
                        
                        // Convert each package from JSON to internal PackageInfo
                        for (JsonElement elem : packagesArray) {
                            JsonObject pkgJson = elem.getAsJsonObject();
                            
                            try {
                                // Create PackageInfo from external JSON
                                String packageId = pkgJson.get("id").getAsString();
                                String name = pkgJson.get("name").getAsString();
                                String version = pkgJson.get("version").getAsString();
                                String category = pkgJson.has("category") ? 
                                    pkgJson.get("category").getAsString() : "uncategorized";
                                String description = pkgJson.has("description") ? 
                                    pkgJson.get("description").getAsString() : "";
                                String downloadUrl = pkgJson.get("download_url").getAsString();
                                long size = pkgJson.has("size") ? 
                                    pkgJson.get("size").getAsLong() : 0;
                                
                                // Parse manifest
                                JsonObject manifestJson = pkgJson.getAsJsonObject("manifest");
                                PackageManifest manifest = PackageManifest.fromJson(manifestJson);
                                
                                PackageInfo pkg = new PackageInfo(
                                    new NoteBytesReadOnly(packageId),
                                    name,
                                    version,
                                    category,
                                    description,
                                    repo.getName(), // Which repo it came from
                                    downloadUrl,
                                    size,
                                    manifest
                                );
                                
                                packages.add(pkg);
                                
                            } catch (Exception e) {
                                Log.logError("[RepositoryManager] Failed to parse package in " + 
                                    repo.getName() + ": " + e.getMessage());
                            }
                        }
                        
                        Log.logMsg("[RepositoryManager] " + repo.getName() + 
                            " provided " + packages.size() + " packages");
                        
                        return packages;
                        
                    } catch (Exception e) {
                        Log.logError("[RepositoryManager] Failed to fetch from " + 
                            repo.getName() + ": " + e.getMessage());
                        return new ArrayList<>();
                    }
                    
                }, VirtualExecutors.getVirtualExecutor())
            );
    }

  
    
    /**
     * Save repositories to cached NoteFile
     * 
     * Format: NoteBytesMap { repositoryId -> Repository.toNoteBytes() }
     */
    private CompletableFuture<Void> saveRepositories() {
        if (sourcesFile == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("RepositoryManager not initialized - sourcesFile is null"));
        }
        
        NoteBytesMap reposMap = new NoteBytesMap();
        
        // Serialize each repository
        for (Repository repo : repositories.values()) {
            reposMap.put(
                repo.getId(),
                repo.toNoteBytes()
            );
        }
        
        // Write to cached NoteFile (no ledger access!)
        return sourcesFile.write(reposMap.toNoteBytes())
            .exceptionally(ex -> {
                Log.logError("[RepositoryManager] Failed to save: " + 
                    ex.getMessage());
                throw new RuntimeException("Failed to save repository sources", ex);
            });
    }
    
    /**
     * Shutdown - ensure sources are saved and NoteFile closed
     */
    public CompletableFuture<Void> shutdown() {
        Log.logMsg("[RepositoryManager] Shutting down, saving sources");
        
        return saveRepositories()
            .whenComplete((v, ex) -> {
                if (ex != null) {
                    Log.logError("[RepositoryManager] Error during shutdown save: " + 
                        ex.getMessage());
                }
                
                if (sourcesFile != null) {
                    sourcesFile.close(); // disables further access
                    Log.logMsg("[RepositoryManager] NoteFile closed");
                }
            })
            .thenRun(() -> {
                Log.logMsg("[RepositoryManager] Shutdown complete");
            });
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        long enabled = repositories.values().stream()
            .filter(Repository::isEnabled)
            .count();
        
        return String.format(
            "Repositories: %d total, %d enabled",
            repositories.size(),
            enabled
        );
    }
    

    @Override
    public void handleStreamChannel(StreamChannel channel, ContextPath fromPath) {
        throw new UnsupportedOperationException("Unimplemented method 'handleStreamChannel'");
    }
    
}