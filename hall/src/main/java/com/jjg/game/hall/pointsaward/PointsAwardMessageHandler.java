package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardSignInConfig;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableConfig;
import com.jjg.game.hall.pointsaward.pb.req.ReqPointAwardTurntableConfig;
import com.jjg.game.hall.pointsaward.pb.req.ReqPointAwardTurntableSpin;
import com.jjg.game.hall.pointsaward.pb.req.ReqPointsAwardSignIn;
import com.jjg.game.hall.pointsaward.pb.req.ReqPointsAwardSignInConfig;
import com.jjg.game.hall.pointsaward.pb.res.ResPointAwardTurntableConfig;
import com.jjg.game.hall.pointsaward.pb.res.ResPointAwardTurntableSpin;
import com.jjg.game.hall.pointsaward.pb.res.ResPointsAwardSignIn;
import com.jjg.game.hall.pointsaward.pb.res.ResPointsAwardSignInConfig;
import com.jjg.game.hall.pointsaward.signin.PointsAwardSignInService;
import com.jjg.game.hall.pointsaward.turntable.PointsAwardTurntableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 积分大奖消息入口
 */
@MessageType(MessageConst.MessageTypeDef.POINTS_AWARD)
@Component
public class PointsAwardMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 签到服务
     */
    private final PointsAwardSignInService pointsAwardSignInService;

    /**
     * 转盘服务
     */
    private final PointsAwardTurntableService pointsAwardTurntableService;

    public PointsAwardMessageHandler(PointsAwardSignInService pointsAwardSignInService, PointsAwardTurntableService pointsAwardTurntableService) {
        this.pointsAwardSignInService = pointsAwardSignInService;
        this.pointsAwardTurntableService = pointsAwardTurntableService;
    }

    /**
     * 获取签到配置
     */
    @Command(PointsAwardConstant.Message.REQ_SIGN_CONFIG)
    public void signInConfig(PlayerController playerController, ReqPointsAwardSignInConfig message) {
        ResPointsAwardSignInConfig res = new ResPointsAwardSignInConfig(Code.SUCCESS);
        try {
            List<PointsAwardSignInConfig> configList = pointsAwardSignInService.getConfigList();
            int signCount = pointsAwardSignInService.getSignCount(playerController.playerId());
            res.setConfigList(configList);
            res.setCount(signCount);
        } catch (Exception e) {
            log.error("积分大奖获取签到配置失败!playerId = [{}]", playerController.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 请求签到
     */
    @Command(PointsAwardConstant.Message.REQ_SIGN)
    public void singIn(PlayerController playerController, ReqPointsAwardSignIn message) {
        ResPointsAwardSignIn res = new ResPointsAwardSignIn(Code.SUCCESS);
        boolean signInResult = pointsAwardSignInService.signIn(playerController.playerId());
        int signCount = pointsAwardSignInService.getSignCount(playerController.playerId());
        if (signInResult) {
            res.setCount(signCount);
        } else {
            res.code = Code.SAMPLE_ERROR;
        }
        playerController.send(res);
    }

    /**
     * 转盘配置
     */
    @Command(PointsAwardConstant.Message.REQ_TURNTABLE_CONFIG)
    public void turntableConfig(PlayerController playerController, ReqPointAwardTurntableConfig message) {
        ResPointAwardTurntableConfig res = new ResPointAwardTurntableConfig(Code.SUCCESS);
        try {
            List<PointsAwardTurntableConfig> configList = pointsAwardTurntableService.getConfigList();
            res.setConfigList(configList);
        } catch (Exception e) {
            log.error("积分大奖获取配置错误!playerId = [{}]", playerController.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 请求旋转转盘
     */
    @Command(PointsAwardConstant.Message.REQ_TURNTABLE)
    public void turntableSpin(PlayerController playerController, ReqPointAwardTurntableSpin message) {
        ResPointAwardTurntableSpin res = new ResPointAwardTurntableSpin(Code.SUCCESS);
        int gridId = pointsAwardTurntableService.spin(playerController.playerId());
        if (gridId > 0) {
            res.setGridId(gridId);
        } else {
            res.code = Code.SAMPLE_ERROR;
        }
        playerController.send(res);
    }


}
