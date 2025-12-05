package com.jjg.game.hall.minigame.game.luckytreasure.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.hall.minigame.game.luckytreasure.message.bean.LuckyTreasureUpdateInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知更新夺宝奇兵库存个人纪录信息
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = LuckyTreasureConstant.Message.NOTIFY_LUCKY_TREASURE_RECORD_UPDATE, resp = true)
@ProtoDesc("通知更新夺宝奇兵库存个人纪录信息")
public class NotifyLuckyTreasureRecordUpdate extends AbstractNotice {

    /**
     * 变化的数据
     */
    @ProtoDesc("变化的数据")
    private List<LuckyTreasureUpdateInfo> updateList = new ArrayList<>();

    public List<LuckyTreasureUpdateInfo> getUpdateList() {
        return updateList;
    }

    public void setUpdateList(List<LuckyTreasureUpdateInfo> updateList) {
        this.updateList = updateList;
    }

}
