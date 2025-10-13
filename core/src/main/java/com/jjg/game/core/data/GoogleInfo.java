package com.jjg.game.core.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/10/13 14:41
 */
@Component
@ConfigurationProperties(prefix = "thirdservice.google")
public class GoogleInfo {
    //验证token地址
    private String verifyUrl;
    //客户端id
    private String clientId;

    public String getVerifyUrl() {
        return verifyUrl;
    }

    public void setVerifyUrl(String verifyUrl) {
        this.verifyUrl = verifyUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
