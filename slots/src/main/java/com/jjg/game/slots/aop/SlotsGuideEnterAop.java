package com.jjg.game.slots.aop;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.service.SlotsGuideService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 在具体 SLOT 游戏进场响应发送完成后检查场景引导。 */
@Aspect
@Component
public class SlotsGuideEnterAop {
    @Autowired
    private SlotsGuideService slotsGuideService;

    @AfterReturning("@annotation(com.jjg.game.common.protostuff.Command) "
            + "&& within(com.jjg.game.slots.game..*)")
    public void afterCommand(JoinPoint joinPoint) {
        PlayerController playerController = null;
        boolean enterGameRequest = false;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof PlayerController controller) {
                playerController = controller;
                continue;
            }
            if (arg == null) {
                continue;
            }
            String className = arg.getClass().getName();
            String simpleName = arg.getClass().getSimpleName();
            if (className.startsWith("com.jjg.game.slots.game.")
                    && simpleName.startsWith("Req")
                    && simpleName.endsWith("EnterGame")) {
                enterGameRequest = true;
            }
        }
        if (enterGameRequest && playerController != null) {
            slotsGuideService.notifyAfterGameEnter(playerController);
        }
    }
}
