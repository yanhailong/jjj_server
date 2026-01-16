package com.jjg.game.core.utils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.IponeAreacodeConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CoreUtil {
    private static final Logger log = LoggerFactory.getLogger(CoreUtil.class);

    /**
     * 检查手机号格式
     * @param phoneNumber
     * @return
     */
    public static boolean validPhoneNumber(String phoneNumber) {
        try{
            if(!phoneNumber.startsWith("+")){
                log.warn("该手机号不是以+号开头  phoneNumber = {}", phoneNumber);
                return false;
            }

            IponeAreacodeConfigCfg cfg = null;
            String tmpPhone = phoneNumber.substring(1);
            for(Map.Entry<Integer, IponeAreacodeConfigCfg> en : GameDataManager.getIponeAreacodeConfigCfgMap().entrySet()){
                IponeAreacodeConfigCfg c = en.getValue();
                String str = String.valueOf(c.getType());
                if(!tmpPhone.startsWith(str)){
                    continue;
                }
                cfg = c;
                tmpPhone = tmpPhone.substring(str.length());
                break;
            }

            if(cfg == null){
                log.warn("根据号码未在配置表中找到对应配置 phoneNumber = {}", phoneNumber);
                return false;
            }

            if(cfg.getBlockedNumber() != null && !cfg.getBlockedNumber().isEmpty()){
                for(String str : cfg.getBlockedNumber()){
                    if(tmpPhone.startsWith(str)){
                        log.warn("该号段在配置中已经屏蔽 phoneNumber = {},blockedNumber = {}", phoneNumber,str);
                        return false;
                    }
                }
            }

            Phonenumber.PhoneNumber parse = PhoneNumberUtil.getInstance().parse(phoneNumber, "");
            return PhoneNumberUtil.getInstance().isValidNumber(parse);
        }catch (Exception e){
            return false;
        }
    }
}
