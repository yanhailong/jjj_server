package com.jjg.game.table.dicecommon;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 骰子类数据持有者
 *
 * @author 2CL
 */
@Repository
public class DiceDataHolder implements ConfigExcelChangeListener {

    private static final Logger log = LoggerFactory.getLogger(DiceDataHolder.class);
    // 骰宝配置tree 大小骰宝配置tree 越南色碟配置tree 鱼虾蟹配置tree
    private final static Map<EGameType, DiceTreeNode<List<WinPosWeightCfg>>> ROOT_NODES = new HashMap<>();

    // 骰子配置数据
    private static class DiceMetaData {
        // 骰子数量
        public int diceNum;
        // 单个骰子最大点数
        public int diceMaxPoint;

        public DiceMetaData(int diceNum, int diceMaxPoint) {
            this.diceNum = diceNum;
            this.diceMaxPoint = diceMaxPoint;
        }
    }

    @Override
    public void changeSampleCallbackCollector() {
        // 监听押注区域和投注倍数配置表
        addInitSampleFileObserveWithCallBack(
            WinPosWeightCfg.EXCEL_NAME, DiceDataHolder::buildDiceTreeBySampleData)
            .addInitSampleFileObserveWithCallBack(
                BetAreaCfg.EXCEL_NAME, DiceDataHolder::buildDiceTreeBySampleData)
            .addChangeSampleFileObserveWithCallBack(
                WinPosWeightCfg.EXCEL_NAME, DiceDataHolder::buildDiceTreeBySampleData)
            .addChangeSampleFileObserveWithCallBack(
                BetAreaCfg.EXCEL_NAME, DiceDataHolder::buildDiceTreeBySampleData);
    }

    /**
     * 获取游戏骰子相关配置
     */
    private static Map<EGameType, DiceMetaData> getDiceGameConfig() {
        Map<EGameType, DiceMetaData> diceGameTypes = new HashMap<>();
        diceGameTypes.put(EGameType.DICE_TREASURE, new DiceMetaData(3, 6));
        diceGameTypes.put(EGameType.VIETNAM_DICE, new DiceMetaData(4, 2));
        diceGameTypes.put(EGameType.SIZE_DICE_TREASURE, new DiceMetaData(3, 6));
        diceGameTypes.put(EGameType.RIVER_ANIMALS, new DiceMetaData(3, 6));
        return diceGameTypes;
    }

    /**
     * 构建骰子类配置tree
     */
    public static void buildDiceTreeBySampleData() {
        log.info("构建骰子类奖励树");
        ROOT_NODES.clear();
        Map<EGameType, DiceMetaData> diceGameTypes = getDiceGameConfig();
        Map<Integer, EGameType> diceGameTypeMap =
            diceGameTypes.keySet().stream()
                .collect(HashMap::new, (map, e) -> map.put(e.getGameTypeId(), e), HashMap::putAll);
        // 游戏类型对应的位置权重列表
        Map<EGameType, List<WinPosWeightCfg>> winPosWeightCfgMap = new HashMap<>();
        for (WinPosWeightCfg weightCfg : GameDataManager.getWinPosWeightCfgList()) {
            if (diceGameTypeMap.containsKey(weightCfg.getGameID())) {
                winPosWeightCfgMap.computeIfAbsent(
                    diceGameTypeMap.get(weightCfg.getGameID()), k -> new ArrayList<>()).add(weightCfg);
            }
        }
        // build tree
        for (Map.Entry<EGameType, List<WinPosWeightCfg>> entry : winPosWeightCfgMap.entrySet()) {
            DiceTreeNode<List<WinPosWeightCfg>> diceRootNode = new DiceTreeNode<>(-1);
            ROOT_NODES.put(entry.getKey(), diceRootNode);
            for (WinPosWeightCfg weightCfg : entry.getValue()) {
                // 构建dice树
                buildDiceNodeTree(0, diceRootNode, weightCfg, diceGameTypes.get(entry.getKey()));
            }
        }
    }

    /**
     * 构建骰子tree
     */
    private static void buildDiceNodeTree(
        int pos, DiceTreeNode<List<WinPosWeightCfg>> diceNode, WinPosWeightCfg winPosWeightCfg, DiceMetaData metaData) {
        if (pos >= metaData.diceNum) {
            List<WinPosWeightCfg> data = diceNode.getData();
            if (data == null) {
                data = new ArrayList<>();
                diceNode.setData(data);
            }
            data.add(winPosWeightCfg);
            return;
        }
        // 将配置的数值进行排序，保持统一查找
        long sortedPosId = sortedNumber(winPosWeightCfg.getWinPosID());
        int posNum = getPosNum(sortedPosId, pos);
        DiceTreeNode<List<WinPosWeightCfg>> next = diceNode.getNext(posNum);
        if (next == null) {
            next = new DiceTreeNode<>(posNum);
            diceNode.addNext(posNum, next);
        }
        buildDiceNodeTree(pos + 1, next, winPosWeightCfg, metaData);
    }

    /**
     * 通过投掷的骰子，获取配置
     */
    public static List<WinPosWeightCfg> getWinPosWeightCfg(EGameType gameType, List<Integer> winPosId) {
        DiceTreeNode<List<WinPosWeightCfg>> diceTreeNode = ROOT_NODES.get(gameType);
        DiceMetaData diceMetaData = getDiceGameConfig().get(gameType);
        String sortedWinPosId = winPosId.stream()
            .sorted(Integer::compare)
            .map(String::valueOf)
            .collect(Collectors.joining(""));
        long finalWinPosId = Long.parseLong(sortedWinPosId);
        DiceTreeNode<List<WinPosWeightCfg>> next = null;
        for (int i = 0; i < diceMetaData.diceNum; i++) {
            int posNum = getPosNum(finalWinPosId, i);
            if (next == null) {
                next = diceTreeNode.getNext(posNum);
            } else {
                next = next.getNext(posNum);
            }
        }
        if (next == null) {
            log.error("找不到游戏类型：{} 开奖ID：{} 对应的开奖配置", gameType.getGameDesc(), finalWinPosId);
            return new ArrayList<>();
        }
        return next.getData();
    }

    /**
     * 将数字进行排序
     */
    public static long sortedNumber(long number) {
        String strNum = String.valueOf(number);
        List<Integer> splitNumber = Arrays.stream(strNum.split("")).map(Integer::parseInt).toList();
        String sortedWinPosId = splitNumber.stream()
            .sorted(Integer::compare)
            .map(String::valueOf)
            .collect(Collectors.joining(""));
        return Long.parseLong(sortedWinPosId);
    }

    /**
     * 获取某个数的第几位
     */
    private static int getPosNum(long num, int pos) {
        String strNum = String.valueOf(num);
        return strNum.charAt(pos) - 48;
    }
}
