package com.jjg.game.season.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("上赛季结算信息")
public class SeasonSettlementInfo {
    @ProtoDesc("赛季配置ID")
    public int seasonId;
    @ProtoDesc("赛季名称多语言ID")
    public int nameLanguageId;
    @ProtoDesc("阶段：1新手，2进阶，3循环")
    public int phase;
    @ProtoDesc("循环赛季序号，前置赛季为0")
    public int cycleIndex;
    @ProtoDesc("结算名次")
    public int rank;
    @ProtoDesc("结算时段位配置ID，0表示无段位")
    public int tierId;
    @ProtoDesc("上赛季累计获得赛季币")
    public long totalEarnedCoin;
    @ProtoDesc("新手试炼累计最高星数（各关卡最高星求和；非新手赛季为0）")
    public int totalTrialStars;
    @ProtoDesc("结算奖励；赛季币部分不进邮件，作为新赛季初始币直接带入")
    public List<ItemInfo> rewards;
    @ProtoDesc("带入新赛季的初始赛季币")
    public long initialCoin;
    @ProtoDesc("赛季币返还上限")
    public long returnCoinMax;
    @ProtoDesc("本赛季获得的段位徽章 (勋章id, 0表示无)")
    public int seasonBadge;
}
