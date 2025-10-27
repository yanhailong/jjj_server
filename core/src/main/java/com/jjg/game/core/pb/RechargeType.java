package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/17 19:53
 */
@ProtobufMessage
public enum RechargeType {
    /**
     * 商城
     */
    @ProtoDesc("商城")
    SHOP(1),

    /**
     * 等级礼包
     */
    @ProtoDesc("等级礼包")
    PLAYER_LEVEL_GIFT(2)

    ;
    private final int type;

    RechargeType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static RechargeType valueOf(int type) {
        switch (type) {
            case 1 -> {
                return SHOP;
            }
            case 2 -> {
                return PLAYER_LEVEL_GIFT;
            }
            default -> {
                return null;
            }
        }
    }
}
