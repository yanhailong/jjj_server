package com.jjg.game.hall.minigame.game.luckytreasure.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.hall.minigame.game.luckytreasure.message.bean.LuckyTreasureInfo;

import java.util.List;

/**
 * 请求夺宝奇兵详情回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = LuckyTreasureConstant.Message.RES_LUCKY_TREASURE,
        resp = true
)
@ProtoDesc("请求夺宝奇兵详情回复")
public class ResLuckyTreasureInfo extends AbstractResponse {

    /**
     * 详情列表
     */
    @ProtoDesc("详情列表")
    private List<LuckyTreasureInfo> infoList;

    /**
     * 每页条数
     */
    @ProtoDesc("每页条数")
    private int pageSize;

    /**
     * 总数量
     */
    @ProtoDesc("总数量")
    private int totalCount;

    /**
     * 总页数
     */
    @ProtoDesc("总页数")
    private int totalPage;

    /**
     * 当前页码
     */
    @ProtoDesc("当前页码")
    private int currPage;

    public ResLuckyTreasureInfo(int code) {
        super(code);
    }

    public List<LuckyTreasureInfo> getInfoList() {
        return infoList;
    }

    public void setInfoList(List<LuckyTreasureInfo> infoList) {
        this.infoList = infoList;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

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
