package com.jjg.game.sim.service.tick;

import com.jjg.game.sim.data.CasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.service.SimCasinoDataService;
import com.jjg.game.sim.service.SimPlayerGameDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 玩家数据自动落库
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimAutoSaveService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimAutoSaveService.class);

    /** 同一玩家最小落库间隔: 5 分钟 */
    private static final long SAVE_INTERVAL_MS = 5 * 60 * 1000L;

    @Autowired
    private SimPlayerGameDataService playerDataService;
    @Autowired
    private SimCasinoDataService casinoDataService;

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        if (ctx.getPlayerGameData() == null) {
            return;
        }
        //节流: 距上次落库不够间隔则跳过
        if (ctx.getLastSaveTime() > 0 && now - ctx.getLastSaveTime() < SAVE_INTERVAL_MS) {
            return;
        }
        if (!ctx.isDirty() && !ctx.hasDirtyCasino()) {
            return;
        }

        try {
            //玩家级
            if (ctx.isDirty()) {
                playerDataService.save(ctx.getPlayerGameData());
                ctx.clearDirty();
            }
            //赌场级 (按 id 收集对应 CasinoData, 批量保存)
            Set<Integer> dirtyCasinoIds = ctx.consumeDirtyCasinoIds();
            if (!dirtyCasinoIds.isEmpty()) {
                List<CasinoData> toSave = new ArrayList<>(dirtyCasinoIds.size());
                for (int casinoId : dirtyCasinoIds) {
                    CasinoData c = ctx.getCasino(casinoId);
                    if (c != null) {
                        toSave.add(c);
                    }
                }
                if (!toSave.isEmpty()) {
                    casinoDataService.saveAll(toSave);
                }
            }
            ctx.setLastSaveTime(now);
        } catch (Exception e) {
            log.error("自动落库失败 playerId={}", ctx.playerId(), e);
        }
    }

    /** 最后执行: 让所有业务 tick 都跑完, 再统一刷盘 */
    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
