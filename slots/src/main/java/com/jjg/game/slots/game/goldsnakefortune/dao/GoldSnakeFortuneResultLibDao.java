package com.jjg.game.slots.game.goldsnakefortune.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class GoldSnakeFortuneResultLibDao extends AbstractResultLibDao<GoldSnakeFortuneResultLib> {
    public GoldSnakeFortuneResultLibDao() {
        super(GoldSnakeFortuneResultLib.class);
    }
}
