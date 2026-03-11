package com.jjg.game.table.russianlette.message;

import com.jjg.game.common.constant.MessageConst;

/**
 * 俄罗斯转盘消息常量
 *
 * @author 2CL
 */
public interface RussianLetteMessageConstant {

    int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    //数字
    class Number {
        //数字
        public int number;
        //是否黑
        public boolean isBlack;
        //是否红
        public boolean isRed;
        //是否奇数
        public boolean isOdd;
        //是否偶数
        public boolean isEvent;

        public Number(boolean isEvent, boolean isOdd, boolean isRed, boolean isBlack, int number) {
            this.isEvent = isEvent;
            this.isOdd = isOdd;
            this.isRed = isRed;
            this.isBlack = isBlack;
            this.number = number;
        }
    }

    interface Numbers {
        //数据库是1-37 37-》0
        Number number_0 = new Number(false, false, false, false, 0);
        Number number_1 = new Number(false, true, true, false, 1);
        Number number_2 = new Number(true, false, false, true, 2);
        Number number_3 = new Number(false, true, true, false, 3);
        Number number_4 = new Number(true, false, false, true, 4);
        Number number_5 = new Number(false, true, true, false, 5);
        Number number_6 = new Number(true, false, false, true, 6);
        Number number_7 = new Number(false, true, true, false, 7);
        Number number_8 = new Number(true, false, false, true, 8);
        Number number_9 = new Number(false, true, true, false, 9);
        Number number_10 = new Number(true, false, false, true, 10);
        Number number_11 = new Number(false, true, false, true, 11);
        Number number_12 = new Number(true, false, true, false, 12);
        Number number_13 = new Number(false, true, false, true, 13);
        Number number_14 = new Number(true, false, true, false, 14);
        Number number_15 = new Number(false, true, false, true, 15);
        Number number_16 = new Number(true, false, true, false, 16);
        Number number_17 = new Number(false, true, false, true, 17);
        Number number_18 = new Number(true, false, true, false, 18);
        Number number_19 = new Number(false, true, true, false, 19);
        Number number_20 = new Number(true, false, false, true, 20);
        Number number_21 = new Number(false, true, true, false, 21);
        Number number_22 = new Number(true, false, false, true, 22);
        Number number_23 = new Number(false, true, true, false, 23);
        Number number_24 = new Number(true, false, false, true, 24);
        Number number_25 = new Number(false, true, true, false, 25);
        Number number_26 = new Number(true, false, false, true, 26);
        Number number_27 = new Number(false, true, true, false, 27);
        Number number_28 = new Number(true, false, false, true, 28);
        Number number_29 = new Number(false, true, false, true, 29);
        Number number_30 = new Number(true, false, true, false, 30);
        Number number_31 = new Number(false, true, false, true, 31);
        Number number_32 = new Number(true, false, true, false, 32);
        Number number_33 = new Number(false, true, false, true, 33);
        Number number_34 = new Number(true, false, true, false, 34);
        Number number_35 = new Number(false, true, false, true, 35);
        Number number_36 = new Number(true, false, true, false, 36);
        Number number_37 = new Number(false, false, false, false, 0);
        // 数组映射，索引即为数字值
        Number[] VALUES = {
                number_0, number_1, number_2, number_3, number_4, number_5, number_6, number_7, number_8, number_9,
                number_10, number_11, number_12, number_13, number_14, number_15, number_16, number_17, number_18, number_19,
                number_20, number_21, number_22, number_23, number_24, number_25, number_26, number_27, number_28, number_29,
                number_30, number_31, number_32, number_33, number_34, number_35, number_36, number_37,
        };

        // 根据数字获取对应的 Number 对象（0–36），超出范围返回 null
        static Number getNumber(int num) {
            if (num >= 0 && num <= 37) {
                return VALUES[num];
            }
            return null;
        }
    }

    interface ReqMsgBean {
        // 请求俄罗斯转盘房间摘要信息列表
        int REQ_RUSSIAN_LETTE_SUMMARY_LIST = BASE_MSG_PREFIX | 0x01;
        // 请求更新俄罗斯转盘房间单个摘要信息
        int REQ_RUSSIAN_LETTE_SUMMARY = BASE_MSG_PREFIX | 0x02;
        // 请求俄罗斯转盘房间信息
        int REQ_RUSSIAN_LETTE_INFO = BASE_MSG_PREFIX | 0x04;
        // 请求进入房间
        int REQ_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x05;
        // 请求退出房间
        int REQ_EXIT_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x06;
        // 请求房间返回
        int REQ_SWITCH_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x07;
        // 请求查看其他房间
        int REQ_RUSSIAN_LETTE_OTHER_SUMMARY_LIST = BASE_MSG_PREFIX | 0x08;
//        // 请求进入房间
//        int REQ_RUSSIAN_LETTE_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x09;
    }

    interface RespMsgBean {
        // 通知切换状态信息
        int NOTIFY_RUSSIAN_LETTE_PHASE_CHANGE_INFO = BASE_MSG_PREFIX | 0x80;
        // 进入房间的信息
        int NOTIFY_RUSSIAN_LETTE_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_RUSSIAN_LETTE_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
        // 返回俄罗斯转盘房间摘要列表
        int RESP_RUSSIAN_LETTE_SUMMARY_LIST = BASE_MSG_PREFIX | 0x83;
        // 返回俄罗斯转盘房间摘要
        int RESP_RUSSIAN_LETTE_SUMMARY = BASE_MSG_PREFIX | 0x84;
        // 进入房间的信息
        int RESP_RUSSIAN_LETTE_INFO = BASE_MSG_PREFIX | 0x85;
        // 进入房间返回
        int RESP_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x86;
        // 退出房间返回
        int RESP_EXIT_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x87;
        // 返回切换房间返回
        int RESP_SWITCH_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x88;
        // 返回查看其他房间
        int RESP_RUSSIAN_LETTE_OTHER_SUMMARY_LIST = BASE_MSG_PREFIX | 0x89;
        // 通知俄罗斯状态切换（牌桌界面）
        int NOTIFY_RUSSIAN_LETTE_SUMMARY = BASE_MSG_PREFIX | 0x90;
    }
}
