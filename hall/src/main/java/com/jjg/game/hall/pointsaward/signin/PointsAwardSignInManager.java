package com.jjg.game.hall.pointsaward.signin;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PointsAwardSigninCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;

/**
 * 积分大奖的签到活动
 */
@Component
public class PointsAwardSignInManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 签到服务
     */
    private final PointsAwardSignInService pointsAwardSignInService;

    private final TreeMap<Integer, PointsAwardSigninCfg> signInCfgMap = new TreeMap<>();

    /**
     * 配置初始化时间
     */
    private LocalDate configDate;

    public PointsAwardSignInManager(PointsAwardSignInService pointsAwardSignInService) {
        this.pointsAwardSignInService = pointsAwardSignInService;
    }

    /**
     * 初始化
     */
    public void init() {
        initConfig();
        pointsAwardSignInService.init(this);
    }

    /**
     * 初始化配置
     */
    public void initConfig() {
        configDate = LocalDate.now();
        LocalDate now = LocalDate.now();
        //当前月最大天数
        int totalDays = now.lengthOfMonth();
        List<PointsAwardSigninCfg> signinCfgList = GameDataManager.getPointsAwardSigninCfgList();
        if (signinCfgList != null && !signinCfgList.isEmpty()) {
            //先保存一份默认配置
            List<PointsAwardSigninCfg> resultList = signinCfgList.stream().filter(cfg -> {
                String time = cfg.getTime();
                //没有时间限制 并且小于本月最大天数 不加载多余配置有几天就加载几条
                return (time == null || time.isEmpty()) && cfg.getId() <= totalDays;
            }).toList();
            if (resultList.isEmpty()) {
                log.warn("积分大奖签到配置没有默认配置!");
            }
            //根据当前月份筛选一份新的配置
            List<PointsAwardSigninCfg> todaySignInConfigs = signinCfgList.stream().filter(cfg -> {
                String time = cfg.getTime();
                //有时间限制
                if (time != null && !time.isEmpty()) {
                    long timestamp = TimeHelper.getTimestamp(time.trim());
                    //有时间限制 并且小于本月最大天数 不加载多余配置有几天就加载几条
                    if (timestamp > 0) {
                        LocalDate dateTime = LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                        return dateTime.isEqual(now) && cfg.getId() <= totalDays;
                    }
                }
                return false;
            }).toList();
            signInCfgMap.clear();
            if (!todaySignInConfigs.isEmpty()) {
                todaySignInConfigs.forEach(cfg -> signInCfgMap.put(cfg.getId(), cfg));
            } else {
                resultList.forEach(cfg -> signInCfgMap.put(cfg.getId(), cfg));
            }
            if (signInCfgMap.isEmpty()) {
                log.error("积分大奖签到配置加载失败!");
            }
        }
    }

    /**
     * 检查跨月
     */
    private void checkMonth() {
        LocalDate now = LocalDate.now();
        if (now.getMonthValue() != configDate.getMonthValue()) {
            //重新初始化配置
            initConfig();
            //清除在线玩家的签到数据
            pointsAwardSignInService.clear();
        }
    }

    /**
     * 每日0点调用
     */
    public void daily() {
        checkMonth();
    }

    /**
     * 获取今天的签到配置
     *
     * @return 存在null值返回
     */
    public PointsAwardSigninCfg getTodayConfig() {
        LocalDate now = LocalDate.now();
        int today = now.getDayOfMonth();
        return signInCfgMap.get(today);
    }

    /**
     * 获取签到配置列表
     */
    public List<PointsAwardSigninCfg> getSignInCfgList() {
        return signInCfgMap.values().stream().toList();
    }

    /**
     * 获取当前月最大签到天数
     */
    public int getSignInMaxCount() {
        return signInCfgMap.size();
    }

    /**
     * 获取签到配置
     *
     * @param dayOfMonth 当前天
     * @return 存在null
     */
    public PointsAwardSigninCfg getSignInCfg(int dayOfMonth) {
        return signInCfgMap.get(dayOfMonth);
    }

}
