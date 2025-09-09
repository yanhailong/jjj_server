package com.jjg.game.slots.game.mahjiongwin.data;

import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
@Document
public class MahjiongWinResultLib extends SlotsResultLib<MahjiongWinAwardLineInfo> {
    private List<MahjiongWinAddIconInfo> addIconInfos;

    public List<MahjiongWinAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<MahjiongWinAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }
}
