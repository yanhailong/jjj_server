package com.jjg.game.hall.pointsaward;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.PageUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.hall.minigame.game.luckytreasure.message.req.ReqLuckyTreasureHistory;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.leaderboard.PointsAwardLeaderboardService;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLadderRewardsInfo;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardSignInConfig;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableConfig;
import com.jjg.game.hall.pointsaward.pb.req.*;
import com.jjg.game.hall.pointsaward.pb.res.*;
import com.jjg.game.hall.pointsaward.signin.PointsAwardSignInService;
import com.jjg.game.hall.pointsaward.turntable.PointsAwardTurntableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    /**
     * 排行榜服务
     */
    private final PointsAwardLeaderboardService pointsAwardLeaderboardService;

    /**
     * 积分大奖服务
     */
    private final PointsAwardService pointsAwardService;


    private final RedDotManager redDotManager;

    public PointsAwardMessageHandler(PointsAwardSignInService pointsAwardSignInService,
                                     PointsAwardLeaderboardService pointsAwardLeaderboardService,
                                     PointsAwardService pointsAwardService,
                                     PointsAwardTurntableService pointsAwardTurntableService, RedDotManager redDotManager) {
        this.pointsAwardSignInService = pointsAwardSignInService;
        this.pointsAwardLeaderboardService = pointsAwardLeaderboardService;
        this.pointsAwardService = pointsAwardService;
        this.pointsAwardTurntableService = pointsAwardTurntableService;
        this.redDotManager = redDotManager;
    }

    /**
     * 获取签到配置
     */
    @Command(PointsAwardConstant.Message.REQ_SIGN_CONFIG)
    public void signInConfig(PlayerController playerController, ReqPointsAwardSignInConfig message) {
        ResPointsAwardSignInConfig res = new ResPointsAwardSignInConfig(Code.SUCCESS);
        try {
            List<PointsAwardSignInConfig> configList = pointsAwardSignInService.getConfigList(playerController.playerId());
            res.setConfigList(configList);
            log.debug("玩家获取积分大奖签到配置 playerId = {}", playerController.playerId());
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
        pointsAwardSignInService.signIn(playerController.playerId(), message.getDayOfMonth());
        List<PointsAwardSignInConfig> configList = pointsAwardSignInService.getConfigList(playerController.playerId());
        res.setConfigList(configList);
        res.setDayOfMonth(message.getDayOfMonth());
        playerController.send(res);
        log.debug("玩家请求积分大奖签到 playerId = {},dayOfMonth = {}", playerController.playerId(), message.getDayOfMonth());
    }

    /**
     * 转盘配置
     */
    @Command(PointsAwardConstant.Message.REQ_TURNTABLE_CONFIG)
    public void turntableConfig(PlayerController playerController, ReqPointsAwardTurntableConfig message) {
        ResPointsAwardTurntableConfig res = new ResPointsAwardTurntableConfig(Code.SUCCESS);
        try {
            List<PointsAwardTurntableConfig> configList = pointsAwardTurntableService.getConfigList();
            res.setConfigList(configList);
            res.setCount(pointsAwardTurntableService.getCount(playerController.playerId()));
            res.setMaxCount(pointsAwardTurntableService.getMaxCount(playerController.playerId()));
            log.debug("玩家请求转盘配置 playerId = {}", playerController.playerId());
        } catch (Exception e) {
            log.error("玩家请求转盘配置错误!playerId = [{}]", playerController.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 请求旋转转盘
     */
    @Command(PointsAwardConstant.Message.REQ_TURNTABLE)
    public void turntableSpin(PlayerController playerController, ReqPointsAwardTurntableSpin message) {
        CommonResult<ResPointsAwardTurntableSpin> result =  pointsAwardTurntableService.spin(playerController.playerId());
        if(!result.success()){
            playerController.send(new ResPointsAwardTurntableSpin(result.code));
            return;
        }

        playerController.send(result.data);
        log.debug("玩家请求积分大奖转盘 playerId = {},count = {},maxCount = {},gridId = {}", playerController.playerId(), result.data.getCount(), result.data.getMaxCount(), result.data.getGridId());
        //更新红点
        redDotManager.updateRedDotByInitialize(pointsAwardTurntableService.getModule(), pointsAwardTurntableService.getSubmodule(), playerController.playerId());
    }

    /**
     * 获取转盘历史记录
     */
    @Command(PointsAwardConstant.Message.REQ_TURNTABLE_HISTORY)
    public void turntableHistory(PlayerController playerController, ReqLuckyTreasureHistory message) {
        ResPointsAwardTurntableHistory res = new ResPointsAwardTurntableHistory(Code.SUCCESS);
        res.setHistoryList(pointsAwardTurntableService.getHistoryList(playerController.playerId()));
        playerController.send(res);
        log.debug("玩家积分大奖转盘历史 playerId = {}", playerController.playerId());
    }

    /**
     * 请求获取玩家积分大奖积分
     */
    @Command(PointsAwardConstant.Message.REQ_POINT)
    public void point(PlayerController playerController, ReqPlayerPoint message) {
        NotifySyncPlayerPoint res = new NotifySyncPlayerPoint();
        res.setPoint(pointsAwardService.getPoints(playerController.playerId()));
        res.setRank(pointsAwardLeaderboardService.getRank(PointsAwardConstant.Leaderboard.TYPE_MONTH, playerController.playerId()));
        res.setState(1);
        playerController.send(res);
        log.debug("返回玩家积分大奖积分 playerId = {},res = {}", playerController.playerId(), JSONObject.toJSONString(res));
    }

    /**
     * 加载排行榜数据
     */
    @Command(PointsAwardConstant.Message.REQ_LOAD_LEADERBOARD)
    public void loadLeaderboard(PlayerController playerController, ReqLoadLeaderboard message) {
        int type = message.getType();
        PageUtils.PageResult<PointsAwardLeaderboardData> pageResult = pointsAwardLeaderboardService.getData(type, message.getPageIndex(), message.getPageSize());
        //自己在排行榜上的名次 -1表示未上榜
        int rank = pointsAwardLeaderboardService.getRank(type, playerController.playerId());
        ResLoadLeaderboard res = new ResLoadLeaderboard(Code.SUCCESS);
        res.setType(type);
        res.setDataList(pageResult.getData());
        res.setTotalCount(pageResult.getTotalCount());
        res.setPageIndex(pageResult.getPageIndex());
        res.setPageSize(pageResult.getPageSize());
        res.setMaxPageIndex(pageResult.getMaxPageIndex());
        res.setSelfIndex(rank);
        playerController.send(res);
        log.debug("返回玩家积分大奖排行榜数据 playerId = {},type = {},pageIndex = {}", playerController.playerId(),message.getType(),message.getPageIndex());
    }

    /**
     * 加载排行榜历史数据
     */
    @Command(PointsAwardConstant.Message.REQ_LOAD_LEADERBOARD_HISTORY)
    public void loadLeaderboardHistory(PlayerController playerController, ReqLoadLeaderboardHistory message) {
        int pageIndex = message.getPageIndex();
        int pageSize = message.getPageSize();
        ResLoadLeaderboardHistory history = pointsAwardLeaderboardService.getHistory(playerController.playerId(), pageIndex, pageSize);
        playerController.send(history);
        log.debug("返回玩家积分大奖排行榜历史数据 playerId = {}", playerController.playerId());
    }

    /**
     * 请求转盘充值信息
     */
    @Command(PointsAwardConstant.Message.REQ_TURNTABLE_RECHARGE_INFO)
    public void turntableRechargeInfo(PlayerController playerController, ReqTurntableRechargeInfo msg) {
        BigDecimal recharge = pointsAwardService.getRecharge(playerController.playerId());
        int addCount = pointsAwardTurntableService.getAddCount(playerController.playerId());
        int checkValue = pointsAwardTurntableService.getRechargeCheckValue();
        ResTurntableRechargeInfo res = new ResTurntableRechargeInfo(Code.SUCCESS);
        res.setAddCount(addCount);
        res.setRechargeValue(recharge.toPlainString());
        res.setConfigValue(checkValue);
        playerController.send(res);
        log.debug("返回玩家积分大奖转盘充值信息 playerId = {}", playerController.playerId());
    }

    /**
     * 处理玩家积分大奖阶梯奖励信息的请求。
     */
    @Command(PointsAwardConstant.Message.REQ_POINTS_AWARD_LADDER_REWARD)
    public void ladderReceiveInfo(PlayerController playerController, ReqPointsAwardLadderRewards msg) {
        ResPointsAwardLadderRewards res = new ResPointsAwardLadderRewards(Code.SUCCESS);
        long points = pointsAwardService.getTimePoints(playerController.playerId());
        List<PointsAwardLadderRewardsInfo> configInfoList = pointsAwardService.getPointsAwardLadderRewardsInfoList(playerController.playerId());
        res.setTotalPoints(points);
        res.setLadderRewardsList(configInfoList);
        playerController.send(res);
//        pointsAwardService.resetTimePoints();
        log.debug("返回玩家积分大奖阶梯奖励信息 playerId = {},points = {}", playerController.playerId(), points);
    }

    /**
     * 领取积分大奖阶梯奖励
     */
    @Command(PointsAwardConstant.Message.REQ_RECEIVE_POINTS_AWARD_LADDER_REWARD)
    public void receiveLadderAward(PlayerController playerController, ReqReceivePointsAwardLadderRewards msg) {
        long points = msg.getPoints();
        int code = pointsAwardService.receiveLader(points, playerController.playerId(), false);
        ResReceivePointsAwardLadderRewards res = new ResReceivePointsAwardLadderRewards(Code.SUCCESS);
        res.setPoints(points);
        res.code = code;
        playerController.send(res);
        log.debug("返回玩家领取积分大奖奖励 playerId = {},points = {},code = {}", playerController.playerId(), points, res.code);
        if (code == Code.SUCCESS) {
            redDotManager.updateRedDotByInitialize(pointsAwardService.getModule(), pointsAwardService.getSubmodule(), playerController.playerId());
        }
    }

}
