package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2026/1/19
 */
public enum ReloadType {
    SMS_CONFIG(1),
    COMMON_CONFIG(2),
    ;

    private int value;

    ReloadType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ReloadType valueOf(int value) {
        for(ReloadType type : ReloadType.values()){
            if(type.getValue() == value){
                return type;
            }
        }
        return null;
    }
}
