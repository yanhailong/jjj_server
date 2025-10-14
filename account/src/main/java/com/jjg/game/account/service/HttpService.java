package com.jjg.game.account.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.account.data.AppleUserInfo;
import com.jjg.game.account.data.FacebookUserInfo;
import com.jjg.game.account.data.GoogleUserInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.GoogleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Proxy;


/**
 * @author 11
 * @date 2025/10/13 14:27
 */
@Service
public class HttpService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GoogleInfo googleInfo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证google token
     *
     * @param token
     * @return
     */
    public CommonResult<GoogleUserInfo> verifyGoogleToken(String token) {
        CommonResult<GoogleUserInfo> result = new CommonResult<>(Code.SUCCESS);

        try {
            HttpRequest httpRequest = HttpRequest.get(googleInfo.getVerifyUrl() + token).timeout(30000);
            httpRequest.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("192.168.3.46", 32649)));

            HttpResponse resp = httpRequest.execute();
            String body = resp.body();

            // 解析返回的JSON
            JsonNode jsonNode = objectMapper.readTree(body);

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
            if (!googleInfo.getClientId().equals(aud)) {
                result.code = Code.FAIL;
                log.warn("无效的受众 token = {},aud = {}", token, aud);
                return null;
            }

            // 3. 校验exp（过期时间）
            int exp = jsonNode.get("exp").asInt();
            int currentTime = TimeHelper.nowInt(); // 当前时间戳（秒级）
            if (currentTime > exp) {
                result.code = Code.FAIL;
                log.warn("Token已过期 token = {},exp = {}", token, exp);
                return null;
            }

            // 4. 校验iat（签发时间，可选但建议）
            long iat = jsonNode.get("iat").asLong();
            if (currentTime < iat) { // 签发时间不能晚于当前时间（防止未来的Token）
                result.code = Code.FAIL;
                log.warn("Token签发时间异常 token = {},iat = {}", token, iat);
                return null;
            }

            // 5. 校验sub不为空（可选）
            if (jsonNode.get("sub") == null || jsonNode.get("sub").asText().isEmpty()) {
                result.code = Code.FAIL;
                log.warn("用户ID为空 token = {}", token);
                return null;
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
        return null;
    }

    /**
     * 验证facebook token
     *
     * @param token
     * @return
     */
    public CommonResult<FacebookUserInfo> verifyFacebookToken(String token) {
        return null;
    }
}
