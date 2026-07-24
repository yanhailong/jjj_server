package com.jjg.game.sim.data;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Transient;

/**
 * 可落库数据基类。
 *
 * @author 11
 * @date 2026/5/28
 */
public abstract class AbstractData {
    //上次落库时的内容哈希; 0 表示尚未落库 (新建数据首次必落)
    //由 IO 线程写、玩家线程读, 故用 volatile 保证可见性
    @Transient
    @JSONField(serialize = false, deserialize = false)
    private transient volatile long savedHash;

    //Jackson 会跳过 transient 字段但仍认这个 public getter, 导致 RPC 序列化带出 savedHash 而对端无法反序列化
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public long getSavedHash() {
        return savedHash;
    }

    /**
     * 落库成功后调用, 记录本次落库内容的哈希
     */
    public void markSaved(long hash) {
        this.savedHash = hash;
    }

    /**
     * 构建文档主键 (联合主键子类覆写); 落库快照前调用, 保证 _id 已就绪。
     * 默认空实现 (主键即 @Id 字段, 无需额外构建)。
     */
    public void buildKey() {
    }
}
