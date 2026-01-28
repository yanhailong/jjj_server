package com.jjg.game.slots.game.zeusVsHades.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class ZeusVsHadesResultLibDao extends AbstractResultLibDao<ZeusVsHadesResultLib> {
    public ZeusVsHadesResultLibDao() {
        super(ZeusVsHadesResultLib.class);
    }
}
