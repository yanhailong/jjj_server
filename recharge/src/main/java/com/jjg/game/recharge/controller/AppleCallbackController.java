package com.jjg.game.recharge.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.ThirdServiceInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

/**
 * @author 11
 * @date 2025/10/16 11:07
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "applepay")
public class AppleCallbackController extends AbstractCallbackController {

    private static final long CLOCK_SKEW_SECOND = 600; // 10分钟时钟偏差
    private static final long CLOCK_SKEW_MILLS = CLOCK_SKEW_SECOND * 1000;

    @Autowired
    private ThirdServiceInfo thirdServiceInfo;


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
            log.info("收到apple充值回调 rawBody = {}", rawBody);

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
            log.debug("开始验证Apple通知签名");

            // ===== 1. 解析原始通知数据 =====
            JsonNode notificationNode = objectMapper.readTree(notificationData);

            // ===== 2. 检查是否包含签名数据 =====
            if (!notificationNode.has("signedPayload")) {
                log.warn("通知中未找到signedPayload字段");
                return false;
            }

            String signedPayload = notificationNode.get("signedPayload").asText();
            log.debug("获取到signedPayload，长度: {}", signedPayload.length());

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
            log.debug("Apple通知签名验证成功");
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
            if (signedDate < (now - CLOCK_SKEW_MILLS)) {  // 5 分钟延迟
                log.warn("通知已过期: signedDate={}, now={}", signedDate, now);
                return false;
            }
            if (signedDate > (now + CLOCK_SKEW_MILLS)) {
                log.warn("通知尚未生效: signedDate={}, now={}", signedDate, now);
                return false;
            }

            JsonNode dataNode = payloadNode.get("data");
            if (!thirdServiceInfo.getAppleBundleId().equals(dataNode.get("bundleId").asText())) {
                log.warn("bundleId 匹配不上 : jwtBundleId={}, cfgBundleId={}", dataNode.get("bundleId").asText(), thirdServiceInfo.getAppleBundleId());
                return false;
            }
            if (thirdServiceInfo.getAppleAppId() != dataNode.get("appAppleId").asLong()) {
                log.warn("apple app id 匹配不上 : jwtAppid={}, cfgAppid={}", dataNode.get("appAppleId").asText(), thirdServiceInfo.getAppleAppId());
                return false;
            }

            log.debug("基本声明验证通过");
            return true;

        } catch (Exception e) {
            log.error("验证基本声明异常", e);
            return false;
        }
    }

    /**
     * 验证签名（使用 x5c 证书链）
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
            if (!x5cNode.isArray() || x5cNode.size() == 0) {
                log.warn("x5c 证书链为空或格式无效");
                return false;
            }

            // 提取证书链中的第一张证书（签名证书）
            String certStr = x5cNode.get(0).asText();
            X509Certificate certificate = parseCertificate(certStr);
            if (certificate == null) {
                log.warn("无法解析 x5c 证书");
                return false;
            }

            // 获取公钥
            ECPublicKey publicKey = (ECPublicKey) certificate.getPublicKey();
//            log.debug("成功获取公钥: {}", publicKey);

            // 创建验证器并验证签名
            Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .acceptLeeway(CLOCK_SKEW_SECOND)
                    .build();

            verifier.verify(jwt.getToken());
            log.debug("签名验证成功");
            return true;

        } catch (JWTVerificationException e) {
            log.error("签名验证失败: {}", e.getMessage());
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
     * 验证并解码 signedTransactionInfo
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

            // 验证签名
            if (!verifySignature(decodedJWT)) {
                return null;
            }

            // 提取 payload
            String payload = new String(Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.error("解析 signedTransactionInfo 异常", e);
            return null;
        }
    }
}

