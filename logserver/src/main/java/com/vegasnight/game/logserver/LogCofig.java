package com.vegasnight.game.logserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/27 17:54
 */
@Component
@ConfigurationProperties(prefix = "logconfig")
public class LogCofig {
    //filebeat log.json文件地址
    private String filebeatPath;
    //游戏日志文件前缀
    private String gameLogPrefix = "gamelog";
    //清理超过多少小时的日志文件
    private int clearExpireHours = 6;

    public String getFilebeatPath() {
        return filebeatPath;
    }

    public void setFilebeatPath(String filebeatPath) {
        this.filebeatPath = filebeatPath;
    }

    public String getGameLogPrefix() {
        if(gameLogPrefix == null || gameLogPrefix.length() < 1){
            return "gamelog";
        }
        return gameLogPrefix;
    }

    public void setGameLogPrefix(String gameLogPrefix) {
        this.gameLogPrefix = gameLogPrefix;
    }

    public int getClearExpireHours() {
        return clearExpireHours;
    }

    public void setClearExpireHours(int clearExpireHours) {
        this.clearExpireHours = clearExpireHours;
    }
}
