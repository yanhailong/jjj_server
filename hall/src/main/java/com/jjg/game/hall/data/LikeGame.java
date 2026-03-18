package com.jjg.game.hall.data;

import java.util.TreeSet;

/**
 * 玩家收藏/点赞游戏列表
 * @author 11
 * @date 2025/8/21 10:04
 */
public class LikeGame {
    private long playerId;
    private TreeSet<Integer> gameTypeSet;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public TreeSet<Integer> getGameTypeSet() {
        return gameTypeSet;
    }

    public void setGameTypeSet(TreeSet<Integer> gameTypeSet) {
        this.gameTypeSet = gameTypeSet;
    }

    public boolean addLikeGame(int gameType) {
        if(this.gameTypeSet == null) {
            this.gameTypeSet = new TreeSet<>();
        }
        return this.gameTypeSet.add(gameType);
    }

    public boolean containsGameType(int gameType) {
        if(this.gameTypeSet == null) {
            return false;
        }
        return this.gameTypeSet.contains(gameType);
    }
}
