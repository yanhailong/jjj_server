package com.jjg.game.recharge.controller;

import com.alibaba.fastjson.JSON;
import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.*;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.recharge.dto.AppleValidateDto;
import com.jjg.game.recharge.vo.AppleValidateVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/10/16 11:07
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "applepay")
public class AppleCallbackController extends AbstractCallbackController {

    // Apple相关常量
    private static final String APPLE_ISSUER = "appstoreconnect-v1";
    private static final String APPLE_AUDIENCE = "appstoreconnect-v1";

    private static final long CLOCK_SKEW = 300; // 5分钟时钟偏差

    private final SignedDataVerifier verifier;

    private final AppStoreServerAPIClient client;

    private final JwkProvider jwkProvider;

    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;

    public AppleCallbackController(ThirdServiceInfo thirdServiceInfo) throws Exception {

        try {
            // 创建JWK提供者，用于获取Apple的公钥
            this.jwkProvider = new JwkProviderBuilder(thirdServiceInfo.getAppleJwksUrl())
                    .cached(10, 24, TimeUnit.HOURS) // 缓存10个密钥，24小时
                    .rateLimited(10, 1, TimeUnit.MINUTES) // 限流：每分钟10次
                    .build();

            Set<InputStream> certificates = new HashSet<>();
            certificates.add(new FileInputStream("config/AppleRootCA-G2.cer"));
            certificates.add(new FileInputStream("config/AppleRootCA-G3.cer"));

            Path subKeyfilePath = Path.of("config/SubscriptionKey.p8");
            String subKeyEncodedKey = Files.readString(subKeyfilePath);

            if (thirdServiceInfo.isSandbox()) {
                this.verifier = new SignedDataVerifier(certificates, thirdServiceInfo.getAppleBundleId(), thirdServiceInfo.getAppleAppId(), Environment.SANDBOX, true);
                this.client = new AppStoreServerAPIClient(subKeyEncodedKey, thirdServiceInfo.getAppleKeyId(), thirdServiceInfo.getAppleIssuerId(), thirdServiceInfo.getAppleBundleId(), Environment.SANDBOX);
            } else {
                this.verifier = new SignedDataVerifier(certificates, thirdServiceInfo.getAppleBundleId(), thirdServiceInfo.getAppleAppId(), Environment.PRODUCTION, true);
                this.client = new AppStoreServerAPIClient(subKeyEncodedKey, thirdServiceInfo.getAppleKeyId(), thirdServiceInfo.getAppleIssuerId(), thirdServiceInfo.getAppleBundleId(), Environment.PRODUCTION);
            }

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 接收App Store Server Notifications V2格式的回调通知
     * @param rawBody
     * @return
     * @throws Exception
     */
    @RequestMapping("callback")
    public ResponseEntity<String> callback(@RequestBody String rawBody) throws Exception {
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

    /**
     * 回调
     *
     * @return
     */
    @RequestMapping("validate")
    public WebResult<AppleValidateVo> validate(@RequestBody AppleValidateDto dto) {
        try {
            log.debug("收到apple充值验证 dto = {}", JSON.toJSONString(dto));

            //检查参数
            if (StringUtils.isEmpty(dto.getPlayerToken()) || dto.getPlayerId() < 1) {
                log.debug("参数 playerToken、playerId 不能为空，apple充值失败 dto = {}", JSON.toJSONString(dto));
                return new WebResult<>(Code.FAIL, "参数 playerToken、playerId 不能为空");
            }

            // 提取 jws
            String jwsRepresentation = dto.getJwsRepresentation();
            String transactionId = dto.getTransactionId();
            if (StringUtils.isEmpty(transactionId) || StringUtils.isEmpty(jwsRepresentation)) {
                log.debug("参数 transactionId、jwsRepresentation 不能为空，apple充值失败 dto = {}", JSON.toJSONString(dto));
                return new WebResult<>(Code.FAIL, "参数 transactionId、jwsRepresentation 不能为空");
            }

            //检查rechargeType
            RechargeType rechargeType = RechargeType.valueOf(dto.rechargeType);
            if (rechargeType == null || StringUtils.isEmpty(dto.getProductId())) {
                log.debug("参数 rechargeType、productId 不能为空，apple充值失败 dto = {}", JSON.toJSONString(dto));
                return new WebResult<>(Code.FAIL, "参数 rechargeType、productId 不能为空，apple充值失败");
            }

            //本地校验
            boolean verify = verify(transactionId, jwsRepresentation);
            if (!verify) {
                return new WebResult<>(Code.FAIL, "校验 jwt 失败");
            }

            //去apple服务器获取该交易信息
            TransactionInfoResponse transactionInfo = this.client.getTransactionInfo(transactionId);
            if (transactionInfo == null || StringUtils.isEmpty(transactionInfo.getSignedTransactionInfo())) {
                log.debug("从apple服务器获取的transactionInfo错误，apple充值失败 dto = {},transactionInfo = {}", JSON.toJSONString(dto), transactionInfo);
                return new WebResult<>(Code.FAIL, "从apple服务器获取的transactionInfo错误");
            }

            log.debug("从apple服务器成功获取交易信息 jwsRepresentation = {}", transactionInfo.getSignedTransactionInfo());
            //从apple获取的订单信息，进行校验
            verify = verify(transactionId, transactionInfo.getSignedTransactionInfo());
            if (!verify) {
                return new WebResult<>(Code.FAIL, "从apple服务器获取的信息 校验 jwt 失败");
            }

            //获取商品价格
            BigDecimal productPrice = getProductPrice(rechargeType, dto.getProductId());
            if (productPrice == null) {
                log.debug("获取商品价格失败 dto = {}", JSON.toJSONString(dto));
                return new WebResult<>(Code.FAIL, "获取商品价格失败");
            }

            //获取redis中的token信息
            PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(dto.getPlayerId());
            if (playerSessionToken == null) {
                log.debug("获取玩家 playerSessionToken 失败 dto = {}", JSON.toJSONString(dto));
                return new WebResult<>(Code.FAIL, "获取玩家 playerSessionToken 失败");
            }

            //对比token
            if (!playerSessionToken.getToken().equals(dto.getPlayerToken())) {
                log.debug("玩家 token 校验失败 dto = {},dbToken = {}", JSON.toJSONString(dto), playerSessionToken.getToken());
                return new WebResult<>(Code.FAIL, "玩家 token 校验失败");
            }

            //生成订单
            Order order = orderService.generateOrder(dto.getPlayerId(), ChannelType.APPLE, PayType.IOS, dto.productId, productPrice, rechargeType, OrderStatus.SUCCESS, dto.getTransactionId());
            if (order == null) {
                log.debug("生成订单失败 dto = {}", JSON.toJSONString(dto));
                return new WebResult<>(Code.FAIL, "玩家 token 校验失败");
            }

            payCallback(order);
            log.info("ios充值成功 orderId = {},transactionId = {}", order.getId(), transactionId);
            return new WebResult<>(Code.SUCCESS);
        } catch (Exception e) {
            log.error("处理Apple充值回调异常", e);
            return new WebResult<>(Code.EXCEPTION);
        }
    }

    /**
     * 校验
     *
     * @param jwt
     * @return
     * @throws Exception
     */
    public boolean verify(String transactionId, String jwt) throws Exception {
        JWSTransactionDecodedPayload jwsPayload = this.verifier.verifyAndDecodeTransaction(jwt);
        log.debug("jwsPayload = {}", jwsPayload);

        // 检查 transactionId 一致性
        if (!transactionId.equals(jwsPayload.getTransactionId())) {
            log.debug("transactionId 不一致 req.transactionId = {},jws.transactionId = {},jwt = {} ", transactionId, jwsPayload.getTransactionId(), jwt);
            return false;
        }

        // 检查交易是否被撤销
        if (jwsPayload.getRevocationDate() != null) {
            log.debug("交易被撤销 date = {},jwt = {}", jwsPayload.getRevocationDate(), jwt);
            return false;
        }

        // 检查订阅是否过期
        if (jwsPayload.getExpiresDate() != null && jwsPayload.getExpiresDate() < System.currentTimeMillis()) {
            log.debug("订阅过期 jwt = {}", jwt);
            return false;
        }
        return true;
    }
}

