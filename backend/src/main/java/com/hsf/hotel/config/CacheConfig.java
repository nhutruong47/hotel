package com.hsf.hotel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @Primary
    public CacheManager cacheManager(org.springframework.beans.factory.ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
                                     org.springframework.core.env.Environment env) {
        String cacheType = env.getProperty("spring.cache.type", "auto");
        RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();

        if (!"none".equalsIgnoreCase(cacheType) && !"simple".equalsIgnoreCase(cacheType) && factory != null) {
            try {
                // Test connection
                factory.getConnection().close();
                log.info("Configuring Distributed Redis Cache");

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()))
                        .disableCachingNullValues();

                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
                
                // Metadata / Static (TTL: 24h)
                RedisCacheConfiguration longTtlConfig = defaultConfig.entryTtl(Duration.ofHours(24));
                cacheConfigurations.put("roomTypes", longTtlConfig);
                cacheConfigurations.put("amenities", longTtlConfig);
                cacheConfigurations.put("promotions", longTtlConfig);
                cacheConfigurations.put("promotionViews", longTtlConfig);
                cacheConfigurations.put("faqs", longTtlConfig);

                // Real-time Availability (TTL: 5m)
                RedisCacheConfiguration shortTtlConfig = defaultConfig.entryTtl(Duration.ofMinutes(5));
                cacheConfigurations.put("rooms", shortTtlConfig);
                cacheConfigurations.put("roomViews", shortTtlConfig);

                return RedisCacheManager.builder(factory)
                        .cacheDefaults(defaultConfig)
                        .withInitialCacheConfigurations(cacheConfigurations)
                        .build();
            } catch (Exception ex) {
                log.warn("Redis is unreachable ({}), falling back to in-memory ConcurrentMapCacheManager", ex.getMessage());
            }
        }

        log.info("Configuring in-memory ConcurrentMapCacheManager");
        return new ConcurrentMapCacheManager("roomTypes", "amenities", "promotions", "promotionViews", "faqs", "rooms", "roomViews");
    }
}
