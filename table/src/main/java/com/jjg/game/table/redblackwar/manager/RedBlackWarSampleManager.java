package com.jjg.game.table.redblackwar.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.redblackwar.constant.HandType;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.table.redblackwar.constant.RedBlackWarConstant.Common.RED_BLACK_LIMIT;

/**
 * @author 11
 * @date 2025/6/30 10:38
 */
@Component
public class RedBlackWarSampleManager implements ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(getClass());
    //区域id->区域配置
    private Map<Integer, BetAreaCfg> betAreaMap;
    //阵容->牌型->开奖结果配置
    private Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> winMap;
    //所有的开奖结果配置
    private final List<WinPosWeightCfg> allWinPosWeight = new ArrayList<>();

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(BetAreaCfg.EXCEL_NAME, this::initSampleConfig)
            .addInitSampleFileObserveWithCallBack(WinPosWeightCfg.EXCEL_NAME, this::initSampleConfig);
    }

    public void initSampleConfig() {
        boolean isLoad = true;
        try {
            //初始化红黑大战压分区域
            Map<Integer, BetAreaCfg> tempBetAreaMap = GameDataManager.getBetAreaCfgList()
                .stream()
                .filter(betAreaCfg -> betAreaCfg.getGameID() == CoreConst.GameType.RED_BLACK_WAR)
                .collect(Collectors.toMap(BetAreaCfg::getId, betAreaCfg -> betAreaCfg));
            //红黑大战胜利配置
            Map<Integer, WinPosWeightCfg> tempWinMap = GameDataManager.getWinPosWeightCfgList()
                .stream()
                .filter(betAreaCfg -> betAreaCfg.getGameID() == CoreConst.GameType.RED_BLACK_WAR)
                .collect(Collectors.toMap(WinPosWeightCfg::getId, betAreaCfg -> betAreaCfg));
            //校验押注区域和获胜的配置
            //押注id->对应的开奖位置
            Map<Integer, List<Integer>> map = new HashMap<>();
            for (Map.Entry<Integer, WinPosWeightCfg> entry : tempWinMap.entrySet()) {
                WinPosWeightCfg winPosWeightCfg = entry.getValue();
                List<Integer> betArea = winPosWeightCfg.getBetArea();
                if (Objects.isNull(betArea) || betArea.size() != 1) {
                    isLoad = false;
                    break;
                }
                for (Integer betAreaId : betArea) {
                    map.computeIfAbsent(betAreaId, b -> new ArrayList<>()).add(entry.getKey());
                }
            }
            if (isLoad) {
                for (Map.Entry<Integer, BetAreaCfg> cfgEntry : tempBetAreaMap.entrySet()) {
                    BetAreaCfg betAreaCfg = cfgEntry.getValue();
                    List<Integer> posWin = betAreaCfg.getPosWin();
                    List<Integer> posWin2 = map.get(cfgEntry.getKey());
                    if (posWin == null && posWin2 == null) {
                        continue;
                    }
                    if (posWin2 == null || posWin == null) {
                        isLoad = false;
                        break;
                    }
                    if (posWin.size() != posWin2.size()) {
                        isLoad = false;
                        break;
                    }
                    for (Integer winId : posWin) {
                        if (!posWin2.contains(winId)) {
                            isLoad = false;
                            break;
                        }
                    }
                    for (Integer winId : posWin) {
                        WinPosWeightCfg winPosWeightCfg = tempWinMap.get(winId);
                        if (betAreaCfg.getAreaID() == RedBlackWarConstant.Common.RED_AREA) {
                            if (winPosWeightCfg.getWinPosID() > 0 && winPosWeightCfg.getWinPosID() < 10) {
                                continue;
                            }
                        }
                        if (betAreaCfg.getAreaID() == RedBlackWarConstant.Common.BLACK_AREA) {
                            if (winPosWeightCfg.getWinPosID() > 10) {
                                continue;
                            }
                        }
                        if (betAreaCfg.getAreaID() == RedBlackWarConstant.Common.LUCK_AREA) {
                            if (winPosWeightCfg.getWinPosID() % 10 > 1) {
                                continue;
                            }
                        }
                        isLoad = false;
                        break;
                    }
                    if (!isLoad) {
                        break;
                    }
                }
            }
            if (isLoad) {
                betAreaMap = tempBetAreaMap;
                Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> typeListHashMap = new HashMap<>();
                for (WinPosWeightCfg value : tempWinMap.values()) {
                    allWinPosWeight.add(value);
                    Map<HandType, List<WinPosWeightCfg>> handTypeListMap =
                        typeListHashMap.computeIfAbsent(value.getWinPosID() > RED_BLACK_LIMIT ?
                                RedBlackWarConstant.Camp.BLACK : RedBlackWarConstant.Camp.RED,
                        b -> new HashMap<>());
                    handTypeListMap.computeIfAbsent(HandType.getHandType(value.getWinPosID() % RED_BLACK_LIMIT),
                            k -> new ArrayList<>())
                        .add(value);
                }
                winMap = typeListHashMap;
            }
        } catch (Exception e) {
            log.error("加载配置表失败", e);
            isLoad = false;
        }
        if (!isLoad) {
            throw new RuntimeException("配置错误");

        }
    }

    public List<WinPosWeightCfg> getAllWinPosWeight() {
        return allWinPosWeight;
    }

    public Map<Integer, BetAreaCfg> getBetAreaMap() {
        return betAreaMap;
    }

    public Map<RedBlackWarConstant.Camp, Map<HandType, List<WinPosWeightCfg>>> getWinMap() {
        return winMap;
    }


}
