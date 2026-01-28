package com.jjg.game.core.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.dao.LoginConfigDao;
import com.jjg.game.core.data.ChannelType;
import com.jjg.game.core.data.LoginConfigData;
import com.jjg.game.core.data.LoginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

    //登录配置  channel -> loginType -> LoginConfigData
    private Map<Integer, Map<Integer, LoginConfigData>> loginConfigMap = new HashMap<>();

    public void init() {
        loadAll();
        initChannelConfig(ChannelType.GOOGLE);
        initChannelConfig(ChannelType.APPLE);
    }

    private void initChannelConfig(ChannelType channel) {
        // 获取或创建通道配置Map
        Map<Integer, LoginConfigData> channelMap = loginConfigMap
                .computeIfAbsent(channel.getValue(), k -> new HashMap<>());

        // 确保包含必要的登录方式
        ensureLoginType(channelMap, LoginType.GUEST);
        ensureLoginType(channelMap, LoginType.PHONE);
    }

    private void ensureLoginType(Map<Integer, LoginConfigData> channelMap, LoginType loginType) {
        channelMap.computeIfAbsent(loginType.getValue(),
                k -> new LoginConfigData(loginType.getValue(), true, true));
    }

    public void save(int loginType, Map<Integer, LoginConfigData> map) {
        loginConfigDao.save(loginType, map);
    }

    public void loadAll() {
        ChannelType[] values = ChannelType.values();
        for (ChannelType channelType : values) {
            load(channelType.getValue());
        }
    }

    public void load(int channel) {
        Map map = loginConfigDao.getAll(channel);
        if (map == null || map.isEmpty()) {
            loginConfigMap.remove(channel);
            log.warn("map为空，移除所有登录方式 channel = {}", channel);
            return;
        }
        loginConfigMap.put(channel, map);
        log.info("加载登录配置 channle = {},loginConfigMap = {}", channel, JSON.toJSONString(map));
    }

    /**
     * 获取该登录方式是否开启
     *
     * @param loginType
     * @return
     */
    public boolean isLoginOpen(int channel, int loginType) {
        LoginConfigData data = getData(channel, loginType);
        if (data == null) {
            return false;
        }

        return data.isLoginOpen();
    }

    /**
     * 是否开启改登录方式的奖励
     *
     * @param channel
     * @param loginType
     * @return
     */
    public boolean isRewardOpen(int channel, int loginType) {
        LoginConfigData data = getData(channel, loginType);
        if (data == null) {
            return false;
        }

        return data.isLoginOpen() && data.isRewardOpen();
    }

    public Map<Integer, LoginConfigData> getDataMap(int channel) {
        return loginConfigMap.get(channel);
    }


    /**
     * 获取登录配置
     *
     * @param channel
     * @param loginType
     * @return
     */
    private LoginConfigData getData(int channel, int loginType) {
        if (loginConfigMap == null || loginConfigMap.isEmpty()) {
            return null;
        }
        Map<Integer, LoginConfigData> tmpMap = loginConfigMap.get(channel);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(loginType);
    }
}
