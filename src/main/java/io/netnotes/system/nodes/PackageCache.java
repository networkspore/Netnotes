package io.netnotes.system.nodes;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;

/**
 * PackageCache - In-memory cache of available packages
 * Built from repository updates (like apt-cache)
 */
public class PackageCache {
    private final ConcurrentHashMap<NoteBytesReadOnly, PackageInfo> cache;
    
    public PackageCache() {
        this.cache = new ConcurrentHashMap<>();
    }
    
    public void updateCache(List<PackageInfo> packages) {
        cache.clear();
        packages.forEach(pkg -> cache.put(pkg.getPackageId(), pkg));
        Log.logMsg("[PackageCache] Cached " + packages.size() + " packages", LogLevel.IMPORTANT);
    }
    
    public List<PackageInfo> getAllPackages() {
        return new ArrayList<>(cache.values());
    }
    
    public PackageInfo getPackage(String packageId) {
        return cache.get(packageId);
    }
}