package com.jjg.game.slots.game.steamAge.data;

import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightAddIconInfo;
import com.jjg.game.slots.game.steamAge.pb.SteamAgeExpand;
import com.jjg.game.slots.game.steamAge.pb.SteamAgeIconInfo;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
@Document
public class SteamAgeResultLib extends SlotsResultLib<SteamAgeAwardLineInfo> {
    //本次触发的jackpotId
    private int jackpotId;
    //增加的免费次数
    private int addFreeCount;
    //消除补齐的信息
    private List<SteamAgeExpandIconInfo> addIconInfos;

    public List<SteamAgeExpandIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<SteamAgeExpandIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
