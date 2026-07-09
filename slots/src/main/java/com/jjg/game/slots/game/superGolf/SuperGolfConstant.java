package com.jjg.game.slots.game.superGolf;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 超级高尔夫游戏常量。
 * <p>
 * 跟 Dracula 主要区别：
 * <ul>
 *   <li>没有"消除轮数倍率"，乘倍来自盘面上的神秘符号（id={@link BaseElement#MYSTERY}）</li>
 *   <li>免费模式下乘倍值跨 spin 累计直到免费结束</li>
 *   <li>免费触发：4/5/6 scatter → 10/12/14 局（不是德古拉的 12/14/16）</li>
 *   <li>百搭可代替"除夺宝和神秘外"的符号（比德古拉多一个排除）</li>
 *   <li>奖池：3 个夺宝 + 1 个奖池符号同时出现触发对应奖池</li>
 * </ul>
 */
public interface SuperGolfConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SUPER_GOLF_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    /**
     * 文档表 1 的符号 ID 映射。
     */
    interface BaseElement {
        //普通图标 1-11
        int NORMAL_MIN = 1;
        int NORMAL_MAX = 11;
        //百搭符号（只在 2-6 轴出现，且不能代替夺宝/神秘）
        int WILD = 12;
        //夺宝符号（scatter）
        int SCATTER = 13;
        //神秘符号（不直接出现在 roller，由"带框符号中奖"动态转换而来）
        int MYSTERY = 14;
        int MYSTERY_TWO = 74;   // 2格神秘符号
        int MYSTERY_THREE = 75; // 3格神秘符号
        //奖池图标
        int JACKPOT_MINI = 15;
        int JACKPOT_MINOR = 16;
        int JACKPOT_MAJOR = 17;
        int JACKPOT_GRAND = 18;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FREE = SlotsConst.Status.FREE;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
    }

    interface SpecialPlay {
        //增加免费次数（scatter 触发免费）—— SpecialPlay 表 playType=2
        //  格式 libType,scatter_iconId,count_addFreeCount_prop|...
        //  超级高尔夫：4_10_10000|5_12_10000|6_14_10000
        int TYPE_ADD_FREE_COUNT = 2;
    }

    /**
     * 文档"大符号"：占据 2/3 个符号位置，仅 101-111 可成为大符号，只出现在 2/3/4/5 轴。
     */
    interface SpecialGird {
        //2 格大符号
        int GRID_TWO = 10480005;
        //3 格大符号
        int GRID_THERE = 10480006;
    }

    interface Common {
        //神秘符号触发倍率所需的最少个数：3+ 同时存在时转换并触发倍率（按策划口述确认，对齐 doc 行 72 "初始为X3"）
        int MYSTERY_TRIGGER_MIN = 3;
    }
}
