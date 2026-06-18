package com.jjg.game.sim.data;

import com.jjg.game.sim.pb.struct.DestinationInfo;

import java.util.Map;

/**
 * 购买游客 (花钱购买的特殊游客):
 * - 由客户端"购买"请求触发立即生成, 不走定时生成逻辑
 * - 生成时预生成目的地序列和奖励 (奖励不添加到玩家身上); 分配唯一 uid
 * - 客户端凭 uid 再发起领奖请求, 服务端此时才把预生成的奖励添加到玩家身上
 * - 随 {@link SimCasinoData} 落库, 用于断线重连后重新下发
 *
 * @author 11
 * @date 2026/6/15
 */
public class PurchasedGuestData {
    //唯一id (生成时分配)
    private String uid;
    //游客 id (对应 VisitorQuestCfg.id)
    private int guestId;
    //生成时的星级快照 (用于领奖时结算奖励)
    private int star;
    //生成时的等级快照 (用于领奖时结算奖励)
    private int level;
    //预生成的目的地序列 (rewarded 标记有奖励交互点, 奖励延后领取时才结算)
    private Map<Integer,DestinationInfo> destinations;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getGuestId() {
        return guestId;
    }

    public void setGuestId(int guestId) {
        this.guestId = guestId;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<Integer, DestinationInfo> getDestinations() {
        return destinations;
    }

    public void setDestinations(Map<Integer, DestinationInfo> destinations) {
        this.destinations = destinations;
    }
}
