package com.jjg.game.hall.sharepromote;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.handler.BindPhoneCondition;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.LoginType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.service.MailService;
import com.jjg.game.hall.dao.SharePromoteRewardDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 分享推广信息
 *
 * @author lhc
 */
@Service
public class SharePromoteRewardService implements GameEventListener {

    private static final Logger log = LoggerFactory.getLogger(SharePromoteRewardService.class);

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private SharePromoteRewardDao sharePromoteRewardDao;

    @Autowired
    private MailService mailService;

    @Autowired
    private ConditionManager conditionManager;

    /**
     * 节点管理
     */
    @Autowired
    private MarsCurator marsCurator;

    /**
     * 开始分享推广
     *
     * @return
     */
    public int startSharePromote(PlayerController playerController) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.SHARE_PROMOTE);

        String value = globalConfigCfg.getValue();
        if (value == null || value.isEmpty()) {
            return Code.SUCCESS;
        }
        String[] split = value.split("\\|");
        String[] split1 = split[1].split("_");
        long playId = playerController.getPlayer().getId();
        MatchResultData match = conditionManager.isAchievementAndGetResult(playerController.getPlayer(), "", split[0]);
        String ip = playerController.getPlayer().getIp();
        if (match.result() == MatchResult.MATCH) {
            boolean canReceive = true;
            if (split.length >= 4) {
                //判断是否领取条件
                MatchResultData receiveCheck = conditionManager.isAchievementAndGetResult(playerController.getPlayer(), "", split[3]);
                if (receiveCheck.result() != MatchResult.MATCH) {
                    canReceive = false;
                }
            }
            if (canReceive) {
                Account account = accountDao.queryAccountByPlayerId(playId);
                if (account.getThirdAccounts() != null) {
                    Map<LoginType, String> thirdAccounts = account.getThirdAccounts();
                    log.info("开始分享推广 playId = {}, ip = {}", playId, ip);
                    String registerIp = account.getRegisterIp();
                    String equipNum = thirdAccounts.get(LoginType.GUEST);
                    if (sharePromoteRewardDao.judge(playId, registerIp, ip, equipNum)) {
                        boolean b = sharePromoteRewardDao.addSharePromote(playId, ip, equipNum, account.getRegisterIp());
                        if (b) {
                            int itemId = Integer.parseInt(split1[0]);
                            int itemNum = Integer.parseInt(split1[1]);
                            //返回奖励
                        mailService.addCfgMail(playId, GameConstant.Mail.ID_SHARING_REWARD, List.of(new Item(itemId, itemNum)), AddType.ACTIVITY_SHARE_PROMOTE);
                        }
                        return Code.SUCCESS;
                    } else {
                        log.info("开始分享推广 次数受到上线，不发送邮件 playId = {}, ip = {}", playId, ip);
                        return Code.SUCCESS;
                    }
                }
            }
            return Code.SUCCESS;
        }
        log.info("开始分享推广 前置条件检测失败 playId = {}, ip = {}", playId, ip);
        return match.errorCode();
    }

    /**
     * 跨天
     */
    public void dailyReset() {
        if (marsCurator.isMaster()) {
            sharePromoteRewardDao.clear();
            //跨月检查
            log.info("分享推广信息 跨天数据清理完成");
        }
    }

    /**
     * 处理事件
     *
     * @param gameEvent 事件
     */
    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof ClockEvent clockEvent) {
            int hour = clockEvent.getHour();
            if (hour == 0) {
                dailyReset();
            }
        }
    }

    /**
     * 需要监听的事件类型, 根据实际需要监听的类型写入，通过配置表配置或者手动配置，需尽量避免写入无关事件类型
     *
     * @return 事件类型列表
     */
    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT, EGameEventType.SHAREPROMOTE);
    }
}
