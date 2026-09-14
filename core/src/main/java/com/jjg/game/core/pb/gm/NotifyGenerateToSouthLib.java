package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/** 通知 Poker 节点生成牌库。旧请求未携带 gameType 时仍按南方前进处理。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_GENERATE_TO_SOUTH_LIB, resp = true, toPbFile = false)
@ProtoDesc("通知生成Poker牌库")
public class NotifyGenerateToSouthLib extends AbstractNotice {
    /** 生成局数 */
    public int count;
    /** 游戏类型；0兼容旧版南方前进请求。保留 count 为首字段以兼容旧消息结构。 */
    public int gameType;
    /** 每组初始牌的完整后续模拟次数，仅斗仙牌使用。 */
    public int rolloutCount;
}
