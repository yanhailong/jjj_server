package com.jjg.game.core.service;

import com.jjg.game.core.dao.LoginConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author 11
 * @date 2025/10/22 13:36
 */
@Service
public class LoginConfigService {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private LoginConfigDao loginConfigDao;

    private Map<Integer,Boolean> loginConfigMap;

    public void init(){
        load();
    }

    public void save(int loginType,boolean open){
        loginConfigDao.save(loginType,open);
    }

    public void load(){
        this.loginConfigMap = loginConfigDao.getAll();
        log.debug("加载登录配置 loginConfigMap = {}", this.loginConfigMap);
    }

    /**
     * 获取该登录方式是否开启
     * @param loginType
     * @return
     */
    public boolean isOpen(int loginType){
        if(loginConfigMap == null || loginConfigMap.isEmpty()){
            return true;
        }

        Boolean b = loginConfigMap.get(loginType);
        if(b == null){
            return true;
        }
        return b;
    }
}
