package com.jjg.game.slots.game.tigerbringsriches.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class TigerBringsRichesResultLibDao extends AbstractResultLibDao<TigerBringsRichesResultLib> {
    public TigerBringsRichesResultLibDao() {
        super(TigerBringsRichesResultLib.class);
    }
}
