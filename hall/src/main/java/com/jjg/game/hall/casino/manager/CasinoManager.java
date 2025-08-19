package com.jjg.game.hall.casino.manager;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.hall.casino.service.PlayerBuildingService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.casino.data.MachineInfo;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import com.jjg.game.hall.casino.pb.CasinoBuilder;
import com.jjg.game.hall.casino.pb.bean.CasinoFloorInfo;
import com.jjg.game.hall.casino.pb.bean.CasinoMachineInfo;
import com.jjg.game.hall.casino.pb.req.ReqCasinoBuyClaimAllRewards;
import com.jjg.game.hall.casino.pb.req.ReqCasinoInfo;
import com.jjg.game.hall.casino.pb.res.ResCasinoBuyClaimAllRewards;
import com.jjg.game.hall.casino.pb.res.ResCasinoInfo;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.utils.GlobalDataCache;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author lm
 * @date 2025/8/18 16:24
 */
@Component
public class CasinoManager {
    private final Logger log = LoggerFactory.getLogger(CasinoManager.class);
    @Autowired
    private PlayerBuildingService playerBuildingService;
    @Autowired
    private PlayerPackService playerPackService;

    /**
     * 请求购买一键领取
     *
     * @param playerId 玩家id
     * @param req      请求
     * @return 响应
     */
    public ResCasinoBuyClaimAllRewards reqCasinoBuyClaimAllRewards(long playerId, ReqCasinoBuyClaimAllRewards req) {
        ResCasinoBuyClaimAllRewards res = new ResCasinoBuyClaimAllRewards();
        res.casinoId = req.casinoId;
        try {
            Long oneClickClaimEndTime = playerBuildingService.getOneClickClaimEndTime(playerId, req.casinoId);
            if (Objects.nonNull(oneClickClaimEndTime) && oneClickClaimEndTime > System.currentTimeMillis()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(HallConstant.Casino.BUY_ALL_CLAIM_ALL_REWARDS);
            if (Objects.isNull(globalConfigCfg)) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            String consumeStr = globalConfigCfg.getValue();
            String[] split = StringUtils.split(consumeStr, "_");
            if (split.length != 3) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            //检查消耗
            PlayerPack pack = playerPackService.getFromAllDB(playerId);
            if (Objects.isNull(pack)) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            Item consume = new Item(Integer.parseInt(split[0]), Long.parseLong(split[1]));
            if (!pack.checkHasItems(List.of(consume))) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            //扣除道具
            CommonResult<PlayerPack> removed = playerPackService.removeItem(playerId, consume.getId(), consume.getCount(),"一键升级购买");
            if (!removed.success()) {
                res.code = Code.NOT_ENOUGH_ITEM;
                return res;
            }
            //添加数据
            long endTime = System.currentTimeMillis() + Long.parseLong(split[2]);
            CommonResult<Long> updated = playerBuildingService.updateData(playerId, endTime, (param) -> playerBuildingService.setOneClickClaimEndTime(playerId, req.casinoId, endTime));
            if (!updated.success()) {
                log.error("购买一键领取时保存结束时间失败 playerId:{}", playerId);
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
            res.endTime = endTime;
            return res;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求购买一键领取异常", e);
        }
        return res;
    }

    /**
     * 请求我的赌场信息
     *
     * @param playerId 玩家id
     * @param req      请求
     * @return 赌场信息
     */
    public ResCasinoInfo reqCasinoInfo(long playerId, ReqCasinoInfo req) {
        ResCasinoInfo res = new ResCasinoInfo();
        PlayerBuilding playerBuilding = playerBuildingService.getFromAllDB(playerId);
        if (Objects.isNull(playerBuilding)) {
            res.code = Code.UNKNOWN_ERROR;
            return res;
        }
        res.claimAllRewardsEndTime = playerBuilding.getOneClickClaimEndTimeMap().getOrDefault(req.casinoId, 0L);
        Pair<Item, Integer> buyClaimAllRewardsConsumer = GlobalDataCache.getBuyClaimAllRewardsConsumer();
        Item item = buyClaimAllRewardsConsumer.getFirst();
        res.itemInfo = CasinoBuilder.buildItemInfo(item);
        res.casinoId = req.casinoId;
        //构建楼层信息
        List<CasinoFloorInfo> casinoFloorInfos = new ArrayList<>();
        long timeMillis = System.currentTimeMillis();
        Map<Integer, List<Long>> map = playerBuilding.getBuildingData().get(req.casinoId);
        for (Map.Entry<Integer, List<Long>> entry : map.entrySet()) {
            //构建机台信息
            for (Long machineId : entry.getValue()) {

            }
        }
        return null;
    }





}
