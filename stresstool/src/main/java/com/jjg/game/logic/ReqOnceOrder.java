package com.jjg.game.logic;

/**
 * 主要处理登陆流程
 *
 * <p>不要在登陆里面获取道具 在功能模块内获取自己需要的道具 并销毁
 *
 * @author 2CL
 */
public interface ReqOnceOrder {
    /**
     * 请求登录
     */
    int REQ_LOGIN = 0;
    /**
     * 请求已经解锁的的功能 ID
     */
    int REQ_UNLOCKED_FUNCTION = 1;
    /**
     * 玩家改名
     */
    int REQ_CHANGE_NAME = 2;
    /**
     * 清除非使用道具 *
     */
    int REQ_REMOVE_UNUSEDITEMS = 3;
    /** 不要在 登陆里面 获取道具 * */
    /** 在功能模块内获取自己需要的道具 并销毁 * */
    /**
     * 获取背包 并销毁没有使用的道具
     */
    int REQ_ITEM_LIST = 4;
    /**
     * 精灵基本信息
     */
    int REQ_HEROS_INFO = 5;
    /**
     * 获取基建初始信息
     */
    int REQ_SHIP_ALL_INFO = 7;
    /**
     * 获取所有商店的初始信息
     */
    int REQ_SHOPS_ALL_INFO = 8;

    /**
     * 客户端所有初始化数据已经加载完毕 *
     */
    int REQ_LOADOK = 50;
}