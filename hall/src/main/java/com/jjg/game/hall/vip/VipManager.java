package com.jjg.game.hall.vip;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.EnumUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.hall.vip.data.Vip;
import com.jjg.game.hall.vip.data.VipCfgCache;
import com.jjg.game.hall.vip.data.VipGift;
import com.jjg.game.hall.vip.pb.bean.VipGiftInfo;
import com.jjg.game.hall.vip.pb.req.ReqVipClaimGiftReward;
import com.jjg.game.hall.vip.pb.req.ReqVipInfo;
import com.jjg.game.hall.vip.pb.res.ResVipClaimGiftReward;
import com.jjg.game.hall.vip.pb.res.ResVipInfo;
import com.jjg.game.hall.vip.service.VipService;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * vip管理类
 *
 * @author lm
 * @date 2025/8/27 10:01
 */
@Component
public class VipManager implements ConfigExcelChangeListener, IPlayerLoginSuccess {
    private final Logger log = LoggerFactory.getLogger(VipManager.class);
    private final VipService vipService;
    private final PlayerPackService playerPackService;
    private final AccountDao accountDao;
    private final CorePlayerService playerService;

    public VipManager(VipService vipService,
                      PlayerPackService playerPackService,
                      AccountDao accountDao,
                      CorePlayerService playerService) {
        this.vipService = vipService;
        this.playerPackService = playerPackService;
        this.accountDao = accountDao;
        this.playerService = playerService;
    }

    @Override
    public void initSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(ViplevelCfg.EXCEL_NAME, VipCfgCache::initData)
            .addInitSampleFileObserveWithCallBack(ViplevelCfg.EXCEL_NAME, VipCfgCache::initData);

    }

    /**
     * 请求vip基本信息
     *
     * @param playerController 玩家控制器
     * @param req              请求
     */
    public ResVipInfo reqVipInfo(PlayerController playerController, ReqVipInfo req) {
        ResVipInfo res = new ResVipInfo(Code.SUCCESS);
        try {
            Player player = playerService.getFromAllDB(playerController.playerId());
            long playerId = player.getId();
            ViplevelCfg viplevelCfg = VipCfgCache.getVipLevelCfg(player.getVipLevel());
            if (Objects.isNull(viplevelCfg)) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            Optional<Vip> vipOptional = vipService.getFromAllDB(playerId);
            Vip vip = vipOptional.orElseGet(() -> {
                Vip buildVip = Vip.buildVip(playerId);
                vipService.redisSave(playerId, buildVip);
                return buildVip;
            });
            long timeMillis = System.currentTimeMillis();
            res.vipGiftInfo = new ArrayList<>(VipGift.values().length);
            res.nowExp = player.getVipExp();
            res.vipLevel = player.getVipLevel();
            res.claimMaxLv = getMaxClaimLv(vip, player);
            for (VipGift gift : VipGift.values()) {
                VipGiftInfo vipGiftInfo = new VipGiftInfo();
                vipGiftInfo.type = gift.getType();
                long nextClaimNeed = gift.getNextClaimNeed(player, true);
                if (nextClaimNeed > 0) {
                    vipGiftInfo.nextTime = nextClaimNeed - timeMillis;
                }
                vipGiftInfo.needRecharge = gift.getNextClaimNeed(player, false);
                if (vipGiftInfo.needRecharge > 0) {
                    res.needExp = vipGiftInfo.needRecharge;
                }
                vipGiftInfo.camClaim = gift.isCanClaim(player, vip, timeMillis);
                res.vipGiftInfo.add(vipGiftInfo);
            }
            return res;
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求VIP信息异常 playerId:{}", playerController.playerId(), e);
        }
        return res;
    }

    public ResVipClaimGiftReward reqVipClaimGiftReward(PlayerController playerController, ReqVipClaimGiftReward req) {
        ResVipClaimGiftReward res = new ResVipClaimGiftReward(Code.SUCCESS);
        try {
            Player player = playerService.getFromAllDB(playerController.playerId());
            long playerId = player.getId();
            ViplevelCfg viplevelCfg = VipCfgCache.getVipLevelCfg(player.getVipLevel());
            if (Objects.isNull(viplevelCfg)) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            //当前vip配置
            Optional<Vip> vipOptional = vipService.getFromAllDB(playerId);
            if (vipOptional.isEmpty()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            VipGift gift = EnumUtil.getBy(VipGift.class, vipGift -> vipGift.getType() == req.type);
            if (Objects.isNull(gift)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            Vip vip = vipOptional.get();
            long timeMillis = System.currentTimeMillis();
            boolean canClaim = gift.isCanClaim(player, vip, timeMillis);
            if (!canClaim) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            if (vip.getLvGiftGetTime().containsKey(req.vipLevel)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            int nowMax = getMaxClaimLv(vip, player);
            if (nowMax != req.vipLevel) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            Map<Integer, Long> rewards;
            if (gift == VipGift.PROMOTION) {
                ViplevelCfg claimCfg = VipCfgCache.getVipLevelCfg(req.vipLevel);
                if (Objects.isNull(claimCfg)) {
                    res.code = Code.SAMPLE_ERROR;
                    return res;
                }
                rewards = gift.getReward().apply(claimCfg);

            } else {
                rewards = gift.getReward().apply(viplevelCfg);
            }
            if (CollectionUtil.isEmpty(rewards)) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            //修改数据
            if (gift == VipGift.PROMOTION) {
                vip.getLvGiftGetTime().put(req.vipLevel, timeMillis);
            } else {
                vip.getGiftGetTime().put(gift.getType(), timeMillis);
            }
            vipService.redisSave(playerId, vip);
            CommonResult<Void> addedItems = playerPackService.addItems(playerController.playerId(), rewards, "vip奖励领取");
            res.code = addedItems.code;
            if (!addedItems.success()) {
                return res;
            }
            VipGiftInfo vipGiftInfo = new VipGiftInfo();
            vipGiftInfo.type = gift.getType();
            vipGiftInfo.camClaim = gift.isCanClaim(player, vip, timeMillis);
            vipGiftInfo.needRecharge = gift.getNextClaimNeed(player, false);

            long nextClaimNeed = gift.getNextClaimNeed(player, true);
            if (nextClaimNeed > 0) {
                vipGiftInfo.nextTime = nextClaimNeed - timeMillis;
            }
            res.vipGiftInfo = vipGiftInfo;
            res.items = new ArrayList<>(rewards.size());
            for (Map.Entry<Integer, Long> entry : rewards.entrySet()) {
                ItemInfo info = new ItemInfo();
                info.itemId = entry.getKey();
                info.count = entry.getValue();
                res.items.add(info);
            }
            res.claimMaxLv = getMaxClaimLv(vip, player);
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("请求领取VIP信息异常 playerId:{}", playerController.playerId(), e);
        }
        return res;
    }

    public int getMaxClaimLv(Vip vip, Player player) {
        Map<Integer, Long> lvGiftGetTime = vip.getLvGiftGetTime();
        if (CollectionUtil.isEmpty(lvGiftGetTime)) {
            return 0;
        }
        return Math.min(player.getVipLevel(), Collections.max(lvGiftGetTime.keySet()) + 1);
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player) {
        //vip经验衰减
        ViplevelCfg vipLevelCfg = VipCfgCache.getVipLevelCfg(player.getVipLevel());
        if (Objects.nonNull(vipLevelCfg)) {
            Integer interval = vipLevelCfg.getRollback().getFirst();
            if (!interval.equals(-1)) {
                Account account = accountDao.queryAccountByPlayerId(player.getId());
                if (Objects.nonNull(account)) {
                    long lastOfflineTime = account.getLastOfflineTime();
                    long timeMillis = System.currentTimeMillis();
                    //计算差值
                    long difference = TimeHelper.calculateDifference(ChronoUnit.MINUTES, lastOfflineTime, timeMillis);
                    if (difference >= interval) {
                        //经验衰减
                        Player doneSave = playerService.doSave(player.getId(), vipLevelCfg.getRollback().getLast(),
                            (savePlayer, value) -> {
                                savePlayer.setVipExp(Math.max(player.getVipExp() - value, 0));
                            });
                        playerController.setPlayer(doneSave);
                    }
                }
            }
        }
        //获取上次退出时间
    }
}
