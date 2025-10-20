package com.jjg.game.recharge.controller;

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.core.data.ThirdServiceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/9/22 19:32
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "googlepay")
public class GoogleCallbackController extends AbstractCallbackController{

    private final ThirdServiceInfo thirdServiceInfo;

    private final String ISSUER = "https://accounts.google.com";
    private final long CLOCK_SKEW = 300000; // 5分钟时钟容差

    private JwkProvider jwkProvider;

    public GoogleCallbackController(ThirdServiceInfo thirdServiceInfo) throws MalformedURLException {
        this.thirdServiceInfo = thirdServiceInfo;

        URL url = new URL(thirdServiceInfo.getGoogleJwksUrl());
        this.jwkProvider = new JwkProviderBuilder(url)
                .cached(10, 12, TimeUnit.HOURS) // 缓存10个密钥，12小时
                .rateLimited(10, 1, TimeUnit.MINUTES) // 限流：每分钟10次
                .build();
    }

    /**
     * 回调
     *
     * @return
     */
    @RequestMapping("callback")
    public ResponseEntity<String> callback(@RequestBody Map<String, Object> payload,
                                           @RequestHeader("Authorization") String authHeader) {
        try{
            log.debug("收到谷歌充值回调 payload = {}", payload);
            // ===== 1. 安全验证=====
            if(!verifyJwtToken(authHeader, thirdServiceInfo.getGoogleAud())){
                return ResponseEntity.status(403).body("Invalid Authorization header");
            }

            // ===== 2. 解码通知数据 =====
            String base64Data = ((Map<String, Object>) payload.get("message")).get("data").toString();
            String decodedData = new String(Base64.getDecoder().decode(base64Data));
            JsonNode jsonNode = objectMapper.readTree(decodedData);

            log.debug("Decoded notification: " + jsonNode.toString());

            //订阅相关
            JsonNode subscriptionNotificationNode = jsonNode.get("subscriptionNotification");
            if(subscriptionNotificationNode != null){
                log.debug("收到订阅通知 notification = {}", subscriptionNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            //一次性购买
            JsonNode oneTimeProductNotificationNode = jsonNode.get("oneTimeProductNotification");
            if(oneTimeProductNotificationNode != null){
                log.debug("收到一次性购买通知 notification = {}", oneTimeProductNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            //作废的购买交易
            JsonNode voidedPurchaseNotificationNode = jsonNode.get("voidedPurchaseNotification");
            if(voidedPurchaseNotificationNode != null){
                log.debug("收到作废的购买交易通知 notification = {}", voidedPurchaseNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            //测试通知
            JsonNode testNotificationNode = jsonNode.get("testNotification");
            if(testNotificationNode != null){
                log.debug("收到测试通知 notification = {}", testNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            //处理充值回调逻辑
//            payCallback(order);
            return ResponseEntity.ok("Recharge processed");
        }catch (Exception e){
            log.error("",e);
            return ResponseEntity.status(500).body("Error processing callback");
        }
    }


    /**
     * 验证 Google Pub/Sub 的 JWT Token
     */
    public boolean verifyJwtToken(String authorizationHeader, String expectedAudience) {
        try {
            log.debug("authorizationHeader = {}",authorizationHeader);
            // 1. 提取 Bearer Token
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.debug("authorizationHeader 错误,  authorizationHeader = {}", authorizationHeader);
                return false;
            }
            String jwtToken = authorizationHeader.substring(7);

            // 2. 解码
            DecodedJWT decodedJWT = JWT.decode(jwtToken);

            // 加密算法
            if (!"RS256".equals(decodedJWT.getAlgorithm())) {
                log.debug("加密算法不匹配 algorithm = {}", decodedJWT.getAlgorithm());
                return false;
            }

            // 3. 验证基本声明
            if (!verifyBasicClaims(decodedJWT, expectedAudience)) {
                return false;
            }

            // 4. 从 Google JWKS 端点获取公钥
            String keyId = decodedJWT.getKeyId();
            log.debug("kid = {}", keyId);
            RSAPublicKey publicKey = getPublicKeyFromJwks(keyId);

            if (publicKey == null) {
                log.debug("获取公钥失败");
                return false;
            }

            // 5. 创建验证器并验证签名
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = com.auth0.jwt.JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(expectedAudience)
                    .acceptLeeway(CLOCK_SKEW) // 接受时钟偏差
                    .build();

            // 验证签名和过期时间
            verifier.verify(jwtToken);

            // 6. 验证通过，可以解析 payload 获取更多信息
            String payload = new String(java.util.Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            log.debug("JWT Payload: " + payload);

            return true;

        } catch (Exception e) {
            log.error("验证google jwt 异常",e);
            return false;
        }
    }

    /**
     * 验证 JWT 的基本声明
     */
    private boolean verifyBasicClaims(DecodedJWT jwt, String expectedAudience) {
        try {
            // 验证颁发者
            if (!ISSUER.equals(jwt.getIssuer())) {
                log.debug("issuer 不匹配, issuer = {}", jwt.getIssuer());
                return false;
            }

            // 验证受众（应该是您的推送端点 URL）
            if (!jwt.getAudience().contains(expectedAudience)) {
                log.debug("aud 不匹配 aud = {}", jwt.getAudience());
                return false;
            }

            // 验证过期时间
            Date expiresAt = jwt.getExpiresAt();
            if (expiresAt == null || expiresAt.before(new Date())) {
                log.debug("已过期 expiresAt = {}", expiresAt);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("验证 JWT 的基本声明 异常 ",e);
            return false;
        }
    }

    /**
     * 从 Google JWKS 端点获取公钥
     */
    private RSAPublicKey getPublicKeyFromJwks(String keyId) {
        try {
            Jwk jwk = this.jwkProvider.get(keyId);
            return (RSAPublicKey) jwk.getPublicKey();
        } catch (Exception e) {
            log.error("",e);
            return null;
        }
    }
}
