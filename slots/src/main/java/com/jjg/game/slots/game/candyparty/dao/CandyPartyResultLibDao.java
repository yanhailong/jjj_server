package com.jjg.game.slots.game.candyparty.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.candyparty.data.CandyPartyResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class CandyPartyResultLibDao extends AbstractResultLibDao<CandyPartyResultLib> {
    public CandyPartyResultLibDao() {
        super(CandyPartyResultLib.class);
    }

}
