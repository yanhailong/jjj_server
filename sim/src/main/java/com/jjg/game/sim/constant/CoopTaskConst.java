package com.jjg.game.sim.constant;

/**
 * 多人协作任务常量 (sim 任务层 + slots 房间层共用)。
 *
 * @author 11
 * @date 2026/7/6
 */
public interface CoopTaskConst {

    //房主单次频道邀请的最大目标数 (防客户端构造超大列表放大跨节点 RPC/聊天)
    int MAX_INVITE_TARGETS = 30;

    /**
     * 玩家已领取任务的状态机
     */
    interface TaskStatus {
        //已领取, 待创建房间
        int CLAIMED = 1;
        //房间已创建 (等待/进行中, 权威态在 slots 房间)
        int IN_ROOM = 2;
        //任务完成, 发起者待领奖
        int REWARDABLE = 3;
        //任务失败 (奖励不可领取, 领取次数不退)
        int FAILED = 4;
    }

    /**
     * 协作房间状态 (路由记录与 slots 内存房间共用)
     */
    interface RoomStatus {
        //等待中 (可加入/退出/解散)
        int WAITING = 0;
        //游戏进行中 (不可加入/退出)
        int RUNNING = 1;
        //已结算 (成功或失败)
        int FINISHED = 2;
    }

    interface Redis {
        //协作房间路由记录 key 前缀 (value = CoopRoomRecord JSON)
        String ROOM_KEY_PREFIX = "coopRoom:";
        //房间记录兜底 TTL(秒): 覆盖最长任务时限, 节点崩溃后记录自动过期, 任务层自愈回退
        long ROOM_TTL_SECONDS = 24 * 3600;
    }
}
