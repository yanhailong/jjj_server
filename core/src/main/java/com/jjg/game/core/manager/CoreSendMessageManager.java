package com.jjg.game.core.manager;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.MoneyChangeInfo;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.pb.NotifyMoneyChange;
import com.jjg.game.core.service.AbstractPlayerService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.utils.MessageBuildUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/11 17:55
 */
@Component
public class CoreSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private PlayerSessionService playerSessionService;

    /**
     * 推送基本信息变化
     *
     * @param playerController 玩家控制器
     */
    public void buildBaseInfoChangeMessage(PlayerController playerController, Player player) {
        buildBaseInfoChangeMessage(playerController.getSession(), player);
    }

    /**
     * 推送基本信息变化
     */
    public void buildBaseInfoChangeMessage(Player player) {
        if (player == null) {
            return;
        }
        PFSession session = playerSessionService.getSession(player.getId());
        if (session == null) {
            return;
        }
        buildBaseInfoChangeMessage(session, player);
    }

    /**
     * 推送基本信息变化
     */
    public void buildBaseInfoChangeMessage(long playerId, AbstractPlayerService playerService) {
        PFSession session = playerSessionService.getSession(playerId);
        if (session == null) {
            return;
        }
        Player player = playerService.get(playerId);
        buildBaseInfoChangeMessage(session, player);
    }

    /**
     * 推送基本信息变化
     *
     * @param session 玩家连接session
     * @param player  玩家信息
     */
    public void buildBaseInfoChangeMessage(PFSession session, Player player) {
        SendInfo sendInfo = new SendInfo();

        NoticeBaseInfoChange notice = MessageBuildUtil.buildNoticeBaseInfoChange(player);
        sendInfo.addPlayerMsg(session.playerId, notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(session, sendInfo, "推送玩家基础信息", false);
    }

    /*********************************************************************************************/

    /**
     * 推送玩家货币变化信息
     *
     * @param player
     */
    public void buildMoneyChangeMessage(Player player, long goldChangeValue, long diamondChangeValue) {
        PFSession session = playerSessionService.getSession(player.getId());
        if (session == null) {
            return;
        }
        List<MoneyChangeInfo> moneyChangeInfoList = new ArrayList<>();
        if (goldChangeValue != 0) {
            moneyChangeInfoList.add(buildMoneyChangeInfo(GameConstant.Item.TYPE_GOLD, goldChangeValue, player.getGold()));
        }
        if (diamondChangeValue != 0) {
            moneyChangeInfoList.add(buildMoneyChangeInfo(GameConstant.Item.TYPE_DIAMOND, diamondChangeValue, player.getDiamond()));
        }
        buildMoneyChangeInfoMessage(session, moneyChangeInfoList);
    }

    /**
     * 推送玩家货币变化信息
     *
     * @param session
     */
    public void buildGoldChangeMessage(PFSession session, long changeValue, long afterValue) {
        buildMoneyChangeMessage(session, GameConstant.Item.TYPE_GOLD, changeValue, afterValue);
    }

    /**
     * 推送玩家货币变化信息
     *
     */
    public void buildGoldChangeMessage(Player player, long changeValue) {
        PFSession session = playerSessionService.getSession(player.getId());
        if (session == null) {
            return;
        }
        buildMoneyChangeMessage(session, GameConstant.Item.TYPE_GOLD, changeValue, player.getGold());
    }

    /**
     * 推送玩家货币变化信息
     *
     */
    public void buildDiamondChangeMessage(Player player, long changeValue) {
        PFSession session = playerSessionService.getSession(player.getId());
        if (session == null) {
            return;
        }
        buildMoneyChangeMessage(session, GameConstant.Item.TYPE_DIAMOND, changeValue, player.getDiamond());
    }

    /**
     * 推送玩家货币变化信息
     *
     * @param session
     */
    public void buildDiamondChangeMessage(PFSession session, long changeValue, long afterValue) {
        buildMoneyChangeMessage(session, GameConstant.Item.TYPE_DIAMOND, changeValue, afterValue);
    }

    /**
     * 推送玩家货币变化信息
     *
     * @param session
     */
    public void buildMoneyChangeMessage(PFSession session, int moneyType, long changeValue, long afterValue) {
        List<MoneyChangeInfo> list = new ArrayList<>(1);
        list.add(buildMoneyChangeInfo(moneyType, changeValue, afterValue));
        buildMoneyChangeInfoMessage(session, list);
    }

    /**
     * 推送玩家货币变化信息
     *
     * @param session
     * @param moneyChangeInfoList
     */
    public void buildMoneyChangeInfoMessage(PFSession session, List<MoneyChangeInfo> moneyChangeInfoList) {
        SendInfo sendInfo = new SendInfo();

        NotifyMoneyChange notify = new NotifyMoneyChange();
        notify.moneyChangeInfos = moneyChangeInfoList;

        sendInfo.addPlayerMsg(session.playerId, notify);
        sendInfo.getLogMessage().add(notify);
        sendRun(session, sendInfo, "推送玩家货币变化信息", false);
    }


    public MoneyChangeInfo buildMoneyChangeInfo(int moneyType, long changeValue, long afterValue) {
        MoneyChangeInfo moneyChangeInfo = new MoneyChangeInfo();
        moneyChangeInfo.moneyType = moneyType;
        moneyChangeInfo.changeValue = changeValue;
        moneyChangeInfo.afterValue = afterValue;
        return moneyChangeInfo;
    }
}
