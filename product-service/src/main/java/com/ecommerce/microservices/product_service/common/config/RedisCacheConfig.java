package com.ecommerce.microservices.product_service.common.config;

import com.ecommerce.microservices.product_service.common.cache.CacheNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class RedisCacheConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Bean
    RedisCacheConfiguration redisCacheConfiguration(ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer serializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(10));
    }

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultRedisCacheConfiguration
    ) {
        return builder -> builder
                .withCacheConfiguration(
                        CacheNames.PRODUCT_BY_ID,
                        defaultRedisCacheConfiguration.entryTtl(Duration.ofMinutes(10))
                )
                .withCacheConfiguration(
                        CacheNames.CATEGORY_BY_ID,
                        defaultRedisCacheConfiguration.entryTtl(Duration.ofMinutes(30))
                );
    }

    @Bean
    CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logCacheError("get", exception, cache, key);
            }

            @Override
            public void handleCachePutError(
                    RuntimeException exception,
                    org.springframework.cache.Cache cache,
                    Object key,
                    Object value
            ) {
                logCacheError("put", exception, cache, key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logCacheError("evict", exception, cache, key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                logger.warn("Redis cache clear failed for cache '{}': {}", cache.getName(), exception.getMessage());
            }

            private void logCacheError(String operation, RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logger.warn(
                        "Redis cache {} failed for cache '{}' and key '{}': {}",
                        operation,
                        cache.getName(),
                        key,
                        exception.getMessage()
                );
            }
        };
    }
}
