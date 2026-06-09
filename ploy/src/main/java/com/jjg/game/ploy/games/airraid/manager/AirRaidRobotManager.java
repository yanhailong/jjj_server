package com.jjg.game.ploy.games.airraid.manager;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.data.PropInfo;
import com.jjg.game.core.data.RobotPlayer;
import com.jjg.game.core.utils.PropUtil;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.ploy.games.airraid.dao.AirRaidRankDao;
import com.jjg.game.ploy.games.airraid.data.AirRaidGameRoom;
import com.jjg.game.ploy.games.airraid.data.AirRaidRoundBetBook;
import com.jjg.game.ploy.games.airraid.data.AirRaidRuleConfig;
import com.jjg.game.ploy.games.airraid.function.EnqueueCashOutFunction;
import com.jjg.game.ploy.games.airraid.function.SyncCashOutsFunction;
import com.jjg.game.ploy.games.airraid.pb.AirRaidPlayerInfo;
import com.jjg.game.ploy.games.airraid.pb.cluster.BetSync;
import com.jjg.game.ploy.games.airraid.pb.cluster.PlayerCashOut;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AirstrikeRobotCfg;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 11
 * @date 2026/5/15
 */
@Component
public class AirRaidRobotManager {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AirRaidSendMessageManager sendMessageManager;
    @Autowired
    private AirRaidPhaseStateManager phaseStateManager;
    @Autowired
    private AirRaidRankDao airRaidRankDao;
    @Autowired
    private RobotUtil robotUtil;

    //初始的机器人人数
    private volatile int initRobotCount;
    //本回合参与的机器人
    private Map<Long, RobotPlayer> robotPlayerMap = new ConcurrentHashMap<>();
    //机器人下注金额
    private PropInfo robotBetPropInfo = null;

    /**
     * 处理机器人下注事件
     */
    public void handleRobotBetEvent(AirRaidGameRoom gameRoom, AirRaidRoundBetBook roundBetBook, Queue<AirRaidPlayerInfo> pendingBets) {
        AirstrikeRobotCfg cfg = GameDataManager.getAirstrikeRobotCfg(CoreConst.GameType.AIR_STRIKE);
        if (cfg == null || cfg.getInitial() == null || cfg.getInitial().size() < 2 || cfg.getIncrease() == null || cfg.getIncrease().size() < 2 || robotBetPropInfo == null) {
            return;
        }

        //计算本次应该添加的机器人人数
        int addRobotCount = 0;
        if (initRobotCount < 1) {
            initRobotCount = RandomUtils.randomMinMax(cfg.getInitial().get(0), cfg.getInitial().get(1));
            addRobotCount = initRobotCount;
        } else {
            int addProp = RandomUtils.randomMinMax(cfg.getIncrease().get(0), cfg.getIncrease().get(1));
            addRobotCount = PropUtil.propBase(addProp, initRobotCount);
        }

        if (addRobotCount < 1) {
            return;
        }

        BetSync syncMsg = new BetSync();
        syncMsg.roundId = gameRoom.getRoundId();

        for (int i = 0; i < addRobotCount; i++) {
            Integer betCfgId = this.robotBetPropInfo.getRandKey();
            if (betCfgId == null) {
                continue;
            }
            int[] dataSection = this.robotBetPropInfo.getDataSection(betCfgId);
            if (dataSection == null) {
                continue;
            }
            //获取一个机器人
            RobotPlayer robotPlayer = getRobotPlayer();
            if (robotPlayer == null) {
                continue;
            }

            int betCount = robotBetCount(cfg);

            for (int j = 0; j < betCount; j++) {
                //获取一个下注金额
                int bet = RandomUtils.randomMinMax(dataSection[0], dataSection[1]);
                roundBetBook.recordBet(robotPlayer.getId(), robotPlayer.getHeadImgId(), j, bet, robotPlayer.getNickName());

                AirRaidPlayerInfo airRaidPlayerInfo = new AirRaidPlayerInfo();
                airRaidPlayerInfo.playerId = robotPlayer.getId();
                airRaidPlayerInfo.headImgId = robotPlayer.getHeadImgId();
                airRaidPlayerInfo.headFrame = robotPlayer.getHeadFrameId();
                airRaidPlayerInfo.nick = robotPlayer.getNickName();
                airRaidPlayerInfo.bet = bet;
                airRaidPlayerInfo.betIndex = j;
                syncMsg.playerBetInfoList.add(airRaidPlayerInfo);
                pendingBets.offer(airRaidPlayerInfo);
            }
        }
        sendMessageManager.messageSync(syncMsg);
    }

    /**
     * 处理机器人兑现事件
     * 获取AirstrikeRobotCfg.CashIn，数据格式为 1000_2000,即在这个区间随机一个数得到万分比，每秒钟有(机器人人数*万分比)的机器人进行兑现操作
     * 仅主节点执行；产生的兑现 → 入本节点队列(等 tick 推送) + 通过 CashOutSync 同步到其他节点
     */
    public void handRobotCashoutEvent(AirRaidGameRoom gameRoom, AirRaidRoundBetBook roundBetBook, AirRaidRuleConfig airRaidRuleConfig, EnqueueCashOutFunction enqueueCashOut, SyncCashOutsFunction syncCashOutsFunction) {
        try {
            long now = System.currentTimeMillis();
            //必须仍在飞行阶段且未到坠毁
            if (!gameRoom.canCashOut(now)) {
                return;
            }

            AirstrikeRobotCfg cfg = GameDataManager.getAirstrikeRobotCfg(CoreConst.GameType.AIR_STRIKE);
            if (cfg == null || cfg.getCashIn() == null || cfg.getCashIn().size() < 2) {
                return;
            }

            //取所有未兑现的机器人下注
            List<AirRaidPlayerInfo> robotBets = new ArrayList<>();
            for (AirRaidPlayerInfo info : roundBetBook.getAllBets()) {
                if (!info.cashedOut && RobotUtil.isRobot(info.playerId)) {
                    robotBets.add(info);
                }
            }
            if (robotBets.isEmpty()) {
                return;
            }

            //本次应兑现的机器人数 = 总数 * 万分比
            int cashInProp = RandomUtils.randomMinMax(cfg.getCashIn().get(0), cfg.getCashIn().get(1));
            int cashOutCount = PropUtil.propBase(cashInProp, robotBets.size());
            if (cashOutCount < 1) {
                return;
            }

            int currentMultiplier = phaseStateManager.getAuthoritativeCurrentMultiplier(gameRoom, airRaidRuleConfig, now);
            Collections.shuffle(robotBets);
            int limit = Math.min(cashOutCount, robotBets.size());

            List<PlayerCashOut> syncList = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) {
                AirRaidPlayerInfo info = robotBets.get(i);
                RobotPlayer robotPlayer = this.robotPlayerMap.get(info.playerId);
                if (robotPlayer == null) {
                    continue;
                }
                long winAmount = info.bet * currentMultiplier / 10000;
                roundBetBook.recordCashOut(info.playerId, info.betIndex, currentMultiplier, winAmount);

                // 写入排行榜
                airRaidRankDao.addCashOut(info.playerId, robotPlayer.getHeadImgId(), robotPlayer.getHeadFrameId(), now, info.bet, winAmount, currentMultiplier, gameRoom.getCrashMultiplier(), info.betIndex, gameRoom.getRoundId(), robotPlayer.getNickName());

                //本节点入队
                enqueueCashOut.apply(info.playerId, info.betIndex, currentMultiplier, winAmount);

                //集群同步给其他节点
                PlayerCashOut playerCashOut = new PlayerCashOut();
                playerCashOut.playerId = info.playerId;
                playerCashOut.cashOutMultiplier = currentMultiplier;
                playerCashOut.winAmount = winAmount;
                playerCashOut.betIndex = info.betIndex;
                syncList.add(playerCashOut);
            }
            if (!syncList.isEmpty()) {
                syncCashOutsFunction.apply(syncList);
            }
        } catch (Exception e) {
            log.error("AirRaid 机器人兑现异常", e);
        }
    }

    /**
     * 加载机器人配置
     */
    public void loadAirstrikeRobotConfig() {
        AirstrikeRobotCfg cfg = GameDataManager.getAirstrikeRobotCfg(CoreConst.GameType.AIR_STRIKE);
        if (cfg == null) {
            log.warn("初始化空袭机器人配置失败");
            return;
        }

        PropInfo pInfo = new PropInfo();

        int begin, end = 0;

        //cfg.getStake()数据格式 1000_10000_5000|10001_50000_3000|50001_200000_1500|200001_500000_400|500001_1000000_100
        //[0] = 下注区间begin ， [1] = 下注区间end ，  [2] = 该区间对应的权重
        for (int i = 0; i < cfg.getStake().size(); i++) {
            List<Integer> list = cfg.getStake().get(i);

            begin = end;
            end += list.get(2);
            pInfo.addProp(i, begin, end);
            pInfo.addData(i, list.get(0), list.get(1));
        }

        this.robotBetPropInfo = pInfo;
    }

    /**
     * 获取一个机器人
     *
     * @return
     */
    private RobotPlayer getRobotPlayer() {
        List<RobotCfg> robotCfgList = GameDataManager.getRobotCfgList();
        if (robotCfgList == null || robotCfgList.isEmpty()) {
            return null;
        }
        for (int i = 0; i < 100; i++) {
            RobotCfg robotCfg = RandomUtil.randomEle(robotCfgList);
            if (robotCfg == null) {
                continue;
            }
            //获取一个机器人
            RobotPlayer robotPlayer = robotUtil.initRobotPlayer(robotCfg);
            if (robotPlayer == null || this.robotPlayerMap.containsKey(robotPlayer.getId())) {
                continue;
            }
            this.robotPlayerMap.put(robotPlayer.getId(), robotPlayer);
            return robotPlayer;
        }
        return null;
    }

    private int robotBetCount(AirstrikeRobotCfg cfg) {
        try {
            if (cfg.getPeopleNum() != null && !cfg.getPeopleNum().isEmpty()) {
                int rand1 = RandomUtils.randomMinMax(cfg.getPeopleNum().get(0), cfg.getPeopleNum().get(1));
                if (RandomUtils.randomMinMax(1, 10000) <= rand1) {
                    return 2;
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return 1;
    }

    public void clear() {
        this.initRobotCount = 0;
        this.robotPlayerMap.clear();
    }
}
