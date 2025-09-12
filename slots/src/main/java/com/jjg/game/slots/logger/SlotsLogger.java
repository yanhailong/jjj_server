package com.jjg.game.slots.logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;
import com.jjg.game.slots.game.wealthgod.data.WealthGodGameRunInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodResultLineInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodSpinInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/27 10:24
 */
@Component
public class SlotsLogger extends BaseLogger {


    /******************************** 美元快递 begin ********************************/

    /**
     * 游戏开奖结果
     *
     * @param player
     */
    public void gameResult(Player player, DollarExpressGameRunInfo gameRunInfo) {
        try {
            JSONObject json = new JSONObject();
            json.put("allWin", gameRunInfo.getAllWinGold());
            json.put("gameType", player.getGameType());
            json.put("beforeGold", gameRunInfo.getBeforeGold());
            json.put("afterGold", player.getGold());
            json.put("icon", gameRunInfo.getIconArr());
            json.put("status", gameRunInfo.getData().getStatus());
            json.put("auto", gameRunInfo.isAuto());

            //添加中奖线
            json = getAwardLineInfo(json, gameRunInfo);
            //添加火车信息
            json = getTrainInfo(json, gameRunInfo);
            //添加美金信息
            json = getDollarsInfo(json, gameRunInfo);

            sendLog("dollarExpressResult", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 中奖线信息
     *
     * @param gameRunInfo
     * @return
     */
    private JSONObject getAwardLineInfo(JSONObject json, DollarExpressGameRunInfo gameRunInfo) {
        if (gameRunInfo.getAwardLineInfos() == null || gameRunInfo.getAwardLineInfos().isEmpty()) {
            return json;
        }

        JSONArray jsonArray = new JSONArray();
        for (ResultLineInfo lineInfo : gameRunInfo.getAwardLineInfos()) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("lineId", lineInfo.id);
            tempJson.put("count", lineInfo.iconIndexs.isEmpty());
            tempJson.put("winGod", lineInfo.winGold);
            jsonArray.add(tempJson);
        }
        json.put("resultLineInfoList", jsonArray);
        return json;
    }

    /**
     * 火车信息
     *
     * @param gameRunInfo
     * @return
     */
    private JSONObject getTrainInfo(JSONObject json, DollarExpressGameRunInfo gameRunInfo) {
        if (gameRunInfo.getTrainList() == null || gameRunInfo.getTrainList().isEmpty()) {
            return json;
        }

        JSONArray jsonArray = new JSONArray();
        for (TrainInfo trainInfo : gameRunInfo.getTrainList()) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("type", trainInfo.type);
            tempJson.put("golds", trainInfo.goldList);
            tempJson.put("poolId", trainInfo.poolId);
            jsonArray.add(tempJson);
        }
        json.put("trainInfos", jsonArray);
        return json;
    }

    /**
     * 火车信息
     *
     * @param gameRunInfo
     * @return
     */
    private JSONObject getDollarsInfo(JSONObject json, DollarExpressGameRunInfo gameRunInfo) {
        if (gameRunInfo.getDollarsInfo() == null) {
            return json;
        }

        JSONObject tempJson = new JSONObject();
        tempJson.put("coinIndexId", gameRunInfo.getDollarsInfo().coinIndexId);
        tempJson.put("dollarIndexIds", gameRunInfo.getDollarsInfo().dollarIndexIds);
        tempJson.put("dollarValueList", gameRunInfo.getDollarsInfo().dollarValueList);

        json.put("dollarsInfo", tempJson);
        return json;
    }

    /******************************** 美元快递 end ********************************/

    /**
     * 游戏开奖结果
     */
    public void gameResult(Player player, WealthGodGameRunInfo gameRunInfo) {
        try {
            JSONObject json = new JSONObject();
            json.put("allWin", gameRunInfo.getAllWinGold());
            json.put("gameType", player.getGameType());
            json.put("beforeGold", gameRunInfo.getBeforeGold());
            json.put("afterGold", player.getGold());
            json.put("icon", gameRunInfo.getIconArr());
            //添加中奖线
            getAwardLineInfo(json, gameRunInfo);

            sendLog("wealthGodResult", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 中奖线信息
     */
    private void getAwardLineInfo(JSONObject json, WealthGodGameRunInfo gameRunInfo) {
        WealthGodSpinInfo spinInfo = gameRunInfo.getSpinInfo();

        List<WealthGodResultLineInfo> resultLineInfoList = spinInfo.getResultLineInfoList();

        if (resultLineInfoList == null || resultLineInfoList.isEmpty()) {
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (WealthGodResultLineInfo lineInfo : resultLineInfoList) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("lineId", lineInfo.id);
            tempJson.put("count", lineInfo.iconIndex.size());
            tempJson.put("winGod", lineInfo.winGold);
            jsonArray.add(tempJson);
        }
        json.put("resultLineInfoList", jsonArray);
    }

}
