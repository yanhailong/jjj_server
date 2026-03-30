package com.jjg.game.slots.game.acedj.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.acedj.data.AceDjResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class AceDjResultLibDao extends AbstractResultLibDao<AceDjResultLib> {
    public AceDjResultLibDao() {
        super(AceDjResultLib.class);
    }
}
