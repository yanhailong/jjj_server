package com.jjg.game.slots.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GiftListCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.dao.CoopRoomRecordDao;
import com.jjg.game.sim.data.CoopRoomRecord;
import com.jjg.game.sim.data.CoopTaskRule;
import com.jjg.game.sim.service.CoopTaskConfigService;
import com.jjg.game.sim.service.SimNodeService;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.CoopMember;
import com.jjg.game.slots.data.CoopRoom;
import com.jjg.game.slots.pb.CoopMemberInfo;
import com.jjg.game.slots.pb.CoopRoomSnapshot;
import com.jjg.game.slots.pb.NotifyCoopGift;
import com.jjg.game.slots.pb.NotifyCoopRoomResult;
import com.jjg.game.slots.pb.NotifyCoopRoomUpdate;
import com.jjg.game.slots.pb.NotifyCoopSpin;
import com.jjg.game.slots.pb.ResCoopGift;
import com.jjg.game.slots.pb.ResCoopInvite;
import com.jjg.game.slots.pb.ResCoopRoomOp;
import com.jjg.game.slots.pb.ResEnterCoopRoom;
import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.social.bridge.ToSocialBridge;
import com.jjg.game.social.channel.RoomChatProvider;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.NotifyChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 协作任务房间管理器 (slots 节点)。
 * <p>
 * 房间为纯内存态: 全体成员被 hall 路由到同一节点, 旋转进度在房间对象内聚合, 无跨节点同步;
 * Redis 路由记录仅在成员/状态变更时覆写。房间状态变更统一 synchronized(room) 串行化
 * (成员数受任务人数上限约束, 锁粒度小竞争极低); {@code memberRoomIndex} 让非协作玩家的
 * 旋转以一次 ConcurrentHashMap.get 零成本跳过联动。
 *
 * @author 11
 * @date 2026/7/6
 */
@Component
public class CoopRoomManager implements RoomChatProvider {
    private static final Logger log = LoggerFactory.getLogger(CoopRoomManager.class);

    @ClusterRpcReference
    private ToSimBridge toSimBridge;
    @ClusterRpcReference
    private ToSocialBridge toSocialBridge;

    @Autowired
    private CoopRoomRecordDao roomRecordDao;
    @Autowired
    private CoopTaskConfigService coopTaskConfigService;
    @Autowired
    private SimNodeService simNodeService;
    @Autowired
    private MarsCurator marsCurator;
    @Autowired
    private PlayerPackService playerPackService;

    //本节点协作房间 roomId -> room
    private final Map<Long, CoopRoom> rooms = new ConcurrentHashMap<>();
    //成员索引 playerId -> roomId (旋转钩子 O(1) 判定, 非协作玩家 get==null 直接跳过)
    private final Map<Long, Long> memberRoomIndex = new ConcurrentHashMap<>();
    //GC 专用单线程: 解散/结算含同步 Redis 删除与 RPC, 不占用全进程共享的 wheel-timer 线程
    private ScheduledExecutorService gcExecutor;

    public void init() {
        gcExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "coop-room-gc");
            t.setDaemon(true);
            return t;
        });
        recoverRooms();
        //fixedDelay: 上一轮跑完才计时, Redis 抖动时天然防任务堆积
        gcExecutor.scheduleWithFixedDelay(this::gcRooms,
                SlotsConst.GC_PERIOD_SECONDS, SlotsConst.GC_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    // =====================================================================
    // 进房 / 房间操作
    // =====================================================================

    /**
     * 进入房间 (切节点后附着/断线重连/新成员加入的权威判定)。
     */
    public ResEnterCoopRoom enterRoom(PlayerController pc, long roomId) {
        ResEnterCoopRoom res = new ResEnterCoopRoom(Code.FAIL);
        long playerId = pc.playerId();
        CoopRoom room = loadRoom(roomId);
        if (room == null) {
            log.warn("进入房间失败,未找到房间 playerId={},roomId={} ", playerId, roomId);
            return res;
        }
        //在锁外拉取战力 (仅依赖玩家自身 sim 数据, 避免网络 RPC 占用房间锁)
        int combatPower = fetchCombatPower(playerId, pc.ipAddress());
        synchronized (room) {
            if (room.getStatus() == CoopTaskConst.RoomStatus.FINISHED && !room.getMembers().containsKey(playerId)) {
                log.info("进入房间失败,房间已结算 playerId={},roomId={}", playerId, roomId);
                return res;
            }
            CoopMember member = room.getMembers().get(playerId);
            boolean newMember = member == null;
            if (newMember) {
                //新成员加入的权威判定 (hall 侧仅预检查)
                if (room.getStatus() != CoopTaskConst.RoomStatus.WAITING) {
                    log.info("进入房间失败,游戏已开始 playerId={},roomId={},status={}", playerId, roomId, room.getStatus());
                    return res;
                }
                if (room.getMembers().size() >= room.getRule().maxMembers()) {
                    log.info("进入房间失败,房间已满员 playerId={},roomId={},members={},max={}",
                            playerId, roomId, room.getMembers().size(), room.getRule().maxMembers());
                    return res;
                }
                if (!roomRecordDao.acquirePlayerRoom(playerId, roomId)) {
                    log.info("进入协作房间失败,玩家已在其他房间 playerId={},roomId={},occupiedRoomId={}",
                            playerId, roomId, roomRecordDao.getPlayerRoom(playerId));
                    return res;
                }
                member = room.addMember(playerId);
                memberRoomIndex.put(playerId, roomId);
            }
            Player player = pc.getPlayer();
            if (player != null) {
                member.setName(player.getNickName());
                member.setHeadImgId(player.getHeadImgId());
                member.setHeadFrameId(player.getHeadFrameId());
            }
            member.setCombatPower(combatPower);
            member.setOnline(true);
            member.setPlayerController(pc);
            //仅新成员加入才覆写路由记录(memberIds 变更); 断线重连成员数/状态不变, 跳过 Redis 写
            if (newMember) {
                updateRecord(room);
            }

            res.code = Code.SUCCESS;
            res.room = buildSnapshot(room);
            broadcastUpdate(room, playerId);
        }
        log.info("玩家进入协作房间 playerId={},roomId={}", playerId, roomId);
        return res;
    }

    /**
     * 房间操作: 准备/取消准备/开始/退出(房主=解散)/踢人。
     */
    public ResCoopRoomOp operate(PlayerController pc, int op, long targetId) {
        ResCoopRoomOp res = new ResCoopRoomOp(Code.FAIL);
        res.op = op;
        long playerId = pc.playerId();
        CoopRoom room = roomOf(playerId);
        if (room == null) {
            log.info("协作房间操作失败,不在房间中 playerId={},op={}", playerId, op);
            return res;
        }
        synchronized (room) {
            CoopMember member = room.getMembers().get(playerId);
            if (member == null) {
                log.info("协作房间操作失败,非房间成员 playerId={},roomId={},op={}", playerId, room.getRoomId(), op);
                return res;
            }
            switch (op) {
                case SlotsConst.Op.READY, SlotsConst.Op.CANCEL_READY -> {
                    //需求: 准备机制仅约束协助者, 房主点开始
                    if (room.getStatus() != CoopTaskConst.RoomStatus.WAITING
                            || playerId == room.getOwnerId()) {
                        log.info("协作房间准备失败,状态不允许 playerId={},roomId={},status={},owner={}",
                                playerId, room.getRoomId(), room.getStatus(), playerId == room.getOwnerId());
                        return res;
                    }
                    member.setReady(op == SlotsConst.Op.READY);
                    broadcastUpdate(room, 0);
                }
                case SlotsConst.Op.START -> {
                    int reason = start(room, playerId);
                    if (reason != Code.SUCCESS) {
                        res.code = reason;
                        log.info("协作房间开始失败 playerId={},roomId={},reason={}", playerId, room.getRoomId(), reason);
                        return res;
                    }
                }
                case SlotsConst.Op.EXIT -> {
                    int reason = exit(room, playerId);
                    if (reason != Code.SUCCESS) {
                        res.code = reason;
                        log.info("协作房间退出失败 playerId={},roomId={},reason={}", playerId, room.getRoomId(), reason);
                        return res;
                    }
                }
                case SlotsConst.Op.KICK -> {
                    //需求: 游戏开始后无法踢出房间
                    if (playerId != room.getOwnerId() || targetId == room.getOwnerId()
                            || room.getStatus() != CoopTaskConst.RoomStatus.WAITING) {
                        log.info("协作房间踢人失败,状态不允许 playerId={},roomId={},targetId={},status={}",
                                playerId, room.getRoomId(), targetId, room.getStatus());
                        return res;
                    }
                    CoopMember target = room.getMembers().remove(targetId);
                    if (target == null) {
                        log.info("协作房间踢人失败,目标不在房间 playerId={},roomId={},targetId={}",
                                playerId, room.getRoomId(), targetId);
                        return res;
                    }
                    memberRoomIndex.remove(targetId, room.getRoomId());
                    roomRecordDao.releasePlayerRoom(targetId, room.getRoomId());
                    notifyRemoved(room, target);
                    updateRecord(room);
                    broadcastUpdate(room, 0);
                    log.info("协作房间踢人 roomId={},ownerId={},targetId={}", room.getRoomId(), playerId, targetId);
                }
                default -> {
                    log.info("协作房间操作失败,未知操作 playerId={},roomId={},op={}", playerId, room.getRoomId(), op);
                    return res;
                }
            }
        }
        res.code = Code.SUCCESS;
        return res;
    }

    /**
     * 房主开始游戏: 全体协助者已准备 + 人数达标 -> 平分 Spin 份额 -> RUNNING。
     */
    private int start(CoopRoom room, long playerId) {
        if (playerId != room.getOwnerId() || room.getStatus() != CoopTaskConst.RoomStatus.WAITING) {
            log.warn("房间开始游戏错误 playerId={},ownerId={},status={}", playerId, room.getOwnerId(), room.getStatus());
            return Code.PARAM_ERROR;
        }
        CoopTaskRule rule = room.getRule();
        if (room.getMembers().size() < rule.minMembers()) {
            log.warn("房间开始游戏错误,房间人数不足 playerId={},memberSize={},minMemberSize={}", playerId, room.getMembers().size(), rule.minMembers());
            return Code.PARAM_ERROR;
        }
        for (CoopMember m : room.getMembers().values()) {
            if (m.getPlayerId() != room.getOwnerId() && !m.isReady()) {
                //需求: 还有玩家未准备
                log.warn("房间开始游戏错误,该玩家未准备 playerId={},memberPlayerId={}", playerId, m.getPlayerId());
                return Code.PARAM_ERROR;
            }
        }

        //平分 Spin 目标: 每人 total/n, 余数按座位序前 remainder 人各 +1
        int n = room.getMembers().size();
        int quota = rule.spinBudget() / n;
        int remainder = rule.spinBudget() % n;
        int index = 0;
        for (CoopMember m : room.membersBySeat()) {
            m.setSpinQuota(quota + (index < remainder ? 1 : 0));
            m.setSpinUsed(0);
            index++;
        }

        long now = System.currentTimeMillis();
        room.setStatus(CoopTaskConst.RoomStatus.RUNNING);
        room.setStartTime(now);
        //失败兜底时限, 防成员挂机/断线不归导致房间悬挂; 未配时限(0)时用系统级兜底, 保证 RUNNING 一定有终结路径
        long duration = rule.durationMinutes() > 0
                ? rule.durationMinutes() * 60_000L : SlotsConst.RUNNING_MAX_FALLBACK_MS;
        room.setDeadline(now + duration);
        updateRecord(room);
        broadcastUpdate(room, 0);
        log.info("协作房间开始 roomId={},taskId={},members={},quota={}(+{}),target={}x{}",
                room.getRoomId(), room.getTaskId(), n, quota, remainder, rule.modeId(), rule.modeCount());
        return Code.SUCCESS;
    }

    /**
     * 退出房间: 未开始或已结算时房主退出=解散, 协助者退出=移除; 开始后不可退出。
     */
    private int exit(CoopRoom room, long playerId) {
        int status = room.getStatus();
        if (status == CoopTaskConst.RoomStatus.RUNNING) {
            //需求: 游戏已开始, 无法退出房间
            return Code.CAN_NOT_EXIT_GAMING_ROOM;
        }
        if ((status == CoopTaskConst.RoomStatus.WAITING || status == CoopTaskConst.RoomStatus.FINISHED)
                && playerId == room.getOwnerId()) {
            dissolve(room);
            return Code.SUCCESS;
        }
        CoopMember member = room.getMembers().remove(playerId);
        if (member != null) {
            memberRoomIndex.remove(playerId, room.getRoomId());
            roomRecordDao.releasePlayerRoom(playerId, room.getRoomId());
        }
        if (room.getMembers().isEmpty()) {
            removeRoom(room);
        } else {
            updateRecord(room);
            broadcastUpdate(room, 0);
        }
        log.info("玩家退出协作房间 playerId={},roomId={},status={}", playerId, room.getRoomId(), status);
        return Code.SUCCESS;
    }

    /**
     * 解散房间: 全员通知并清理 (房主主动/等待超时)。
     */
    private void dissolve(CoopRoom room) {
        for (CoopMember member : room.getMembers().values()) {
            memberRoomIndex.remove(member.getPlayerId(), room.getRoomId());
            roomRecordDao.releasePlayerRoom(member.getPlayerId(), room.getRoomId());
            notifyRemoved(room, member);
        }
        room.getMembers().clear();
        removeRoom(room);
        log.info("协作房间解散 roomId={},taskId={}", room.getRoomId(), room.getTaskId());
    }

    // =====================================================================
    // 邀请 / 互动道具
    // =====================================================================

    /**
     * 房主发送频道邀请: 经 RPC 在 hall 侧投递 (校验/限频在 hall)。
     */
    public ResCoopInvite invite(PlayerController pc, int channelCode, List<Long> targetIds) {
        ResCoopInvite res = new ResCoopInvite(Code.FAIL);
        long playerId = pc.playerId();
        CoopRoom room = roomOf(playerId);
        if (room == null || playerId != room.getOwnerId()
                || room.getStatus() != CoopTaskConst.RoomStatus.WAITING) {
            log.info("协作房间邀请失败,非房主或状态不允许 playerId={},roomId={}",
                    playerId, room == null ? 0 : room.getRoomId());
            return res;
        }
        if (channelCode != ChatChannelType.WORLD.getCode()
                && channelCode != ChatChannelType.ALLIANCE.getCode()
                && channelCode != ChatChannelType.PRIVATE.getCode()) {
            log.info("协作房间邀请失败,非法频道 playerId={},roomId={},channel={}",
                    playerId, room.getRoomId(), channelCode);
            return res;
        }
        //房间级限频 (私聊频道无个人限频, 防连点放大); 房主请求单线程串行, 无并发写
        long now = System.currentTimeMillis();
        if (now - room.getLastInviteTime() < SlotsConst.COOP_INVITE_INTERVAL_MS) {
            log.info("协作房间邀请失败,发送过于频繁 playerId={},roomId={}", playerId, room.getRoomId());
            return res;
        }
        room.setLastInviteTime(now);

        //邀请内容为客户端约定格式; 加入合法性由加入流程权威校验, 无伪造风险
        JSONObject content = new JSONObject();
        content.put("type", "coopInvite");
        content.put("roomId", room.getRoomId());
        content.put("taskId", room.getTaskId());
        content.put("gameType", room.getGameType());
        String inviteContent = content.toJSONString();

        ClusterClient client = simNodeService.getSimClusterClient(playerId, pc.ipAddress());
        if (client == null) {
            log.warn("协作房间邀请失败,无可用sim节点 playerId={},roomId={}", playerId, room.getRoomId());
            return res;
        }
        List<Long> targets = channelCode == ChatChannelType.PRIVATE.getCode()
                ? (targetIds == null ? List.of() : targetIds)
                : List.of(0L);
        //上限截断: 防客户端构造超大列表放大跨节点 RPC/聊天
        if (targets.size() > CoopTaskConst.MAX_INVITE_TARGETS) {
            targets = targets.subList(0, CoopTaskConst.MAX_INVITE_TARGETS);
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            for (Long targetId : targets) {
                if (targetId == null) {
                    continue;
                }
                long finalTargetId = targetId;
                //异步尽力投递, hall 侧限频兜底; 失败仅日志
                rpcContext.asyncCall(() -> toSocialBridge.sendCoopInvite(
                                playerId, channelCode, finalTargetId, inviteContent))
                        .whenComplete((code, throwable) -> {
                            if (throwable != null) {
                                log.warn("协作房间邀请投递异常 playerId={},channel={},targetId={}",
                                        playerId, channelCode, finalTargetId, throwable);
                            }
                        });
            }
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
        res.code = Code.SUCCESS;
        log.info("协作房间发送邀请 playerId={},roomId={},channel={},targets={}",
                playerId, room.getRoomId(), channelCode, targets.size());
        return res;
    }

    /**
     * 房间互动道具赠送: 按 GiftList 配置扣费 + 全房动效广播。
     */
    public ResCoopGift gift(PlayerController pc, long targetId, int giftId) {
        ResCoopGift res = new ResCoopGift(Code.FAIL);
        long playerId = pc.playerId();
        CoopRoom room = roomOf(playerId);
        if (room == null || !room.getMembers().containsKey(targetId) || playerId == targetId) {
            log.info("协作房间赠送失败,房间或目标非法 playerId={},targetId={},roomId={}",
                    playerId, targetId, room == null ? 0 : room.getRoomId());
            return res;
        }
        GiftListCfg giftCfg = GameDataManager.getGiftListCfg(giftId);
        if (giftCfg == null) {
            log.info("协作房间赠送失败,礼物配置缺失 playerId={},giftId={}", playerId, giftId);
            return res;
        }
        CommonResult<ItemOperationResult> deduct = playerPackService.removeItems(pc.getPlayer(),
                giftCfg.getCost(), AddType.SIM_COOP_ROOM_GIFT, "coopGiftId=" + giftId);
        if (!deduct.success()) {
            //需求: 携带金币或钻石不足时直接提示
            res.code = Code.NOT_ENOUGH;
            log.info("协作房间赠送失败,货币不足 playerId={},giftId={},cost={}", playerId, giftId, giftCfg.getCost());
            return res;
        }
        NotifyCoopGift notify = new NotifyCoopGift(Code.SUCCESS);
        notify.fromId = playerId;
        notify.toId = targetId;
        notify.giftId = giftId;
        List<PlayerController> receivers;
        boolean stillValid;
        synchronized (room) {
            stillValid = rooms.get(room.getRoomId()) == room && room.getMembers().containsKey(playerId)
                    && room.getMembers().containsKey(targetId) && playerId != targetId;
            receivers = stillValid ? receivers(room) : List.of();
        }
        if (!stillValid) {
            CommonResult<ItemOperationResult> refund = playerPackService.addItems(playerId, giftCfg.getCost(),
                    AddType.SIM_COOP_ROOM_GIFT_REFUND, "coopGiftId=" + giftId);
            res.diamond = refund != null && refund.data != null
                    ? refund.data.getDiamond() : (deduct.data == null ? 0 : deduct.data.getDiamond());
            log.warn("协作房间赠送失败并返还扣费,扣费后成员状态变化 playerId={},targetId={},roomId={},refund={}",
                    playerId, targetId, room.getRoomId(), refund != null && refund.success());
            return res;
        }
        res.code = Code.SUCCESS;
        res.diamond = deduct.data == null ? 0 : deduct.data.getDiamond();
        sendAll(receivers, notify);
        return res;
    }

    // =====================================================================
    // 旋转联动 (AbstractSlotsGameManager 钩子)
    // =====================================================================

    /**
     * 旋转前校验: 非协作玩家零成本放行; 房间未开始禁转; 血条耗尽禁转。
     *
     * @return Code.SUCCESS 放行
     */
    public int beforeSpin(long playerId, int gameType) {
        Long roomId = memberRoomIndex.get(playerId);
        if (roomId == null) {
            return Code.SUCCESS;
        }
        CoopRoom room = rooms.get(roomId);
        if (room == null || room.getGameType() != gameType
                || room.getStatus() == CoopTaskConst.RoomStatus.FINISHED) {
            //已结算/非房间游戏: 放行为普通旋转
            return Code.SUCCESS;
        }
        if (room.getStatus() == CoopTaskConst.RoomStatus.WAITING) {
            //需求: 房主点开始后游戏才正式开始
            return Code.FAIL;
        }
        CoopMember member = room.getMembers().get(playerId);
        if (member == null) {
            return Code.SUCCESS;
        }
        //需求: 血量消耗完之后不能继续游戏
        return member.hpLeft() > 0 ? Code.SUCCESS : Code.NOT_ENOUGH;
    }

    /**
     * 旋转成功联动: 扣血 + 特殊模式触发累计共享池 + 成败判定。
     * <p>
     * 特殊事件判定: 普通状态下抽中的结果库类型集含任务的模式id, 即视为触发一次该特殊模式
     * (结果库按 SpecialMode.type 分类生成, 见 AbstractSlotsGenerateManager)。
     */
    public void onSpin(long playerId, int gameType, int statusBefore, GameRunInfo<?> gameRunInfo) {
        Long roomId = memberRoomIndex.get(playerId);
        if (roomId == null) {
            return;
        }
        CoopRoom room = rooms.get(roomId);
        if (room == null || room.getGameType() != gameType
                || room.getStatus() != CoopTaskConst.RoomStatus.RUNNING) {
            return;
        }
        NotifyCoopSpin spinNotify = null;
        List<PlayerController> receivers = null;
        try {
            synchronized (room) {
                if (room.getStatus() != CoopTaskConst.RoomStatus.RUNNING) {
                    return;
                }
                CoopMember member = room.getMembers().get(playerId);
                if (member == null || member.hpLeft() <= 0) {
                    return;
                }
                member.setSpinUsed(member.getSpinUsed() + 1);

                CoopTaskRule rule = room.getRule();
                GameConditionEvent conditionEvent = new GameConditionEvent(
                        gameType, gameType, 0, 0, 0,
                        gameRunInfo.getStake(), gameRunInfo.getAllWinGold(), gameRunInfo.getAllWinTimes(),
                        true, statusBefore == SlotsConst.Status.NORMAL, 0, Map.of(), 0, 0,
                        gameRunInfo.getResultLib() == null || gameRunInfo.getResultLib().getLibTypeSet() == null
                                ? Set.of() : Set.copyOf(gameRunInfo.getResultLib().getLibTypeSet()),
                        List.of(), Map.of());
                ConditionUpdate update = rule.condition().evaluate(conditionEvent);
                room.setSharedProgress(update.apply(room.getSharedProgress()));

                if (room.getSharedProgress() >= rule.modeCount()) {
                    //触发结算: 结果广播(低频)在 settle 内锁内完成, 不再单独发本次 spin (结果已含最终进度)
                    settle(room, true);
                } else if (allQuotaExhausted(room)) {
                    settle(room, false);
                } else {
                    //未结算: 高频 spin 增量广播移出锁, 锁内仅快照接收方 (保证接收集与本次进度原子一致)
                    NotifyCoopSpin notify = new NotifyCoopSpin(Code.SUCCESS);
                    notify.playerId = playerId;
                    notify.hpLeft = member.hpLeft();
                    notify.sharedProgress = room.getSharedProgress();
                    notify.sharedTarget = rule.modeCount();
                    spinNotify = notify;
                    receivers = receivers(room);
                }
            }
        } catch (Exception e) {
            //联动内部吞异常, 不影响旋转主流程
            log.error("协作房间旋转联动异常 playerId={},roomId={}", playerId, roomId, e);
            return;
        }
        if (spinNotify != null) {
            sendAll(receivers, spinNotify);
        }
    }

    /**
     * GM: 给玩家所在进行中房间直接累计共享进度 (联调用, 触发成功结算路径)。
     */
    public int gmAddProgress(long playerId, int count) {
        CoopRoom room = roomOf(playerId);
        if (room == null) {
            return Code.NOT_FOUND;
        }
        synchronized (room) {
            if (room.getStatus() != CoopTaskConst.RoomStatus.RUNNING) {
                return Code.FAIL;
            }
            room.setSharedProgress(room.getSharedProgress() + Math.max(1, count));
            NotifyCoopSpin notify = new NotifyCoopSpin(Code.SUCCESS);
            notify.playerId = playerId;
            CoopMember member = room.getMembers().get(playerId);
            notify.hpLeft = member == null ? 0 : member.hpLeft();
            notify.sharedProgress = room.getSharedProgress();
            notify.sharedTarget = room.getRule().modeCount();
            broadcast(room, notify);
            if (room.getSharedProgress() >= room.getRule().modeCount()) {
                settle(room, true);
            }
        }
        return Code.SUCCESS;
    }

    /**
     * GM: 耗尽玩家所在进行中房间全员血条 (联调用, 触发失败结算路径)。
     */
    public int gmExhaust(long playerId) {
        CoopRoom room = roomOf(playerId);
        if (room == null) {
            return Code.NOT_FOUND;
        }
        synchronized (room) {
            if (room.getStatus() != CoopTaskConst.RoomStatus.RUNNING) {
                return Code.FAIL;
            }
            for (CoopMember member : room.getMembers().values()) {
                member.setSpinUsed(member.getSpinQuota());
            }
            settle(room, false);
        }
        return Code.SUCCESS;
    }

    private boolean allQuotaExhausted(CoopRoom room) {
        for (CoopMember member : room.getMembers().values()) {
            if (member.hpLeft() > 0) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    // 结算 / 生命周期
    // =====================================================================

    /**
     * 结算 (房间锁内调用): 先持久化 FINISHED，再异步回写 sim；只有收到确认后才删除路由记录。
     */
    private void settle(CoopRoom room, boolean success) {
        long now = System.currentTimeMillis();
        room.setStatus(CoopTaskConst.RoomStatus.FINISHED);
        room.setFinishTime(now);
        room.setSuccess(success);

        List<Long> helperIds = new ArrayList<>();
        for (CoopMember member : room.getMembers().values()) {
            if (member.getPlayerId() != room.getOwnerId()) {
                helperIds.add(member.getPlayerId());
            }
            //释放 Redis 占用以允许参与下一局；本地索引保留到玩家退出或房间解散，供结算页继续操作。
            roomRecordDao.releasePlayerRoom(member.getPlayerId(), room.getRoomId());
        }
        room.setSettlementHelperIds(helperIds);
        updateRecord(room);
        requestSettlement(room);

        NotifyCoopRoomResult notify = new NotifyCoopRoomResult(Code.SUCCESS);
        notify.taskId = room.getTaskId();
        notify.success = success;
        notify.sharedProgress = room.getSharedProgress();
        notify.sharedTarget = room.getRule().modeCount();
        notify.dissolveTime = now + SlotsConst.FINISHED_RETAIN_MS;
        broadcast(room, notify);
        log.info("协作房间结算 roomId={},taskId={},success={},progress={}/{},helpers={}",
                room.getRoomId(), room.getTaskId(), success, room.getSharedProgress(),
                room.getRule().modeCount(), helperIds);
    }

    /**
     * 结算回写由 GC 持续重试，直到 sim 明确确认。in-flight 标记避免并发请求放大。
     */
    private void requestSettlement(CoopRoom room) {
        long now = System.currentTimeMillis();
        if (room.isSettlementAcked()) {
            return;
        }
        if (room.isSettlementInFlight()) {
            if (now - room.getLastSettlementAttempt() < SlotsConst.COOP_SETTLE_IN_FLIGHT_TIMEOUT_MS) {
                return;
            }
            log.warn("协作任务结算RPC超时,重新投递 roomId={},ownerId={}", room.getRoomId(), room.getOwnerId());
            room.setSettlementInFlight(false);
        } else if (now - room.getLastSettlementAttempt() < SlotsConst.COOP_SETTLE_RETRY_MS) {
            return;
        }
        room.setSettlementInFlight(true);
        room.setLastSettlementAttempt(now);
        try {
            CoopMember owner = room.getMembers().get(room.getOwnerId());
            String ip = owner != null && owner.getPlayerController() != null
                    ? owner.getPlayerController().ipAddress() : "";
            ClusterClient client = simNodeService.getSimClusterClient(room.getOwnerId(), ip);
            if (client == null) {
                log.error("协作任务结算回写失败, 无可用sim节点 roomId={},ownerId={}", room.getRoomId(), room.getOwnerId());
                room.setSettlementInFlight(false);
                return;
            }
            GameRpcContext rpcContext = GameRpcContext.getContext();
            RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
            try {
                rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                        .addClusterClient(client).setTryMillisPerClient(1000));
                rpcContext.asyncCall(() -> toSimBridge.onCoopRoomSettle(
                                room.getOwnerId(), room.getTaskId(), room.getRoomId(),
                                room.isSuccess(), room.getSettlementHelperIds()))
                        .whenComplete((result, throwable) -> {
                            synchronized (room) {
                                room.setSettlementInFlight(false);
                                if (throwable == null && result != null && result.success()
                                        && Boolean.TRUE.equals(result.data)) {
                                    room.setSettlementAcked(true);
                                    try {
                                        roomRecordDao.delete(room.getRoomId());
                                    } catch (Exception e) {
                                        log.error("删除已确认协作房间记录失败 roomId={}", room.getRoomId(), e);
                                    }
                                } else {
                                    log.warn("协作任务结算回写失败,等待GC重试 roomId={},ownerId={}",
                                            room.getRoomId(), room.getOwnerId(), throwable);
                                }
                            }
                        });
            } finally {
                rpcContext.setReqParameterBuilder(previousBuilder);
            }
        } catch (Exception e) {
            room.setSettlementInFlight(false);
            log.error("协作任务结算回写异常 roomId={},ownerId={}", room.getRoomId(), room.getOwnerId(), e);
        }
    }

    /**
     * 玩家退出 slots 节点钩子: 掉线始终保留成员与座位供重连；主动退出按房间状态处理。
     */
    public int onPlayerExit(long playerId, ExitType exitType) {
        CoopRoom room = roomOf(playerId);
        if (room == null) {
            return Code.SUCCESS;
        }
        synchronized (room) {
            CoopMember member = room.getMembers().get(playerId);
            if (member == null) {
                return Code.SUCCESS;
            }
            if (exitType == ExitType.DROPPED) {
                member.setOnline(false);
                member.setPlayerController(null);
                broadcastUpdate(room, 0);
                return Code.SUCCESS;
            }
            switch (room.getStatus()) {
                case CoopTaskConst.RoomStatus.WAITING, CoopTaskConst.RoomStatus.FINISHED -> {
                    return exit(room, playerId);
                }
                case CoopTaskConst.RoomStatus.RUNNING -> {
                    if (exitType == ExitType.INITIATIVE) {
                        log.info("协作房间进行中,拒绝玩家主动退出 playerId={},roomId={}", playerId, room.getRoomId());
                        return Code.FORBID;
                    }
                    member.setOnline(false);
                    member.setPlayerController(null);
                    broadcastUpdate(room, 0);
                }
                default -> {
                }
            }
        }
        return Code.SUCCESS;
    }

    /**
     * 关服仅允许在节点摘流且所有房间自然结束、回收后执行，禁止人为判负或迁移房间。
     */
    public void shutdown() {
        if (!rooms.isEmpty()) {
            throw new IllegalStateException("协作房间尚未清空，拒绝关闭slots节点 activeRooms=" + rooms.size());
        }
        if (gcExecutor != null) {
            gcExecutor.shutdownNow();
        }
    }

    public int activeRoomCount() {
        return rooms.size();
    }

    @Override
    public long roomIdOf(long playerId) {
        CoopRoom room = roomOf(playerId);
        return room != null && room.getMembers().containsKey(playerId) ? room.getRoomId() : 0;
    }

    @Override
    public void broadcast(ChatMessage message) {
        CoopRoom room = roomOf(message.getFromId());
        if (room == null || room.getRoomId() != message.getChannelSubId()
                || !room.getMembers().containsKey(message.getFromId())) {
            log.warn("协作房间聊天广播失败,发送者不在房间 playerId={},roomId={}",
                    message.getFromId(), message.getChannelSubId());
            return;
        }
        NotifyChat notify = new NotifyChat(Code.SUCCESS);
        notify.msg = SocialPbConverter.toChatMsgInfo(message);
        broadcast(room, notify);
    }

    /**
     * 定时 GC: 进行中超时判负; 等待超时解散; 结算保留期满清理。房间数少, O(n) 扫描廉价。
     */
    private void gcRooms() {
        long now = System.currentTimeMillis();
        for (CoopRoom room : rooms.values()) {
            try {
                synchronized (room) {
                    switch (room.getStatus()) {
                        case CoopTaskConst.RoomStatus.RUNNING -> {
                            if (room.getDeadline() > 0 && now > room.getDeadline()) {
                                log.info("协作房间超时判负 roomId={},taskId={}", room.getRoomId(), room.getTaskId());
                                settle(room, false);
                            }
                        }
                        case CoopTaskConst.RoomStatus.WAITING -> {
                            if (now - room.getCreateTime() > SlotsConst.WAITING_EXPIRE_MS) {
                                log.info("协作房间等待超时解散 roomId={}", room.getRoomId());
                                dissolve(room);
                            }
                        }
                        case CoopTaskConst.RoomStatus.FINISHED -> {
                            if (!room.isSettlementAcked()) {
                                requestSettlement(room);
                            } else if (now - room.getFinishTime() > SlotsConst.FINISHED_RETAIN_MS) {
                                dissolve(room);
                            }
                        }
                        default -> {
                        }
                    }
                }
            } catch (Exception e) {
                log.error("协作房间GC异常 roomId={}", room.getRoomId(), e);
            }
        }
    }

    /**
     * 恢复当前节点在 Redis 中尚未回收的房间。等待房间可继续加入；运行中房间因进度仅在内存，
     * 节点重启后按失败结算；已完成但未收到 sim ACK 的房间恢复结算载荷并继续重投。
     */
    private void recoverRooms() {
        List<CoopRoomRecord> records;
        try {
            records = roomRecordDao.findByNodePath(marsCurator.nodePath);
        } catch (Exception e) {
            log.error("扫描待恢复协作房间失败 nodePath={}", marsCurator.nodePath, e);
            return;
        }
        if (records == null || records.isEmpty()) {
            return;
        }
        for (CoopRoomRecord record : records) {
            try {
                CoopTaskRule rule = coopTaskConfigService.ruleOf(record.getTaskId());
                if (rule == null) {
                    log.error("恢复协作房间失败,任务规则缺失 roomId={},taskId={}",
                            record.getRoomId(), record.getTaskId());
                    continue;
                }
                CoopRoom room = new CoopRoom(record.getRoomId(), record.getTaskId(), record.getOwnerId(),
                        record.getGameType(), record.getRoomCfgId(), rule, record.getCreateTime());
                for (Long memberId : record.getMemberIds()) {
                    if (memberId != null) {
                        room.addMember(memberId, record.getMemberSeats().getOrDefault(memberId, 0));
                    }
                }
                if (record.getStatus() == CoopTaskConst.RoomStatus.WAITING) {
                    room.setStatus(CoopTaskConst.RoomStatus.WAITING);
                    for (CoopMember member : room.getMembers().values()) {
                        if (roomRecordDao.acquirePlayerRoom(member.getPlayerId(), room.getRoomId())) {
                            memberRoomIndex.put(member.getPlayerId(), room.getRoomId());
                        }
                    }
                    rooms.putIfAbsent(room.getRoomId(), room);
                    continue;
                }

                long finishTime = record.getStatus() == CoopTaskConst.RoomStatus.FINISHED
                        && record.getFinishTime() > 0 ? record.getFinishTime() : System.currentTimeMillis();
                boolean success = record.getStatus() == CoopTaskConst.RoomStatus.FINISHED && record.isSuccess();
                List<Long> helperIds = record.getSettlementHelperIds().isEmpty()
                        ? record.getMemberIds().stream().filter(id -> id != null && id != record.getOwnerId()).toList()
                        : record.getSettlementHelperIds();
                room.setStatus(CoopTaskConst.RoomStatus.FINISHED);
                room.setFinishTime(finishTime);
                room.setSuccess(success);
                room.setSharedProgress(record.getSharedProgress());
                room.setSettlementHelperIds(helperIds);
                for (CoopMember member : room.getMembers().values()) {
                    roomRecordDao.releasePlayerRoom(member.getPlayerId(), room.getRoomId());
                }
                rooms.putIfAbsent(room.getRoomId(), room);
                if (record.getStatus() == CoopTaskConst.RoomStatus.RUNNING) {
                    updateRecord(room);
                }
                synchronized (room) {
                    requestSettlement(room);
                }
                log.info("恢复未确认协作房间 roomId={},originalStatus={},success={}",
                        room.getRoomId(), record.getStatus(), success);
            } catch (Exception e) {
                log.error("恢复协作房间异常 roomId={}", record.getRoomId(), e);
            }
        }
    }

    // =====================================================================
    // 内部工具
    // =====================================================================

    /**
     * 懒加载房间: 首个成员到达时从路由记录重建 (仅创建, 成员由进房流程添加)。
     */
    private CoopRoom loadRoom(long roomId) {
        CoopRoom room = rooms.get(roomId);
        if (room != null) {
            return room;
        }
        CoopRoomRecord record = roomRecordDao.get(roomId);
        if (record == null || !marsCurator.nodePath.equals(record.getNodePath())
                || record.getStatus() != CoopTaskConst.RoomStatus.WAITING) {
            return null;
        }
        CoopTaskRule rule = coopTaskConfigService.ruleOf(record.getTaskId());
        if (rule == null) {
            log.error("协作房间任务规则解析失败 roomId={},taskId={}", roomId, record.getTaskId());
            return null;
        }
        return rooms.computeIfAbsent(roomId, k -> {
            CoopRoom created = new CoopRoom(record.getRoomId(), record.getTaskId(),
                    record.getOwnerId(), record.getGameType(), record.getRoomCfgId(), rule);
            for (Long memberId : record.getMemberIds()) {
                if (memberId != null && roomRecordDao.acquirePlayerRoom(memberId, roomId)) {
                    created.addMember(memberId, record.getMemberSeats().getOrDefault(memberId, 0));
                    memberRoomIndex.put(memberId, roomId);
                }
            }
            return created;
        });
    }

    private CoopRoom roomOf(long playerId) {
        Long roomId = memberRoomIndex.get(playerId);
        return roomId == null ? null : rooms.get(roomId);
    }

    private void removeRoom(CoopRoom room) {
        rooms.remove(room.getRoomId());
        for (CoopMember member : room.getMembers().values()) {
            memberRoomIndex.remove(member.getPlayerId(), room.getRoomId());
            roomRecordDao.releasePlayerRoom(member.getPlayerId(), room.getRoomId());
        }
        try {
            roomRecordDao.delete(room.getRoomId());
        } catch (Exception e) {
            log.error("删除协作房间记录失败 roomId={}", room.getRoomId(), e);
        }
    }

    /**
     * 覆写路由记录 (成员/状态变更跟随, 供 hall 预检查与自愈判断)。
     */
    private void updateRecord(CoopRoom room) {
        try {
            if (room.getStatus() != CoopTaskConst.RoomStatus.FINISHED) {
                for (CoopMember member : room.getMembers().values()) {
                    if (!roomRecordDao.acquirePlayerRoom(member.getPlayerId(), room.getRoomId())) {
                        log.error("刷新协作房间成员租约失败 playerId={},roomId={}",
                                member.getPlayerId(), room.getRoomId());
                    }
                }
            }
            CoopRoomRecord record = new CoopRoomRecord();
            record.setRoomId(room.getRoomId());
            record.setTaskId(room.getTaskId());
            record.setOwnerId(room.getOwnerId());
            record.setGameType(room.getGameType());
            record.setRoomCfgId(room.getRoomCfgId());
            record.setNodePath(marsCurator.nodePath);
            record.setStatus(room.getStatus());
            List<CoopMember> members = room.membersBySeat();
            List<Long> memberIds = new ArrayList<>(members.size());
            Map<Long, Integer> memberSeats = new LinkedHashMap<>();
            for (CoopMember member : members) {
                memberIds.add(member.getPlayerId());
                memberSeats.put(member.getPlayerId(), member.getSeat());
            }
            record.setMemberIds(memberIds);
            record.setMemberSeats(memberSeats);
            record.setMaxMembers(room.getRule().maxMembers());
            record.setCreateTime(room.getCreateTime());
            record.setSuccess(room.isSuccess());
            record.setFinishTime(room.getFinishTime());
            record.setSharedProgress(room.getSharedProgress());
            record.setSettlementHelperIds(room.getSettlementHelperIds());
            roomRecordDao.save(record);
        } catch (Exception e) {
            //记录仅影响 hall 预检查, 失败不阻断房间流程
            log.error("覆写协作房间记录失败 roomId={}", room.getRoomId(), e);
        }
    }

    private CoopRoomSnapshot buildSnapshot(CoopRoom room) {
        CoopRoomSnapshot snapshot = new CoopRoomSnapshot();
        snapshot.roomId = room.getRoomId();
        snapshot.taskId = room.getTaskId();
        snapshot.ownerId = room.getOwnerId();
        snapshot.gameType = room.getGameType();
        snapshot.status = room.getStatus();
        snapshot.maxMembers = room.getRule().maxMembers();
        snapshot.sharedTarget = room.getRule().modeCount();
        snapshot.sharedProgress = room.getSharedProgress();
        snapshot.deadline = room.getDeadline();
        List<CoopMemberInfo> members = new ArrayList<>(room.getMembers().size());
        for (CoopMember member : room.membersBySeat()) {
            CoopMemberInfo info = new CoopMemberInfo();
            info.playerId = member.getPlayerId();
            info.seat = member.getSeat();
            info.name = member.getName();
            info.headImgId = member.getHeadImgId();
            info.headFrameId = member.getHeadFrameId();
            info.owner = member.getPlayerId() == room.getOwnerId();
            info.ready = info.owner || member.isReady();
            info.online = member.isOnline();
            info.spinQuota = member.getSpinQuota();
            info.hpLeft = member.hpLeft();
            info.combatPower = member.getCombatPower();
            members.add(info);
        }
        snapshot.members = members;
        return snapshot;
    }

    private int fetchCombatPower(long playerId, String ipAddress) {
        try {
            ClusterClient client = simNodeService.getSimClusterClient(playerId, ipAddress);
            if (client == null) {
                log.warn("获取玩家战力失败,无可用sim节点 playerId={}", playerId);
                return 0;
            }
            GameRpcContext rpcContext = GameRpcContext.getContext();
            RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
            try {
                rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                        .addClusterClient(client).setTryMillisPerClient(1000));
                return toSimBridge.getCombatPower(playerId);
            } finally {
                rpcContext.setReqParameterBuilder(previousBuilder);
            }
        } catch (Exception e) {
            log.warn("获取玩家战力异常 playerId={}", playerId, e);
            return 0;
        }
    }

    /**
     * 全量快照广播 (低频变更; skipPlayerId 用于进房者已从 res 获取快照时跳过)
     */
    private void broadcastUpdate(CoopRoom room, long skipPlayerId) {
        NotifyCoopRoomUpdate notify = new NotifyCoopRoomUpdate(Code.SUCCESS);
        notify.room = buildSnapshot(room);
        for (CoopMember member : room.getMembers().values()) {
            if (member.getPlayerId() == skipPlayerId) {
                continue;
            }
            send(member, notify);
        }
    }

    private void broadcast(CoopRoom room, Object msg) {
        for (CoopMember member : room.getMembers().values()) {
            send(member, msg);
        }
    }

    private void notifyRemoved(CoopRoom room, CoopMember member) {
        NotifyCoopRoomUpdate notify = new NotifyCoopRoomUpdate(Code.SUCCESS);
        notify.removed = true;
        notify.room = buildSnapshot(room);
        send(member, notify);
    }

    private void send(CoopMember member, Object msg) {
        PlayerController pc = member.getPlayerController();
        if (pc == null) {
            return;
        }
        try {
            pc.send(msg);
        } catch (Exception e) {
            log.warn("协作房间消息下发失败 playerId={}", member.getPlayerId(), e);
        }
    }

    /**
     * 锁内快照当前在线成员的会话引用 (锁内快照保证接收集与本次状态变更原子一致)。
     */
    private List<PlayerController> receivers(CoopRoom room) {
        List<PlayerController> list = new ArrayList<>(room.getMembers().size());
        for (CoopMember member : room.getMembers().values()) {
            PlayerController pc = member.getPlayerController();
            if (pc != null) {
                list.add(pc);
            }
        }
        return list;
    }

    /**
     * 锁外批量下发 (把高频广播的实际网络 IO 移出房间锁)。
     */
    private void sendAll(List<PlayerController> receivers, Object msg) {
        if (receivers == null || receivers.isEmpty()) {
            return;
        }
        PFMessage pfMessage;
        try {
            pfMessage = MessageUtil.getPFMessage(msg);
        } catch (Exception e) {
            log.warn("协作房间批量消息序列化失败 receivers={}", receivers.size(), e);
            return;
        }
        for (PlayerController pc : receivers) {
            try {
                pc.send(pfMessage);
            } catch (Exception e) {
                log.warn("协作房间消息下发失败 playerId={}", pc.playerId(), e);
            }
        }
    }
}
