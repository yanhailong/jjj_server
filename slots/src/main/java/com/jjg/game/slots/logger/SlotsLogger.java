package com.jjg.game.slots.logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.slots.data.GameRunInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2025/8/27 10:24
 */
@Component
public class SlotsLogger extends BaseLogger {

    /**
     * 添加日志中的基础信息
     *
     * @param player
     * @param gameRunInfo
     * @param json
     * @return
     */
    private JSONObject baseInfo(Player player, GameRunInfo gameRunInfo, JSONObject json) {
        json.put("bet", gameRunInfo.getStake());
        json.put("allWin", gameRunInfo.getAllWinGold());
        json.put("gameType", player.getGameType());
        json.put("beforeGold", gameRunInfo.getBeforeGold());
        json.put("afterGold", gameRunInfo.getAfterGold());
        json.put("level", player.getLevel());
        json.put("exp", player.getExp());
        json.put("auto", gameRunInfo.isAuto());

        if (gameRunInfo.getBetDivideInfo() != null) {
            //税收
            json.put("tax", gameRunInfo.getBetDivideInfo().getTax());
            //收益
            json.put("income", gameRunInfo.getBetDivideInfo().getInCome());
        }

        if (gameRunInfo.getData() != null) {
            //房间id
            json.put("roomId", gameRunInfo.getData().getRoomId());
        }
        json.put("resultLibId", gameRunInfo.getResultLib() == null ? "null" : gameRunInfo.getResultLib().getId());
        return json;
    }

    /**
     * 游戏结果日志
     *
     * @param player
     * @param gameRunInfo
     * @param res
     */
    public void gameResult(Player player, GameRunInfo gameRunInfo, Object res) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();
            //添加基础公共信息
            json = baseInfo(player, gameRunInfo, json);
            //添加游戏数据
            json.put("gameData", JSON.toJSONString(res));
            //发送日志
            sendLog("slotsResult", player, json);

//            log.debug("打印游戏结果日志 json = {}", json.toJSONString());
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 结果库更新记录
     */
    public void addGenSlotsLib(int gameType, Map<Integer, Integer> libTypeCountMap) {
        try {
            JSONObject json = new JSONObject();
            json.put("genGameType", gameType);
            if (libTypeCountMap != null && !libTypeCountMap.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                libTypeCountMap.forEach((k, v) -> {
                    JSONObject tmpJson = new JSONObject();
                    tmpJson.put("libType", k);
                    tmpJson.put("count", v);
                    jsonArray.add(tmpJson);
                });
                json.put("libTypeCount", jsonArray);
            }
            sendLog("genSlotsLib", null, json);
        } catch (Exception e) {
            log.error("addGenSlotsLib", e);
        }
    }
}
