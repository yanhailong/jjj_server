package com.jjg.game.slots.game.tigerbringsriches.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
@Document
public class TigerBringsRichesResultLib extends SlotsResultLib<TigerBringsRichesAwardLineInfo> {
    private int jackpotId;
    private List<TigerBringsRichesResultLib> specialResult;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public List<TigerBringsRichesResultLib> getSpecialResult() {
        return specialResult;
    }

    public void setSpecialResult(List<TigerBringsRichesResultLib> specialResult) {
        this.specialResult = specialResult;
    }

    public void addSpecialResult(TigerBringsRichesResultLib result){
        if (specialResult == null) {
            specialResult = new ArrayList<>();
        }
        specialResult.add(result);
    }

}

