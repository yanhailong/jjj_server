package com.jjg.game.core.service;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerDao;
import com.jjg.game.core.dao.PlayerLoginTimeDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.utils.VipUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PlayerLevelConfigCfg;
import org.apache.kafka.common.utils.PrimitiveRef;
import org.apache.kafka.common.utils.PrimitiveRef.LongRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author 11
 * @date 2025/6/19 10:00
 */
public class AbstractPlayerService {
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final String tableName = "player";
    private final String lockTableName = "lockplayer:";
    protected final String nickTableName = "playerNickToId";

    @Autowired
    protected RedisTemplate<String, Player> redisTemplate;
    @Autowired
    protected RedisLock redisLock;
    @Autowired
    protected PlayerLoginTimeDao playerLoginTimeDao;
    @Autowired
    protected CoreLogger coreLogger;
    @Autowired
    protected PlayerDao playerDao;
    @Autowired
    protected PlayerBuffService playerBuffService;
    @Autowired
    protected CoreSendMessageManager sendMessageManager;
    @Autowired
    protected GameEventManager eventManager;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }

    public Player checkAndSave(long playerId, DataSaveCallback<Player> cbk) {
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            Player player = get(playerId);
            if (player == null || player instanceof RobotPlayer) {
                return null;
            }

            //如果执行失败
            if (!(boolean) cbk.updateDataWithRes(player)) {
                return null;
            }
            player.setUpdateTime(System.currentTimeMillis());
            redisTemplate.opsForHash().put(tableName, playerId, player);
            return player;
        } catch (Exception e) {
            log.error("保存player失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    public Player doSave(long playerId, DataSaveCallback<Player> cbk) {
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            Player player = get(playerId);
            // 找不到的玩家或者机器人玩家不保存数据
            if (player == null || player instanceof RobotPlayer) {
                return null;
            }
            //如果执行失败
            cbk.updateData(player);
            player.setUpdateTime(System.currentTimeMillis());
            redisTemplate.opsForHash().put(tableName, playerId, player);
            return player;
        } catch (Exception e) {
            log.warn("保存player失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    public <K> Player doSave(long playerId, K k, BiConsumer<Player, K> cbk) {
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            Player player = get(playerId);
            // 找不到的玩家或者机器人玩家不保存数据
            if (player == null || player instanceof RobotPlayer) {
                return null;
            }
            cbk.accept(player, k);
            player.setUpdateTime(System.currentTimeMillis());
            redisTemplate.opsForHash().put(tableName, playerId, player);
            return player;
        } catch (Exception e) {
            log.warn("保存player失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    public CommonResult<Player> addDiamond(long playerId, long addNum, String addType) {
        return addDiamond(playerId, addNum, addType, null);
    }

    public CommonResult<Player> addSafeBoxDiamond(long playerId, long addNum, String addType) {
        return addSafeBoxDiamond(playerId, addNum, addType, null);
    }

    public CommonResult<Player> addGoldAndDiamond(long playerId, long goldNum, long diamondNum, String addType) {
        return addGoldAndDiamond(playerId, goldNum, diamondNum, addType, false, null);
    }

    public CommonResult<Player> deductDiamond(long playerId, long addNum, String addType) {
        return deductDiamond(playerId, addNum, addType, null);
    }

    public CommonResult<Player> deductSafeBoxDiamond(long playerId, long addNum, String addType) {
        return deductSafeBoxDiamond(playerId, addNum, addType, null);
    }

    /**
     * 添加钻石
     *
     * @param playerId
     * @param addNum
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addDiamond(long playerId, long addNum, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (addNum < 1) {
            log.warn("添加钻石错误 playerId={},addNum={}", playerId, addNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getDiamond();
                player.setDiamond(Math.min(Long.MAX_VALUE, player.getDiamond() + addNum));
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useDiamond(p, beforeCoin[0], addNum, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 添加保险箱钻石
     *
     * @param playerId
     * @param addNum
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addSafeBoxDiamond(long playerId, long addNum, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (addNum < 1) {
            log.warn("添加钻石错误 playerId={},addNum={}", playerId, addNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getSafeBoxDiamond();
                player.setSafeBoxDiamond(Math.min(Long.MAX_VALUE, player.getSafeBoxDiamond() + addNum));
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useSafeBoxDiamond(p, beforeCoin[0], addNum, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 添加金币和钻石
     *
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addGoldAndDiamond(long playerId, long goldNum, long diamondNum, String addType,
                                                  boolean isNotify, String desc) {
        // TODO 添加金币时只能保证分布式服务状态下的更新同步，不能保证当前服的线程安全引起的数据同步问题
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (goldNum < 0 || diamondNum < 0 || (goldNum + diamondNum) < 1) {
            log.warn("添加金币和钻石数量错误 playerId={},goldNum={},diamondNum = {}", playerId, goldNum, diamondNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0, 0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getGold();
                beforeCoin[1] = player.getDiamond();
                player.setGold(player.getGold() + goldNum);
                player.setDiamond(player.getDiamond() + diamondNum);
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            if (goldNum > 0) {
                coreLogger.useGold(p, beforeCoin[0], goldNum, addType, desc);
            }
            if (diamondNum > 0) {
                coreLogger.useDiamond(p, beforeCoin[1], diamondNum, addType, desc);
            }
            result.code = Code.SUCCESS;
            result.data = p;

            if (isNotify) {
                sendMessageManager.buildPlayerMoneyInfo(p);
            }
            return result;
        }
        return result;
    }

    /**
     * 扣除钻石
     *
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> deductDiamond(long playerId, long num, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (num < 1) {
            log.warn("扣除钻石错误 playerId={},num={}", playerId, num);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getDiamond();
                long afterCoin = player.getDiamond() - num;
                if (afterCoin < 0) {
                    result.code = Code.NOT_ENOUGH;
                    return false;
                }
                player.setDiamond(afterCoin);
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useDiamond(p, beforeCoin[0], -num, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 扣除钻石
     *
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> deductSafeBoxDiamond(long playerId, long num, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (num < 1) {
            log.warn("扣除保险箱钻石错误 playerId={},num={}", playerId, num);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getSafeBoxDiamond();
                long afterCoin = player.getSafeBoxDiamond() - num;
                if (afterCoin < 0) {
                    result.code = Code.NOT_ENOUGH;
                    return false;
                }
                player.setSafeBoxDiamond(afterCoin);
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useSafeBoxDiamond(p, beforeCoin[0], -num, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 设置vip等级
     *
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> setVip(long playerId, int vipLevel, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (vipLevel < 0) {
            log.warn("设置vip等级错误 playerId={},vipLevel={}", playerId, vipLevel);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final int[] beforeLevel = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<Player>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeLevel[0] = player.getVipLevel();
                player.setVipLevel(vipLevel);
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.vip(p, beforeLevel[0], vipLevel, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    public CommonResult<Player> addGold(long playerId, long addNum, String addType) {
        return addGold(playerId, addNum, addType, null);
    }

    public CommonResult<Player> deductGold(long playerId, long addNum, String addType) {
        return deductGold(playerId, addNum, addType, null, false);
    }

    public CommonResult<Player> deductGold(long playerId, long addNum, String addType, String desc) {
        return deductGold(playerId, addNum, addType, desc, false);
    }

    public CommonResult<Player> deductGold(long playerId, long addNum, String addType, boolean isNotify) {
        return deductGold(playerId, addNum, addType, null, isNotify);
    }

    public CommonResult<Player> deductSafeBoxGold(long playerId, long addNum, String addType) {
        return deductSafeBoxGold(playerId, addNum, addType, null);
    }

    public CommonResult<Player> betDeductGold(long playerId, long addNum, boolean effective, String addType) {
        return betDeductGold(playerId, addNum, addType, effective, null);
    }

    public CommonResult<Player> deductGoldAndDiamond(long playerId, long goldNum, long diamondNum, String addType) {
        return deductGoldAndDiamond(playerId, goldNum, diamondNum, addType, null);
    }

    /**
     * 添加金币
     */
    public CommonResult<Player> addGold(long playerId, long addNum, String addType, boolean isNotify) {
        return addGold(playerId, addNum, addType, "", isNotify);
    }

    /**
     * 添加金币
     */
    public CommonResult<Player> addGold(long playerId, long addNum, String addType, String desc) {
        return addGold(playerId, addNum, addType, desc, false);
    }

    /**
     * 添加保险箱金币
     */
    public CommonResult<Player> addSafeBoxGold(long playerId, long addNum, String addType, String desc) {
        return addSafeBoxGold(playerId, addNum, addType, desc, false);
    }


    /**
     * 添加金币
     *
     * @param playerId 玩家ID
     * @param addNum   添加数量
     * @param addType  添加类型
     * @param isNotify 是否向客户端通知
     * @param desc     dec
     * @return 最新Player
     */
    protected CommonResult<Player> addGold(long playerId, long addNum, String addType, String desc, boolean isNotify) {
        LongRef playerBeforeGoldRef = PrimitiveRef.ofLong(0);
        Supplier<Player> supplier = () -> checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                playerBeforeGoldRef.value = player.getGold();
                player.setGold(Math.min(Long.MAX_VALUE, player.getGold() + addNum));
                return true;
            }
        });
        return addGold(playerId, addNum, addType, desc, isNotify, supplier, playerBeforeGoldRef);
    }


    /**
     * 添加金币
     *
     * @param playerId           玩家ID
     * @param addNum             添加数量
     * @param addType            添加类型
     * @param isNotify           是否向客户端通知
     * @param desc               dec
     * @param updatePlayerMethod 金币更新方法
     * @param beforeUpdateGold   更新之前的金币
     * @return 最新Player
     */
    public <P extends Player> CommonResult<P> addGold(
            long playerId,
            long addNum,
            String addType,
            String desc,
            boolean isNotify,
            Supplier<P> updatePlayerMethod,
            LongRef beforeUpdateGold) {
        CommonResult<P> result = new CommonResult<>(Code.FAIL);
        if (addNum < 1) {
            log.warn("添加金币错误 playerId={},addNum={}", playerId, addNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }
        // 执行更新方法
        P p = updatePlayerMethod.get();
        //记录日志
        if (p != null) {
            coreLogger.useGold(p, beforeUpdateGold.value, addNum, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            if (isNotify) {
                // 推送金币变化消息
                sendMessageManager.buildPlayerMoneyInfo(p);
            }
            return result;
        }
        return result;
    }

    /**
     * 添加保险箱金币
     *
     * @param playerId 玩家ID
     * @param addNum   添加数量
     * @param addType  添加类型
     * @param isNotify 是否向客户端通知
     * @param desc     dec
     * @return 最新Player
     */
    protected CommonResult<Player> addSafeBoxGold(long playerId, long addNum, String addType, String desc,
                                                  boolean isNotify) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (addNum < 1) {
            log.warn("添加保险箱金币错误 playerId={},addNum={}", playerId, addNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getSafeBoxGold();
                player.setSafeBoxGold(Math.min(Long.MAX_VALUE, player.getSafeBoxGold() + addNum));
                return true;
            }
        });

        //记录日志
        if (p != null) {
            coreLogger.useSafeBoxGold(p, beforeCoin[0], addNum, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            if (isNotify) {
                // 推送金币变化消息
                sendMessageManager.buildPlayerMoneyInfo(p);
            }
            return result;
        }

        return result;
    }


    /**
     * 扣除金币
     */
    protected CommonResult<Player> deductGold(long playerId, long num, String addType, String desc, boolean isNotify) {
        LongRef ref = PrimitiveRef.ofLong(0);
        Supplier<Player> supplier = () -> checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                ref.value = player.getGold();
                long afterCoin = player.getGold() - num;
                if (afterCoin < 0) {
                    return false;
                }
                player.setGold(afterCoin);
                return true;
            }
        });
        return deductGold(playerId, num, addType, desc, isNotify, supplier, ref);
    }

    /**
     * 扣除金币
     */
    public <P extends Player> CommonResult<P> deductGold(
            long playerId,
            long num,
            String addType,
            String desc,
            boolean isNotify,
            Supplier<P> playerUpdateMethod,
            LongRef beforeUpdateGold) {
        CommonResult<P> result = new CommonResult<>(Code.FAIL);
        if (num < 1) {
            log.warn("扣除金币错误 playerId={},num={}", playerId, num);
            result.code = Code.PARAM_ERROR;
            return result;
        }
        // 执行更新方法
        P p = playerUpdateMethod.get();
        //记录日志
        if (p != null) {
            if (isNotify) {
                // 推送金币变化消息
                sendMessageManager.buildPlayerMoneyInfo(p);
            }
            //TODO 后期要排除机器人的情况
            coreLogger.useGold(p, beforeUpdateGold.value, -num, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        } else {
            result.code = Code.NOT_FOUND;
        }
        return result;
    }

    /**
     * 扣除保险箱金币
     *
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> deductSafeBoxGold(long playerId, long num, String addType, String desc) {
        // TODO 添加金币时只能保证分布式服务状态下的更新同步，不能保证当前服的线程安全引起的数据同步问题
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (num < 1) {
            log.warn("扣除保险箱金币错误 playerId={},num={}", playerId, num);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getSafeBoxGold();
                long afterCoin = player.getSafeBoxGold() - num;
                if (afterCoin < 0) {
                    result.code = Code.NOT_ENOUGH;
                    return false;
                }
                player.setSafeBoxGold(afterCoin);
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useSafeBoxGold(p, beforeCoin[0], -num, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 扣除金币和钻石
     *
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> deductGoldAndDiamond(
        long playerId, long goldNum, long diamondNum, String addType, String desc) {
        // TODO 添加金币时只能保证分布式服务状态下的更新同步，不能保证当前服的线程安全引起的数据同步问题
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (goldNum < 0 || diamondNum < 0 || (goldNum + diamondNum) < 1) {
            log.warn("扣除金币和钻石数量错误 playerId={},goldNum={},diamondNum = {}", playerId, goldNum, diamondNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0, 0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getGold();
                beforeCoin[1] = player.getDiamond();
                long afterGold = player.getGold() - goldNum;
                long afterDiamond = player.getDiamond() - diamondNum;
                if (afterGold < 0) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("同时扣除金币钻石时，金币不足  playerId = {},gold = {},deductGold = {}", playerId, player.getGold(),
                            goldNum);
                    return false;
                }
                if (afterDiamond < 0) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("同时扣除金币钻石时，钻石不足  playerId = {},diamond = {},deductDiamond = {}", playerId,
                            player.getDiamond(), diamondNum);
                    return false;
                }
                player.setGold(afterGold);
                player.setDiamond(afterDiamond);
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            if (goldNum > 0) {
                coreLogger.useGold(p, beforeCoin[0], -goldNum, addType, desc);
            }
            if (diamondNum > 0) {
                coreLogger.useDiamond(p, beforeCoin[1], -diamondNum, addType, desc);
            }
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 押注扣除金币
     *
     * @param playerId
     * @param addType
     * @param desc
     * @param effective ture 是有效流水
     * @return
     */
    public CommonResult<Player> betDeductGold(long playerId, long num, String addType, boolean effective, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (num < 1) {
            log.warn("押注扣除金币错误 playerId={},num={}", playerId, num);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        //基础经验倍率
        int baseExpProp = GameDataManager.getGlobalConfigCfg(GameConstant.GlobalConfig.ID_BASE_EXP_PROP).getIntValue();
        //基础流水倍率
        int baseStatementProp =
                GameDataManager.getGlobalConfigCfg(GameConstant.GlobalConfig.ID_BASE_STATEMENT_PROP).getIntValue();

        //获取buff，是否有经验和流水的加成
        List<PlayerBuffDetail> expPropDetails = null;
        List<PlayerBuffDetail> statementPropDetails = null;
        PlayerBuff playerBuff = playerBuffService.get(playerId);
        if (playerBuff != null && playerBuff.getDetails() != null && !playerBuff.getDetails().isEmpty()) {
            expPropDetails = playerBuff.getDetails().get(GameConstant.PlayerBuff.TYPE_EXP_PROP);
            statementPropDetails = playerBuff.getDetails().get(GameConstant.PlayerBuff.TYPE_STATEMENT_PROP);
        }

        BigDecimal expProp = playerBuffService.calProp(baseExpProp, expPropDetails);
        BigDecimal statementProp = playerBuffService.calProp(baseStatementProp, statementPropDetails);

        BigDecimal value = BigDecimal.valueOf(num);
        //计算应获得的流水
        BigDecimal statement = value.multiply(statementProp);
        //计算应该增加的经验
        long addExp = statement.multiply(expProp).longValue();

        final BigDecimal finalStatementProp = statementProp;
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getGold();
                long afterCoin = player.getGold() - num;
                if (afterCoin < 0) {
                    result.code = Code.NOT_ENOUGH;
                    return false;
                }
                player.setGold(afterCoin);
                //获取当前等级升级需要的经验
                PlayerLevelConfigCfg cfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
                if (cfg == null) {
                    //增加经验
                    player.setExp(player.getExp() + addExp);
                    log.debug("获取等级经验配置失败 playerId={},level={}", playerId, player.getLevel());
                    return true;
                }

                long tmpAddExp = addExp;
                //检查配置中，是否有额外的流水系数
                if (cfg.getProp() > 0) {
                    BigDecimal tmpStatementProp = playerBuffService.calProp(finalStatementProp, cfg.getProp());
                    tmpAddExp = value.multiply(tmpStatementProp).multiply(expProp).longValue();
                }
                //增加经验
                player.setExp(player.getExp() + tmpAddExp);

                player = levelUp(player, cfg);
                if (effective) {
                    if (VipUtil.checkVipLevel(player, num)) {
                        sendMessageManager.buildPlayerMoneyInfo(player);
                    }
                }
                log.info("玩家押注获取经验 playerId = {},addExp = {},level = {}", playerId, tmpAddExp, player.getLevel());
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useGold(p, beforeCoin[0], -num, addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 通过玩家ID获取玩家对象
     *
     * @param playerId 玩家ID
     * @return 玩家对象
     */
    public Player get(long playerId) {
        HashOperations<String, String, Player> operations = redisTemplate.opsForHash();
        return operations.get(tableName, playerId);
    }

    /**
     * 通过玩家昵称获取玩家对象
     *
     * @return 玩家对象
     */
    public Player getByNick(String nick) {
        return playerDao.queryByName(nick);
    }

    /**
     * 批量获取玩家
     */
    public List<Player> multiGetPlayer(Collection<Long> playerId) {
        HashOperations<String, Long, Player> operations = redisTemplate.opsForHash();
        return operations.multiGet(tableName, playerId);
    }

    /**
     * 批量获取玩家
     */
    public Map<Long, Player> multiGetPlayerMap(Collection<Long> playerId) {
        List<Player> players = multiGetPlayer(playerId);
        return players.stream().collect(HashMap::new, (map, e) -> map.put(e.getId(), e), HashMap::putAll);
    }

    /**
     * 通过玩家ID获取玩家对象并更新playerController中的对象值
     *
     * @param playerController 玩家controller
     * @return 玩家对象
     */
    public Player getOrUpdatePlayerController(PlayerController playerController) {
        HashOperations<String, String, Player> operations = redisTemplate.opsForHash();
        Player player = operations.get(tableName, playerController.playerId());
        playerController.setPlayer(player);
        return player;
    }

    /**
     * 查询player对象
     * 先查询redis
     * 再查询mongodb
     *
     * @param playerId
     * @return
     */
    public Player getFromAllDB(long playerId) {
        Player player = get(playerId);
        if (player != null) {
            return player;
        }

        Optional<Player> optional = playerDao.findById(playerId);
        return optional.orElse(null);
    }

    /**
     * 将金币存入保险箱
     *
     * @param playerId
     * @param gold
     * @return
     */
    public CommonResult<Player> goldInSafeBox(long playerId, long gold, String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (gold < 1) {
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0, 0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if (dataEntity.getGold() < gold) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("携带金币不足，存入保险箱失败 playerId={},gold={},inSafeBoxGold = {}", playerId, dataEntity.getGold()
                            , gold);
                    return false;
                }

                beforeCoin[0] = dataEntity.getGold();
                beforeCoin[1] = dataEntity.getSafeBoxGold();

                dataEntity.setGold(dataEntity.getGold() - gold);
                dataEntity.setSafeBoxGold(dataEntity.getSafeBoxGold() + gold);
                return true;
            }
        });

        if (p != null) {
            coreLogger.transSafeBoxGold(p, beforeCoin[0], beforeCoin[1], gold, addType, null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 将钻石存入保险箱
     *
     * @param playerId
     * @param diamond
     * @return
     */
    public CommonResult<Player> diamondInSafeBox(long playerId, long diamond, String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (diamond < 1) {
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0, 0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if (dataEntity.getDiamond() < diamond) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("携带钻石不足，存入保险箱失败 playerId={},diamond={},inSafeBoxDiamond = {}", playerId,
                            dataEntity.getDiamond(), diamond);
                    return false;
                }

                beforeCoin[0] = dataEntity.getDiamond();
                beforeCoin[1] = dataEntity.getSafeBoxDiamond();

                dataEntity.setDiamond(dataEntity.getDiamond() - diamond);
                dataEntity.setSafeBoxDiamond(dataEntity.getSafeBoxDiamond() + diamond);
                return true;
            }
        });

        if (p != null) {
            coreLogger.transSafeBoxDiamond(p, beforeCoin[0], beforeCoin[1], diamond, addType, null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 将金币从保险箱取出
     *
     * @param playerId
     * @param gold
     * @return
     */
    public CommonResult<Player> goldOutFromSafeBox(long playerId, long gold, String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (gold < 1) {
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0, 0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if (dataEntity.getSafeBoxGold() < gold) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("保险箱金币不足，取出失败 playerId={},safeBoxGold={},outFromSafeBoxGold = {}", playerId,
                            dataEntity.getSafeBoxGold(), gold);
                    return false;
                }

                beforeCoin[0] = dataEntity.getGold();
                beforeCoin[1] = dataEntity.getSafeBoxGold();

                dataEntity.setGold(dataEntity.getGold() + gold);
                dataEntity.setSafeBoxGold(dataEntity.getSafeBoxGold() - gold);
                return true;
            }
        });

        if (p != null) {
            coreLogger.transSafeBoxGold(p, beforeCoin[0], beforeCoin[1], gold, addType, null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }

        return result;
    }

    /**
     * 将钻石从保险箱取出
     *
     * @param playerId
     * @param diamond
     * @return
     */
    public CommonResult<Player> diamondOutFromSafeBox(long playerId, long diamond, String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if (diamond < 1) {
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0, 0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if (dataEntity.getSafeBoxDiamond() < diamond) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("保险箱钻石不足，取出失败 playerId={},safeBoxDiamond={},outFromSafeBoxDiamond = {}", playerId,
                            dataEntity.getSafeBoxDiamond(), diamond);
                    return false;
                }

                beforeCoin[0] = dataEntity.getDiamond();
                beforeCoin[1] = dataEntity.getSafeBoxDiamond();

                dataEntity.setDiamond(dataEntity.getDiamond() + diamond);
                dataEntity.setSafeBoxDiamond(dataEntity.getSafeBoxDiamond() - diamond);
                return true;
            }
        });

        if (p != null) {
            coreLogger.transSafeBoxDiamond(p, beforeCoin[0], beforeCoin[1], diamond, addType, null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 升级
     *
     * @param player
     * @param cfg
     * @return
     */
    protected Player levelUp(Player player, PlayerLevelConfigCfg cfg) {
        if (cfg.getLevelUpExp() < 1) {
            return player;
        }
        long oldLevel = player.getLevel();
        int maxLevel = GameDataManager.getPlayerLevelConfigCfgList().size();
        for (int i = 0; i < maxLevel; i++) {
            //判断经验是否足够升级
            long diffExp = player.getExp() - cfg.getLevelUpExp();
            if (diffExp < 0) {
                break;
            }
            player.setExp(diffExp);
            player.setLevel(player.getLevel() + 1);

            cfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
            if (cfg == null || cfg.getLevelUpExp() < 1) {
                break;
            }
        }
        // 升级需要抛升级事件
        if (player.getLevel() != oldLevel) {
            eventManager.triggerEvent(
                    new PlayerEvent(player, EGameEventType.PLAYER_LEVEL, oldLevel, player.getLevel()));
        }
        return player;
    }

    /**
     * 将昵称转换为安全的存储格式
     *
     * @param nick
     * @return
     */
    private String encodeNickname(String nick) {
        nick = nick.trim();
        byte[] bytes = nick.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String decodeNickname(String encodedNick) {
        byte[] bytes = Base64.getDecoder().decode(encodedNick);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 因为玩家昵称不能重复，所以要单独存储
     *
     * @param playerId
     * @param nick
     */
    public boolean savePlayerNick(long playerId, String nick) {
        String encodedNick = encodeNickname(nick);
        return redisTemplate.opsForHash().putIfAbsent(nickTableName, encodedNick, playerId);
    }

    public long queryPlayerIdByNick(String nick) {
        Object o = redisTemplate.opsForHash().get(nickTableName, encodeNickname(nick));
        if (o == null) {
            return 0;
        }

        return Long.parseLong(o.toString());
    }

    /**
     * 检查昵称是否已经存在
     *
     * @param nick
     * @return
     */
    public boolean nickExist(String nick) {
        return redisTemplate.opsForHash().hasKey(nickTableName, encodeNickname(nick));
    }


    /**
     * gm使用 修改金币
     * 该功能为gm专用，因为正常情况不会将玩家的金币钻石设置为负数
     *
     * @param playerId 玩家ID
     * @param num      添加数量
     * @param addType  添加类型
     * @param desc     dec
     * @return 最新Player
     */
    public CommonResult<Player> gmSetGold(long playerId, long num, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getGold();

                if (num >= 0) {
                    player.setGold(Math.min(Long.MAX_VALUE, num));
                } else {
                    player.setGold(Math.max(Long.MIN_VALUE, num));
                }
                return true;
            }
        });

        //记录日志
        if (p != null) {
            coreLogger.useGold(p, beforeCoin[0], beforeCoin[0] - p.getGold(), addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * gm使用 修改钻石
     * 该功能为gm专用，因为正常情况不会将玩家的金币钻石设置为负数
     *
     * @param playerId 玩家ID
     * @param num      添加数量
     * @param addType  添加类型
     * @param desc     dec
     * @return 最新Player
     */
    public CommonResult<Player> gmSetDiamond(long playerId, long num, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(Player player) {
                beforeCoin[0] = player.getDiamond();

                if (num >= 0) {
                    player.setDiamond(Math.min(Long.MAX_VALUE, num));
                } else {
                    player.setDiamond(Math.max(Long.MIN_VALUE, num));
                }
                return true;
            }
        });

        //记录日志
        if (p != null) {
            coreLogger.useDiamond(p, beforeCoin[0], beforeCoin[0] - p.getDiamond(), addType, desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }
}
