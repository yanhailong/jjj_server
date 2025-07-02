package com.jjg.game.common.baselogic;

import com.jjg.game.common.constant.EResetType;

/**
 * 游戏中重置接口,所有重置接口垫的基类,用于重置游戏中所有的重置逻辑,用于统一调用重置方法和管理重置逻辑
 *
 * @author 2CL
 */
public interface IReset {
    /**
     * 重置接口
     */
    void reset(EResetType resetType, Object... params);
}
