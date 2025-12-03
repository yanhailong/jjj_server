package com.jjg.game.core.dao;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.VerCodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存验证码dao
 *
 * @author 11
 * @date 2025/8/6 18:12
 */
@Component
public class VerCodeDao {
    private Logger log = LoggerFactory.getLogger(getClass());

    //短信验证码
    private String smsVercodeTableName = "vercode:sms:";
    //邮箱验证码
    private String mailVercodeTableName = "vercode:mail:";

    //缓存要绑定的信息，多少分钟后过期自动删除(分钟)
    private int PRE_EXPIRE_TIME = 10;
    //限制操作频繁，设置空闲时间(分钟)
    private int VER_CODE_IDLE_TIME = 1;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String smsVercodeTableName(VerCodeType verCodeType, long playerId) {
        return smsVercodeTableName + verCodeType.name() + playerId;
    }

    private String smsVercodeTableName(VerCodeType verCodeType, String phone) {
        return smsVercodeTableName + verCodeType.name() + phone;
    }

    private String mailVercodeTableName(VerCodeType verCodeType, long playerId) {
        return mailVercodeTableName + verCodeType.name() + playerId;
    }

    private String mailVercodeTableName(VerCodeType verCodeType, String mail) {
        return mailVercodeTableName + verCodeType.name() + mail;
    }

    /**
     * 缓存验证码
     *
     * @param playerId
     * @param verCodeType
     * @param data
     * @param verCode
     */
    public void addVerCode(long playerId, VerCodeType verCodeType, String data, int verCode) {
        if (verCodeType == VerCodeType.MAIL_BIND_MAIL) {
            redisTemplate.opsForValue().set(mailVercodeTableName(verCodeType, playerId), verCodeValue(data, verCode), PRE_EXPIRE_TIME, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(smsVercodeTableName(verCodeType, playerId), verCodeValue(data, verCode), PRE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
    }

    /**
     * 缓存验证码
     *
     * @param data
     * @param verCodeType
     * @param verCode
     */
    public void addVerCode(String data, VerCodeType verCodeType, int verCode) {
        if (verCodeType == VerCodeType.MAIL_BIND_MAIL) {
            redisTemplate.opsForValue().set(mailVercodeTableName(verCodeType, data), verCodeValue(verCode), PRE_EXPIRE_TIME, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(smsVercodeTableName(verCodeType, data), verCodeValue(verCode), PRE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
    }

    /**
     * 获取验证码
     *
     * @param playerId
     * @param verCodeType
     * @return
     */
    public Object getVerCode(long playerId, VerCodeType verCodeType) {
        if (verCodeType == VerCodeType.MAIL_BIND_MAIL) {
            return redisTemplate.opsForValue().get(mailVercodeTableName(verCodeType, playerId));
        }
        return redisTemplate.opsForValue().get(smsVercodeTableName(verCodeType, playerId));
    }

    /**
     * 获取验证码
     *
     * @param data
     * @param verCodeType
     * @return
     */
    public Object getVerCode(String data, VerCodeType verCodeType) {
        if (verCodeType == VerCodeType.MAIL_BIND_MAIL) {
            return redisTemplate.opsForValue().get(mailVercodeTableName(verCodeType, data));
        }
        return redisTemplate.opsForValue().get(smsVercodeTableName(verCodeType, data));
    }

    /**
     * 校验验证码
     *
     * @param playerId
     * @param verCodeType
     * @param verCode
     * @return
     */
    public CommonResult<String> verifyVerCode(long playerId, VerCodeType verCodeType, int verCode) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);
        Object o = getVerCode(playerId, verCodeType);

        if (o == null) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该类型的验证码 playerId = {}, verCodeType = {}, verCode = {}", playerId, verCodeType, verCode);
            return result;
        }

        String[] arr = o.toString().split("&");
        int cacheCode = Integer.parseInt(arr[1]);
        if (cacheCode != verCode) {
            result.code = Code.FAIL;
            log.warn("验证码不匹配，校验失败 playerId = {}, verCodeType = {},verCode = {},cacheCode = {}", playerId, verCodeType, verCode, cacheCode);
            return result;
        }

        result.data = arr[0];
        delVerCode(playerId, verCodeType);
        return result;
    }

    /**
     * 校验验证码
     *
     * @param data
     * @param verCodeType
     * @param verCode
     * @return
     */
    public CommonResult<String> verifyVerCode(String data, VerCodeType verCodeType, int verCode) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);
        Object o = getVerCode(data, verCodeType);

        if (o == null) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该类型的验证码 data = {}, verCodeType = {}, verCode = {}", data, verCodeType, verCode);
            return result;
        }

        String[] arr = o.toString().split("&");
        int cacheCode = Integer.parseInt(arr[0]);
        if (cacheCode != verCode) {
            result.code = Code.FAIL;
            log.warn("验证码不匹配，校验失败 data = {}, verCodeType = {},verCode = {}", data, verCodeType, verCode);
            return result;
        }

        delVerCode(data, verCodeType);
        result.data = arr[0];
        return result;
    }

    /**
     * 获取空闲时间
     *
     * @param playerId
     * @return
     */
    public CommonResult<Integer> verCodeIdleTime(long playerId, VerCodeType verCodeType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        Object o = getVerCode(playerId, verCodeType);

        if (o == null) {
            result.data = 0;
            return result;
        }
        String[] arr = o.toString().split("&");
        result.data = Integer.parseInt(arr[2]);
        return result;
    }

    /**
     * 获取空闲时间
     *
     * @param data
     * @return
     */
    public CommonResult<Integer> verCodeIdleTime(String data, VerCodeType verCodeType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        Object o = getVerCode(data, verCodeType);

        if (o == null) {
            result.data = 0;
            return result;
        }
        String[] arr = o.toString().split("&");
        result.data = Integer.parseInt(arr[1]);
        return result;
    }

    /**
     * 移除验证码
     *
     * @param playerId
     */
    public void delVerCode(long playerId, VerCodeType verCodeType) {
        if (verCodeType == VerCodeType.MAIL_BIND_MAIL) {
            redisTemplate.delete(mailVercodeTableName(verCodeType, playerId));
        } else {
            redisTemplate.delete(smsVercodeTableName(verCodeType, playerId));
        }
    }

    /**
     * 移除验证码
     *
     * @param data
     * @param verCodeType
     */
    public void delVerCode(String data, VerCodeType verCodeType) {
        if (verCodeType == VerCodeType.MAIL_BIND_MAIL) {
            redisTemplate.delete(mailVercodeTableName(verCodeType, data));
        } else {
            redisTemplate.delete(smsVercodeTableName(verCodeType, data));
        }

    }

    private String verCodeValue(String data, int verCode) {
        int idleTime = TimeHelper.nowInt() + VER_CODE_IDLE_TIME * 60;
        return data + "&" + verCode + "&" + idleTime;
    }

    private String verCodeValue(int verCode) {
        int idleTime = TimeHelper.nowInt() + VER_CODE_IDLE_TIME * 60;
        return verCode + "&" + idleTime;
    }
}
