package com.jjg.game.social.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.dao.SimTaskDao;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoUnlock;
import com.jjg.game.sim.service.SimCasinoService;
import com.jjg.game.social.dao.FriendDao;
import com.jjg.game.social.data.FriendData;
import com.jjg.game.social.pb.res.ResPlayerCard;
import com.jjg.game.social.pb.struct.CasinoIconInfo;
import com.jjg.game.social.pb.struct.PlayerCardInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家信息卡聚合: 基础信息 + 联盟名 + 场景图标(全部, 标注解锁) + 与本人的关系。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class PlayerCardInfoService {
    private static final Logger log = LoggerFactory.getLogger(PlayerCardInfoService.class);

    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private FriendDao friendDao;
    @Autowired
    private AllianceMemberProvider allianceProvider;
    @Autowired
    private SimCasinoService simCasinoService;
    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SocialSender socialSender;
    @Autowired
    private SimRobotService simRobotService;

    /**
     * 获取玩家信息卡
     *
     * @param selfId
     * @param targetId
     * @return
     */
    public ResPlayerCard card(long selfId, long targetId) {
        ResPlayerCard res = new ResPlayerCard(Code.SUCCESS);
        try {
            boolean robot = RobotUtil.isRobot(targetId);
            Player p;

            PlayerCardInfo card;
            if (robot) {
                RobotCfg robotCfg = simRobotService.queryRobotCfg(targetId);
                if (robotCfg == null) {
                    log.warn("获取机器人配置失败 selfId={},targetId={}", selfId, targetId);
                    res.code = Code.SAMPLE_ERROR;
                    return res;
                }

                long bindPlayerId = simRobotService.queryPlayerId(robotCfg.getId());
                if (bindPlayerId < 1) {
                    bindPlayerId = simPlayerGameDao.findRandomVisitCandidates(2).stream()
                            .mapToLong(candidate -> candidate.getPlayerId())
                            .filter(playerId -> playerId != selfId)
                            .findFirst()
                            .orElse(0);
                    if (bindPlayerId > 0) {
                        simRobotService.bind(targetId, bindPlayerId);
                    }
                }

                p = corePlayerService.get(bindPlayerId);
                if (p == null) {
                    res.code = Code.NOT_FOUND;
                    log.warn("获取玩家信息卡失败，未找到机器人对应的player信息 selfId={},targetId={},bindPlayerId={}", selfId, targetId, bindPlayerId);
                    return res;
                }

                card = new PlayerCardInfo();
                card.playerId = targetId;
                card.nick = robotCfg.getNameId();
                card.headImg = robotCfg.getPicture();
                card.headFrame = robotCfg.getFrame();


                targetId = bindPlayerId;
                card.online = true;
            } else {
                p = corePlayerService.get(targetId);

                if (p == null) {
                    res.code = Code.NOT_FOUND;
                    log.warn("获取玩家信息卡失败，未找到该玩家 selfId={},targetId={}", selfId, targetId);
                    return res;
                }

                card = new PlayerCardInfo();
                card.playerId = p.getId();
                card.nick = p.getNickName();
                card.headImg = p.getHeadImgId();
                card.headFrame = p.getHeadFrameId();

                card.online = socialSender.online(targetId);
            }

            SimBaseData baseData = simPlayerGameDao.findById(targetId).orElse(null);
            if(baseData != null){
                card.level = baseData.getAllLevel();
            }else {
                card.level = p.getLevel();
            }

            card.gold = p.getGold();
            card.diamond = p.getDiamond();

            long allianceId = allianceProvider.getAllianceId(targetId);
            card.allianceName = allianceId > 0 ? allianceProvider.getAllianceName(allianceId) : null;

            card.casinos = buildCasinoIcons(targetId);
            card.displayedMedalIds = simTaskDao.findDisplayedMedalIds(targetId);

            FriendData selfData = friendDao.getOrEmpty(selfId);
            card.relation = selfData.isFriend(targetId) ? 1 : 0;
            card.inBlacklist = selfData.isBlacklisted(targetId);

            res.card = card;
        } catch (Exception e) {
            log.error("", e);
        }

        return res;
    }

    /**
     * 全部场景图标, 标注目标玩家是否已解锁。
     */
    private List<CasinoIconInfo> buildCasinoIcons(long targetId) {
        SimCasinoUnlock casinoUnlock = simCasinoService.getCasinoUnlock(targetId);
        if(casinoUnlock == null){
            return Collections.emptyList();
        }

        List<CasinoIconInfo> casinos = new ArrayList<>();
        for (CasinoListCfg cfg : GameDataManager.getCasinoListCfgList()) {
            CasinoIconInfo icon = new CasinoIconInfo();
            icon.casinoId = cfg.getId();
            icon.unlocked = casinoUnlock.hasUnlockCasino(cfg.getId());
            casinos.add(icon);
        }
        return casinos;
    }
}
