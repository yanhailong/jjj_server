package com.jjg.game.table.redblackwar.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.data.Card;

/**
 * @author 11
 * @date 2025/6/27 17:55
 */
public interface RedBlackWarConstant {

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "redblackwar";
        //最大历史记录
        int MAX_HISTORY = 50;
        //红黑大战配置表界限
        int RED_BLACK_LIMIT = 10;
        //幸运一击 对子的界限
        Card PAIR_MIN_LIMIT = new Card(1, 9);
        //押注红方区域
        int RED_AREA = 1;
        //押注黑方区域
        int BLACK_AREA = 2;
        //押注幸运区域
        int LUCK_AREA = 3;
        //前端押注幸运区域
        int CLIENT_LUCK_AREA = 20010003;
    }

    enum Camp {
        RED, BLACK
    }
}
