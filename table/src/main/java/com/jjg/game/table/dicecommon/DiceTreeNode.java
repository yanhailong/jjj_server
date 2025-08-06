package com.jjg.game.table.dicecommon;

import java.util.HashMap;
import java.util.Map;

/**
 * 骰子类的tree, 所有的骰子都可对应
 *
 * @author 2CL
 */
public class DiceTreeNode<D> {
    // 骰子点数
    private int diceNum;
    // next 骰子点数 <=> 骰子节点tree
    private final Map<Integer, DiceTreeNode<D>> next = new HashMap<>();
    // pre
    private DiceTreeNode<D> prev;
    // 节点对应保存的数据
    private D data;

    public DiceTreeNode(int diceNum) {
        this.diceNum = diceNum;
    }

    public int getDiceNum() {
        return diceNum;
    }

    public void setDiceNum(int diceNum) {
        this.diceNum = diceNum;
    }

    public DiceTreeNode<D> getNext(int diceNum) {
        return next.get(diceNum);
    }

    public void addNext(int diceNum, DiceTreeNode<D> next) {
        this.next.put(diceNum, next);
    }

    public DiceTreeNode<D> getPrev() {
        return prev;
    }

    public void setPrev(DiceTreeNode<D> prev) {
        this.prev = prev;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }
}
