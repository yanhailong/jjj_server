package com.jjg.game.alliance.service;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AllianceIdDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceApplication;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AllianceMember;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.pb.AlliancePbConverter;
import com.jjg.game.alliance.pb.res.*;
import com.jjg.game.alliance.pb.struct.AllianceApplicationInfo;
import com.jjg.game.alliance.pb.struct.AllianceBrief;
import com.jjg.game.alliance.pb.struct.AllianceMemberInfo;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.social.channel.AllianceChatChannel;
import com.jjg.game.social.service.SocialStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 联盟核心域服务: 创建/加入/申请/审批/退出/踢人/转让/解散/编辑/列表查询。
 * <p>
 * 跨"玩家文档 + 联盟文档"的双写一律按 "先玩家占位 -> 再联盟写入 -> 失败回滚占位" 的顺序,
 * 两边都是条件原子更新, 不需要分布式事务; 任一中间态都能被读路径自愈
 * (见 {@link #allianceInfo}: 玩家指向的联盟不存在时自动清理占位)。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceService {
    private static final Logger log = LoggerFactory.getLogger(AllianceService.class);

    //一键申请单次扫描的候选联盟数
    private static final int ONE_KEY_SCAN_LIMIT = 50;

    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private AllianceConfigService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private AllianceRankService rankService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SocialStatusService statusService;
    @Autowired
    private SimManager simManager;
    @Autowired
    private AllianceChatChannel allianceChatChannel;
    @Autowired
    private AllianceIdDao allianceIdDao;

    // =====================================================================
    // 信息查询
    // =====================================================================

    /**
     * 联盟主界面信息。玩家指向的联盟已不存在(解散兜底)时自愈清理占位。
     */
    public ResAllianceInfo allianceInfo(long playerId) {
        ResAllianceInfo res = new ResAllianceInfo(Code.SUCCESS);
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        res.myContribution = playerData.getContribution();
        long aid = playerData.getAllianceId();
        if (aid <= 0) {
            return res;
        }
        AllianceData alliance = cacheService.getAlliance(aid);
        if (alliance == null) {
            //联盟已解散但玩家文档未被批量清理到 (并发窗口): 自愈
            alliancePlayerDao.clearAlliance(playerId, aid);
            cacheService.invalidatePlayer(playerId);
            return res;
        }
        res.myAllianceId = aid;
        res.alliance = AlliancePbConverter.toBrief(alliance, configService);
        res.myPosition = alliance.isLeader(playerId) ? AllianceConst.Position.LEADER : AllianceConst.Position.MEMBER;
        return res;
    }

    /**
     * 按 ID 搜索联盟。
     */
    public ResSearchAlliance search(long allianceId) {
        ResSearchAlliance res = new ResSearchAlliance(Code.SUCCESS);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        res.alliance = AlliancePbConverter.toBrief(alliance, configService);
        return res;
    }

    /**
     * 可加入联盟列表: 按声誉降序, 只含满足"我的场景等级 >= 门槛"且未满员的联盟。
     */
    public ResAllianceList allianceList(long playerId) {
        ResAllianceList res = new ResAllianceList(Code.SUCCESS);
        res.list = new ArrayList<>();
        int myCasinoLevel = casinoLevelOf(playerId);
        for (AllianceData data : allianceDao.listByReputation(ONE_KEY_SCAN_LIMIT)) {
            if (data.getJoinMinCasinoLevel() > myCasinoLevel) {
                continue;
            }
            if (data.getMemberCount() >= configService.memberCap(data.getLevel())) {
                continue;
            }
            res.list.add(AlliancePbConverter.toBrief(data, configService));
        }
        return res;
    }

    /**
     * 成员列表: 批量取 Player 展示信息 + 会话状态 + 玩家侧贡献度, 盟主排首位。
     */
    public ResMemberList memberList(long playerId) {
        ResMemberList res = new ResMemberList(Code.SUCCESS);
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        res.memberCap = configService.memberCap(alliance.getLevel());
        res.list = new ArrayList<>(alliance.getMemberCount());

        List<Long> memberIds = new ArrayList<>(alliance.getMembers().keySet());
        Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(memberIds);
        Map<Long, PlayerSessionInfo> sessionMap = statusService.infosOf(memberIds);
        Map<Long, AlliancePlayerData> allianceDataMap = alliancePlayerDao.multiGet(memberIds);

        for (Map.Entry<Long, AllianceMember> en : alliance.getMembers().entrySet()) {
            long pid = en.getKey();
            Player player = playerMap.get(pid);
            if (player == null) {
                continue;
            }
            AllianceMemberInfo info = new AllianceMemberInfo();
            info.playerId = pid;
            info.nick = player.getNickName();
            info.headImg = player.getHeadImgId();
            info.headFrame = player.getHeadFrameId();
            info.level = player.getLevel();
            info.position = alliance.isLeader(pid) ? AllianceConst.Position.LEADER : AllianceConst.Position.MEMBER;
            info.status = statusService.statusOf(sessionMap.get(pid));
            info.joinTime = en.getValue().getJoinTime();
            AlliancePlayerData apd = allianceDataMap.get(pid);
            info.contributionTotal = apd == null ? 0 : apd.getContributionTotal();
            res.list.add(info);
        }
        //盟主在前, 其余按 在线状态降序 > 贡献度降序
        res.list.sort((a, b) -> {
            if (a.position != b.position) {
                return Integer.compare(a.position, b.position);
            }
            if (a.status != b.status) {
                return Integer.compare(b.status, a.status);
            }
            return Long.compare(b.contributionTotal, a.contributionTotal);
        });
        return res;
    }

    /**
     * 入盟申请列表 (盟主); 顺带惰性清理过期申请。
     */
    public ResApplicationList applicationList(long playerId) {
        ResApplicationList res = new ResApplicationList(Code.SUCCESS);
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_NOT_LEADER;
            return res;
        }
        res.list = new ArrayList<>();
        if (alliance.getApplications().isEmpty()) {
            return res;
        }
        //过期申请惰性清理
        long now = System.currentTimeMillis();
        List<Long> expired = new ArrayList<>();
        List<Long> validIds = new ArrayList<>();
        for (Map.Entry<Long, AllianceApplication> en : alliance.getApplications().entrySet()) {
            if (now - en.getValue().getApplyTime() > AllianceConst.Cfg.APPLICATION_VALID_MILLS) {
                expired.add(en.getKey());
            } else {
                validIds.add(en.getKey());
            }
        }
        if (!expired.isEmpty()) {
            allianceDao.removeApplications(alliance.getAllianceId(), expired);
            cacheService.publishInvalidate(alliance.getAllianceId());
        }

        Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(validIds);
        for (Long pid : validIds) {
            AllianceApplication app = alliance.getApplications().get(pid);
            Player player = playerMap.get(pid);
            if (app == null || player == null) {
                continue;
            }
            AllianceApplicationInfo info = new AllianceApplicationInfo();
            info.playerId = pid;
            info.nick = player.getNickName();
            info.headImg = player.getHeadImgId();
            info.casinoLevel = app.getCasinoLevel();
            info.applyTime = app.getApplyTime();
            res.list.add(info);
        }
        res.list.sort((a, b) -> Long.compare(a.applyTime, b.applyTime));
        return res;
    }

    // =====================================================================
    // 创建
    // =====================================================================

    /**
     * 创建联盟: 资格预检 -> 扣钻石 -> 占位玩家侧归属 -> 建档, 任一步失败回滚之前的步骤。
     * <p>
     * 扣钻石刻意放在占位之前: 钻石不足是常见失败, 此时尚未占位, 无副作用;
     * 反之若先占位再扣钻, 回滚的 {@link AlliancePlayerDao#clearAlliance} 只清 allianceId 不清
     * createdAllianceId, 会给"已创建过"留下脏标记 (联盟没建成却吃掉了创建资格)。
     */
    public ResCreateAlliance create(PlayerController pc, String name, int icon, String notice,
                                    int joinMinCasinoLevel, boolean joinNeedAudit) {
        ResCreateAlliance res = new ResCreateAlliance(Code.SUCCESS);
        long playerId = pc.playerId();

        int code = validateSettings(name, notice, joinMinCasinoLevel);
        if (code != Code.SUCCESS) {
            res.code = code;
            log.warn("创建联盟失败,校验设置失败 playerId={},code={}", playerId, code);
            return res;
        }

        int allLevel = casinoLevelOf(playerId);
        if (allLevel < AllianceConst.Cfg.CREATE_MIN_CASINO_LEVEL) {
            res.code = Code.LEVEL_NOT_ENOUGH;
            log.warn("创建联盟失败,场景等级不足 playerId={},allLevel={}", playerId, allLevel);
            return res;
        }

        //资格预检: 已在盟 / 已创建过 (需求: 每人最多加入1个、最多创建1个) —— 快速失败, 不取号不扣钻
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        if (playerData.inAlliance()) {
            res.code = Code.ALLIANCE_ALREADY_IN;
            log.warn("创建联盟失败,玩家已有所属联盟 playerId={},allianceId={}", playerId, playerData.getAllianceId());
            return res;
        }
        if (playerData.getCreatedAllianceId() > 0) {
            res.code = Code.ALLIANCE_CREATED_LIMIT;
            log.warn("创建联盟失败,玩家已创建过联盟 playerId={},createdAllianceId={}", playerId, playerData.getCreatedAllianceId());
            return res;
        }

        //扣除资源
        Map<Integer, Long> cost = Map.of(ItemUtils.getDiamondItemId(), (long) AllianceConst.Cfg.CREATE_COST_DIAMOND);
        CommonResult<ItemOperationResult> deduct = playerPackService.removeItems(pc.getPlayer(), cost, AddType.ALLIANCE_CREATE, "");
        if (!deduct.success()) {
            res.code = Code.NOT_ENOUGH_ITEM;
            log.warn("创建联盟失败,余额不足 playerId={},allLevel={}", playerId, allLevel);
            return res;
        }

        //获取联盟id
        long allianceId = allianceIdDao.nextAllianceId();
        long now = System.currentTimeMillis();

        //占位玩家侧归属: 原子写入"当前联盟 + 已创建联盟"(条件: 未在盟 且 未创建过), 兜底跨节点并发
        if (!alliancePlayerDao.tryOccupyCreate(playerId, allianceId, now)) {
            //同一玩家请求按 playerId 串行, 预检已过, 正常不触发; 极端并发下退还钻石
            playerPackService.addItems(playerId, cost, AddType.ALLIANCE_CREATE, "创建联盟回滚", true);
            res.code = Code.ALLIANCE_ALREADY_IN;
            log.warn("创建联盟失败,占位失败已退钻石 playerId={},allianceId={}", playerId, allianceId);
            return res;
        }

        //建档
        AllianceData alliance = new AllianceData();
        alliance.setAllianceId(allianceId);
        alliance.setName(name);
        alliance.setIcon(icon);
        alliance.setNotice(notice == null ? "" : notice);
        alliance.setLeaderId(playerId);
        alliance.setCreatorId(playerId);
        alliance.setCreateTime(now);
        alliance.setJoinMinCasinoLevel(joinMinCasinoLevel);
        alliance.setJoinNeedAudit(joinNeedAudit);
        alliance.setMemberCount(1);
        alliance.getMembers().put(playerId, new AllianceMember(AllianceConst.Position.LEADER, now));
        allianceDao.save(alliance);
        cacheService.invalidatePlayer(playerId);
        res.alliance = AlliancePbConverter.toBrief(alliance, configService);
        log.info("创建联盟成功 playerId={},allianceId={},name={}", playerId, allianceId, name);
        return res;
    }

    // =====================================================================
    // 加入 / 申请 / 审批
    // =====================================================================

    /**
     * 加入联盟。allianceId > 0 指定联盟; allianceId == 0 一键申请。
     */
    public ResJoinAlliance join(long playerId, long allianceId) {
        ResJoinAlliance res = new ResJoinAlliance(Code.SUCCESS);
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        if (playerData.inAlliance()) {
            res.code = Code.ALLIANCE_ALREADY_IN;
            log.warn("加入联盟失败,玩家已有所属联盟 playerId={},oldAllianceId={}", playerId, playerData.getAllianceId());
            return res;
        }
        int myCasinoLevel = casinoLevelOf(playerId);
        if (myCasinoLevel < AllianceConst.Cfg.CREATE_MIN_CASINO_LEVEL) {
            res.code = Code.LEVEL_NOT_ENOUGH;
            log.warn("加入联盟失败,玩家场景等级不足 playerId={},allLevel={}", playerId, myCasinoLevel);
            return res;
        }
        if (allianceId > 0) {
            return joinOne(res, playerId, allianceId, myCasinoLevel);
        }
        return oneKeyJoin(res, playerId, myCasinoLevel);
    }

    /**
     * 指定联盟: 免审核直接加入, 需审核提交申请。
     */
    private ResJoinAlliance joinOne(ResJoinAlliance res, long playerId, long allianceId, int myCasinoLevel) {
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("加入单个联盟失败,未找到该联盟数据 playerId={},allianceId={}", playerId, allianceId);
            return res;
        }
        if (alliance.getJoinMinCasinoLevel() > myCasinoLevel) {
            res.code = Code.LEVEL_NOT_ENOUGH;
            log.warn("加入单个联盟失败,不满足该联盟的入门等级要求 playerId={},myCasinoLevel={},joinMinLevel={}", playerId, myCasinoLevel, alliance.getJoinMinCasinoLevel());
            return res;
        }
        if (alliance.getMemberCount() >= configService.memberCap(alliance.getLevel())) {
            res.code = Code.ALLIANCE_FULL;
            log.warn("加入单个联盟失败,该联盟人数已满 playerId={},allianceId={}", playerId, allianceId);
            return res;
        }
        if (!alliance.isJoinNeedAudit()) {
            int code = directJoin(playerId, alliance);
            res.code = code;
            if (code == Code.SUCCESS) {
                res.result = 1;
                res.alliance = AlliancePbConverter.toBrief(cacheService.getAlliance(allianceId), configService);
            }
            return res;
        }
        //需审核: 提交申请
        if (alliance.getApplications().size() >= AllianceConst.Cfg.APPLICATION_LIMIT) {
            //申请列表满: 挤掉最早的一条 (宽松上限, 防无界膨胀)
            long earliest = 0;
            long earliestTime = Long.MAX_VALUE;
            for (Map.Entry<Long, AllianceApplication> en : alliance.getApplications().entrySet()) {
                if (en.getValue().getApplyTime() < earliestTime) {
                    earliestTime = en.getValue().getApplyTime();
                    earliest = en.getKey();
                }
            }
            if (earliest > 0) {
                allianceDao.removeApplications(allianceId, List.of(earliest));
            }
        }
        if (!allianceDao.addApplication(allianceId, playerId,
                new AllianceApplication(System.currentTimeMillis(), myCasinoLevel))) {
            res.code = Code.ALLIANCE_APPLY_EXIST;
            return res;
        }
        cacheService.publishInvalidate(allianceId);
        //通知盟主有新申请
        assetService.notifyPlayer(alliance.getLeaderId(), AllianceConst.NotifyType.NEW_APPLICATION,
                allianceId, String.valueOf(playerId));
        res.result = 2;
        return res;
    }

    /**
     * 一键申请: 免审核的直接加入第一个成功的; 否则向全部满足条件的需审核联盟发申请。
     */
    private ResJoinAlliance oneKeyJoin(ResJoinAlliance res, long playerId, int myCasinoLevel) {
        List<AllianceData> candidates = new ArrayList<>();
        for (AllianceData data : allianceDao.listByReputation(ONE_KEY_SCAN_LIMIT)) {
            if (data.getJoinMinCasinoLevel() > myCasinoLevel) {
                continue;
            }
            if (data.getMemberCount() >= configService.memberCap(data.getLevel())) {
                continue;
            }
            candidates.add(data);
        }
        if (candidates.isEmpty()) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        //优先尝试免审核联盟 (按声誉从高到低)
        for (AllianceData data : candidates) {
            if (data.isJoinNeedAudit()) {
                continue;
            }
            if (directJoin(playerId, data) == Code.SUCCESS) {
                res.result = 1;
                res.alliance = AlliancePbConverter.toBrief(cacheService.getAlliance(data.getAllianceId()), configService);
                return res;
            }
        }
        //无可直接加入的: 向需审核的联盟批量发申请
        long now = System.currentTimeMillis();
        int applied = 0;
        for (AllianceData data : candidates) {
            if (!data.isJoinNeedAudit()) {
                continue;
            }
            if (allianceDao.addApplication(data.getAllianceId(), playerId, new AllianceApplication(now, myCasinoLevel))) {
                cacheService.publishInvalidate(data.getAllianceId());
                assetService.notifyPlayer(data.getLeaderId(), AllianceConst.NotifyType.NEW_APPLICATION,
                        data.getAllianceId(), String.valueOf(playerId));
                applied++;
            }
        }
        if (applied == 0) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        res.result = 2;
        return res;
    }

    /**
     * 直接加入: 玩家占位 -> 联盟条件写入 -> 失败回滚。
     */
    private int directJoin(long playerId, AllianceData alliance) {
        long allianceId = alliance.getAllianceId();
        long now = System.currentTimeMillis();
        if (!alliancePlayerDao.tryOccupy(playerId, allianceId, now)) {
            return Code.ALLIANCE_ALREADY_IN;
        }
        int cap = configService.memberCap(alliance.getLevel());
        if (!allianceDao.tryAddMember(allianceId, playerId,
                new AllianceMember(AllianceConst.Position.MEMBER, now), cap)) {
            //满员/已解散/已在盟中: 回滚占位
            alliancePlayerDao.clearAlliance(playerId, allianceId);
            cacheService.invalidatePlayer(playerId);
            return Code.ALLIANCE_FULL;
        }
        cacheService.invalidatePlayer(playerId);
        cacheService.publishInvalidate(allianceId);
        log.info("加入联盟成功 playerId={},allianceId={}", playerId, allianceId);
        return Code.SUCCESS;
    }

    /**
     * 处理入盟申请 (盟主, 支持一键): 同意走 directJoin, 失败者(已入他盟/已满)只删申请。
     */
    public ResHandleApplication handleApplications(long playerId, List<Long> applicantIds, boolean agree) {
        ResHandleApplication res = new ResHandleApplication(Code.SUCCESS);
        res.agreedIds = new ArrayList<>();
        res.failedIds = new ArrayList<>();
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_NOT_LEADER;
            return res;
        }
        if (applicantIds == null || applicantIds.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        long allianceId = alliance.getAllianceId();
        List<Long> toRemove = new ArrayList<>();
        for (Long pid : applicantIds) {
            if (pid == null || !alliance.getApplications().containsKey(pid)) {
                continue;
            }
            if (!agree) {
                toRemove.add(pid);
                assetService.notifyPlayer(pid, AllianceConst.NotifyType.APPLY_REJECTED, allianceId, "");
                continue;
            }
            //同意: directJoin 内部的 tryAddMember 会顺带 unset 申请
            int code = directJoin(pid, alliance);
            if (code == Code.SUCCESS) {
                res.agreedIds.add(pid);
                assetService.notifyPlayer(pid, AllianceConst.NotifyType.APPLY_AGREED, allianceId, "");
                //刷新缓存里的人数, 供后续循环的 cap 预检
                AllianceData fresh = cacheService.getAlliance(allianceId);
                if (fresh != null) {
                    alliance = fresh;
                }
            } else {
                res.failedIds.add(pid);
                toRemove.add(pid);
            }
        }
        if (!toRemove.isEmpty()) {
            allianceDao.removeApplications(allianceId, toRemove);
        }
        cacheService.publishInvalidate(allianceId);
        return res;
    }

    // =====================================================================
    // 退出 / 踢人 / 转让 / 解散 / 编辑
    // =====================================================================

    /**
     * 退出联盟 (盟主须先转让)。贡献值保留 (需求), 仅盟内周榜移除。
     */
    public ResQuitAlliance quit(long playerId) {
        ResQuitAlliance res = new ResQuitAlliance(Code.SUCCESS);
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        if (alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_LEADER_CANT_QUIT;
            return res;
        }
        leaveInternal(playerId, alliance.getAllianceId());
        log.info("退出联盟 playerId={},allianceId={}", playerId, alliance.getAllianceId());
        return res;
    }

    /**
     * 踢出成员 (盟主)。
     */
    public ResKickMember kick(long playerId, long targetId) {
        ResKickMember res = new ResKickMember(Code.SUCCESS);
        res.playerId = targetId;
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_NOT_LEADER;
            return res;
        }
        if (targetId == playerId || !alliance.isMember(targetId)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        leaveInternal(targetId, alliance.getAllianceId());
        assetService.notifyPlayer(targetId, AllianceConst.NotifyType.KICKED, alliance.getAllianceId(), "");
        log.info("踢出联盟成员 operator={},target={},allianceId={}", playerId, targetId, alliance.getAllianceId());
        return res;
    }

    /**
     * 退出/被踢共用: 联盟文档移除 -> 玩家文档清占位 -> 缓存失效 -> 周榜移除。
     */
    private void leaveInternal(long playerId, long allianceId) {
        allianceDao.removeMember(allianceId, playerId);
        alliancePlayerDao.clearAlliance(playerId, allianceId);
        cacheService.invalidatePlayer(playerId);
        cacheService.publishInvalidate(allianceId);
        rankService.removeFromContribRank(allianceId, playerId);
    }

    /**
     * 转让盟主。
     */
    public ResTransferLeader transferLeader(long playerId, long targetId) {
        ResTransferLeader res = new ResTransferLeader(Code.SUCCESS);
        res.newLeaderId = targetId;
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_NOT_LEADER;
            return res;
        }
        if (targetId == playerId || !alliance.isMember(targetId)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (!allianceDao.transferLeader(alliance.getAllianceId(), playerId, targetId)) {
            res.code = Code.FAIL;
            return res;
        }
        cacheService.publishInvalidate(alliance.getAllianceId());
        assetService.notifyPlayer(targetId, AllianceConst.NotifyType.BECOME_LEADER, alliance.getAllianceId(), "");
        log.info("转让盟主 from={},to={},allianceId={}", playerId, targetId, alliance.getAllianceId());
        return res;
    }

    /**
     * 解散联盟 (盟主): 删文档 -> 批量清成员占位 -> 榜单/聊天缓存清理 -> 全员通知。
     * 成员贡献值保留 (需求: 解散后贡献值随玩家, 入新盟继续用)。
     */
    public ResDissolveAlliance dissolve(long playerId) {
        ResDissolveAlliance res = new ResDissolveAlliance(Code.SUCCESS);
        //解散要拿全量成员清单, 直读 DB 避免缓存视图缺人
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        long allianceId = playerData.getAllianceId();
        if (allianceId <= 0) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        AllianceData alliance = allianceDao.findById(allianceId).orElse(null);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_NOT_LEADER;
            return res;
        }
        //先广播再删 (删后拿不到成员清单)
        assetService.broadcastToAlliance(allianceId, AllianceConst.NotifyType.DISSOLVED, "");
        if (!allianceDao.dissolve(allianceId, playerId)) {
            res.code = Code.FAIL;
            return res;
        }
        List<Long> memberIds = new ArrayList<>(alliance.getMembers().keySet());
        alliancePlayerDao.clearAllianceBulk(memberIds, allianceId);
        cacheService.invalidatePlayers(memberIds);
        cacheService.publishInvalidate(allianceId);
        //榜单清理 (声誉总榜/赛季榜移除, 周榜删 key); 对决积分按需求冻结不动, 结算时联盟已不存在则不发奖
        rankService.removeAllianceFromRanks(allianceId);
        //联盟聊天缓存清理 (social 预留的接口)
        allianceChatChannel.clearCache(allianceId);
        log.info("解散联盟 allianceId={},leaderId={},members={}", allianceId, playerId, memberIds.size());
        return res;
    }

    /**
     * 编辑联盟信息 (盟主)。
     */
    public ResEditAlliance edit(long playerId, String name, int icon, String notice,
                                int joinMinCasinoLevel, boolean joinNeedAudit) {
        ResEditAlliance res = new ResEditAlliance(Code.SUCCESS);
        AllianceData alliance = allianceOf(playerId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        if (!alliance.isLeader(playerId)) {
            res.code = Code.ALLIANCE_NOT_LEADER;
            return res;
        }
        int code = validateSettings(name, notice, joinMinCasinoLevel);
        if (code != Code.SUCCESS) {
            res.code = code;
            return res;
        }
        if (!allianceDao.updateSettings(alliance.getAllianceId(), playerId, name, icon,
                notice == null ? "" : notice, joinMinCasinoLevel, joinNeedAudit)) {
            res.code = Code.FAIL;
            return res;
        }
        cacheService.publishInvalidate(alliance.getAllianceId());
        res.alliance = AlliancePbConverter.toBrief(cacheService.getAlliance(alliance.getAllianceId()), configService);
        return res;
    }

    // =====================================================================
    // 生命周期联动
    // =====================================================================

    /**
     * 玩家登录: 刷新成员活跃时间 (对决"活跃人数"报名模式统计依据)。由 AllianceManager 挂登录监听调用。
     */
    public void onPlayerLogin(long playerId) {
        long allianceId = cacheService.getAllianceId(playerId);
        if (allianceId <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        //节流: 据缓存视图判断距上次活跃记录是否超过阈值, 未超过则跳过写入,
        //避免登录潮下同盟成员并发写同一共享联盟文档 (缓存30s滞后远小于阈值, 不影响判定)
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance != null) {
            AllianceMember member = alliance.getMembers().get(playerId);
            if (member != null && now - member.getLastActiveTime() < AllianceConst.Cfg.ACTIVE_TOUCH_THROTTLE_MILLS) {
                return;
            }
        }
        allianceDao.touchMemberActive(allianceId, playerId, now);
    }

    // =====================================================================
    // 内部工具
    // =====================================================================

    /**
     * 取玩家所在联盟 (缓存); 无盟返回 null。
     */
    public AllianceData allianceOf(long playerId) {
        long allianceId = cacheService.getAllianceId(playerId);
        return allianceId <= 0 ? null : cacheService.getAlliance(allianceId);
    }

    /**
     * 玩家场景等级: 在线取 ctx 当前场景, 离线回源场景文档取最高等级。
     */
    public int casinoLevelOf(long playerId) {
        SimPlayerContext ctx = simManager.getContext(playerId);
        if (ctx == null) {
            return 0;
        }
        return ctx.getSimBaseData().getAllLevel();
    }

    /**
     * 名称/公告/入盟等级合法性校验。
     */
    private int validateSettings(String name, String notice, int joinMinCasinoLevel) {
        if (name == null || name.isBlank() || name.length() > AllianceConst.Cfg.NAME_MAX_LEN) {
            return Code.ILLEGAL_NAME;
        }
        if (notice != null && notice.length() > AllianceConst.Cfg.NOTICE_MAX_LEN) {
            return Code.PARAM_ERROR;
        }
        if (joinMinCasinoLevel < AllianceConst.Cfg.JOIN_LEVEL_MIN || joinMinCasinoLevel > AllianceConst.Cfg.JOIN_LEVEL_MAX) {
            return Code.PARAM_ERROR;
        }
        return Code.SUCCESS;
    }

    public List<AllianceBrief> toBriefs(List<AllianceData> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<AllianceBrief> briefs = new ArrayList<>(list.size());
        for (AllianceData data : list) {
            briefs.add(AlliancePbConverter.toBrief(data, configService));
        }
        return briefs;
    }
}
