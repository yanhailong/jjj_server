package com.jjg.game.slots.game.steamAge.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.christmasBashNight.pb.ChristmasBashNightCascade;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.STEAM_AGE, cmd = SteamAgeConstant.MsgBean.RES_START_GAME,resp = true)
@ProtoDesc("开始游戏结果返回")
public class ResSteamAgeStartGame extends AbstractResponse {
    @ProtoDesc("图标id列表")
    public List<Integer> iconList;
    @ProtoDesc("本局中奖金币")
    public long allWinGold;
    @ProtoDesc("累计中奖金币")
    public long totalWinGold;
    @ProtoDesc("当前状态 0.正常  1.免费")
    public int status;
    @ProtoDesc("剩余免费次数")
    public int remainFreeCount;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("大奖展示  1.sweet   2.big   3.mega  4.epic  5.legendary")
    public long bigWinShow;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("中奖图标信息")
    public SteamAgeIconInfo rewardIconInfo;
    @ProtoDesc("扩列图标的信息")
    public List<SteamAgeExpand> addIconInfoList;
    @ProtoDesc("高亮坐标")
    public List<Integer> highlightList;
    @ProtoDesc("触发免费转  0.正常  1.免费")
    public int triggerStatus;


    public ResSteamAgeStartGame(int code) {
        super(code);
    }
}
