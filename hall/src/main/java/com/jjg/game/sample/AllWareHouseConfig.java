
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-20 11:50:27
 */
 @ProtobufMessage
 public class AllWareHouseConfig extends Sample{
    public static SampleFactory<AllWareHouseConfig> factory = new SampleFactoryImpl<>();
    public static AllWareHouseConfig getAllWareHouseConfig(int sid) {
        return (AllWareHouseConfig)factory.getSample(sid);
    }

    public static AllWareHouseConfig newAllWareHouseConfig(int sid) {
        return (AllWareHouseConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("游戏类型")
	public int gameType;
	@Tag(4)
	@ProtoDesc("场次id")
	public int wareId;
	@Tag(5)
	@ProtoDesc("水池初始值")
	public long basicWarehouse;
	@Tag(6)
	@ProtoDesc("水池大奖虚拟金额")
	public int grandPrize;
	@Tag(7)
	@ProtoDesc("进入条件_VIP等级")
	public int require_viplevel;
	@Tag(8)
	@ProtoDesc("进入条件_最低金额")
	public int require_amount;

 }
