package com.jjg.game.slots.game.dollarexpress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 18:08
 */
@Component
public class DollarExpressLogger extends BaseLogger {
    /**
     * 游戏开奖结果
     * @param player
     */
    public void gameResult(Player player, DollarExpressGameRunInfo gameRunInfo){
        try{
            JSONObject json = new JSONObject();
            json.put("logType","dollarExpressResult");
            json.put("allWin",gameRunInfo.getAllWinGold());
            json.put("beforeGold",gameRunInfo.getBeforeGold());
            json.put("afterGold",player.getGold());
            json.put("icon",gameRunInfo.getIconArr());
            json.put("status",gameRunInfo.getStatus());

            //添加中奖线
            json = getAwardLineInfo(json,gameRunInfo);
            //添加火车信息
            json = getTrainInfo(json,gameRunInfo);
            //添加美金信息
            json = getDollarsInfo(json,gameRunInfo);

            sendLog(player,json);
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 中奖线信息
     * @param gameRunInfo
     * @return
     */
    private JSONObject getAwardLineInfo(JSONObject json,DollarExpressGameRunInfo gameRunInfo){
        if(gameRunInfo.getAwardLineInfos() == null || gameRunInfo.getAwardLineInfos().isEmpty()){
            return json;
        }

        JSONArray jsonArray = new JSONArray();
        for(ResultLineInfo lineInfo : gameRunInfo.getAwardLineInfos()){
            JSONObject tempJson = new JSONObject();
            tempJson.put("lineId",lineInfo.id);
            tempJson.put("count",lineInfo.iconIndexs.isEmpty());
            tempJson.put("winGod",lineInfo.winGold);
            jsonArray.add(tempJson);
        }
        json.put("resultLineInfoList",jsonArray);
        return json;
    }

    /**
     * 火车信息
     * @param gameRunInfo
     * @return
     */
    private JSONObject getTrainInfo(JSONObject json,DollarExpressGameRunInfo gameRunInfo){
        if(gameRunInfo.getTrainList() == null || gameRunInfo.getTrainList().isEmpty()){
            return json;
        }

        JSONArray jsonArray = new JSONArray();
        for(TrainInfo trainInfo : gameRunInfo.getTrainList()){
            JSONObject tempJson = new JSONObject();
            tempJson.put("type",trainInfo.type);
            tempJson.put("golds",trainInfo.goldList);
            tempJson.put("poolId",trainInfo.poolId);
            jsonArray.add(tempJson);
        }
        json.put("trainInfos",jsonArray);
        return json;
    }

    /**
     * 火车信息
     * @param gameRunInfo
     * @return
     */
    private JSONObject getDollarsInfo(JSONObject json,DollarExpressGameRunInfo gameRunInfo){
        if(gameRunInfo.getDollarsInfo() == null){
            return json;
        }

        JSONObject tempJson = new JSONObject();
        tempJson.put("coinIndexId",gameRunInfo.getDollarsInfo().coinIndexId);
        tempJson.put("dollarIndexIds",gameRunInfo.getDollarsInfo().dollarIndexIds);
        tempJson.put("dollarValueList",gameRunInfo.getDollarsInfo().dollarValueList);

        json.put("dollarsInfo",tempJson);
        return json;
    }
}
