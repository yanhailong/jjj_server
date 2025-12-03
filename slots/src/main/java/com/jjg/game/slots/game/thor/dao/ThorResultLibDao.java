package com.jjg.game.slots.game.thor.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/12/1 18:08
 */
@Repository
public class ThorResultLibDao extends AbstractResultLibDao<ThorResultLib> {
    public ThorResultLibDao() {
        super(ThorResultLib.class);
    }
}
