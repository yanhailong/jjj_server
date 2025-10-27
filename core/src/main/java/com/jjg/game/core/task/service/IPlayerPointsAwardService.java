package com.jjg.game.core.task.service;

import com.jjg.game.core.constant.PointsAwardType;
import org.springframework.stereotype.Service;

/**
 * 大厅提供给core调用的积分处理接口
 */
@Service
public interface IPlayerPointsAwardService {

    /**
     * 添加积分
     *
     * @param playerId    玩家id
     * @param pointsAward 增加的积分
     * @param type        {@link PointsAwardType}
     */
    void add(long playerId, int pointsAward, int type);

    /**
     * 扣除积分
     *
     * @param playerId    玩家id
     * @param pointsAward 扣除的积分 只支持正数
     */
    boolean deduct(long playerId, int pointsAward, int type);

}
