package com.jjg.game.slots.game.lianHuanDuoBao.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.SpecialModeCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoAddIconInfo;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoAwardLineInfo;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoChestReward;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 连环夺宝结果库生成器
 * <p>
 * 阶段 3 已实现：
 * <ul>
 *   <li>消除逻辑（基于父类 checkAssignPatternAward，按 BaseElementReward.lineType=7 配置的 rewardNum 判定）</li>
 *   <li>cascade 递归：消除 → 上方下落 → roller 补图标 → 检查新一轮消除</li>
 *   <li>钥匙图标特殊判定：在 BaseElementReward 里没配 rewardNum，但任何 cluster 大小都算"中奖被消除"，
 *       通过 assignPatternAwardSpecialCheck / hasAssignPatternSpecialCheck hook 实现</li>
 *   <li>本局钥匙累计数（用于关卡切换判定，切换逻辑在 GameManager 层做）</li>
 *   <li>本局 cascade 次数（用于"连续消除 5 次额外送 1 龙珠"）</li>
 * </ul>
 * 待补（阶段 4+）：宝箱奖励、龙珠掉落规则、聚宝盆抽水、bonus 小游戏
 *
 * @author lm
 * @date 2026/6/2
 */
//TODO 连环夺宝配表完成后取消注释即可启用
@Component
public class LianHuanDuoBaoGameGenerateManager extends AbstractSlotsGenerateManager<LianHuanDuoBaoAwardLineInfo, LianHuanDuoBaoResultLib> {

    /**
     * 过关图标（钥匙）配置：layer → (iconId, needCount)
     * 由 SpecialPlay PASSING_CRITERIA_ID 加载，格式：layer_iconId_needCount|...
     */
    private Map<Integer, Pair<Integer, Integer>> passingCriteriaMap = Map.of();

    /**
     * 连续消除多少次额外送 1 颗龙珠（从 SpecialPlay CASCADE_DRAGON_BALL_ID 配置加载），默认 5。
     */
    private int cascadeDragonBallThreshold = 5;

    /**
     * 宝箱掉龙珠的万分比（默认 2000 = 20%）。
     */
    private int chestDragonBallProp = 2000;

    /**
     * 宝箱奖励倍数权重表 [倍数, 权重]。例：[[1,3000],[2,2500],[3,2000],[5,1500],[10,1000]]
     */
    private List<int[]> chestRewardWeights = new ArrayList<>();
    private int chestRewardWeightTotal = 0;

    public LianHuanDuoBaoGameGenerateManager() {
        super(LianHuanDuoBaoResultLib.class);
    }

    @Override
    protected LianHuanDuoBaoAwardLineInfo getAwardLineInfo() {
        return new LianHuanDuoBaoAwardLineInfo();
    }

    /**
     * 启动时加载本游戏的 SpecialPlay 配置
     */
    @Override
    protected void specialPlayConfig() {
        loadPassingCriteriaConfig();
        loadCascadeDragonBallConfig();
        loadChestDragonBallPropConfig();
        loadChestRewardWeightsConfig();
    }

    /**
     * 加载过关图标（钥匙）配置。
     * 配置 ID = SpecialPlay.PASSING_CRITERIA_ID（5052001）。
     * value 格式：layer_iconId_needCount|layer_iconId_needCount|...
     * 例：1_16_15|2_17_15|3_18_15
     */
    private void loadPassingCriteriaConfig() {
        SpecialPlayCfg cfg = GameDataManager.getSpecialPlayCfg(LianHuanDuoBaoConstant.SpecialPlay.PASSING_CRITERIA_ID);
        if (cfg == null || StringUtil.isEmpty(cfg.getValue())) {
            passingCriteriaMap = Map.of();
            return;
        }
        String[] entries = StringUtils.split(cfg.getValue(), "|");
        Map<Integer, Pair<Integer, Integer>> tmpMap = new HashMap<>(entries.length);
        for (String entry : entries) {
            String[] parts = StringUtils.split(entry, "_");
            if (parts.length != 3) {
                log.warn("过关图标配置格式错误，跳过：{}", entry);
                continue;
            }
            try {
                int layer = Integer.parseInt(parts[0]);
                int iconId = Integer.parseInt(parts[1]);
                int needCount = Integer.parseInt(parts[2]);
                tmpMap.put(layer, Pair.newPair(iconId, needCount));
            } catch (NumberFormatException e) {
                log.warn("过关图标配置数值错误：{}", entry, e);
            }
        }
        this.passingCriteriaMap = tmpMap;
        log.info("连环夺宝过关图标配置加载完成 {}", tmpMap);
    }

    /**
     * 加载"连续消除 N 次额外送龙珠"的阈值配置。
     * 配置 ID = SpecialPlay.CASCADE_DRAGON_BALL_ID（5052003）。
     * value 直接是阈值数字（如 "5"），缺省按文档 [12] 用 5。
     */
    private void loadCascadeDragonBallConfig() {
        SpecialPlayCfg cfg = GameDataManager.getSpecialPlayCfg(LianHuanDuoBaoConstant.SpecialPlay.CASCADE_DRAGON_BALL_ID);
        if (cfg == null || StringUtil.isEmpty(cfg.getValue())) {
            return;
        }
        try {
            int v = Integer.parseInt(cfg.getValue().trim());
            if (v > 0) {
                this.cascadeDragonBallThreshold = v;
            }
        } catch (NumberFormatException e) {
            log.warn("连续消除送龙珠阈值配置数值错误：{}", cfg.getValue(), e);
        }
    }

    /**
     * 加载宝箱掉龙珠概率配置。
     * 配置 ID = SpecialPlay.CHEST_DRAGON_BALL_PROP_ID（5052002）。
     * value 直接是万分比数字（例 "2000" = 20%）。
     */
    private void loadChestDragonBallPropConfig() {
        SpecialPlayCfg cfg = GameDataManager.getSpecialPlayCfg(LianHuanDuoBaoConstant.SpecialPlay.CHEST_DRAGON_BALL_PROP_ID);
        if (cfg == null || StringUtil.isEmpty(cfg.getValue())) {
            return;
        }
        try {
            int v = Integer.parseInt(cfg.getValue().trim());
            if (v >= 0 && v <= 10000) {
                this.chestDragonBallProp = v;
            }
        } catch (NumberFormatException e) {
            log.warn("宝箱掉龙珠概率配置数值错误：{}", cfg.getValue(), e);
        }
    }

    /**
     * 加载宝箱奖励倍数权重配置。
     * 配置 ID = SpecialPlay.CHEST_REWARD_PROP_ID（5052005）。
     * value 格式：倍数_权重|倍数_权重|...，例 1_3000|2_2500|3_2000|5_1500|10_1000。
     * 最大倍数 ≤ 10（文档 [11] 宝箱奖金最大押注 10x）。
     */
    private void loadChestRewardWeightsConfig() {
        SpecialPlayCfg cfg = GameDataManager.getSpecialPlayCfg(LianHuanDuoBaoConstant.SpecialPlay.CHEST_REWARD_PROP_ID);
        if (cfg == null || StringUtil.isEmpty(cfg.getValue())) {
            //默认配置兜底，避免配表没补时跑空
            chestRewardWeights = new ArrayList<>();
            chestRewardWeights.add(new int[]{1, 3000});
            chestRewardWeights.add(new int[]{2, 2500});
            chestRewardWeights.add(new int[]{3, 2000});
            chestRewardWeights.add(new int[]{5, 1500});
            chestRewardWeights.add(new int[]{10, 1000});
            chestRewardWeightTotal = 10000;
            return;
        }
        String[] entries = StringUtils.split(cfg.getValue(), "|");
        List<int[]> tmp = new ArrayList<>(entries.length);
        int total = 0;
        for (String entry : entries) {
            String[] parts = StringUtils.split(entry, "_");
            if (parts.length != 2) {
                log.warn("宝箱奖励配置格式错误，跳过：{}", entry);
                continue;
            }
            try {
                int times = Integer.parseInt(parts[0]);
                int weight = Integer.parseInt(parts[1]);
                if (weight > 0) {
                    tmp.add(new int[]{times, weight});
                    total += weight;
                }
            } catch (NumberFormatException e) {
                log.warn("宝箱奖励配置数值错误：{}", entry, e);
            }
        }
        chestRewardWeights = tmp;
        chestRewardWeightTotal = total;
        log.info("连环夺宝宝箱奖励权重加载完成 size={}, total={}", tmp.size(), total);
    }

    public Map<Integer, Pair<Integer, Integer>> getPassingCriteriaMap() {
        return passingCriteriaMap == null ? Map.of() : passingCriteriaMap;
    }

    public int getCascadeDragonBallThreshold() {
        return cascadeDragonBallThreshold;
    }

    // -------------------------------------------------------------------------
    // 阶段 4：宝箱开启 & 奖励
    // -------------------------------------------------------------------------

    /**
     * 根据关卡 + 已开启数，给出"第 nthOpen 次开启时" UI 上要被翻开的宝箱位置。
     * <ul>
     *   <li>第一、二关：从上往下，位置 = nthOpen（1..15）</li>
     *   <li>第三关：中间起，左右交替。15 个位置编号 1..15，中心为 8，
     *       序列为 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15</li>
     * </ul>
     */
    public int chestPositionForOpenIndex(int layer, int nthOpen) {
        if (nthOpen <= 0) {
            return 0;
        }
        if (layer < LianHuanDuoBaoConstant.Common.MAX_LAYER) {
            return nthOpen;
        }
        //第三关：中间起，左右交替
        int center = (LianHuanDuoBaoConstant.Common.KEYS_PER_LAYER + 1) / 2; //15 → 8
        if (nthOpen == 1) {
            return center;
        }
        int offset = nthOpen / 2;
        boolean isLeft = (nthOpen % 2 == 0);
        return isLeft ? center - offset : center + offset;
    }

    /**
     * 随机选一个宝箱奖励倍数（按 chestRewardWeights 权重抽）
     */
    private int randomChestRewardTimes() {
        if (chestRewardWeights.isEmpty() || chestRewardWeightTotal <= 0) {
            return 1;
        }
        int r = RandomUtils.randomMinMax(0, chestRewardWeightTotal - 1);
        int acc = 0;
        for (int[] entry : chestRewardWeights) {
            acc += entry[1];
            if (r < acc) {
                return entry[0];
            }
        }
        return chestRewardWeights.get(chestRewardWeights.size() - 1)[0];
    }

    /**
     * 给定本局收集到的钥匙数 + 玩家在当前关卡已开启的宝箱数，生成 N 把钥匙对应的宝箱奖励列表。
     * 每把钥匙开一个宝箱：倍数随机 + 是否掉龙珠按 prop 抽。
     *
     * @param keyCount               本局收集到的钥匙数
     * @param layer                  当前关卡
     * @param alreadyOpenedInLayer   本关卡之前已经开了几个宝箱（玩家状态）
     * @return 本局开出的宝箱奖励列表
     */
    public List<LianHuanDuoBaoChestReward> openChests(int keyCount, int layer, int alreadyOpenedInLayer) {
        if (keyCount <= 0) {
            return Collections.emptyList();
        }
        List<LianHuanDuoBaoChestReward> result = new ArrayList<>(keyCount);
        int remainSlots = LianHuanDuoBaoConstant.Common.KEYS_PER_LAYER - alreadyOpenedInLayer;
        //本局开宝箱不能超过本关剩余槽位
        int actualOpen = Math.min(keyCount, remainSlots);
        for (int i = 0; i < actualOpen; i++) {
            LianHuanDuoBaoChestReward reward = new LianHuanDuoBaoChestReward();
            int nthOpen = alreadyOpenedInLayer + i + 1;
            reward.setChestIndex(chestPositionForOpenIndex(layer, nthOpen));
            reward.setRewardTimes(randomChestRewardTimes());
            //是否掉龙珠
            int r = RandomUtils.randomMinMax(0, 9999);
            reward.setDropDragonBall(r < chestDragonBallProp);
            result.add(reward);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // 钥匙图标特殊处理：钥匙没有 lineType=7 的 BaseElementReward 配置，
    // 但盘面上出现就要消除并累计。复用父类 specialCheck 的 hook。
    // -------------------------------------------------------------------------

    @Override
    public boolean assignPatternAwardSpecialCheck(Set<Integer> targetCounts, int icon, int size) {
        if (isPassingIcon(icon)) {
            //钥匙：任何 cluster 都算消除一次
            return size >= 1;
        }
        return false;
    }

    @Override
    protected boolean hasAssignPatternSpecialCheck(int icon) {
        return isPassingIcon(icon);
    }

    private boolean isPassingIcon(int icon) {
        if (passingCriteriaMap == null || passingCriteriaMap.isEmpty()) {
            return false;
        }
        for (Pair<Integer, Integer> pair : passingCriteriaMap.values()) {
            if (pair != null && pair.getFirst() != null && pair.getFirst() == icon) {
                return true;
            }
        }
        return false;
    }

    /**
     * 钥匙图标没有 BaseElementReward 配置，没有 bet 倍数；其它图标按表查。
     */
    @Override
    protected int resolveAssignPatternBet(int icon, int count) {
        Map<Integer, BaseElementRewardCfg> rewardCfgMap = this.assignPatternRewardCfgMap.get(icon);
        if (CollectionUtil.isEmpty(rewardCfgMap)) {
            //钥匙等没有赔率配置的图标 bet=0，被消除但不加金币
            return 0;
        }
        BaseElementRewardCfg rewardCfg = rewardCfgMap.get(count);
        if (rewardCfg == null) {
            //cluster 大小超过最大配置档（如 4x4 关里出现 7 连），使用最大档的 bet
            int maxCount = -1;
            for (Integer c : rewardCfgMap.keySet()) {
                if (c > maxCount) {
                    maxCount = c;
                }
            }
            rewardCfg = rewardCfgMap.get(maxCount);
        }
        return rewardCfg == null ? 0 : rewardCfg.getBet();
    }

    // -------------------------------------------------------------------------
    // 主流程
    // -------------------------------------------------------------------------

    @Override
    public LianHuanDuoBaoResultLib checkAward(int[] arr, LianHuanDuoBaoResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //通过 libType 反查矩阵大小
        Integer libType = lib.getLibTypeSet() != null && !lib.getLibTypeSet().isEmpty()
                ? lib.getLibTypeSet().iterator().next() : null;
        SpecialModeCfg specialModeCfg = libType == null ? null : specialModeCfgMap.get(libType);
        if (specialModeCfg == null) {
            log.warn("连环夺宝 checkAward 找不到 specialModeCfg，libType={}", libType);
            calTimes(lib);
            return lib;
        }
        int rows = specialModeCfg.getRows();
        int cols = specialModeCfg.getCols();

        //首次中奖判定（基于父类 checkAssignPatternAward，按 BaseElementReward.lineType=7 + 钥匙 hook）
        List<LianHuanDuoBaoAwardLineInfo> awardLineInfoList = checkAssignPatternAward(arr, rows, cols);
        lib.addAllAwardLineInfo(awardLineInfoList);

        //本次 spin 整局可能用到的关卡（按 libType 推断）：libType=1/2/3 直接对应 layer
        int layer = inferLayer(libType);
        lib.setLayerNumber(layer);

        //cascade
        List<LianHuanDuoBaoAddIconInfo> addIconInfoList = new ArrayList<>();
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        repairIcons(cols, rows, newArr, lib.getAwardLineInfoList(), addIconInfoList, lib.getRollerMode());
        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }

        //累计本局收集到的钥匙数：把所有 cascade（含首次）里 sameIcon==该关钥匙图标 的中奖线 sameIconSet.size() 累加
        Pair<Integer, Integer> passingCfg = passingCriteriaMap.get(layer);
        if (passingCfg != null) {
            int keyCount = 0;
            int keyIconId = passingCfg.getFirst();
            //首次中奖
            if (CollectionUtil.isNotEmpty(lib.getAwardLineInfoList())) {
                for (LianHuanDuoBaoAwardLineInfo info : lib.getAwardLineInfoList()) {
                    if (info.getSameIcon() == keyIconId && info.getSameIconSet() != null) {
                        keyCount += info.getSameIconSet().size();
                    }
                }
            }
            //cascade
            if (CollectionUtil.isNotEmpty(lib.getAddIconInfos())) {
                for (LianHuanDuoBaoAddIconInfo addInfo : lib.getAddIconInfos()) {
                    if (CollectionUtil.isEmpty(addInfo.getAwardLineInfoList())) {
                        continue;
                    }
                    for (LianHuanDuoBaoAwardLineInfo info : addInfo.getAwardLineInfoList()) {
                        if (info.getSameIcon() == keyIconId && info.getSameIconSet() != null) {
                            keyCount += info.getSameIconSet().size();
                        }
                    }
                }
            }
            lib.setKeyCollectionNum(keyCount);
        }

        //连续消除 N 次额外送 1 颗龙珠：cascade 次数 = 首次中奖 + addIconInfos 中含中奖的那些
        int cascadeCount = 0;
        if (CollectionUtil.isNotEmpty(lib.getAwardLineInfoList())) {
            cascadeCount++;
        }
        if (CollectionUtil.isNotEmpty(lib.getAddIconInfos())) {
            for (LianHuanDuoBaoAddIconInfo addInfo : lib.getAddIconInfos()) {
                if (CollectionUtil.isNotEmpty(addInfo.getAwardLineInfoList())) {
                    cascadeCount++;
                }
            }
        }
        if (cascadeDragonBallThreshold > 0 && cascadeCount >= cascadeDragonBallThreshold) {
            lib.setCascadeDragonBalls(cascadeCount / cascadeDragonBallThreshold);
        }

        calTimes(lib);
        return lib;
    }

    private int inferLayer(Integer libType) {
        if (libType == null) {
            return 1;
        }
        for (Map.Entry<Integer, Integer> en : LianHuanDuoBaoConstant.SpecialMode.NORMAL_MAP.entrySet()) {
            if (Objects.equals(en.getValue(), libType)) {
                return en.getKey();
            }
        }
        return 1;
    }

    // -------------------------------------------------------------------------
    // Cascade 消除 + 下落 + 顶部补充
    // 直接借用糖果派对的实现（已验证可用），核心逻辑：
    //   1. 按列汇总要消除的格子
    //   2. 每列从底往上保留 kept 图标，顶部空缺由 roller 补
    //   3. 补完后 fullLine 检查新一轮中奖，有就递归
    // -------------------------------------------------------------------------

    public void repairIcons(int cols, int rows, int[] arr,
                            List<LianHuanDuoBaoAwardLineInfo> list,
                            List<LianHuanDuoBaoAddIconInfo> addIconInfoList, int rollerMode) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        LianHuanDuoBaoAddIconInfo addIconInfo = new LianHuanDuoBaoAddIconInfo();
        //按列汇总要消除的格子
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (LianHuanDuoBaoAwardLineInfo info : list) {
            if (CollectionUtil.isEmpty(info.getSameIconSet())) {
                continue;
            }
            for (Integer index : info.getSameIconSet()) {
                int columnId = index / rows;
                if ((index % rows) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);
            }
        }
        if (CollectionUtil.isEmpty(allSameMap)) {
            return;
        }
        Map<Integer, Integer> addIconMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            processIcons(rows, en.getKey(), en.getValue(), arr, addIconMap, rollerMode);
        }
        addIconInfo.setAddIconMap(addIconMap);

        //检查新一轮中奖
        List<LianHuanDuoBaoAwardLineInfo> newAwardInfoList = checkAssignPatternAward(arr, rows, cols);
        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(cols, rows, arr, newAwardInfoList, addIconInfoList, rollerMode);
    }

    /**
     * 单列处理：消除 → 剩余图标下落 → 顶部 roller 补充
     */
    public void processIcons(int rows, int colIndex, Set<Integer> removedIndexes, int[] arr,
                             Map<Integer, Integer> addIconMap, int rollerMode) {
        int beginIndex = (colIndex - 1) * rows + 1;
        int endIndex = beginIndex + rows - 1;

        //保留下来的图标按顶到底顺序
        List<Integer> validIndexes = new ArrayList<>(rows);
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (!removedIndexes.contains(i)) {
                validIndexes.add(icon);
            }
            arr[i] = -1;
        }
        //反转后从列底回填，模拟"上方图标下落"
        validIndexes = validIndexes.reversed();
        int curIndex = endIndex;
        for (Integer v : validIndexes) {
            arr[curIndex--] = v;
        }

        //顶部空位从 roller 补
        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.get(rollerMode);
        if (rollerCfgMap == null) {
            log.warn("找不到 rollerMode={} 的 roller 配置", rollerMode);
            return;
        }
        BaseRollerCfg baseRollerCfg = rollerCfgMap.get(colIndex);
        if (baseRollerCfg == null || baseRollerCfg.getAxleCountScope() == null
                || baseRollerCfg.getAxleCountScope().size() < 2) {
            log.warn("找不到 col={} 的 roller 配置或范围", colIndex);
            return;
        }
        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        for (int i = 0; i < rows; i++) {
            if (scopeIndex > last) {
                scopeIndex = first;
            }
            int index = beginIndex + i;
            if (arr[index] > 0) {
                continue;
            }
            int elementId = baseRollerCfg.getElements().get(scopeIndex);
            arr[index] = elementId;
            addIconMap.put(index, elementId);
            scopeIndex++;
        }
    }

    // -------------------------------------------------------------------------
    // 倍数累加
    // -------------------------------------------------------------------------

    @Override
    public void calTimes(LianHuanDuoBaoResultLib lib) throws Exception {
        //首次中奖
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //cascade 中奖
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
    }

    public long calLineTimes(List<LianHuanDuoBaoAwardLineInfo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }
        long times = 0;
        for (LianHuanDuoBaoAwardLineInfo info : list) {
            times += info.getBaseTimes();
        }
        return times;
    }

    public long calAfterAddIcons(List<LianHuanDuoBaoAddIconInfo> addIconInfos) {
        if (CollectionUtil.isEmpty(addIconInfos)) {
            return 0;
        }
        long times = 0;
        for (LianHuanDuoBaoAddIconInfo info : addIconInfos) {
            if (CollectionUtil.isEmpty(info.getAwardLineInfoList())) {
                continue;
            }
            for (LianHuanDuoBaoAwardLineInfo line : info.getAwardLineInfoList()) {
                times += line.getBaseTimes();
            }
        }
        return times;
    }
}
