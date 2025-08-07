package com.jjg.game.hall.dao;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.hall.constant.HallConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/8/6 18:12
 */
@Component
public class BindDao {
    private Logger log = LoggerFactory.getLogger(getClass());

    //绑定手机号
    private String bindPhoneTableName = "bindPhone";
    //绑定邮箱
    private String bindEmailTableName = "bindEmail";

    //缓存要绑定的信息，多少分钟后过期自动删除(分钟)
    private int PRE_EXPIRE_TIME = 10;
    //限制操作频繁，设置空闲时间(分钟)
    private int VER_CODE_IDLE_TIME = 1;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String preBindPhoneTableName(long playerId) {
        return bindPhoneTableName + ":" + playerId;
    }
    public String preBindEmailTableName(long playerId) {
        return bindEmailTableName + ":" + playerId;
    }

    /**
     * 添加绑定手机号验证码
     * @param playerId
     * @param phoneNumber
     * @param verCode
     */
    public void addPhoneVerCode(long playerId,String phoneNumber,int verCode) {

        redisTemplate.opsForValue().set(preBindPhoneTableName(playerId), verCodeValue(phoneNumber,verCode),PRE_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 添加绑定邮箱验证码
     * @param playerId
     * @param email
     * @param verCode
     */
    public void addEmailVerCode(long playerId,String email,int verCode) {
        redisTemplate.opsForValue().set(preBindEmailTableName(playerId), verCodeValue(email,verCode),PRE_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 校验验证码
     * @param playerId
     * @param verCodeType
     * @param verCode
     * @return
     */
    public CommonResult<String> verifyVerCode(long playerId,int verCodeType, int verCode) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);
        Object o = null;
        if(verCodeType == HallConstant.VerCode.TYPE_BIND_PHONE){
            o = redisTemplate.opsForValue().get(preBindPhoneTableName(playerId));
        }else if(verCodeType == HallConstant.VerCode.TYPE_BIND_EMAIL){
            o = redisTemplate.opsForValue().get(preBindEmailTableName(playerId));
        }

        if(o == null) {
            result.code = Code.NOT_FOUND;
            log.debug("未找到该类型的验证码 playerId = {}, verCodeType = {}, verCode = {}", playerId, verCodeType,verCode);
            return result;
        }

        String[] arr = o.toString().split("&");
        int cacheCode = Integer.parseInt(arr[1]);
        if(cacheCode != verCode) {
            result.code = Code.FAIL;
            log.debug("验证码不匹配，校验失败 playerId = {}, verCodeType = {},verCode = {}", playerId, verCodeType,verCode);
            return result;
        }

        result.data = arr[0];
        return result;
    }
    /**
     * 获取空闲时间
     * @param playerId
     * @param verCodeType
     * @return
     */
    public CommonResult<Integer> verCodeIdleTime(long playerId,int verCodeType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        Object o = null;
        if(verCodeType == HallConstant.VerCode.TYPE_BIND_PHONE){
            o = redisTemplate.opsForValue().get(preBindPhoneTableName(playerId));
        }else if(verCodeType == HallConstant.VerCode.TYPE_BIND_EMAIL){
            o = redisTemplate.opsForValue().get(preBindEmailTableName(playerId));
        }

        if(o == null) {
            result.data = 0;
            return result;
        }
        String[] arr = o.toString().split("&");
        result.data = Integer.parseInt(arr[2]);
        return result;
    }

    /**
     * 移除验证码
     * @param playerId
     * @param verCodeType
     */
    public void delVerCode(long playerId,int verCodeType){
        if(verCodeType == HallConstant.VerCode.TYPE_BIND_PHONE){
            redisTemplate.delete(preBindPhoneTableName(playerId));
        }else if(verCodeType == HallConstant.VerCode.TYPE_BIND_EMAIL){
            redisTemplate.delete(preBindEmailTableName(playerId));
        }
    }

    private String verCodeValue(String data,int verCode){
        int idleTime = TimeHelper.nowInt() + VER_CODE_IDLE_TIME * 60;
        return data + "&" + verCode + "&" + idleTime;
    }
}
