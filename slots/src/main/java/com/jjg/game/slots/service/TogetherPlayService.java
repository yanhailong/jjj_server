package com.jjg.game.slots.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.Code;
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
import java.util.List;
import java.util.Map;
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
    private final SocialRelationCache relationCache;
    @ClusterRpcReference
    private ToSocialBridge toSocialBridge;

    public TogetherPlayService(TogetherPlayDao togetherPlayDao, TogetherPlayReconnectDao reconnectDao,
                               CorePlayerService playerService,
                               SocialRelationCache relationCache) {
        this.togetherPlayDao = togetherPlayDao;
        this.reconnectDao = reconnectDao;
        this.playerService = playerService;
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
        gameData.setTogetherPlayData(togetherData);
        try {
            togetherPlayDao.enter(gameData.getGameType(), gameData.getPlayerId(), winGold);
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
        res.playerInfos = toPlayerInfos(scores);
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
                                    togetherData.getInvitePlayerIds().add(targetPlayerId);
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

    private List<TogetherPlayPlayerInfo> toPlayerInfos(List<TogetherPlayDao.PlayerScore> scores) {
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
            infos.add(info);
        }
        return infos;
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
