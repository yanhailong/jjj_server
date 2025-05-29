package com.vegasnight.game.core.data;

import com.vegasnight.game.common.protostuff.PFSession;

/**
 * @author 11
 * @date 2025/5/28 9:32
 */
public class PlayerController {
    public PFSession session;
    public Player player;
    public Object scene;

    public PlayerController(PFSession session, Player player) {
        this.session = session;
        this.player = player;
    }

    public void send(Object msg) {
        if (session!=null) {
            session.send(msg);
        }
    }

    public long playerId() {
        return player.getId();
    }

    public String ipAddress() {
        if (player.getIp() == null || player.getIp().isEmpty()) {
            return session.getAddress().getHost();
        }
        return player.getIp();
    }

    public int roomId() {
        return player.getRoomId();
    }

    public void setRoomId(int roomId) {
        player.setRoomId(roomId);
    }

    public Object getScene() {
        return scene;
    }

    public void setScene(Object scene) {
        this.scene = scene;
    }
}
