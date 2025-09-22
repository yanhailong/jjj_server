package com.jjg.game.hall.minigame.game.luckytreasure.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.LuckyTreasureConstant;

/**
 * 请求夺宝奇兵详情
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.REQ_LUCKY_TREASURE
)
@ProtoDesc("请求夺宝奇兵详情")
public class ReqLuckyTreasureInfo extends AbstractMessage {

    /**
     * 当前页码
     */
    @ProtoDesc("当前页码")
    private int currPage;

    /**
     * 每页条数
     */
    @ProtoDesc("每页条数 最大20")
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
