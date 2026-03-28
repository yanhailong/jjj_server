package com.jjg.game.slots.game.candyparty.manager;


import com.jjg.game.slots.game.candyparty.dao.CandyPartyResultLibDao;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class CandyPartyGameManager extends AbstractCandyPartyGameManager {
    public CandyPartyGameManager(CandyPartyGameGenerateManager gameGenerateManager, CandyPartyResultLibDao candyPartyResultLibDao) {
        super(gameGenerateManager, candyPartyResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }
}
