package com.jjg.game.hall.minigame.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.constant.MinigameConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求开启的小游戏列表回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = MinigameConstant.Message.RES_MINIGAME_LIST
)
@ProtoDesc("请求开启的小游戏列表回复")
public class ResMinigameList extends AbstractResponse {

    @ProtoDesc("游戏id列表,没有开启游戏则为空数组")
    private List<Integer> gameIdList = new ArrayList<>();

    public ResMinigameList(int code) {
        super(code);
    }

    public List<Integer> getGameIdList() {
        return gameIdList;
    }

    public void setGameIdList(List<Integer> gameIdList) {
        this.gameIdList = gameIdList;
    }
}
