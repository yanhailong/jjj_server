package com.jjg.game.slots.game.thor.manager;

import com.jjg.game.slots.game.thor.data.ThorAwardLineInfo;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Component
public class ThorGenerateManager extends AbstractSlotsGenerateManager<ThorAwardLineInfo, ThorResultLib> {
    public ThorGenerateManager() {
        super(ThorResultLib.class);
    }


}
