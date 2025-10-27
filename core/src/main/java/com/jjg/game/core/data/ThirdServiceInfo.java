package com.jjg.game.core.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 第三方服务配置信息
 * @author 11
 * @date 2025/10/13 14:41
 */
@Component
public class ThirdServiceInfo {
    //google 配置信息
    @Value("${thirdservice.google.verify_url:}")
    private String googleVerifyUrl;
    @Value("${thirdservice.google.client_id:}")
    private String googleClientId;
    @Value("${thirdservice.google.public_key:}")
    private String googlePublicKey;
    @Value("${thirdservice.google.jwks_url:}")
    private String googleJwksUrl;
    @Value("${thirdservice.google.aud:}")
    private String googleAud;

    //facebook配置信息
    @Value("${thirdservice.facebook.user_info_url:}")
    private String facebookUserInfoUrl;
    @Value("${thirdservice.facebook.debug_token_url:}")
    private String facebookDebugTokenUrl;
    @Value("${thirdservice.facebook.app_id:}")
    private String facebookAppId;
    @Value("${thirdservice.facebook.app_secret:}")
    private String facebookSecret;

    //apple配置信息
    @Value("${thirdservice.apple.jwks_url:}")
    private String appleJwksUrl;
    @Value("${thirdservice.apple.aud:}")
    private String appleAud;

    //短信配置信息
    @Value("${thirdservice.sms.send_sms_url:}")
    private String smsSensSmsUrl;
    @Value("${thirdservice.sms.app_id:}")
    private String smsAppId;
    @Value("${thirdservice.sms.app_key:}")
    private String smsAppKey;
    @Value("${thirdservice.sms.app_secret:}")
    private String smsAppSecret;


    public String getGoogleVerifyUrl() {
        return googleVerifyUrl;
    }

    public void setGoogleVerifyUrl(String googleVerifyUrl) {
        this.googleVerifyUrl = googleVerifyUrl;
    }

    public String getGoogleClientId() {
        return googleClientId;
    }

    public void setGoogleClientId(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public String getGooglePublicKey() {
        return googlePublicKey;
    }

    public void setGooglePublicKey(String googlePublicKey) {
        this.googlePublicKey = googlePublicKey;
    }

    public String getGoogleJwksUrl() {
        return googleJwksUrl;
    }

    public void setGoogleJwksUrl(String googleJwksUrl) {
        this.googleJwksUrl = googleJwksUrl;
    }

    public String getGoogleAud() {
        return googleAud;
    }

    public void setGoogleAud(String googleAud) {
        this.googleAud = googleAud;
    }

    public String getFacebookUserInfoUrl() {
        return facebookUserInfoUrl;
    }

    public void setFacebookUserInfoUrl(String facebookUserInfoUrl) {
        this.facebookUserInfoUrl = facebookUserInfoUrl;
    }

    public String getFacebookDebugTokenUrl() {
        return facebookDebugTokenUrl;
    }

    public void setFacebookDebugTokenUrl(String facebookDebugTokenUrl) {
        this.facebookDebugTokenUrl = facebookDebugTokenUrl;
    }

    public String getFacebookAppId() {
        return facebookAppId;
    }

    public void setFacebookAppId(String facebookAppId) {
        this.facebookAppId = facebookAppId;
    }

    public String getFacebookSecret() {
        return facebookSecret;
    }

    public void setFacebookSecret(String facebookSecret) {
        this.facebookSecret = facebookSecret;
    }

    public String getAppleJwksUrl() {
        return appleJwksUrl;
    }

    public void setAppleJwksUrl(String appleJwksUrl) {
        this.appleJwksUrl = appleJwksUrl;
    }

    public String getAppleAud() {
        return appleAud;
    }

    public void setAppleAud(String appleAud) {
        this.appleAud = appleAud;
    }

    public String getSmsSensSmsUrl() {
        return smsSensSmsUrl;
    }

    public void setSmsSensSmsUrl(String smsSensSmsUrl) {
        this.smsSensSmsUrl = smsSensSmsUrl;
    }

    public String getSmsAppId() {
        return smsAppId;
    }

    public void setSmsAppId(String smsAppId) {
        this.smsAppId = smsAppId;
    }

    public String getSmsAppKey() {
        return smsAppKey;
    }

    public void setSmsAppKey(String smsAppKey) {
        this.smsAppKey = smsAppKey;
    }

    public String getSmsAppSecret() {
        return smsAppSecret;
    }

    public void setSmsAppSecret(String smsAppSecret) {
        this.smsAppSecret = smsAppSecret;
    }
}
