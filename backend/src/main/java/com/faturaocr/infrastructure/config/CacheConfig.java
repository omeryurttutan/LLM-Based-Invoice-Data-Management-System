package com.faturaocr.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

                RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(1)) // Default TTL 1 minute
                                .disableCachingNullValues()
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer));

                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                // Categories: 5 minutes
                cacheConfigurations.put("categories", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

                // Invoice counts: 30 seconds
                cacheConfigurations.put("invoice-counts", defaultCacheConfig.entryTtl(Duration.ofSeconds(30)));

                // Unread notification count: 10 seconds (short because it updates frequently)
                cacheConfigurations.put("unread-notification-count",
                                defaultCacheConfig.entryTtl(Duration.ofSeconds(10)));

                // Supplier templates: 5 minutes
                cacheConfigurations.put("supplier-templates", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

                // Rules: 5 minutes
                cacheConfigurations.put("rules", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

                // User profile: 10 minutes
                cacheConfigurations.put("user-profile", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

                // Company info: 10 minutes
                cacheConfigurations.put("company-info", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultCacheConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }
}
