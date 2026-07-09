package com.jjg.game.slots.game.dracula.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.dracula.data.DraculaResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class DraculaResultLibDao extends AbstractResultLibDao<DraculaResultLib> {
    public DraculaResultLibDao() {
        super(DraculaResultLib.class);
    }
}
