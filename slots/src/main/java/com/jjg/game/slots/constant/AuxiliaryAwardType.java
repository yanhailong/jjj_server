package com.jjg.game.slots.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/5 13:39
 */
public enum AuxiliaryAwardType {
    //金币
    GOLD(0),
    //免费次数
    FREE_GAME_COUNT(1),

    //指定滚轴
    APPOINT_ROLLER(6),

    //收集金币
    COLLOCT_GOLD(13),

    //金币系数
    GOLD_PROP(16),
    //奖励小游戏
    REWARD_MINI_GAME(18),
    //重转次数
    SPIN_COUNT_AGAIN(19);
    
    private int type;

    AuxiliaryAwardType(int type){
        this.type = type;
    }

    public int getType() {
        return type;
    }

    private static final Map<Integer, AuxiliaryAwardType> map = new HashMap<>();

    public static AuxiliaryAwardType getType(int type){
        if(map.isEmpty()){
            for(AuxiliaryAwardType t : values()){
                map.put(t.type, t);
            }
        }
        return map.get(type);
    }
}
