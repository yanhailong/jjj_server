package com.jjg.game.slots.game.moneyrabbit.manager;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MoneyRabbitGameManager extends AbstractMoneyRabbitGameManager {
    public MoneyRabbitGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }
}
