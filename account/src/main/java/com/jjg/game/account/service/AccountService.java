package com.jjg.game.account.service;

import com.jjg.game.account.constant.AccountConstant;
import com.jjg.game.account.dao.PlayerIdDao;
import com.jjg.game.account.dto.LoginDto;
import com.jjg.game.account.logger.AccountLogger;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.BlackListService;
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
    private BlackListService blackListService;
    @Autowired
    private AccountLogger accountLogger;

    public CommonResult<Account> login(LoginType loginType, ChannelUserInfo channelUserInfo, LoginDto loginDto, String ip) {
        CommonResult<Account> accountResult = getOrCreateAccount(loginType, channelUserInfo, loginDto, ip);
        if (!accountResult.success()) {
            log.warn("获取或者创建账号失败,登录失败 loginType = {},channelUserId = {}", loginType, channelUserInfo.getUserId());
            return accountResult;
        }

        Account account = accountResult.data;

        //如果是游客登录，要检测是否已经认证
        if (loginType == LoginType.GUEST && account.getAccountType() != GameConstant.AccountType.GUEST) {
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
        if (blackListService.isBlackId(account.getPlayerId())) {
            log.debug("该用户在黑名单，无法登录 loginType = {},channelUserId = {},playerId = {}", loginType, channelUserInfo.getUserId(), account.getPlayerId());
            accountResult.code = Code.BAN_ACCOUNT;
            return accountResult;
        }

        if (!Objects.equals(loginDto.getMac(), account.getLastLoginMac())) {
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
    private CommonResult<Account> getOrCreateAccount(LoginType loginType, ChannelUserInfo channelUserInfo, LoginDto loginDto, String ip) {
        CommonResult<Account> result = new CommonResult<>(Code.FAIL);
        //要加锁，防止重复创建账号
        String lockKey = getLockKey(loginType, channelUserInfo);
        redisLock.executeWithLock(lockKey, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES, TimeUnit.MILLISECONDS, () -> {
            //查询该账号是否存在
            Account account = accountDao.queryThirdAccount(loginType, channelUserInfo.getUserId());
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
                account.setRegisterMac(loginDto.getMac());
                account.setLastLoginMac(loginDto.getMac());
                account.setChannel(ChannelType.valueOf(loginDto.getChannel()));

                account = accountDao.setChannelValue(loginType, channelUserInfo, account);

                account = accountDao.insert(account);

                accountLogger.register(channelUserInfo.getUserId(), loginType.getValue(), playerId, loginDto.getChannel(), ip, loginDto.getDevice(), loginDto.getMac());
            } else {
                account.setLastLoginMac(loginDto.getMac());
                accountDao.save(account);
            }

            result.code = Code.SUCCESS;
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
                return "applelogin:" + channelUserInfo.getUserId();
            }
            case FACEBOOK -> {
                return "facebooklogin:" + channelUserInfo.getUserId();
            }
            case PHONE -> {
                return "phonelogin:" + channelUserInfo.getUserId();
            }
            default -> {
                return "guestlogin:" + channelUserInfo.getUserId();
            }
        }
    }
}
