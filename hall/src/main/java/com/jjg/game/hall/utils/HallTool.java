package com.jjg.game.hall.utils;

import cn.hutool.core.lang.Snowflake;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 11
 * @date 2025/8/6 16:55
 */
public class HallTool {
    // 邮箱正则表达式
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern emailPattern = Pattern.compile(EMAIL_REGEX);
    private static final Snowflake SNOWFLAKE = new Snowflake(NodeType.HALL.getValue());

    public static boolean checkGender(byte gender) {
        return gender == GameConstant.Gender.MAN || gender == GameConstant.Gender.WOMAN || gender == GameConstant.Gender.OTHER;
    }

    public static boolean checkEmail(String email) {
        Matcher matcher = emailPattern.matcher(email);
        return matcher.matches();
    }

    public static long getNextId() {
        return SNOWFLAKE.nextId();
    }

    public static boolean validPhoneNumber(String phoneNumber) {
        try{
            Phonenumber.PhoneNumber parse = PhoneNumberUtil.getInstance().parse(phoneNumber, "");
            return PhoneNumberUtil.getInstance().isValidNumber(parse);
        }catch (Exception e){
            return false;
        }
    }
}
