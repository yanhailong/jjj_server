package com.vegasnight.game.hall.service;

import com.vegasnight.game.core.dao.PlayerDao;
import com.vegasnight.game.core.dao.PlayerLastGameInfoDao;
import com.vegasnight.game.core.data.Player;
import com.vegasnight.game.core.service.AbstractPlayerService;
import com.vegasnight.game.core.service.PlayerSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author 11
 * @date 2025/5/26 16:49
 */
@Service
public class PlayerService extends AbstractPlayerService {
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private PlayerSessionService playerSessionService;

    public Player getFromAllDB(long playerId) {
        Player player = super.get(playerId);
        if(player != null){
            return player;
        }

        Optional<Player> optional = playerDao.findById(playerId);
        return optional.orElse(null);
    }

    /**
     * 从redis删除过期的player数据,并且存储到mongodb
     * @param playerId
     * @param expireTime
     * @return
     */
    public boolean clear(long playerId,long expireTime){
        if(playerId < 1){
            return false;
        }

        Double score = playerLoginTimeDao.score(playerId);
        //如果再次检测到玩家数据还没有过期,就不用保存
        if(score != null && score > expireTime){
            return false;
        }

        Player player = get(playerId);
        if(player != null){
            //如果在线就暂时不保存到mongodb
            boolean online = playerSessionService.hasSession(playerId);
            if(online){
                return false;
            }

            playerDao.save(player);
        }

        redisTemplate.opsForHash().delete(tableName,playerId);
        playerLastGameInfoDao.deleteById(playerId);
        playerLoginTimeDao.remove(playerId);
        return true;
    }
}
