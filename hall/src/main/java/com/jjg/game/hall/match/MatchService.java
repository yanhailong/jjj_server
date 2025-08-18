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
        return matchDataDao.getWaitJoinRoomId(gameType, roomConfigId);
    }

    /**
     * 添加到等待房间ID
     */
    public void addWaitingRoomId(int gameType, int roomConfigId, long roomId, long roomCreateTime) {
        matchDataDao.addWaitJoinRoomId(gameType, roomConfigId, roomId, roomCreateTime);
    }
}
