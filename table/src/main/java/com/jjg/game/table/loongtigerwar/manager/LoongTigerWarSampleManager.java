package com.jjg.game.table.loongtigerwar.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.table.loongtigerwar.constant.LoongTigerWarConstant;
import com.jjg.game.table.redblackwar.constant.HandType;
import com.jjg.game.table.redblackwar.constant.RedBlackWarConstant;
import com.jjg.game.table.redblackwar.sample.GameDataManager;
import com.jjg.game.table.redblackwar.sample.bean.BetAreaCfg;
import com.jjg.game.table.redblackwar.sample.bean.WinPosWeightCfg;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.jjg.game.table.redblackwar.constant.RedBlackWarConstant.Common.RED_BLACK_LIMIT;

/**
 * @author 11
 * @date 2025/6/30 10:38
 */
@Component
public class LoongTigerWarSampleManager extends AbstractSampleManager {
    //区域id->区域配置
    private Map<Integer, BetAreaCfg> betAreaMap;
    //押注区域id(1龙,2虎,3合)
    private Map<Integer, List<WinPosWeightCfg>> cfgMap;

    public void init() {
        log.info("开始加载龙虎斗游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return LoongTigerWarConstant.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() {
        boolean isLoad = true;
        try {
            GameDataManager.loadAllData(getSamplePath());
            //初始化龙虎斗压分区域
            Map<Integer, BetAreaCfg> tempBetAreaMap = GameDataManager.getBetAreaCfgList()
                    .stream()
                    .filter(betAreaCfg -> betAreaCfg.getGameID() == CoreConst.GameType.LOONG_TIGER_WAR)
                    .collect(Collectors.toMap(BetAreaCfg::getId, betAreaCfg -> betAreaCfg));
            //初始化龙虎斗胜利配置
            Map<Integer, List<WinPosWeightCfg>> tempAreaWinMap = new HashMap<>();
            Map<Integer, WinPosWeightCfg> tempWinMap = new HashMap<>();
            for (WinPosWeightCfg winPosWeightCfg : GameDataManager.getWinPosWeightCfgList()) {
                if (winPosWeightCfg.getGameID() == CoreConst.GameType.LOONG_TIGER_WAR) {
                    List<WinPosWeightCfg> weightCfgs = tempAreaWinMap.computeIfAbsent(winPosWeightCfg.getWinPosID(), key -> new ArrayList<>());
                    tempWinMap.put(winPosWeightCfg.getId(), winPosWeightCfg);
                    weightCfgs.add(winPosWeightCfg);
                }
            }
            //校验押注区域和获胜的配置
            //押注id->对应的开奖位置
            Map<Integer, List<Integer>> map = new HashMap<>();
            for (Map.Entry<Integer, WinPosWeightCfg> entry : tempWinMap.entrySet()) {
                WinPosWeightCfg winPosWeightCfg = entry.getValue();
                List<Integer> betArea = winPosWeightCfg.getBetArea();
                if (Objects.isNull(betArea) || betArea.isEmpty()) {
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
                    if (!isLoad) {
                        break;
                    }
                }
            }
            if (isLoad) {
                betAreaMap = tempBetAreaMap;
                cfgMap = tempAreaWinMap;
            }
        } catch (Exception e) {
            log.error("加载配置表失败", e);
            isLoad = false;
        }
        if (!isLoad) {
            //throw new RuntimeException("配置错误");
            return;
        }
    }

    @Override
    protected void sampleChange(File file) {
        try {
//            Set<Class<? extends BaseCfgBean>> changeCfgBean = GameDataManager.getInstance().loadDataByChangeFileList(getSamplePath(), Collections.singletonList(file));
//            Map<String, ConfigExcelChangeListener> configExcelChangeListeners = CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
//            configExcelChangeListeners.values().forEach(listener -> {
//                listener.change(changeCfgBean.iterator().next().getSimpleName());
//            });
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public Map<Integer, List<WinPosWeightCfg>> getCfgMap() {
        return cfgMap;
    }

    public Map<Integer, BetAreaCfg> getBetAreaMap() {
        return betAreaMap;
    }
}
