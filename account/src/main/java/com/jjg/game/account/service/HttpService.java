package com.jjg.game.account.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.account.data.*;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.VerCodeDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.VerCodeType;
import com.jjg.game.core.data.ThirdServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;


/**
 * @author 11
 * @date 2025/10/13 14:27
 */
@Service
public class HttpService {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final ThirdServiceInfo thirdServiceInfo;
    @Autowired
    private VerCodeDao verCodeDao;

    private final JwkProvider appleJwkProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpService(ThirdServiceInfo thirdServiceInfo) {
        this.thirdServiceInfo = thirdServiceInfo;

        appleJwkProvider = new JwkProviderBuilder(thirdServiceInfo.getAppleJwksUrl())
                .cached(10, 12, TimeUnit.HOURS) // 缓存10个公钥，12小时
                .rateLimited(true)
                .build();
    }

    /**
     * 验证google token
     *
     * @param token
     * @return
     */
    public CommonResult<GoogleUserInfo> verifyGoogleToken(String token) {
        CommonResult<GoogleUserInfo> result = new CommonResult<>(Code.SUCCESS);

        try {
            HttpRequest httpRequest = HttpRequest.get(thirdServiceInfo.getGoogleVerifyUrl() + token);
//            httpRequest.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("192.168.3.46", 32649)));

            HttpResponse resp = httpRequest.execute();
            String body = resp.body();

            // 解析返回的JSON
            JsonNode jsonNode = objectMapper.readTree(body);

            log.info("返回google信息 json = {}", jsonNode.toString());
            if (!resp.isOk()) {
                result.code = Code.FAIL;
                log.warn("验证google token失败 token = {},error = {}", token, jsonNode.get("error").asText());
                return result;
            }

            // 1. 校验iss（签发者）
            String iss = jsonNode.get("iss").asText();
            if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
                result.code = Code.FAIL;
                log.warn("无效的签发者 token = {},iss = {}", token, iss);
                return result;
            }

            // 2. 校验aud（受众）
            String aud = jsonNode.get("aud").asText();
            if (!thirdServiceInfo.getGoogleClientId().equals(aud)) {
                result.code = Code.FAIL;
                log.warn("无效的受众 token = {},aud = {}", token, aud);
                return result;
            }

            // 3. 校验exp（过期时间）
            int exp = jsonNode.get("exp").asInt();
            int currentTime = TimeHelper.nowInt(); // 当前时间戳（秒级）
            if (currentTime > exp) {
                result.code = Code.FAIL;
                log.warn("Token已过期 token = {},exp = {}", token, exp);
                return result;
            }

            // 4. 校验iat（签发时间，可选但建议）
            long iat = jsonNode.get("iat").asLong();
            if (currentTime < iat) { // 签发时间不能晚于当前时间（防止未来的Token）
                result.code = Code.FAIL;
                log.warn("Token签发时间异常 token = {},iat = {}", token, iat);
                return result;
            }

            // 5. 校验sub不为空（可选）
            if (jsonNode.get("sub") == null || jsonNode.get("sub").asText().isEmpty()) {
                result.code = Code.FAIL;
                log.warn("用户ID为空 token = {}", token);
                return result;
            }

            // 将JSON转换为用户信息实体类
            GoogleUserInfo userInfo = new GoogleUserInfo();
            userInfo.setUserId(jsonNode.get("sub").asText());
            userInfo.setEmail(jsonNode.get("email").asText());
            userInfo.setEmailVerified(jsonNode.get("email_verified").asBoolean());
            result.data = userInfo;
            return result;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 验证apple token
     *
     * @param token
     * @return
     */
    public CommonResult<AppleUserInfo> verifyAppleToken(String token) {
        CommonResult<AppleUserInfo> result = new CommonResult<>(Code.SUCCESS);

        try {
            // 解析 Token Header，获取 kid
            DecodedJWT tempJwt = JWT.decode(token);
            String kid = tempJwt.getHeaderClaim("kid").asString();

            // 获取对应公钥
            Jwk jwk = this.appleJwkProvider.get(kid);
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            if (publicKey == null) {
                result.code = Code.FAIL;
                return result;
            }
            // 验证
            DecodedJWT jwt = JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer("https://appleid.apple.com")
                    .withAudience(thirdServiceInfo.getAppleClientId())
                    .withClaimPresence("sub")
                    .build()
                    .verify(token);

            // 8. 填充用户信息
            AppleUserInfo userInfo = new AppleUserInfo();
            userInfo.setUserId(jwt.getSubject());

            result.data = userInfo;
            return result;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 验证facebook token
     *
     * @param token
     * @return
     */
    public CommonResult<FacebookUserInfo> verifyFacebookToken(String token) {
        CommonResult<FacebookUserInfo> result = new CommonResult<>(Code.SUCCESS);

        try {
            // 验证token
            HttpRequest httpRequest = HttpRequest.get(thirdServiceInfo.getFacebookDebugTokenUrl());
            httpRequest.body("input_token", token);
            httpRequest.body("access_token", thirdServiceInfo.getFacebookAppId() + "|" + thirdServiceInfo.getFacebookSecret());
            HttpResponse resp = httpRequest.execute();
            String body = resp.body();

            // 解析返回的JSON
            JsonNode debugJsonNode = objectMapper.readTree(body);

            log.info("返回facebook debug信息 json = {}", debugJsonNode.toString());
            if (!resp.isOk()) {
                result.code = Code.FAIL;
                JsonNode errorJsonNode = debugJsonNode.get("error");
                if (errorJsonNode != null) {
                    log.warn("验证facebook token失败 token = {},msg = {}", token, errorJsonNode.get("message").asText());
                } else {
                    log.warn("验证facebook token失败 token = {}", token);
                }
                log.warn("验证facebook token失败 token = {}", token);
                return result;
            }

            JsonNode data = debugJsonNode.get("data");
            if (!data.get("is_valid").asBoolean()) {
                result.code = Code.FAIL;
                log.warn("验证facebook token失败 token = {}", token);
                return result;
            }

            // 验证应用 ID
            if (!data.get("app_id").asText().equals(thirdServiceInfo.getFacebookAppId())) {
                result.code = Code.FAIL;
                log.warn("facebook appid验证失败， token = {}", token);
                return result;
            }

            // 检查 token 是否过期
            if (data.has("expires_at")) {
                long expiresAt = data.get("expires_at").asLong();
                if (TimeHelper.nowInt() > expiresAt) {
                    result.code = Code.FAIL;
                    log.warn("facebook Token已过期 token = {},exp = {}", token, expiresAt);
                    return result;
                }
            }

            //获取用户信息
            HttpRequest userInfohttpRequest = HttpRequest.get(thirdServiceInfo.getFacebookUserInfoUrl() + token).timeout(30000);
            HttpResponse userInfoResp = userInfohttpRequest.execute();
            String userInfoBody = userInfoResp.body();

            // 解析返回的JSON
            JsonNode userInfoJsonNode = objectMapper.readTree(userInfoBody);
            log.info("返回facebook userinfo json = {}", userInfoJsonNode.toString());

            if (!userInfoResp.isOk()) {
                result.code = Code.FAIL;
                JsonNode errorJsonNode = userInfoJsonNode.get("error");
                if (errorJsonNode != null) {
                    log.warn("获取facebook用户信息失败 token = {},msg = {}", token, errorJsonNode.get("message").asText());
                } else {
                    log.warn("获取facebook用户信息失败 token = {}", token);
                }
                return result;
            }

            FacebookUserInfo userInfo = new FacebookUserInfo();
            userInfo.setUserId(userInfoJsonNode.get("id").asText());

            result.data = userInfo;
            return result;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 验证登录短信
     *
     * @return
     */
    public CommonResult<PhoneUserInfo> verifyPhoneLoginCode(String phone, int code) {
        CommonResult<PhoneUserInfo> result = new CommonResult<>(Code.SUCCESS);

        try {
            CommonResult<String> verResult = verCodeDao.verifyVerCode(phone, VerCodeType.SMS_LOGIN, code);
            if (!verResult.success()) {
                result.code = verResult.code;
                return result;
            }

            PhoneUserInfo userInfo = new PhoneUserInfo();
            userInfo.setUserId(phone);
            result.data = userInfo;
            return result;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }
}
