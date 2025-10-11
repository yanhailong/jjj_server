package com.jjg.game.hall.pointsaward;

import com.jjg.game.hall.pointsaward.signin.PointsAwardSignInManager;
import com.jjg.game.hall.pointsaward.turntable.PointsAwardTurntableService;
import org.springframework.stereotype.Component;

/**
 * 积分大奖管理器
 */
@Component
public class PointsAwardManager {

    /**
     * 转盘服务
     */
    private final PointsAwardTurntableService pointsAwardTurntableService;

    /**
     * 签到管理器
     */
    private final PointsAwardSignInManager pointsAwardSignInManager;

    public PointsAwardManager(PointsAwardTurntableService pointsAwardTurntableService, PointsAwardSignInManager pointsAwardSignInManager) {
        this.pointsAwardTurntableService = pointsAwardTurntableService;
        this.pointsAwardSignInManager = pointsAwardSignInManager;
    }

    public void init() {
        pointsAwardSignInManager.init();
        pointsAwardTurntableService.init();
    }


}
