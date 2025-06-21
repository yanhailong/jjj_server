
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
 public class DollarExpressResultShowConfig extends Sample{
    public static SampleFactory<DollarExpressResultShowConfig> factory = new SampleFactoryImpl<>();
    public static DollarExpressResultShowConfig getDollarExpressResultShowConfig(int sid) {
        return (DollarExpressResultShowConfig)factory.getSample(sid);
    }

    public static DollarExpressResultShowConfig newDollarExpressResultShowConfig(int sid) {
        return (DollarExpressResultShowConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("类型")
	public int type;
	@Tag(4)
	@ProtoDesc("权重")
	public int weight;
	@Tag(5)
	@ProtoDesc("免费次数")
	public int freetime;
	@Tag(6)
	@ProtoDesc("位置0")
	public int icon_0;
	@Tag(7)
	@ProtoDesc("位置1")
	public int icon_1;
	@Tag(8)
	@ProtoDesc("位置2")
	public int icon_2;
	@Tag(9)
	@ProtoDesc("位置3")
	public int icon_3;
	@Tag(10)
	@ProtoDesc("位置4")
	public int icon_4;
	@Tag(11)
	@ProtoDesc("位置5")
	public int icon_5;
	@Tag(12)
	@ProtoDesc("位置6")
	public int icon_6;
	@Tag(13)
	@ProtoDesc("位置7")
	public int icon_7;
	@Tag(14)
	@ProtoDesc("位置8")
	public int icon_8;
	@Tag(15)
	@ProtoDesc("位置9")
	public int icon_9;
	@Tag(16)
	@ProtoDesc("位置10")
	public int icon_10;
	@Tag(17)
	@ProtoDesc("位置11")
	public int icon_11;
	@Tag(18)
	@ProtoDesc("位置12")
	public int icon_12;
	@Tag(19)
	@ProtoDesc("位置13")
	public int icon_13;
	@Tag(20)
	@ProtoDesc("位置14")
	public int icon_14;
	@Tag(21)
	@ProtoDesc("位置15")
	public int icon_15;
	@Tag(22)
	@ProtoDesc("位置16")
	public int icon_16;
	@Tag(23)
	@ProtoDesc("位置17")
	public int icon_17;
	@Tag(24)
	@ProtoDesc("位置18")
	public int icon_18;
	@Tag(25)
	@ProtoDesc("位置19")
	public int icon_19;

 }
