package com.jjg.game.slots.game.zeusVsHades;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author lihaocao
 * @date 2025/12/2 17:36
 */
public interface ZeusVsHadesConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ZEUS_VS_HADES << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;


        //二选一 宙斯vs哈迪斯 选择
        int REQ_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x5;
        int RES_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x6;

        //请求奖池
        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x7;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x8;
    }

    interface Common{
        int MINI_POOL_ID = 102000101;
        int MINOR_POOL_ID = 102000102;
        int MAJOR_POOL_ID = 102000103;
        int GRAND_POOL_ID = 102000104;
    }

    interface Status{
        int NORMAL = 0;
        //免费触发局
        int CHOOSE_ONE = 1;
        //宙斯触发局
        int ZEUS = 2;
        //哈迪斯触发局
        int HADES = 3;
    }

    interface BaseElement{
        int TYPE0_SMALL = 1;//蓝10 类型为0 最小的id
        int TYPE0_BIG = 20;//哈迪斯 类型为0 最大的id

        int ID_WILD = 21;
        int ID_SCATTER = 34;
        int ID_MINI = 35;
        int ID_MINOR = 36;
        int ID_MAJOR = 37;
        int ID_GRAND = 38;
        int ID_ZEUS_WILD = 41;
        int ID_HADES_WILD = 42;
    }

    interface SpecialMode{
        //普通旋转
        int NORMAL = 1;
        //免费触发局
        int CHOOSE = 2;
        //大奖
        int JACKPOOL = 3;
        //宙斯触发局
        int ZEUS = 4;
        //哈迪斯触发局
        int HADES = 5;
    }

    interface SpecialPlay{
        //用户 选择哈里斯还是宙斯
        int FREE_CHOOSE_ZEUS_OR_HADES = 1;
        //普通转 选择哈里斯还是宙斯
        int NORMAL_CHOOSE_ZEUS_OR_HADES = 2;
    }

    interface SpecialAuxiliary{
        //普通转 宙斯
        int NORMAL_ZEUS = 30200007;
        //普通转 哈里斯
        int NORMAL_HADES = 30200008;

        //普通转  第一列
        int NORMAL_1 = 30200003;
        //免费转  第一列
        int NORMAL_2 = 30200004;
        //免费转  第一列
        int NORMAL_3 = 30200005;
        //免费转  第一列
        int NORMAL_4 = 30200006;

        //免费转 宙斯 触发
        int FREE_ZEUS = 30200009;
        //免费转 宙斯 第一列
        int FREE_ZEUS_1 = 30200010;
        //免费转 宙斯 第一列
        int FREE_ZEUS_2 = 30200011;
        //免费转 宙斯 第一列
        int FREE_ZEUS_3 = 30200012;
        //免费转 宙斯 第一列
        int FREE_ZEUS_4 = 30200013;

        //免费转 哈里斯 触发
        int FREE_HADES = 30200014;
        //免费转 哈里斯 第一列
        int FREE_HADES_1 = 30200015;
        //免费转 哈里斯 第一列
        int FREE_HADES_2 = 30200016;
        //免费转 哈里斯 第一列
        int FREE_HADES_3 = 30200017;
        //免费转 哈里斯 第一列
        int FREE_HADES_4 = 30200018;
    }

}
