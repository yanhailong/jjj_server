package com.jjg.game.alliance.service;

import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.data.AlliancePlayerData;
import com.jjg.game.alliance.pb.res.ResAllianceShopBuy;
import com.jjg.game.alliance.pb.res.ResAllianceShopList;
import com.jjg.game.alliance.pb.struct.AllianceShopGoodsInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AllianceShopCfg;
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
    private PlayerPackService playerPackService;

    /**
     * 商店列表。
     */
    public ResAllianceShopList shopList(long playerId) {
        ResAllianceShopList res = new ResAllianceShopList(Code.SUCCESS);
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();

        res.myContribution = playerData.getContribution();
        //商店每日 0 点刷新 (需求: 24:00)
        res.refreshTime = TimeHelper.getTomorrowZeroSecondTime(System.currentTimeMillis()) * 1000L;
        res.goods = new ArrayList<>();
        for (AllianceShopCfg cfg : GameDataManager.getAllianceShopCfgList()) {
            AllianceShopGoodsInfo info = new AllianceShopGoodsInfo();
            info.goodsId = cfg.getId();
            info.itemId = cfg.getGoods().get(0);
            info.count = cfg.getGoods().get(1);
            info.price = cfg.getCost().get(1);
            info.dailyLimit = cfg.getDailyPurchaseLimit();
            info.boughtToday = playerData.shopPurchasedOf(today, cfg.getId());
            info.unlockLevel = cfg.getPurchaseLevel();
            info.unlocked = alliance.getLevel() >= cfg.getPurchaseLevel();
            res.goods.add(info);
        }
        return res;
    }

    /**
     * 购买: 等级解锁 -> 每日限购 -> 条件扣贡献值 -> 发货。
     */
    public ResAllianceShopBuy buy(long playerId, int goodsId, int count) {
        ResAllianceShopBuy res = new ResAllianceShopBuy(Code.SUCCESS);
        res.goodsId = goodsId;
        if (count <= 0) {
            res.code = Code.PARAM_ERROR;
            log.warn("联盟商店兑换失败,购买数量非法 playerId={},goodsId={},count={}", playerId, goodsId, count);
            return res;
        }
        long allianceId = cacheService.getAllianceId(playerId);
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null) {
            res.code = Code.NOT_FOUND;
            log.warn("联盟商店兑换失败,玩家不在联盟 playerId={},goodsId={}", playerId, goodsId);
            return res;
        }
        AllianceShopCfg cfg = GameDataManager.getAllianceShopCfg(goodsId);
        if (cfg == null) {
            res.code = Code.PARAM_ERROR;
            log.warn("联盟商店兑换失败,商品配置不存在 playerId={},goodsId={}", playerId, goodsId);
            return res;
        }
        if (alliance.getLevel() < cfg.getPurchaseLevel()) {
            res.code = Code.NOT_UNLOCKED;
            log.warn("联盟商店兑换失败,联盟等级未解锁该商品 playerId={},goodsId={},allianceLevel={},unlockLevel={}", playerId, goodsId, alliance.getLevel(), cfg.getPurchaseLevel());
            return res;
        }
        AlliancePlayerData playerData = alliancePlayerDao.getOrEmpty(playerId);
        int today = TimeHelper.getDayNumerical();
        int bought = playerData.shopPurchasedOf(today, goodsId);
        if (bought + count > cfg.getDailyPurchaseLimit()) {
            res.code = Code.FORBID;
            log.warn("联盟商店兑换失败,超出今日限购 playerId={},goodsId={},bought={},count={},limit={}", playerId, goodsId, bought, count, cfg.getDailyPurchaseLimit());
            return res;
        }
        //条件扣减贡献值 (余额不足/超限返回 false)
        if (!alliancePlayerDao.tryPurchase(playerId, today, goodsId, cfg.getDailyPurchaseLimit(), cfg.getCost().get(1), count)) {
            AlliancePlayerData latest = alliancePlayerDao.getOrEmpty(playerId);
            if (latest.shopPurchasedOf(today, goodsId) + count > cfg.getDailyPurchaseLimit()) {
                res.code = Code.FORBID;
                log.warn("联盟商店兑换失败,超出今日限购(并发) playerId={},goodsId={},count={},limit={}", playerId, goodsId, count, cfg.getDailyPurchaseLimit());
                return res;
            }
            res.code = Code.NOT_ENOUGH;
            log.warn("联盟商店兑换失败,贡献值不足 playerId={},goodsId={},price={},count={},myContribution={}", playerId, goodsId, cfg.getCost().get(1), count, latest.getContribution());
            return res;
        }
        //限购计数 (跨天首次购买重置 map)
        //发货 (单份道具数 * 购买份数)
        long totalItems = cfg.getGoods().get(1) * count;
        playerPackService.addItems(playerId, Map.of(cfg.getGoods().get(0), totalItems),
                AddType.ALLIANCE_SHOP_BUY, "", true);

        res.itemId = cfg.getGoods().get(0);
        res.count = totalItems;
        res.myContribution = Math.max(0, alliancePlayerDao.getOrEmpty(playerId).getContribution());
        log.info("联盟商店兑换 playerId={},goodsId={},count={},price={}", playerId, goodsId, count, cfg.getGoods().get(1));
        return res;
    }
}
