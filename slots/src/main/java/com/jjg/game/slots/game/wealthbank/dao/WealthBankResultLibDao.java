package com.jjg.game.slots.game.wealthbank.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.wealthbank.data.WealthBankResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/6/23 10:54
 */
@Repository
public class WealthBankResultLibDao extends AbstractResultLibDao<WealthBankResultLib> {

    public WealthBankResultLibDao() {
        super(WealthBankResultLib.class);
    }
}
