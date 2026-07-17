package com.shou.lims.security.service;

import com.shou.lims.common.cache.CacheService;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RsaKeyServiceTest {

    @Test
    void shouldRoundTripPasswordWithRedisSafePrivateKey() throws Exception {
        CacheService cacheService = mock(CacheService.class);
        AtomicReference<Object> cachedValue = new AtomicReference<>();
        doAnswer(invocation -> {
            cachedValue.set(invocation.getArgument(1));
            return null;
        }).when(cacheService).set(anyString(), any(), anyLong(), any());
        when(cacheService.get(anyString())).thenAnswer(invocation -> cachedValue.get());

        RsaKeyService service = new RsaKeyService(cacheService);
        RsaKeyService.RsaKeyPair keyPair = service.generateKeyPair();
        assertThat(cachedValue.get()).isInstanceOf(String.class);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyFactory.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(keyPair.publicKey()))));
        String cipherText = Base64.getEncoder().encodeToString(
                cipher.doFinal("123456".getBytes(StandardCharsets.UTF_8)));

        assertThat(service.decrypt(keyPair.keyId(), cipherText)).isEqualTo("123456");
    }
}
