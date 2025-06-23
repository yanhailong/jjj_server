
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
 public class DollarExpressIconConfig extends Sample{
    public static SampleFactory<DollarExpressIconConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressIconConfig getDollarExpressIconConfig(int sid) {
        return (DollarExpressIconConfig)factory.getSample(sid);
    }

    public static DollarExpressIconConfig newDollarExpressIconConfig(int sid) {
        return (DollarExpressIconConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("资源名称")
	private String icon;
	@Tag(4)
	@ProtoDesc("1连倍率")
	private int payout_1;
	@Tag(5)
	@ProtoDesc("2连倍率")
	private int payout_2;
	@Tag(6)
	@ProtoDesc("3连倍率")
	private int payout_3;
	@Tag(7)
	@ProtoDesc("4连倍率")
	private int payout_4;
	@Tag(8)
	@ProtoDesc("5连倍率")
	private int payout_5;
	@Tag(9)
	@ProtoDesc("图标类型")
	private int type;
	@Tag(10)
	@ProtoDesc("乘以额外倍数")
	private int doubling;
	@Tag(11)
	@ProtoDesc("不中奖时用于填充的权重")
	private int noWinning;

 	public String getIcon() {
		return this.icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public int getPayout_1() {
		return this.payout_1;
	}

	public void setPayout_1(int payout_1) {
		this.payout_1 = payout_1;
	}

	public int getPayout_2() {
		return this.payout_2;
	}

	public void setPayout_2(int payout_2) {
		this.payout_2 = payout_2;
	}

	public int getPayout_3() {
		return this.payout_3;
	}

	public void setPayout_3(int payout_3) {
		this.payout_3 = payout_3;
	}

	public int getPayout_4() {
		return this.payout_4;
	}

	public void setPayout_4(int payout_4) {
		this.payout_4 = payout_4;
	}

	public int getPayout_5() {
		return this.payout_5;
	}

	public void setPayout_5(int payout_5) {
		this.payout_5 = payout_5;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getDoubling() {
		return this.doubling;
	}

	public void setDoubling(int doubling) {
		this.doubling = doubling;
	}

	public int getNoWinning() {
		return this.noWinning;
	}

	public void setNoWinning(int noWinning) {
		this.noWinning = noWinning;
	}


 }
