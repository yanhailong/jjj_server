package com.jjg.game.core.utils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class CoreUtil {
    /**
     * 检查手机号格式
     * @param phoneNumber
     * @return
     */
    public static boolean validPhoneNumber(String phoneNumber) {
        try{
            Phonenumber.PhoneNumber parse = PhoneNumberUtil.getInstance().parse(phoneNumber, "");
            return PhoneNumberUtil.getInstance().isValidNumber(parse);
        }catch (Exception e){
            return false;
        }
    }
}
