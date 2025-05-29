package com.vegasnight.game.common.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @author 11
 * @date 2022/6/9
 */
public class AESUtil {
    private static final String AES_ALGORITHM = "AES";

    public static byte[] encrypt (JSONObject json, String key) {
        if(StringUtils.isEmpty(key)){
            return null;
        }
        return encrypt(json.toJSONString().getBytes(StandardCharsets.UTF_8),key.getBytes(StandardCharsets.UTF_8));
    }
    /**
     * 加密
     *
     * @param data 待加密内容
     * @param key  加密秘钥
     * @return 十六进制字符串
     */
    public static byte[] encrypt (byte[] data, byte[] key) {
        if (key.length != 16) {
            throw new RuntimeException("Invalid AES key length (must be 16 bytes)");
        }

        try {
            // 创建密码器
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            // 初始化
            cipher.init(Cipher.ENCRYPT_MODE, genKey(key));
            // 加密
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 解密
     *
     * @param data 待解密内容(十六进制字符串)
     * @param key  加密秘钥
     * @return
     */
    public static byte[] decrypt (byte[] data, byte[] key) {
        if (key.length != 16) {
            throw new RuntimeException("Invalid AES key length (must be 16 bytes)");
        }
        try {
            // 创建密码器
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            // 初始化
            cipher.init(Cipher.DECRYPT_MODE, genKey(key));
            // 加密
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("解密失败，如果只是出现一次则忽略：");
        }
    }

    /**
     * 创建加密解密密钥
     *
     * @param key 加密解密密钥
     * @return
     */
    private static SecretKeySpec genKey (byte[] key) {
        SecretKeySpec secretKey;
        try {
            secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            byte[] enCodeFormat = secretKey.getEncoded();
            return new SecretKeySpec(enCodeFormat, AES_ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("genKey fail!", e);
        }
    }
}
