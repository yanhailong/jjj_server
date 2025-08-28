package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * 玩家头像信息
 * @author 11
 * @date 2025/8/7 16:16
 */
@Document
public class PlayerAvatar {
    @Id
    private long playerId;
    //已解锁的头像列表
    private Set<Integer> unlockAvatarSet;
    //已解锁的头像框列表
    private Set<Integer> unlockFrameSet;
    //已解锁的称号列表
    private Set<Integer> unlockTitleSet;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Set<Integer> getUnlockAvatarSet() {
        return unlockAvatarSet;
    }

    public void setUnlockAvatarSet(Set<Integer> unlockAvatarSet) {
        this.unlockAvatarSet = unlockAvatarSet;
    }

    public Set<Integer> getUnlockFrameSet() {
        return unlockFrameSet;
    }

    public void setUnlockFrameSet(Set<Integer> unlockFrameSet) {
        this.unlockFrameSet = unlockFrameSet;
    }

    public Set<Integer> getUnlockTitleSet() {
        return unlockTitleSet;
    }

    public void setUnlockTitleSet(Set<Integer> unlockTitleSet) {
        this.unlockTitleSet = unlockTitleSet;
    }

    public void addAvatar(int id){
        if(id < 1){
            return;
        }
        if(this.unlockAvatarSet == null){
            this.unlockAvatarSet = new HashSet<>();
        }
        this.unlockAvatarSet.add(id);
    }
    public void addFrame(int id){
        if(id < 1){
            return;
        }
        if(this.unlockFrameSet == null){
            this.unlockFrameSet = new HashSet<>();
        }
        this.unlockFrameSet.add(id);
    }
    public void addTitle(int id){
        if(id < 1){
            return;
        }
        if(this.unlockTitleSet == null){
            this.unlockTitleSet = new HashSet<>();
        }
        this.unlockTitleSet.add(id);
    }

}
