package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.slots.game.cleopatra.data.CleopatraAwardLineInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinAwardLineInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:33
 */
@Component
public class CleopatraGenerateManager extends AbstractSlotsGenerateManager<CleopatraAwardLineInfo, CleopatraResultLib> {
    public CleopatraGenerateManager() {
        super(CleopatraResultLib.class);
    }
}
