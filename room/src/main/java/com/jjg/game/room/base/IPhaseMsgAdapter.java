package com.jjg.game.room.base;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.data.room.GameDataVo;

/**
 * 需要接收客户端消息的游戏阶段
 *
 * @author 2CL
 */
public interface IPhaseMsgAdapter<M extends AbstractMessage> extends IRoomPhase {

    /**
     * 获取对应的消息处理ID
     *
     * @return 消息
     */
    int reqMsgId();

    /**
     * 需要处理的消息
     *
     * @param message 消息
     */
    void dealMsg(PlayerController playerController, M message);
}
