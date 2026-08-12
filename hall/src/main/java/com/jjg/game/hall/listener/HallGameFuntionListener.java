package com.jjg.game.hall.listener;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.GameFunctionListener;
import com.jjg.game.hall.pb.res.ResFunctionOpenList;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HallGameFuntionListener implements GameFunctionListener {
    @Override
    public void notifyAllFunction(PFSession session, List<Integer> openedFuncIdList) {
        if(session == null) {
            return;
        }
        // 推送功能发生了变化
        ResFunctionOpenList res = new ResFunctionOpenList(Code.SUCCESS);
        res.openedFunctionIdList = openedFuncIdList;
        session.send(res);
    }
}
