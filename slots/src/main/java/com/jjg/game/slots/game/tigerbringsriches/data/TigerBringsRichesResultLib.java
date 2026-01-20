package com.jjg.game.slots.game.tigerbringsriches.data;

import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
@Document
public class TigerBringsRichesResultLib extends SlotsResultLib<TigerBringsRichesAwardLineInfo> {

    private List<TigerBringsRichesResultLib> specialResult;
    //特殊模式icon
    private int specialModeIcon;

    public void setSpecialResult(List<TigerBringsRichesResultLib> specialResult) {
        this.specialResult = specialResult;
    }

    public int getSpecialModeIcon() {
        return specialModeIcon;
    }

    public void setSpecialModeIcon(int specialModeIcon) {
        this.specialModeIcon = specialModeIcon;
    }

    public List<TigerBringsRichesResultLib> getSpecialResult() {
        return specialResult;
    }

    public void addSpecialResult(TigerBringsRichesResultLib result){
        if (specialResult == null) {
            specialResult = new ArrayList<>();
        }
        specialResult.add(result);
    }

}

