package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.FullAwardLineInfo;

public class WolfMoonAwardLineInfo extends FullAwardLineInfo {
    private boolean containsWild;

    public boolean isContainsWild() {
        return containsWild;
    }

    public void setContainsWild(boolean containsWild) {
        this.containsWild = containsWild;
    }
}
