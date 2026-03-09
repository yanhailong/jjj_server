//package com.jjg.game.table.russianlette.gamephase;
//
//import com.jjg.game.room.constant.EGamePhase;
//import com.jjg.game.room.controller.AbstractPhaseGameController;
//import com.jjg.game.sampledata.bean.Room_BetCfg;
//import com.jjg.game.table.common.gamephase.TableWaitReadyPhase;
//import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
//import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
//
///**
// * 俄罗斯转盘休闲阶段（REST）
// * <p>
// * 游戏循环的最后一个阶段，上一局结算结束后进入，持续 {@code stageTime[0]} 秒（默认 4s）。
// * <ul>
// *   <li>继承 {@link TableWaitReadyPhase} 负责清除本局数据、广播通用等待消息</li>
// *   <li>额外广播 {@code NotifyRussianLettePhaseChangInfo(REST)} 供客户端切换为休闲动画</li>
// * </ul>
// * </p>
// *
// * @author lhc
// */
//public class RussianLetteTableWaitReadyPhase extends TableWaitReadyPhase<RussianLetteGameDataVo> {
//
//    public RussianLetteTableWaitReadyPhase(AbstractPhaseGameController<Room_BetCfg, RussianLetteGameDataVo> gameController) {
//        super(gameController);
//    }
//
//    /**
//     * 休闲阶段使用 REST 枚举，而非基类的 WAIT_READY
//     */
//    @Override
//    public EGamePhase getGamePhase() {
//        return EGamePhase.REST;
//    }
//
//    /**
//     * 阶段开始：
//     * 1. 调用父类逻辑（发送 NotifyRoomReadyWait、清除本局数据）
//     * 2. 额外广播俄罗斯转盘专属阶段变化通知（REST，无概率/开奖信息）
//     */
//    @Override
//    public void phaseDoAction() {
//        super.phaseDoAction();
//        log.info("执行RussianLetteTableWaitReadyPhase（休闲阶段）中phaseDoAction");
//        // 广播俄罗斯转盘专属阶段切换通知（prob/settlementInfo 在休闲阶段均为 null）
//        broadcastMsgToRoom(
//                RussianLetteMessageBuilder.buildPhaseChangInfo(
//                        EGamePhase.REST,
//                        gameDataVo.getPhaseEndTime(),
//                        null,
//                        null));
//    }
//
//    @Override
//    public int hashCode() {
//        return super.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        return super.equals(o);
//    }
//}
