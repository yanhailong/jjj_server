package com.jjg.game.sim.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimItemOperationResult;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 在sim模块中如果要添加道具，一律用该service
 *
 * @author 11
 * @date 2026/6/11
 */
@Service
public class SimPackService {
    private static final Logger log = LoggerFactory.getLogger(SimPackService.class);

    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private SimCasinoDao simCasinoDao;
    //@Lazy 打破循环: AllianceTaskService -> 本类 -> SimManager -> AllianceEventService -> AllianceTaskService
    @Lazy
    @Autowired
    private SimManager simManager;


    /**
     * 添加道具 (BuildingOutputType 维度): 转换为 itemId 后统一入账
     */
    public CommonResult<SimItemOperationResult> addItem(SimPlayerContext ctx, Map<BuildingOutputType, Long> resources, AddType addType, String desc, boolean notify) {
        return addItems(ctx, toItemMap(resources), addType, desc, notify);
    }

    /**
     * 添加道具 (itemId 维度统一入口)
     *
     * @param ctx
     * @param items
     * @param addType
     * @param desc
     * @param notify
     * @return
     */
    public CommonResult<SimItemOperationResult> addItems(SimPlayerContext ctx, Map<Integer, Long> items, AddType addType, String desc, boolean notify) {
        CommonResult<SimItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
        if (items == null || items.isEmpty()) {
            result.code = Code.FAIL;
            return result;
        }

        Map<Integer, Long> packItems = new HashMap<>(items.size());
        for (Map.Entry<Integer, Long> en : items.entrySet()) {
            int itemId = en.getKey();
            long count = en.getValue();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.ID_POWER) {  //能量
                SimBaseData base = ctx.getSimBaseData();
                base.setPower(base.getPower() + (int) count);
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {  //知名度
                SimCasinoData casino = ctx.getCurrentCasino();
                if (casino != null) {
                    casino.setAwareness(casino.getAwareness() + (int) count);
                }
            } else if (itemId == SimConstant.Item.ID_EXPOD) {  //曝光度

            } else if (itemId == SimConstant.Item.ID_RESEARCH_POINT) {  //研究点
                SimBaseData base = ctx.getSimBaseData();
                base.addResearchPoint(SimConstant.ResearchPoint.NORMAL_TPYE, (int) count);
            } else if (itemId == SimConstant.Item.ID_RARE_RESEARCH_POINT) {  //稀有研究点
                SimBaseData base = ctx.getSimBaseData();
                base.addResearchPoint(SimConstant.ResearchPoint.RARE_TPYE, (int) count);
            } else {
                packItems.merge(itemId, count, Long::sum);
            }
        }

        SimItemOperationResult data = new SimItemOperationResult();
        if (!packItems.isEmpty()) {
            CommonResult<ItemOperationResult> itemResult = playerPackService.addItems(ctx.playerId(), packItems, addType, desc, notify);
            if (itemResult.success() && itemResult.data != null) {
                data = SimItemOperationResult.createFromItemResult(itemResult.data);
            }
//            log.info("添加道具 playerId={},items={},gold={}", ctx.playerId(), packItems, itemResult.data.getGoldNum());
        }
        //回填 sim 特殊资源最新值, 供下发客户端
        data.setPower(ctx.getSimBaseData().getPower());
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino != null) {
            data.setAwareness(casino.getAwareness());
        }
        data.setResearchPoint(ctx.getSimBaseData().findResearchPoint(SimConstant.ResearchPoint.NORMAL_TPYE));
        data.setRareResearchPoint(ctx.getSimBaseData().findResearchPoint(SimConstant.ResearchPoint.RARE_TPYE));
        result.data = data;
        return result;
    }

    /**
     * 按 playerId 添加道具 (联盟等跨节点发奖统一入口):
     * 玩家在本节点在线则走 {@link #addItems} 正确入账 sim 内存资源/背包;
     * 不在本节点则走 {@link #addItemsOffline} 直接写入持久化数据。
     */
    public void addItemsByPlayerId(long playerId, Map<Integer, Long> items, AddType addType, String desc, boolean notify) {
        if (items == null || items.isEmpty()) {
            return;
        }
        SimPlayerContext ctx = simManager.getContext(playerId);
        if (ctx != null) {
            addItems(ctx, items, addType, desc, notify);
            return;
        }
        addItemsOffline(playerId, items, addType, desc, notify);
    }

    /**
     * 离线 (玩家不在本节点) 添加道具: sim 特殊资源直接写入持久化数据 (能量/研究点入 SimBaseData,
     * 知名度入当前场景 SimCasinoData), 其余道具走背包。
     * 注: 若玩家此刻正在其它节点在线, 该节点的内存快照落库可能覆盖此处直写 —— 联盟任务离线发奖
     * 仅 onTaskHelped 触发, 概率低, 暂可接受。
     */
    private void addItemsOffline(long playerId, Map<Integer, Long> items, AddType addType, String desc, boolean notify) {
        int powerAdd = 0;
        int normalResAdd = 0;
        int rareResAdd = 0;
        int awarenessAdd = 0;
        Map<Integer, Long> packItems = new HashMap<>(items.size());
        for (Map.Entry<Integer, Long> en : items.entrySet()) {
            int itemId = en.getKey();
            long count = en.getValue();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.ID_POWER) {
                powerAdd += (int) count;
            } else if (itemId == SimConstant.Item.ID_RESEARCH_POINT) {
                normalResAdd += (int) count;
            } else if (itemId == SimConstant.Item.ID_RARE_RESEARCH_POINT) {
                rareResAdd += (int) count;
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {
                awarenessAdd += (int) count;
            } else if (itemId == SimConstant.Item.ID_EXPOD) {
                //曝光度无内存承载, 与在线入账一致丢弃
            } else {
                packItems.merge(itemId, count, Long::sum);
            }
        }

        //能量 / 研究点: 写 SimBaseData; 知名度: 写当前场景 SimCasinoData
        if (powerAdd > 0 || normalResAdd > 0 || rareResAdd > 0 || awarenessAdd > 0) {
            SimBaseData base = simPlayerGameDao.findById(playerId).orElse(null);
            if (base == null) {
                log.warn("离线发放sim资源失败, 无SimBaseData playerId={},items={}", playerId, items);
            } else {
                if (powerAdd > 0) {
                    base.setPower(base.getPower() + powerAdd);
                }
                if (normalResAdd > 0) {
                    base.addResearchPoint(SimConstant.ResearchPoint.NORMAL_TPYE, normalResAdd);
                }
                if (rareResAdd > 0) {
                    base.addResearchPoint(SimConstant.ResearchPoint.RARE_TPYE, rareResAdd);
                }
                if (powerAdd > 0 || normalResAdd > 0 || rareResAdd > 0) {
                    simPlayerGameDao.save(base);
                }
                if (awarenessAdd > 0) {
                    addAwarenessOffline(playerId, base.getCurrentCasinoId(), awarenessAdd);
                }
            }
        }

        if (!packItems.isEmpty()) {
            playerPackService.addItems(playerId, packItems, addType, desc, notify);
        }
    }

    /**
     * 离线知名度入账: 优先当前场景, currentCasinoId 失效则回退任一场景。
     */
    private void addAwarenessOffline(long playerId, int currentCasinoId, int awarenessAdd) {
        SimCasinoData casino = currentCasinoId > 0 ? simCasinoDao.findOne(playerId, currentCasinoId) : null;
        if (casino == null) {
            casino = simCasinoDao.findFirstByPlayerId(playerId);
        }
        if (casino == null) {
            log.warn("离线发放知名度失败, 无场景 playerId={},awareness={}", playerId, awarenessAdd);
            return;
        }
        casino.setAwareness(casino.getAwareness() + awarenessAdd);
        simCasinoDao.save(casino);
    }

    /**
     * 扣除道具 (itemId 维度统一入口): 先校验充足再扣除, 任一不足整体失败且不产生扣除。
     *
     * @return 扣除成功返回 true
     */
    public boolean removeItems(SimPlayerContext ctx, Map<Integer, Long> items, AddType addType, String desc) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        long needPower = 0;
        long needAwareness = 0;
        Map<Integer, Long> packItems = new HashMap<>(items.size());
        for (Map.Entry<Integer, Long> en : items.entrySet()) {
            int itemId = en.getKey();
            long count = en.getValue();
            if (count <= 0) {
                continue;
            }
            if (itemId == SimConstant.Item.ID_POWER) {
                needPower += count;
            } else if (itemId == SimConstant.Item.ID_AWARENESS) {
                needAwareness += count;
            } else {
                packItems.merge(itemId, count, Long::sum);
            }
        }

        SimBaseData base = ctx.getSimBaseData();
        SimCasinoData casino = ctx.getCurrentCasino();
        //先校验特殊资源是否充足
        if (needPower > 0 && base.getPower() < needPower) {
            log.warn("扣除道具失败, 能量不足 playerId={},need={},have={}", ctx.playerId(), needPower, base.getPower());
            return false;
        }
        if (needAwareness > 0 && (casino == null || casino.getAwareness() < needAwareness)) {
            log.warn("扣除道具失败, 知名度不足 playerId={},need={},have={}", ctx.playerId(), needAwareness, casino == null ? 0 : casino.getAwareness());
            return false;
        }
        //背包道具扣除 (失败不产生副作用, 此时特殊资源尚未扣除)
        if (!packItems.isEmpty()) {
            CommonResult<ItemOperationResult> r = playerPackService.removeItems(ctx.getPlayerController().getPlayer(), packItems, addType, desc);
            if (!r.success()) {
                log.warn("扣除道具失败 playerId={},items={},code={}", ctx.playerId(), packItems, r.code);
                return false;
            }
        }
        //背包扣除成功后再扣除特殊资源
        if (needPower > 0) {
            base.setPower(base.getPower() - (int) needPower);
        }
        if (needAwareness > 0) {
            casino.setAwareness(casino.getAwareness() - (int) needAwareness);
        }
        return true;
    }

    /**
     * 扣除单个道具
     */
    public boolean removeItem(SimPlayerContext ctx, int itemId, long count, AddType addType) {
        if (count <= 0) {
            return true;
        }
        return removeItems(ctx, Collections.singletonMap(itemId, count), addType, null);
    }

    /**
     * BuildingOutputType 产出映射为 itemId (无对应道具的产出类型返回 null, 不入账)
     */
    private Map<Integer, Long> toItemMap(Map<BuildingOutputType, Long> resources) {
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> items = new HashMap<>(resources.size());
        for (Map.Entry<BuildingOutputType, Long> en : resources.entrySet()) {
            Integer itemId = toItemId(en.getKey());
            if (itemId == null) {
                continue;
            }
            items.merge(itemId, en.getValue(), Long::sum);
        }
        return items;
    }


    private Integer toItemId(BuildingOutputType type) {
        return switch (type) {
            case GOLD -> ItemUtils.getGoldItemId();
            case POWER -> SimConstant.Item.ID_POWER;
            case AWARENESS -> SimConstant.Item.ID_AWARENESS;
            default -> null;
        };
    }
}
