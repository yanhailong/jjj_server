package com.jjg.game.activity.privilegecard.message.res;

import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardDetailInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
public class ResPrivilegeCardDetailInfo extends AbstractResponse {
    @ProtoDesc("活动详细信息")
    public List<PrivilegeCardDetailInfo> detailInfo;

    public ResPrivilegeCardDetailInfo(int code) {
        super(code);
    }
}
