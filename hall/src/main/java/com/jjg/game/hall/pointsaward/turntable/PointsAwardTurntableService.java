package com.jjg.game.hall.pointsaward.turntable;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.pointsaward.PlayerPointsAwardService;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableConfig;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PointsAwardTurntableCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 积分大奖转盘服务
 */
@Service
public class PointsAwardTurntableService implements GameEventListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 配置初始化时间
     */
    private LocalDate configDate;

    private final PlayerPointsAwardService pointsAwardService;
    private final PlayerPackService playerPackService;

    private final TreeMap<Integer, PointsAwardTurntableCfg> cfgTreeMap = new TreeMap<>();

    public PointsAwardTurntableService(PlayerPointsAwardService pointsAwardService, PlayerPackService playerPackService) {
        this.pointsAwardService = pointsAwardService;
        this.playerPackService = playerPackService;
    }

    public void init() {
        initConfig();
    }

    /**
     * 初始化配置
     */
    public void initConfig() {
        configDate = LocalDate.now();
        LocalDate now = LocalDate.now();
        //当前月最大天数
        int totalDays = now.lengthOfMonth();
        List<PointsAwardTurntableCfg> cfgList = GameDataManager.getPointsAwardTurntableCfgList();
        if (cfgList != null && !cfgList.isEmpty()) {
            //先保存一份默认配置
            List<PointsAwardTurntableCfg> resultList = cfgList.stream().filter(cfg -> {
                String time = cfg.getTime();
                //没有时间限制 并且小于本月最大天数 不加载多余配置有几天就加载几条
                return (time == null || time.isEmpty()) && cfg.getId() <= totalDays;
            }).toList();
            if (resultList.isEmpty()) {
                log.warn("积分大奖转盘配置没有默认配置!");
            }
            //根据当前月份筛选一份新的配置
            List<PointsAwardTurntableCfg> mothConfigList = cfgList.stream().filter(cfg -> {
                String time = cfg.getTime();
                //有时间限制
                if (time != null && !time.isEmpty()) {
                    long timestamp = TimeHelper.getTimestamp(time.trim());
                    //有时间限制 并且小于本月最大天数 不加载多余配置有几天就加载几条
                    if (timestamp > 0) {
                        LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                        return dateTime.isEqual(now) && cfg.getId() <= totalDays;
                    }
                }
                return false;
            }).toList();
            cfgTreeMap.clear();
            if (!mothConfigList.isEmpty()) {
                mothConfigList.forEach(cfg -> cfgTreeMap.put(cfg.getId(), cfg));
            } else {
                resultList.forEach(cfg -> cfgTreeMap.put(cfg.getId(), cfg));
            }
            if (cfgTreeMap.isEmpty()) {
                log.error("积分大奖转盘配置加载失败!");
            }
        }
    }

    /**
     * 获取转盘配置
     */
    public PointsAwardTurntableCfg getCfg(int gridId) {
        return cfgTreeMap.get(gridId);
    }

    /**
     * 获取当前的转盘配置列表
     */
    public List<PointsAwardTurntableConfig> getConfigList() {
        List<PointsAwardTurntableConfig> result = new ArrayList<>();
        if (cfgTreeMap.isEmpty()) {
            return result;
        }
        return cfgTreeMap.values().stream().map(cfg -> {
            PointsAwardTurntableConfig config = new PointsAwardTurntableConfig();
            config.setGridId(cfg.getId());
            config.setIntegralNum(cfg.getIntegralNum());
            config.setItemList(ItemUtils.buildItemInfos(cfg.getGetItem()));
            return config;
        }).toList();
    }

    /**
     * 玩家旋转转盘
     *
     * @param playerId 玩家id
     * @return true 成功
     */
    public int spin(long playerId) {
        int selectedId = -1;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(40);
        if (globalConfigCfg == null) {
            return selectedId;
        }
        int consume = globalConfigCfg.getIntValue();
        //先出结果
        Map<Integer, Integer> probabilityMap = cfgTreeMap.values().stream().collect(Collectors.toMap(PointsAwardTurntableCfg::getId, PointsAwardTurntableCfg::getProbability, (a, b) -> b));
        Set<Integer> selectedIds = RandomUtils.getRandomByWeight(probabilityMap, 1);
        if (!selectedIds.iterator().hasNext()) {
            return selectedId;
        }
        selectedId = selectedIds.iterator().next();
        //在扣除积分
        boolean deduct = pointsAwardService.deduct(playerId, consume, null);
        if (deduct) {
            //发送奖励
            PointsAwardTurntableCfg awardTurntableCfg = getCfg(selectedId);
            if (awardTurntableCfg != null) {
                int integralPoints = awardTurntableCfg.getIntegralNum();
                //积分奖励
                if (integralPoints > 0) {
                    pointsAwardService.add(playerId, integralPoints, null);
                }
                //道具奖励
                if (awardTurntableCfg.getGetItem() != null && !awardTurntableCfg.getGetItem().isEmpty()) {
                    playerPackService.addItems(playerId, ItemUtils.buildItems(awardTurntableCfg.getGetItem()), "积分大奖转盘奖励");
                }
            } else {
                log.warn("玩家[{}]积分大奖转盘奖励发送失败!中奖id[{}]配置不存在!", playerId, selectedId);
            }
        }
        return selectedId;
    }

    /**
     * 处理事件
     *
     * @param gameEvent 事件
     */
    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof ClockEvent clockEvent) {
            int hour = clockEvent.getHour();
            if (hour == 0) {
                LocalDate now = LocalDate.now();
                if (now.getMonthValue() != configDate.getMonthValue()) {
                    //重新初始化配置
                    initConfig();
                }
            }
        }
    }

    /**
     * 需要监听的事件类型, 根据实际需要监听的类型写入，通过配置表配置或者手动配置，需尽量避免写入无关事件类型
     *
     * @return 事件类型列表
     */
    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT);
    }
}
