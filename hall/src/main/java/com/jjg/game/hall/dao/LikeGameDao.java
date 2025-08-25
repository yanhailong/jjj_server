package com.jjg.game.hall.dao;

import com.jjg.game.core.data.Player;
import com.jjg.game.hall.data.LikeGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * @author 11
 * @date 2025/8/21 10:01
 */
@Component
public class LikeGameDao {
    //收藏
    private String likeGameTableName = "likeGame";

    @Autowired
    protected RedisTemplate<String, Player> redisTemplate;

    /**
     * 添加收藏的游戏
     * @param playerId
     * @param gameTypes
     * @return
     */
    public TreeSet<Integer> addLikeGame(long playerId, List<Integer> gameTypes){
        LikeGame likeGame = getLikeGame(playerId);
        if(likeGame == null){
            likeGame = new LikeGame();
            likeGame.setPlayerId(playerId);
        }

        if(gameTypes == null || gameTypes.isEmpty()){
            return likeGame.getGameTypeSet();
        }

        for(int gameType : gameTypes){
            likeGame.addLikeGame(gameType);
        }
        save(likeGame);
        return likeGame.getGameTypeSet();
    }

    /**
     * 添加收藏的游戏
     * @param playerId
     * @param gameTypes
     * @return
     */
    public TreeSet<Integer> calcelLikeGame(long playerId, List<Integer> gameTypes){
        LikeGame likeGame = getLikeGame(playerId);
        if(likeGame == null){
            return null;
        }

        if(gameTypes == null || gameTypes.isEmpty()){
            return likeGame.getGameTypeSet();
        }

        for(int gameType : gameTypes){
            likeGame.getGameTypeSet().remove(gameType);
        }
        save(likeGame);
        return likeGame.getGameTypeSet();
    }

    /**
     * 获取玩家收藏列表
     * @param playerId
     * @return
     */
    public LikeGame getLikeGame(long playerId){
        HashOperations<String, String, LikeGame> operations = redisTemplate.opsForHash();
        return operations.get(likeGameTableName, playerId);
    }

    /**
     * 获取玩家收藏的游戏列表
     * @param playerId
     * @return
     */
    public List<Integer> getLikeGames(long playerId){
        LikeGame likeGame = getLikeGame(playerId);
        if(likeGame == null || likeGame.getGameTypeSet() == null || likeGame.getGameTypeSet().isEmpty()){
            return null;
        }
        return new ArrayList<>(likeGame.getGameTypeSet());
    }

    public void save(LikeGame likeGame){
        redisTemplate.opsForHash().put(likeGameTableName, likeGame.getPlayerId(), likeGame);
    }
}
