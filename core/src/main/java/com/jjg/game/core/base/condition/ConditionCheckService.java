package com.jjg.game.core.base.condition;

import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.data.Player;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 条件检查服务
 *
 * @author 2CL
 */
@Service
public class ConditionCheckService implements GameEventListener {

    /**
     * 是否触发成功
     *
     * @param conditionCfg 触发条件配置
     * @param checkParams  检查参数
     */
    public boolean isTriggerComplete(ConditionCfg conditionCfg, List<Object> checkParams) {

        return true;
    }


    /**
     * 是否触发成功
     *
     * @param player       玩家
     * @param conditionCfg 触发条件配置
     * @param checkParams  检查参数
     */
    public boolean isTriggerComplete(Player player, ConditionCfg conditionCfg, List<Object> checkParams) {

        return true;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {

    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of();
    }
}
