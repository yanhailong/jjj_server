package com.jjg.game.core.service;

import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerLoginTimeDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.CoreLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/19 10:00
 */
public class AbstractPlayerService {
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected static final String tableName = "player";
    private static final String lockTableName = "lockplayer:";

    @Autowired
    protected RedisTemplate redisTemplate;
    @Autowired
    protected RedisLock redisLock;
    @Autowired
    protected PlayerLoginTimeDao playerLoginTimeDao;
    @Autowired
    protected CoreLogger coreLogger;

    protected String getLockKey(long playerId){
        return lockTableName + playerId;
    }

    public interface PlayerSaveCallback {
        void newexe(Player player) throws UnsupportedEncodingException;
        void exe(Player player) throws UnsupportedEncodingException;
    }

    public interface PlayerSaveCallback2 {
        void exe(Player player) throws UnsupportedEncodingException;
    }

    public interface PlayerSaveCallback3 {
        boolean exe(Player player) throws UnsupportedEncodingException;
    }

    public Player checkAndSave(long playerId, PlayerSaveCallback3 cbk) {
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if(redisLock.lock(key)){
                try{
                    Player player = get(playerId);
                    if (player == null) {
                        return null;
                    }

                    //如果执行失败
                    if (!cbk.exe(player)) {
                        break;
                    }

                    redisTemplate.opsForHash().put(tableName, playerId, player);
                    return player;
                }catch (Exception e){
                    log.warn("保存player失败22 playerId={}",playerId, e);
                }finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                log.warn("保存player数据失败出现异常333,playerId = " + playerId, e);
            }

        }
        return null;
    }

    public Player doSave(long playerId, PlayerSaveCallback2 cbk) {
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if(redisLock.lock(key)){
                try{
                    Player player = get(playerId);
                    if (player == null) {
                        return null;
                    }
                    //如果执行失败
                    cbk.exe(player);
                    redisTemplate.opsForHash().put(tableName, playerId, player);
                    return player;
                }catch (Exception e){
                    log.warn("保存player失败212 playerId={}",playerId, e);
                }finally {
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
     * @param playerId
     * @param addNum
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addDiamond(long playerId, long addNum, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if(addNum == 0){
            log.warn("添加钻石错误 playerId={},addNum={}",playerId,addNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, player -> {
            beforeCoin[0] = player.getDiamond();
            if(addNum > 0){
                player.setDiamond(Math.min(Long.MAX_VALUE, player.getDiamond() + addNum));
            }else {
                long afterCoin = player.getDiamond() + addNum;
                if(afterCoin < 0){
                    result.code = Code.NOT_ENOUGHT;
                    return false;
                }
                player.setDiamond(afterCoin);
            }
            return true;
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useMoney(p,beforeCoin[0],addNum,addType,desc);
            result.code = Code.SUCCESS;
            result.data = p;
            return result;
        }
        return result;
    }

    /**
     * 设置vip等级
     * @param playerId
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> setVip(long playerId, int vipLevel, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if(vipLevel < 0){
            log.warn("设置vip等级错误 playerId={},vipLevel={}",playerId,vipLevel);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final int[] beforeLevel = {0};

        Player p = checkAndSave(playerId, player -> {
            beforeLevel[0] = player.getVipLevel();
            player.setVipLevel(vipLevel);
            return true;
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.vip(p,beforeLevel[0],vipLevel,addType,desc);
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
     * @param playerId
     * @param addNum
     * @param addType
     * @param desc
     * @return
     */
    public CommonResult<Player> addGold(long playerId, long addNum, String addType, String desc) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        if(addNum == 0){
            log.warn("添加金币错误 playerId={},addNum={}",playerId,addNum);
            result.code = Code.PARAM_ERROR;
            return result;
        }

        final long[] beforeCoin = {0};

        Player p = checkAndSave(playerId, player -> {
            beforeCoin[0] = player.getGold();
            if(addNum > 0){
                player.setGold(Math.min(Long.MAX_VALUE, player.getGold() + addNum));
            }else {
                long afterCoin = player.getGold() + addNum;
                if(afterCoin < 0){
                    result.code = Code.NOT_ENOUGHT;
                    return false;
                }
                player.setGold(afterCoin);
            }
            return true;
        });

        //记录日志
        if (p != null) {
            //TODO 后期要排除机器人的情况
            coreLogger.useMoney(p,beforeCoin[0],addNum,addType,desc);
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
        return (Player) redisTemplate.opsForHash().get(tableName,playerId);
    }
}
