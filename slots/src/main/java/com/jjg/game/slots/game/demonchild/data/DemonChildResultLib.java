package com.jjg.game.slots.game.demonchild.data;

import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.game.captainjack.data.CaptainJackAddIconInfo;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
@Document
public class DemonChildResultLib extends SlotsResultLib<DemonChildAwardLineInfo> {
    //增加的免费次数
    private int addFreeCount;

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
