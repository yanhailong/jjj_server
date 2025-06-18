package com.jjg.game.dollarexpress.manager;

import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.room.listener.RoomStartListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 16:37
 */
@Component
public class DollarExpressStartManager implements RoomStartListener {

    @Autowired
    private DollarExpressManager dollarExpressManager;

    @Override
    public int[] getGameTypes() {
        return new int[]{DollarExpressConst.COMMON.GAME_TYPE_DOLLAR_EXPRESS};
    }

    @Override
    public void start() {
        this.dollarExpressManager.init();
    }

    @Override
    public void shutdown() {

    }
}
