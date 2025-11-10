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
    //生成结果库的状态
    String GENERATE_LIB_STATUS = "generateLibStatus";

    //保存商品
    String SAVE_SHOP_PRODUCTS = "saveShopProducts";
    //删除商品
    String DEL_SHOP_PRODUCTS = "delShopProducts";
    //修改黑名单
    String CHANGE_BLACK_LIST = "changeBlackList";
    //服务器节点列表
    String QUERY_GAME_SERVER_NODE_LIST = "queryGameServerNodeList";
    //修改服务器信息
    String CHANG_GAME_NODE_INFO = "changeGameNodeInfo";

    //获取全部活动数据
    String GET_ALL_ACTIVITY_DATA = "getAllActivityData";
    //更新excel配置
    String UPDATE_EXCEL_CONFIGS = "updateExcelConfigs";
    //更新登录配置
    String CHANGE_LOGIN_CONFIG = "changeLoginConfig";
    //修改玩家积分大奖的积分
    String CHANGE_PLAYER_POINTS = "changePlayerPoints";

    //保存公告
    String SAVE_NOTICE = "saveNotice";
    //删除公告
    String DEL_NOTICE = "delNotice";

    /**
     * 配置相关q
     */
    interface Config {
        //加载配置列表
        String GET_CONFIG_LIST = "getConfigList";
        //覆盖配置
        String REPLACE_CONFIG = "replaceConfig";
        //删除配置
        String DELETE_CONFIG = "deleteConfig";
        //同步配置
        String SYNC_CONFIG = "syncConfig";
    }

    interface Result {
        String SUCCESS = "success";
        String FAIL = "fail";
    }
}
