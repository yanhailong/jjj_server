package com.jjg.game.room.message;

import com.jjg.game.core.pb.NotifyExitRoom;
import com.jjg.game.room.message.resp.NotifyRoomLongTimeNoOperate;

/**
 * @author lm
 * @date 2025/9/19 15:47
 */
public class BaseRoomMessageBuilder {
    /**
     * 构建桌面长时间无操作通知
     */
    public static NotifyRoomLongTimeNoOperate buildNotifyRoomLongTimeNoOperate(int langId) {
        NotifyRoomLongTimeNoOperate notify = new NotifyRoomLongTimeNoOperate();
        notify.langId = langId;
        return notify;
    }

    /**
     * 构建桌面退出房间通知
     */
    public static NotifyExitRoom buildNotifyExitRoom(int langId) {
        NotifyExitRoom notify = new NotifyExitRoom();
        notify.langId = langId;
        return notify;
    }
}
