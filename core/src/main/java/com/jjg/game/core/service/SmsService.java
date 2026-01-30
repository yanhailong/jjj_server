package com.jjg.game.core.service;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.SmsConfigDao;
import com.jjg.game.core.dao.VerCodeDao;
import com.jjg.game.core.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private SmsConfigDao smsConfigDao;

    public void init(){
        reloadConfig();
    }

    public void reloadConfig() {
        thirdServiceInfo.setSmsConfigInfoList(getAll());
        log.info("加载sms配置 ,size={}", thirdServiceInfo.getSmsConfigInfoList() == null ? 0 : thirdServiceInfo.getSmsConfigInfoList().size());
    }

    public List<SmsConfigInfo> getAll() {
        return smsConfigDao.getAll();
    }

    public VerCode getSmsCodeByPlayerId(long playerId) {
        return verCodeDao.getSmsVerCodeByPlayerId(playerId);
    }

    public VerCode getSmsCodeByPhone(String phone) {
        return verCodeDao.getSmsVerCodeByPhone(phone);
    }

    public void save(List<SmsConfigInfo> list) {
        Map<Integer, SmsConfigInfo> map = new HashMap<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(c -> map.put(c.getCfgId(), c));
        }
        smsConfigDao.save(map);
    }

    /**
     * 发送验证码短信
     *
     * @param vc
     * @return
     */
    public CommonResult<VerCode> sendCode(VerCode vc) {
        CommonResult<VerCode> result = new CommonResult<>(Code.SUCCESS);
        //生成验证码
        int verCode = RandomUtils.randomNum(GameConstant.VerCode.CODE_MIN, GameConstant.VerCode.CODE_MAX);
        String content = verCode + " is your verification code";
        int sendResultCode = sendOnbukaSms(vc.getData(), content, vc.getVerCodeType());
        if (sendResultCode != Code.SUCCESS) {
            result.code = sendResultCode;
            return result;
        }

        vc.setCode(verCode);
        //缓存验证码
        result.code = verCodeDao.addSmsVerCode(vc);
        result.data = vc;
        return result;
    }

    /**
     * 发送短信(onbuka)
     *
     * @param phoneNumber
     */
    private int sendOnbukaSms(String phoneNumber, String content, VerCodeType verCodeType) {
        int cfgId = 0;
        try {
            SmsConfigInfo smsConfigInfo = randSmsConfigInfoList();
            if (smsConfigInfo == null) {
                log.warn("获取sms配置失败");
                return Code.SMS_CONFIGURATION_MISSING_CAPTCHA_FAILED;
            }
            cfgId = smsConfigInfo.getCfgId();

            HttpRequest httpRequest = HttpRequest.post(smsConfigInfo.getSendSmsUrl());

//            httpRequest.setHttpProxy("127.0.0.1", 7897);

            //当前时间戳
            String now = String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
            //签名
            String sign = SecureUtil.md5(smsConfigInfo.getAppKey().concat(smsConfigInfo.getAppSecret()).concat(now));

            httpRequest.header(Header.CONNECTION, "Keep-Alive")
                    .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                    .header("Sign", sign)
                    .header("Timestamp", now)
                    .header("Api-Key", smsConfigInfo.getAppKey());


            String params = JSONUtil.createObj()
                    .set("appId", smsConfigInfo.getAppId())
                    .set("numbers", phoneNumber)
                    .set("content", content)
                    .toString();

            HttpResponse resp = httpRequest.body(params).execute();
            String body = resp.body();

            JSONObject json = JSONUtil.parseObj(body);

            if (!resp.isOk()) {
                log.warn("发送短信失败 phoneNumber = {},code = {},reason = {},cfgId = {}", phoneNumber, json.getInt("status"), json.getStr("reason"), smsConfigInfo.getCfgId());
                return Code.SEND_SMS_FAILED;
            }

            int status = json.getInt("status");
            if (status != 0) {
                log.warn("发送短信失败1 phoneNumber = {},code = {},reason = {},cfgId = {}", phoneNumber, json.getInt("status"), json.getStr("reason"), smsConfigInfo.getCfgId());
                return Code.SEND_SMS_FAILED;
            }

            log.info("发送短信成功 smsType = {},content = {},resp = {},cfgId = {}", verCodeType, content, body, smsConfigInfo.getCfgId());
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("cfgId={}", cfgId, e);
            return Code.SEND_SMS_FAILED;
        }
    }

    private SmsConfigInfo randSmsConfigInfoList() {
        if (thirdServiceInfo.getSmsConfigInfoList() == null) {
            return null;
        }
        return thirdServiceInfo.getSmsConfigInfoList().stream().filter(c -> c.isOpen()).findFirst().orElse(null);
    }

    public CommonResult<VerCode> verifySmsVerCode(VerCode reqVerCode) {
        return verCodeDao.verifySmsVerCode(reqVerCode);
    }

    public void setThirdServiceInfo(ThirdServiceInfo thirdServiceInfo) {
        this.thirdServiceInfo = thirdServiceInfo;
    }
}
