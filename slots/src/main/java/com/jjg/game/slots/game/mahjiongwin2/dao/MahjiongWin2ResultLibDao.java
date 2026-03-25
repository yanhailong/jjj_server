package com.jjg.game.slots.game.mahjiongwin2.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2ResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class MahjiongWin2ResultLibDao extends AbstractResultLibDao<MahjiongWin2ResultLib> {
    public MahjiongWin2ResultLibDao() {
        super(MahjiongWin2ResultLib.class);
    }
}
