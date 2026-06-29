package com.jjg.game.social.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.NoticeTipBuilder;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sim.service.SimConfigCacheService;
import com.jjg.game.sim.service.SimPackService;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.dao.FriendDao;
import com.jjg.game.social.data.FriendData;
import com.jjg.game.social.data.FriendEntry;
import com.jjg.game.social.data.SendGiftConfig;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.*;
import com.jjg.game.social.pb.struct.FriendInfo;
import com.jjg.game.social.pb.struct.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * 好友业务: 列表/搜索/申请/处理/删除/黑名单/赠送/领取。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class FriendService {
    private static final Logger log = LoggerFactory.getLogger(FriendService.class);

    @Autowired
    private FriendDao friendDao;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SocialSender sender;
    @Autowired
    private SocialStatusService statusService;
    @Autowired
    private SocialRelationCache relationCache;
    @Autowired
    private SimPackService simPackService;
    @Autowired
    private SimConfigCacheService simConfigCacheService;

    // ----------------------- 列表 -----------------------

    /**
     * 获取好友列表
     *
     * @param playerId
     * @return
     */
    public ResFriendList friendList(long playerId) {
        ResFriendList res = new ResFriendList(Code.SUCCESS);
        try {
            FriendData data = friendDao.getOrEmpty(playerId);
            Map<Long, FriendEntry> friends = data.getFriends();

            res.friendLimit = SocialConst.Cfg.FRIEND_LIMIT;
            res.sendCountLimit = simConfigCacheService.getSendGiftConfig().sendCountPerPersonLimit();
            res.sendFriendLimit = simConfigCacheService.getSendGiftConfig().sendPersonLimit();

            int hasSendCountToady = 0;
            List<FriendInfo> list = new ArrayList<>();
            int onlineCount = 0;
            if (friends != null && !friends.isEmpty()) {
                int today = today();
                Map<Long, Player> players = corePlayerService.multiGetPlayerMap(friends.keySet());
                //一次 HMGET 批量取在线会话信息, 替代逐好友 getInfo/online 的 2N 次 Redis 往返
                Map<Long, PlayerSessionInfo> sessionInfos = statusService.infosOf(friends.keySet());
                for (Map.Entry<Long, FriendEntry> en : friends.entrySet()) {
                    long fid = en.getKey();
                    Player p = players.get(fid);
                    PlayerSessionInfo info = sessionInfos.get(fid);
                    int status = statusService.statusOf(info);
                    long offlineSeconds = statusService.offlineSeconds(info, p);
                    //今日对该好友的赠送次数未达每人上限即可继续赠送
                    int sentToday = en.getValue() == null ? 0 : en.getValue().currentGiftSendCount(today);
                    boolean hasPendingGift = data.getPendingGifts() != null && data.getPendingGifts().containsKey(fid);
                    if (status != SocialStatusService.OFFLINE) {
                        onlineCount++;
                    }
                    list.add(SocialPbConverter.toFriendInfo(p, status, offlineSeconds, sentToday, hasPendingGift));
                    hasSendCountToady += sentToday;
                }
                //在线 > 游戏中 > 离线
                list.sort(Comparator.comparingInt(f -> statusRank(f.status)));
            }
            res.friends = list;
            res.onlineCount = onlineCount;
            res.hasSendCountToady = hasSendCountToady;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 好友列表中好友状态排序
     *
     * @param status
     * @return
     */
    private int statusRank(int status) {
        if (status == SocialStatusService.ONLINE) {
            return 0;
        }
        if (status == SocialStatusService.IN_GAME) {
            return 1;
        }
        return 2;
    }

    // ----------------------- 搜索 / 申请 -----------------------

    /**
     * 搜索玩家
     *
     * @param selfId
     * @param targetId
     * @return
     */
    public ResSearchPlayer search(long selfId, long targetId) {
        ResSearchPlayer res = new ResSearchPlayer(Code.SUCCESS);
        try {
            if (targetId <= 0) {
                res.code = Code.PARAM_ERROR;
                log.warn("搜索玩家失败，targetId不能小于0， selfId={},targetId={}", selfId, targetId);
                return res;
            }
            Player p = corePlayerService.get(targetId);
            if (p == null) {
                res.code = Code.NOT_FOUND;
                log.warn("搜索玩家失败，未找到该玩家， selfId={},targetId={}", selfId, targetId);
                return res;
            }
            res.playerId = p.getId();
            res.nick = p.getNickName();
            res.headImg = p.getHeadImgId();
            res.headFrame = p.getHeadFrameId();
            res.level = p.getLevel();
            //关系: 0陌生 1好友 2已申请
            FriendData selfData = friendDao.getOrEmpty(selfId);
            if (selfData.isFriend(targetId)) {
                res.relation = 1;
            } else if (targetId != selfId && friendDao.hasPendingRequest(targetId, selfId)) {
                res.relation = 2;
            } else {
                res.relation = 0;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;

    }

    /**
     * 发起好友申请
     *
     * @param self
     * @param targetId
     * @return
     */
    public ResAddFriend addFriend(Player self, long targetId) {
        ResAddFriend res = new ResAddFriend(Code.SUCCESS);
        try {
            long selfId = self.getId();
            if (targetId <= 0 || targetId == selfId) {
                res.code = Code.PARAM_ERROR;
                log.warn("发起好友申请失败,targetId错误, selfId={},targetId={}", selfId, targetId);
                return res;
            }
            Player target = corePlayerService.get(targetId);
            if (target == null) {
                res.code = Code.NOT_FOUND;
                log.warn("发起好友申请失败，未找到该玩家， selfId={},targetId={}", selfId, targetId);
                return res;
            }
            FriendData selfData = friendDao.getOrEmpty(selfId);
            if (selfData.isFriend(targetId)) {
                res.code = Code.FORBID;
                log.warn("发起好友申请失败，该玩家已经是好友， selfId={},targetId={}", selfId, targetId);
                return res;
            }
            if (selfData.friendCount() >= SocialConst.Cfg.FRIEND_LIMIT) {
                res.code = Code.FORBID;
                log.warn("发起好友申请失败，好友数量达到上限， selfId={},targetId={},friendCount={}", selfId, targetId, selfData.friendCount());
                return res;
            }

            int today = today();
            int sentToday = selfData.currentDailyRequestCount(today);
            if (sentToday >= SocialConst.Cfg.DAILY_REQUEST_LIMIT) {
                res.code = Code.FORBID;
                log.warn("发起好友申请失败，进入申请达到上限， selfId={},targetId={},sentToday={}", selfId, targetId, sentToday);
                return res;
            }

            //对方待处理申请封顶, 防止热门玩家 pendingRequests 无界膨胀 (聚合只回传计数)
            if (friendDao.pendingRequestCount(targetId) >= SocialConst.Cfg.PENDING_REQUEST_LIMIT) {
                res.code = Code.FORBID;
                log.warn("发起好友申请失败，对方待处理申请已达上限 selfId={},targetId={}", selfId, targetId);
                return res;
            }

            long now = System.currentTimeMillis();
            //条件 upsert 原子判重+写入, 替代"先查重(hasPendingRequest)再写"的两次往返
            if (!friendDao.addRequestIfAbsent(targetId, selfId, now)) {
                res.code = Code.FORBID;
                log.warn("发起好友申请失败，已经申请过添加该好友， selfId={},targetId={}", selfId, targetId);
                return res;
            }
            friendDao.setDailyRequest(selfId, today, sentToday + 1);

            //通知在线目标
            NotifyFriendRequest notify = new NotifyFriendRequest(Code.SUCCESS);
            notify.request = SocialPbConverter.toRequestInfo(self, now);
            sender.sendTo(targetId, notify);
            log.info("发起好友申请 playerId={},targetId={}", selfId, targetId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;

    }

    // ----------------------- 申请管理 -----------------------

    /**
     * 好友申请列表
     *
     * @param playerId
     * @return
     */
    public ResRequestList requestList(long playerId) {
        ResRequestList res = new ResRequestList(Code.SUCCESS);
        try {
            FriendData data = friendDao.getOrEmpty(playerId);
            res.requests = new ArrayList<>();
            Map<Long, Long> pending = data.getPendingRequests();
            if (pending == null || pending.isEmpty()) {
                return res;
            }
            long now = System.currentTimeMillis();

            //过滤过期申请, 并清理
            List<Long> expired = new ArrayList<>();
            List<Long> validIds = new ArrayList<>();
            for (Map.Entry<Long, Long> en : pending.entrySet()) {
                if (now - en.getValue() > SocialConst.Cfg.REQUEST_VALID_MILLS) {
                    expired.add(en.getKey());
                } else {
                    validIds.add(en.getKey());
                }
            }
            if (!expired.isEmpty()) {
                friendDao.removeRequests(playerId, expired);
            }
            if (validIds.isEmpty()) {
                return res;
            }

            Map<Long, Player> players = corePlayerService.multiGetPlayerMap(validIds);
            for (Long rid : validIds) {
                Player p = players.get(rid);
                if (p == null) {
                    continue;
                }
                res.requests.add(SocialPbConverter.toRequestInfo(p, pending.get(rid)));
            }
            //按申请时间倒序
            res.requests.sort(Comparator.comparingLong((RequestInfo r) -> r.requestTime).reversed());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 处理好友申请
     *
     * @param ids
     * @param agree
     * @return
     */
    public void handleRequest(PlayerController playerController, List<Long> ids, boolean agree) {
        ResHandleRequest res = new ResHandleRequest(Code.SUCCESS);
        try {
            FriendData selfData = friendDao.getOrEmpty(playerController.playerId());
            res.handledIds = new ArrayList<>();
            res.addedFriends = new ArrayList<>();

            Map<Long, Long> pending = selfData.getPendingRequests();
            if (pending == null || pending.isEmpty()) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.warn("处理好友申请失败，申请列表为空 playerId={}", playerController.playerId());
                return;
            }
            //ids 为空表示一键处理全部
            List<Long> targetIds = (ids == null || ids.isEmpty()) ? new ArrayList<>(pending.keySet()) : ids;

            int friendCount = selfData.friendCount();
            int friendLimit = SocialConst.Cfg.FRIEND_LIMIT;
            long now = System.currentTimeMillis();

            //本次实际处理的申请(去重 + 必须仍在待处理中); 无论同意/拒绝都会被移除
            List<Long> handledIds = new ArrayList<>();
            for (Long rid : targetIds) {
                if (rid != null && pending.containsKey(rid) && !handledIds.contains(rid)) {
                    handledIds.add(rid);
                }
            }
            if (handledIds.isEmpty()) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("处理好友申请失败，实际处理申请列表为空 playerId={}", playerController.playerId());
                return;
            }
            res.handledIds.addAll(handledIds);

            if (agree) {
                //同意: 批量取申请者资料 + 在线状态, 受双方好友上限约束筛出可加好友
                Map<Long, Player> players = corePlayerService.multiGetPlayerMap(handledIds);
                Map<Long, PlayerSessionInfo> sessionInfos = statusService.infosOf(handledIds);
                //一次聚合取各申请者当前好友数, 申请方已满者不建立关系(其申请照常移除)
                Map<Long, Integer> requesterCounts = friendDao.friendCounts(handledIds);
                Map<Long, FriendEntry> acceptedFriends = new LinkedHashMap<>();
                for (Long rid : handledIds) {
                    if (friendCount >= friendLimit) {
                        break;
                    }
                    Player requester = players.get(rid);
                    if (requester == null) {
                        continue;
                    }
                    if (requesterCounts.getOrDefault(rid, 0) >= friendLimit) {
                        log.info("同意好友申请跳过，申请方好友数已达上限 selfId={},requesterId={}", playerController.playerId(), rid);
                        continue;
                    }
                    acceptedFriends.put(rid, new FriendEntry(now));
                    friendCount++;

                    PlayerSessionInfo info = sessionInfos.get(rid);
                    int status = statusService.statusOf(info);
                    long offlineSeconds = statusService.offlineSeconds(info, requester);
                    res.addedFriends.add(SocialPbConverter.toFriendInfo(requester, status, offlineSeconds, 0, false));
                }

                //一次性写入: 移除全部已处理申请 + 双向建立已同意好友(1 次单文档更新 + 1 次 bulk)
                friendDao.applyHandleRequest(playerController.playerId(), handledIds, acceptedFriends, now);

                playerController.send(res);

                //通知已同意的申请方刷新(其新增了我这个好友); selfId 状态为常量, 循环外算一次
                if (!acceptedFriends.isEmpty()) {
                    //好友关系已变更, 失效双方的好友id缓存
                    relationCache.invalidateFriendIds(playerController.playerId(), acceptedFriends.keySet());
                    NotifyNewFriendHandle notify = new NotifyNewFriendHandle();
                    notify.friendInfo = SocialPbConverter.toFriendInfo(playerController.getPlayer(), statusService.statusOf(playerController.playerId()), 0, 0, false);
                    notify.agree = true;
                    sender.sendTo(acceptedFriends.keySet(), notify);
                }

            } else {
                //全部拒绝: 一次性移除全部已处理申请
                friendDao.removeRequests(playerController.playerId(), handledIds);
                NoticeTipBuilder builder = NoticeTipBuilder.builder().tipType(TipUtils.TipType.TOAST).languageId(SocialConst.LangIds.REJECT_ADD_FRIEND_APPLY);
                builder.addArg(0, playerController.getPlayer().getNickName());
                TipUtils.sendTip(playerController,TipUtils.TipType.TOAST,() -> builder.build());
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
            playerController.send(res);
        }
    }

    // ----------------------- 删除 -----------------------

    /**
     * 删除好友
     *
     * @param selfId
     * @param ids
     * @return
     */
    public ResDeleteFriend deleteFriend(long selfId, List<Long> ids) {
        ResDeleteFriend res = new ResDeleteFriend(Code.SUCCESS);
        try {
            FriendData selfData = friendDao.getOrEmpty(selfId);
            res.removedIds = new ArrayList<>();
            if (ids == null || ids.isEmpty()) {
                return res;
            }
            //去重 + 仅保留确为好友者
            List<Long> toRemove = new ArrayList<>();
            for (Long fid : ids) {
                if (fid != null && selfData.isFriend(fid) && !toRemove.contains(fid)) {
                    toRemove.add(fid);
                }
            }
            if (toRemove.isEmpty()) {
                return res;
            }
            //双向批量删除 (1 次单文档更新 + 1 次 bulk)
            friendDao.removeFriendsBidirectional(selfId, toRemove);
            //好友关系已变更, 失效双方的好友id缓存
            relationCache.invalidateFriendIds(selfId, toRemove);
            res.removedIds.addAll(toRemove);
            log.info("删除好友 playerId={},ids={}", selfId, toRemove);

            NotifyDeleteFriend notify = new NotifyDeleteFriend();
            notify.friendId = selfId;
            sender.sendTo(toRemove, notify);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    // ----------------------- 黑名单 -----------------------

    /**
     * 黑名单操作
     *
     * @param selfId
     * @param op
     * @param ids
     * @return
     */
    public ResBlacklistOp blacklistOp(long selfId, int op, List<Long> ids) {
        ResBlacklistOp res = new ResBlacklistOp(Code.SUCCESS);
        try {
            FriendData selfData = friendDao.getOrEmpty(selfId);
            res.op = op;
            res.playerIds = new ArrayList<>();
            long now = System.currentTimeMillis();

            switch (op) {
                case SocialConst.OpType.BLACKLIST_ADD -> {
                    if (ids == null || ids.isEmpty()) {
                        return res;
                    }
                    int count = selfData.blacklistCount();
                    int limit = SocialConst.Cfg.BLACKLIST_LIMIT;
                    List<Long> toAdd = new ArrayList<>();
                    for (Long id : ids) {
                        if (id == null || id == selfId || selfData.isBlacklisted(id) || toAdd.contains(id)) {
                            continue;
                        }
                        if (count >= limit) {
                            res.code = Code.FORBID;
                            log.warn("添加到黑名单失败,数量达到上限 playerId={},op={},ids={},count={}.limit={}", selfId, op, ids, count, limit);
                            break;
                        }
                        toAdd.add(id);
                        count++;
                    }
                    if (!toAdd.isEmpty()) {
                        friendDao.addBlacklists(selfId, toAdd, now);
                        relationCache.publishInvalidate(selfId);
                        res.playerIds.addAll(toAdd);
                    }
                }
                case SocialConst.OpType.BLACKLIST_REMOVE -> {
                    if (ids == null || ids.isEmpty()) {
                        return res;
                    }
                    friendDao.removeBlacklists(selfId, ids);
                    relationCache.publishInvalidate(selfId);
                    res.playerIds.addAll(ids);
                }
                case SocialConst.OpType.BLACKLIST_CLEAR -> {
                    res.playerIds.addAll(selfData.getBlacklist().keySet());
                    friendDao.clearBlacklist(selfId);
                    relationCache.publishInvalidate(selfId);
                }
                default -> res.code = Code.PARAM_ERROR;
            }
            log.info("黑名单操作 playerId={},op={},ids={},code={}", selfId, op, ids, res.code);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 黑名单列表
     *
     * @param playerId
     * @return
     */
    public ResBlacklistList blacklistList(long playerId) {
        ResBlacklistList res = new ResBlacklistList(Code.SUCCESS);
        try {
            FriendData data = friendDao.getOrEmpty(playerId);
            res.list = new ArrayList<>();
            Map<Long, Long> blacklist = data.getBlacklist();
            if (blacklist == null || blacklist.isEmpty()) {
                return res;
            }
            Map<Long, Player> players = corePlayerService.multiGetPlayerMap(blacklist.keySet());
            for (Long id : blacklist.keySet()) {
                res.list.add(SocialPbConverter.toBlacklistInfo(players.get(id)));
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    // ----------------------- 赠送 / 领取 -----------------------

    /**
     * 赠送礼物
     *
     * @param selfId
     * @param ids
     * @param all
     * @return
     */
    public ResSendGift sendGift(long selfId, List<Long> ids, boolean all) {
        ResSendGift res = new ResSendGift(Code.SUCCESS);
        try {
            SendGiftConfig cfg = simConfigCacheService.getSendGiftConfig();
            if (cfg == null) {
                res.code = Code.SAMPLE_ERROR;
                log.warn("赠送礼物失败, 赠礼配置未加载 selfId={}", selfId);
                return res;
            }
            FriendData selfData = friendDao.getOrEmpty(selfId);
            res.sentIds = new ArrayList<>();

            Map<Long, FriendEntry> friends = selfData.getFriends();
            if (friends == null || friends.isEmpty()) {
                return res;
            }
            int today = today();

            //候选 = 全部好友 或 指定好友(必须是好友); LinkedHashSet 保持顺序便于按配额截断
            Set<Long> candidates = new LinkedHashSet<>();
            if (all) {
                candidates.addAll(friends.keySet());
            } else if (ids != null) {
                for (Long id : ids) {
                    if (friends.containsKey(id)) {
                        candidates.add(id);
                    }
                }
            }
            if (candidates.isEmpty()) {
                return res;
            }

            int perPersonLimit = cfg.sendCountPerPersonLimit();
            int personsToday = selfData.currentDailyGiftPersonCount(today);
            //今日剩余可赠送的不同好友数 (新好友才消耗该配额, 给老好友续送不占)
            int personQuota = cfg.sendPersonLimit() - personsToday;

            //逐个判定: 跳过今日次数已满者; 新好友受今日人数配额约束; 记录赠送后该好友的当日累计次数
            Map<Long, Integer> targetNewCounts = new LinkedHashMap<>();
            int newPersons = 0;
            for (Long fid : candidates) {
                FriendEntry entry = friends.get(fid);
                int sentToday = entry == null ? 0 : entry.currentGiftSendCount(today);
                if (sentToday >= perPersonLimit) {
                    continue;
                }
                if (sentToday == 0) {
                    if (newPersons >= personQuota) {
                        continue;
                    }
                    newPersons++;
                }
                targetNewCounts.put(fid, sentToday + 1);
            }
            if (targetNewCounts.isEmpty()) {
                return res;
            }

            friendDao.bulkSendGift(selfId, targetNewCounts, cfg.count(), today, personsToday + newPersons);

            //通知在线好友(一键收送红点)
            List<Long> targets = new ArrayList<>(targetNewCounts.keySet());
            NotifyGiftReceived notify = new NotifyGiftReceived(Code.SUCCESS);
            notify.senderId = selfId;
            notify.itemId = cfg.itemId();
            notify.count = cfg.count();
            sender.sendTo(targets, notify);
            res.sentIds = targets;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 领取赠礼
     *
     * @param selfId
     * @return
     */
    public ResCollectGift collectGift(long selfId) {
        ResCollectGift res = new ResCollectGift(Code.SUCCESS);
        try {
            SendGiftConfig cfg = simConfigCacheService.getSendGiftConfig();
            if (cfg == null) {
                res.code = Code.SAMPLE_ERROR;
                log.warn("领取赠礼失败, 赠礼配置未加载 selfId={}", selfId);
                return res;
            }
            FriendData selfData = friendDao.getOrEmpty(selfId);
            Map<Long, Long> pendingGifts = selfData.getPendingGifts();

            long total = 0;
            if (pendingGifts != null) {
                for (Long v : pendingGifts.values()) {
                    if (v != null) {
                        total += v;
                    }
                }
            }
            if (total <= 0) {
                res.code = Code.NOT_FOUND;
                return res;
            }
            //发放道具 (领取者即调用方本人, addItemsByPlayerId 自动按在线/离线入账)
            simPackService.addItemsByPlayerId(selfId, Map.of(cfg.itemId(), total), AddType.FRIEND_GIFT_COLLECT, "", true);
            friendDao.clearPendingGifts(selfId);
            res.itemId = cfg.itemId();
            res.gainCount = total;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    private int today() {
        LocalDate d = LocalDate.now();
        return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
    }
}
