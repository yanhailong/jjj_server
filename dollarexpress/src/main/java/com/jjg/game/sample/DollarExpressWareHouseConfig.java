
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-17 17:20:40
 */
 @ProtobufMessage
 public class DollarExpressWareHouseConfig extends Sample{
    public static SampleFactory<DollarExpressWareHouseConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressWareHouseConfig getDollarExpressWareHouseConfig(int sid) {
        return (DollarExpressWareHouseConfig)factory.getSample(sid);
    }

    public static DollarExpressWareHouseConfig newDollarExpressWareHouseConfig(int sid) {
        return (DollarExpressWareHouseConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("水池初始值")
	public long basicWarehouse;
	@Tag(4)
	@ProtoDesc("水池大奖虚拟金额")
	public long grandPrize;
	@Tag(5)
	@ProtoDesc("进入条件_VIP等级")
	public int require_viplevel;
	@Tag(6)
	@ProtoDesc("进入条件_最低金额")
	public long require_amount;
	@Tag(7)
	@ProtoDesc("押注_1")
	public long stake_1;
	@Tag(8)
	@ProtoDesc("押注_2")
	public long stake_2;
	@Tag(9)
	@ProtoDesc("押注_3")
	public long stake_3;
	@Tag(10)
	@ProtoDesc("押注_4")
	public long stake_4;
	@Tag(11)
	@ProtoDesc("押注_5")
	public long stake_5;
	@Tag(12)
	@ProtoDesc("押注_6")
	public long stake_6;
	@Tag(13)
	@ProtoDesc("押注_7")
	public long stake_7;
	@Tag(14)
	@ProtoDesc("押注_8")
	public long stake_8;
	@Tag(15)
	@ProtoDesc("押注_9")
	public long stake_9;
	@Tag(16)
	@ProtoDesc("押注_10")
	public long stake_10;
	@Tag(17)
	@ProtoDesc("底注倍数")
	public int multiplier;

 }
