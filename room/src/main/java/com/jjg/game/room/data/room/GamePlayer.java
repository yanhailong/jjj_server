package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;

import java.util.Objects;

/**
 * 玩家游戏中的数据集
 *
 * @author 2CL
 */
public class GamePlayer extends Player {
    // 玩家是否处于托管状态
    protected transient boolean hosting;
    // table类的玩家数据
    protected transient TablePlayerGameData tableGameData;
    // poker类的玩家数据
    protected transient PokerPlayerGameData pokerPlayerGameData;
    //进入游戏的时间
    protected int enterGameTime;


    public void setPokerPlayerGameData(PokerPlayerGameData pokerPlayerGameData) {
        this.pokerPlayerGameData = pokerPlayerGameData;
    }

    public boolean isHosting() {
        return hosting;
    }

    public void setHosting(boolean hosting) {
        this.hosting = hosting;
    }

    public TablePlayerGameData getTableGameData() {
        return tableGameData;
    }

    public PokerPlayerGameData getPokerPlayerGameData() {
        return pokerPlayerGameData;
    }

    public void setTableGameData(TablePlayerGameData tableGameData) {
        this.tableGameData = tableGameData;
    }

    public int getEnterGameTime() {
        return enterGameTime;
    }

    public void setEnterGameTime(int enterGameTime) {
        this.enterGameTime = enterGameTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GamePlayer that = (GamePlayer) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    public void fromPlayer(Player p) {
        if (p == null) {
            return;
        }
        this.setId(p.getId());
        this.setNickName(p.getNickName());
        this.setGender(p.getGender());
        this.setHeadImgId(p.getHeadImgId());
        this.setHeadFrameId(p.getHeadFrameId());
        this.setNationalId(p.getNationalId());
        this.setTitleId(p.getTitleId());
        this.setChipsId(p.getChipsId());
        this.setBackgroundId(p.getBackgroundId());
        this.setCardBackgroundId(p.getCardBackgroundId());
        this.setRoomId(p.getRoomId());
        this.setGameType(p.getGameType());
        this.setRoomCfgId(p.getRoomCfgId());
        this.setGold(p.getGold());
        this.setDiamond(p.getDiamond());
        this.setSafeBoxGold(p.getSafeBoxGold());
        this.setSafeBoxDiamond(p.getSafeBoxDiamond());
        this.setLevel(p.getLevel());
        this.setExp(p.getExp());
        this.setVipLevel(p.getVipLevel());
        this.setVipExp(p.getVipExp());
        this.setStatement(p.getStatement());
        this.setIp(p.getIp());
        this.setDeviceType(p.getDeviceType());
        this.setCreateTime(p.getCreateTime());
        this.setUpdateTime(p.getUpdateTime());
        this.setFriendRoomInvitationCode(p.getFriendRoomInvitationCode());
        this.setChannel(p.getChannel());
        this.setLoginType(p.getLoginType());
    }
}
