package com.jjg.game.core.dao;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
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
    @Autowired
    private GameEventManager gameEventManager;
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
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} playerId:{} ", key, playerId);
                return null;
            }
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
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    public Account checkAndSaveRes(long playerId, DataSaveCallback<Account> cbk) {
        String key = getLockKey(playerId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} playerId:{} ", key, playerId);
                return null;
            }
            Account account = queryAccountByPlayerId(playerId);
            if (account == null) {
                log.debug("获取account为空 playerId = {}", playerId);
                return null;
            }
            boolean flag = cbk.updateDataWithRes(account);
            if (!flag) {
                log.debug("条件判断未通过 playerId = {}", playerId);
                return null;
            }
            redisTemplate.opsForHash().put(DATA_TABLE_NAME, playerId, account);
            return account;
        } catch (Exception e) {
            log.warn("保存account失败 playerId={}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
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
    public CommonResult<Account> addThirdAccount(Player player, LoginType loginType, ChannelUserInfo channelUserInfo) {
        CommonResult<Account> result = new CommonResult<>(Code.FAIL);

        //要加锁，防止重复绑定
        String lockKey = getBindLockKey(loginType, channelUserInfo.getUserId());
        CommonResult<Account> accountCommonResult = redisLock.executeWithLock(lockKey, GameConstant.Redis.TIME, TimeUnit.MILLISECONDS, () -> {
            //查询该账号是否被绑定
            Account existBindAccount = queryThirdAccount(loginType, channelUserInfo.getUserId());
            if (existBindAccount != null) {
                log.warn("该账号已经存在，绑定失败 playerId={},loginType = {},bindData = {},hasBindPlayerId = {}", player.getId(), loginType, channelUserInfo.getUserId(), existBindAccount.getPlayerId());
                result.code = Code.EXIST;
                return result;
            }

            Account tmpAccount = checkAndSaveRes(player.getId(), new DataSaveCallback<>() {
                @Override
                public void updateData(Account dataEntity) {
                }

                @Override
                public boolean updateDataWithRes(Account dataEntity) {
                    //检查该玩家之前是否已经绑定
                    String thirdAccount = dataEntity.getThirdAccount(loginType);
                    if (StringUtils.isEmpty(thirdAccount)) {
                        setChannelValue(loginType, channelUserInfo, dataEntity);
                        return true;
                    }
                    log.warn("该玩家已经绑定了第三方账号，无法再次绑定 playerId = {},oldBindValue = {}", player.getId(), thirdAccount);
                    return false;
                }
            });

            if (tmpAccount == null) {
                result.code = Code.NOT_FOUND;
            } else {
                result.code = Code.SUCCESS;
                result.data = tmpAccount;
            }
            return result;
        });

        if (accountCommonResult.success()) {
            save(accountCommonResult.data, loginType, channelUserInfo.getUserId(), false);
            if (loginType == LoginType.PHONE) {
                gameEventManager.triggerEvent(new PlayerEvent(player, EGameEventType.BIND_PHONE, accountCommonResult.data, accountCommonResult.data));
            }
        }
        return result;
    }

    /**
     * 解绑第三方账号
     *
     * @param loginType
     * @return
     */
    public CommonResult<Account> removeThirdAccount(Player player, LoginType loginType) {
        CommonResult<Account> result = new CommonResult<>(Code.FAIL);

        //要加锁
        String lockKey = getLockKey(player.getId());
        redisLock.executeWithLock(lockKey, GameConstant.Redis.TIME, TimeUnit.MILLISECONDS, () -> {
            Account tmpAccount = checkAndSave(player.getId(), a -> {
                String thirdAccountData = a.removeThirdAccount(loginType);
                if (StringUtils.isNotBlank(thirdAccountData)) {
                    redisTemplate.opsForHash().delete(thirdTableName(loginType), thirdAccountData);
                }
            });

            if (tmpAccount == null) {
                log.warn("解绑第三方账号时获取account数据未找到 playerId={}", player.getId());
                result.code = Code.NOT_FOUND;
            } else {
                result.code = Code.SUCCESS;
                result.data = tmpAccount;
            }
            return result;
        });

        return result;
    }

    /**
     * 批量获取玩家
     */
    public Map<Long, Account> multiGetAccountMap(Collection<Long> ids) {
        List<Account> players = multiGetAccount(ids);
        return players.stream()
                .filter(Objects::nonNull)
                .collect(HashMap::new, (map, e) -> map.put(e.getPlayerId(), e), HashMap::putAll);
    }

    /**
     * 批量获取账号
     */
    public List<Account> multiGetAccount(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        HashOperations<String, Long, Account> operations = redisTemplate.opsForHash();
        List<Account> redisAccount = operations.multiGet(DATA_TABLE_NAME, ids);
        // 过滤空数据
        redisAccount = redisAccount.stream().filter(Objects::nonNull).toList();
        if (redisAccount.size() == ids.size()) {
            return new ArrayList<>(redisAccount);
        }
        Map<Long, Account> accountMap =
                redisAccount.stream().filter(Objects::nonNull)
                        .collect(HashMap::new, (map, e) -> map.put(e.getPlayerId(), e), HashMap::putAll);

        // 需要从数据中查询
        Set<Long> queryFromDb = new HashSet<>(ids);
        queryFromDb.removeAll(accountMap.keySet());
        List<Account> players = findAllById(queryFromDb);
        // 如果数据库中也查不到，直接返回从redis中查询到的数据
        if (players.isEmpty()) {
            return new ArrayList<>(accountMap.values().stream().toList());
        }
        players.addAll(accountMap.values());
        return new ArrayList<>(players.stream().filter(Objects::nonNull).toList());
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
        return save(account, loginType, data, true);
    }

    public Account save(Account account, LoginType loginType, String data, boolean saveData) {
        if (saveData) {
            redisTemplate.opsForHash().put(DATA_TABLE_NAME, account.getPlayerId(), account);
        }
        redisTemplate.opsForHash().put(thirdTableName(loginType), data, account.getPlayerId());
        return account;
    }

    public void moveToMongo(long playerId) {
        Account account = queryAccountByPlayerId(playerId, false);
        if (account == null) {
            return;
        }
        save(account);
        redisTemplate.opsForHash().delete(DATA_TABLE_NAME, playerId);
    }
}
