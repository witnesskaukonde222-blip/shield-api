package com.enterprise.shield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.ByteArrayInputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Configuration
public class RsaKeyConfig {

    @Value("${RSA_PUBLIC_KEY_BASE64:}")
    private String publicKeyBase64;

    @Value("${RSA_PRIVATE_KEY_BASE64:}")
    private String privateKeyBase64;

    @Bean
    @Primary
    public RsaKeyProperties rsaKeyProperties(
            @Value("${rsa.public-key:classpath:keys/public.pem}") org.springframework.core.io.Resource publicKeyResource,
            @Value("${rsa.private-key:classpath:keys/private_key.pem}") org.springframework.core.io.Resource privateKeyResource
    ) throws Exception {

        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;

        if (!publicKeyBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(publicKeyBase64.trim());
            publicKey = RsaKeyConverters.x509().convert(new ByteArrayInputStream(decoded));
        } else {
            publicKey = RsaKeyConverters.x509().convert(publicKeyResource.getInputStream());
        }

        if (!privateKeyBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(privateKeyBase64.trim());
            privateKey = RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(decoded));
        } else {
            privateKey = RsaKeyConverters.pkcs8().convert(privateKeyResource.getInputStream());
        }

        return new RsaKeyProperties(publicKey, privateKey);
    }
}