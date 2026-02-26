package com.jjg.game.recharge.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.jjg.game.core.data.Order;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.CertPathValidator;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2025/10/16 11:07
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "applepay")
public class AppleCallbackController extends AbstractCallbackController {

    private static final long CLOCK_SKEW_SECOND = 3600; // 1小时时钟偏差
    private static final long CLOCK_SKEW_MILLS = CLOCK_SKEW_SECOND * 1000;

    /** Apple 受信任的根证书集合 */
    private static final Set<TrustAnchor> APPLE_TRUST_ANCHORS;
    private static final Set<X509Certificate> APPLE_ROOT_CERTS;

    /** 根证书文件路径列表（相对于工作目录） */
    private static final String[] APPLE_ROOT_CA_FILES = {
            "config/AppleRootCA-G3.cer",
            "config/AppleRootCA-G2.cer"
    };

    static {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Set<TrustAnchor> anchors = new HashSet<>();
            Set<X509Certificate> rootCerts = new HashSet<>();

            for (String filePath : APPLE_ROOT_CA_FILES) {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    continue;
                }
                try (InputStream is = new FileInputStream(path.toFile())) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
                    anchors.add(new TrustAnchor(cert, null));
                    rootCerts.add(cert);
                }
            }

            if (anchors.isEmpty()) {
                throw new IllegalStateException("未找到任何 Apple 根证书文件，请将 AppleRootCA-G2.cer 和 AppleRootCA-G3.cer 放置到 config 目录下");
            }

            APPLE_TRUST_ANCHORS = Collections.unmodifiableSet(anchors);
            APPLE_ROOT_CERTS = Collections.unmodifiableSet(rootCerts);
        } catch (IllegalStateException e) {
            throw new ExceptionInInitializerError(e.getMessage());
        } catch (Exception e) {
            throw new ExceptionInInitializerError("加载 Apple 根证书失败: " + e.getMessage());
        }
    }

    /**
     * 接收App Store Server Notifications V2格式的回调通知
     *
     * @param rawBody
     * @return
     * @throws Exception
     */
    @RequestMapping("callback")
    public ResponseEntity<String> callback(@RequestBody String rawBody) throws Exception {
        try {
//            log.info("收到apple充值回调 rawBody = {}", rawBody);
            log.info("收到apple充值回调");

            //安全验证
            if (!verifyAppleNotificationSignature(rawBody)) {
                log.warn("Apple回调签名验证失败");
                return ResponseEntity.status(403).body("Invalid signature");
            }

            //提取验证后的通知数据
            JsonNode notificationNode = extractNotificationData(rawBody);
            if (notificationNode == null) {
                return ResponseEntity.status(400).body("Invalid notification data");
            }

//            log.debug("验证后的通知数据: {}", notificationNode);

            // ===== 3. 处理不同类型的通知 =====
//            String notificationType = notificationNode.get("notificationType").asText();
//            String notificationUUID = notificationNode.get("notificationUUID").asText();

//            log.info("处理Apple通知 type={}, uuid={}", notificationType, notificationUUID);

            // 解析 signedTransactionInfo
            JsonNode transactionInfo = verifyAndDecodeTransactionInfo(notificationNode);
            if (transactionInfo == null) {
                log.warn("无法解析 signedTransactionInfo");
                return ResponseEntity.status(400).body("Invalid transaction info");
            }

            log.debug("解析后的交易信息: {}", transactionInfo);

            JsonNode appAccountTokenNode = transactionInfo.get("appAccountToken");
            if (appAccountTokenNode == null) {
                log.warn("缺少 appAccountToken 信息");
                return ResponseEntity.status(400).body("no appAccountToken");
            }

            Order order = orderService.getOrderByUUid(appAccountTokenNode.asText());
            if (order == null) {
                log.warn("未找到该订单 uuid = {}", appAccountTokenNode.asText());
                return ResponseEntity.status(400).body("not found order");
            }

            String transactionId = transactionInfo.get("transactionId").asText();
            order.setChannelOrderId(transactionId);
            order = checkOrder(order);
            if (order == null) {
                log.warn("检查订单失败 uuid = {}", appAccountTokenNode.asText());
                return ResponseEntity.status(400).body("check order failed");
            }
            String money = transactionInfo.get("price").asText();
            String currency = transactionInfo.get("currency").asText();
            String channelProductId = transactionInfo.get("productId").asText();

            payCallback(order, money, currency, channelProductId);
        } catch (Exception e) {
            log.error("处理Apple充值回调异常", e);
            return ResponseEntity.status(500).body("Error processing callback");
        }
        return ResponseEntity.ok("Recharge processed");
    }

    /**
     * 验证Apple通知签名
     *
     * @param notificationData 原始的JSON字符串
     * @return 验证结果
     */
    public boolean verifyAppleNotificationSignature(String notificationData) {
        try {
//            log.debug("开始验证Apple通知签名");

            // ===== 1. 解析原始通知数据 =====
            JsonNode notificationNode = objectMapper.readTree(notificationData);

            // ===== 2. 检查是否包含签名数据 =====
            if (!notificationNode.has("signedPayload")) {
                log.warn("通知中未找到signedPayload字段");
                return false;
            }

            String signedPayload = notificationNode.get("signedPayload").asText();

            // 2. 验证 signedPayload 是否为有效的 JWT
            if (!isValidJWT(signedPayload)) {
                log.warn("signedPayload 不是有效的 JWT 格式: {}", signedPayload);
                return false;
            }

            // ===== 3. 解码JWS Token =====
            DecodedJWT decodedJWT = decodeJWS(signedPayload);
            if (decodedJWT == null) {
                return false;
            }

            // ===== 4. 验证基本声明 =====
            if (!verifyBasicClaims(decodedJWT)) {
                return false;
            }

            // ===== 5. 获取公钥并验证签名 =====
            if (!verifySignature(decodedJWT)) {
                return false;
            }

            // ===== 6. 验证通过，可以安全使用数据 =====
//            log.debug("Apple通知签名验证成功");
            return true;

        } catch (Exception e) {
            log.error("验证Apple通知签名异常", e);
            return false;
        }
    }

    /**
     * 验证字符串是否为有效的 JWT 格式
     */
    private boolean isValidJWT(String signedPayload) {
        try {
            String[] parts = signedPayload.split("\\.");
            if (parts.length != 3) {
                log.warn("JWT 格式无效，部分数量不正确: {}", parts.length);
                return false;
            }
            Base64.getUrlDecoder().decode(parts[0]); // header
            Base64.getUrlDecoder().decode(parts[1]); // payload
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("JWT 格式无效，Base64 解码失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解码JWS Token
     */
    private DecodedJWT decodeJWS(String signedPayload) {
        try {
            // 直接解码JWT（不验证签名）
            DecodedJWT decodedJWT = JWT.decode(signedPayload);

            // 验证算法
            if (!"ES256".equals(decodedJWT.getAlgorithm())) {
                log.warn("不支持的签名算法: {}", decodedJWT.getAlgorithm());
                return null;
            }

            return decodedJWT;
        } catch (JWTDecodeException e) {
            log.warn("JWS解码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证基本声明
     */
    private boolean verifyBasicClaims(DecodedJWT jwt) {
        try {
            // 验证 signedDate
            JsonNode payloadNode = objectMapper.readTree(new String(Base64.getUrlDecoder().decode(jwt.getPayload())));
            if (!payloadNode.has("signedDate")) {
                log.warn("缺少 signedDate 字段");
                return false;
            }

            long signedDate = payloadNode.get("signedDate").asLong();
            long now = System.currentTimeMillis();
            if (signedDate < (now - CLOCK_SKEW_MILLS)) {
                log.warn("通知已过期: signedDate={}, now={}", signedDate, now);
                return false;
            }
            if (signedDate > (now + CLOCK_SKEW_MILLS)) {
                log.warn("通知尚未生效: signedDate={}, now={}", signedDate, now);
                return false;
            }

            // 验证 environment：防止沙盒回调在生产环境被处理（反之亦然）
//            JsonNode dataNode = payloadNode.get("data");
//            if (dataNode != null && dataNode.has("environment")) {
//                String environment = dataNode.get("environment").asText();
//                String expected = thirdServiceInfo.isSandbox() ? "Sandbox" : "Production";
//                if (!expected.equals(environment)) {
//                    log.warn("environment 不匹配: expected={}, actual={}", expected, environment);
//                    return false;
//                }
//            }

//            log.debug("基本声明验证通过");
            return true;

        } catch (Exception e) {
            log.error("验证基本声明异常", e);
            return false;
        }
    }

    /**
     * 验证签名（使用 x5c 证书链，并验证证书链锚定到 Apple Root CA）
     */
    private boolean verifySignature(DecodedJWT jwt) {
        try {
            // 解析 header 获取 x5c 证书链
            JsonNode headerNode = objectMapper.readTree(new String(Base64.getUrlDecoder().decode(jwt.getHeader())));
            if (!headerNode.has("x5c")) {
                log.warn("JWT 头部缺少 x5c 字段");
                return false;
            }

            JsonNode x5cNode = headerNode.get("x5c");
            if (!x5cNode.isArray() || x5cNode.isEmpty()) {
                log.warn("x5c 证书链为空或格式无效");
                return false;
            }

            // 解析 x5c 中的所有证书
            List<X509Certificate> allCerts = new ArrayList<>();
            for (JsonNode certNode : x5cNode) {
                X509Certificate cert = parseCertificate(certNode.asText());
                if (cert == null) {
                    log.warn("无法解析 x5c 证书链中的证书");
                    return false;
                }
                allCerts.add(cert);
            }

            // 从链中剥离受信任的根证书，剩余部分构建 CertPath
            // Java PKIX 规范要求 CertPath 不包含 trust anchor
            List<X509Certificate> pathCerts = new ArrayList<>();
            for (X509Certificate cert : allCerts) {
                if (!APPLE_ROOT_CERTS.contains(cert)) {
                    pathCerts.add(cert);
                }
            }

            if (pathCerts.isEmpty()) {
                log.warn("x5c 证书链中不包含非根证书（叶子/中间证书）");
                return false;
            }

            // 使用 PKIX 验证证书链
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(pathCerts);

            PKIXParameters params = new PKIXParameters(APPLE_TRUST_ANCHORS);
            params.setRevocationEnabled(false);

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            validator.validate(certPath, params);

            // 证书链验证通过，使用叶子证书的公钥验证 JWT 签名
            X509Certificate leafCert = allCerts.get(0);
            ECPublicKey publicKey = (ECPublicKey) leafCert.getPublicKey();

            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .acceptLeeway(CLOCK_SKEW_SECOND)
                    .build();

            verifier.verify(jwt.getToken());
//            log.debug("签名及证书链验证成功");
            return true;

        } catch (JWTVerificationException e) {
            log.error("JWT签名验证失败: {}", e.getMessage());
            return false;
        } catch (java.security.cert.CertPathValidatorException e) {
            log.error("x5c证书链验证失败，证书可能不是Apple签发: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }

    /**
     * 解析 Base64 编码的 X.509 证书
     */
    private X509Certificate parseCertificate(String certStr) {
        try {
            byte[] certBytes = Base64.getDecoder().decode(certStr);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (Exception e) {
            log.error("解析 X.509 证书失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从验证通过的 JWT 中提取通知数据
     */
    public JsonNode extractNotificationData(String rawBody) {
        try {
            // 解析 rawBody 获取 signedPayload
            JsonNode notificationNode = objectMapper.readTree(rawBody);
            if (!notificationNode.has("signedPayload")) {
                log.warn("rawBody 中未找到 signedPayload 字段");
                return null;
            }

            String signedPayload = notificationNode.get("signedPayload").asText();
            if (StringUtils.isEmpty(signedPayload)) {
                log.warn("signedPayload 为空");
                return null;
            }

            // 解码 JWT 的 payload 部分
            DecodedJWT decodedJWT = JWT.decode(signedPayload);
            String payload = new String(Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.error("提取通知数据异常", e);
            return null;
        }
    }

    /**
     * 解码 signedTransactionInfo
     */
    public JsonNode verifyAndDecodeTransactionInfo(JsonNode notificationNode) {
        try {
            // 提取 signedTransactionInfo
            JsonNode dataNode = notificationNode.get("data");
            if (dataNode == null || !dataNode.has("signedTransactionInfo")) {
                log.warn("通知中未找到 data.signedTransactionInfo 字段");
                return null;
            }

            String signedTransactionInfo = dataNode.get("signedTransactionInfo").asText();
            if (StringUtils.isEmpty(signedTransactionInfo)) {
                log.warn("signedTransactionInfo 为空");
                return null;
            }

            // 验证 signedTransactionInfo 是否为有效的 JWT
            if (!isValidJWT(signedTransactionInfo)) {
                log.warn("signedTransactionInfo 不是有效的 JWT 格式: {}", signedTransactionInfo);
                return null;
            }

            // 解码 JWS
            DecodedJWT decodedJWT = decodeJWS(signedTransactionInfo);
            if (decodedJWT == null) {
                return null;
            }

            // signedTransactionInfo 来自已验签的 signedPayload，避免重复验签

            // 提取 payload
            String payload = new String(Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.error("解析 signedTransactionInfo 异常", e);
            return null;
        }
    }
}

