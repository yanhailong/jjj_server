
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-23 12:47:30
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
	private int gameType;
	@Tag(4)
	@ProtoDesc("场次id")
	private int wareId;
	@Tag(5)
	@ProtoDesc("水池初始值")
	private long basicWarehouse;
	@Tag(6)
	@ProtoDesc("水池大奖虚拟金额")
	private int grandPrize;
	@Tag(7)
	@ProtoDesc("进入条件_VIP等级")
	private int require_viplevel;
	@Tag(8)
	@ProtoDesc("进入条件_最低金额")
	private int require_amount;

 	public int getGameType() {
		return this.gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
	}

	public int getWareId() {
		return this.wareId;
	}

	public void setWareId(int wareId) {
		this.wareId = wareId;
	}

	public long getBasicWarehouse() {
		return this.basicWarehouse;
	}

	public void setBasicWarehouse(long basicWarehouse) {
		this.basicWarehouse = basicWarehouse;
	}

	public int getGrandPrize() {
		return this.grandPrize;
	}

	public void setGrandPrize(int grandPrize) {
		this.grandPrize = grandPrize;
	}

	public int getRequire_viplevel() {
		return this.require_viplevel;
	}

	public void setRequire_viplevel(int require_viplevel) {
		this.require_viplevel = require_viplevel;
	}

	public int getRequire_amount() {
		return this.require_amount;
	}

	public void setRequire_amount(int require_amount) {
		this.require_amount = require_amount;
	}


 }
