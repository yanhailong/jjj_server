package com.jjg.game.slots.game.mahjiongwin.manager;

import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinAwardLineInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:33
 */
@Component
public class MajiongWinGenerateManager extends AbstractSlotsGenerateManager<MahjiongWinAwardLineInfo, MahjiongWinResultLib> {
    public MajiongWinGenerateManager() {
        super(MahjiongWinResultLib.class);
    }
}
