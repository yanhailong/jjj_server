package com.jjg.game.core.data;

import com.jjg.game.common.data.AbsReferenceObject;
import com.jjg.game.common.protostuff.PFSession;

/**
 * @author 11
 * @date 2025/5/28 9:32
 */
public class PlayerController extends AbsReferenceObject {
    private PFSession session;
    private Player player;
    private Object scene;
    //子场景
    private Object subScene;

    public PlayerController(PFSession session, Player player) {
        this.session = session;
        this.player = player;
    }

    public void send(Object msg) {
        if (session != null) {
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

    public Object getScene() {
        return scene;
    }

    public void setScene(Object scene) {
        this.scene = scene;
    }

    public long roomId() {
        return player.getRoomId();
    }

    public boolean isRobotPlayer(){
        return player instanceof RobotPlayer;
    }

    public PFSession getSession() {
        return session;
    }

    public void setSession(PFSession session) {
        this.session = session;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Object getSubScene() {
        return subScene;
    }

    public void setSubScene(Object subScene) {
        this.subScene = subScene;
    }

    public boolean isOnline() {
        if (session == null) {
            return false;
        }
        if (session.getConnect() == null) {
            return false;
        }
        return session.getConnect().isActive();
    }
}
