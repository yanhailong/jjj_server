package com.jjg.game.ploy.games.luckypoker;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerConstant;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerPlayerPloyGameData;
import com.jjg.game.ploy.games.luckypoker.data.PokerRank;
import com.jjg.game.ploy.games.luckypoker.pb.ReqDealCards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2026/3/20
 */
@Component
@MessageType(MessageConst.MessageTypeDef.PLOY_LUCKY_POKER)
public class LuckyPokerMessageHandler implements GmListener {
    private final Logger log = LoggerFactory.getLogger(LuckyPokerMessageHandler.class);

    @Autowired
    private LuckyPokerPloyController luckyPokerPloyController;

    /**
     * 请求发牌
     *
     * @param playerController
     * @param req
     */
    @Command(LuckyPokerConstant.MsgBean.REQ_DEAL_CARDS)
    public void reqDealCards(PlayerController playerController, ReqDealCards req) {
        playerController.send(luckyPokerPloyController.dealCards(playerController, req.pokerIds));
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("forceRank".equalsIgnoreCase(gmOrders[0])) {
                //GM命令：强制下一把/后续每把的牌型；传 0 或 off 清除
                //用法：forceRank 9  → 必出皇家同花顺
                //     forceRank off → 关闭强制
                LuckyPokerPlayerPloyGameData gameData = luckyPokerPloyController.getPlayerGameData(playerController.playerId());
                if (gameData == null) {
                    log.warn("forceRank 失败，未找到玩家数据 playerId = {}", playerController.playerId());
                    res.code = Code.NOT_FOUND;
                    return res;
                }
                String arg = gmOrders.length > 1 ? gmOrders[1] : "";
                if ("off".equalsIgnoreCase(arg) || "0".equals(arg) || arg.isEmpty()) {
                    gameData.setTestForceRank(null);
                    log.info("已清除强制牌型 playerId = {}", playerController.playerId());
                } else {
                    int rank = Integer.parseInt(arg);
                    PokerRank pokerRank = PokerRank.rankOf(rank);
                    if (pokerRank == null) {
                        log.warn("forceRank 参数非法 playerId = {},arg = {}", playerController.playerId(), arg);
                        res.code = Code.PARAM_ERROR;
                        return res;
                    }
                    gameData.setTestForceRank(rank);
                    log.info("已设置强制牌型 playerId = {},rank = {}", playerController.playerId(), pokerRank);
                }
                res.code = Code.SUCCESS;
            } else if ("dealCards".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到选择 dealCards 的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);

                List<Integer> list = new ArrayList<>();
                String[] arr = gmOrders[1].split(",");
                for (String s : arr) {
                    int id = Integer.parseInt(s);
                    if(id < 1){
                        continue;
                    }

                    list.add(id);
                }

                ReqDealCards req = new ReqDealCards();
                req.pokerIds = list;


                reqDealCards(playerController, req);
                res.code = Code.SUCCESS;
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
