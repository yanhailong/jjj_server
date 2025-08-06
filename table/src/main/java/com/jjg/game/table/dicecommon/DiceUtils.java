package com.jjg.game.table.dicecommon;

import com.jjg.game.common.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 骰子工具类
 *
 * @author 2CL
 */
public class DiceUtils {

    /**
     * 随机几个骰子，将骰子变成一个整数
     */
    public static Long randomNumDice(int diceNum, int diceMinNum, int diceMaxNum) {
        List<Integer> randomDice = randomDice(diceNum, diceMinNum, diceMaxNum);
        long randomDiceNum = randomDice.get(0);
        for (int i = 1; i < randomDice.size(); i++) {
            randomDiceNum += (long) (randomDice.get(i) * Math.pow(10, i));
        }
        return randomDiceNum;
    }

    /**
     * 随机几个骰子
     */
    public static List<Integer> randomDice(int diceNum, int diceMinNum, int diceMaxNum) {
        List<Integer> diceNums = new ArrayList<>();
        for (int i = 0; i < diceNum; i++) {
            int dice = RandomUtils.nextIntInclude(diceMinNum, diceMaxNum);
            diceNums.add(dice);
        }
        return diceNums;
    }
}
