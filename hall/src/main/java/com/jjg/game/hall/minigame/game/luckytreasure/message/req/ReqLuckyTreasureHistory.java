package com.jjg.game.hall.minigame.game.luckytreasure.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.luckytreasure.constant.LuckyTreasureConstant;

/**
 * 请求查看历史开奖记录
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.REQ_LUCKY_TREASURE_AWARD_HISTORY
)
@ProtoDesc("请求查看历史开奖记录")
public class ReqLuckyTreasureHistory extends AbstractMessage {

    /**
     * 页码
     */
    @ProtoDesc("页码")
    private int currPage;

    /**
     * 每页条数
     */
    @ProtoDesc("每页条数")
    private int pageSize;

    public int getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

}
