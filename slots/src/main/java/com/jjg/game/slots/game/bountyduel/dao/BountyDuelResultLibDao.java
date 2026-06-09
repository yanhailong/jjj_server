package com.jjg.game.slots.game.bountyduel.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class BountyDuelResultLibDao extends AbstractResultLibDao<BountyDuelResultLib> {
    public BountyDuelResultLibDao() {
        super(BountyDuelResultLib.class);
    }
}
