package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.RES_WEALTH_BANK_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResWealthBankStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("中奖信息")
    public List<WealthBankResultLineInfo> wealthBankResultLineInfoList;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("当前状态 0.正常  1.普通二选一  2.黄金列车二选一  3.二选一之拉普通火车  4.二选一之拉黄金火车  5.二选一之免费模式")
    public int status;
    @ProtoDesc("火车信息")
    public List<WealthBankTrainInfo> wealthBankTrainInfoList;
    @ProtoDesc("美元信息")
    public WealthBankDollarsInfo wealthBankDollarsInfo;
    @ProtoDesc("累计的美元数量，进度条")
    public int totalDollars;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("投资可选区域，只有当触发投资游戏后才会有值")
    public List<Integer> choosableAreas;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("高亮坐标")
    public List<Integer> highlightList;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;


    public ResWealthBankStartGame(int code) {
        super(code);
    }
}
