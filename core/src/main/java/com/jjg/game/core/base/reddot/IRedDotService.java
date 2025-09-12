package com.jjg.game.core.base.reddot;

import com.jjg.game.core.pb.reddot.RedDotDetails;

import java.util.List;

/**
 * 红点服务
 * <p style = "color:red"> 需要定义红点模块和子模块
 * <p>红点模块{@link RedDotDetails.RedDotModule}
 * <p>子模块{@link RedDotDetails.RedDotSubmodule}
 */
public interface IRedDotService {

    /**
     * 获取所属模块{@link RedDotDetails.RedDotModule}
     */
    RedDotDetails.RedDotModule getModule();

    /**
     * 初始化红点信息
     *
     * @param playerId  玩家id
     * @param submodule 子模块 {@link RedDotDetails.RedDotSubmodule}
     *                  </p>
     *                  (如果指定了子模块则加载子模块数据,没有则加载所有子模块)
     */
    List<RedDotDetails> initialize(long playerId, RedDotDetails.RedDotSubmodule submodule);


}
