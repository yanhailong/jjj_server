package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.data.Carousel;
import com.jjg.game.core.pb.MarqueeInfo;
import com.jjg.game.hall.pb.struct.GameListConfig;
import com.jjg.game.hall.pb.struct.GameWareInfo;

import java.util.List;

/**
 * @author 11
 * @since 2025/5/26 15:22
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CERTIFY_MESSAGE_TYPE, cmd = MessageConst.CertifyMessage.RES_LOGIN, resp = true)
@ProtoDesc("登录返回")
public class ResLogin extends AbstractResponse {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nickName;
    @ProtoDesc("性别")
    public int gender;
    @ProtoDesc("金币")
    public long gold;
    @ProtoDesc("钻石")
    public long diamond;
    @ProtoDesc("vip等级")
    public int vipLevel;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("国旗id")
    public int nationalId;
    @ProtoDesc("称号id")
    public int titleId;
    @ProtoDesc("当前使用的筹码id")
    public int chipsId;
    @ProtoDesc("当前使用的背景id")
    public int backgroundId;
    @ProtoDesc("当前使用的牌背ID")
    public int cardBackgroundId;
    @ProtoDesc("游戏列表")
    public List<GameListConfig> gameList;
    @ProtoDesc("跑马灯信息")
    public MarqueeInfo marqueeInfo;
    @ProtoDesc("保险箱金币")
    public long safeBoxGold;
    @ProtoDesc("保险箱钻石")
    public long safeBoxDiamond;
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("收藏游戏列表")
    public List<Integer> gameTypeList;
    @ProtoDesc("如果有值，表示要重连进入游戏")
    public GameWareInfo gameWareInfo;
    @ProtoDesc("轮播数据")
    public List<Carousel> carouselList;
    @ProtoDesc("玩家创建时间(秒)")
    public int createTime;
    @ProtoDesc("是否注册")
    public boolean register;
    @ProtoDesc("注册奖励领取状态 1已经领取 2未领取")
    public int registerRewardsState;

    public ResLogin(int code) {
        super(code);
    }
}
