
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-20 11:50:22
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
	@ProtoDesc("押注_1")
	public int stake_1;
	@Tag(5)
	@ProtoDesc("押注_2")
	public int stake_2;
	@Tag(6)
	@ProtoDesc("押注_3")
	public int stake_3;
	@Tag(7)
	@ProtoDesc("押注_4")
	public int stake_4;
	@Tag(8)
	@ProtoDesc("押注_5")
	public int stake_5;
	@Tag(9)
	@ProtoDesc("押注_6")
	public int stake_6;
	@Tag(10)
	@ProtoDesc("押注_7")
	public int stake_7;
	@Tag(11)
	@ProtoDesc("押注_8")
	public int stake_8;
	@Tag(12)
	@ProtoDesc("押注_9")
	public int stake_9;
	@Tag(13)
	@ProtoDesc("押注_10")
	public int stake_10;
	@Tag(14)
	@ProtoDesc("底注倍数")
	public int multiplier;

 }
