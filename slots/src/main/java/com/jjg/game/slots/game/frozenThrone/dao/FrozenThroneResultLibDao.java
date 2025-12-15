package com.jjg.game.slots.game.frozenThrone.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class FrozenThroneResultLibDao extends AbstractResultLibDao<FrozenThroneResultLib> {
    public FrozenThroneResultLibDao() {
        super(FrozenThroneResultLib.class);
    }
}
