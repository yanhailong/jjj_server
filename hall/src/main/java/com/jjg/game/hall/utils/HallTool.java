package com.jjg.game.hall.utils;

import com.jjg.game.core.constant.GameConstant;

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

    public static boolean checkGender(byte gender) {
        return gender == GameConstant.Gender.MAN || gender == GameConstant.Gender.WOMAN || gender == GameConstant.Gender.OTHER;
    }

    public static boolean checkEmail(String email) {
        Matcher matcher = emailPattern.matcher(email);
        return matcher.matches();
    }
}
