package com.jjg.game.core.base.item;

import cn.hutool.core.util.EnumUtil;
import com.jjg.game.core.dao.PlayerAvatarDao;
import com.jjg.game.core.data.AvatarType;
import com.jjg.game.core.data.Item;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AvatarCfg;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 皮肤道具使用类
 *
 * @author 2CL
 */
@Component
public class SkinItemUse implements IItemUseInterface {

    private final Logger log = LoggerFactory.getLogger(SkinItemUse.class);
    private final PlayerAvatarDao playerAvatarDao;

    public SkinItemUse(PlayerAvatarDao playerAvatarDao) {
        this.playerAvatarDao = playerAvatarDao;
    }

    @Override
    public int autoUse(long playerId, Item item, ItemCfg itemCfg) {
        try {
            if (item.getItemCount() < 1) {
                return 0;
            }
            AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(itemCfg.getAvatarID());
            if (avatarCfg == null) {
                log.error("自动添加皮肤失败，皮肤配置不存在，  playerId={} cfgId={}", playerId, itemCfg.getAvatarID());
                return 0;
            }
            AvatarType type = EnumUtil.getBy(AvatarType.class, t -> t.getType() == avatarCfg.getResourceType());
            if (type == null) {
                log.error("自动添加皮肤失败 不存在皮肤类型，  playerId={} cfgId={}", playerId, itemCfg.getAvatarID());
                return 0;
            }
            boolean add = playerAvatarDao.addByType(playerId, type, avatarCfg.getId());
            if (!add) {
                log.error("自动添加皮肤失败，  playerId={} cfgId={}", playerId, avatarCfg.getId());
                return 0;
            }
            return 1;
        } catch (Exception e) {
            log.error("自动添加皮肤异常 ,playerId={} cfgId={}", playerId, itemCfg.getAvatarID(), e);
        }
        return 0;
    }
}
