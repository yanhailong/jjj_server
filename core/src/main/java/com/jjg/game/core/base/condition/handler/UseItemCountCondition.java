package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.UserItem;
import com.jjg.game.core.base.condition.event.UserItemEvent;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 12006_每累计达到有效流水时触发|指定倍场游戏类型可多个倍场
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class UseItemCountCondition extends BaseRedisCondition<UserItem> {


    protected UseItemCountCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "useItemCount";
    }

    @Override
    public UserItem parse(List<String> args) {
        String itemId = args.getFirst();
        String count = args.get(1);
        return new UserItem(Integer.parseInt(itemId), Integer.parseInt(count));
    }

    public boolean matchCheck(UserItemEvent event, UserItem config) {
        return event.getItemId() == config.itemId();
    }

    @Override
    public MatchResultData match(ConditionContext ctx, UserItem config) {
        String customId = String.valueOf(ctx.player().getId()) + config.itemId();
        BigDecimal count = countDao.getCount(getFeatureId(ctx), customId);
        if (count.longValue() >= config.count()) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.count(), count.longValue());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, UserItem config) {
        if (ctx.event() instanceof UserItemEvent event && matchCheck(event, config)) {
            String customId = String.valueOf(ctx.player().getId()) + event.getItemId();
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (count.longValue() >= config.count()) {
                return MatchResultData.match();
            }
            count = countDao.incrBy(ctx.player().getId(), featureId, customId, BigDecimal.valueOf(event.getCount()));
            if (count.longValue() >= config.count()) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode(), config.count(), count.longValue());
        }
        return MatchResultData.unknown();
    }

    @Override
    public void delete(ConditionContext ctx, UserItem config) {
        String customId = String.valueOf(ctx.player().getId()) + config.itemId();
        countDao.reset(ctx.player().getId(), getFeatureId(ctx), customId);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12101);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}
