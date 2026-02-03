package com.jjg.game.core.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/2/3
 */
@Component
public class AdjustConfig {
    @Value("${adjust.open:false}")
    private boolean open;
    @Value("${adjust.app_token:}")
    private String appToken;
    @Value("${adjust.api_token:}")
    private String apiToken;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
