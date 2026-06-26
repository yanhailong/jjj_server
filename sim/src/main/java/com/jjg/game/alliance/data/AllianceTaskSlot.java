package com.jjg.game.alliance.data;

/**
 * 联盟任务池条目 (内嵌于 {@link AllianceData#getTasks()})。
 * <p>
 * 接取即从池中原子移除(独占), 放弃不退回(需求: 放弃后的任务直接删除); 整点由访问方惰性补齐。
 *
 * @author 11
 * @date 2026/6/11
 */
public class AllianceTaskSlot {
    //任务配置 id (AllianceConfigService 任务表)
    private int cfgId;
    //生成时间(ms)
    private long createTime;
    //过期时间(ms): 池中超期未被接取则补齐时清除
    private long expireTime;

    public AllianceTaskSlot() {
    }

    public AllianceTaskSlot(int cfgId, long createTime, long expireTime) {
        this.cfgId = cfgId;
        this.createTime = createTime;
        this.expireTime = expireTime;
    }

    public int getCfgId() {
        return cfgId;
    }

    public void setCfgId(int cfgId) {
        this.cfgId = cfgId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
}
