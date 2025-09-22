package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 夺宝奇兵配置更新
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_ALL_UPDATE_LUCKY_TREASURE, resp = true, toPbFile = false)
public class LuckyTreasureConfigUpdate extends AbstractNotice {

    /**
     * 1=更新,2=删除
     */
    private int type;

    /**
     * 变化的数据,均为LuckyTreasureConfig的json字符串
     */
    List<String> jsonList = new ArrayList<>();

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<String> getJsonList() {
        return jsonList;
    }

    public void setJsonList(List<String> jsonList) {
        this.jsonList = jsonList;
    }
}
