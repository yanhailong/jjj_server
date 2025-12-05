package com.jjg.game.activity.wealthroulette.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.wealthroulette.message.bean.WealthRouletteGoodInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_WEALTH_ROULETTE_SHOP_INFOS,resp = true)
@ProtoDesc("财富轮盘商店信息")
public class ResWealthRouletteShopInfos extends AbstractResponse {
    @ProtoDesc("商品信息")
    public List<WealthRouletteGoodInfo> shopInfos;

    public ResWealthRouletteShopInfos(int code) {
        super(code);
    }
}
