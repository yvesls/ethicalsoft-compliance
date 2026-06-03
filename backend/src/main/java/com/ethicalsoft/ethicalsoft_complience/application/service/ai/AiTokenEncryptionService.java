package com.ethicalsoft.ethicalsoft_complience.application.service.ai;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@Slf4j
public class AiTokenEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final String configuredKey;
    private SecretKey secretKey;

    public AiTokenEncryptionService(@Value("${app.ai.token-encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes;
        if (configuredKey == null || configuredKey.isBlank()) {
            log.warn("[ai-token] 'app.ai.token-encryption-key' não definida — derivando chave do JWT secret. " +
                    "Em produção, configure AI_TOKEN_ENCRYPTION_KEY (32 bytes base64).");
            keyBytes = derive("ethicalsoft-ai-token-fallback-key".getBytes(StandardCharsets.UTF_8));
        } else {
            try {
                byte[] decoded = Base64.getDecoder().decode(configuredKey.trim());
                keyBytes = (decoded.length == 32) ? decoded : derive(decoded);
            } catch (IllegalArgumentException ex) {
                keyBytes = derive(configuredKey.getBytes(StandardCharsets.UTF_8));
            }
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            log.error("[ai-token] Falha ao criptografar token", ex);
            throw new BusinessException("ai.token.encryption_failed",
                    "Falha ao proteger o token de IA. Tente novamente.");
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Payload criptografado inválido");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("[ai-token] Falha ao decriptar token", ex);
            throw new BusinessException("ai.token.decryption_failed",
                    "Não foi possível ler seu token de IA. Cadastre-o novamente.");
        }
    }

    public String maskToken(String plaintext) {
        if (plaintext == null) return null;
        String trimmed = plaintext.trim();
        if (trimmed.length() <= 8) return "********";
        return trimmed.substring(0, 4) + "…" + trimmed.substring(trimmed.length() - 4);
    }

    private byte[] derive(byte[] source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(source);
        } catch (Exception ex) {
            throw new BusinessException("ai.token.encryption_failed",
                    "Não foi possível inicializar a criptografia de tokens de IA.");
        }
    }
}
