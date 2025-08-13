package com.jjg.game.hall.manager;

import com.jjg.game.hall.service.HallPlayerService;
import com.mongodb.client.result.DeleteResult;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.PlayerLoginTimeDao;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author 11
 * @date 2025/5/27 9:32
 */
@Component
@EnableScheduling
public class HallClearManager {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected MarsCurator marsCurator;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private PlayerLoginTimeDao playerLoginTimeDao;
    @Autowired
    private HallPlayerService hallPlayerService;

    /**
     * 每天凌晨4点半定时执行
     */
    @Scheduled(cron = "0 30 4 * * ? ")
    private void dailyClear(){
        //是主节点才能执行
        if(marsCurator.master(NodeType.HALL.getValue())){
            clearPlayerData();
        }
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    private void clearToken(){
        //是主节点才能执行
        if(marsCurator.master(NodeType.HALL.getValue())){
            DeleteResult deleteResult = playerSessionTokenDao.clearExpireToken();
            log.info("删除过期token条数: {}", deleteResult.getDeletedCount());
        }
    }

    /**
     * 清理用户数据
     */
    private void clearPlayerData(){

        log.info("开始清除过期player数据");

        long now = System.currentTimeMillis();

        //获取一个时间
        long expireTime = now - TimeHelper.ONE_DAY_OF_MILES;

        Set<Object> loginSet = playerLoginTimeDao.getLoginSet(expireTime);
        if(loginSet == null || loginSet.isEmpty()){
            return;
        }

        int index = 0;
        int finishNum = 0;
        for(Object o : loginSet){
            try{
                if (index % 1000 == 0) {
                    log.info("已执行循环次数index={},成功次数={}", index,finishNum);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                boolean clear = hallPlayerService.clear(Long.parseLong(o.toString()), expireTime);
                if(clear){
                    finishNum++;
                }
                index++;
            }catch (Exception e){
                log.error("清除player数据异常,playerId:{}", o, e);
            }
        }
        log.info("清除player数据完成,循环次数={},成功次数={},消耗时间={} ms",index,finishNum,System.currentTimeMillis()-now);
    }
}
