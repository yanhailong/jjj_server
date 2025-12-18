package com.jjg.game.slots.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SlotsSchedulManager {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsRoomManager slotsRoomManager;


    @Scheduled(fixedRate = 60000)
    private void slotsCollect() {
        slotsRoomManager.slotsRoomCollect();
        slotsRoomManager.updatePool();
    }
}
