package com.jjg.game.core.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.core.data.Marquee;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 11
 * @date 2025/8/6 13:56
 */
public class CoreToServerMessageHandler {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private CoreMarqueeManager marqueeManager;

    /**
     * 其他节点推送的跑马灯信息
     * @param notify
     */
    @Command(MessageConst.ToServer.NOTICE_MARQUEE_HALL_MASTER)
    public void notifyMarqueeHallMaster(NotifyAllNodesMarqueeServer notify) {
        try{
            log.info("收到其他节点推送的跑马灯信息 notify = {}", JSON.toJSONString(notify));
            Marquee marquee = new Marquee();
            marquee.setId(notify.id);
            marquee.setContent(notify.content);
            marquee.setShowTime(notify.showTime);
            marquee.setInterval(notify.interval);
            marquee.setType(notify.type);
            marquee.setStartTime(notify.startTime);
            marquee.setEndTime(notify.endTime);

            marqueeManager.addNewMarquee(marquee);
        }catch (Exception e) {
            log.error("",e);
        }
    }

    /**
     * 收到其他节点推送的停止跑马灯信息
     * @param notify
     */
    @Command(MessageConst.ToServer.NOTICE_STOP_MARQUEE_HALL_MASTER)
    public void notifyStopMarqueeHallMaster(NotifyAllNodesStopMarqueeServer notify) {
        try{
            log.info("收到其他节点推送的停止跑马灯信息 id = {}",notify.id);
            marqueeManager.removeMarquee(notify.id);
        }catch (Exception e) {
            log.error("",e);
        }
    }
}
