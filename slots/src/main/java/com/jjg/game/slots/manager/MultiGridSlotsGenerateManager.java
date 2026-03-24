package com.jjg.game.slots.manager;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementCfg;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.SpecialGirdCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.utils.SlotsUtil;

import java.util.*;

/**
 * 多格子结果集生成器
 *
 * @author
 * @date 2025/7/2 13:54
 */
public abstract class MultiGridSlotsGenerateManager<A extends AwardLineInfo, T extends SlotsResultLib<A>> extends AbstractSlotsGenerateManager<A, T> {

    private Map<Integer, BaseElementCfg> baseElementCfgMap = Map.of();

    public MultiGridSlotsGenerateManager(Class<T> resultLibClazz) {
        super(resultLibClazz);
    }

    @Override
    protected void baseElementConfig() {
        Map<Integer, Set<Integer>> tmpIconMap = new HashMap<>();
        Map<Integer, BaseElementCfg> tmpCfgMap = new HashMap<>();
        Map<Integer, PropInfo> tmpBaseElementPostChangeMap = new HashMap<>();

        for (Map.Entry<Integer, BaseElementCfg> en : GameDataManager.getBaseElementCfgMap().entrySet()) {
            BaseElementCfg cfg = en.getValue();
            if (cfg.getGameId() != this.gameType) {
                continue;
            }
            tmpCfgMap.put(cfg.getElementId(), cfg);
            tmpIconMap.computeIfAbsent(cfg.getType(), k -> new HashSet<>()).add(cfg.getElementId());

            if (cfg.getPostChangeElementId() != null && !cfg.getPostChangeElementId().isEmpty()) {
                tmpBaseElementPostChangeMap.put(cfg.getElementId(), SlotsUtil.converMapToPropInfo(cfg.getPostChangeElementId()));
            }
        }
        baseElementCfgMap = tmpCfgMap;
        this.iconsMap = tmpIconMap;
        this.baseElementPostChangeMap = tmpBaseElementPostChangeMap;
    }

    public Map<Integer, BaseElementCfg> getBaseElementCfgMap() {
        return baseElementCfgMap;
    }

    /**
     * 是否可以进行替换(空间检查,替换元素检查)
     *
     * @return
     */
    public boolean canDoReplace(BaseInitCfg baseInitCfg, SpecialGirdCfg specialGirdCfg, int[] arrIcon, int needRow, int girdId) {
        //计算格子所在行信息,判断上面的元素是否能替换，以及是否有足够多的元素支持替换
        int rows = baseInitCfg.getRows();
        int currentRow = (girdId - 1) % rows + 1;
        if (currentRow < needRow) {
            return false;
        }
        for (int i = 1; i < needRow; i++) {
            int tempIcon = arrIcon[girdId - i];
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(tempIcon)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据给定的配置ID和图标数组更新特殊格子信息。
     *
     * @param lib   结果库实例
     * @param cfgId specialGirdCfg的配置id
     * @param arr   图标数组
     * @return 更新后的SpecialGirdInfo对象
     */
    protected SpecialGirdInfo gridUpdate(T lib, int cfgId, int[] arr) {
        return gridUpdate(cfgId, arr);
    }

    /**
     * 格子修改
     *
     * @param cfgId specialGirdCfg的配置id
     * @param arr   图标数组
     * @return
     */
    public SpecialGirdInfo gridUpdate(int cfgId, int[] arr) {
        log.debug("开始修改格子 specialGirdCfgId = {}", cfgId);
        SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(cfgId);
        if (specialGirdCfg == null) {
            log.debug("修改格子未找到对应的配置 cfgId = {}", cfgId);
            return null;
        }

        GirdUpdatePropConfig girdUpdatePropConfig = this.specialGirdCfgMap.get(cfgId);
        if (girdUpdatePropConfig == null) {
            log.debug("修改格子未找到计算后的权重信息 cfgId = {}", cfgId);
            return null;
        }

        if (girdUpdatePropConfig.getRandCountPropInfo() == null) {
            log.debug("修改格子未找到计算后的随机次数权重信息 cfgId = {}", cfgId);
            return null;
        }

        //获取随机次数
        Integer randCount = girdUpdatePropConfig.getRandCountPropInfo().getRandKey();
        if (randCount == null || randCount < 1) {
            return null;
        }

        log.debug("获取到随机次数 cfgId = {},randCount = {}", cfgId, randCount);
        //因为有最大次数限制，所以先clone
        PropInfo cloneAffectGirdPropInfo = girdUpdatePropConfig.getAffectGirdPropInfo().clone();
        //出现的次数记录
        Map<Integer, Integer> girdShowMap = new HashMap<>();

        SpecialGirdInfo info = new SpecialGirdInfo();
        info.setCfgId(specialGirdCfg.getId());

        //记录实际修改格子的次数
        int x = 0;
        int maxForCount = arr.length * 2;
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(gameType);
        if (baseInitCfg == null) {
            return null;
        }
        for (int i = 0; i < maxForCount; i++) {
            //获取一个需要替换的格子
            Integer girdId = cloneAffectGirdPropInfo.getRandKey();
            if (girdId == null) {
                log.debug("获取一个需要替换的格子失败");
                break;
            }
            girdShowMap.merge(girdId, 1, Integer::sum);

            //该格子上的图标id
            int icon = arr[girdId];
            //检查该格子是否不可替换
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(icon)) {
                continue;
            }
            //随机一个需要出现的图标
            Integer newIcon = girdUpdatePropConfig.getShowIconPropInfo().getRandKey();
            if (newIcon == null) {
                log.debug("随机一个需要出现的图标失败");
                break;
            }
            //获取该格子的信息
            BaseElementCfg baseElementCfg = baseElementCfgMap.get(newIcon);
            if (baseElementCfg == null) {
                continue;
            }
            //判断当前元素是否处于可替换的状态
            //往上替换
            if (baseElementCfg.getSpace() > 1) {
                //检查是否有不可变元素
                if (!canDoReplace(baseInitCfg, specialGirdCfg, arr, baseElementCfg.getSpace(), girdId)) {
                    continue;
                }
                for (int j = 1; j < baseElementCfg.getSpace(); j++) {
                    arr[girdId - j] = SlotsConst.Common.PLACEHOLDER_ELEMENTS;
                }
            }
            log.debug("修改格子 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);
            arr[girdId] = newIcon;

            //赋值
            if (girdUpdatePropConfig.getValuePropInfo() != null) {
                Integer value = girdUpdatePropConfig.getValuePropInfo().getRandKey();
                if (value == null) {
                    log.debug("修改图标后赋值失败 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);
                    break;
                }
                info.addValue(girdId, value);
                log.debug("赋值 girdId = {}, value = {}", girdId, value);
            }

            //达到最大次数限制后，移除
            if (girdShowMap.get(girdId) >= cloneAffectGirdPropInfo.getMaxShowLimit(girdId)) {
                cloneAffectGirdPropInfo.removeKeyAndRecalculate(girdId);
            }

            x++;
            if (x >= randCount) {
                break;
            }
        }

        //值类型
        if (specialGirdCfg.getValueType() != null && !specialGirdCfg.getValueType().isEmpty()) {
            info.setValueType(specialGirdCfg.getValueType().get(0));
            info.setMiniGameId(specialGirdCfg.getValueType().get(1));
        }

        log.debug("修改后的图标 arr = {}", Arrays.toString(arr));
        return info;
    }


}
