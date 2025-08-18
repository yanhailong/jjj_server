package com.jjg.game.core.service;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerDao;
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
import java.util.Optional;

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
    @Autowired
    protected PlayerDao playerDao;

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

    public CommonResult<Player> addDiamond(long playerId, long addNum, String addType) {
        return addDiamond(playerId, addNum, addType, null);
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
     * @param playerId
     * @param gold
     * @return
     */
    public CommonResult<Player> goldInSafeBox(long playerId, long gold,String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if(gold < 1){
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0,0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if(dataEntity.getGold() < gold) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("携带金币不足，存入保险箱失败 playerId={},gold={},inSafeBoxGold = {}", playerId,dataEntity.getGold(), gold);
                    return false;
                }

                beforeCoin[0] = dataEntity.getGold();
                beforeCoin[1] = dataEntity.getSafeBoxGold();

                dataEntity.setGold(dataEntity.getGold() - gold);
                dataEntity.setSafeBoxGold(dataEntity.getSafeBoxGold() + gold);
                return true;
            }
        });

        if(p != null){
            coreLogger.transSafeBoxGold(p, beforeCoin[0], beforeCoin[1],gold,addType,null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 将钻石存入保险箱
     * @param playerId
     * @param diamond
     * @return
     */
    public CommonResult<Player> diamondInSafeBox(long playerId, long diamond,String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if(diamond < 1){
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0,0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if(dataEntity.getDiamond() < diamond) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("携带钻石不足，存入保险箱失败 playerId={},diamond={},inSafeBoxDiamond = {}", playerId,dataEntity.getDiamond(), diamond);
                    return false;
                }

                beforeCoin[0] = dataEntity.getDiamond();
                beforeCoin[1] = dataEntity.getSafeBoxDiamond();

                dataEntity.setDiamond(dataEntity.getDiamond() - diamond);
                dataEntity.setSafeBoxDiamond(dataEntity.getSafeBoxDiamond() + diamond);
                return true;
            }
        });

        if(p != null){
            coreLogger.transSafeBoxDiamond(p, beforeCoin[0], beforeCoin[1],diamond,addType,null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 将金币从保险箱取出
     * @param playerId
     * @param gold
     * @return
     */
    public CommonResult<Player> goldOutFromSafeBox(long playerId, long gold,String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if(gold < 1){
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0,0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if(dataEntity.getSafeBoxGold() < gold) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("保险箱金币不足，取出失败 playerId={},safeBoxGold={},outFromSafeBoxGold = {}", playerId,dataEntity.getSafeBoxGold(), gold);
                    return false;
                }

                beforeCoin[0] = dataEntity.getGold();
                beforeCoin[1] = dataEntity.getSafeBoxGold();

                dataEntity.setGold(dataEntity.getGold() + gold);
                dataEntity.setSafeBoxGold(dataEntity.getSafeBoxGold() - gold);
                return true;
            }
        });

        if(p != null){
            coreLogger.transSafeBoxGold(p, beforeCoin[0], beforeCoin[1],gold,addType,null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }

        return result;
    }

    /**
     * 将钻石从保险箱取出
     * @param playerId
     * @param diamond
     * @return
     */
    public CommonResult<Player> diamondOutFromSafeBox(long playerId, long diamond,String addType) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        if(diamond < 1){
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0,0};
        Player p = checkAndSave(playerId, new DataSaveCallback<>() {
            @Override
            public void updateData(Player dataEntity) {

            }

            @Override
            public Boolean updateDataWithRes(Player dataEntity) {
                if(dataEntity.getSafeBoxDiamond() < diamond) {
                    result.code = Code.NOT_ENOUGH;
                    log.debug("保险箱钻石不足，取出失败 playerId={},safeBoxDiamond={},outFromSafeBoxDiamond = {}", playerId,dataEntity.getSafeBoxDiamond(), diamond);
                    return false;
                }

                beforeCoin[0] = dataEntity.getDiamond();
                beforeCoin[1] = dataEntity.getSafeBoxDiamond();

                dataEntity.setDiamond(dataEntity.getDiamond() + diamond);
                dataEntity.setSafeBoxDiamond(dataEntity.getSafeBoxDiamond() - diamond);
                return true;
            }
        });

        if(p != null){
            coreLogger.transSafeBoxDiamond(p, beforeCoin[0], beforeCoin[1],diamond,addType,null);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }
}
