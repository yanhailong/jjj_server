
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
 public class GameListConfig extends Sample{
    public static SampleFactory<GameListConfig> factory = new SampleFactoryImpl<>();
    public static GameListConfig getGameListConfig(int sid) {
        return (GameListConfig)factory.getSample(sid);
    }

    public static GameListConfig newGameListConfig(int sid) {
        return (GameListConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("状态  0.开启  1.维护  2.关闭")
	public int status;

 }
