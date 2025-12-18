package com.jjg.game.slots.game.wealthbank.manager;


import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 财富银行游戏逻辑处理器
 *
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class WealthBankGameManager extends AbstractWealthBankGameManager {
    public WealthBankGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

}
