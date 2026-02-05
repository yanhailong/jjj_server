package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class PegasusUnbridleResultLib extends SlotsResultLib<PegasusUnbridleAwardLineInfo> {
    private List<PegasusUnbridleResultLib> specialResult;
    //特殊模式icon
    private int specialModeIcon;

    public List<PegasusUnbridleResultLib> getSpecialResult() {
        return specialResult;
    }

    public void setSpecialResult(List<PegasusUnbridleResultLib> specialResult) {
        this.specialResult = specialResult;
    }

    public void addSpecialResult(PegasusUnbridleResultLib result) {
        if (specialResult == null) {
            specialResult = new ArrayList<>();
        }
        specialResult.add(result);
    }

    public int getSpecialModeIcon() {
        return specialModeIcon;
    }

    public void setSpecialModeIcon(int specialModeIcon) {
        this.specialModeIcon = specialModeIcon;
    }
}

