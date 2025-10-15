package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardTurntableConfig;

import java.util.List;

/**
 * 请求转盘数据回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_TURNTABLE_CONFIG,
        resp = true
)
@ProtoDesc("请求转盘数据回复")
public class ResPointsAwardTurntableConfig extends AbstractResponse {

    /**
     * 当前旋转次数
     */
    @ProtoDesc("当前旋转次数")
    private int count;

    /**
     * 最大旋转次数
     */
    @ProtoDesc("最大旋转次数")
    private int maxCount;

    /**
     * 转盘配置
     */
    @ProtoDesc("转盘配置")
    private List<PointsAwardTurntableConfig> configList;

    public ResPointsAwardTurntableConfig(int code) {
        super(code);
    }

    public List<PointsAwardTurntableConfig> getConfigList() {
        return configList;
    }

    public void setConfigList(List<PointsAwardTurntableConfig> configList) {
        this.configList = configList;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
}
