package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage
@ProtoDesc("雇员详细信息")
public class EmployDetailInfo {
    @ProtoDesc("雇员id")
    public int id;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("星级")
    public int star;
    @ProtoDesc("是否为主管")
    public boolean manager;
    @ProtoDesc("主管百分比加成  key参考BuildingAreaTable表的typeValue值")
    public List<KVInfo> manageEmployeeBonus;
    @ProtoDesc("主管固定加成  key参考BuildingAreaTable表的typeValue值")
    public List<KVInfo> manageEmployeeFixBonus;
    @ProtoDesc("普通加成(雇员等级加成)  key参考BuildingAreaTable表的typeValue值")
    public List<KVInfo> employeeBonus;
}
