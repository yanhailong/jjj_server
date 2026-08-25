package com.jjg.game.sim.tools;

import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.pb.struct.BuildingTipArgs;
import com.jjg.game.sim.pb.struct.BuildingTips;

import java.util.ArrayList;
import java.util.Map;

public class SimTool {
    public static BuildingTips buildTips(int languageId, String... args) {
        BuildingTips tips = new BuildingTips();
        tips.languageId = languageId;

        tips.tipArgs = new ArrayList<>();
        for (String arg : args) {
            BuildingTipArgs tipArgs = new BuildingTipArgs();
            tipArgs.setType(2);
            tipArgs.setArg(String.valueOf(arg));
            tips.tipArgs.add(tipArgs);
        }
        return tips;
    }

    public static BuildingTips buildTips(int languageId, Map<Integer, Long> itemMap) {
        BuildingTips tips = new BuildingTips();
        tips.languageId = languageId;

        if (itemMap != null && !itemMap.isEmpty()) {
            tips.tipArgs = new ArrayList<>();
            BuildingTipArgs tipArgs = new BuildingTipArgs();
            tipArgs.setType(3);

            tipArgs.items = ItemUtils.buildItemInfo(itemMap);
            tips.tipArgs.add(tipArgs);
        }
        return tips;
    }

    public static BuildingTips buildTips(int languageId, int paramLanguageId, String... args) {
        BuildingTips tips = new BuildingTips();
        tips.languageId = languageId;

        tips.tipArgs = new ArrayList<>();
        BuildingTipArgs tipArgs1 = new BuildingTipArgs();
        tipArgs1.setType(1);
        tipArgs1.setArg(String.valueOf(paramLanguageId));
        tips.tipArgs.add(tipArgs1);

        for (String arg : args) {
            BuildingTipArgs tipArgs = new BuildingTipArgs();
            tipArgs.setType(2);
            tipArgs.setArg(String.valueOf(arg));
            tips.tipArgs.add(tipArgs);
        }
        return tips;
    }
}
