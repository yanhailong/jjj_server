package com.jjg.game.core.dao;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.VerCode;
import com.jjg.game.core.data.VerCodeType;
import org.apache.commons.lang3.StringUtils;
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

    //短信验证码(存储无法获知playerId时的验证码)
    private String smsVercodeTableName = "vercode:sms:";
    //短信验证码
    private String smsPlayerVercodeTableName = "vercode:sms:player:";

    //邮箱验证码(存储无法获知playerId时的验证码)
    private String mailVercodeTableName = "vercode:mail:";
    //邮箱验证码
    private String mailPlayerVercodeTableName = "vercode:mail:player:";

    //缓存要绑定的信息，多少分钟后过期自动删除(分钟)
    private int PRE_EXPIRE_TIME = 10;
    //限制操作频繁，设置空闲时间(分钟)
    private int VER_CODE_IDLE_TIME = 1;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String smsTableName(String phone) {
        return smsVercodeTableName + phone;
    }

    public String smsPlayerTableName(long playerId) {
        return smsPlayerVercodeTableName + playerId;
    }

    public String mailTableName(String mail) {
        return mailVercodeTableName + mail;
    }

    public String mailPlayerTableName(long playerId) {
        return mailPlayerVercodeTableName + playerId;
    }

    /**
     * 添加短信验证码
     *
     * @param verCode
     */
    public int addSmsVerCode(VerCode verCode) {
        int idleTime = TimeHelper.nowInt() + VER_CODE_IDLE_TIME * 60;
        verCode.setIdleTime(idleTime);

        if (verCode.getPlayerId() < 1) {
            if (StringUtils.isNotBlank(verCode.getData())) {
                redisTemplate.opsForValue().set(smsTableName(verCode.getData()), verCode, PRE_EXPIRE_TIME, TimeUnit.MINUTES);
            } else {
                return Code.PARAM_ERROR;
            }
        } else {
            redisTemplate.opsForValue().set(smsPlayerTableName(verCode.getPlayerId()), verCode, PRE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        return Code.SUCCESS;
    }

    /**
     * 添加邮件验证码
     *
     * @param verCode
     */
    public int addMailVerCode(VerCode verCode) {
        int idleTime = TimeHelper.nowInt() + VER_CODE_IDLE_TIME * 60;
        verCode.setIdleTime(idleTime);

        if (verCode.getPlayerId() < 1) {
            if (StringUtils.isNotBlank(verCode.getData())) {
                redisTemplate.opsForValue().set(mailTableName(verCode.getData()), verCode, PRE_EXPIRE_TIME, TimeUnit.MINUTES);
            } else {
                return Code.PARAM_ERROR;
            }
        } else {
            redisTemplate.opsForValue().set(mailPlayerTableName(verCode.getPlayerId()), verCode, PRE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        return Code.SUCCESS;
    }

    public VerCode getSmsVerCodeByPlayerId(long playerId) {
        return (VerCode) redisTemplate.opsForValue().get(smsPlayerTableName(playerId));
    }

    public VerCode getSmsVerCodeByPhone(String phone) {
        return (VerCode) redisTemplate.opsForValue().get(smsTableName(phone));
    }

    public VerCode getMailVerCodeByPlayerId(long playerId) {
        return (VerCode) redisTemplate.opsForValue().get(mailPlayerTableName(playerId));
    }

    public VerCode getMailVerCodeByMail(String mail) {
        return (VerCode) redisTemplate.opsForValue().get(mailTableName(mail));
    }

    /**
     * 校验短信验证码
     *
     * @param reqVerCode
     * @return
     */
    public CommonResult<VerCode> verifySmsVerCode(VerCode reqVerCode) {
        CommonResult<VerCode> result = new CommonResult<>(Code.SUCCESS);

        VerCode dbVerCode;
        if (reqVerCode.getPlayerId() < 1) {
            dbVerCode = getSmsVerCodeByPhone(reqVerCode.getData());
        } else {
            dbVerCode = getSmsVerCodeByPlayerId(reqVerCode.getPlayerId());
        }

        if (dbVerCode == null) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该玩家的验证码 reqVerCode = {}", JSON.toJSONString(reqVerCode));
            return result;
        }

        if (reqVerCode.getVerCodeType() != dbVerCode.getVerCodeType()) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该玩家的验证码1 reqVerCode = {}, dbVerCode = {}", JSON.toJSONString(reqVerCode), JSON.toJSONString(dbVerCode));
            return result;
        }

        if (reqVerCode.getCode() != dbVerCode.getCode()) {
            result.code = Code.FAIL;
            log.warn("验证码不匹配，校验失败 reqVerCode = {}, dbVerCode = {}", JSON.toJSONString(reqVerCode), JSON.toJSONString(dbVerCode));
            return result;
        }

        result.data = dbVerCode;
        if (dbVerCode.getPlayerId() < 1) {
            redisTemplate.delete(smsTableName(dbVerCode.getData()));
        } else {
            redisTemplate.delete(smsPlayerTableName(dbVerCode.getPlayerId()));
        }
        return result;
    }

    /**
     * 校验邮件验证码
     *
     * @param reqVerCode
     * @return
     */
    public CommonResult<String> verifyMailVerCode(VerCode reqVerCode) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);

        VerCode dbVerCode;
        if (reqVerCode.getPlayerId() < 1) {
            dbVerCode = getMailVerCodeByMail(reqVerCode.getData());
        } else {
            dbVerCode = getMailVerCodeByPlayerId(reqVerCode.getPlayerId());
        }

        if (dbVerCode == null) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该玩家的邮件验证码 reqVerCode = {}", JSON.toJSONString(reqVerCode));
            return result;
        }

        if (reqVerCode.getVerCodeType() != dbVerCode.getVerCodeType()) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该玩家的邮件验证码1 reqVerCode = {}, dbVerCode = {}", JSON.toJSONString(reqVerCode), JSON.toJSONString(dbVerCode));
            return result;
        }

        if (reqVerCode.getCode() != dbVerCode.getCode()) {
            result.code = Code.FAIL;
            log.warn("邮件验证码不匹配，校验失败 reqVerCode = {}, dbVerCode = {}", JSON.toJSONString(reqVerCode), JSON.toJSONString(dbVerCode));
            return result;
        }

        result.data = dbVerCode.getData();
        if (dbVerCode.getPlayerId() < 1) {
            redisTemplate.delete(mailTableName(dbVerCode.getData()));
        } else {
            redisTemplate.delete(mailPlayerTableName(dbVerCode.getPlayerId()));
        }
        return result;
    }

    /**
     * 获取短信验证码信息
     *
     * @return
     */
    public CommonResult<VerCode> querySmsVerCodeInfo(VerCode reqVerCode) {
        CommonResult<VerCode> result = new CommonResult<>(Code.SUCCESS);
        VerCode dbVerCode;
        if (reqVerCode.getPlayerId() < 1) {
            dbVerCode = getSmsVerCodeByPhone(reqVerCode.getData());
        } else {
            dbVerCode = getSmsVerCodeByPlayerId(reqVerCode.getPlayerId());
        }

        if (dbVerCode == null) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该玩家的验证码3 reqVerCode = {}", JSON.toJSONString(reqVerCode));
            return result;
        }

        if (reqVerCode.getVerCodeType() != null) {
            if (reqVerCode.getVerCodeType() != dbVerCode.getVerCodeType()) {
                result.code = Code.NOT_FOUND;
                log.warn("未找到该玩家的验证码34 reqVerCode = {}, dbVerCode = {}", JSON.toJSONString(reqVerCode), JSON.toJSONString(dbVerCode));
                return result;
            }
        }

        result.data = dbVerCode;
        return result;
    }

    /**
     * 获取邮件验证码信息
     *
     * @return
     */
    public CommonResult<VerCode> queryMailVerCodeInfo(VerCode reqVerCode) {
        CommonResult<VerCode> result = new CommonResult<>(Code.SUCCESS);
        VerCode dbVerCode;
        if (reqVerCode.getPlayerId() < 1) {
            dbVerCode = getMailVerCodeByMail(reqVerCode.getData());
        } else {
            dbVerCode = getMailVerCodeByPlayerId(reqVerCode.getPlayerId());
        }

        if (dbVerCode == null) {
            result.code = Code.NOT_FOUND;
            log.warn("未找到该玩家的邮件验证码3 reqVerCode = {}", JSON.toJSONString(reqVerCode));
            return result;
        }

        if (reqVerCode.getVerCodeType() != null) {
            if (reqVerCode.getVerCodeType() != dbVerCode.getVerCodeType()) {
                result.code = Code.NOT_FOUND;
                log.warn("未找到该玩家的邮件验证码34 reqVerCode = {}, dbVerCode = {}", JSON.toJSONString(reqVerCode), JSON.toJSONString(dbVerCode));
                return result;
            }
        }

        result.data = dbVerCode;
        return result;
    }
}
