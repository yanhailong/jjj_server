package com.jjg.game.slots.game.demonchild.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class DemonChildResultLibDao extends AbstractResultLibDao<DemonChildResultLib> {
    public DemonChildResultLibDao() {
        super(DemonChildResultLib.class);
    }
}
