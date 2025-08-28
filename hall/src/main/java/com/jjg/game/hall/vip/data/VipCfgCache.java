package com.jjg.game.hall.vip.data;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lm
 * @date 2025/8/28 09:55
 */
@Component
public class VipCfgCache {
    private static Map<Integer, Map<Integer, Set<Integer>>> vipSkin;


    public static void initData() {
        Map<Integer, Map<Integer, Set<Integer>>> tempMap = new HashMap<>();
        Map<Integer, Set<Integer>> beforeHas = new HashMap<>();
        for (ViplevelCfg viplevelCfg : GameDataManager.getViplevelCfgList()) {
            Map<Integer, Integer> avatarType = viplevelCfg.getAvatarType();
            if (CollectionUtil.isNotEmpty(avatarType)) {
                for (Map.Entry<Integer, Integer> entry : avatarType.entrySet()) {
                    Set<Integer> before = beforeHas.computeIfAbsent(entry.getKey(), k -> new HashSet<>());
                    before.add(entry.getValue());
                }
            }
            tempMap.put(viplevelCfg.getViplevel(), new HashMap<>(beforeHas));
        }
        vipSkin = tempMap;
    }

    /**
     * 获取是否拥有vip皮肤
     *
     * @param vipLv VIP等级
     * @param type  皮肤类型
     * @param id    皮肤id
     * @return 是否拥有
     */
    public static boolean hasSkin(int vipLv, int type, int id) {
        Map<Integer, Set<Integer>> vipSkinMap = vipSkin.get(vipLv);
        if (CollectionUtil.isNotEmpty(vipSkinMap)) {
            Set<Integer> has = vipSkinMap.get(type);
            if (CollectionUtil.isNotEmpty(has)) {
                return has.contains(id);
            }
        }
        return false;
    }

    /**
     * 获取该类型对应VIP的所有皮肤id
     *
     * @param vipLv vip等级
     * @param type  皮肤类型
     * @return 皮肤id列表
     */
    public static List<Integer> getSkinsByType(int vipLv, int type) {
        Map<Integer, Set<Integer>> vipSkinMap = vipSkin.get(vipLv);
        if (CollectionUtil.isNotEmpty(vipSkinMap)) {
            if (vipSkinMap.containsKey(type)) {
                return new ArrayList<>(vipSkinMap.get(type));
            }
        }
        return null;
    }
}
