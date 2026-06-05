package com.jjg.game.slots.manager;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.sim.bridge.ToSimBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * slots → sim 联动统一出口 (所有 slots 游戏共用)。
 *
 * 独立成具体 @Component 是因为 ClusterRpcPostProcessor 只注入 bean 自身声明的字段,
 * 不扫描父类; 故 @ClusterRpcReference 不能放在抽象的 AbstractSlotsGameManager。
 *
 * @author 11
 * @date 2026/6/5
 */
@Component
public class SlotsSimLinkService {
    private static final Logger log = LoggerFactory.getLogger(SlotsSimLinkService.class);

    @ClusterRpcReference
    private ToSimBridge toSimBridge;

    /**
     * 异步通知 sim(hall 节点): slots 旋转联动 (扣能量/加经验/赌场升级/道具掉落)。
     * 失败不影响 slots 旋转本身。
     *
     * @param playerId 玩家id
     * @param gameType slots 游戏类型
     * @param winTimes 本次中奖倍数 (gameRunInfo.allWinTimes)
     */
    public void notifySpin(long playerId, int gameType, int winTimes) {
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerId, 0, new BaseHandler<String>() {
            @Override
            public void action() {
                try {
                    toSimBridge.onSlotsSpin(playerId, gameType, winTimes);
                } catch (Exception e) {
                    log.error("通知 sim slots 旋转联动失败 playerId=" + playerId + ",gameType=" + gameType, e);
                }
            }
        }.setHandlerParamWithSelf("slotsSimSpin"));
    }
}
