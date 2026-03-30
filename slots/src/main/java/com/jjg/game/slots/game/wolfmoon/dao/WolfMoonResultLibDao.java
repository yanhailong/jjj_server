package com.jjg.game.slots.game.wolfmoon.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Repository
public class WolfMoonResultLibDao extends AbstractResultLibDao<WolfMoonResultLib> {

    public WolfMoonResultLibDao() {
        super(WolfMoonResultLib.class);
    }

}
