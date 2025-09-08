package com.jjg.game.activity.privilegecard.message.res;

import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardType;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
public class ResPrivilegeCardTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<PrivilegeCardType> activityData;

    public ResPrivilegeCardTypeInfo(int code) {
        super(code);
    }
}
