package com.jjg.game.social.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.utils.TimeHelper;

/**
 * 社交(好友 + 聊天)模块常量。
 * <p>
 * 消息号段复用框架已预留的 {@link MessageConst.MessageTypeDef#SOCIAL}(0x56);
 * 聊天采用"频道字段"复用一套收发协议, 好友相关独立成消息, 玩家信息卡独立成消息。
 * <p>
 * 所有数值型可调参数集中在 {@link Cfg} 中以死常量配置 (与需求文档一致), 需要调整直接改这里即可。
 *
 * @author 11
 * @date 2026/6/9
 */
public interface SocialConst {

    /**
     * 客户端 <-> 服务端 消息号 (messageType = SOCIAL)
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SOCIAL << MessageConst.MessageCommon.RIGHT_MOVE;

        //----------------- 聊天 (世界/系统/联盟/房间/私聊 共用, 以 channel 字段区分) -----------------
        //发送聊天
        int REQ_SEND_CHAT = BASE_MSG_PREFIX | 0x01;
        int RES_SEND_CHAT = BASE_MSG_PREFIX | 0x02;

        //拉取聊天历史
        int REQ_PULL_CHAT_HISTORY = BASE_MSG_PREFIX | 0x03;
        int RES_CHAT_HISTORY = BASE_MSG_PREFIX | 0x04;

        //新聊天消息下发 (服务端 -> 客户端)
        int NOTIFY_CHAT = BASE_MSG_PREFIX | 0x05;

        //----------------- 好友 -----------------
        //好友列表
        int REQ_FRIEND_LIST = BASE_MSG_PREFIX | 0x10;
        int RES_FRIEND_LIST = BASE_MSG_PREFIX | 0x11;

        //搜索玩家
        int REQ_SEARCH_PLAYER = BASE_MSG_PREFIX | 0x12;
        int RES_SEARCH_PLAYER = BASE_MSG_PREFIX | 0x13;

        //发送好友申请
        int REQ_ADD_FRIEND = BASE_MSG_PREFIX | 0x14;
        int RES_ADD_FRIEND = BASE_MSG_PREFIX | 0x15;

        //收到的申请列表
        int REQ_REQUEST_LIST = BASE_MSG_PREFIX | 0x16;
        int RES_REQUEST_LIST = BASE_MSG_PREFIX | 0x17;

        //处理申请 (同意/拒绝, 支持一键)
        int REQ_HANDLE_REQUEST = BASE_MSG_PREFIX | 0x18;
        int RES_HANDLE_REQUEST = BASE_MSG_PREFIX | 0x19;

        //删除好友 (支持一键)
        int REQ_DELETE_FRIEND = BASE_MSG_PREFIX | 0x1A;
        int RES_DELETE_FRIEND = BASE_MSG_PREFIX | 0x1B;

        //黑名单操作 (拉黑/移除/一键移除)
        int REQ_BLACKLIST_OP = BASE_MSG_PREFIX | 0x1C;
        int RES_BLACKLIST_OP = BASE_MSG_PREFIX | 0x1D;

        //黑名单列表
        int REQ_BLACKLIST_LIST = BASE_MSG_PREFIX | 0x1E;
        int RES_BLACKLIST_LIST = BASE_MSG_PREFIX | 0x1F;

        //赠送礼物 (支持一键)
        int REQ_SEND_GIFT = BASE_MSG_PREFIX | 0x20;
        int RES_SEND_GIFT = BASE_MSG_PREFIX | 0x21;

        //一键收送(领取好友赠送的礼物)
        int REQ_COLLECT_GIFT = BASE_MSG_PREFIX | 0x22;
        int RES_COLLECT_GIFT = BASE_MSG_PREFIX | 0x23;

        //好友状态变更通知
        int NOTIFY_FRIEND_STATUS = BASE_MSG_PREFIX | 0x30;
        //收到新好友申请通知
        int NOTIFY_FRIEND_REQUEST = BASE_MSG_PREFIX | 0x31;
        //收到好友赠礼通知
        int NOTIFY_GIFT_RECEIVED = BASE_MSG_PREFIX | 0x32;
        //好友申请处理结果
        int NOTIFY_NEW_FRIEND_HANDLE = BASE_MSG_PREFIX | 0x33;
        //通知删除好友
        int NOTIFY_DELETE_FRIEND = BASE_MSG_PREFIX | 0x34;


        //----------------- 玩家信息卡 -----------------
        //玩家信息
        int REQ_PLAYER_CARD = BASE_MSG_PREFIX | 0x40;
        int RES_PLAYER_CARD = BASE_MSG_PREFIX | 0x41;



        //----------------- 私聊会话 -----------------
        //会话列表
        int REQ_CONVERSATION_LIST = BASE_MSG_PREFIX | 0x50;
        int RES_CONVERSATION_LIST = BASE_MSG_PREFIX | 0x51;

        //删除私聊会话
        int REQ_DELETE_CONVERSATION = BASE_MSG_PREFIX | 0x52;
        int RES_DELETE_CONVERSATION = BASE_MSG_PREFIX | 0x53;
    }

    /**
     * 好友申请处理类型 / 黑名单操作类型
     */
    interface OpType {
        //处理申请
        int REJECT = 0;
        int AGREE = 1;
        //黑名单操作
        int BLACKLIST_ADD = 1;
        int BLACKLIST_REMOVE = 2;
        int BLACKLIST_CLEAR = 3;
    }

    /**
     * 数值型配置 (死常量, 与需求文档一致; 需调整直接改这里)。
     */
    interface Cfg {
        //--------- 世界聊天 ---------
        //单条消息最大字数
        int WORLD_MSG_MAX_LEN = 200;
        //同一玩家最小发送间隔(秒)
        int WORLD_SEND_INTERVAL_SEC = 5;
        //节点级全服发送上限(条/秒): 每条世界消息要扇出给全部在线客户端, 必须封顶;
        //多个 hall 节点时全服实际上限 = 该值 × 节点数
        int WORLD_GLOBAL_QPS_LIMIT = 20;
        //服务器常驻缓存条数(超出淘汰最旧)
        int WORLD_CACHE_SIZE = 200;
        //单次拉取/前端展示条数(世界/系统/联盟通用)
        int CHAT_PULL_SIZE = 50;

        //--------- 系统消息 ---------
        //服务器常驻缓存条数
        int SYSTEM_CACHE_SIZE = 300;

        //--------- 联盟聊天 ---------
        //单条消息最大字数
        int ALLIANCE_MSG_MAX_LEN = 300;
        //同一玩家最小发送间隔(秒)
        int ALLIANCE_SEND_INTERVAL_SEC = 5;
        //服务器常驻缓存条数
        int ALLIANCE_CACHE_SIZE = 200;

        //--------- 私聊 ---------
        //单条消息最大字数
        int PRIVATE_MSG_MAX_LEN = 300;
        //消息保留天数(TTL 自动过期)
        int PRIVATE_KEEP_DAYS = 7;
        //私聊消息批量落库间隔(秒)
        int PRIVATE_FLUSH_INTERVAL_SEC = 10;
        //写缓冲容量上限: 超限(通常是 Mongo 持续不可用)降级为同步单条落库, 防止内存无界增长
        int PRIVATE_WRITE_BUFFER_MAX = 50_000;

        //--------- 好友 ---------
        //好友数量上限
        int FRIEND_LIMIT = 50;
        //每日发送申请上限
        int DAILY_REQUEST_LIMIT = 50;
        //单个玩家待处理申请上限: 防止热门玩家的 pendingRequests 无界膨胀
        int PENDING_REQUEST_LIMIT = 100;
        //申请有效期(ms, 过期自动失效)
        int REQUEST_VALID_MILLS = 7 * TimeHelper.ONE_DAY_OF_MILLIS;
        //黑名单数量上限
        int BLACKLIST_LIMIT = 100;
    }

    /**
     * Redis key (全服共享的频道常驻缓存)。
     */
    interface RedisKey {
        //世界频道有界列表 (LPUSH 最新, 末尾最旧)
        String WORLD_CHANNEL = "social:chat:world";
        //系统消息有界列表
        String SYSTEM_CHANNEL = "social:chat:system";
        //联盟频道前缀 (拼接 allianceId): social:chat:alliance:{id}
        String ALLIANCE_CHANNEL_PREFIX = "social:chat:alliance:";
    }

    interface FriendOnlineStatus{
        int ONLINE = 0;
        int OFFLINE = 1;
        int IN_GAME = 2;
    }

    interface LangIds{
        //拒绝好友
        int REJECT_ADD_FRIEND_APPLY = 4058048;
    }
}
