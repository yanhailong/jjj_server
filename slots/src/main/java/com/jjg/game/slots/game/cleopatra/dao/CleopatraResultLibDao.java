package com.jjg.game.slots.game.cleopatra.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class CleopatraResultLibDao extends AbstractResultLibDao<CleopatraResultLib> {
    public CleopatraResultLibDao() {
        super(CleopatraResultLib.class);
    }
}
