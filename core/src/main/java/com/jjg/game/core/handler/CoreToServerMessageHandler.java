package com.jjg.game.core.handler;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.core.pb.NotifyMarquee;
import com.jjg.game.core.pb.NotifyStopMarquee;
import com.jjg.game.core.pb.gm.ReqMarqueeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 11
 * @date 2025/8/6 13:56
 */
public class CoreToServerMessageHandler {
    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 收到跑马灯信息
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_MARQUEE)
    public void noticeMarquee(ReqMarqueeServer req) {
        try{
            NotifyMarquee notifyMarquee = new NotifyMarquee();
            notifyMarquee.content = req.content;
            ClusterSystem.system.sessionMap().entrySet().forEach(en -> {en.getValue().send(notifyMarquee);});
            log.info("推送跑马灯结束 content = {}",req.content);
        }catch (Exception e) {
            log.error("",e);
        }
    }

    /**
     * 收到停止跑马灯信息
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_STOP_MARQUEE)
    public void noticeStopMarquee(ReqMarqueeServer req) {
        try{
            NotifyStopMarquee notifyStopMarquee = new NotifyStopMarquee();
            ClusterSystem.system.sessionMap().entrySet().forEach(en -> {en.getValue().send(notifyStopMarquee);});
            log.info("停止跑马灯 content = {}",req.content);
        }catch (Exception e) {
            log.error("",e);
        }
    }
}
