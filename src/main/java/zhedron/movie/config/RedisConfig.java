package zhedron.movie.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import zhedron.movie.dto.response.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("users", config.serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new JacksonJsonRedisSerializer<>(UserResponse.class))));
        cacheConfigurations.put("mediaContents", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(MediaContentResponse.class))
        ));
        cacheConfigurations.put("seasons", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(SeasonResponse.class))
        ));
        cacheConfigurations.put("films", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(FilmResponse.class))
        ));
        cacheConfigurations.put("episodes", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(EpisodeResponse.class))
        ));
        cacheConfigurations.put("comments", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(CommentResponse.class))
        ));
        cacheConfigurations.put("actors", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(ActorResponse.class))
        ));
        cacheConfigurations.put("subtitles", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(SubtitleResponse.class))
        ));
        cacheConfigurations.put("favorites", config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(FavoriteResponse.class))
        ));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config).
                withInitialCacheConfigurations(cacheConfigurations).
                build();
    }
}
