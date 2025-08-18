package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/13 18:19
 */
@ProtobufMessage()
@ProtoDesc("邮件信息")
public class MailInfo {
    public long id;
    @ProtoDesc("邮件标题")
    public String title;
    @ProtoDesc("邮件内容")
    public String content;
    @ProtoDesc("发送邮件的时间")
    public int sendTime;
    @ProtoDesc("超时时间")
    public int timeout;
    @ProtoDesc("邮件中的道具列表")
    public List<ItemInfo> items;
    @ProtoDesc("邮件状态 0.未阅读  1.已阅读  2.已领取")
    public int status;
}
