package com.jjg.game.core.base.item;

import com.jjg.game.core.dao.PlayerAvatarDao;
import com.jjg.game.core.data.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 皮肤道具使用类
 *
 * @author 2CL
 */
@Component
public class SkinItemUse implements IItemUseInterface {

    private static final Logger log = LoggerFactory.getLogger(SkinItemUse.class);
    @Autowired
    private PlayerAvatarDao playerAvatarDao;

    @Override
    public void autoUse(long playerId, List<Item> itemList) {
        List<Integer> avatarIdList = new ArrayList<>();
        for (Item item : itemList) {
            avatarIdList.add(item.getId());
        }
        // 添加皮肤
        if (!playerAvatarDao.addByType(playerId, avatarIdList)) {
            log.error("自动添加皮肤失败，  playerId={} cfgId={}", playerId, avatarIdList);
        }
    }
}
