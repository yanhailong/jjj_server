package com.jjg.game.hall.manager;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.hall.dao.HallPoolDao;
import com.jjg.game.hall.pointsaward.PointsAwardService;
import com.jjg.game.hall.service.HallPlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/27 9:32
 */
@Component
@EnableScheduling
public class HallSchedulManager {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected MarsCurator marsCurator;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private HallPlayerService hallPlayerService;
    @Autowired
    private MailService mailService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PointsAwardService pointsAwardService;
    @Autowired
    private HallPoolDao hallPoolDao;

    /**
     * 每天凌晨4点半定时执行
     */
    @Scheduled(cron = "0 30 4 * * ? ")
    private void dailyClear() {
        //是主节点才能执行
        if (marsCurator.isMaster()) {
            hallPlayerService.clean();
            mailService.cleanMails();
            orderService.clean();
        }
    }

    /**
     * 每天凌晨0点定时执行
     */
    @Scheduled(cron = "0 0 0 * * ? ")
//    @Scheduled(initialDelay = 10 * 1000, fixedRate = 40 * 1000)
    private void dailyZero() {
        //是主节点才能执行
        if (marsCurator.isMaster()) {
            hallPoolDao.snapshot();
        }
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    private void clearToken() {
        //是主节点才能执行
        if (marsCurator.isMaster()) {
            int delCount = playerSessionTokenDao.clearExpireToken();
            log.info("删除过期token条数: {}", delCount);
        }
    }

}
