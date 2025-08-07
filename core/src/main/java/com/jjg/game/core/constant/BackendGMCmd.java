package com.jjg.game.core.constant;

/**
 * @author lm
 * @date 2025/7/15 16:33
 */
public interface BackendGMCmd {
    String CHANGE_GAME_STATUS = "changeGameStatus";
    //跑马灯
    String SNED_MARQUEE = "sendMarquee";
    //停止跑马灯
    String STOP_MARQUEE = "stopMarquee";
    //发送公告
    String SEND_ANNOUNCEMENT = "sendAnnouncement";
    //删除公告
    String DEL_ANNOUNCEMENT = "delAnnouncement";
    //查询账户
    String QUERY_ACCOUNT = "queryAccount";
    //金币gm
    String GOLD_OPERATOR = "goldOperator";
    //发送邮件
    String SEND_EMAIL = "sendEmail";

    interface Result {
        String SUCCESS = "success";
        String FAIL = "fail";
    }
}
