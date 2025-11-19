package com.jjg.game.activity.common.message.handler;

import com.jjg.game.activity.cashcow.controller.CashCowController;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowFreeRewards;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowTotalPool;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.req.ReqActivityClaimRewards;
import com.jjg.game.activity.common.message.req.ReqActivityDetailInfo;
import com.jjg.game.activity.common.message.req.ReqActivityInfoByType;
import com.jjg.game.activity.common.message.req.ReqActivityPlayerJoin;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.levelpack.manager.PlayerLevelPackManager;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.activity.officialawards.controller.OfficialAwardsController;
import com.jjg.game.activity.officialawards.message.req.ReqOfficialAwardsRecord;
import com.jjg.game.activity.officialawards.message.req.ReqOfficialAwardsTotalPool;
import com.jjg.game.activity.sharepromote.controller.SharePromoteController;
import com.jjg.game.activity.sharepromote.message.req.ReqSharePromoteBindPlayer;
import com.jjg.game.activity.sharepromote.message.req.ReqSharePromoteSelfRankInfo;
import com.jjg.game.activity.sharepromote.message.req.ReqSharePromoteWeekRankInfo;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/9/3 11:11
 */
@Component
@MessageType(value = MessageConst.MessageTypeDef.ACTIVITY)
public class ActivityMessageHandler {
    private final ActivityManager activityManager;
    private final CashCowController cashCowController;
    private final SharePromoteController sharePromoteController;
    private final PlayerLevelPackManager playerLevelPackManager;
    private final NodeConfig nodeConfig;
    private final OfficialAwardsController officialAwardsController;

    public ActivityMessageHandler(ActivityManager activityManager, CashCowController cashCowController, SharePromoteController sharePromoteController, PlayerLevelPackManager playerLevelPackManager, NodeConfig nodeConfig, OfficialAwardsController officialAwardsController) {
        this.activityManager = activityManager;
        this.cashCowController = cashCowController;
        this.sharePromoteController = sharePromoteController;
        this.playerLevelPackManager = playerLevelPackManager;
        this.nodeConfig = nodeConfig;
        this.officialAwardsController = officialAwardsController;
    }

    /**
     * 根据活动id,详情id获取详细数据
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_DETAIL_INFO)
    public void reqActivityDetailInfo(PlayerController playerController, ReqActivityDetailInfo req) {
        //查找活动数据
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getValue().contains(req.detailId)) {
            BaseActivityController controller = data.getType().getController();
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                AbstractResponse response = controller.getPlayerActivityDetail(playerController.getPlayer(), data, req.detailId);
                if (response != null) {
                    playerController.send(response);
                }
            }
        }
    }

    /**
     * 根据活动类型获取详细数据
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_INFO_BY_TYPE)
    public void reqActivityInfoByType(PlayerController playerController, ReqActivityInfoByType req) {
        ActivityType activityType = ActivityType.fromType(req.activityType);
        if (activityType == null) {
            return;
        }
        AbstractResponse response = activityType.getController().getPlayerActivityInfoByType(playerController.getPlayer(), activityType);
        if (response != null) {
            playerController.send(response);
        }
    }

    /**
     * 请求领取奖励
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_CLAIM_REWARDS)
    public void reqActivityClaimRewards(PlayerController playerController, ReqActivityClaimRewards req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data == null || !data.getValue().contains(req.detailId)) {
            return;
        }
        BaseActivityController controller = data.getType().getController();
        if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
            AbstractResponse response = controller.claimActivityRewards(playerController.getPlayer(), data, req.detailId);
            if (response != null) {
                playerController.send(response);
                if (response.code == Code.SUCCESS) {
                    controller.updateRodDot(playerController.playerId(), data, true);
                }
            }
        }
    }

    /**
     * 玩家请求参与活动
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_PLAYER_JOIN)
    public void reqActivityPlayerJoin(PlayerController playerController, ReqActivityPlayerJoin req) {
        //查找活动数据
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getValue().contains(req.detailId) && data.getType().isCanInitiativeJoin()) {
            BaseActivityController controller = data.getType().getController();
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                AbstractResponse response = controller.joinActivity(playerController.getPlayer(), data, req.detailId, req.joinTimes);
                if (response != null) {
                    playerController.send(response);
                    if (response.code == Code.SUCCESS) {
                        controller.updateRodDot(playerController.playerId(), data, false, true);
                    }
                }
            }
        }
    }


    /**
     * 摇钱树请求记录
     */
    @Command(ActivityConstant.MsgBean.REQ_CASH_COW_RECORD)
    public void reqCashCowRecord(PlayerController playerController, ReqCashCowRecord req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getType() == ActivityType.CASH_COW) {
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                AbstractResponse res = cashCowController.reqCashCowRecord(playerController, req);
                playerController.send(res);
            }
        }
    }

    /**
     * 摇钱树总奖池
     */
    @Command(ActivityConstant.MsgBean.REQ_CASH_COW_TOTAL_POOL)
    public void reqCashCowTotalPool(PlayerController playerController, ReqCashCowTotalPool req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getType() == ActivityType.CASH_COW) {
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                AbstractResponse res = cashCowController.reqCashCowTotalPool(req);
                playerController.send(res);
            }
        }
    }

    /**
     * 摇钱树请求领取免费奖励
     */
    @Command(ActivityConstant.MsgBean.REQ_CASH_COW_FREE_REWARDS)
    public void reqCashCowFreeRewards(PlayerController playerController, ReqCashCowFreeRewards req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getType() == ActivityType.CASH_COW) {
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                AbstractResponse res = cashCowController.reqCashCowFreeRewards(playerController, data, req);
                playerController.send(res);
                if (res.code == Code.SUCCESS) {
                    cashCowController.updateRodDot(playerController.playerId(), data, true);
                }
            }
        }
    }

    /**
     * 推广分享-请求绑定玩家
     */
    @Command(ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_BIND_PLAYER)
    public void reqSharePromoteBindPlayer(PlayerController playerController, ReqSharePromoteBindPlayer req) {
        ActivityData activityData = activityManager.getOpenActivityData(playerController.getPlayer(), ActivityType.SHARE_PROMOTE);
        if (activityData == null) {
            return;
        }
        AbstractResponse res = sharePromoteController.reqSharePromoteBindPlayer(playerController, activityData, req);
        if (res != null) {
            playerController.send(res);
        }
    }


    /**
     * 推广分享-请求领取收益奖励
     */
    @Command(ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_CLAIM_PROFIT_REWARD)
    public void reqSharePromoteClaimProfitReward(PlayerController playerController) {
        ActivityData activityData = activityManager.getOpenActivityData(playerController.getPlayer(), ActivityType.SHARE_PROMOTE);
        if (activityData == null) {
            return;
        }
        AbstractResponse res = sharePromoteController.reqSharePromoteClaimProfitReward(playerController, activityData);
        playerController.send(res);
        if (res.code == Code.SUCCESS) {
            sharePromoteController.updateRodDot(playerController.playerId(), activityData, true);
        }
    }

    /**
     * 推广分享-请求推广分享总览信息
     */
    @Command(ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_GLOBAL_INFO)
    public void reqSharePromoteGlobalInfo(PlayerController playerController) {
        ActivityData activityData = activityManager.getOpenActivityData(playerController.getPlayer(), ActivityType.SHARE_PROMOTE);
        if (activityData == null) {
            return;
        }
        AbstractResponse res = sharePromoteController.reqSharePromoteGlobalInfo(playerController, activityData);
        playerController.send(res);
    }

    /**
     * 推广分享-请求推广分享周榜信息
     */
    @Command(ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_WEEK_RANK_INFO)
    public void reqSharePromoteWeekRankInfo(PlayerController playerController, ReqSharePromoteWeekRankInfo req) {
        ActivityData activityData = activityManager.getOpenActivityData(playerController.getPlayer(), ActivityType.SHARE_PROMOTE);
        if (activityData == null) {
            return;
        }
        AbstractResponse res = sharePromoteController.reqSharePromoteWeekRankInfo(activityData, req);
        playerController.send(res);
    }

    /**
     * 推广分享-请求推广分享我的收益排行榜信息
     */
    @Command(ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_SELF_RANK_INFO)
    public void reqSharePromoteSelfRankInfo(PlayerController playerController, ReqSharePromoteSelfRankInfo req) {
        ActivityData activityData = activityManager.getOpenActivityData(playerController.getPlayer(), ActivityType.SHARE_PROMOTE);
        if (activityData == null) {
            return;
        }
        AbstractResponse res = sharePromoteController.reqSharePromoteSelfRankInfo(playerController, req);
        playerController.send(res);
    }


    /**
     * 当前服务器能否领奖
     *
     * @return true 可以 false不行
     */
    private boolean serverCanClaimRewardsAndJoin() {
        if (NodeType.getNodeTypeByName(nodeConfig.getType()) == NodeType.GAME) {
            if (nodeConfig.getGameMajorTypes() != null) {
                for (int gameMajorType : nodeConfig.getGameMajorTypes()) {
                    if (gameMajorType == CoreConst.GameMajorType.POKER || gameMajorType == CoreConst.GameMajorType.TABLE) {
                        return true;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 等级礼包 请求领取等级礼包
     *
     * @param playerController 玩家信息
     */
    @Command(ActivityConstant.MsgBean.REQ_PLAYER_LEVEL_PACK_DETAIL_INFO)
    public void reqPlayerLevelPackDetailInfo(PlayerController playerController) {
        AbstractResponse res = playerLevelPackManager.reqPlayerLevelPackDetailInfo(playerController);
        playerController.send(res);
        if (res.code == Code.SUCCESS) {
            playerLevelPackManager.updateRedDot(playerController.playerId(), false);
        }
    }


    /**
     * 官方派奖 请求记录
     *
     * @param playerController 玩家信息
     */
    @Command(ActivityConstant.MsgBean.REQ_OFFICIAL_AWARDS_RECORD)
    public void reqOfficialAwardsRecord(PlayerController playerController, ReqOfficialAwardsRecord req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getType() == ActivityType.OFFICIAL_AWARDS) {
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                playerController.send(officialAwardsController.reqOfficialAwardsRecord(playerController, req));
            }
        }
    }

    /**
     * 官方派奖 总奖池
     *
     * @param playerController 玩家信息
     */
    @Command(ActivityConstant.MsgBean.REQ_OFFICIAL_AWARDS_TOTAL_POOL)
    public void reqOfficialAwardsTotalPool(PlayerController playerController, ReqOfficialAwardsTotalPool req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getType() == ActivityType.OFFICIAL_AWARDS) {
            if (activityManager.playerCanJoinActivity(data, playerController.getPlayer())) {
                playerController.send(officialAwardsController.reqOfficialAwardsTotalPool(req));
            }
        }
    }


}
