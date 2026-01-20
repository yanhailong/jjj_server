package com.jjg.game.hall.friendroom.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class RoomSlotsPoolDao extends AbstractPoolDao {
    /**
     * 根据房间id批量获取池子
     *
     * @return
     */
    public Map<Long, Long> getBigPoolByRoomIds(List<Object> roomIds) {
        List<Object> objects = this.redisTemplate.opsForHash().multiGet(room_pool_prefix, roomIds);
        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < roomIds.size(); i++) {
            Object object = objects.get(i);
            long poolValue = object == null ? 0 : ((Number) object).longValue();
            result.put((Long) roomIds.get(i), poolValue);
        }
        return result;
    }
}
