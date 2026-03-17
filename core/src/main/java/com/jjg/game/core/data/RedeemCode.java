package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2026/3/12 15:24
 */
@Document
@CompoundIndex(name = "idx_redeem_id_use_player_id", def = "{'redeemId': 1, 'usePlayerId': 1}")
public class RedeemCode {
    @Id
    private String code;
    //礼包id
    private long redeemId;
    //创建时间
    private long createTime;
    //使用时间
    private long useTime;
    //使用玩家id
    private long usePlayerId;

    public long getRedeemId() {
        return redeemId;
    }

    public void setRedeemId(long redeemId) {
        this.redeemId = redeemId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUseTime() {
        return useTime;
    }

    public void setUseTime(long useTime) {
        this.useTime = useTime;
    }

    public long getUsePlayerId() {
        return usePlayerId;
    }

    public void setUsePlayerId(long usePlayerId) {
        this.usePlayerId = usePlayerId;
    }
}
