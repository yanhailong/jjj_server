package com.jjg.game.poker.game.douxian.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 斗仙牌数值常量，取值来源见 douxian/DESIGN.md 二、核心配置
 */
public interface DouXianConstant {

    interface Common {
        //牌库总数
        int DECK_SIZE = 52;
        //玩家人数
        int PLAYER_NUM = 4;
        //总回合数
        int TOTAL_ROUND = 4;
        //每回合手牌数
        int HAND_CARD_NUM = 8;
        //三区域总容量（凡2+灵3+仙5）
        int TOTAL_ZONE_CARD_NUM = 10;
        //每回合倍率，下标0对应第1回合
        int[] ROUND_MULTIPLIER = {1, 2, 2, 3};
        //触发灵气复苏的回合（1-based）
        int[] AETHER_REVIVAL_ROUND = {1, 3};
        //A常规点数
        int ACE_HIGH_RANK = 14;
        //A参与低位顺子（如A2、A23）时的点数
        int ACE_LOW_RANK = 1;
        //全胜所需至少完全大于的区域数
        int GRAND_WIN_STRICT_ZONE_MIN = 1;
        //一回合内全胜/全输达到此人数触发特殊规则
        int SPECIAL_RULE_TRIGGER_PLAYER_COUNT = 2;
    }

    interface Time {
        //匹配倒计时
        int MATCH_TIME = 30_000;
        //匹配无真人后机器人替补等待
        int MATCH_ROBOT_FILL_TIME = 10_000;
        //发牌动画时长(开局/补牌阶段结束后自动进入出牌阶段)
        int DEAL_TIME = 2_000;
        //出牌倒计时
        int PLAY_CARD_TIME = 30_000;
        //弃牌倒计时
        int DISCARD_TIME = 20_000;
        //每个已开放境界的结算表现：比牌3秒、结果展示2秒
        int SETTLEMENT_ZONE_EFFECT_TIME = 5_000;
        //全胜飞行和爆炸表现预留
        int SETTLEMENT_GRAND_WIN_EFFECT_TIME = 2_000;
        //每名特殊规则触发玩家的表现预留
        int SETTLEMENT_SPECIAL_RULE_EFFECT_TIME = 1_500;
        //进入下一阶段前的消息传输和客户端协程调度缓冲
        int SETTLEMENT_EFFECT_BUFFER_TIME = 1_000;
        int SETTLEMENT_ANIMATION_ACK_TIMEOUT = 60_000;
        int SETTLEMENT_NO_ONLINE_CLIENT_WAIT_TIME = 1_000;
        //飞升动画停留
        int TIER_ADVANCE_EFFECT_TIME = 3_000;
        //即时充值复活倒计时
        int RECHARGE_TIME = 30_000;
    }

    interface MatchState {
        int IDLE = 0;
        int MATCHING = 1;
        int SUCCESS = 2;
        int TIMEOUT = 3;
    }

    static int getRoundMultiplier(int round) {
        return Common.ROUND_MULTIPLIER[round - 1];
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.DOU_XIAN_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //响应房间基础信息
        int REPS_DOU_XIAN_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0x1;
        //通知发牌
        int NOTIFY_DOU_XIAN_DEAL_CARDS = BASE_MSG_PREFIX | 0x2;
        //请求摆牌
        int REQ_DOU_XIAN_PLACE_CARD = BASE_MSG_PREFIX | 0x3;
        //通知摆牌结果
        int NOTIFY_DOU_XIAN_PLACE_CARD_RESULT = BASE_MSG_PREFIX | 0x4;
        //请求确认出牌
        int REQ_DOU_XIAN_CONFIRM_PLAY = BASE_MSG_PREFIX | 0x5;
        //通知确认出牌结果
        int NOTIFY_DOU_XIAN_CONFIRM_RESULT = BASE_MSG_PREFIX | 0x6;
        //通知飞升结果
        int NOTIFY_DOU_XIAN_TIER_ADVANCE = BASE_MSG_PREFIX | 0x7;
        //通知结算结果
        int NOTIFY_DOU_XIAN_SETTLEMENT = BASE_MSG_PREFIX | 0x8;
        //通知特殊规则触发(得证大道/隐忍渡劫)
        int NOTIFY_DOU_XIAN_SPECIAL_RULE_TRIGGER = BASE_MSG_PREFIX | 0x9;
        //请求弃牌
        int REQ_DOU_XIAN_DISCARD = BASE_MSG_PREFIX | 0xA;
        //通知弃牌结果
        int NOTIFY_DOU_XIAN_DISCARD_RESULT = BASE_MSG_PREFIX | 0xB;
        //请求取消托管
        int REQ_DOU_XIAN_CANCEL_HOSTING = BASE_MSG_PREFIX | 0xC;
        //通知托管状态变化
        int NOTIFY_DOU_XIAN_HOSTING_STATE = BASE_MSG_PREFIX | 0xD;
        //请求即时充值复活
        int REQ_DOU_XIAN_RECHARGE = BASE_MSG_PREFIX | 0xE;
        //通知即时充值复活状态
        int NOTIFY_DOU_XIAN_RECHARGE = BASE_MSG_PREFIX | 0xF;
        //请求认输
        int REQ_DOU_XIAN_CONCEDE = BASE_MSG_PREFIX | 0x10;
        //通知玩家认输
        int NOTIFY_DOU_XIAN_CONCEDE = BASE_MSG_PREFIX | 0x11;
        //通知大结算
        int NOTIFY_DOU_XIAN_GRAND_SETTLEMENT = BASE_MSG_PREFIX | 0x12;
        //请求准备
        int REQ_DOU_XIAN_GO_READY = BASE_MSG_PREFIX | 0x13;
        //通知玩家准备状态
        int NOTIFY_DOU_XIAN_PLAYER_READY = BASE_MSG_PREFIX | 0x14;
        //通知进入弃牌/换牌阶段
        int NOTIFY_DOU_XIAN_DISCARD_START = BASE_MSG_PREFIX | 0x15;
        int NOTIFY_DOU_XIAN_MATCH_STATE = BASE_MSG_PREFIX | 0x16;
        // Request a recommendation for one clicked zone.
        int REQ_DOU_XIAN_RECOMMEND_CARDS = BASE_MSG_PREFIX | 0x17;
        // Return the recommendation for the clicked zone.
        int NOTIFY_DOU_XIAN_RECOMMEND_CARDS = BASE_MSG_PREFIX | 0x18;
        int REQ_DOU_XIAN_SETTLEMENT_ANIMATION_COMPLETE = BASE_MSG_PREFIX | 0x19;
    }
}
