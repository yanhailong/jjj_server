package com.jjg.game.common.message;

import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.pb.ResHeartBeat;
import com.jjg.game.common.protostuff.*;
import com.jjg.game.common.protostuff.*;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2022/5/17
 */
@Component
@MessageType(MessageConst.ToClientConst.TYPE)
public class HeartBeatHandler {

    @Command(MessageConst.ToClientConst.REQ_HEART_BEAT)
    public void heartHeatReq(PFSession session, Connect connect){
        if (session != null) {
            session.send(new ResHeartBeat(System.currentTimeMillis()));
        } else if (connect != null) {
            ResHeartBeat pong = new ResHeartBeat(0);
            PFMessage pfMessage = new PFMessage(MessageConst.ToClientConst.RES_HEART_BEAT, ProtostuffUtil.serialize(pong));
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            connect.write(clusterMessage);
        }
    }
}
