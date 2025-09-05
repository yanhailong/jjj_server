package com.jjg.game.gm.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *
 * @author lm
 * @date 2025/7/11 11:11
 */
public class CryptoUtils {
    private static final PrivateKey privateKey;

    private static final Logger log = LoggerFactory.getLogger(CryptoUtils.class);

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
        Map<String, JsonNode> map = objectMapper.readValue(request, new TypeReference<>() {
        });
        String sign = map.remove("sign").asText();
        log.debug("getDecryptRequest request={}", request);
        log.debug("getDecryptRequest map={}", map);
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

    private static String getMd5String(Map<String, JsonNode> map) {
        List<String> keyList = map.keySet().stream().sorted().toList();
        StringBuilder builder = new StringBuilder();
        for (String key : keyList) {
            JsonNode value = map.get(key);
            // 跳过null值，与客户端逻辑一致
            if (value == null || value.isNull()) {
                continue;
            }
            // 处理不同类型的值，与客户端逻辑一致
            String valueStr = formatValueForSign(value);
            if (valueStr != null) {
                builder.append(key).append("=").append(valueStr);
                if (!key.equals(keyList.getLast())) {
                    builder.append("&");
                }
            }
        }
        log.debug("getMd5String builder.toString()={}", builder);
        return DigestUtils.md5Hex(builder.toString()).toUpperCase();
    }

    /**
     * 格式化值以匹配PHP客户端的处理逻辑
     */
    private static String formatValueForSign(JsonNode value) {
        if (value.isNull()) {
            return null; // 返回null表示跳过该字段
        } else if (value.isArray() || value.isObject()) {
            // 数组或对象转换为JSON字符串
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(value);
            } catch (Exception e) {
                return value.toString();
            }
        } else {
            // 基本类型直接转换为字符串
            return value.asText();
        }
    }

    public static void main(String[] args) throws Exception {
        PrivateKey privateKey = CryptoUtils.loadPrivateKey(Path.of(System.getProperty("user.dir"), "/config/private_key.pem"));
        PublicKey publicKey = CryptoUtils.loadPublicKey(Path.of(System.getProperty("user.dir"), "/config/public_key.pem"));

        // 测试与PHP客户端兼容的验签
        String testData = """
                {"activityImageType":"1","sourceName":"123.png","jumpType":1,"jumpValue":"200500","sort":55,"showType":null,"id":9}""";

        System.out.println("=== 测试与PHP客户端兼容的验签 ===");
        System.out.println("原始数据: " + testData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, JsonNode> map = objectMapper.readValue(testData, new TypeReference<>() {
        });

        // 模拟PHP客户端的签名过程
        System.out.println("\n模拟PHP客户端签名过程:");
        List<String> keyList = map.keySet().stream().sorted().toList();
        StringBuilder phpBuilder = new StringBuilder();
        for (String key : keyList) {
            JsonNode value = map.get(key);
            if (value != null && !value.isNull()) {
                String valueStr = formatValueForSign(value);
                if (valueStr != null) {
                    phpBuilder.append(key).append("=").append(valueStr).append("&");
                }
            }
        }
        String phpStr = phpBuilder.toString();
        if (phpStr.endsWith("&")) {
            phpStr = phpStr.substring(0, phpStr.length() - 1);
        }
        System.out.println("PHP签名字符串: " + phpStr);
        String phpMd5 = DigestUtils.md5Hex(phpStr).toUpperCase();
        System.out.println("PHP MD5: " + phpMd5);

        // 测试服务端验签
        String encrypt = CryptoUtils.encrypt(phpMd5, publicKey);
        System.out.println("加密签名: " + encrypt);

        // 构建带签名的请求
        String requestWithSign = testData.substring(0, testData.length() - 1) + ",\"sign\":\"" + encrypt + "\"}";
        System.out.println("\n带签名的请求: " + requestWithSign);

        // 测试服务端验签
        String result = getDecryptRequest(requestWithSign);
        System.out.println("验签结果: " + result);

        // 验证MD5是否匹配
        String decrypt = decrypt(encrypt, privateKey);
        System.out.println("解密得到的MD5: " + decrypt);
        System.out.println("MD5是否匹配: " + phpMd5.equals(decrypt));
    }
}
