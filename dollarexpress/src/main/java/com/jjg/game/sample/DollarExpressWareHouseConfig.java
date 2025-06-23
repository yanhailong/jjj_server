
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-23 12:47:43
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
	private long basicWarehouse;
	@Tag(4)
	@ProtoDesc("押注_1")
	private int stake_1;
	@Tag(5)
	@ProtoDesc("押注_2")
	private int stake_2;
	@Tag(6)
	@ProtoDesc("押注_3")
	private int stake_3;
	@Tag(7)
	@ProtoDesc("押注_4")
	private int stake_4;
	@Tag(8)
	@ProtoDesc("押注_5")
	private int stake_5;
	@Tag(9)
	@ProtoDesc("押注_6")
	private int stake_6;
	@Tag(10)
	@ProtoDesc("押注_7")
	private int stake_7;
	@Tag(11)
	@ProtoDesc("押注_8")
	private int stake_8;
	@Tag(12)
	@ProtoDesc("押注_9")
	private int stake_9;
	@Tag(13)
	@ProtoDesc("押注_10")
	private int stake_10;
	@Tag(14)
	@ProtoDesc("底注倍数")
	private int multiplier;

 	public long getBasicWarehouse() {
		return this.basicWarehouse;
	}

	public void setBasicWarehouse(long basicWarehouse) {
		this.basicWarehouse = basicWarehouse;
	}

	public int getStake_1() {
		return this.stake_1;
	}

	public void setStake_1(int stake_1) {
		this.stake_1 = stake_1;
	}

	public int getStake_2() {
		return this.stake_2;
	}

	public void setStake_2(int stake_2) {
		this.stake_2 = stake_2;
	}

	public int getStake_3() {
		return this.stake_3;
	}

	public void setStake_3(int stake_3) {
		this.stake_3 = stake_3;
	}

	public int getStake_4() {
		return this.stake_4;
	}

	public void setStake_4(int stake_4) {
		this.stake_4 = stake_4;
	}

	public int getStake_5() {
		return this.stake_5;
	}

	public void setStake_5(int stake_5) {
		this.stake_5 = stake_5;
	}

	public int getStake_6() {
		return this.stake_6;
	}

	public void setStake_6(int stake_6) {
		this.stake_6 = stake_6;
	}

	public int getStake_7() {
		return this.stake_7;
	}

	public void setStake_7(int stake_7) {
		this.stake_7 = stake_7;
	}

	public int getStake_8() {
		return this.stake_8;
	}

	public void setStake_8(int stake_8) {
		this.stake_8 = stake_8;
	}

	public int getStake_9() {
		return this.stake_9;
	}

	public void setStake_9(int stake_9) {
		this.stake_9 = stake_9;
	}

	public int getStake_10() {
		return this.stake_10;
	}

	public void setStake_10(int stake_10) {
		this.stake_10 = stake_10;
	}

	public int getMultiplier() {
		return this.multiplier;
	}

	public void setMultiplier(int multiplier) {
		this.multiplier = multiplier;
	}


 }
