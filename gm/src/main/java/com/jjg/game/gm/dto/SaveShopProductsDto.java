package com.jjg.game.gm.dto;

import com.jjg.game.core.data.ShopProduct;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/18 9:44
 */
public record SaveShopProductsDto(
        List<ShopProduct> products
) {
}
