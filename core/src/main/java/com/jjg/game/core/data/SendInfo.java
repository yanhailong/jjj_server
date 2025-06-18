package com.jjg.game.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/12 17:33
 */
public class SendInfo {
    //非广播，每个用户需要发送的消息
    private Map<Long, List<Object>> sendMess = new HashMap<>();
    //广播消息
    private List<BroadcastMsg> broadcastMsgList;
    //需要打印的日志
    private List<Object> logMessage = new ArrayList<>();

    public Map<Long, List<Object>> getSendMess() {
        return sendMess;
    }

    public void setSendMess(Map<Long, List<Object>> sendMess) {
        this.sendMess = sendMess;
    }

    public void broadcastMsg(Object sendBroadcast) {
        broadcastMsg(sendBroadcast,null);
    }

    public void broadcastMsg(Object sendBroadcast, String exceptPlayerId) {
        BroadcastMsg msg = new BroadcastMsg();
        msg.setMsg(sendBroadcast);
        msg.setExceptPlayerId(exceptPlayerId);
        if(this.broadcastMsgList == null){
            this.broadcastMsgList = new ArrayList<>();
        }
        this.broadcastMsgList.add(msg);
    }

    public void addPlayerMsg(long playerId,Object msg){
        List<Object> list = sendMess.computeIfAbsent(playerId, k -> new ArrayList<>());
        list.add(msg);
    }

    public List<Object> getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(List<Object> logMessage) {
        this.logMessage = logMessage;
    }

    public boolean broadcast(){
        return this.broadcastMsgList != null;
    }

    public List<BroadcastMsg> getBroadcastMsgList() {
        return broadcastMsgList;
    }

    public void setBroadcastMsgList(List<BroadcastMsg> broadcastMsgList) {
        this.broadcastMsgList = broadcastMsgList;
    }
}
