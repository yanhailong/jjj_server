package com.jjg.game.poker.manager;

import com.jjg.game.common.cluster.ClusterClient;

/** Poker玩家在赛季入口下的运行时账户快照。 */
public class PokerSeasonAccount {

    private final long playerId;
    private String ip;
    private ClusterClient simClient;
    private long balance;

    public PokerSeasonAccount(long playerId, String ip) {
        this.playerId = playerId;
        this.ip = ip;
    }

    public long getPlayerId() {
        return playerId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ClusterClient getSimClient() {
        return simClient;
    }

    public void setSimClient(ClusterClient simClient) {
        this.simClient = simClient;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }
}
