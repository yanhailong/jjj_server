package com.jjg.game.slots.game.findgoldcity.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;
import com.jjg.game.slots.game.findgoldcity.pb.bean.FindGoldCityPoolInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FIND_GOLD_CITY, cmd = FindGoldCityConstant.MsgBean.RES_FIND_GOLD_CITY_ENTER_GAME, resp = true)
@ProtoDesc("返回配置信息")
public class ResFindGoldCityEnterGame extends AbstractResponse {
    @ProtoDesc("押注列表")
    public List<Long> stakeList;
    @ProtoDesc("默认押注")
    public long defaultBet;
    @ProtoDesc("累计免费中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常 ")
    public int status;
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("奖池信息")
    public List<FindGoldCityPoolInfo> poolList;
    @ProtoDesc("当前倍数")
    public int currentMultiple;
    @ProtoDesc("剩余免费次数")
    public int remainFreeTimes;

    public ResFindGoldCityEnterGame(int code) {
        super(code);
    }
}
