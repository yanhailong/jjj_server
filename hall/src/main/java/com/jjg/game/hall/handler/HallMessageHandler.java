package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallMessageConst;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.pb.ReqChooseWare;
import com.jjg.game.hall.pb.ReqChooseGame;
import com.jjg.game.hall.pb.ResChooseGame;
import com.jjg.game.hall.pb.ResChooseWare;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/10 17:13
 */
@Component
@MessageType(HallMessageConst.MsgBean.TYPE)
public class HallMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private HallPlayerService hallPlayerService;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private HallService hallService;


    /**
     * 进入游戏
     * @param playerController
     * @param req
     */
    @Command(HallMessageConst.MsgBean.REQ_ENTER_GAME)
    public void reqChooseGame(PlayerController playerController, ReqChooseGame req){
        ResChooseGame res = new ResChooseGame(HallCode.SUCCESS);
        try{
            if(req.gameType < 1){
                res.code = Code.PARAM_ERROR;
                log.debug("游戏类型错误，选择游戏失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            List<WareHouseConfigInfo> wareHouseConfigList = hallService.getWareHouseConfigByGameType(req.gameType);
            if(wareHouseConfigList == null || wareHouseConfigList.isEmpty()){
                res.code = Code.NOT_FOUND;
                log.debug("未找到对应的游戏场次配置，选择游戏失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            res.wareHouseList = wareHouseConfigList;
            playerController.send(res);
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 选择游戏场次进入
     * @param playerController
     * @param req
     */
    @Command(HallMessageConst.MsgBean.REQ_CHOOSE_WARE)
    public void reqChooseWare(PlayerController playerController, ReqChooseWare req){
        ResChooseWare res = new ResChooseWare(HallCode.SUCCESS);
        try{
            log.info("收到玩家选择游戏场次 playerId={},req={}",playerController.playerId(), JSONObject.toJSONString(req));

            if(req.gameType < 1){
                res.code = Code.PARAM_ERROR;
                log.debug("游戏类型错误，选择场次失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            List<WareHouseConfigInfo> wareHouseConfigList = hallService.getWareHouseConfigByGameType(req.gameType);
            if(wareHouseConfigList == null || wareHouseConfigList.isEmpty()){
                res.code = Code.NOT_FOUND;
                log.debug("未找到对应的游戏场次配置，选择场次失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            WareHouseConfigInfo info = wareHouseConfigList.stream().filter(c -> c.wareId == req.wareId).findFirst().orElse(null);
            if(info == null){
                res.code = Code.NOT_FOUND;
                log.debug("未找到对应的游戏场次配置2，选择场次失败 playerId = {},gameType = {},wareId = {}",playerController.playerId(),req.gameType,req.wareId);
                return;
            }

            MarsNode node = nodeManager.loadGameNode(NodeType.GAME, req.gameType, playerController.playerId(), playerController.player.getIp());
            if(node == null){
                res.code = Code.NOT_FOUND;
                log.debug("获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}",playerController.playerId(),req.gameType);
                return;
            }

            //更新session中的gametype
            playerSessionService.changeGameType(playerController.playerId(), req.gameType, req.wareId);
            //切换节点
            clusterSystem.switchNode(playerController.session,node);
            playerController.send(res);
        }catch (Exception e){
            log.error("", e);
        }
    }

    @Override
    public String gm(PlayerController playerController,String cmd,String params) {
        try{
            log.debug("收到gm命令 playerId = {},cmd = {},params = {}",playerController.playerId(),cmd,params);
            if("enterGame".equals(cmd)){
                ReqChooseGame req = new ReqChooseGame();
                req.gameType = Integer.parseInt(params);
                reqChooseGame(playerController,req);
            }

        }catch (Exception e){
            log.error("",e);
        }
        return null;
    }
}
