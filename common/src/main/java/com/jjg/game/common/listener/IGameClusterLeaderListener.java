package com.jjg.game.common.listener;

/**
 * 游戏
 *
 * @author 2CL
 */
public interface IGameClusterLeaderListener {

    /**
     * 当LeaderLatch的状态从hasLeadership=false变为hasLeadership=true时，就会调用此函数。
     * <p>
     * 请注意，当此方法调用发生时，hasLeadership可能已经回落到false。
     * 如果发生这种情况，您可以预期{@link notLeader（）}也会被调用。
     */
    void isLeader();

    /**
     * 当LeaderLatch的状态从hasLeadership=true变为hasLeadership=false时，就会调用此函数。
     * <p>
     * 请注意，当此方法调用发生时，hasLeadership可能已变为true。
     * 如果发生这种情况，您可以预期{@link isLeader（）}也会被调用。
     */
    void notLeader();
}
