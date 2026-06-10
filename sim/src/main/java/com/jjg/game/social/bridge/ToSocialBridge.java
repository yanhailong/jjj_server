package com.jjg.game.social.bridge;

import com.jjg.game.common.rpc.IGameRpc;

/**
 * 其他节点 -> 社交系统 的跨节点 RPC 接口 (预留)。
 * <p>
 * 范式对齐 {@link com.jjg.game.sim.bridge.ToSimBridge}: 由游戏节点(slots/table/poker)通过
 * {@code @ClusterRpcReference} 调用, 把房间内的聊天/大奖/系统播报投递进社交系统。
 * <p>
 * 本期(大厅社交)仅声明接口, 房间相关方法待 slots 房间接入时实现。系统消息方法已可用。
 *
 * @author 11
 * @date 2026/6/9
 */
public interface ToSocialBridge extends IGameRpc {

    /**
     * 推送系统消息(全服公告)。
     *
     * @param content 公告内容
     */
    void pushSystemMessage(String content);

    /**
     * 房间大奖播报 (预留): 玩家中大奖后由游戏节点调用, 在房间聊天框内显示。
     *
     * @param roomId    房间id
     * @param playerId  中奖玩家
     * @param prizeName 大奖名称
     * @param amount    中奖金额
     */
    void pushBigWin(long roomId, long playerId, String prizeName, long amount);

    /**
     * 房间公共聊天 (预留): 后续由游戏节点接入房间频道时使用。
     *
     * @param roomId   房间id
     * @param playerId 发送者
     * @param content  内容
     */
    void pushRoomChat(long roomId, long playerId, String content);
}
