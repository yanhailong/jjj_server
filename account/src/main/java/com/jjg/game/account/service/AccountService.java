package com.jjg.game.account.service;

import com.jjg.game.account.constant.AccountConstant;
import com.jjg.game.account.dao.PlayerIdDao;
import com.jjg.game.account.data.*;
import com.jjg.game.account.logger.AccountLogger;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.BlackListDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.ChannelType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.LoginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/10/13 16:35
 */
@Service
public class AccountService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AccountDao accountDao;
    @Autowired
    private PlayerIdDao playerIdDao;
    @Autowired
    private RedisLock redisLock;
    @Autowired
    private BlackListDao blackListDao;
    @Autowired
    private AccountLogger accountLogger;

    public CommonResult<Account> login(LoginType loginType, ChannelUserInfo channelUserInfo, String mac, int channel) {
        CommonResult<Account> accountResult = getOrCreateAccount(loginType, channelUserInfo, mac, channel);
        if (!accountResult.success()) {
            log.warn("获取或者创建账号失败,登录失败 loginType = {},channelUserId = {}", loginType, channelUserInfo.getUserId());
            return accountResult;
        }

        Account account = accountResult.data;

        //如果是游客登录，要检测是否已经认证
        if (loginType == LoginType.GUEST && account.getAccountType() != AccountConstant.AccountType.GUEST) {
            log.debug("该用户已经认证，无法使用游客登录 loginType = {},channelUserId = {},playerId = {}", loginType, channelUserInfo.getUserId(), account.getPlayerId());
            accountResult.code = Code.PARAM_ERROR;
            return accountResult;
        }

        //是否被封号
        if (account.getStatus() == GameConstant.AccountStatus.BAN) {
            log.debug("该用户已被封号，无法登录 loginType = {},channelUserId = {},playerId = {}", loginType, channelUserInfo.getUserId(), account.getPlayerId());
            accountResult.code = Code.BAN_ACCOUNT;
            return accountResult;
        }

        //检测黑名单
        if (blackListDao.blackId(account.getPlayerId())) {
            log.debug("该用户在黑名单，无法登录 loginType = {},channelUserId = {},playerId = {}", loginType, channelUserInfo.getUserId(), account.getPlayerId());
            accountResult.code = Code.BAN_ACCOUNT;
            return accountResult;
        }

        if (!Objects.equals(mac, account.getLastLoginMac())) {
            accountDao.save(account);
        }
        return accountResult;
    }


    /**
     * 获取或者创建账号
     *
     * @param loginType
     * @param channelUserInfo
     * @return
     */
    private CommonResult<Account> getOrCreateAccount(LoginType loginType, ChannelUserInfo channelUserInfo, String mac, int channel) {
        CommonResult<Account> result = new CommonResult<>(Code.SUCCESS);
        //要加锁，防止重复创建账号
        String lockKey = getLockKey(loginType, channelUserInfo);
        redisLock.executeWithLock(lockKey,GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES, TimeUnit.MILLISECONDS,()->{
            //查询该账号是否存在
            Account account = getAccountByLoginType(loginType, channelUserInfo);
            if (account == null) {
                //注册新账号
                long playerId = playerIdDao.getNewId();
                // 如果player获取为0，则失败
                if (playerId <= 0) {
                    log.warn("创建新的账号，从数据库中获取玩家ID失败 loginType = {},channelUserId = {}", loginType, channelUserInfo.getUserId());
                    result.code = Code.FAIL;
                    return result;
                }
                account = new Account();
                account.setPlayerId(playerId);
                account.setRegisterMac(mac);
                account.setLastLoginMac(mac);
                account.setChannel(ChannelType.valueOf(channel));

                account = setChannelValue(loginType, channelUserInfo, account);

                account = accountDao.insert(account);

                accountLogger.register(channelUserInfo.getUserId(), loginType.getValue(), playerId);
            }

            result.data = account;
            return result;
        });
        return result;
    }


    private String getLockKey(LoginType loginType, ChannelUserInfo channelUserInfo) {
        switch (loginType) {
            case GUEST -> {
                return "guestlogin:" + channelUserInfo.getUserId();
            }
            case GOOGLE -> {
                return "googlelogin:" + channelUserInfo.getUserId();
            }
            case APPLE -> {
                return "applelogin:" + channelUserInfo.toString();
            }
            case FACEBOOK -> {
                return "facebooklogin:" + channelUserInfo.toString();
            }
            default -> {
                return "guestlogin:" + channelUserInfo.toString();
            }
        }
    }

    /**
     * 根据登录类型查询账号信息
     *
     * @param loginType
     * @param channelUserInfo
     * @return
     */
    private Account getAccountByLoginType(LoginType loginType, ChannelUserInfo channelUserInfo) {
        switch (loginType) {
            case GUEST -> {
                return accountDao.queryAccountByGuest(channelUserInfo.getUserId());
            }

            default -> {
                return accountDao.queryAccountByGuest(channelUserInfo.getUserId());
            }
        }
    }

    private Account setChannelValue(LoginType loginType, ChannelUserInfo channelUserInfo, Account account) {
        switch (loginType) {
            case GUEST -> {
                account.setGuest(channelUserInfo.getUserId());
                account.setAccountType(AccountConstant.AccountType.GUEST);
                return account;
            }
            case GOOGLE -> {
                GoogleUserInfo googleUserInfo = (GoogleUserInfo)channelUserInfo;
                account.setEmail(googleUserInfo.getEmail());
                account.setGoogleUserId(googleUserInfo.getUserId());
                account.setAccountType(AccountConstant.AccountType.VERIFIED);
                return account;
            }
            case FACEBOOK -> {
                account.setFacebookUserId(channelUserInfo.getUserId());
                account.setAccountType(AccountConstant.AccountType.VERIFIED);
                return account;
            }
            case PHONE -> {
                account.setPhoneNumber(channelUserInfo.getUserId());
                account.setAccountType(AccountConstant.AccountType.VERIFIED);
                return account;
            }

            default -> {
                account.setGuest(channelUserInfo.getUserId());
                account.setAccountType(AccountConstant.AccountType.GUEST);
                return account;
            }
        }
    }
}
