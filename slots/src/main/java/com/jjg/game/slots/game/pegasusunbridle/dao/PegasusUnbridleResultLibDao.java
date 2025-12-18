package com.jjg.game.slots.game.pegasusunbridle.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class PegasusUnbridleResultLibDao extends AbstractResultLibDao<PegasusUnbridleResultLib> {
    public PegasusUnbridleResultLibDao() {
        super(PegasusUnbridleResultLib.class);
    }
}
