
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
	private int levelUpExp;
	@Tag(4)
	@ProtoDesc("额外流水系数")
	private int prop;

 	public int getLevelUpExp() {
		return this.levelUpExp;
	}

	public void setLevelUpExp(int levelUpExp) {
		this.levelUpExp = levelUpExp;
	}

	public int getProp() {
		return this.prop;
	}

	public void setProp(int prop) {
		this.prop = prop;
	}


 }
