package com.jjg.game.core.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * vip工具类
 *
 * @author lm
 * @date 2025/8/29 10:52
 */
@Component
public class VipUtil implements GameEventListener, ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(VipUtil.class);
    //节点管理
    private final NodeManager nodeManager;
    private final CoreSendMessageManager sendMessageManager;
    private final CorePlayerService playerService;
    // vip配置缓存
    private static Map<Integer, ViplevelCfg> VIP_LEVEL_CFG_MAP = new HashMap<>();

    public VipUtil(NodeManager nodeManager, CoreSendMessageManager sendMessageManager, CorePlayerService playerService) {
        this.nodeManager = nodeManager;
        this.sendMessageManager = sendMessageManager;
        this.playerService = playerService;
    }

    public void initVipLevelCfg() {
        VIP_LEVEL_CFG_MAP = GameDataManager.getViplevelCfgList()
                .stream()
                .collect(Collectors.toMap(ViplevelCfg::getViplevel, cfg -> cfg));
    }

    public static Map<Integer, ViplevelCfg> getVipLevelCfgMap() {
        return VIP_LEVEL_CFG_MAP;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(ViplevelCfg.EXCEL_NAME, this::initVipLevelCfg)
                .addChangeSampleFileObserveWithCallBack(ViplevelCfg.EXCEL_NAME, this::initVipLevelCfg);
    }

    /**
     * 充值时检查vip等级
     *
     * @param player   玩家数据
     * @param addValue 充值金额
     * @return vip等级是否改变
     */
    public static boolean rechargeCheckVipLevel(Player player, BigDecimal addValue) {
        //计算转换比例
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(46);
        long finalAdd = addValue.longValue();
        if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
            String[] cfg = StringUtils.split(globalConfigCfg.getValue(), "_");
            if (cfg.length == 2) {
                finalAdd = addValue
                        .multiply(new BigDecimal(cfg[1]))
                        .divide(new BigDecimal(cfg[0]).setScale(2, RoundingMode.DOWN), 2, RoundingMode.DOWN).longValue();
            }
        }
        return checkVipLevel(player, finalAdd, true);
    }

    /**
     * 有效下注检查vip等级
     *
     * @param player   玩家数据
     * @param addValue 增加值
     * @return vip等级是否变化
     */
    public static boolean bettingCheckVipLevel(Player player, long addValue) {
        return checkVipLevel(player, addValue, false);
    }

    /**
     * 检查vip等级
     *
     * @param player   玩家数据
     * @param addValue 增加值
     * @param recharge 是否是充值
     * @return vip等级是否变化
     */
    public static boolean checkVipLevel(Player player, long addValue, boolean recharge) {
        Map<Integer, ViplevelCfg> vipLevelCfgMap = getVipLevelCfgMap();
        if (CollectionUtil.isEmpty(vipLevelCfgMap)) {
            return false;
        }
        //进行经验升级
        int newLv = player.getVipLevel();
        long newExp = player.getVipExp();
        boolean chenge = false;
        for (int i = 0; i < vipLevelCfgMap.size(); i++) {
            ViplevelCfg viplevelCfg = vipLevelCfgMap.get(newLv);
            if (Objects.isNull(viplevelCfg)) {
                break;
            }
            int coefficient = recharge ? viplevelCfg.getRecharge() : viplevelCfg.getEffectiveBetting();
            if (coefficient == 0) {
                break;
            }
            long needEffectiveWaterFlow = BigDecimal.valueOf(viplevelCfg.getViplevelUpExp() - newExp)
                    .multiply(BigDecimal.valueOf(10000))
                    .divide(BigDecimal.valueOf(coefficient), RoundingMode.DOWN).longValue();
            if (addValue >= needEffectiveWaterFlow) {
                addValue -= needEffectiveWaterFlow;
                newExp = 0;
                newLv++;
                chenge = true;
            } else {
                newExp += BigDecimal.valueOf(addValue)
                        .multiply(BigDecimal.valueOf(coefficient))
                        .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN).longValue();
                addValue = 0;
            }
            if (addValue == 0) {
                break;
            }
        }
        if (chenge || newExp != player.getVipExp()) {
            player.setVipExp(newExp);
            player.setVipLevel(newLv);
        }
        return chenge;
    }

    /**
     * 检查vip等级
     *
     * @param player 玩家信息
     * @param num    数量
     */
    public static void checkVipLevel(Player player, long num) {
        bettingCheckVipLevel(player, num);
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            boolean inMemoryNode = nodeManager.isPlayerDataInMemoryNode();
            Player player = event.getPlayer();
            if (inMemoryNode) {
                log.info("玩家数据在内存中,直接返回,让对应接口处理,不统一处理 player:{} order:{}", player.getId(), JSON.toJSONString(event.getOrder()));
                return;
            }
            Order order = event.getOrder();
            BigDecimal rechargeAmount = BigDecimal.valueOf(order.getPrice());
            boolean change = rechargeCheckVipLevel(player, rechargeAmount);
            //回存玩家数据
            Player newPlayer = playerService.doSave(player.getId(), (oldPlayer) -> {
                oldPlayer.setVipExp(player.getVipExp());
                oldPlayer.setVipLevel(player.getVipLevel());
            });
            if (change) {
                //信息变化推送一次
                sendMessageManager.buildMoneyChangeMessage(newPlayer);
            }
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }
}
