package com.jjg.game.core.handler;

import com.jjg.game.core.data.ReloadType;
import com.jjg.game.core.rpc.GmToAllBridge;
import com.jjg.game.core.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 11
 * @date 2026/1/19
 */
public class CoreRPCController implements GmToAllBridge {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SmsService smsService;

    @Override
    public int reload(int reloadType) {
        try {
            ReloadType type = ReloadType.valueOf(reloadType);
            log.info("收到重新加载配置的消息 reloadType = {}", type);
            switch (type) {
                case SMS_CONFIG -> smsService.reloadConfig();
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return 0;
    }
}
