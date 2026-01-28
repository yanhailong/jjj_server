package com.jjg.game.core.base.condition;
import com.jjg.game.core.data.Player;

/**
 * @author lm
 * @date 2026/1/14 10:29
 */


public record ConditionContext(Player player, Object event, String prefix) {

}