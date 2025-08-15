package com.jjg.game.hall.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2025/6/18 16:18
 */
@Component
public class HallPoolDao extends AbstractPoolDao {
    @Override
    public void initPool() {

    }

    public Map<Object, Object> getSmallPoolByRoomCfgId(int gameType) {
        // 直接获取整个Hash（因为只有3个字段，HGETALL最有效率）
        return redisTemplate.opsForHash().entries(smallTableName(gameType));
    }

    public Map<Object, Object> getFakeSmallPoolByRoomCfgId(int gameType) {
        return redisTemplate.opsForHash().entries(fakeSmallTableName(gameType));
    }
}
