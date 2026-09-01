package com.jjg.game.slots.service;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.TogetherPlayDao;
import com.jjg.game.slots.pb.NotifyPlayerRewards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 真人与展示机器人共用的跨节点中奖广播，不负责任何奖励结算。 */
@Service
public class TogetherPlayRewardNotifier {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TogetherPlayDao togetherPlayDao;
    private final PlayerSessionService playerSessionService;

    public TogetherPlayRewardNotifier(TogetherPlayDao togetherPlayDao, PlayerSessionService playerSessionService) {
        this.togetherPlayDao = togetherPlayDao;
        this.playerSessionService = playerSessionService;
    }

    public void notify(int gameType, NotifyPlayerRewards winner, Map<Long, Long> commissions) {
        int start = 0;
        while (true) {
            List<TogetherPlayDao.PlayerScore> players;
            List<Long> playerIds;
            List<PlayerSessionInfo> sessionInfos;
            try {
                players = togetherPlayDao.page(gameType, start, SlotsConst.Common.TOGETHER_PLAY_PAGE_SIZE);
                if (players.isEmpty()) {
                    return;
                }
                playerIds = players.stream().map(TogetherPlayDao.PlayerScore::playerId).toList();
                sessionInfos = playerSessionService.getInfos(playerIds);
            } catch (Exception e) {
                log.error("获取好友同玩中奖广播会话失败 winnerId={},gameType={},start={}",
                        winner.playerId, gameType, start, e);
                return;
            }
            for (int i = 0; i < playerIds.size(); i++) {
                PlayerSessionInfo sessionInfo = sessionInfos.get(i);
                if (sessionInfo == null) {
                    continue;
                }
                try {
                    PFSession session = playerSessionService.getSession(sessionInfo);
                    if (session == null) {
                        continue;
                    }
                    NotifyPlayerRewards notify = winner;
                    if (!commissions.isEmpty()) {
                        notify = new NotifyPlayerRewards();
                        notify.playerId = winner.playerId;
                        notify.headImg = winner.headImg;
                        notify.headFrame = winner.headFrame;
                        notify.rewards = winner.rewards;
                        notify.times = winner.times;
                        notify.commission = commissions.getOrDefault(playerIds.get(i), 0L);
                    }
                    session.send(notify);
                } catch (Exception e) {
                    log.warn("发送好友同玩中奖广播失败 playerId={},winnerId={},gameType={}",
                            playerIds.get(i), winner.playerId, gameType, e);
                }
            }
            if (players.size() < SlotsConst.Common.TOGETHER_PLAY_PAGE_SIZE) {
                return;
            }
            start += players.size();
        }
    }
}
