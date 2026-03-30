package com.jjg.game.slots.game.frozenThrone.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;
import com.jjg.game.slots.game.frozenThrone.dao.FrozenThroneGameDataDao;
import com.jjg.game.slots.game.frozenThrone.dao.FrozenThroneResultLibDao;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneGameRunInfo;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThronePlayerGameData;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThronePlayerGameDataDTO;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneResultLib;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 寒冰王座游戏逻辑处理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:25
 */
@Component
public class FrozenThroneGameManager extends AbstractFrozenThroneGameManager {

    public FrozenThroneGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }


}
