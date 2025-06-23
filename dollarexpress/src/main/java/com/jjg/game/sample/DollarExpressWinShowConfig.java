
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
 public class DollarExpressWinShowConfig extends Sample{
    public static SampleFactory<DollarExpressWinShowConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressWinShowConfig getDollarExpressWinShowConfig(int sid) {
        return (DollarExpressWinShowConfig)factory.getSample(sid);
    }

    public static DollarExpressWinShowConfig newDollarExpressWinShowConfig(int sid) {
        return (DollarExpressWinShowConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("倍率=奖金/底注")
	private int payout_min;
	@Tag(4)
	@ProtoDesc("特效资源")
	private String icon;

 	public int getPayout_min() {
		return this.payout_min;
	}

	public void setPayout_min(int payout_min) {
		this.payout_min = payout_min;
	}

	public String getIcon() {
		return this.icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}


 }
