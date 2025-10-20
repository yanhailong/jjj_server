package com.jjg.game.core.service;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.VerCodeDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.VerCodeType;
import com.jjg.game.core.data.ThirdServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author 11
 * @date 2025/10/18 9:38
 */
@Service
public class SmsService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ThirdServiceInfo thirdServiceInfo;
    @Autowired
    private VerCodeDao verCodeDao;


    /**
     * 发送验证码短信
     * @param phone
     * @param verCodeType
     * @return
     */
    public int sendCode(String phone, VerCodeType verCodeType) {
        //生成验证码
        int verCode = RandomUtils.randomNum(100000, 999999);
        String content = "code:" + verCode;
        int sendResultCode = sendOnbukaSms(phone, content, verCodeType);
        if (sendResultCode != Code.SUCCESS) {
            return sendResultCode;
        }
        //缓存验证码
        verCodeDao.addVerCode(phone, verCodeType, verCode);
        return Code.SUCCESS;
    }

    /**
     * 发送验证码短信
     * @param playerId
     * @param phone
     * @param verCodeType
     * @return
     */
    public CommonResult<Integer> sendCode(long playerId, String phone, VerCodeType verCodeType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        //生成验证码
        int verCode = RandomUtils.randomNum(100000, 999999);
        String content = "code:" + verCode;
        int sendResultCode = sendOnbukaSms(phone, content, verCodeType);
        if (sendResultCode != Code.SUCCESS) {
            result.code = sendResultCode;
            return result;
        }
        //缓存验证码
        verCodeDao.addVerCode(playerId, verCodeType, phone, verCode);
        result.data = verCode;
        return result;
    }


    /**
     * 发送短信(颂量)
     *
     * @param phoneNumber
     */
    private int sendItniotechSms(String phoneNumber, String content, VerCodeType verCodeType) {
        try {
            HttpRequest httpRequest = HttpRequest.post(thirdServiceInfo.getSmsSensSmsUrl());

            //当前时间戳
            String now = String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
            //签名
            String sign = SecureUtil.md5(thirdServiceInfo.getSmsAppKey().concat(thirdServiceInfo.getSmsAppSecret()).concat(now));

            httpRequest.header(Header.CONNECTION, "Keep-Alive")
                    .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                    .header("Sign", sign)
                    .header("Timestamp", now)
                    .header("Api-Key", thirdServiceInfo.getSmsAppKey());


            String params = JSONUtil.createObj()
                    .set("appId", thirdServiceInfo.getSmsAppId())
                    .set("numbers", phoneNumber)
                    .set("content", content)
                    .toString();

            HttpResponse resp = httpRequest.body(params).execute();
            String body = resp.body();

            JSONObject json = JSONUtil.parseObj(body);

            if (!resp.isOk()) {
                log.warn("发送短信失败 phoneNumber = {},code = {},reason = {}", phoneNumber, json.getInt("status"), json.getStr("reason"));
                return Code.FAIL;
            }

            log.info("发送短信成功 smsType = {},content = {},resp = {}", verCodeType, content, body);
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }

    /**
     * 发送短信(onbuka)
     *
     * @param phoneNumber
     */
    private int sendOnbukaSms(String phoneNumber, String content, VerCodeType verCodeType) {
        try {
            HttpRequest httpRequest = HttpRequest.post(thirdServiceInfo.getSmsSensSmsUrl());

            //当前时间戳
            String now = String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
            //签名
            String sign = SecureUtil.md5(thirdServiceInfo.getSmsAppKey().concat(thirdServiceInfo.getSmsAppSecret()).concat(now));

            httpRequest.header(Header.CONNECTION, "Keep-Alive")
                    .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                    .header("Sign", sign)
                    .header("Timestamp", now)
                    .header("Api-Key", thirdServiceInfo.getSmsAppKey());


            String params = JSONUtil.createObj()
                    .set("appId", thirdServiceInfo.getSmsAppId())
                    .set("numbers", phoneNumber)
                    .set("content", content)
                    .toString();

            HttpResponse resp = httpRequest.body(params).execute();
            String body = resp.body();

            JSONObject json = JSONUtil.parseObj(body);

            if (!resp.isOk()) {
                log.warn("发送短信失败 phoneNumber = {},code = {},reason = {}", phoneNumber, json.getInt("status"), json.getStr("reason"));
                return Code.FAIL;
            }

            log.info("发送短信成功 smsType = {},content = {},resp = {}", verCodeType, content, body);
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }
}
