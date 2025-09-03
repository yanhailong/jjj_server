package com.jjg.game.hall.vip.data;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AvatarCfg;
import com.jjg.game.sampledata.bean.ViplevelCfg;

import java.util.*;

/**
 * @author lm
 * @date 2025/8/28 09:55
 */
public class VipCfgCache {
    //vip等级->皮肤类型->皮肤id
    private static Map<Integer, Map<Integer, Set<Integer>>> vipSkin;
    //vip等级->vip配置
    private static Map<Integer, ViplevelCfg> viplevelCfgMap;

    public static void initData() {
        Map<Integer, Map<Integer, Set<Integer>>> tempvipSkinMap = new HashMap<>();
        Map<Integer, ViplevelCfg> tempViplevelCfgMap = new HashMap<>();
        Map<Integer, Set<Integer>> beforeHas = new HashMap<>();
        for (ViplevelCfg viplevelCfg : GameDataManager.getViplevelCfgList()) {
            Map<Integer, Set<Integer>> temp = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> entry : beforeHas.entrySet()) {
                temp.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            List<Integer> serverAvatarType = viplevelCfg.getServerAvatarType();
            if (CollectionUtil.isNotEmpty(serverAvatarType)) {
                for (Integer id : serverAvatarType) {
                    AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
                    if (Objects.nonNull(avatarCfg)) {
                        Set<Integer> before = temp.computeIfAbsent(avatarCfg.getResourceType(), k -> new HashSet<>());
                        before.add(avatarCfg.getId());
                        Set<Integer> beforeHasSet = beforeHas.computeIfAbsent(avatarCfg.getResourceType(), k -> new HashSet<>());
                        beforeHasSet.add(avatarCfg.getId());
                    }
                }
            }
            tempViplevelCfgMap.put(viplevelCfg.getViplevel(), viplevelCfg);
            tempvipSkinMap.put(viplevelCfg.getViplevel(), temp);
        }
        vipSkin = tempvipSkinMap;
        viplevelCfgMap = tempViplevelCfgMap;
    }


    public static ViplevelCfg getVipLevelCfg(int vipLevel) {
        return viplevelCfgMap.get(vipLevel);
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
