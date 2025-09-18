package com.jjg.game.core.constant;

/**
 * @author lm
 * @since 2025/7/15 16:33
 */
public interface BackendGMCmd {
    String CHANGE_GAME_STATUS = "changeGameStatus";
    //跑马灯
    String SNED_MARQUEE = "sendMarquee";
    //停止跑马灯
    String STOP_MARQUEE = "stopMarquee";
    //查询账户
    String QUERY_ACCOUNT = "queryAccount";
    //金币gm
    String GOLD_OPERATOR = "goldOperator";
    //发送邮件
    String SEND_EMAIL = "sendEmail";
    //踢玩家
    String KICK_ACCOUNT = "kickAccount";
    //封禁账号
    String BAN_ACCOUNT = "banAccount";
    //在线玩家信息
    String PLAYING_INFO = "playingInfo";
    //添加/更新轮播数据
    String REPLACE_CAROUSEL = "eventImage";
    //删除轮播数据
    String DELETE_CAROUSEL = "eventImageDel";
    //同步轮播数据
    String SYNC_CAROUSEL = "syncEventImage";
    //生成结果库
    String GENERATE_LIB = "generateLib";
    //保存商品
    String SAVE_SHOP_PRODUCTS = "saveShopProducts";
    //删除商品
    String DEL_SHOP_PRODUCTS = "delShopProducts";

    interface Result {
        String SUCCESS = "success";
        String FAIL = "fail";
    }
}
