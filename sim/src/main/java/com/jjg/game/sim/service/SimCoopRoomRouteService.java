package com.jjg.game.sim.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.dao.CoopRoomRecordDao;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.CoopRoomRecord;
import com.jjg.game.sim.data.CoopTaskRule;
import com.jjg.game.sim.data.SimCoopTaskEntry;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.res.ResCreateCoopRoom;
import com.jjg.game.sim.pb.res.ResJoinCoopRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 协作房间路由 (hall 侧): 创建/加入时校验 + 写路由记录 + 把会话切到房间所在 slots 节点。
 * <p>
 * 一个房间一个 gameType, 全体成员被路由到同一 slots 节点, 房间态在该节点内存单点聚合,
 * 从而避免跨节点分布式状态同步 (范式对齐好友房 {@code HallRoomService.joinFriendRoom})。
 * hall 侧对记录的满员/状态检查仅为廉价预检查, 权威判定在 slots 进房时完成。
 *
 * @author 11
 * @date 2026/7/6
 */
@Service
public class SimCoopRoomRouteService {
    private static final Logger log = LoggerFactory.getLogger(SimCoopRoomRouteService.class);

    @Autowired
    private CoopTaskConfigService configService;
    @Autowired
    private CoopRoomRecordDao roomRecordDao;
    @Autowired
    private SimCoopTaskService coopTaskService;
    @Autowired
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SnowflakeManager snowflakeManager;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private MarsCurator marsCurator;

    /**
     * 发起者创建房间: 校验任务态/游戏合法性 -> 选 slots 节点 -> 写路由记录 -> 切节点。
     */
    public ResCreateCoopRoom createRoom(SimPlayerContext ctx, int taskId, int gameType, int roomCfgId) {
        ResCreateCoopRoom res = new ResCreateCoopRoom(Code.FAIL);
        res.taskId = taskId;
        res.gameType = gameType;
        long playerId = ctx.playerId();

        SimCoopTaskEntry entry = ctx.getSimCoopTaskData() == null
                ? null : ctx.getSimCoopTaskData().getTasks().get(taskId);
        if (entry == null || entry.getStatus() != CoopTaskConst.TaskStatus.CLAIMED) {
            log.warn("创建协作房间失败,任务状态不符 playerId={},taskId={},status={}",
                    playerId, taskId, entry == null ? null : entry.getStatus());
            return res;
        }
        CoopTaskRule rule = configService.ruleOf(taskId);
        if (rule == null) {
            log.warn("创建协作房间失败,任务规则缺失 playerId={},taskId={}", playerId, taskId);
            return res;
        }
        //条件限定了游戏时只能选该游戏 (0=任意已解锁游戏)
        if (rule.gameType() != 0 && rule.gameType() != gameType) {
            log.info("创建协作房间失败,任务限定其他游戏 playerId={},taskId={},ruleGameType={},gameType={}",
                    playerId, taskId, rule.gameType(), gameType);
            return res;
        }
        int code = validateGame(playerId, gameType, roomCfgId);
        if (code != Code.SUCCESS) {
            log.info("创建协作房间失败,游戏校验不通过 playerId={},gameType={},roomCfgId={},reason={}",
                    playerId, gameType, roomCfgId, code);
            return res;
        }

        MarsNode node = nodeManager.getGameNodeByWeight(gameType, playerId,
                ctx.getPlayerController().ipAddress());
        if (node == null) {
            log.warn("创建协作房间失败,无可用游戏节点 playerId={},gameType={}", playerId, gameType);
            return res;
        }

        long roomId = snowflakeManager.nextId();
        CoopRoomRecord record = new CoopRoomRecord();
        record.setRoomId(roomId);
        record.setTaskId(taskId);
        record.setOwnerId(playerId);
        record.setGameType(gameType);
        record.setRoomCfgId(roomCfgId);
        record.setNodePath(node.getNodePath());
        record.setStatus(CoopTaskConst.RoomStatus.WAITING);
        List<Long> members = new ArrayList<>();
        members.add(playerId);
        record.setMemberIds(members);
        record.setMaxMembers(rule.maxMembers());
        record.setCreateTime(System.currentTimeMillis());
        roomRecordDao.save(record);

        coopTaskService.markRoomCreated(ctx, taskId, roomId, gameType);

        res.code = Code.SUCCESS;
        res.roomId = roomId;
        //先回包再切节点, 客户端凭 roomId 在 slots 节点进房
        ctx.send(res);
        switchToNode(ctx, gameType, roomCfgId, node);
        log.info("创建协作房间 playerId={},taskId={},roomId={},gameType={},node={}",
                playerId, taskId, roomId, gameType, node.getNodePath());
        return null;
    }

    /**
     * 协助者加入房间: 记录预检查 -> 按记录路由到房间所在节点 (权威判定在 slots 进房)。
     */
    public ResJoinCoopRoom joinRoom(SimPlayerContext ctx, long roomId) {
        ResJoinCoopRoom res = new ResJoinCoopRoom(Code.FAIL);
        res.roomId = roomId;
        long playerId = ctx.playerId();

        CoopRoomRecord record = roomRecordDao.get(roomId);
        if (record == null) {
            log.info("加入协作房间失败,房间不存在或已解散 playerId={},roomId={}", playerId, roomId);
            return res;
        }
        res.gameType = record.getGameType();
        if (record.getStatus() != CoopTaskConst.RoomStatus.WAITING) {
            log.info("加入协作房间失败,游戏已开始 playerId={},roomId={},status={}",
                    playerId, roomId, record.getStatus());
            return res;
        }
        boolean member = record.getMemberIds().contains(playerId);
        if (!member && record.getMemberIds().size() >= record.getMaxMembers()) {
            log.info("加入协作房间失败,房间已满员 playerId={},roomId={},members={},max={}",
                    playerId, roomId, record.getMemberIds().size(), record.getMaxMembers());
            return res;
        }
        //需求: 被邀请玩家未解锁此游戏时提示"游戏未解锁"
        if (simSkillsDao.findByGameType(playerId, record.getGameType()) == null) {
            log.info("加入协作房间失败,游戏未解锁 playerId={},roomId={},gameType={}",
                    playerId, roomId, record.getGameType());
            return res;
        }
        MarsNode node = marsCurator.getMarsNode(record.getNodePath());
        if (node == null) {
            //房间所在节点已下线, 记录留给 TTL/任务自愈清理
            log.warn("加入协作房间失败,节点不存在 playerId={},roomId={},node={}",
                    playerId, roomId, record.getNodePath());
            return res;
        }

        res.code = Code.SUCCESS;
        //先回包再切节点
        ctx.send(res);
        switchToNode(ctx, record.getGameType(), record.getRoomCfgId(), node);
        log.info("加入协作房间路由 playerId={},roomId={},gameType={},node={}",
                playerId, roomId, record.getGameType(), record.getNodePath());
        return null;
    }

    /**
     * 校验游戏已解锁且房间配置与游戏匹配。
     */
    private int validateGame(long playerId, int gameType, int roomCfgId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null || warehouseCfg.getGameID() != gameType) {
            log.warn("协作房间游戏配置不匹配 playerId={},gameType={},roomCfgId={}", playerId, gameType, roomCfgId);
            return Code.NOT_FOUND;
        }
        //必须是普通单人 slots 配置(roomType<100): 切节点后 sessionEnter 才走 enterSlotsGame 建单人 gameData;
        //好友房/SVIP 配置会被分流到房间 gameManager, 与协作房间模型不兼容
        if (warehouseCfg.getRoomType() >= GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
            log.warn("协作房间需普通单人slots配置 playerId={},roomCfgId={},roomType={}",
                    playerId, roomCfgId, warehouseCfg.getRoomType());
            return Code.PARAM_ERROR;
        }
        //游戏解锁判定与拜访客座赌局一致: 有该游戏技能数据即已解锁
        if (simSkillsDao.findByGameType(playerId, gameType) == null) {
            return Code.NOT_FOUND;
        }
        return Code.SUCCESS;
    }

    /**
     * 切换会话到目标游戏节点 (范式对齐 HallRoomService.enterGameNode)。
     */
    private void switchToNode(SimPlayerContext ctx, int gameType, int roomCfgId, MarsNode node) {
        playerSessionService.changeGameType(ctx.playerId(), gameType, roomCfgId);
        clusterSystem.switchNode(ctx.getPlayerController().getSession(), node);
    }
}
