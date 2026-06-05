package com.jjg.game.slots.manager;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.pb.res.NotifyItemDrop;
import com.jjg.game.sim.service.SimNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * slots → sim 联动统一出口 (所有 slots 游戏共用)。
 * <p>
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
    @Autowired
    private SimNodeService simNodeService;

    /**
     * 异步通知 sim(hall 节点): slots 旋转联动 (扣能量/加经验/赌场升级/道具掉落)。
     * 失败不影响 slots 旋转本身。
     *
     * @param gameType slots 游戏类型
     * @param winTimes 本次中奖倍数 (gameRunInfo.allWinTimes)
     */
    public void notifySpin(PlayerController playerController, int gameType, int winTimes) {
        ClusterClient client = simNodeService.getSimClusterClient(playerController.playerId());
        if(client == null){
            return;
        }

        GameRpcContext.getContext().withReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(client).setTryMillisPerClient(1000));

        CommonResult<Map<Integer, Long>> result = toSimBridge.onSlotsSpin(playerController.playerId(), gameType, winTimes);
        if(!result.success() || result.data == null || result.data.isEmpty()){
            return;
        }
        NotifyItemDrop notify = new NotifyItemDrop();
        notify.items = ItemUtils.buildItemInfo(result.data);
        playerController.send(notify);
    }
}
