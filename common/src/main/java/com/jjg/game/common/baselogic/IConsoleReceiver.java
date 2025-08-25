package com.jjg.game.common.baselogic;

import java.util.List;

/**
 * 接收控制台输入
 *
 * @author 2CL
 */
public interface IConsoleReceiver {

    /**
     * 处理指令
     */
    void doCommand(String command, List<String> params);

    /**
     * 需要处理的指令，不区分大小写
     */
    List<String> needHandleCommands();
}
