package com.jjg.game.core.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.pb.NoticeMoneyChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/11 17:55
 */
@Component
public class CoreSendMessageManager {
    protected Logger log = LoggerFactory.getLogger(getClass());

    public NoticeMoneyChange packMoneyChangeMessage(long gold,long diamond){
        NoticeMoneyChange notice = new NoticeMoneyChange();
        notice.gold = gold;
        notice.diamond = diamond;
        return notice;
    }


    /*********************************************************************************************/
    /**
     * 按需要获取发送的消息
     */
    protected void sendRun(PlayerController playerController, SendInfo sendInfo, String logDescribe, boolean debug){
        if(sendInfo == null){
            return;
        }

        //单独发给用户的消息
        sendInfo.getSendMess().entrySet().stream().forEach(en -> {
            en.getValue().forEach(msg -> {
                playerController.send(msg);
            });
        });

        if(sendInfo.getLogMessage().size() > 0){
            logOut(playerController.playerId(),logDescribe, sendInfo.getLogMessage(), debug);
        }
    }

    private void logOut(long playerId,String logDescribe, Object logMessage, boolean debug) {
        if(playerId < 1){
            if (debug) {
                log.debug(logDescribe + ",message={}", JSON.toJSONString(logMessage, SerializerFeature.DisableCircularReferenceDetect));
            } else {
                log.info(logDescribe + ",message={}", JSON.toJSONString(logMessage, SerializerFeature.DisableCircularReferenceDetect));
            }
        }else {
            if (debug) {
                log.debug(logDescribe + ",playerId={},message={}", playerId,JSON.toJSONString(logMessage, SerializerFeature.DisableCircularReferenceDetect));
            } else {
                log.info(logDescribe + ",playerId={},message={}", playerId,JSON.toJSONString(logMessage, SerializerFeature.DisableCircularReferenceDetect));
            }
        }
    }
}
