package com.jjg.game.social.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimTaskDao;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SocialSender socialSender;

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
            Player p = corePlayerService.get(targetId);
            if (p == null) {
                res.code = Code.NOT_FOUND;
                log.warn("获取玩家信息卡失败，未找到该玩家 selfId={},targetId={}", selfId, targetId);
                return res;
            }
            PlayerCardInfo card = new PlayerCardInfo();
            card.playerId = p.getId();
            card.nick = p.getNickName();
            card.headImg = p.getHeadImgId();
            card.headFrame = p.getHeadFrameId();
            card.level = p.getLevel();
            card.gold = p.getGold();
            card.diamond = p.getDiamond();

            long allianceId = allianceProvider.getAllianceId(targetId);
            card.allianceName = allianceId > 0 ? allianceProvider.getAllianceName(allianceId) : null;

            card.casinos = buildCasinoIcons(targetId);
            card.displayedMedalIds = simTaskDao.findDisplayedMedalIds(targetId);

            FriendData selfData = friendDao.getOrEmpty(selfId);
            card.relation = selfData.isFriend(targetId) ? 1 : 0;
            card.inBlacklist = selfData.isBlacklisted(targetId);
            card.online = socialSender.online(targetId);
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
        Set<Integer> unlocked = new HashSet<>(simCasinoDao.findCasinoIdsByPlayerId(targetId));
        List<CasinoIconInfo> casinos = new ArrayList<>();
        List<CasinoListCfg> all = GameDataManager.getCasinoListCfgList();
        if (all != null) {
            for (CasinoListCfg cfg : all) {
                CasinoIconInfo icon = new CasinoIconInfo();
                icon.casinoId = cfg.getId();
                icon.unlocked = unlocked.contains(cfg.getId());
                casinos.add(icon);
            }
        }
        return casinos;
    }
}
