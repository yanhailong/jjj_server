package com.jjg.game.ploy.controller;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.ploy.data.PlayerMultiPloyGameData;
import com.jjg.game.ploy.data.PloyGameRoom;
import com.jjg.game.ploy.games.airraid.data.AirRaidPlayerPloyGameData;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 多人策略游戏抽象控制器
 * <p>
 * 提供集群通信、本地广播、机器人管理的通用能力，子类实现具体游戏逻辑。
 * </p>
 *
 * @author 11
 * @date 2026/3/19
 */
public abstract class AbstractMultiPloyController<T extends PlayerMultiPloyGameData, R extends PloyGameRoom> extends AbstractPloyController<T> implements IGameClusterLeaderListener {
    @Autowired
    protected ClusterSystem clusterSystem;
    @Autowired
    protected MarsCurator marsCurator;

    protected Class<R> gameRoomDataCla;

    /**
     * 机器人玩家数据 key=robotId
     */
    protected final Map<Long, T> robotDataMap = new ConcurrentHashMap<>();

    public AbstractMultiPloyController(Logger log, Class<T> playerGameDataCla, Class<R> gameRoomDataCla) {
        super(log, playerGameDataCla);
        this.gameRoomDataCla = gameRoomDataCla;
    }

    @Override
    public void notLeader() {

    }

    @Override
    public void isLeader() {

    }

    /**
     * 消息同步到其他节点
     *
     * @param msg      要同步的消息
     */
    protected void messageSync(AbstractMessage msg) {
        PFMessage pfMessage = MessageUtil.getPFMessage(msg);
        clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString(), NodeType.GAME.toString())::contains);
    }

    /**
     * 通知到本节点所有的真实玩家
     *
     * @param msg 要推送的消息
     */
    protected void broadcastLocalPlayers(AbstractMessage msg) {
        broadcastLocalPlayersExcept(msg, 0);
    }

    /**
     * 通知到本节点所有的真实玩家
     *
     * @param msg
     * @param excludePlayerId 排除玩家
     */
    protected void broadcastLocalPlayersExcept(AbstractMessage msg, long excludePlayerId) {
        Map<Long, T> playerMap = this.gameDataMap.get(this.roomCfgId);
        if (playerMap == null) {
            return;
        }
        playerMap.forEach((playerId, playerData) -> {
            if (playerId == excludePlayerId) {
                return;
            }
            if (playerData.getPlayerController() != null) {
                playerData.getPlayerController().send(msg);
            }
        });
    }

    // ==================== 机器人管理(子类按需覆写) ====================

    /**
     * 获取当前时段目标机器人数量(从配置表读取)
     * <p>
     * 配置来自 PloygameRoomCfg.robot_num，格式: [[时段小时, 机器人数量], ...]
     * 子类可覆写以实现更复杂的时段逻辑。
     * </p>
     *
     * @return 目标机器人数量, 无配置返回0
     */
    protected int getTargetRobotCount() {
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(this.roomCfgId);
        if (cfg == null || cfg.getRobot_num() == null || cfg.getRobot_num().isEmpty()) {
            return 0;
        }
        List<List<Integer>> robotNum = cfg.getRobot_num();
        // 默认取第一个配置段的数量
        List<Integer> first = robotNum.get(0);
        return first != null && first.size() > 1 ? first.get(1) : 0;
    }

    /**
     * 获取机器人加入间隔时间(ms)
     *
     * @return 间隔时间列表(随机取一个), 无配置返回null
     */
    protected List<Integer> getRobotIntervalConfig() {
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(this.roomCfgId);
        return cfg != null ? cfg.getIntervalTime() : null;
    }

    /**
     * 机器人行为调度(由游戏循环定时器触发)
     * <p>
     * 子类覆写此方法实现机器人的下注、兑现等AI逻辑。
     * 在游戏循环的每个tick中调用，子类根据当前游戏阶段决定机器人行为。
     * </p>
     */
    protected void tickRobots() {
        // 默认空实现, 子类按需覆写
    }

    /**
     * 清空所有机器人数据(新回合时调用)
     */
    protected void clearRobots() {
        robotDataMap.clear();
    }
}
