package com.jjg.game.core.listener;

import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.ChannelUserInfo;
import com.jjg.game.core.data.LoginType;
import com.jjg.game.core.data.Player;

public interface BindThirdAccountListener {
    void bind(Player player, Account account, LoginType loginType, ChannelUserInfo channelUserInfo);
}
