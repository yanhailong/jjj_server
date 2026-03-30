package com.jjg.game.slots.game.tenfoldgoldenbull.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class TenFoldGoldenBullResultLibDao extends AbstractResultLibDao<TenFoldGoldenBullResultLib> {
    public TenFoldGoldenBullResultLibDao() {
        super(TenFoldGoldenBullResultLib.class);
    }
}
