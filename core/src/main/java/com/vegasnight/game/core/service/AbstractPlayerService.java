package com.vegasnight.game.core.service;

import com.vegasnight.game.core.RedisLock;
import com.vegasnight.game.core.constant.Code;
import com.vegasnight.game.core.constant.GameConstant;
import com.vegasnight.game.core.dao.PlayerLoginTimeDao;
import com.vegasnight.game.core.data.CommonResult;
import com.vegasnight.game.core.data.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.UnsupportedEncodingException;

/**
 * 操作redis中的player数据
 * @author 11
 * @date 2025/5/26 11:24
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

    protected String getLockKey(long playerId){
        return lockTableName + playerId;
    }

    public interface PlayerSaveCallback {
        void exe(Player player) throws UnsupportedEncodingException;
    }

    public interface PlayerSaveCallback2 {
        boolean exe(Player player);
    }

    /**
     * 仅在登录时调用
     * 创建或保存  要记录登录时间
     * @param playerId
     * @param cbk
     * @return
     */
    public CommonResult<Player> loginAndNewOrSave(long playerId, PlayerSaveCallback cbk) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if(redisLock.lock(key)){
                try{
                    Player player = get(playerId);
                    if(player == null){
                        player = new Player();
                        player.setId(playerId);
                    }
                    cbk.exe(player);
                    //记录登录时间
                    playerLoginTimeDao.add(playerId, System.currentTimeMillis());
                    redisTemplate.opsForHash().put(tableName, playerId, player);
                    result.code = Code.SUCCESS;
                    result.data = player;
                    return result;
                }catch (Exception e){
                    log.warn("创建或保存对象异常1 playerId={}",playerId, e);
                }finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                log.warn("创建或保存对象异常2 playerId={}" ,playerId, e);
            }
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
