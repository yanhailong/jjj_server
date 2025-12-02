package com.jjg.game.activity.wealthroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteDrawItemInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_WEALTH_ROULETTE_DETAIL_INFO, resp = true)
@ProtoDesc("财富轮盘详细信息")
public class ResWealthRouletteDetailInfo extends AbstractResponse {
    @ProtoDesc("每次抽取所需积分")
    public long drawNeedPoint;
    @ProtoDesc("今日获取的总积分")
    public long totalPoint;
    @ProtoDesc("当前积分")
    public long currentPoint;
    @ProtoDesc("可获得积分 4位小数")
    public String tomorrowPoint;
    @ProtoDesc("转盘道具信息 ")
    public List<WealthRouletteDrawItemInfo> rouletteItemInfo;

    public ResWealthRouletteDetailInfo(int code) {
        super(code);
    }
}
