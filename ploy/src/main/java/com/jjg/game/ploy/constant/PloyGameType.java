package com.jjg.game.ploy.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.ploy.controller.AbstractPloyController;
import com.jjg.game.ploy.games.airraid.AirRaidPloyController;
import com.jjg.game.ploy.games.highlowpoker.HighLowPokerController;
import com.jjg.game.ploy.games.luckypoker.LuckyPokerPloyController;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略游戏类型
 *
 * @author 11
 * @date 2026/3/19
 */
public enum PloyGameType {
    //幸运3d
//    LUCKY_3D(CoreConst.GameType.LUCKY_3D, Lucky3dPloyController.class),
    //鸿运扑克
    LUCKY_POKER(CoreConst.GameType.LUCKY_POKER, LuckyPokerPloyController.class),
    //空袭
    AIR_STRIKE(CoreConst.GameType.AIR_STRIKE, AirRaidPloyController.class),
    //hillo
//    HILLO(CoreConst.GameType.HILLO, HilloPloyController.class),
    //高低扑克
    HIGH_LOW_POKER(CoreConst.GameType.HIGH_LOW_POKER, HighLowPokerController.class),
    ;

    //游戏类型
    private final int gameType;
    //活动控制器的class
    private final Class<? extends AbstractPloyController> controllerClass;


    //对应的游戏控制器
    private AbstractPloyController controller;
    //映射表
    private static final Map<Integer, PloyGameType> gameTypeMap = new HashMap<>();

    PloyGameType(int gameType, Class<? extends AbstractPloyController> controllerClass) {
        this.gameType = gameType;
        this.controllerClass = controllerClass;
    }

    public int getGameType() {
        return gameType;
    }

    public AbstractPloyController getController() {
        return controller;
    }

    public static void init() {
        for (PloyGameType type : values()) {
            gameTypeMap.put(type.gameType, type);
            type.controller = CommonUtil.getContext().getBean(type.controllerClass);
            type.controller.init();
        }
    }

    public static PloyGameType fromType(int gameType) {
        return gameTypeMap.get(gameType);
    }
}
