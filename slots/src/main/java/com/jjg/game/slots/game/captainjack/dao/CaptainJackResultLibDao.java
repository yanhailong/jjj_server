package com.jjg.game.slots.game.captainjack.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class CaptainJackResultLibDao extends AbstractResultLibDao<CaptainJackResultLib> {
    public CaptainJackResultLibDao() {
        super(CaptainJackResultLib.class);
    }
}
