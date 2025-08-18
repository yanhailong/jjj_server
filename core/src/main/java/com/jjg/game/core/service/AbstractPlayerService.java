package com.jjg.game.core.service;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerLoginTimeDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.logger.CoreLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/19 10:00
 */
public class AbstractPlayerService {
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final String tableName = "player";
    private final String lockTableName = "lockplayer:";

    @Autowired
    protected RedisTemplate<String, Player> redisTemplate;
    @Autowired
    protected RedisLock redisLock;
    @Autowired
    protected PlayerLoginTimeDao playerLoginTimeDao;
    @Autowired
    protected CoreLogger coreLogger;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }

    public Player checkAndSave(long playerId, DataSaveCallback<Player> cbk) {
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    Player player = get(playerId);
                    if (player == null || player instanceof RobotPlayer) {
                        return null;
                    }

                    //如果执行失败
                    if (!(boolean) cbk.updateDataWithRes(player)) {
                        break;
                    }
                    player.setUpdateTime(System.currentTimeMillis());
                    redisTemplate.opsForHash().put(tableName, playerId, player);
                    return player;
                } catch (Exception e) {
                    log.error("保存player失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                // TODO 需要阻塞的等待 30ms?如果一直拿不到redis锁，最差会将当前调用线程阻塞等待 REDIS_TRANSACTION_TRY_COUNT * 30 ms
                Thread.sleep(30);
            } catch (InterruptedException e) {
                log.error("保存player数据失败出现异常,playerId = {}", playerId, e);
            }

        }
        return null;
    }

    public Player doSave(long playerId, DataSaveCallback<Player> cbk) {
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
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
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.warn("保存player数据失败出现异常44,playerId = " + playerId, e);
            }
        }
        return null;
    }

    /**
     * 减少金币
     *
     * @param playerId
     * @param addNum
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addDiamond(long playerId, long addNum, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (addNum == 0) {
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
                if (addNum > 0) {
                    player.setDiamond(Math.min(Long.MAX_VALUE, player.getDiamond() + addNum));
                } else {
                    long afterCoin = player.getDiamond() + addNum;
                    if (afterCoin < 0) {
                        result.code = Code.NOT_ENOUGH;
                        return false;
                    }
                    player.setDiamond(afterCoin);
                }
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

    /**
     * 添加金币
     *
     * @param playerId
     * @param addNum
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addGold(long playerId, long addNum, String addType, String desc) {
        // TODO 添加金币时只能保证分布式服务状态下的更新同步，不能保证当前服的线程安全引起的数据同步问题
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if (addNum == 0) {
            log.warn("添加金币错误 playerId={},addNum={}", playerId, addNum);
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
                beforeCoin[0] = player.getGold();
                if (addNum > 0) {
                    player.setGold(Math.min(Long.MAX_VALUE, player.getGold() + addNum));
                } else {
                    long afterCoin = player.getGold() + addNum;
                    if (afterCoin < 0) {
                        result.code = Code.NOT_ENOUGH;
                        return false;
                    }
                    player.setGold(afterCoin);
                }
                return true;
            }
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useGold(p, beforeCoin[0], addNum, addType, desc);
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
     * 批量获取玩家
     */
    public List<Player> multiGetPlayer(List<Long> playerId) {
        HashOperations<String, String, Player> operations = redisTemplate.opsForHash();
        return operations.multiGet(tableName, playerId.stream().map(String::valueOf).toList());
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
}
