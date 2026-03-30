package com.jjg.game.slots.game.panJinLian.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class PanJinLianResultLibDao extends AbstractResultLibDao<PanJinLianResultLib> {
    public PanJinLianResultLibDao() {
        super(PanJinLianResultLib.class);
    }
}
