package com.jjg.game.dollarexpress.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/11 14:54
 */
public interface DollarExpressConst {
    interface COMMON{
        int GAME_TYPE_DOLLAR_EXPRESS = 100100;

        String AXLE_PREFIX = "axle";
        String SPECIAL_PREFIX = "special";

        String COLUM_PREFIX = "column";
        String ICON_PREFIX = "icon";


        //在icon表中普通图标的类型
        int ICON_NORMAL_TYPE = 0;
        //表示是4*5的游戏，总共会出现20个图标
        int ALL_CION_COUNT = 20;

        //每一列有4个图标
        int COLUM_ICON_COUNT = 4;
        //总共有多少列
        int COLUMS_COUNT = 5;

        //每一列的坐标
        int[] COLUM_1_LOCATIONS = {1,2,3,4};
        int[] COLUM_2_LOCATIONS = {5,6,7,8};
        int[] COLUM_3_LOCATIONS = {9,10,11,12};
        int[] COLUM_4_LOCATIONS = {13,14,15,16};
    }

    interface MSGBEAN{
        int TYPE = 0x7;

        //请求场次列表
        int REQ_WARE_HOUSE = 0X7001;
        int RES_WARE_HOUSE = 0X7002;

        //选择游戏场次进入
        int REQ_CHOOSE_WARE = 0X7003;
        int RES_CHOOSE_WARE = 0X7004;

        //开始游戏
        int REQ_START_GAME = 0X7005;
        int RES_START_GAME = 0X7006;
    }
}
