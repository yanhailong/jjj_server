package com.jjg.game.core.data;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * 玩家背包数据
 *
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
    // 已经被占用的格子id
    private Set<Integer> usedGird;

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

    public Map<Integer, List<Integer>> getItemIndexMap() {
        return itemIndexMap;
    }

    public void setItemIndexMap(Map<Integer, List<Integer>> itemIndexMap) {
        this.itemIndexMap = itemIndexMap;
    }

    public Set<Integer> getUsedGird() {
        return usedGird;
    }

    public void setUsedGird(Set<Integer> usedGird) {
        this.usedGird = usedGird;
    }

    /**
     * 往背包中添加道具
     *
     * @param id     道具id
     * @param num    道具数量
     * @param maxNum 该道具最大堆叠数量
     */
    public void addItem(int id, long num, int maxNum) {
        if (num < 1) {
            return;
        }

        // 1. 先尝试堆叠到已有道具
        if (this.itemIndexMap != null && !this.itemIndexMap.isEmpty() && this.itemIndexMap.containsKey(id)) {
            List<Integer> indexes = itemIndexMap.get(id);
            for (int index : indexes) {
                Item item = items.get(index);
                if (maxNum == GameConstant.Item.PROP_MAX) {
                    maxNum = Integer.MAX_VALUE;
                }
                if (item.getCount() >= maxNum) { // 该格子道具已满
                    continue;
                }

                //未到最大堆叠数量，计算出还可以添加的数量
                long canAdd = maxNum - item.getCount();
                if (num <= canAdd) { // 全部可以添加
                    item.addCount(num);
                    return;
                } else { // 部分添加
                    item.addCount(canAdd);
                    num -= canAdd;
                }
            }
        }

        // 2. 剩余数量需要放入新格子
        while (num > 0) {
            // 找一个空闲格子
            int newIndex = findAvailableIndex();
            this.usedGird.add(newIndex);

            // 计算新格子能放多少
            long addNum = Math.min(num, maxNum);
            Item newItem = new Item(id, addNum);

            // 放入背包
            addItem(newIndex, newItem);

            // 更新索引
            if (this.itemIndexMap == null) {
                this.itemIndexMap = new HashMap<>();
            }
            itemIndexMap.computeIfAbsent(id, k -> new ArrayList<>()).add(newIndex);
            num -= addNum;
        }
    }

    /**
     * 删除道具
     *
     * @param id
     * @param num
     * @return
     */
    public CommonResult<Long> removeItem(int id, int num) {
        CommonResult<Long> result = new CommonResult<>(Code.SUCCESS);
        if (num < 1) {
            result.code = Code.PARAM_ERROR;
            return result;
        }

        // 检查背包中是否有足够的道具
        long itemCount = getItemCount(id);
        if (itemCount < num) {
            result.code = Code.NOT_ENOUGH;
            return result;
        }

        List<Integer> girdList = itemIndexMap.get(id);
        long remaining = num;
        // 使用迭代器安全地移除元素
        ListIterator<Integer> iterator = girdList.listIterator(girdList.size());
        while (iterator.hasPrevious() && remaining > 0) {
            int gird = iterator.previous();
            Item item = items.get(gird);

            if (item.getCount() <= remaining) {
                // 移除整个道具
                remaining -= item.getCount();
                items.remove(gird);
                usedGird.remove(gird);
                iterator.remove(); // 从索引列表中移除
            } else {
                // 只减少数量
                item.addCount(-remaining);
                remaining = 0;
            }
        }

        // 如果该道具的所有格子都被移除，从itemIndexMap中清除
        if (itemIndexMap.containsKey(id) && itemIndexMap.get(id).isEmpty()) {
            itemIndexMap.remove(id);
        }

        result.data = itemCount - num;
        return result;
    }

    /**
     * 获取指定道具的总数量
     */
    public long getItemCount(int id) {
        if (itemIndexMap == null || itemIndexMap.isEmpty()) {
            return 0;
        }
        if (!itemIndexMap.containsKey(id)) {
            return 0;
        }
        long total = 0;
        for (int gird : itemIndexMap.get(id)) {
            total += items.get(gird).getCount();
        }
        return total;
    }

    /**
     * 查找可用的格子索引
     *
     * @return 可用索引，
     */
    private int findAvailableIndex() {
        if (this.usedGird == null || this.usedGird.isEmpty()) {
            this.usedGird = new HashSet<>();
            return 0;
        }
        int i = 0;
        while (this.usedGird.contains(i)) {
            i++;
        }
        return i;
    }

    private void addItem(int index, Item item) {
        if (this.items == null) {
            this.items = new HashMap<>();
        }
        this.items.put(index, item);
    }


    public boolean checkHasItems(List<Item> checkItems) {
        if (Objects.isNull(checkItems) || checkItems.isEmpty()) {
            return true;
        }
        if (itemIndexMap == null || itemIndexMap.isEmpty()) {
            return false;
        }
        for (Item checkItem : checkItems) {
            long count = getItemCount(checkItem.getId());
            if (count < checkItem.getCount()) {
                return false;
            }
        }
        return true;
    }
}
