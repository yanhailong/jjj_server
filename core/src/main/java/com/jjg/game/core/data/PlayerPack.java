package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * 玩家背包数据
 * @author 11
 * @date 2025/8/6 15:41
 */
@Document
public class PlayerPack {
    //玩家id
    private long playerId;
    //道具  格子id ->道具
    private Map<Integer, Item> items;
    //道具  道具id -> 格子id
    private Map<Integer, List<Integer>> itemIndexMap;
    // 格子位置管理
//    private BitSet usedSlots;
//
//    public PlayerPack() {
//    }
//
//    public PlayerPack(int capacity) {
//        this.usedSlots = new BitSet(capacity);
//        //初始时所有格子都可用
//        this.usedSlots.clear(0,capacity);
//    }
//
//
    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, Item> getItems() {
        return items;
    }

    public void setItems(Map<Integer, Item> items) {
        this.items = items;
    }
//
//    public Map<Integer, List<Integer>> getItemIndexMap() {
//        return itemIndexMap;
//    }
//
//    public void setItemIndexMap(Map<Integer, List<Integer>> itemIndexMap) {
//        this.itemIndexMap = itemIndexMap;
//    }
//
//    public BitSet getUsedSlots() {
//        return usedSlots;
//    }
//
//    public void setUsedSlots(BitSet usedSlots) {
//        this.usedSlots = usedSlots;
//    }
//
//    /**
//     * 往背包中添加道具
//     * @param id 道具id
//     * @param num 道具数量
//     * @param maxNum 该道具最大堆叠数量
//     */
//    public void addItem(int id,int num,int maxNum) {
//        if(num < 1){
//            return;
//        }
//
//        // 1. 先尝试堆叠到已有道具
//        if (this.itemIndexMap != null && !this.itemIndexMap.isEmpty() && this.itemIndexMap.containsKey(id)) {
//            List<Integer> indexes = itemIndexMap.get(id);
//            for (int index : indexes) {
//                Item item = items.get(index);
//                if(item.getCount() >= maxNum){ // 该格子道具已满
//                    continue;
//                }
//
//                //未到最大堆叠数量，计算出还可以添加的数量
//                int canAdd = maxNum - item.getCount();
//                if (num <= canAdd) { // 全部可以添加
//                    item.addCount(num);
//                    return;
//                } else { // 部分添加
//                    item.addCount(canAdd);
//                    num -= canAdd;
//                }
//            }
//        }
//
//        // 2. 剩余数量需要放入新格子
//        while (num > 0) {
//            // 找一个空闲格子
//            int newIndex = findAvailableIndex();
//            this.usedSlots.set(newIndex);
//
//            // 计算新格子能放多少
//            int addNum = Math.min(num, maxNum);
//            Item newItem = new Item(id,addNum);
//
//            // 放入背包
//            addItem(newIndex,newItem);
//
//            // 更新索引
//            if(this.itemIndexMap == null){
//                this.itemIndexMap = new HashMap<>();
//            }
//            itemIndexMap.computeIfAbsent(id, k -> new ArrayList<>()).add(newIndex);
//            num -= addNum;
//        }
//    }
//
//    /**
//     * 删除道具
//     * @param id
//     * @param num
//     * @return
//     */
//    public boolean removeItem(int id,int num) {
//        if(num < 1){
//            return false;
//        }
//
//        if(this.itemIndexMap == null || this.itemIndexMap.isEmpty()){
//            return false;
//        }
//
//        List<Integer> indexList = this.itemIndexMap.get(id);
//        if(indexList == null || indexList.isEmpty()){
//            return false;
//        }
//        return false;
//
//    }
//
//    /**
//     * 查找可用的格子索引
//     * @return 可用索引，
//     */
//    private int findAvailableIndex() {
//        //还要考虑 需要扩容的情况
//        return this.usedSlots.nextClearBit(0);
//    }
//
//    private void addItem(int index,Item item) {
//        if(this.items == null){
//            this.items = new HashMap<>();
//        }
//        this.items.put(index, item);
//    }

}
