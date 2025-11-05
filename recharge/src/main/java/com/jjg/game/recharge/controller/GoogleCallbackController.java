package com.jjg.game.recharge.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.ThirdServiceInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/9/22 19:32
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "googlepay")
public class GoogleCallbackController extends AbstractCallbackController {

    private final ThirdServiceInfo thirdServiceInfo;

    private final String ISSUER = "https://accounts.google.com";
    //获取交易详情的连接
    private final String PRODUCT_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s/purchases/products/%s/tokens/%s";
    //获取token的连接
    private final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final String JWT_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private final String JWT_AUD = "https://oauth2.googleapis.com/token";


    private final long CLOCK_SKEW = 300000; // 5分钟时钟容差
    private JwkProvider jwkProvider;

    //服务账号
    private final String serviceAccountEmail;
    private final RSA rsa;
    private final String privateKeyPem;

    //缓存的accessToken
    private String accessToken;
    private int expiresTime;

    public GoogleCallbackController(ThirdServiceInfo thirdServiceInfo) throws MalformedURLException {
        this.thirdServiceInfo = thirdServiceInfo;

        URL url = new URL(thirdServiceInfo.getGoogleJwksUrl());
        this.jwkProvider = new JwkProviderBuilder(url)
                .cached(10, 12, TimeUnit.HOURS) // 缓存10个密钥，12小时
                .rateLimited(10, 1, TimeUnit.MINUTES) // 限流：每分钟10次
                .build();


        File file = new File("config/google-service-account.json");
        String jsonContent = FileUtil.readUtf8String(file.getAbsolutePath());
        JSONObject googleServiceAccount = JSON.parseObject(jsonContent);

        this.serviceAccountEmail = googleServiceAccount.getString("client_email");
        this.privateKeyPem = googleServiceAccount.getString("private_key"); // 保留原始格式

        String cleanedPrivateKey = this.privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\s", "")
                .trim();
        byte[] privateKeyBytes = Base64.getDecoder().decode(cleanedPrivateKey);
        this.rsa = new RSA(privateKeyBytes, null); // 使用字节数组初始化
    }

    /**
     * 回调
     *
     * @return
     */
    @RequestMapping("callback")
    public ResponseEntity<String> callback(@RequestBody Map<String, Object> payload,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            log.debug("收到谷歌充值回调 payload = {}", payload);
            // ===== 1. 安全验证=====
            if (!verifyJwtToken(authHeader, thirdServiceInfo.getGoogleAud())) {
                return ResponseEntity.status(403).body("Invalid Authorization header");
            }

            // ===== 2. 解码通知数据 =====
            String base64Data = ((Map<String, Object>) payload.get("message")).get("data").toString();
            String decodedData = new String(Base64.getDecoder().decode(base64Data));
            JsonNode jsonNode = objectMapper.readTree(decodedData);

            log.debug("Decoded notification: " + jsonNode.toString());

            //订阅相关
            JsonNode subscriptionNotificationNode = jsonNode.get("subscriptionNotification");
            if (subscriptionNotificationNode != null) {
                log.debug("收到订阅通知 notification = {}", subscriptionNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            //一次性购买
            JsonNode oneTimeProductNotificationNode = jsonNode.get("oneTimeProductNotification");
            if (oneTimeProductNotificationNode != null) {
                return handleOneTimeProductNotification(jsonNode, oneTimeProductNotificationNode);
            }

            //作废的购买交易
            JsonNode voidedPurchaseNotificationNode = jsonNode.get("voidedPurchaseNotification");
            if (voidedPurchaseNotificationNode != null) {
                log.debug("收到作废的购买交易通知 notification = {}", voidedPurchaseNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            //测试通知
            JsonNode testNotificationNode = jsonNode.get("testNotification");
            if (testNotificationNode != null) {
                log.debug("收到测试通知 notification = {}", testNotificationNode);
                return ResponseEntity.ok("Recharge processed");
            }

            return ResponseEntity.ok("Recharge processed");
        } catch (Exception e) {
            log.error("", e);
            return ResponseEntity.status(500).body("Error processing callback");
        }
    }

    /**
     * 一次性购买
     *
     * @param jsonNode
     * @param oneTimeProductNotificationNode
     * @return
     */
    private ResponseEntity<String> handleOneTimeProductNotification(JsonNode jsonNode, JsonNode oneTimeProductNotificationNode) {
        log.debug("收到一次性购买通知 notification = {}", oneTimeProductNotificationNode);
        String purchaseToken = oneTimeProductNotificationNode.get("purchaseToken").asText();
        String sku = oneTimeProductNotificationNode.get("sku").asText();
        String packageName = jsonNode.get("packageName").asText();

        JSONObject productInfoJson = getProductInfo(purchaseToken, packageName, sku);
        log.debug("商品信息 productInfoJson = {}", productInfoJson);
        if (productInfoJson == null) {
            return ResponseEntity.ok("get product fail");
        }

        String orderId = productInfoJson.get("obfuscatedExternalProfileId").toString();
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            log.debug("未找到该订单 orderId = {}", orderId);
            return ResponseEntity.ok("not found order");
        }

        String channelOrderId = productInfoJson.getString("orderId");
        order.setChannelOrderId(channelOrderId);
        order = checkOrder(order);
        if (order == null) {
            log.debug("检查订单失败 orderId = {}", orderId);
            return ResponseEntity.ok("check order fail");
        }

        String regionCode = productInfoJson.getString("regionCode");
        payCallback(order, null, regionCode);
        return ResponseEntity.ok("Recharge processed");
    }


    /**
     * 验证 Google Pub/Sub 的 JWT Token
     */
    public boolean verifyJwtToken(String authorizationHeader, String expectedAudience) {
        try {
            log.debug("authorizationHeader = {}", authorizationHeader);
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
            log.error("验证google jwt 异常", e);
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
            log.error("验证 JWT 的基本声明 异常 ", e);
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
            log.error("", e);
            return null;
        }
    }

    /**
     * 生成jwt
     *
     * @return
     */
    private String generateJwt() {
        long now = System.currentTimeMillis() / 1000;
        long expiry = now + TimeUnit.HOURS.toSeconds(1);

        // 构建 JWT Header
        Map<String, String> header = new HashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        // 构建 JWT Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", serviceAccountEmail);
        payload.put("scope", JWT_SCOPE);
        payload.put("aud", JWT_AUD);
        payload.put("exp", expiry);
        payload.put("iat", now);

        // 编码 Header 和 Payload
        String headerBase64 = StrUtil.removeAllLineBreaks(
                Base64.getUrlEncoder().encodeToString(JSON.toJSONBytes(header)));
        String payloadBase64 = StrUtil.removeAllLineBreaks(
                Base64.getUrlEncoder().encodeToString(JSON.toJSONBytes(payload)));

        // 使用 RSA 签名
        String dataToSign = headerBase64 + "." + payloadBase64;
        Sign sign = new Sign(SignAlgorithm.SHA256withRSA, rsa.getPrivateKey(), null);
        byte[] signatureBytes = sign.sign(dataToSign.getBytes());
        String signatureBase64 = StrUtil.removeAllLineBreaks(
                Base64.getUrlEncoder().encodeToString(signatureBytes));

        return headerBase64 + "." + payloadBase64 + "." + signatureBase64;
    }

    /**
     * 获取access_token
     *
     * @return
     */
    private String getAccessToken() {
        int now = TimeHelper.nowInt();
        if (!StringUtils.isEmpty(accessToken) && expiresTime > now) {
            return accessToken;
        }

        String jwt = generateJwt();
        HttpRequest request = HttpRequest.post(TOKEN_URL);

        HttpResponse response = request
                .form("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .form("assertion", jwt)
                .execute();

        if (response.isOk()) {
            JSONObject tokenResponse = JSON.parseObject(response.body());

            String accessToken = tokenResponse.getString("access_token");

            this.accessToken = accessToken;
            this.expiresTime = now + tokenResponse.getIntValue("expires_in") - 300;
            return accessToken;
        } else {
            throw new RuntimeException("Failed to get access token: " + response.body());
        }
    }

    /**
     * 获取商品信息
     *
     * @param purchaseToken
     * @param packageName
     * @param prodctId
     * @return
     */
    private JSONObject getProductInfo(String purchaseToken, String packageName, String prodctId) {
        String accessToken = getAccessToken();

        String url = String.format(this.PRODUCT_URL, packageName, prodctId, purchaseToken);
        HttpRequest request = HttpRequest.get(url);
        HttpResponse response = request
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .execute();

        if (response.isOk()) {
            return JSON.parseObject(response.body());
        }

        log.warn("获取商品信息失败 resp = {}", JSON.parseObject(response.body()));
        return null;
    }
}
