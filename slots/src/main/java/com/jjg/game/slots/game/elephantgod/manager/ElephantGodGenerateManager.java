package com.jjg.game.slots.game.elephantgod.manager;

import com.jjg.game.slots.game.elephantgod.data.ElephantGodAwardLineInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

@Component
public class ElephantGodGenerateManager extends AbstractSlotsGenerateManager<ElephantGodAwardLineInfo, ElephantGodResultLib> {
    public ElephantGodGenerateManager() {
        super(ElephantGodResultLib.class);
    }
}