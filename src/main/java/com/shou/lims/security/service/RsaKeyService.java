package com.shou.lims.security.service;

import com.shou.lims.common.cache.CacheService;
import com.shou.lims.common.constant.GlobalConstants;
import com.shou.lims.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RsaKeyService {

    private final CacheService cacheService;

    public record RsaKeyPair(String keyId, String publicKey) {}

    public RsaKeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String keyId = java.util.UUID.randomUUID().toString();
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            // Cache private key in Redis, TTL 5 minutes
            cacheService.set(GlobalConstants.REDIS_RSA_KEY_PREFIX + keyId,
                    keyPair.getPrivate().getEncoded(), 5, TimeUnit.MINUTES);

            return new RsaKeyPair(keyId, publicKey);
        } catch (Exception e) {
            throw new RuntimeException("RSA密钥生成失败", e);
        }
    }

    public String decrypt(String keyId, String cipherText) {
        byte[] privateKeyBytes = cacheService.get(GlobalConstants.REDIS_RSA_KEY_PREFIX + keyId);
        if (privateKeyBytes == null) {
            throw new BusinessException(400, "RSA密钥不存在或已过期");
        }
        // Delete private key after use (one-time use)
        cacheService.delete(GlobalConstants.REDIS_RSA_KEY_PREFIX + keyId);

        try {
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keyFactory.generatePrivate(spec));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(400, "RSA解密失败");
        }
    }
}
