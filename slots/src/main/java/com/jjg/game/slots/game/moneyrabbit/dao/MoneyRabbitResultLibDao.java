package com.jjg.game.slots.game.moneyrabbit.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class MoneyRabbitResultLibDao extends AbstractResultLibDao<MoneyRabbitResultLib> {
    public MoneyRabbitResultLibDao() {
        super(MoneyRabbitResultLib.class);
    }
}
