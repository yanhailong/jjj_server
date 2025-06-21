
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
 public class VipLevelConfig extends Sample{
    public static SampleFactory<VipLevelConfig> factory = new SampleFactoryImpl<>();
    public static VipLevelConfig getVipLevelConfig(int sid) {
        return (VipLevelConfig)factory.getSample(sid);
    }

    public static VipLevelConfig newVipLevelConfig(int sid) {
        return (VipLevelConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("升级所需经验")
	public int levelUpExp;
	@Tag(4)
	@ProtoDesc("额外流水系数")
	public int prop;

 }
