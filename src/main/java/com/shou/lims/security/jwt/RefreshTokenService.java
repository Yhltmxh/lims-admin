package com.shou.lims.security.jwt;

import com.shou.lims.common.cache.CacheService;
import com.shou.lims.common.constant.GlobalConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenService {

    private final CacheService cacheService;
    private final JwtRefreshTokenProperties refreshTokenProperties;

    public void store(Long userId, String refreshToken) {
        String key = GlobalConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        cacheService.set(key, refreshToken, refreshTokenProperties.expiration(), TimeUnit.DAYS);
    }

    public boolean validate(Long userId, String refreshToken) {
        String key = GlobalConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        String stored = cacheService.get(key);
        return stored != null && stored.equals(refreshToken);
    }

    public void revoke(Long userId) {
        String key = GlobalConstants.REDIS_REFRESH_TOKEN_PREFIX + userId;
        cacheService.delete(key);
    }
}
