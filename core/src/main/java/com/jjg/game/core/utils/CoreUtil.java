package com.jjg.game.core.utils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.IponeAreacodeConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CoreUtil {
    private static final Logger log = LoggerFactory.getLogger(CoreUtil.class);

    /**
     * 检查手机号格式
     *
     * @param phoneNumber
     * @return
     */
    public static String validPhoneNumber(String phoneNumber) {
        try {
            if (!phoneNumber.startsWith("+")) {
                log.warn("该手机号不是以+号开头  phoneNumber = {}", phoneNumber);
                return null;
            }

            String realPhone = phoneNumber;

            IponeAreacodeConfigCfg cfg = null;
            String tmpPhone = phoneNumber.substring(1);
            for (Map.Entry<Integer, IponeAreacodeConfigCfg> en : GameDataManager.getIponeAreacodeConfigCfgMap().entrySet()) {
                IponeAreacodeConfigCfg c = en.getValue();
                String str = String.valueOf(c.getType());
                if (!tmpPhone.startsWith(str)) {
                    continue;
                }
                cfg = c;
                //去掉国际编码
                String tmpRealPhone = tmpPhone.substring(str.length());
                if (tmpRealPhone.isEmpty()) {
                    log.warn("手机号去除国际编码后为空 phoneNumber = {},str = {}", phoneNumber, str);
                    return null;
                }

                if (tmpRealPhone.startsWith("0")) {
                    tmpRealPhone = tmpRealPhone.substring(1);
                    realPhone = "+" + str + tmpRealPhone;
                }

                //检查被屏蔽的号段
                if (cfg.getBlockedNumber() != null && !cfg.getBlockedNumber().isEmpty()) {
                    for (String s : cfg.getBlockedNumber()) {
                        if (tmpRealPhone.startsWith(s)) {
                            log.warn("该号段在配置中已经屏蔽 phoneNumber = {},blockedNumber = {}", phoneNumber, s);
                            return null;
                        }
                    }
                }
                break;
            }

            if (cfg == null) {
                log.warn("根据号码未在配置表中找到对应配置 phoneNumber = {}", phoneNumber);
                return null;
            }

            Phonenumber.PhoneNumber parse = PhoneNumberUtil.getInstance().parse(phoneNumber, "");
            boolean validNumber = PhoneNumberUtil.getInstance().isValidNumber(parse);
            if (!validNumber) {
                log.warn("手机号校验未通过 phoneNumber = {}", phoneNumber);
                return null;
            }
            return realPhone;
        } catch (Exception e) {
            log.error("验证手机号异常 ", e);
        }
        return null;
    }
}
