package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.thor.pb.ThorWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/12/1 18:10
 */
public class ThorGameRunInfo extends GameRunInfo<ThorPlayerGameData> {
    //中奖线信息
    private List<ThorWinIconInfo> awardLineInfos;

    //奖池金额
    private long mini;
    private long minor;
    private long major;
    private long grand;

    public ThorGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<ThorWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<ThorWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public long getMini() {
        return mini;
    }

    public void setMini(long mini) {
        this.mini = mini;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getGrand() {
        return grand;
    }

    public void setGrand(long grand) {
        this.grand = grand;
    }
}
