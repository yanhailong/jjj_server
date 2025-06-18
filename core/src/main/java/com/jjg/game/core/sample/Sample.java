package com.jjg.game.core.sample;

import com.jjg.game.common.proto.ProtoDesc;
import io.protostuff.Tag;

import java.io.Serializable;

/**
 * 样本类
 * @author 11
 * @date 2025/6/6 14:34
 */
public abstract class Sample implements Cloneable , Serializable {

    @Tag(1)
    @ProtoDesc("配置id")
    public int sid;

    @Tag(2)
    @ProtoDesc("配置名称")
    public String name;

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * 从配置文件读取属性
     * @param attributeName 属性名称
     * @param attributeValue 属性值
     * @return 返回是否成功
     */
    public boolean setAttribute(String attributeName, String attributeValue) {
        return false;
    }

    @Override
    public Sample clone() {
        try {
            return (Sample) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(
                    getClass().getName() + " clone, sid=" + sid, e);
        }
    }
}
