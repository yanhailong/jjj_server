package com.jjg.game.room.services;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.robot.RobotAcquireRequest;
import com.jjg.game.room.robot.RobotPool;
import com.jjg.game.room.robot.RobotPoolEntry;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.ObjLongConsumer;
import java.util.function.ToLongFunction;

/**
 * 机器人处理逻辑，目前将机器人数据放在redis中，如果后续redis数据的IO过于频繁，考虑将机器人放入内存中管理
 *
 * @author lm
 */
@Service
public class RobotService implements IRoomStartListener, ConfigExcelChangeListener {

    private final Logger log = LoggerFactory.getLogger(RobotService.class);
    // 不同时间段房间创建机器人的人数限制
    private final Map<Integer, TreeMap<Integer, Integer>> roomRobotCreateLimit = new HashMap<>();
    //机器人池
    private final RobotPool robotPool = new RobotPool();
    private final RobotUtil robotUtil;
    private final MarsCurator marsCurator;

    public RobotService(RobotUtil robotUtil, MarsCurator marsCurator) {
        this.robotUtil = robotUtil;
        this.marsCurator = marsCurator;
    }


    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(RobotCfg.EXCEL_NAME, this::initRobotPool);
        addChangeSampleFileObserveWithCallBack(RobotCfg.EXCEL_NAME, this::initRobotPool);
    }

    /**
     * 获取机器人
     *
     * @param roomCfgId 房间配置id
     * @param roomId    房间id
     * @return 机器人
     */
    private RobotPlayer getRobotPlayer(int roomCfgId, long roomId) {
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        RoomCfg roomCfg = GameDataManager.getRoomCfg(roomCfgId);
        if (warehouseCfg == null || roomCfg == null) {
            return null;
        }
        int itemId = warehouseCfg.getTransactionItemId();
        long enterLimit = getRealEnterLimit(warehouseCfg);
        try {
            // 获取条件只描述房间限制，实际的池索引和借出状态由 RobotPool 维护。
            RobotAcquireRequest request = new RobotAcquireRequest(itemId, enterLimit, warehouseCfg.getEnterMax(), warehouseCfg.getPlayerLvLimit());
            RobotPoolEntry robotPoolEntry = robotPool.acquire(request);
            if (robotPoolEntry == null) {
                return null;
            }
            try {
                return createRobot(robotPoolEntry, itemId, roomId, roomCfg);
            } catch (Exception e) {
                // 池已经完成借出标记；后续创建失败必须立即回收，避免机器人丢失。
                robotPool.recycle(robotPoolEntry);
                throw e;
            }
        } catch (Exception e) {
            log.error("获取机器人异常 roomCfgId:{} roomId:{}", roomCfgId, roomId, e);
        }
        return null;
    }


    /**
     * 创建robot playerController
     */
    public PlayerController getOrCreateRobotPlayerController(int roomCfgId, long roomId) {
        RobotPlayer robotPlayer = getRobotPlayer(roomCfgId, roomId);
        if (robotPlayer == null) {
            return null;
        }
        String nodePath = marsCurator.nodePath;
        PFSession robotSession = new PFSession(null, null, null);
        robotSession.setGatePath(nodePath);
        return new PlayerController(robotSession, robotPlayer);
    }


    /**
     * 根据池条目创建房间内使用的机器人玩家数据。
     */
    private RobotPlayer createRobot(RobotPoolEntry robotPoolEntry, int itemId, long roomId, RoomCfg roomCfg) {
        RobotPlayer robotPlayer = robotUtil.initRobotPlayer(robotPoolEntry.robotCfg());
        robotPlayer.setRoomId(roomId);
        robotPlayer.setRoomCfgId(roomCfg.getId());
        setRobotCurrency(robotPlayer, itemId, robotPoolEntry.getAmount(itemId));
        robotPlayer.setGameType(roomCfg.getGameID());
        robotPlayer.setRoomCfgId(roomCfg.getId());
        return robotPlayer;
    }


    /**
     * 检查当前房间是否还能创建机器人。
     * 同时受时间段人数上限、池内可用机器人、房间已有机器人数量限制。
     */
    public boolean checkCanCreateRobot(int roomCfgId, Room room) {
        // 检查当前游戏的人数是否达到上限
        TreeMap<Integer, Integer> createLimitByTime = roomRobotCreateLimit.get(roomCfgId);
        if (createLimitByTime == null || createLimitByTime.isEmpty()) {
            return false;
        }
        Calendar calendar = DateUtils.toCalendar(new Date(System.currentTimeMillis()));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int robotLimit = 0;
        for (Map.Entry<Integer, Integer> entry : createLimitByTime.descendingMap().entrySet()) {
            if (hour <= entry.getKey()) {
                robotLimit = entry.getValue();
            } else {
                break;
            }
        }
        // 获取当前机器人数量
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg == null) {
            return false;
        }
        if (!robotPool.hasAvailableRobot(warehouseCfg.getTransactionItemId())) {
            // 如果没有机器人可以创建,所有的机器人都已被分配完
            return false;
        }
        // 获取房间中机器人数量
        long curRobotNum = room.countRobots();
        return curRobotNum < robotLimit;
    }


    /**
     * 机器人启动加载流程
     */
    @Override
    public void start() {
        // 检查机器人数量是否配置正确,robot表中的机器人创建限制字段和机器人表中对应游戏的机器人数量是否一致
        List<RoomCfg> roomCfgList = GameDataManager.getRoomCfgList();
        for (RoomCfg roomCfg : roomCfgList) {
            List<List<Integer>> robotNumList = roomCfg.getRobot_num();
            for (List<Integer> robotNumConf : robotNumList) {
                // robot_num 配置格式：[序号, 结束小时, 机器人上限]。
                roomRobotCreateLimit
                        .computeIfAbsent(roomCfg.getId(), k -> new TreeMap<>())
                        .put(robotNumConf.get(1), robotNumConf.get(2));
            }
        }
    }


    /**
     * 随机生成机器人本次携带的金币数量。
     */
    private long getRobotRealMoney(RobotCfg cfg) {
        return RandomUtils.randomWeightList(cfg.getAddMoney());
    }

    /**
     * 随机生成机器人本次携带的贝币数量。
     */
    private long getRobotRealConchMoney(RobotCfg cfg) {
        return RandomUtils.randomWeightList(cfg.getAddMoney1());
    }

    /**
     * 按最新 robot.xlsx 配置重建机器人池。
     * RobotPool 会跳过已经借出的机器人，避免热加载导致重复分配。
     */
    public void initRobotPool() {
        List<RobotPoolEntry> robotPoolEntries = new ArrayList<>();
        List<RobotCfg> robotCfgList = GameDataManager.getRobotCfgList();
        for (RobotCfg robotCfg : robotCfgList) {
            // available != 0 表示该机器人不进入匹配池。
            if (robotCfg.getAvailable() != 0) {
                continue;
            }
            robotPoolEntries.add(createRobotPoolEntry(robotCfg));
        }
        robotPool.reload(robotPoolEntries);
    }

    /**
     * 批量回收离开房间的机器人。
     */
    public void recycleRobotPlayers(List<Long> robotIds) {
        try {
            for (Long robotId : robotIds) {
                if (robotId == null) {
                    continue;
                }
                recycleRobot(robotId);
            }
        } catch (Exception e) {
            log.error("recycleRobotPlayer error robotId:{}", robotIds, e);
        }
    }

    /**
     * 回收单个离开房间的机器人。
     */
    public void recycleRobotPlayer(long robotId) {
        try {
            recycleRobot(robotId);
        } catch (Exception e) {
            log.error("recycleRobotPlayer error robotId:{}", robotId, e);
        }
    }

    public RobotCfg getRobotCfg(long robotId) {
        return robotUtil.getRobotCfg(robotId);
    }

    /**
     * 将机器人配置ID转换为当前节点的真实机器人ID。
     */
    private long getRobotId(RobotCfg robotCfg) {
        return robotUtil.getId(robotCfg.getId());
    }

    /**
     * 计算房间真实入场限制。
     * 金币房需要按全局配置111放大，其他货币直接使用房间配置。
     */
    private long getRealEnterLimit(WarehouseCfg warehouseCfg) {
        long enterLimit = warehouseCfg.getEnterLimit();
        if (warehouseCfg.getTransactionItemId() != ItemUtils.getGoldItemId()) {
            return enterLimit;
        }
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(111);
        if (globalConfigCfg == null) {
            return enterLimit;
        }
        return enterLimit * globalConfigCfg.getIntValue();
    }

    /**
     * 创建机器人入池快照。
     * 每次入池都会重新随机携带货币，回收后再次入池也会生成新的携带数量。
     */
    private RobotPoolEntry createRobotPoolEntry(RobotCfg robotCfg) {
        Map<Integer, Long> itemAmounts = new HashMap<>();
        for (RobotCurrencyDefinition definition : getRobotCurrencyDefinitions()) {
            itemAmounts.put(definition.itemId(), definition.amountGetter().applyAsLong(robotCfg));
        }
        return new RobotPoolEntry(getRobotId(robotCfg), robotCfg, Map.copyOf(itemAmounts));
    }

    /**
     * 当前机器人支持的货币定义。
     * 后续新增货币时，只需在这里加入 itemId 和金额生成方式。
     */
    private List<RobotCurrencyDefinition> getRobotCurrencyDefinitions() {
        List<RobotCurrencyDefinition> definitions = new ArrayList<>(3);
        int goldItemId = ItemUtils.getGoldItemId();
        if (goldItemId > 0) {
            definitions.add(new RobotCurrencyDefinition(goldItemId, this::getRobotRealMoney, RobotPlayer::setGold));
        }
        int shellItemId = ItemUtils.getShellItemId();
        if (shellItemId > 0) {
            definitions.add(new RobotCurrencyDefinition(shellItemId, this::getRobotRealConchMoney, RobotPlayer::setShell));
        }
        // 赛季币只用于房间内机器人模拟结算，不访问 SIM；复用普通金币携带区间和 Player.gold 作为临时余额。
        definitions.add(new RobotCurrencyDefinition(GameConstant.Item.ID_SEASON_COIN,
                this::getRobotRealMoney, RobotPlayer::setGold));
        return definitions;
    }

    /**
     * 按当前配置回收机器人。
     * 配置缺失或已禁用时只释放借出状态，不重新进入可匹配池。
     */
    private void recycleRobot(long robotId) {
        RobotCfg robotCfg = robotUtil.getRobotCfg(robotId);
        if (robotCfg == null || robotCfg.getAvailable() != 0) {
            robotPool.discard(robotId);
            return;
        }
        robotPool.recycle(createRobotPoolEntry(robotCfg));
    }

    /**
     * 将机器人本次匹配使用的货币数量写入 Player 字段。
     */
    private void setRobotCurrency(RobotPlayer robotPlayer, int itemId, long amount) {
        for (RobotCurrencyDefinition definition : getRobotCurrencyDefinitions()) {
            if (definition.itemId() == itemId) {
                definition.amountSetter().accept(robotPlayer, amount);
                return;
            }
        }
    }

    /**
     * 机器人货币定义。
     *
     * @param itemId       货币道具ID
     * @param amountGetter 从 RobotCfg 随机生成携带数量的方法
     * @param amountSetter 将匹配货币写入 RobotPlayer 的方法
     */
    private record RobotCurrencyDefinition(int itemId, ToLongFunction<RobotCfg> amountGetter, ObjLongConsumer<RobotPlayer> amountSetter) {
    }


    @Override
    public void shutdown() {
    }

}
