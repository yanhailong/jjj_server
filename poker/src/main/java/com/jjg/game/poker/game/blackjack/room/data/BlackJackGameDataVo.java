package com.jjg.game.poker.game.blackjack.room.data;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/28 14:04
 */
public class BlackJackGameDataVo extends BasePokerGameDataVo {
    /**
     * 庄家信息
     */
    private PlayerSeatInfo master;

    /**
     * 分牌
     */
    private List<Integer> cutCardList;


    public List<Integer> getCutCardList() {
        return cutCardList;
    }

    public void setCutCardList(List<Integer> cutCardList) {
        this.cutCardList = cutCardList;
    }

    public BlackJackGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    public PlayerSeatInfo getMaster() {
        return master;
    }

    public void setMaster(PlayerSeatInfo master) {
        this.master = master;
    }
}
