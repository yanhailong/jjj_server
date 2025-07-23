package com.jjg.game.core.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.data.RoomType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 2CL
 */
public enum EGameType {
    // SLOTS
    DOLLAR_EXPRESS(CoreConst.GameType.DOLLAR_EXPRESS, RoomType.TEAM, "美元快递"),
    WOODS_MAN(CoreConst.GameType.WOODS_MAN, RoomType.TEAM, "森林人"),
    SUPER_STAR(CoreConst.GameType.SUPER_STAR, RoomType.TEAM, "超级明星"),
    BUFFALO_WEALTH(CoreConst.GameType.BUFFALO_WEALTH, RoomType.TEAM, "野牛财富"),
    FIRE_CAR(CoreConst.GameType.FIRE_CAR, RoomType.TEAM, "消防车"),
    WOMAN_GOD(CoreConst.GameType.WOMAN_GOD, RoomType.TEAM, "女武神"),
    MAHJIONG_WIN(CoreConst.GameType.MAHJIONG_WIN, RoomType.TEAM, "麻将胡了"),
    FORTUNE_CAT(CoreConst.GameType.FORTUNE_CAT, RoomType.TEAM, "招财猫"),
    PIRATES_CARIBBEAN(CoreConst.GameType.PIRATES_CARIBBEAN, RoomType.TEAM, "加勒比海盗"),
    GOLD_CITY(CoreConst.GameType.GOLD_CITY, RoomType.TEAM, "夺宝黄金城"),


    // TABLE
    RED_BLACK_WAR(CoreConst.GameType.RED_BLACK_WAR, RoomType.BET_ROOM, "红黑大战"),
    LOONG_TIGER_WAR(CoreConst.GameType.LOONG_TIGER_WAR, RoomType.BET_ROOM, "龙虎斗"),
    CATCH_FISH(CoreConst.GameType.CATCH_FISH, RoomType.BET_ROOM, "捕鱼"),
    BIRDS_ANIMAL(CoreConst.GameType.BIRDS_ANIMAL, RoomType.BET_ROOM, "飞禽走兽"),
    GOOD_CAR_CLUB(CoreConst.GameType.GOOD_CAR_CLUB, RoomType.BET_ROOM, "豪车俱乐部"),
    BACCARAT(CoreConst.GameType.BACCARAT, RoomType.BET_ROOM, "百家乐"),
    DICE_BABY(CoreConst.GameType.DICE_BABY, RoomType.BET_ROOM, "骰宝"),
    VIETNAM_SEXY_DISK(CoreConst.GameType.VIETNAM_SEXY_DISK, RoomType.BET_ROOM, "越南色碟"),
    SIZE_DICE_BABY(CoreConst.GameType.SIZE_DICE_BABY, RoomType.BET_ROOM, "大小骰宝"),
    FISH_SHRIMP_CRAB(CoreConst.GameType.FISH_SHRIMP_CRAB, RoomType.BET_ROOM, "鱼虾蟹"),
    APPLE_FRUITS(CoreConst.GameType.APPLE_FRUITS, RoomType.TEAM, "苹果机-水果"),
    APPLE_ANIMAL(CoreConst.GameType.APPLE_ANIMAL, RoomType.TEAM, "苹果机-动物"),


    // POKER
    BLACK_JACK(CoreConst.GameType.BLACK_JACK, RoomType.POKER_ROOM, "21点"),
    TEXAS(CoreConst.GameType.TEXAS, RoomType.POKER_ROOM, "德州"),
    VEGAS_THREE(CoreConst.GameType.VEGAS_THREE, RoomType.POKER_ROOM, "拉斯维加斯拼三张"),
    ;
    // 游戏类型ID
    final int gameTypeId;
    // 游戏描述
    final String gameDesc;
    // 游戏对应的房间类型
    final RoomType roomType;
    // 房间游戏类型ID Set
    static final ConcurrentHashMap<Integer, EGameType> GAME_TYPE_ID_SET = new ConcurrentHashMap<>();

    EGameType(int gameTypeId, RoomType roomType, String gameDesc) {
        this.gameTypeId = gameTypeId;
        this.gameDesc = gameDesc;
        this.roomType = roomType;
    }

    public int getGameTypeId() {
        return gameTypeId;
    }

    public String getGameDesc() {
        return gameDesc;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    /**
     * 通过游戏配置ID获取游戏类型枚举
     */
    public static EGameType getGameByTypeId(int gameTypeId) {
        EGameType gameType = null;
        if (GAME_TYPE_ID_SET.isEmpty()) {
            for (EGameType value : values()) {
                if (value.getGameTypeId() == gameTypeId) {
                    gameType = value;
                }
                GAME_TYPE_ID_SET.put(value.getGameTypeId(), value);
            }
        } else {
            gameType = GAME_TYPE_ID_SET.get(gameTypeId);
        }
        return gameType;
    }

    /**
     * 通过房间类型获取所有的游戏
     */
    public static List<EGameType> getGameTypesSetByRoomType(RoomType roomType) {
        List<EGameType> set = new ArrayList<>();
        for (EGameType value : values()) {
            if (value.getRoomType().equals(roomType)) {
                set.add(value);
            }
        }
        return set;
    }
}
