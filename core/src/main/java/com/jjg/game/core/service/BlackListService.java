package com.jjg.game.core.service;

import com.jjg.game.core.dao.BlackListDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2025/10/22 15:11
 */
@Service
public class BlackListService {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private BlackListDao blackListDao;

    private Set<Long> blackIdSet;
    private Set<String> blackIpSet;

    public void init(){
        loadAllBlackId();
        loadAllBlackIp();
    }

    public void loadAllBlackId(){
        this.blackIdSet = blackListDao.getAllBlackId();
        log.debug("加载黑名单id");
    }

    public void loadAllBlackIp(){
        this.blackIpSet = blackListDao.getAllBlackIp();
        log.debug("加载黑名单ip");
    }

    public void addBlackIds(List<Long> ids){
        blackListDao.addBlackIds(ids);
    }

    public void removeBlackIds(List<Long> ids){
        blackListDao.removeBlackIds(ids);
    }

    public void addBlackIps(List<String> ips){
        blackListDao.addBlackIps(ips);
    }

    public void removeBlackIps(List<String> ips){
        blackListDao.removeBlackIps(ips);
    }

    public boolean isBlackIp(String ip){
        if(this.blackIpSet == null || this.blackIpSet.isEmpty()){
            return false;
        }
        return this.blackIpSet.contains(ip);
    }

    public boolean isBlackId(long playerId){
        if(this.blackIdSet == null || this.blackIdSet.isEmpty()){
            return false;
        }
        return this.blackIdSet.contains(playerId);
    }
}
