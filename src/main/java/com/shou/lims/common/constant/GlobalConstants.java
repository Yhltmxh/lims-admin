package com.shou.lims.common.constant;

public final class GlobalConstants {

    private GlobalConstants() {}

    public static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String REDIS_BLACKLIST_PREFIX = "blacklist:";
    public static final String REDIS_RSA_KEY_PREFIX = "rsa:";
    public static final String REDIS_AUTHORIZATION_PREFIX = "authz:effective:v2:";
    public static final String SUPER_ADMIN_ROLE = "ROLE_ADMIN";
}
