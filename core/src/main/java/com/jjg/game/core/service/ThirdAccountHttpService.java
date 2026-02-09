package com.jjg.game.core.service;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.core.data.*;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ThirdServiceInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.UndergarmentCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author 11
 * @date 2025/10/13 14:27
 */
@Service
public class ThirdAccountHttpService {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final ThirdServiceInfo thirdServiceInfo;

    private final JwkProvider appleJwkProvider;

    private final AdjustConfig adjustConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final long MAX_TOKEN_AGE = 3600L; // 最大token年龄1小时

    private final String ADJUST_URL = "https://api.adjust.com/device_service/api/v1/inspect_device";

    public ThirdAccountHttpService(ThirdServiceInfo thirdServiceInfo, AdjustConfig adjustConfig) {
        this.thirdServiceInfo = thirdServiceInfo;
        this.adjustConfig = adjustConfig;

        if (this.thirdServiceInfo != null && StringUtils.isNotEmpty(thirdServiceInfo.getAppleJwksUrl())) {
            appleJwkProvider = new JwkProviderBuilder(thirdServiceInfo.getAppleJwksUrl())
                    .cached(10, 12, TimeUnit.HOURS) // 缓存10个公钥，12小时
                    .rateLimited(true)
                    .build();
        } else {
            this.appleJwkProvider = null;
        }
    }

    /**
     * 验证google token
     *
     * @param westeId 马甲id
     * @param token
     * @return
     */
    public CommonResult<GoogleUserInfo> verifyGoogleToken(int westeId, String token) {
        CommonResult<GoogleUserInfo> result = new CommonResult<>(Code.SUCCESS);

        try {
            String clientId;
            if (westeId > 0) {
                UndergarmentCfg undergarmentCfg = GameDataManager.getUndergarmentCfg(westeId);
                if (undergarmentCfg == null) {
                    result.code = Code.SAMPLE_ERROR;
                    log.warn("获取马甲配置为空 westeId = {}", westeId);
                    return result;
                }
                clientId = undergarmentCfg.getGoogleClientID();
            } else {
                clientId = thirdServiceInfo.getGoogleClientId();
            }

            if (StringUtils.isBlank(clientId)) {
                result.code = Code.FAIL;
                log.warn("google 配置中的 clientId 为空,westeId = {}", westeId);
                return result;
            }

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
            if (!clientId.equals(aud)) {
                result.code = Code.FAIL;
                log.warn("无效的受众 token = {},aud = {},cfgClientId = {}", token, aud, clientId);
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
            if (thirdServiceInfo.getAppleAppId() == null || StringUtils.isBlank(thirdServiceInfo.getAppleBundleId())) {
                result.code = Code.FAIL;
                log.warn("apple配置缺失 appleId = {},appleBundleId = {}", thirdServiceInfo.getAppleAppId(), thirdServiceInfo.getAppleBundleId());
                return result;
            }

            // 解析 Token Header，获取 kid
            DecodedJWT tempJwt = JWT.decode(token);
            String kid = tempJwt.getHeaderClaim("kid").asString();

            //解码预校验
            DecodedJWT decodedJWT = JWT.decode(token);
            if (!"https://appleid.apple.com".equals(decodedJWT.getIssuer())) {
                result.code = Code.FORBID;
                log.warn("非Apple发行的Token = {}", token);
                return result;
            }
            if (!decodedJWT.getAudience().contains(thirdServiceInfo.getAppleBundleId())) {
                result.code = Code.FORBID;
                log.warn("受众不匹配 configAud = {},jwtAud = {},token = {}", thirdServiceInfo.getAppleBundleId(), decodedJWT.getAudience(), token);
                return result;
            }
            Date now = new Date();
            if (decodedJWT.getExpiresAt().before(now)) {
                result.code = Code.EXPIRE;
                log.warn("apple token已过期 token = {}", token);
                return result;
            }
            if (decodedJWT.getIssuedAt() != null) {
                long tokenAge = (now.getTime() - decodedJWT.getIssuedAt().getTime()) / 1000;
                if (tokenAge > MAX_TOKEN_AGE) {
                    result.code = Code.FORBID;
                    log.warn("token签发时间过久 issuedAt = {},token = {}", decodedJWT.getIssuedAt().getTime(), token);
                    return result;
                }
            }

            // 获取对应公钥
            Jwk jwk = this.appleJwkProvider.get(kid);
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            if (publicKey == null) {
                result.code = Code.FAIL;
                log.warn("获取公钥失败 kid = {},token = {}", kid, token);
                return result;
            }

            // 验证
            DecodedJWT jwt = JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer("https://appleid.apple.com")
                    .withAudience(thirdServiceInfo.getAppleBundleId())
                    .withClaimPresence("sub")
                    .withClaimPresence("exp")
                    .withClaimPresence("iat")
                    .acceptLeeway(60)
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
            if (StringUtils.isBlank(thirdServiceInfo.getFacebookAppId()) || StringUtils.isBlank(thirdServiceInfo.getFacebookSecret())) {
                result.code = Code.FAIL;
                log.warn("facebook 配置缺失 facebookAppId = {},facebookSecret is null={}", thirdServiceInfo.getFacebookAppId(), StringUtils.isBlank(thirdServiceInfo.getFacebookSecret()));
                return result;
            }

            // 验证token
            HttpRequest httpRequest = HttpRequest.get(thirdServiceInfo.getFacebookDebugTokenUrl());
//            httpRequest.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("192.168.3.46", 32649)));

            Map<String, Object> params = new HashMap<>();
            params.put("input_token", token);
            params.put("access_token", thirdServiceInfo.getFacebookAppId() + "|" + thirdServiceInfo.getFacebookSecret());
            HttpResponse resp = httpRequest.form(params).execute();
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
                log.warn("facebook appid验证失败， token = {},appId = {},cfgAppId = {}", token, data.get("app_id").asText(), thirdServiceInfo.getFacebookAppId());
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

            int now = TimeHelper.nowInt();
            //签发时间
            if (data.get("issued_at").asInt() > now) {
                result.code = Code.FAIL;
                log.warn("该token未生效 token = {}，issued_at = {}", token, data.get("issued_at"));
                return result;
            }

            //过期时间
            if (data.get("expires_at").asInt() < now) {
                result.code = Code.FAIL;
                log.warn("该token已过期 token = {}，expires_at = {}", token, data.get("expires_at"));
                return result;
            }

            FacebookUserInfo userInfo = new FacebookUserInfo();
            userInfo.setUserId(data.get("user_id").asText());

            result.data = userInfo;
            return result;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 检查是否要切换服务器
     *
     * @param adid
     * @return
     */
    public boolean checkSwitchServerByAdid(String adid) {
        if (this.adjustConfig == null || !this.adjustConfig.isOpen() || StringUtils.isBlank(adid)) {
            return false;
        }

        if (StringUtils.isBlank(this.adjustConfig.getApiToken()) || StringUtils.isBlank(this.adjustConfig.getAppToken())) {
            log.warn("adjust配置为空");
            return false;
        }

        try {
            HttpRequest httpRequest = HttpRequest.get(ADJUST_URL);

            httpRequest.header(Header.AUTHORIZATION, "Bearer " + this.adjustConfig.getApiToken());

            httpRequest.form("app_token", this.adjustConfig.getAppToken());
            httpRequest.form("advertising_id", adid);

            httpRequest.timeout(10000);

            HttpResponse resp = httpRequest.execute();
            String body = resp.body();
            if (resp.isOk()) {
                JSONObject json = JSONUtil.parseObj(body);
                if ("Organic".equals(json.getStr("TrackerName"))) {
                    return true;
                }
            } else {
                log.warn("从adjust获取信息失败 adid = {},body = {}", adid, body);
            }
            return false;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }
}
