package com.jjg.game.sim.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/5/15
 */
public interface SimConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SIM_GAME << MessageConst.MessageCommon.RIGHT_MOVE;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //退出游戏
        int REQ_EXIT_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_EXIT_GAME = BASE_MSG_PREFIX | 0x4;

        //完成新手引导
        int REQ_FINISH_GUIDE = BASE_MSG_PREFIX | 0x5;
//        int RES_FINISH_GUIDE = BASE_MSG_PREFIX | 0x6;

        //通知客户端生成游客
        int NOTIFY_GENERATE_GUEST = BASE_MSG_PREFIX | 0x7;

        //请求解锁建筑
        int REQ_UNLOCK_BUILDING = BASE_MSG_PREFIX | 0x8;
        int RES_UNLOCK_BUILDING = BASE_MSG_PREFIX | 0x9;

        //获取技能
        int REQ_SIM_GET_SKILLS = BASE_MSG_PREFIX | 0xA;
        int RES_SIM_GET_SKILLS = BASE_MSG_PREFIX | 0xB;

        //升级技能
        int REQ_SIM_UPGRADE_SKILL = BASE_MSG_PREFIX | 0xC;
        int RES_SIM_UPGRADE_SKILL = BASE_MSG_PREFIX | 0xD;

        //请求升级建筑 (启动 CD)
        int REQ_UPGRADE_BUILDING = BASE_MSG_PREFIX | 0xE;
        int RES_UPGRADE_BUILDING = BASE_MSG_PREFIX | 0xF;

        //请求完成建筑升级 (CD 到时后客户端发起)
        int REQ_COMPLETE_BUILDING_UPGRADE = BASE_MSG_PREFIX | 0x10;
        int RES_COMPLETE_BUILDING_UPGRADE = BASE_MSG_PREFIX | 0x11;

        //请求清除建筑升级CD (钻石/道具)
        int REQ_CLEAR_BUILDING_CD = BASE_MSG_PREFIX | 0x12;
        int RES_CLEAR_BUILDING_CD = BASE_MSG_PREFIX | 0x13;

        //招募/解锁雇员
        int REQ_RECRUIT_EMPLOYEE = BASE_MSG_PREFIX | 0x14;
        int RES_RECRUIT_EMPLOYEE = BASE_MSG_PREFIX | 0x15;

        //升级雇员
        int REQ_UPGRADE_EMPLOYEE = BASE_MSG_PREFIX | 0x16;
        int RES_UPGRADE_EMPLOYEE = BASE_MSG_PREFIX | 0x17;

        //升星雇员
        int REQ_STAR_UP_EMPLOYEE = BASE_MSG_PREFIX | 0x18;
        int RES_STAR_UP_EMPLOYEE = BASE_MSG_PREFIX | 0x19;

        //任命主管 (按建筑类型)
        int REQ_ASSIGN_SUPERVISOR = BASE_MSG_PREFIX | 0x1A;
        int RES_ASSIGN_SUPERVISOR = BASE_MSG_PREFIX | 0x1B;
    }

    interface PropConfig {
        //下注金额
        int TYPE_STAKE = 1;
    }

    interface Common {

        //默认赌场STATS id (CasinoStatsSheetCfg level=0)
        int DEFAULT_CASINO_STATS_ID = 1001;

        //随机模式选目的地时单点最大重试次数
        int MAX_DEST_PICK_RETRY = 10;

        //长时掉线阈值 (ms) — 5 分钟
        long DISCONNECT_LONG_THRESHOLD_MS = 5 * 60 * 1000L;

        //"赌场满员"判定窗口 (ms) — 10 分钟
        long CAPACITY_WINDOW_MS = 10 * 60 * 1000L;

        //服务能力加成系数基数 (VisitorStarCfg.Additioncoefficient, 100 表示 +0%)
        int SERVICE_CAPACITY_COEFFICIENT_BASE = 100;

        //雇员主管加成基数 (EmployeeStarCfg.SupervisorBonus, 100 表示 +0%)
        int EMPLOYEE_SUPERVISOR_BONUS_BASE = 100;
    }

    /**
     * 建筑分类 (BuildingAreaTable.type)
     */
    interface BuildingType {
        int GAME = 1;       //游戏 (SLOT/扑克/捕鱼)
        int REST = 2;       //休息 (普通/VIP休息区)
        int MANAGEMENT = 3; //管理 (前台/运营/营销/研发)
    }

    /**
     * 装备分类 (EquipmentTable.type)
     */
    interface EquipmentType {
        int DEVICE = 1;     //可交互设备
        //其它值: 装饰
    }
}
