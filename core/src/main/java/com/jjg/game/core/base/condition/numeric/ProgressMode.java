package com.jjg.game.core.base.condition.numeric;

/** 条件进度聚合方式。 */
public enum ProgressMode {
    /** 将本次事件值累加到当前进度。 */
    ADD,
    /** 保留当前进度和本次事件值中的较大值。 */
    MAX,
    /** 用本次状态值覆盖当前进度。 */
    SET
}
