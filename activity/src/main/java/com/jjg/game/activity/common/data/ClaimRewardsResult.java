package com.jjg.game.activity.common.data;

import com.jjg.game.core.data.ItemOperationResult;

/**
 * @author lm
 * @date 2025/10/13 09:54
 */
public record ClaimRewardsResult(PlayerActivityData playerActivityData,
                                 ItemOperationResult itemOperationResult) {
}
