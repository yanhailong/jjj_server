package com.jjg.game.slots.game.hulk.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.hulk.data.HulkResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2026/1/15
 */
@Repository
public class HulkResultLibDao extends AbstractResultLibDao<HulkResultLib> {
    public HulkResultLibDao() {
        super(HulkResultLib.class);
    }
}
