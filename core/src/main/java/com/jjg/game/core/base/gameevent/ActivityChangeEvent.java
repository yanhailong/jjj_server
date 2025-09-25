package com.jjg.game.core.base.gameevent;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.pb.activity.NotifyActivityServerChange;

/**
 * 活动变化事件
 * @author lm
 * @date 2025/9/23 20:48
 */
public interface ActivityChangeEvent extends IGameSysFuncInterface {
    /**
     * 当活动数据被后台修改时触发
     */
    void onActivityDataChange(NotifyActivityServerChange change);


    enum ChangeType{
        //更新活动开关
        UPDATE_ACTIVITY_OPEN,
        //更新活动
        UPDATE_ACTIVITY,
        //更新活动详情
        UPDATE_ACTIVITY_DETAILS,
    }
}
