package com.jjg.game.hall.dao;

import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.Player;
import com.jjg.game.hall.data.SharePromoteReward;
import com.jjg.game.hall.sharepromote.SharePromoteRewardService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/8/21 10:01
 */
@Component
public class SharePromoteRewardDao {
    //收藏
    private String sharePromoteTableName = "sharePromote";

    @Autowired
    protected RedisTemplate<String, Player> redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(SharePromoteRewardDao.class);

    /**
     * 添加收藏的游戏
     *
     * @return
     */
    public int getEquipNumSize(List<SharePromoteReward> sharePromoteList, String equipNum) {
        List<SharePromoteReward> newList = sharePromoteList.stream().filter(sharePromoteReward -> {
            return sharePromoteReward.getEquipNum() != null && sharePromoteReward.getEquipNum().equals(equipNum);
        }).toList();
        return newList.size();
    }

    public int getIpSize(List<SharePromoteReward> sharePromoteList, String ip) {
        List<SharePromoteReward> newList = sharePromoteList.stream().filter(sharePromoteReward -> {
            return sharePromoteReward.getIp() != null && sharePromoteReward.getIp().equals(ip);
        }).toList();
        return newList.size();
    }


    public int getRegisterIpSize(List<SharePromoteReward> sharePromoteList, String registerIp) {
        List<SharePromoteReward> newList = sharePromoteList.stream().filter(sharePromoteReward -> {
            return sharePromoteReward.getRegisterIp()!=null && sharePromoteReward.getRegisterIp().equals(registerIp);
        }).toList();
        return newList.size();
    }

    public SharePromoteReward getSharePromote(long playerId) {
        Object object = redisTemplate.opsForHash().get(sharePromoteTableName, playerId);
        if (object == null) {
            return null;
        }
        return (SharePromoteReward) object;
    }

    public List<SharePromoteReward> getSharePromoteList() {
        // 只获取所有值
        List<Object> allValues = redisTemplate.opsForHash().values(sharePromoteTableName);
        // 转换类型处理
        List<SharePromoteReward> sharePromotes = allValues.stream()
                .map(value -> (SharePromoteReward) value)
                .collect(Collectors.toList());
        return sharePromotes;
    }

    /**
     * 添加收藏的游戏
     *
     * @param playerId
     * @return
     */
    public boolean addSharePromote(long playerId, String ip, String equipNum, String registerIp) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.SHARE_PROMOTE);
        String value = globalConfigCfg.getValue();
        String[] split = value.split("\\|");
        String[] split1 = split[2].split("_");
        int playerSizeRequire = Integer.parseInt(split1[0]);

        SharePromoteReward sharePromote = getSharePromote(playerId);
        if (sharePromote == null) {
            sharePromote = new SharePromoteReward();
            sharePromote.setPlayerId(playerId);
            sharePromote.setEquipNum(equipNum);
            sharePromote.setRegisterIp(registerIp);
            sharePromote.setIp(ip);
            sharePromote.setNum(1);
            redisTemplate.opsForHash().put(sharePromoteTableName, playerId, sharePromote);
            return true;
        } else if (sharePromote.getNum() < playerSizeRequire) {
            sharePromote = new SharePromoteReward();
            sharePromote.setPlayerId(playerId);
            sharePromote.setEquipNum(equipNum);
            sharePromote.setRegisterIp(registerIp);
            sharePromote.setIp(ip);
            sharePromote.setNum(sharePromote.getNum() + 1);
            redisTemplate.opsForHash().put(sharePromoteTableName, playerId, sharePromote);
            return true;
        }
        return false;
    }


    public void clear() {
        redisTemplate.delete(sharePromoteTableName);
    }

    public boolean judge(long playId, String registerIp, String ip, String equipNum) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.SHARE_PROMOTE);
        String value = globalConfigCfg.getValue();
        String[] split = value.split("\\|");
        String[] split1 = split[2].split("_");
        int playerSizeRequire = Integer.parseInt(split1[0]);
        int equipNumSizeRequire = Integer.parseInt(split1[1]);
        int ipSizeRequire = Integer.parseInt(split1[2]);
        int registerIpSizeRequire = Integer.parseInt(split1[3]);

        SharePromoteReward sharePromote = getSharePromote(playId);
        if (sharePromote == null) {
            List<SharePromoteReward> sharePromoteList = getSharePromoteList();
            int equipNumSize = getEquipNumSize(sharePromoteList, equipNum);
            int ipSize = getIpSize(sharePromoteList, ip);
            int registerIpSize = getIpSize(sharePromoteList, registerIp);
            if (equipNumSize >= equipNumSizeRequire) {
                log.info("分享上线 已领取设备:{}, 配置上线:{}", equipNumSize, equipNumSizeRequire);
            }
            if (ipSize >= ipSizeRequire) {
                log.info("分享上线 已领取ip:{}, 配置ip:{}", ipSize, ipSizeRequire);
            }
            if (registerIpSize >= registerIpSizeRequire) {
                log.info("分享上线 已领取注册ip:{}, 配置注册ip:{}", registerIpSize, registerIpSizeRequire);
            }
            return equipNumSize < equipNumSizeRequire && ipSize < ipSizeRequire && registerIpSize < registerIpSizeRequire;
        } else if (sharePromote.getNum() < playerSizeRequire) {
            int equipNumSize = getEquipNumSize(getSharePromoteList(), equipNum);
            int ipSize = getIpSize(getSharePromoteList(), ip);
            int registerIpSize = getIpSize(getSharePromoteList(), registerIp);
            if (equipNumSize >= equipNumSizeRequire) {
                log.info("分享上线 已领取设备:{}, 配置上线:{}", equipNumSize, equipNumSizeRequire);
            }
            if (ipSize >= ipSizeRequire) {
                log.info("分享上线 已领取ip:{}, 配置ip:{}", ipSize, ipSizeRequire);
            }
            if (registerIpSize >= registerIpSizeRequire) {
                log.info("分享上线 已领取注册ip:{}, 配置注册ip:{}", registerIpSize, registerIpSizeRequire);
            }
            return equipNumSize < equipNumSizeRequire && ipSize < ipSizeRequire && registerIpSize < registerIpSizeRequire;
        }
        return false;
    }
}
