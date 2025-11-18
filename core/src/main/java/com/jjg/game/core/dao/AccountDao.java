package com.jjg.game.core.dao;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/5/24 17:39
 */
@Repository
public class AccountDao extends MongoBaseDao<Account, Long> {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private RedisLock redisLock;
    @Autowired
    private RedisTemplate redisTemplate;

    private final String DATA_TABLE_NAME = "account:data";
    private final String THIRD_TABLE_NAME = "account:";
    private final String LOCK_TABLE_NAME = "lockaccount:";

    private String thirdTableName(LoginType loginType) {
        return THIRD_TABLE_NAME + loginType.name();
    }

    private String getLockKey(long playerId) {
        return LOCK_TABLE_NAME + playerId;
    }


    public AccountDao(@Autowired MongoTemplate mongoTemplate) {
        super(Account.class, mongoTemplate);
    }

    /**
     * 根据玩家id查询
     *
     * @return
     */
    public Account queryAccountByPlayerId(long playerId) {
        return queryAccountByPlayerId(playerId, true);
    }

    public Account queryAccountByPlayerId(long playerId, boolean mongo) {
        Object object = redisTemplate.opsForHash().get(DATA_TABLE_NAME, playerId);
        if (object != null) {
            return (Account) object;
        }

        if (mongo) {
            return mongoTemplate.findById(playerId, this.clazz);
        }
        return null;
    }

    public Account checkAndSave(long playerId, DataSaveCallback<Account> cbk) {
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            Account account = queryAccountByPlayerId(playerId);
            if (account == null) {
                log.debug("获取account为空 playerId = {}", playerId);
                return null;
            }
            cbk.updateData(account);
            redisTemplate.opsForHash().put(DATA_TABLE_NAME, playerId, account);
            return account;
        } catch (Exception e) {
            log.warn("保存account失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    /**
     * 根据注册mac查询
     *
     * @return
     */
    public Account queryByRegisterMac(String registerMac) {
        Query query = new Query(Criteria.where("registerMac").is(registerMac));
        return mongoTemplate.findOne(query, this.clazz);
    }

    /**
     * 根据登录mac查询
     *
     * @return
     */
    public Account queryByLoginMac(String loginMac) {
        Query query = new Query(Criteria.where("lastLoginMac").is(loginMac));
        return mongoTemplate.findOne(query, this.clazz);
    }

    /**
     * 查询第三方账号
     *
     * @param data
     * @return
     */
    public Account queryThirdAccount(LoginType loginType, String data) {
        Object object = redisTemplate.opsForHash().get(thirdTableName(loginType), data);
        if (object == null) {
            return null;
        }

        long playerId = Long.parseLong(object.toString());
        Object accountObj = redisTemplate.opsForHash().get(DATA_TABLE_NAME, playerId);
        if (accountObj != null) {
            return (Account) accountObj;
        }
        return mongoTemplate.findById(playerId, this.clazz);
    }

    public Account setChannelValue(LoginType loginType, ChannelUserInfo channelUserInfo, Account account) {
        boolean add = account.addThirdAccount(loginType, channelUserInfo.getUserId());
        if (!add) {
            return account;
        }

        if (loginType == LoginType.GOOGLE) {
            GoogleUserInfo googleUserInfo = (GoogleUserInfo) channelUserInfo;
            account.setEmail(googleUserInfo.getEmail());
        }
        return account;
    }

    /**
     * 绑定第三方账号
     *
     * @param loginType
     * @param channelUserInfo
     * @return
     */
    public CommonResult<Account> addThirdAccount(long playerId, LoginType loginType, ChannelUserInfo channelUserInfo) {
        CommonResult<Account> result = new CommonResult<>(Code.FAIL);

        //要加锁，防止重复绑定
        String lockKey = getBindLockKey(loginType, channelUserInfo.getUserId());
        redisLock.executeWithLock(lockKey, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES, TimeUnit.MILLISECONDS, () -> {
            //查询该账号是否被绑定
            Account existBindAccount = queryThirdAccount(loginType, channelUserInfo.getUserId());
            if (existBindAccount != null) {
                result.code = Code.EXIST;
                return result;
            }

            Account tmpAccount = checkAndSave(playerId, a -> {
                setChannelValue(loginType, channelUserInfo, a);
            });

            if (tmpAccount == null) {
                result.code = Code.NOT_FOUND;
            } else {
                result.code = Code.SUCCESS;
                result.data = tmpAccount;
            }
            return result;
        });
        return result;
    }

    private String getBindLockKey(LoginType loginType, String data) {
        switch (loginType) {
            case GUEST -> {
                return "guestbind:" + data;
            }
            case GOOGLE -> {
                return "googlebind:" + data;
            }
            case APPLE -> {
                return "applebind:" + data;
            }
            case FACEBOOK -> {
                return "facebookbind:" + data;
            }
            case PHONE -> {
                return "phonebind:" + data;
            }
            default -> {
                return "guestbind:" + data;
            }
        }
    }

    /**
     * 新增account
     *
     * @param account
     * @param loginType
     * @param data
     * @return
     */
    public Account save(Account account, LoginType loginType, String data) {
        redisTemplate.opsForHash().put(DATA_TABLE_NAME, account.getPlayerId(), account);
        redisTemplate.opsForHash().put(thirdTableName(loginType), data, account.getPlayerId());
        return account;
    }

    public void moveToMongo(long playerId) {
        Account account = queryAccountByPlayerId(playerId,false);
        if (account == null) {
            return;
        }
        save(account);
        redisTemplate.opsForHash().delete(DATA_TABLE_NAME, playerId);
    }
}
