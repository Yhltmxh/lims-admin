package com.shou.lims.security.jwt;

import com.shou.lims.common.constant.GlobalConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, long ttlSeconds) {
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key(token), "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
    }

    private String key(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return GlobalConstants.REDIS_BLACKLIST_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前JDK不支持SHA-256", e);
        }
    }
}
