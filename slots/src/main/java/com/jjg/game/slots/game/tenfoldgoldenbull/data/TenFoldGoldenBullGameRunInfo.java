package com.jjg.game.slots.game.tenfoldgoldenbull.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.bean.TenFoldGoldenBullWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class TenFoldGoldenBullGameRunInfo extends GameRunInfo<TenFoldGoldenBullPlayerGameData> {

    public TenFoldGoldenBullGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //是否是福马模式结束
    private boolean isFuMaEnd;
    //中奖线信息
    private List<TenFoldGoldenBullWinIconInfo> awardLineInfos;
    //滚轴类型
    private int scrollType;

    public boolean isFuMaEnd() {
        return isFuMaEnd;
    }

    public void setFuMaEnd(boolean fuMaEnd) {
        isFuMaEnd = fuMaEnd;
    }

    public List<TenFoldGoldenBullWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<TenFoldGoldenBullWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public int getScrollType() {
        return scrollType;
    }

    public void setScrollType(int scrollType) {
        this.scrollType = scrollType;
    }
}
