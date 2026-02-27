package com.jjg.game.slots.game.wolfmoon.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class WolfMoonResultLibDao extends AbstractResultLibDao<WolfMoonResultLib> {
    public WolfMoonResultLibDao() {
        super(WolfMoonResultLib.class);
    }
}
