package com.jjg.game.slots.game.wealthgod.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.wealthgod.data.WealthGodResultLib;
import org.springframework.stereotype.Repository;

/**
 * 财神
 */
@Repository
public class WealthGodResultLibDao extends AbstractResultLibDao<WealthGodResultLib> {

    public WealthGodResultLibDao() {
        super(WealthGodResultLib.class);
    }

}
