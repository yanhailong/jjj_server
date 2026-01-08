package com.jjg.game.slots.game.luckymouse.manager;

import com.jjg.game.slots.game.luckymouse.data.LuckyMouseAwardLineInfo;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

@Component
public class LuckyMouseGenerateManager extends AbstractSlotsGenerateManager<LuckyMouseAwardLineInfo, LuckyMouseResultLib> {
    public LuckyMouseGenerateManager() {
        super(LuckyMouseResultLib.class);
    }
}
