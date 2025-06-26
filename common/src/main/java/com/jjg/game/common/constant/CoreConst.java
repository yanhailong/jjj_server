package com.jjg.game.common.constant;

/**
 * 系统级常量池
 * 
 * @since 1.0
 */
public interface CoreConst {
    // 本系统统一文件分隔符
    String SEPARATOR = "/";

    //redis如果发生锁竞争,则设置重试最大次数
    int REDIS_TRY_COUNT = 10;
}
