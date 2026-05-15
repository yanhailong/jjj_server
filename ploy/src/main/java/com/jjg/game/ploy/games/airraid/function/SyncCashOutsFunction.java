package com.jjg.game.ploy.games.airraid.function;

import com.jjg.game.ploy.games.airraid.pb.cluster.PlayerCashOut;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/15
 */
@FunctionalInterface
public interface SyncCashOutsFunction {
    void apply(List<PlayerCashOut> syncList);
}
