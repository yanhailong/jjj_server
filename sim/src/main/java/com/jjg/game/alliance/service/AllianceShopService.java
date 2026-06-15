package com.jjg.game.alliance.service;

import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.pb.res.ResShopBuy;
import com.jjg.game.alliance.pb.res.ResShopList;
import com.jjg.game.alliance.pb.struct.AllianceShopGoodsInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.service.PlayerPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

/**
 * 联盟商店: 贡献值兑换道具。
 * <p>
 * 限购记录在玩家侧文档、随玩家不随联盟 (需求: 换盟后当日限购仍生效);
 * 商品按联盟等级解锁; 0 点自然日切。贡献值扣减走条件 $inc 兜底并发,
 * 扣减成功后发货 —— 发货走 PlayerPackService 不会失败丢道具(入背包/邮件兜底由其内部保证)。
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class AllianceShopService {
    private static final Logger log = LoggerFactory.getLogger(AllianceShopService.class);

    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private AllianceCacheService cacheService;
    @Autowired
    private AllianceConfigService configService;
    @Autowired
    private AllianceAssetService assetService;
    @Autowired
    private PlayerPackService playerPackService;

    /**
     * 商店列表。
     */
    public ResShopList shopList(long playerId) {
        ResShopList res = new ResShopList(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();

        res.myContribution = playerData.getContribution();
        //商店每日 0 点刷新 (需求: 24:00)
        res.refreshTime = TimeHelper.getTomorrowZeroSecondTime(System.currentTimeMillis()) * 1000L;
        res.goods = new ArrayList<>();
        for (AllianceConfigService.ShopGoodsCfg cfg : configService.shopGoods()) {
            AllianceShopGoodsInfo info = new AllianceShopGoodsInfo();
            info.goodsId = cfg.goodsId();
            info.itemId = cfg.itemId();
            info.count = cfg.count();
            info.price = cfg.price();
            info.dailyLimit = cfg.dailyLimit();
            info.boughtToday = playerData.shopPurchasedOf(today, cfg.goodsId());
            info.unlockLevel = cfg.unlockLevel();
            info.unlocked = alliance.getLevel() >= cfg.unlockLevel();
            res.goods.add(info);
        }
        return res;
    }

    /**
     * 购买: 等级解锁 -> 每日限购 -> 条件扣贡献值 -> 发货。
     */
    public ResShopBuy buy(long playerId, int goodsId) {
        ResShopBuy res = new ResShopBuy(Code.SUCCESS);
        res.goodsId = goodsId;
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.ALLIANCE_NOT_MEMBER;
            return res;
        }
        AllianceConfigService.ShopGoodsCfg cfg = configService.shopGoods(goodsId);
        if (cfg == null) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (alliance.getLevel() < cfg.unlockLevel()) {
            res.code = Code.NOT_UNLOCKED;
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        int bought = playerData.shopPurchasedOf(today, goodsId);
        if (bought >= cfg.dailyLimit()) {
            res.code = Code.ALLIANCE_SHOP_SOLD_OUT;
            return res;
        }
        //条件扣减贡献值 (余额不足返回 false)
        if (!alliancePlayerDao.tryPurchase(playerId, today, goodsId, cfg.dailyLimit(), cfg.price())) {
            AlliancePlayerData latest = alliancePlayerDao.getOrEmpty(playerId);
            if (latest.shopPurchasedOf(today, goodsId) >= cfg.dailyLimit()) {
                res.code = Code.ALLIANCE_SHOP_SOLD_OUT;
                return res;
            }
            res.code = Code.ALLIANCE_CONTRIBUTION_NOT_ENOUGH;
            return res;
        }
        //限购计数 (跨天首次购买重置 map)
        //发货
        playerPackService.addItems(playerId, Map.of(cfg.itemId(), cfg.count()),
                AddType.ALLIANCE_SHOP_BUY, "联盟商店兑换", true);

        res.itemId = cfg.itemId();
        res.count = cfg.count();
        res.myContribution = Math.max(0, alliancePlayerDao.getOrEmpty(playerId).getContribution());
        log.info("联盟商店兑换 playerId={},goodsId={},price={}", playerId, goodsId, cfg.price());
        return res;
    }
}
