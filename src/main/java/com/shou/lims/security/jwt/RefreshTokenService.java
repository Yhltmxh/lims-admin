package com.shou.lims.security.jwt;

import com.shou.lims.common.constant.GlobalConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtRefreshTokenProperties refreshTokenProperties;

    public void store(Long userId, String refreshToken) {
        String key = GlobalConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken,
                Duration.ofDays(refreshTokenProperties.expiration()));
    }

    /**
     * 原子校验并轮换 RefreshToken，避免并发刷新同时成功。
     */
    public boolean rotate(Long userId, String oldToken, String newToken) {
        String key = GlobalConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        long ttlSeconds = TimeUnit.DAYS.toSeconds(refreshTokenProperties.expiration());
        Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(key),
                oldToken, newToken, String.valueOf(ttlSeconds));
        return Long.valueOf(1L).equals(result);
    }

    public void revoke(Long userId) {
        String key = GlobalConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
