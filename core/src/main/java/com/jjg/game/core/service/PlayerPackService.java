package com.jjg.game.core.service;

import com.jjg.game.core.dao.PlayerPackDao;
import com.jjg.game.core.data.PlayerPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 11
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerPackService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PlayerPackDao playerPackDao;

    /**
     * 持久化到mongodb
     * @param playerId
     */
    public void moveToMongo(long playerId) {
        PlayerPack playerPack = playerPackDao.redisGet(playerId);
        if (playerPack == null) {
            return;
        }
        playerPackDao.save(playerPack);
        playerPackDao.redisDel(playerPack.getPlayerId());
    }
}
