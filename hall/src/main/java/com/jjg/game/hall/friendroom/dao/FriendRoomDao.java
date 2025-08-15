package com.jjg.game.hall.friendroom.dao;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 房间邀请码，
 *
 * @author 2CL
 */
@Repository
public class FriendRoomDao {

    private static final Logger log = LoggerFactory.getLogger(FriendRoomDao.class);
    // 最大的code
    private static final int MAX_CODE = 9999_9999;
    // code掩码
    private static final int CODE_MASK = MAX_CODE - TimeHelper.ONE_DAY_OF_MILES;

    public FriendRoomDao() {

    }


    /**
     * 获取一个邀请码
     */
    public int genInvitationCode() {
        long curTime = System.currentTimeMillis();
        long currentDateZeroMileTime = TimeHelper.getCurrentDateZeroMileTime();
        // 如果想让邀请码最低从 1000_0000 开始，将随机值的最低位设置为 1000_0000
        int maskData = RandomUtils.randomMinMax(0, CODE_MASK);
        int invitationCode = (int) (curTime - currentDateZeroMileTime + maskData);
        log.info("生成邀请码：{}", invitationCode);
        return invitationCode;
    }
}
