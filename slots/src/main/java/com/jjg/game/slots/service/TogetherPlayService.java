package com.jjg.game.slots.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.TogetherPlayReconnectDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.TogetherPlayDao;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.TogetherPlayData;
import com.jjg.game.slots.pb.ResTogetherPlayInvite;
import com.jjg.game.slots.pb.ResTogetherPlayPlayerList;
import com.jjg.game.slots.pb.TogetherPlayPlayerInfo;
import com.jjg.game.social.bridge.ToSocialBridge;
import com.jjg.game.social.service.SocialRelationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class TogetherPlayService {
    public static final int LIST_ALL = 0;
    public static final int LIST_INVITED = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 20;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TogetherPlayDao togetherPlayDao;
    private final TogetherPlayReconnectDao reconnectDao;
    private final CorePlayerService playerService;
    private final SlotsPlayerService slotsPlayerService;
    private final SocialRelationCache relationCache;
    @ClusterRpcReference
    private ToSocialBridge toSocialBridge;

    public TogetherPlayService(TogetherPlayDao togetherPlayDao, TogetherPlayReconnectDao reconnectDao,
                               CorePlayerService playerService, SlotsPlayerService slotsPlayerService,
                               SocialRelationCache relationCache) {
        this.togetherPlayDao = togetherPlayDao;
        this.reconnectDao = reconnectDao;
        this.playerService = playerService;
        this.slotsPlayerService = slotsPlayerService;
        this.relationCache = relationCache;
    }

    public void onEnter(SlotsPlayerGameData gameData) {
        if (!normal(gameData)) {
            return;
        }
        int reconnectGameType = 0;
        try {
            reconnectGameType = reconnectDao.getGameType(gameData.getPlayerId());
        } catch (Exception e) {
            log.error("读取好友同玩断线标记失败 playerId={}", gameData.getPlayerId(), e);
        }
        boolean reconnect = reconnectGameType == gameData.getGameType();
        TogetherPlayData oldData = gameData.getTogetherPlayData();
        TogetherPlayData togetherData = reconnect && oldData != null ? oldData : new TogetherPlayData();
        long winGold = reconnect && togetherData.getOfflineWinGold() != null
                ? togetherData.getOfflineWinGold() : 0L;
        togetherData.setCommissionUsers(Math.max(0,
                gameData.getTogetherPlaySkillEffect().getCommissionUsers()));
        togetherData.setWinCommission(Math.max(0,
                gameData.getTogetherPlaySkillEffect().getWinCommission()));
        gameData.setTogetherPlayData(togetherData);
        try {
            if (!reconnect) {
                togetherPlayDao.resetSession(gameData.getGameType(), gameData.getPlayerId());
            }
            togetherPlayDao.enter(gameData.getGameType(), gameData.getPlayerId(), winGold,
                    togetherData.getCommissionUsers(), togetherData.getWinCommission());
            togetherData.setOfflineWinGold(null);
        } catch (Exception e) {
            log.error("好友同玩玩家进入实时集合失败 playerId={},gameType={}",
                    gameData.getPlayerId(), gameData.getGameType(), e);
            return;
        }
        if (reconnectGameType != 0) {
            removeReconnectMark(gameData.getPlayerId());
        }
    }

    public void onExit(SlotsPlayerGameData gameData, boolean dropped) {
        if (!normal(gameData)) {
            return;
        }
        Long winGold = null;
        try {
            if (dropped) {
                winGold = togetherPlayDao.leave(gameData.getGameType(), gameData.getPlayerId());
            } else {
                togetherPlayDao.remove(gameData.getGameType(), gameData.getPlayerId());
                togetherPlayDao.resetSession(gameData.getGameType(), gameData.getPlayerId());
            }
        } catch (Exception e) {
            log.error("好友同玩玩家离开实时集合失败 playerId={},gameType={}",
                    gameData.getPlayerId(), gameData.getGameType(), e);
        }
        if (!dropped) {
            gameData.setTogetherPlayData(null);
            removeReconnectMark(gameData.getPlayerId());
            return;
        }
        TogetherPlayData togetherData = gameData.getTogetherPlayData();
        if (togetherData == null) {
            togetherData = new TogetherPlayData();
            gameData.setTogetherPlayData(togetherData);
        }
        togetherData.setOfflineWinGold(winGold);
    }

    public void markReconnect(SlotsPlayerGameData gameData) {
        if (!normal(gameData)) {
            return;
        }
        try {
            reconnectDao.mark(gameData.getPlayerId(), gameData.getGameType(),
                    SlotsConst.Common.MAX_OFFLINE_TIME);
        } catch (Exception e) {
            log.error("写入好友同玩断线标记失败 playerId={},gameType={}",
                    gameData.getPlayerId(), gameData.getGameType(), e);
        }
    }

    public void onShutdown(SlotsPlayerGameData gameData) {
        if (!normal(gameData)) {
            return;
        }
        try {
            togetherPlayDao.remove(gameData.getGameType(), gameData.getPlayerId());
        } catch (Exception e) {
            log.error("关闭Slots节点时移除好友同玩玩家失败 playerId={},gameType={}",
                    gameData.getPlayerId(), gameData.getGameType(), e);
        }
    }

    public void onSpin(SlotsPlayerGameData gameData, long change) {
        if (!normal(gameData) || change == 0) {
            return;
        }
        try {
            togetherPlayDao.addWinGold(gameData.getGameType(), gameData.getPlayerId(), change);
        } catch (Exception e) {
            log.error("更新好友同玩净输赢失败 playerId={},gameType={},change={}",
                    gameData.getPlayerId(), gameData.getGameType(), change, e);
            return;
        }
        if (change > 0) {
            awardCommissions(gameData.getGameType(), gameData.getPlayerId(), change);
        }
    }

    public ResTogetherPlayPlayerList playerList(SlotsPlayerGameData gameData, int listType,
                                                 int pageIndex, int requestedPageSize) {
        ResTogetherPlayPlayerList res = new ResTogetherPlayPlayerList(Code.SUCCESS);
        res.listType = listType;
        res.pageIndex = pageIndex;
        res.pageSize = normalizePageSize(requestedPageSize);
        if (!normal(gameData)) {
            res.code = Code.FORBID;
            return res;
        }
        if ((listType != LIST_ALL && listType != LIST_INVITED) || pageIndex < 0) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        long startValue = (long) pageIndex * res.pageSize;
        if (startValue > Integer.MAX_VALUE - res.pageSize - 1L) {
            res.playerInfos = List.of();
            return res;
        }
        List<TogetherPlayDao.PlayerScore> scores = listType == LIST_ALL
                ? allPlayerScores(gameData.getGameType(), (int) startValue, res.pageSize, res)
                : invitedPlayerScores(gameData, (int) startValue, res.pageSize, res);
        Set<Long> commissionEnabled = Set.of();
        Map<Long, Long> commissions = Map.of();
        if (listType == LIST_INVITED) {
            TogetherPlayData togetherData = gameData.getTogetherPlayData();
            res.commissionUsers = togetherData == null ? 0 : togetherData.getCommissionUsers();
            if (togetherData != null && togetherData.getWinCommission() > 0) {
                commissionEnabled = activeInvitees(
                        gameData.getGameType(), gameData.getPlayerId(), res.commissionUsers);
            }
            res.currentCommissionUsers = commissionEnabled.size();
            commissions = togetherPlayDao.commissions(gameData.getGameType(), gameData.getPlayerId(),
                    scores.stream().map(TogetherPlayDao.PlayerScore::playerId).toList());
        }
        res.playerInfos = toPlayerInfos(scores, commissionEnabled, commissions);
        return res;
    }

    public CompletableFuture<ResTogetherPlayInvite> invite(PlayerController playerController,
                                                            SlotsPlayerGameData gameData,
                                                            long targetPlayerId) {
        ResTogetherPlayInvite res = new ResTogetherPlayInvite(Code.SUCCESS);
        res.targetPlayerId = targetPlayerId;
        long playerId = playerController.playerId();
        if (!normal(gameData)) {
            res.code = Code.FORBID;
            return CompletableFuture.completedFuture(res);
        }
        if (targetPlayerId <= 0 || targetPlayerId == playerId) {
            res.code = Code.PARAM_ERROR;
            return CompletableFuture.completedFuture(res);
        }
        if (!relationCache.getFriendIds(playerId).contains(targetPlayerId)) {
            res.code = Code.FRIEND_NOT_FOLLOWED;
            return CompletableFuture.completedFuture(res);
        }
        ClusterClient hallClient = gameData.getSimClient();
        if (hallClient == null) {
            res.code = Code.EXCEPTION;
            return CompletableFuture.completedFuture(res);
        }

        Player inviter = playerController.getPlayer();
        String inviterNick = inviter.getNickName();
        int inviterHeadImg = inviter.getHeadImgId();
        int inviterHeadFrame = inviter.getHeadFrameId();
        int gameType = gameData.getGameType();
        int wareId = gameData.getRoomCfgId();
        CompletableFuture<Integer> rpcFuture;
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(hallClient).setTryMillisPerClient(1000));
            rpcFuture = rpcContext.asyncCall(() -> toSocialBridge.sendTogetherPlayInvite(
                    playerId, targetPlayerId, inviterNick, inviterHeadImg,
                    inviterHeadFrame, gameType, wareId));
        } catch (Exception e) {
            log.error("异步发送好友同玩邀请失败 playerId={},targetPlayerId={}",
                    playerId, targetPlayerId, e);
            res.code = Code.EXCEPTION;
            return CompletableFuture.completedFuture(res);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }

        CompletableFuture<ResTogetherPlayInvite> responseFuture = new CompletableFuture<>();
        rpcFuture.whenComplete((code, throwable) ->
                PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                        playerId, 0, new BaseHandler<String>() {
                            @Override
                            public void action() {
                                if (throwable != null) {
                                    log.warn("好友同玩邀请Hall投递异常 playerId={},targetPlayerId={}",
                                            playerId, targetPlayerId, throwable);
                                    res.code = Code.EXCEPTION;
                                } else {
                                    res.code = code == null ? Code.EXCEPTION : code;
                                }
                                TogetherPlayData togetherData = gameData.getTogetherPlayData();
                                if (res.code == Code.SUCCESS && gameData.isOnline()
                                        && normal(gameData) && togetherData != null) {
                                    Set<Long> invitePlayerIds = togetherData.getInvitePlayerIds();
                                    if (!invitePlayerIds.contains(targetPlayerId)) {
                                        try {
                                            togetherPlayDao.recordInvite(gameType, playerId, targetPlayerId,
                                                    invitePlayerIds.size() + 1L);
                                            invitePlayerIds.add(targetPlayerId);
                                        } catch (Exception e) {
                                            log.error("记录好友同玩邀请关系失败 playerId={},targetPlayerId={},gameType={}",
                                                    playerId, targetPlayerId, gameType, e);
                                            res.code = Code.EXCEPTION;
                                        }
                                    }
                                }
                                responseFuture.complete(res);
                            }
                        }.setHandlerParamWithSelf("together play invite callback")));
        return responseFuture;
    }

    private List<TogetherPlayDao.PlayerScore> allPlayerScores(int gameType, int start, int pageSize,
                                                               ResTogetherPlayPlayerList res) {
        List<TogetherPlayDao.PlayerScore> scores = togetherPlayDao.page(gameType, start, pageSize + 1);
        if (scores.size() > pageSize) {
            res.nextPageIndex = res.pageIndex + 1;
            return new ArrayList<>(scores.subList(0, pageSize));
        }
        return scores;
    }

    private List<TogetherPlayDao.PlayerScore> invitedPlayerScores(SlotsPlayerGameData gameData, int start,
                                                                   int pageSize, ResTogetherPlayPlayerList res) {
        TogetherPlayData togetherData = gameData.getTogetherPlayData();
        if (togetherData == null || togetherData.getInvitePlayerIds().isEmpty()) {
            return List.of();
        }
        List<TogetherPlayDao.PlayerScore> page = new ArrayList<>(pageSize);
        Iterator<Long> iterator = togetherData.getInvitePlayerIds().iterator();
        int activeIndex = 0;
        while (iterator.hasNext()) {
            List<Long> batchIds = new ArrayList<>(MAX_PAGE_SIZE);
            while (iterator.hasNext() && batchIds.size() < MAX_PAGE_SIZE) {
                batchIds.add(iterator.next());
            }
            List<Double> redisScores = togetherPlayDao.scores(gameData.getGameType(), batchIds);
            for (int i = 0; i < batchIds.size(); i++) {
                Double score = redisScores.get(i);
                if (score == null || activeIndex++ < start) {
                    continue;
                }
                if (page.size() == pageSize) {
                    res.nextPageIndex = res.pageIndex + 1;
                    return page;
                }
                page.add(new TogetherPlayDao.PlayerScore(batchIds.get(i), score.longValue()));
            }
        }
        return page;
    }

    private List<TogetherPlayPlayerInfo> toPlayerInfos(List<TogetherPlayDao.PlayerScore> scores,
                                                       Set<Long> commissionEnabled,
                                                       Map<Long, Long> commissions) {
        if (scores.isEmpty()) {
            return List.of();
        }
        List<Long> playerIds = scores.stream().map(TogetherPlayDao.PlayerScore::playerId).toList();
        Map<Long, Player> players = playerService.multiGetPlayerMap(playerIds);
        List<TogetherPlayPlayerInfo> infos = new ArrayList<>(scores.size());
        for (TogetherPlayDao.PlayerScore score : scores) {
            Player player = players.get(score.playerId());
            if (player == null) {
                continue;
            }
            TogetherPlayPlayerInfo info = new TogetherPlayPlayerInfo();
            info.playerId = score.playerId();
            info.nick = player.getNickName();
            info.headImg = player.getHeadImgId();
            info.headFrame = player.getHeadFrameId();
            info.winGold = score.winGold();
            info.commissionEnabled = commissionEnabled.contains(score.playerId());
            info.commissionGold = commissions.getOrDefault(score.playerId(), 0L);
            infos.add(info);
        }
        return infos;
    }

    private void awardCommissions(int gameType, long winnerId, long winGold) {
        Map<Long, CommissionRecipient> recipients;
        try {
            recipients = commissionRecipients(gameType, winnerId);
        } catch (Exception e) {
            log.error("计算好友同玩提成失败 winnerId={},gameType={},winGold={}",
                    winnerId, gameType, winGold, e);
            return;
        }
        for (Map.Entry<Long, CommissionRecipient> en : recipients.entrySet()) {
            long commission = commission(winGold, en.getValue().winCommission());
            if (commission <= 0) {
                continue;
            }
            try {
                if (!slotsPlayerService.addGold(en.getKey(), commission,
                        AddType.SLOTS_TOGETHER_PLAY_COMMISSION,
                        "togetherPlay:" + winnerId, true).success()) {
                    log.warn("发放好友同玩提成失败 playerId={},winnerId={},gameType={},commission={}",
                            en.getKey(), winnerId, gameType, commission);
                    continue;
                }
                if (en.getValue().winnerIsInvitee()) {
                    togetherPlayDao.addCommission(gameType, en.getKey(), winnerId, commission);
                }
            } catch (Exception e) {
                log.error("发放好友同玩提成异常 playerId={},winnerId={},gameType={},commission={}",
                        en.getKey(), winnerId, gameType, commission, e);
            }
        }
    }

    private Map<Long, CommissionRecipient> commissionRecipients(int gameType, long winnerId) {
        Map<Long, CommissionRecipient> result = new LinkedHashMap<>();
        int start = 0;
        while (true) {
            List<Long> inviterIds = togetherPlayDao.inviters(
                    gameType, winnerId, start, MAX_PAGE_SIZE);
            if (inviterIds.isEmpty()) {
                break;
            }
            addInviterRecipients(result, gameType, winnerId, inviterIds);
            if (inviterIds.size() < MAX_PAGE_SIZE) {
                break;
            }
            start += inviterIds.size();
        }

        start = 0;
        while (true) {
            List<Long> inviteeIds = togetherPlayDao.invitees(
                    gameType, winnerId, start, MAX_PAGE_SIZE);
            if (inviteeIds.isEmpty()) {
                break;
            }
            addInviteeRecipients(result, gameType, winnerId, inviteeIds);
            if (inviteeIds.size() < MAX_PAGE_SIZE) {
                break;
            }
            start += inviteeIds.size();
        }
        return result;
    }

    private void addInviterRecipients(Map<Long, CommissionRecipient> result, int gameType,
                                      long winnerId, List<Long> inviterIds) {
        List<Double> scores = togetherPlayDao.scores(gameType, inviterIds);
        Map<Long, TogetherPlayDao.CommissionEffect> effects =
                togetherPlayDao.effects(gameType, inviterIds);
        for (int i = 0; i < inviterIds.size(); i++) {
            long inviterId = inviterIds.get(i);
            TogetherPlayDao.CommissionEffect effect = effects.get(inviterId);
            if (!togetherPlayDao.hasInvite(gameType, inviterId, winnerId)) {
                togetherPlayDao.removeInviter(gameType, winnerId, inviterId);
                continue;
            }
            if (!active(scores, i) || !valid(effect)
                    || !eligibleInvitee(gameType, inviterId, winnerId, effect.commissionUsers())) {
                continue;
            }
            mergeRecipient(result, inviterId,
                    new CommissionRecipient(effect.winCommission(), true));
        }
    }

    private void addInviteeRecipients(Map<Long, CommissionRecipient> result, int gameType,
                                      long winnerId, List<Long> inviteeIds) {
        List<Double> scores = togetherPlayDao.scores(gameType, inviteeIds);
        Map<Long, TogetherPlayDao.CommissionEffect> effects =
                togetherPlayDao.effects(gameType, inviteeIds);
        for (int i = 0; i < inviteeIds.size(); i++) {
            long inviteeId = inviteeIds.get(i);
            TogetherPlayDao.CommissionEffect effect = effects.get(inviteeId);
            if (!active(scores, i) || !valid(effect)
                    || !eligibleInviter(gameType, inviteeId, winnerId, effect.commissionUsers())) {
                continue;
            }
            mergeRecipient(result, inviteeId,
                    new CommissionRecipient(effect.winCommission(), false));
        }
    }

    private void mergeRecipient(Map<Long, CommissionRecipient> recipients, long playerId,
                                CommissionRecipient recipient) {
        recipients.merge(playerId, recipient, (before, current) ->
                new CommissionRecipient(current.winCommission(),
                        before.winnerIsInvitee() || current.winnerIsInvitee()));
    }

    private Set<Long> activeInvitees(int gameType, long inviterId, int limit) {
        if (limit <= 0) {
            return Set.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        int start = 0;
        while (result.size() < limit) {
            List<Long> playerIds = togetherPlayDao.invitees(
                    gameType, inviterId, start, MAX_PAGE_SIZE);
            if (playerIds.isEmpty()) {
                break;
            }
            List<Double> scores = togetherPlayDao.scores(gameType, playerIds);
            for (int i = 0; i < playerIds.size() && result.size() < limit; i++) {
                if (active(scores, i)) {
                    result.add(playerIds.get(i));
                }
            }
            if (playerIds.size() < MAX_PAGE_SIZE) {
                break;
            }
            start += playerIds.size();
        }
        return result;
    }

    private boolean eligibleInvitee(int gameType, long inviterId, long inviteeId, int limit) {
        return activeInvitees(gameType, inviterId, limit).contains(inviteeId);
    }

    private boolean eligibleInviter(int gameType, long inviteeId, long inviterId, int limit) {
        int activeCount = 0;
        int start = 0;
        while (activeCount < limit) {
            List<Long> inviterIds = togetherPlayDao.inviters(
                    gameType, inviteeId, start, MAX_PAGE_SIZE);
            if (inviterIds.isEmpty()) {
                return false;
            }
            List<Double> scores = togetherPlayDao.scores(gameType, inviterIds);
            for (int i = 0; i < inviterIds.size(); i++) {
                long currentInviterId = inviterIds.get(i);
                if (!togetherPlayDao.hasInvite(gameType, currentInviterId, inviteeId)) {
                    togetherPlayDao.removeInviter(gameType, inviteeId, currentInviterId);
                    continue;
                }
                if (!active(scores, i)) {
                    continue;
                }
                if (++activeCount > limit) {
                    return false;
                }
                if (currentInviterId == inviterId) {
                    return true;
                }
            }
            if (inviterIds.size() < MAX_PAGE_SIZE) {
                return false;
            }
            start += inviterIds.size();
        }
        return false;
    }

    private boolean active(List<Double> scores, int index) {
        return scores != null && index < scores.size() && scores.get(index) != null;
    }

    private boolean valid(TogetherPlayDao.CommissionEffect effect) {
        return effect != null && effect.commissionUsers() > 0 && effect.winCommission() > 0;
    }

    private long commission(long winGold, int winCommission) {
        return winGold / GameConstant.TEN_THOUSAND * winCommission
                + winGold % GameConstant.TEN_THOUSAND * winCommission / GameConstant.TEN_THOUSAND;
    }

    private record CommissionRecipient(int winCommission, boolean winnerIsInvitee) {
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private boolean normal(SlotsPlayerGameData gameData) {
        return gameData != null && gameData.getEnterType() == 0;
    }

    private void removeReconnectMark(long playerId) {
        try {
            reconnectDao.remove(playerId);
        } catch (Exception e) {
            log.error("删除好友同玩断线标记失败 playerId={}", playerId, e);
        }
    }
}
