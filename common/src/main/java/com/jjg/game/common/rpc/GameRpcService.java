package com.jjg.game.common.rpc;

import java.lang.annotation.*;

/**
 * 服务提供者
 *
 * @author 2CL
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GameRpcService {
}
