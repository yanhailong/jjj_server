package com.jjg.game.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.GoogleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author 11
 * @date 2025/10/13 14:27
 */
@Service
public class HttpService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GoogleInfo googleInfo;


    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证google token
     * @param token
     * @return
     */
    public boolean verifyGoogleToken(String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // 调用Google验证接口
            ResponseEntity<String> response = restTemplate.exchange(
                    googleInfo.getVerifyUrl() + token,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                return false; // 接口返回非200，直接失败
            }

            // 解析返回的JSON
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // 1. 校验iss（签发者）
            String iss = jsonNode.get("iss").asText();
            if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
                System.out.println("无效的签发者：" + iss);
                return false;
            }

            // 2. 校验aud（受众）
            String aud = jsonNode.get("aud").asText();
            if (!googleInfo.getClientId().equals(aud)) {
                System.out.println("无效的受众：" + aud);
                return false;
            }

            // 3. 校验exp（过期时间）
            int exp = jsonNode.get("exp").asInt();
            int currentTime = TimeHelper.nowInt(); // 当前时间戳（秒级）
            if (currentTime > exp) {
                System.out.println("Token已过期");
                return false;
            }

            // 4. 校验iat（签发时间，可选但建议）
            long iat = jsonNode.get("iat").asLong();
            if (currentTime < iat) { // 签发时间不能晚于当前时间（防止未来的Token）
                System.out.println("Token签发时间异常");
                return false;
            }

            // 5. 校验sub不为空（可选）
            if (jsonNode.get("sub") == null || jsonNode.get("sub").asText().isEmpty()) {
                System.out.println("用户ID为空");
                return false;
            }

            // 所有校验通过
            return true;
        } catch (Exception e) {
            log.error("",e);
        }
        return false;
    }
}
