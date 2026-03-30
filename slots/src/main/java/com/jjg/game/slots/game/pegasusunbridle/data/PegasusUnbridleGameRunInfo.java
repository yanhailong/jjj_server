package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.pb.bean.PegasusUnbridleWinIconInfo;
import com.jjg.game.slots.game.thor.pb.ThorWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class PegasusUnbridleGameRunInfo extends GameRunInfo<PegasusUnbridlePlayerGameData> {

    public PegasusUnbridleGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //是否是福马模式结束
    private boolean isFuMaEnd;
    //中奖线信息
    private List<PegasusUnbridleWinIconInfo> awardLineInfos;
    //滚轴类型
    private int scrollType;
    //特殊模式icon
    private int specialModeIcon;
    public boolean isFuMaEnd() {
        return isFuMaEnd;
    }

    public void setFuMaEnd(boolean fuMaEnd) {
        isFuMaEnd = fuMaEnd;
    }

    public List<PegasusUnbridleWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<PegasusUnbridleWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public int getScrollType() {
        return scrollType;
    }

    public void setScrollType(int scrollType) {
        this.scrollType = scrollType;
    }

    public int getSpecialModeIcon() {
        return specialModeIcon;
    }

    public void setSpecialModeIcon(int specialModeIcon) {
        this.specialModeIcon = specialModeIcon;
    }
}
