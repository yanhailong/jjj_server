package com.jjg.game.hall.listener;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.GameFunctionListener;
import com.jjg.game.hall.pb.res.ResFunctionOpenList;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class HallGameFuntionListener implements GameFunctionListener {
    private final SimPlayerContextRegistry contextRegistry;

    public HallGameFuntionListener(SimPlayerContextRegistry contextRegistry) {
        this.contextRegistry = contextRegistry;
    }

    @Override
    public Set<Integer> checkOpenFunction(Player player) {
        SimPlayerContext ctx = contextRegistry.getContext(player.getId());
        return ctx == null ? Collections.emptySet() : ctx.getSimBaseData().getUnlockedFunctionIds();
    }

    @Override
    public void notifyAllFunction(PFSession session, List<Integer> openedFuncIdList) {
        if(session == null) {
            return;
        }
        // 推送功能发生了变化
        ResFunctionOpenList res = new ResFunctionOpenList(Code.SUCCESS);
        res.openedFunctionIdList = openedFuncIdList;
        session.send(res);
    }
}
