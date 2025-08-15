package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/6 15:54
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_QUERY_PLAYER_INFO,resp = true)
@ProtoDesc("玩家信息返回")
public class ResQueryPlayerInfo extends AbstractResponse {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("创建时间")
    public int createTime;
    @ProtoDesc("性别")
    public int gender;
    @ProtoDesc("vip等级")
    public int vipLevel;
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("钻石")
    public long diamond;
    @ProtoDesc("保险箱金币")
    public long safeBoxGold;
    @ProtoDesc("保险箱钻石")
    public long safeBoxDiamond;
    @ProtoDesc("手机号")
    public String phoneNumber;
    @ProtoDesc("邮箱")
    public String email;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("国旗id")
    public int nationalId;
    @ProtoDesc("称号id")
    public int titleId;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;


    public ResQueryPlayerInfo(int code) {
        super(code);
    }
}
