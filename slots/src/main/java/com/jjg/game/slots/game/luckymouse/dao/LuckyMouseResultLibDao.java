package com.jjg.game.slots.game.luckymouse.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.luckymouse.data.LuckyMouseResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class LuckyMouseResultLibDao extends AbstractResultLibDao<LuckyMouseResultLib> {
    public LuckyMouseResultLibDao() {
        super(LuckyMouseResultLib.class);
    }
}
