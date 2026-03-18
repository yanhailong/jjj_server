package com.jjg.game.hall.dao;

import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.hall.data.LikeGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 新游期待榜dao
 *
 * @author 11
 * @date 2026/3/13
 */
@Component
public class NewGameExpectDao implements GameEventListener {
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    //游戏的点赞数
    private final String TABLE_NAME = "newGameExpect:data";
    //玩家点赞相关数据
    private final String PLAYER_DATA_TABLE_NAME = "newGameExpect:player";

    /**
     * 点赞
     *
     * @param playerId
     * @param gameType
     */
    public boolean add(long playerId, int gameType) {
        LikeGame likeGame = getLikeNewGame(playerId);
        if (likeGame == null) {
            likeGame = new LikeGame();
            likeGame.setPlayerId(playerId);
        }

        boolean add = likeGame.addLikeGame(gameType);
        if (!add) {
            return false;
        }
        redisTemplate.opsForHash().put(PLAYER_DATA_TABLE_NAME, playerId, likeGame);
        redisTemplate.opsForHash().increment(TABLE_NAME, gameType, 1);
        return true;
    }

    public LikeGame getLikeNewGame(long playerId) {
        HashOperations<String, String, LikeGame> operations = redisTemplate.opsForHash();
        return operations.get(PLAYER_DATA_TABLE_NAME, playerId);
    }

    /**
     * 查询所有的点赞数据
     *
     * @return
     */
    public Map<Object, Object> queryAll() {
        return redisTemplate.opsForHash().entries(TABLE_NAME);
    }

    /**
     * 清除玩家点赞数据
     */
    public void clearPlayerData() {
        redisTemplate.delete(PLAYER_DATA_TABLE_NAME);
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof ClockEvent event) {
            if (event.getHour() == 0) {
                clearPlayerData();
            }
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT);
    }
}
