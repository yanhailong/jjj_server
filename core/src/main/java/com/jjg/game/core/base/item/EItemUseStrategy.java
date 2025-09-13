package com.jjg.game.core.base.item;

import com.jjg.game.common.utils.CommonUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 道具策略类
 *
 * @author 2CL
 */
public enum EItemUseStrategy {
    // 皮肤自使用
    SKIN_AUTO_USE(3, SkinItemUse.class),
    ;
    // 具体的使用策略
    private final Class<? extends IItemUseInterface> useStrategy;
    // 道具类型
    private final int itemType;

    EItemUseStrategy(int itemType, Class<? extends IItemUseInterface> useStrategy) {
        this.useStrategy = useStrategy;
        this.itemType = itemType;
    }

    public int getItemType() {
        return itemType;
    }

    public IItemUseInterface getUseStrategy() {
        return CommonUtil.getContext().getBean(useStrategy);
    }

    /**
     * 是否是自动使用类型
     */
    public static boolean isAutoUseType(int itemType) {
        Set<Integer> autoUseType =
            Arrays.stream(values()).map(EItemUseStrategy::getItemType).collect(Collectors.toSet());
        return autoUseType.contains(itemType);
    }

    public static EItemUseStrategy getItemUseStrategy(int itemType) {
        for (EItemUseStrategy value : values()) {
            if (value.itemType == itemType) {
                return value;
            }
        }
        return null;
    }
}
