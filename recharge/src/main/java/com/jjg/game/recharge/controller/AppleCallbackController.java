package com.jjg.game.recharge.controller;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.jjg.game.core.data.ThirdServiceInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/10/16 11:07
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "applepay")
public class AppleCallbackController extends AbstractCallbackController{

    // Apple相关常量
    private static final String APPLE_ISSUER = "appstoreconnect-v1";
    private static final String APPLE_AUDIENCE = "appstoreconnect-v1";
    private static final long CLOCK_SKEW = 300; // 5分钟时钟偏差

    private final JwkProvider jwkProvider;
    public AppleCallbackController(ThirdServiceInfo thirdServiceInfo) {
        // 创建JWK提供者，用于获取Apple的公钥
        this.jwkProvider = new JwkProviderBuilder(thirdServiceInfo.getAppleJwksUrl())
                .cached(10, 24, TimeUnit.HOURS) // 缓存10个密钥，24小时
                .rateLimited(10, 1, TimeUnit.MINUTES) // 限流：每分钟10次
                .build();

    }

    /**
     * 回调
     * 接收App Store Server Notifications V2格式的回调通知
     *
     * @return
     */
    @RequestMapping("callback")
    public ResponseEntity<String> callback(@RequestBody String rawBody) {
        try{
            log.debug("收到apple充值回调 rawBody = {}", rawBody);

            //安全验证
            if(!verifyAppleNotificationSignature(rawBody)){
                log.warn("Apple回调签名验证失败");
                return ResponseEntity.status(403).body("Invalid signature");
            }

            //提取验证后的通知数据
            JsonNode notificationNode = extractNotificationData(rawBody);
            if (notificationNode == null) {
                return ResponseEntity.status(400).body("Invalid notification data");
            }

            log.debug("验证后的通知数据: {}", notificationNode);

            // ===== 3. 处理不同类型的通知 =====
            String notificationType = notificationNode.get("notificationType").asText();
            String notificationUUID = notificationNode.get("notificationUUID").asText();

            log.info("处理Apple通知 type={}, uuid={}", notificationType, notificationUUID);

            // 根据通知类型路由到不同的处理方法
//            switch (notificationType) {
//                case "DID_RENEW" -> {}
//                    return handleSuccessfulRenewal(notificationNode);
//                case "DID_FAIL_TO_RENEW":
//                    return handleRenewalFailure(notificationNode);
//                case "EXPIRED":
//                    return handleSubscriptionExpired(notificationNode);
//                case "REFUND":
//                    return handleRefund(notificationNode);
//                case "TEST":
//                    return handleTestNotification(notificationNode);
//                default:
//                    return handleOtherNotification(notificationNode);
//            }
        }catch (Exception e){
            log.error("处理Apple充值回调异常", e);
            return ResponseEntity.status(500).body("Error processing callback");
        }
        return ResponseEntity.ok("Recharge processed");
    }

    /**
     * 验证Apple通知签名
     *
     * @param notificationData 原始的JSON字符串（从请求体获取）
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
     * 解码JWS Token
     */
    private DecodedJWT decodeJWS(String signedPayload) {
        try {
            // 直接解码JWT（不验证签名）
            DecodedJWT decodedJWT = JWT.decode(signedPayload);

            // 验证算法
            if (!"RS256".equals(decodedJWT.getAlgorithm())) {
                log.warn("不支持的签名算法: {}", decodedJWT.getAlgorithm());
                return null;
            }

            log.debug("JWS解码成功，Key ID: {}", decodedJWT.getKeyId());
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
            // 验证颁发者
            if (!APPLE_ISSUER.equals(jwt.getIssuer())) {
                log.warn("颁发者验证失败: expected={}, actual={}", APPLE_ISSUER, jwt.getIssuer());
                return false;
            }

            // 验证受众
            if (!jwt.getAudience().contains(APPLE_AUDIENCE)) {
                log.warn("受众验证失败: expected={}, actual={}", APPLE_AUDIENCE, jwt.getAudience());
                return false;
            }

            // 验证过期时间
            Date expiresAt = jwt.getExpiresAt();
            if (expiresAt == null) {
                log.warn("缺少过期时间声明");
                return false;
            }

            Date now = new Date();
            if (expiresAt.before(new Date(now.getTime() - CLOCK_SKEW * 1000))) {
                log.warn("Token已过期: expiresAt={}, now={}", expiresAt, now);
                return false;
            }

            // 验证生效时间（可选）
            Date notBefore = jwt.getNotBefore();
            if (notBefore != null && notBefore.after(new Date(now.getTime() + CLOCK_SKEW * 1000))) {
                log.warn("Token尚未生效: notBefore={}, now={}", notBefore, now);
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
     * 验证签名
     */
    private boolean verifySignature(DecodedJWT jwt) {
        try {
            String keyId = jwt.getKeyId();
            if (keyId == null || keyId.isEmpty()) {
                log.warn("JWT中缺少Key ID");
                return false;
            }

            log.debug("获取公钥，Key ID: {}", keyId);

            // 从Apple JWKS端点获取公钥
            Jwk jwk = jwkProvider.get(keyId);
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            if (publicKey == null) {
                log.warn("无法获取公钥");
                return false;
            }

            // 5. 创建验证器并验证签名
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(APPLE_ISSUER)
                    .withAudience(APPLE_AUDIENCE)
                    .acceptLeeway(CLOCK_SKEW)
                    .build();

            // 验证签名
            verifier.verify(jwt.getToken());

            log.debug("签名验证成功");
            return true;

        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }

    /**
     * 从验证通过的JWT中提取通知数据
     */
    public JsonNode extractNotificationData(String signedPayload) {
        try {
            DecodedJWT decodedJWT = JWT.decode(signedPayload);
            String payload = new String(java.util.Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.error("提取通知数据异常", e);
            return null;
        }
    }

}

