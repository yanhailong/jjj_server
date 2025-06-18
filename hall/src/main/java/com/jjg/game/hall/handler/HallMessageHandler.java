package com.jjg.game.hall.handler;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallMessageConst;
import com.jjg.game.hall.pb.ReqEnterGame;
import com.jjg.game.hall.pb.ResEnterGame;
import com.jjg.game.hall.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 17:13
 */
@Component
@MessageType(HallMessageConst.MSGBEAN.TYPE)
public class HallMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PlayerService playerService;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private NodeManager nodeManager;


    /**
     * 进入游戏
     * @param playerController
     * @param req
     */
    @Command(HallMessageConst.MSGBEAN.REQ_ENTER_GAME)
    public void reqEnterGame(PlayerController playerController, ReqEnterGame req){
        ResEnterGame res = new ResEnterGame(HallCode.SUCCESS);
        try{
            if(req.gameType < 1){
                log.debug("游戏类型错误，进入游戏失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            //获取对应的游戏节点
            MarsNode node = nodeManager.loadGameNode(NodeType.GAME, req.gameType, playerController.playerId(), playerController.player.getIp());
            if(node == null){
                log.debug("获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            //切换节点
            clusterSystem.switchNode(playerController.session,node);

            playerController.send(res);

            playerService.checkAndSave(playerController.playerId(), p -> {
                p.setGameType(req.gameType);
                return true;
            });
        }catch (Exception e){
            log.error("",e);
        }
    }

    @Override
    public String gm(PFSession session,String cmd,String params) {
        try{
            log.debug("收到gm命令 playerId = {},cmd = {},params = {}",session.getPlayerId(),cmd,params);
            if("enterGame".equals(cmd)){
                Player player = playerService.get(session.getPlayerId());
                PlayerController playerController = new PlayerController(session,player);

                ReqEnterGame req = new ReqEnterGame();
                req.gameType = Integer.parseInt(params);

                reqEnterGame(playerController,req);
            }

        }catch (Exception e){
            log.error("",e);
        }
        return null;
    }
}
