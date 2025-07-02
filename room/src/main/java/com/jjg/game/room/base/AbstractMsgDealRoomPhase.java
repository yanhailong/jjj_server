package com.jjg.game.room.base;

import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.RoomCfg;

/**
 * 接收消息处理的游戏逻辑类
 *
 * @param <G> 游戏配置信息
 * @param <M> 需要处理的消息泛型
 * @author 2CL
 */
public abstract class AbstractMsgDealRoomPhase<RC extends RoomCfg, G extends GameDataVo<RC>, M extends AbstractMessage>
    extends AbstractRoomPhase<RC, G> implements IPhaseMsgAdapter<M> {

    public AbstractMsgDealRoomPhase(AbstractGameController<RC, G> gameController) {
        super(gameController);
    }
}
