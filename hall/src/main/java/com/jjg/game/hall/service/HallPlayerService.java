package com.jjg.game.hall.service;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerDao;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.AbstractPlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.PlayerSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author 11
 * @date 2025/5/26 16:49
 */
@Service
public class HallPlayerService extends AbstractPlayerService {
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private PlayerPackService playerPackService;

    /**
     * 仅在登录时调用
     * 创建或保存  要记录登录时间
     *
     * @param playerId
     * @param cbk
     * @return
     */
    public CommonResult<Player> loginAndNewOrSave(long playerId, DataSaveCallback<Player> cbk) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            Player player = getFromAllDB(playerId);
            if (player == null) {
                player = new Player();
                player.setId(playerId);
                cbk.updateData(player);
            } else {
                cbk.updateData(player);
            }
            //记录登录时间
            playerLoginTimeDao.add(playerId, System.currentTimeMillis());
            redisTemplate.opsForHash().put(tableName, playerId, player);
            result.code = Code.SUCCESS;
            result.data = player;
            return result;
        } catch (Exception e) {
            log.warn("创建或保存对象异常 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
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
        Player player = super.get(playerId);
        if (player != null) {
            return player;
        }

        Optional<Player> optional = playerDao.findById(playerId);
        return optional.orElse(null);
    }

    /**
     * 从redis删除过期的player数据,并且存储到mongodb
     *
     * @param playerId
     * @param expireTime
     * @return
     */
    public boolean clear(long playerId, long expireTime) {
        if (playerId < 1) {
            return false;
        }

        Double score = playerLoginTimeDao.score(playerId);
        //如果再次检测到玩家数据还没有过期,就不用保存
        if (score != null && score > expireTime) {
            return false;
        }

        Player player = get(playerId);
        if (player != null) {
            //如果在线就暂时不保存到mongodb
            boolean online = playerSessionService.hasSession(playerId);
            if (online) {
                return false;
            }

            playerDao.save(player);
        }

        redisTemplate.opsForHash().delete(tableName, playerId);
        playerPackService.moveToMongo(playerId);
        playerLastGameInfoDao.deleteById(playerId);
        playerLoginTimeDao.remove(playerId);
        return true;
    }
}
