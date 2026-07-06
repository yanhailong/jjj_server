package com.jjg.game.slots.game.lianHuanDuoBao.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.Map;

/**
 * 连环夺宝常量定义
 *
 * @author lm
 * @date 2026/6/2
 */
public interface LianHuanDuoBaoConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.LIAN_HUAN_DUO_BAO_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;
        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;
        //奖池
        int REQ_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
        //bonus 小游戏开始 / 结束
        int REQ_BONUS_START = BASE_MSG_PREFIX | 0x9;
        int RES_BONUS_RESULT = BASE_MSG_PREFIX | 0xA;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        //bonus 小游戏
        int BONUS = 2;
    }

    interface SpecialMode {
        //关卡 -> modelId（不同关卡用不同 specialMode 配置不同矩阵）
        //第一关 4x4 -> modelId=1
        //第二关 5x5 -> modelId=2
        //第三关 6x6 -> modelId=3
        Map<Integer, Integer> NORMAL_MAP = Map.of(1, 1, 2, 2, 3, 3);
    }

    interface SpecialPlay {
        //过关图标配置：value 格式 layer_iconId_needCount|...
        int PASSING_CRITERIA_ID = 5052001;
        //宝箱掉龙珠概率配置：value 格式 万分比（例 2000 = 20%）
        int CHEST_DRAGON_BALL_PROP_ID = 5052002;
        //连续消除送龙珠配置（如 5 连消除 +1 龙珠）
        int CASCADE_DRAGON_BALL_ID = 5052003;
        //聚宝盆抽水比例配置（万分比）
        int POOL_TAX_RATE_ID = 5052004;
        //宝箱奖励倍数权重配置：value 格式 倍数_权重|倍数_权重|...
        //例 1_3000|2_2500|3_2000|5_1500|10_1000（最大 10x 押注，文档 [11]）
        int CHEST_REWARD_PROP_ID = 5052005;
    }

    interface Common {
        //最大关卡数（4x4 → 5x5 → 6x6）
        int MAX_LAYER = 3;
        //bonus 小游戏龙珠保底数量
        int BONUS_MIN_DRAGON_BALL = 5;
        //bonus 倒计时秒数（客户端用）
        int BONUS_COUNTDOWN_SEC = 15;
        //每关需要的钥匙数
        int KEYS_PER_LAYER = 15;
    }

    /**
     * 元素类型标签，用来区分钥匙/龙珠/宝箱等特殊图标
     */
    interface ElementTag {
        //钥匙图标基础区段（实际 id 走 BaseElement 表，这里只是约定）
        int KEY = 1;
        //宝箱
        int CHEST = 2;
        //龙珠
        int DRAGON_BALL = 3;
    }
}
