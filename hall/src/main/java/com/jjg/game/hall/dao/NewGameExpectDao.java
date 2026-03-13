package com.jjg.game.hall.dao;

import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
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
    private final String PLAYER_DATA_TABLE_NAME = "newGameExpect:player:";

    private String playerDataTableName(int gameType) {
        return PLAYER_DATA_TABLE_NAME + gameType;
    }

    /**
     * 点赞
     *
     * @param playerId
     * @param gameType
     */
    public boolean add(long playerId, int gameType) {
        long add = redisTemplate.opsForSet().add(playerDataTableName(gameType), playerId);
        if (add > 0) {
            redisTemplate.opsForHash().increment(TABLE_NAME, gameType, 1);
            return true;
        }
        return false;
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
        Set<String> keys = new HashSet<>();
        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(ScanOptions.scanOptions().match(PLAYER_DATA_TABLE_NAME + "*").count(1000).build());

        while (cursor.hasNext()) {
            keys.add(new String(cursor.next()));
        }

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
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
