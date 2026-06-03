package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/6/3
 */
@ProtobufMessage
@ProtoDesc("雇员信息")
public class EmployeeInfo {
    @ProtoDesc("雇员id")
    public int id;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("星级")
    public int star;
}
