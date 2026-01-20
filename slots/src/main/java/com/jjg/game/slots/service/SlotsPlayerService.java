package com.jjg.game.slots.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.service.AbstractPlayerService;
import com.jjg.game.core.utils.ItemUtils;
import org.springframework.stereotype.Service;

/**
 * @author 11
 * @date 2025/6/10 18:03
 */
@Service
public class SlotsPlayerService extends AbstractPlayerService {

    public CommonResult<Player> betDeductCurrent(long playerId, long deductNum, long currentId, boolean effective, AddType addType, String desc, boolean isNotify) {
        if (currentId == ItemUtils.getGoldItemId()) {
            return betDeductGold(playerId, deductNum, addType, effective, isNotify, desc);
        }
        if (currentId == ItemUtils.getDiamondItemId()) {
            return deductDiamond(playerId, deductNum, addType, desc, isNotify);
        }
        return new CommonResult<>(Code.FAIL);
    }

    public CommonResult<Player> addCurrent(long playerId, long addNum, long currentId, AddType addType, String desc, boolean isNotify) {
        if (currentId == ItemUtils.getGoldItemId()) {
            return addGold(playerId, addNum, addType, desc, isNotify);
        }
        if (currentId == ItemUtils.getDiamondItemId()) {
            return addDiamond(playerId, addNum, addType, desc, isNotify);
        }
        return new CommonResult<>(Code.FAIL);
    }

}
