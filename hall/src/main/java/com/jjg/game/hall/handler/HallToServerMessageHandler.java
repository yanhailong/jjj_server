package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.LuckyTreasureUpdateBroadcast;
import com.jjg.game.core.pb.ReqActivityInfos;
import com.jjg.game.core.pb.ResActivityInfos;
import com.jjg.game.core.pb.gm.NotifyLoadNoticeConfig;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.hall.minigame.game.luckytreasure.service.LuckyTreasureService;
import com.jjg.game.hall.pointsaward.PointsAwardService;
import com.jjg.game.hall.service.HallService;
import com.jjg.game.hall.service.NoticeService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/8/12 9:25
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class HallToServerMessageHandler extends CoreToServerMessageHandler {

    private final HallService hallService;
    private final CoreLogger coreLogger;
    private final LuckyTreasureService luckyTreasureService;
    private final ActivityManager activityManager;
    private final NoticeService noticeService;

    public HallToServerMessageHandler(LuckyTreasureService luckyTreasureService,
                                      HallService hallService,
                                      CoreLogger coreLogger,
                                      ActivityManager activityManager, NoticeService noticeService) {
        this.luckyTreasureService = luckyTreasureService;
        this.hallService = hallService;
        this.coreLogger = coreLogger;
        this.activityManager = activityManager;
        this.noticeService = noticeService;
    }

    @Command(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS)
    public void reqRefreshGameStatus(ReqRefreshGameStatus req) {
        log.info("收到刷新游戏状态命令: {}", JSON.toJSONString(req));
        String result = BackendGMCmd.Result.SUCCESS;
        try {
            hallService.refreshGameStatuses();
        } catch (Exception e) {
            log.error("", e);
            result = BackendGMCmd.Result.FAIL;
        }
        coreLogger.gmOrder(BackendGMCmd.CHANGE_GAME_STATUS + ":" + req.cmdParam, null, result);
    }

    /**
     * 收到其他节点同步更新库存数据
     */
    @Command(MessageConst.ToServer.NOTIFY_LUCKY_TREASURE_UPDATE_STOCK)
    public void handleLuckyTreasureUpdate(LuckyTreasureUpdateBroadcast message) {
        luckyTreasureService.handleUpdateMessage(message.getIssueNumber());
    }

    /**
     * gm请求活动数据
     */
    @Command(MessageConst.ToServer.REQ_ACTIVITY_INFOS)
    public void reqActivityInfos(Connect<ClusterMessage> connect, ReqActivityInfos req) {
        Map<Long, ActivityData> activityData = activityManager.getActivityData();
        List<ActivityData> data = new ArrayList<>(activityData.values());
        ResActivityInfos infos = new ResActivityInfos();
        infos.activityJsonStr = JSON.toJSONString(data);
        infos.reqId = req.reqId;
        ClusterMessage message = new ClusterMessage(MessageUtil.getPFMessage(infos));
        try {
            connect.write(message);
        } catch (Exception e) {
            log.error("响应后台请求活动信息失败");
        }
    }

    /**
     * 通知加载公告列表
     */
    @Command(MessageConst.ToServer.NOTIFY_LOAD_NOTICE_LIST)
    public void notifyLoadNoticeConfig(NotifyLoadNoticeConfig notify) {
        noticeService.loadNotice(false);
    }

}
