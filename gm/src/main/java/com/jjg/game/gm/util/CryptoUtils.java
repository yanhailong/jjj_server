package com.jjg.game.gm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * gm请求加密,解密工具
 * @author lm
 * @date 2025/7/11 11:11
 */
public class CryptoUtils {
    private static final PrivateKey privateKey;

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {
            privateKey = CryptoUtils.loadPrivateKey(Path.of(System.getProperty("user.dir"), "/config/private_key.pem"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey loadPrivateKey(Path path) throws Exception {
        PEMParser parser = new PEMParser(Files.newBufferedReader(path));
        PrivateKeyInfo keyInfo = (PrivateKeyInfo) parser.readObject();
        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(keyInfo);
        parser.close();
        return privateKey;
    }

    public static PublicKey loadPublicKey(Path path) throws Exception {
        PEMParser parser = new PEMParser(Files.newBufferedReader(path));
        Object object = parser.readObject();
        parser.close();

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PublicKey publicKey;

        if (object instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) {
            publicKey = converter.getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) object);
        } else {
            throw new IllegalArgumentException("无效的公钥文件格式");
        }

        return publicKey;
    }

    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }


    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }


    public static String getDecryptRequest(String request) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(request, new TypeReference<>() {
        });
        String sign = (String) map.remove("sign");
        if (Objects.isNull(sign)) {
            return null;
        }
        String md5String = getMd5String(map);
        String decrypt = decrypt(sign, privateKey);
        if (md5String.equals(decrypt)) {
            return objectMapper.writeValueAsString(map);
        }
        return null;
    }

    private static String getMd5String(Map<String, Object> map) {
        List<String> keyList = map.keySet().stream().sorted().toList();
        StringBuilder builder = new StringBuilder();
        for (String key : keyList) {
            Object value = map.get(key);
            if (Objects.nonNull(value)) {
                builder.append(key)
                        .append("=")
                        .append(value);
                if (!key.equals(keyList.get(keyList.size() - 1))) {
                    builder.append("&");
                }
            }
        }
        return DigestUtils.md5Hex(builder.toString()).toUpperCase();
    }

}
