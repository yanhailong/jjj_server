package com.jjg.game.slots.game.tigerbringsriches.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.tigerbringsriches.pb.bean.TigerBringsRichesWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class TigerBringsRichesGameRunInfo extends GameRunInfo<TigerBringsRichesPlayerGameData> {

    public TigerBringsRichesGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //是否是特殊模式结束
    private boolean isSpecialModeEnd;
    //中奖线信息
    private List<TigerBringsRichesWinIconInfo> awardLineInfos;
    //滚轴类型
    private int scrollType;
    //特殊模式icon
    private int specialModeIcon;

    public int getSpecialModeIcon() {
        return specialModeIcon;
    }

    public void setSpecialModeIcon(int specialModeIcon) {
        this.specialModeIcon = specialModeIcon;
    }

    public boolean isSpecialModeEnd() {
        return isSpecialModeEnd;
    }

    public void setSpecialModeEnd(boolean specialModeEnd) {
        isSpecialModeEnd = specialModeEnd;
    }

    public List<TigerBringsRichesWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<TigerBringsRichesWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public int getScrollType() {
        return scrollType;
    }

    public void setScrollType(int scrollType) {
        this.scrollType = scrollType;
    }
}
