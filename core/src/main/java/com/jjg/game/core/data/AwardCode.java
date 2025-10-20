package com.jjg.game.core.data;

import com.jjg.game.core.constant.AwardCodeType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 领奖码
 */
@Document(collection = "AwardCode")
public class AwardCode {

    /**
     * 雪花id
     */
    @Id
    private long snowflakeId;

    /**
     * 雪花id加密后的字符串
     */
    private String code;

    /**
     * 领奖码绑定的玩家id
     */
    private long playerId;

    /**
     * 领奖码类型
     */
    private AwardCodeType type;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 使用时间
     */
    private long useTime;

    public long getSnowflakeId() {
        return snowflakeId;
    }

    public void setSnowflakeId(long snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public AwardCodeType getType() {
        return type;
    }

    public void setType(AwardCodeType type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "AwardCode{" +
                "snowflakeId=" + snowflakeId +
                ", code='" + code + '\'' +
                ", playerId=" + playerId +
                ", type=" + type +
                ", createTime=" + createTime +
                ", useTime=" + useTime +
                '}';
    }
}
