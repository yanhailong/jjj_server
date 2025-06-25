package com.jjg.game.logic.constant;

/**
 * GM 操作常量
 *
 * @author 2CL
 */
public interface GmOperateConstant {
  /** 操作前缀 */
  String OPERATE_PREFIX = "./";
  /** 通过道具字符串列表添加道具 */
  String ADD_ITEMS = "addItemList";
  /** 通过道具字符串列表移除道具 */
  String REMOVE_ITEMS = "delItemList";
  /** 清除所有未使用的道具 */
  String CLEAN_ALL_UNUSED_ITEM = "cleanUnUsedItems";
}
