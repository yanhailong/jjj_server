package com.jjg.game.slots.data;

import com.jjg.game.common.utils.RandomUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/16 13:40
 */
public class PropInfo {
    private int sum;
    private Map<Integer, int[]> propMap = new HashMap<>();

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

    public void addProp(Integer key, int begin, int end) {
        this.propMap.put(key, new int[]{begin, end});
    }

    public PropInfo copy(){
        PropInfo propInfo = new PropInfo();
        propInfo.sum = this.sum;
        propInfo.propMap = this.propMap;
        return propInfo;
    }

    /**
     * 随机获取一个key
     * @return
     */
    public Integer getRandKey() {
        int rand = RandomUtils.randomMinMax(0,this.sum);
        for(Map.Entry<Integer,int[]> en: this.propMap.entrySet()){
            if(rand >= en.getValue()[0] && rand < en.getValue()[1]){
                return en.getKey();
            }
        }
        return null;
    }

    // 删除指定 key 并重新计算 sum 和范围
    public void removeKeyAndRecalculate(int removeKey) {
        //从 map 中移除指定 key
        int[] removedRange = this.propMap.remove(removeKey);
        if (removedRange == null) {
            return; // key 不存在，无需处理
        }

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
}
