package com.jjg.game.slots.game.christmasBashNight.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
@Document
public class ChristmasBashNightResultLib extends SlotsResultLib<ChristmasBashNightAwardLineInfo> {
    //消除补齐的信息
    private List<ChristmasBashNightAddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;

    public List<ChristmasBashNightAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<ChristmasBashNightAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
