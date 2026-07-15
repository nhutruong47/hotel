package com.hsf.hotel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache abstraction wired to Caffeine (in-memory, low-latency). The {@code
 * CacheManager} bean is the only thing to swap if the deployment later moves
 * to Redis — callers use {@link org.springframework.cache.annotation.Cacheable}
 * and never reference a cache implementation directly.
 *
 * <p>Tunables default to small numbers: this is a cache for <em>hot
 * reference data</em> (room types, amenities, room listings) and a heavy
 * cache here would defeat the purpose. Adjust {@code app.cache.*} from the
 * deployment environment to dial up.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${app.cache.max-size:500}")
    private long maxSize;

    @Value("${app.cache.ttl-seconds:300}")
    private long ttlSeconds;

    @Bean
    public CacheManager cacheManager() {
        log.info("Configuring Caffeine cache: maxSize={}, ttl={}s", maxSize, ttlSeconds);
        CaffeineCacheManager manager = new CaffeineCacheManager("rooms", "roomTypes",
                "amenities", "promotions", "faqs");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats());
        manager.setAllowNullValues(false);
        return manager;
    }
}