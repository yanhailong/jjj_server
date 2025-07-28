package com.jjg.game.slots.game.dollarexpress;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGameManager;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressSendMessageManager;
import com.jjg.game.slots.game.dollarexpress.pb.ReqChooseFreeModel;
import com.jjg.game.slots.game.dollarexpress.pb.ReqConfigInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ReqInvestArea;
import com.jjg.game.slots.game.dollarexpress.pb.ReqStartGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/12 17:15
 */
@Component
@MessageType(MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE)
public class DollarExpressMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressGameManager dollarExpressManager;
    @Autowired
    private DollarExpressSendMessageManager sendMessageManager;


    /**
     * 请求配置信息
     *
     * @param playerController
     * @param req
     */
    @Command(SlotsConst.MsgBean.REQ_CONFIG_INFO)
    public void reqConfigInfo(PlayerController playerController, ReqConfigInfo req) {
        try {
            log.info("收到玩家请求配置 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            sendMessageManager.sendConfigMessage(playerController);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param req
     */
    @Command(SlotsConst.MsgBean.REQ_START_GAME)
    public void reqStartGame(PlayerController playerController, ReqStartGame req) {
        try {
            log.info("收到玩家开始游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo dollarExpressGameRunInfo = this.dollarExpressManager.playerStartGame(playerController, req.stakeVlue);
            sendMessageManager.sendStartGameMessage(playerController, dollarExpressGameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 选择免费游戏类型
     *
     * @param playerController
     * @param req
     */
    @Command(SlotsConst.MsgBean.REQ_CHOOSE_FREE_MODEL)
    public void reqChooseFreeModel(PlayerController playerController, ReqChooseFreeModel req) {
        try {
            log.info("收到选择免费游戏类型 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo = dollarExpressManager.playerChooseFreeGameType(playerController, req.status);
            sendMessageManager.sendChooseOneMessage(playerController, gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 投资游戏
     *
     * @param playerController
     * @param req
     */
    @Command(SlotsConst.MsgBean.REQ_INVEST_AREA)
    public void reqInvestArea(PlayerController playerController, ReqInvestArea req) {
        try {
            log.info("收到选择投资游戏 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            DollarExpressGameRunInfo gameRunInfo = dollarExpressManager.invest(playerController, req.areaId);
            sendMessageManager.sendInvers(playerController,gameRunInfo);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("setIcons".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到设置图标的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);

                TestLibData testLibData = new TestLibData();

                String[] arr2 = gmOrders[1].split(",");
                int[] iconArr = new int[21];
                for (int i = 1; i < iconArr.length; i++) {
                    iconArr[i] = Integer.parseInt(arr2[i - 1]);
                }
                testLibData.setIcons(iconArr);
                if (gmOrders.length > 2) {
                    testLibData.setUpdateGird(Boolean.parseBoolean(gmOrders[2]));
                }
                dollarExpressManager.addTestIconData(playerController, testLibData,true);
            }else if("setRoller".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到设置滚轴的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                TestLibData testLibData = new TestLibData();

                String[] arr2 = gmOrders[1].split(",");
                int[] iconArr = new int[20];
                for (int i = 0; i < iconArr.length; i++) {
                    iconArr[i] = Integer.parseInt(arr2[i]);
                }
                testLibData.setIcons(iconArr);
                if (gmOrders.length > 2) {
                    testLibData.setUpdateGird(Boolean.parseBoolean(gmOrders[2]));
                }
                dollarExpressManager.addTestIconData(playerController, testLibData,false);
            }else if("chooseFreeModel".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择免费模式的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                ReqChooseFreeModel req = new ReqChooseFreeModel();
                req.status = Integer.parseInt(gmOrders[1]);
                reqChooseFreeModel(playerController, req);
            }else if("startGame".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到开始游戏的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                ReqStartGame req = new ReqStartGame();
                req.stakeVlue = Long.parseLong(gmOrders[1]);
                reqStartGame(playerController, req);
            }else if("invest".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到投资游戏的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                ReqInvestArea req = new ReqInvestArea();
                req.areaId = Integer.parseInt(gmOrders[1]);
                reqInvestArea(playerController, req);
            }else if("selectAllArea".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择所有地区的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                dollarExpressManager.selectAllArea(playerController);
            }else if("adminGenerateLib".equals(gmOrders[0])) {
                log.debug("收到生成结果库的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                int count = Integer.parseInt(gmOrders[1]);
                if(count > 100000){
                    log.debug("数字太大，请重新输入 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                    res.code = Code.FAIL;
                    return res;
                }
                dollarExpressManager.generateLib(count);
            }else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
