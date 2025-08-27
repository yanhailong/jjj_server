package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/1 17:40
 */
@Component
public class CleopatraSendMessageManager extends BaseSendMessageManager {

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController) {

    }


    /**
     * 发送游戏结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendStartGameMessage(PlayerController playerController, CleopatraGameRunInfo gameRunInfo) {

    }
}
