package com.jjg.game.slots.game.basketballSuperstar.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class BasketballSuperstarResultLibDao extends AbstractResultLibDao<BasketballSuperstarResultLib> {
    public BasketballSuperstarResultLibDao() {
        super(BasketballSuperstarResultLib.class);
    }
}
