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
public class PlayerSkin {
    @Id
    private long playerId;
    //已解锁的头像列表
    private Set<Integer> unlockAvatarSet;
    //已解锁的头像框列表
    private Set<Integer> unlockFrameSet;
    //已解锁的称号列表
    private Set<Integer> unlockTitleSet;
    //已解锁的筹码列表
    private Set<Integer> unlockChipsSet;
    //已解锁的背景图列表
    private Set<Integer> unlockBackgroundSet;
    //已解锁的牌背列表
    private Set<Integer> unlockCardBackgroundSet;

    public Set<Integer> getUnlockChipsSet() {
        return unlockChipsSet;
    }

    public void setUnlockChipsSet(Set<Integer> unlockChipsSet) {
        this.unlockChipsSet = unlockChipsSet;
    }

    public Set<Integer> getUnlockBackgroundSet() {
        return unlockBackgroundSet;
    }

    public void setUnlockBackgroundSet(Set<Integer> unlockBackgroundSet) {
        this.unlockBackgroundSet = unlockBackgroundSet;
    }

    public Set<Integer> getUnlockCardBackgroundSet() {
        return unlockCardBackgroundSet;
    }

    public void setUnlockCardBackgroundSet(Set<Integer> unlockCardBackgroundSet) {
        this.unlockCardBackgroundSet = unlockCardBackgroundSet;
    }

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
    public void addChip(int id){
        if(id < 1){
            return;
        }
        if(this.unlockChipsSet == null){
            this.unlockChipsSet = new HashSet<>();
        }
        this.unlockChipsSet.add(id);
    }
    public void addBackground(int id){
        if(id < 1){
            return;
        }
        if(this.unlockBackgroundSet == null){
            this.unlockBackgroundSet = new HashSet<>();
        }
        this.unlockBackgroundSet.add(id);
    }
    public void addCardBackground(int id){
        if(id < 1){
            return;
        }
        if(this.unlockCardBackgroundSet == null){
            this.unlockCardBackgroundSet = new HashSet<>();
        }
        this.unlockCardBackgroundSet.add(id);
    }
}
