package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.proto.ProtoDesc;

/**
 * @author lm
 * @date 2025/9/16 16:36
 */
public class SharePromoteRankInfo {
    @ProtoDesc("昵称")
    public String nickname;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("国旗id")
    public int nationalId;
    @ProtoDesc("称号id")
    public int titleId;
    @ProtoDesc("总收益")
    public long totalScore;

}
