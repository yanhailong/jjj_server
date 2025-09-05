package com.jjg.game.slots.data;

import com.jjg.game.common.utils.RandomUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/16 13:40
 */
public class PropInfo implements Cloneable{
    private int sum;
    //元素在随机是的起始点和结尾点
    private Map<Integer, int[]> propMap = new HashMap<>();
    //元素的最大出现次数
    private Map<Integer, Integer> maxShowLimitMap;

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public Map<Integer, int[]> getPropMap() {
        return propMap;
    }

    public void setPropMap(Map<Integer, int[]> propMap) {
        this.propMap = propMap;
    }

    public void addSum(int value) {
        this.sum += value;
    }

    public void addProp(Integer key, int begin, int end,int maxLimit) {
        addProp(key,begin,end);
        if(this.maxShowLimitMap == null) {
            this.maxShowLimitMap = new HashMap<>();
        }
        this.maxShowLimitMap.put(key, maxLimit);
    }

    public void addProp(Integer key, int begin, int end) {
        this.propMap.put(key, new int[]{begin, end});
        this.sum = end;
    }

    /**
     * 随机获取一个key
     * @return
     */
    public Integer getRandKey() {
        if(this.sum < 1){
            return null;
        }
        int rand = RandomUtils.randomInt(this.sum);
//        System.out.println("rand : " + rand + ", sum = " + this.sum + ", propMap = " + JSON.toJSONString(this.propMap));
        for(Map.Entry<Integer,int[]> en: this.propMap.entrySet()){
            if(rand >= en.getValue()[0] && rand < en.getValue()[1]){
                return en.getKey();
            }
        }
        return null;
    }

    public int getMaxShowLimit(int key) {
        if(this.maxShowLimitMap == null) {
            return Integer.MAX_VALUE;
        }
        return this.maxShowLimitMap.get(key);
    }

    // 删除指定 key 并重新计算 sum 和范围
    public void removeKeyAndRecalculate(int removeKey) {
        //从 map 中移除指定 key
        int[] removedRange = this.propMap.remove(removeKey);
        if (removedRange == null) {
            return; // key 不存在，无需处理
        }
        this.sum = 0;

        //重新计算每个 key 的范围
        int begin = 0;
        for (Map.Entry<Integer, int[]> entry : this.propMap.entrySet()) {
            int[] range = entry.getValue();
            int weight = range[1] - range[0];

            range[0] = begin;
            range[1] = begin + weight;
            begin = range[1];
            this.sum += weight;
        }
    }

    @Override
    public PropInfo clone() {
        try {
            PropInfo cloned = (PropInfo) super.clone();
            cloned.propMap = new HashMap<>();
            for (Map.Entry<Integer, int[]> entry : this.propMap.entrySet()) {
                cloned.propMap.put(entry.getKey(), entry.getValue().clone());
            }

            if (this.maxShowLimitMap != null) {
                cloned.maxShowLimitMap = new HashMap<>(this.maxShowLimitMap);
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
