package com.jjg.game.slots.game.wealthgod.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 图标更改信息类，用于存储图标更改的相关数据。
 */
@ProtobufMessage
@ProtoDesc("图标更改信息")
public class WealthGodIconChangeInfo {

    /**
     * 变化的位置索引
     */
    @ProtoDesc("变化的位置索引")
    private int index;

    /**
     * 结果icon
     */
    @ProtoDesc("结果icon")
    private int icon;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
