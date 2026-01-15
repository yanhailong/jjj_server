package com.jjg.game.core.base.condition.data;

import java.util.List;

public record PlayerEffective(
        List<Integer> ids,
        /*
          条件达到所需数目
         */
        long achievedProcess
) {
}

