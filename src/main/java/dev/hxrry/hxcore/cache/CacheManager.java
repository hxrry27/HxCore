package dev.hxrry.hxcore.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

public class CacheManager {

    private final Logger logger;
    private final Map<String, Cache<?, ?>> caches;
    private final boolean enableStats;
    
    /**
     * @param logger logger for debug output
     * @param enableStats Whether to track cache statistics
     */
    public CacheManager(Logger logger, boolean enableStats) {
        this.logger = logger;
        this.caches = new ConcurrentHashMap<>();
        this.enableStats = enableStats;
    }

    /**
     * @param name
     * @param expireAfterWrite 
     * @param expireAfterAccess null=never
     * @param maxSize null=unlim
     * @return the cache
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(String name, Duration expireAfterWrite, Duration expireAfterAccess, Integer maxSize) {
        
        return (Cache<K, V>) caches.computeIfAbsent(name, k -> {
            Caffeine<Object, Object> builder = Caffeine.newBuilder();
            
            // set expiration
            if (expireAfterWrite != null) {
                builder.expireAfterWrite(expireAfterWrite);
            }
            if (expireAfterAccess != null) {
                builder.expireAfterAccess(expireAfterAccess);
            }
            
            // set size limit
            if (maxSize != null && maxSize > 0) {
                builder.maximumSize(maxSize);
            }
            
            // enable statistics
            if (enableStats) {
                builder.recordStats();
            }
            
            // add removal listener for debugging
            builder.removalListener((key, value, cause) -> {
                if (cause != RemovalCause.REPLACED) {
                    logger.fine("Cache entry removed from " + name + ": " + key + " (reason: " + cause + ")");
                }
            });
            
            Cache<Object, Object> cache = builder.build();
            logger.info("Created cache '" + name + "' (maxSize=" + maxSize + ", expireWrite=" + expireAfterWrite + ", expireAccess=" + expireAfterAccess + ")");
            
            return cache;
        });
    }

    /**
     * @param name 
     * @param ttlMinutes 
     * @param maxSize max entries
     */
    public <K, V> Cache<K, V> createSimpleCache(String name, int ttlMinutes, int maxSize) {
        return createCache(name, Duration.ofMinutes(ttlMinutes), null, maxSize);
    }

    /**
     * @param name cache name
     * @return null if no cache
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }

    /**
     * @param cacheName 
     * @param key 
     * @param loader
     * @return
     */
    public <K, V> V get(String cacheName, K key, Function<K, V> loader) {
        Cache<K, V> cache = getCache(cacheName);
        if (cache == null) {
            logger.warning("Cache '" + cacheName + "' not found, loading without cache");
            return loader.apply(key);
        }
        
        return cache.get(key, loader);
    }

    /**
     * @param cacheName 
     * @param key 
     */
    public <K> void invalidate(String cacheName, K key) {
        Cache<K, ?> cache = getCache(cacheName);
        if (cache != null) {
            cache.invalidate(key);
            logger.fine("Invalidated " + key + " from cache " + cacheName);
        }
    }

    /**
     * @param cacheName
     */
    public void clearCache(String cacheName) {
        Cache<?, ?> cache = getCache(cacheName);
        if (cache != null) {
            long size = cache.estimatedSize();
            cache.invalidateAll();
            logger.info("Cleared cache '" + cacheName + "' (" + size + " entries)");
        }
    }

    public void clearAll() {
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            entry.getValue().invalidateAll();
        }
        logger.info("Cleared all " + caches.size() + " caches");
    }
    
    /**
     * @param cacheName Cache name
     * @return statistics or null
     */
    // for debuggin
    public CacheStats getStats(String cacheName) {
        if (!enableStats) {
            return null;
        }
        
        Cache<?, ?> cache = getCache(cacheName);
        return cache != null ? cache.stats() : null;
    }
    
    // for debuggin
    public void printStats() {
        if (!enableStats) {
            logger.info("Cache statistics are disabled");
            return;
        }
        
        logger.info("===== Cache Statistics =====");
        
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            String name = entry.getKey();
            Cache<?, ?> cache = entry.getValue();
            CacheStats stats = cache.stats();
            
            logger.info(String.format(
                "%s: size=%d, hits=%d, misses=%d, hit rate=%.2f%%, " +
                "loads=%d, avg load time=%.2fms",
                name,
                cache.estimatedSize(),
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate() * 100,
                stats.loadCount(),
                stats.averageLoadPenalty() / 1_000_000.0 // convert nanos to millis
            ));
        }
        
        logger.info("============================");
    }
    
    // builder for cache w api
    public class CacheBuilder<K, V> {
        private final String name;
        private Duration expireAfterWrite;
        private Duration expireAfterAccess;
        private Integer maxSize;
        
        private CacheBuilder(String name) {
            this.name = name;
        }
        
        public CacheBuilder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
            this.expireAfterWrite = Duration.of(duration, unit.toChronoUnit());
            return this;
        }
        
        public CacheBuilder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
            this.expireAfterAccess = Duration.of(duration, unit.toChronoUnit());
            return this;
        }
        
        public CacheBuilder<K, V> maximumSize(int size) {
            this.maxSize = size;
            return this;
        }
        
        public Cache<K, V> build() {
            return createCache(name, expireAfterWrite, expireAfterAccess, maxSize);
        }
    }

    //starts new cache build
    public <K, V> CacheBuilder<K, V> builder(String name) {
        return new CacheBuilder<>(name);
    }
}