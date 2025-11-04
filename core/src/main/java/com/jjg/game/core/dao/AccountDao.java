package com.jjg.game.core.dao;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.*;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/5/24 17:39
 */
@Repository
public class AccountDao extends MongoBaseDao<Account, Long> {

    @Autowired
    private RedisLock redisLock;

    public AccountDao(@Autowired MongoTemplate mongoTemplate) {
        super(Account.class, mongoTemplate);
    }

    /**
     * 根据玩家id查询
     *
     * @return
     */
    public Account queryAccountByPlayerId(long playerId) {
        return mongoTemplate.findById(playerId, this.clazz);
    }

    /**
     * 更新邮箱
     *
     * @param playerId
     * @param email
     * @return
     */
    public boolean updateEmail(long playerId, String email) {
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("email", email);
        return mongoTemplate.updateFirst(query, update, this.clazz).getModifiedCount() > 0;
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
     * 修改账号状态
     *
     * @param playerId
     * @param status
     * @return
     */
    public boolean updateAccountStatus(long playerId, int status) {
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("status", status);
        return mongoTemplate.updateFirst(query, update, this.clazz).getModifiedCount() > 0;
    }

    /**
     * 修改最近一次登录时间
     */
    public boolean updateLastLoginTime(long playerId, long lastLoginTime) {
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("lastLoginTime", lastLoginTime);
        return mongoTemplate.updateFirst(query, update, this.clazz).getModifiedCount() > 0;
    }

    /**
     * 修改最近一次离线时间
     */
    public boolean updateLastOfflineTime(long playerId, long lastOfflineTime) {
        Query query = new Query(Criteria.where("playerId").is(playerId));

        Update update = new Update().set("lastOfflineTime", lastOfflineTime);
        return mongoTemplate.updateFirst(query, update, this.clazz).getModifiedCount() > 0;
    }

    /**
     * 查询第三方账号
     *
     * @param data
     * @return
     */
    public Account queryThirdAccount(LoginType loginType, String data) {
        // 构建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("thirdAccounts." + loginType).is(data));
        return mongoTemplate.findOne(query, this.clazz);
    }

    public Account setChannelValue(LoginType loginType, ChannelUserInfo channelUserInfo, Account account) {
        boolean add = account.addThirdAccount(loginType, channelUserInfo.getUserId());
        if (!add) {
            return null;
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
            //查询该账号是否存在
            Account account = queryAccountByPlayerId(playerId);
            if (account == null) {
                result.code = Code.NOT_FOUND;
            } else {
                Account tmpAccount = setChannelValue(loginType, channelUserInfo, account);
                if (tmpAccount == null) {
                    result.code = Code.QUERY_EXCEPTION;
                    return result;
                }
                result.code = Code.SUCCESS;
                result.data = account;
                save(account);
            }
            return result;
        });
        return result;
    }

    /**
     * 绑定第三方账号
     *
     * @param loginType
     * @return
     */
    public CommonResult<Account> addThirdAccount(long playerId, LoginType loginType, String data) {
        CommonResult<Account> result = new CommonResult<>(Code.FAIL);
        //要加锁，防止重复绑定
        String lockKey = getBindLockKey(loginType, data);
        redisLock.executeWithLock(lockKey, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES, TimeUnit.MILLISECONDS, () -> {
            //查询该账号是否存在
            Account account = queryAccountByPlayerId(playerId);
            if (account == null) {
                result.code = Code.NOT_FOUND;
            } else {
                boolean add = account.addThirdAccount(loginType, data);
                if (!add) {
                    result.code = Code.QUERY_EXCEPTION;
                    return result;
                }
                result.code = Code.SUCCESS;
                result.data = account;
                save(account);
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
}
