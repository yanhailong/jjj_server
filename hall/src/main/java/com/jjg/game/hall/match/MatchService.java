package com.jjg.game.hall.match;

import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.match.MatchDataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 预留逻辑
 * 房间匹配服务，通过等待房间ID是否存在，判断房间是否可以加入
 *
 * @author 2CL
 */
@Service
public class MatchService {

    @Autowired
    private MatchDataDao matchDataDao;

    /**
     * 获取一个处于等待中的房间
     */
    public long getWaitingRoomId(int gameType, int roomConfigId) {
        for (int i = 0; i < GameConstant.Common.REDIS_LOCK_TRY_COUNT; i++) {
            long roomId = matchDataDao.getWaitJoinRoomId(gameType, roomConfigId);
            if (roomId > 0) {
                return roomId;
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return 0;
    }

    /**
     * 添加到等待房间ID
     */
    public void addWaitingRoomId(int gameType, int roomConfigId, long roomId, long roomCreateTime) {
        for (int i = 0; i < GameConstant.Common.REDIS_LOCK_TRY_COUNT; i++) {
            boolean addRes = matchDataDao.addWaitJoinRoomId(gameType, roomConfigId, roomId, roomCreateTime);
            if (addRes) {
                return;
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }
}
