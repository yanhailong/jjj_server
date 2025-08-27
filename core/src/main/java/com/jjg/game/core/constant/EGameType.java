package com.jjg.game.core.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.data.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 2CL
 */
public enum EGameType {
    // SLOTS
    DOLLAR_EXPRESS(CoreConst.GameType.DOLLAR_EXPRESS, RoomType.SLOTS, "美元快递"),
    WOODS_MAN(CoreConst.GameType.WOODS_MAN, RoomType.SLOTS, "森林人"),
    SUPER_STAR(CoreConst.GameType.SUPER_STAR, RoomType.SLOTS, "超级明星"),
    BUFFALO_WEALTH(CoreConst.GameType.BUFFALO_WEALTH, RoomType.SLOTS, "野牛财富"),
    FIRE_CAR(CoreConst.GameType.FIRE_CAR, RoomType.SLOTS, "消防车"),
    WOMAN_GOD(CoreConst.GameType.WOMAN_GOD, RoomType.SLOTS, "女武神"),
    MAHJIONG_WIN(CoreConst.GameType.MAHJIONG_WIN, RoomType.SLOTS, "麻将胡了"),
    FORTUNE_CAT(CoreConst.GameType.FORTUNE_CAT, RoomType.SLOTS, "招财猫"),
    PIRATES_CARIBBEAN(CoreConst.GameType.PIRATES_CARIBBEAN, RoomType.SLOTS, "加勒比海盗"),
    GOLD_CITY(CoreConst.GameType.GOLD_CITY, RoomType.SLOTS, "夺宝黄金城"),
    WEALTH_GOD(CoreConst.GameType.WEALTH_GOD, RoomType.SLOTS, "财神"),
    WEST_JOURNEY(CoreConst.GameType.WEST_JOURNEY, RoomType.SLOTS, "西游"),
    CLEOPATRA(CoreConst.GameType.CLEOPATRA, RoomType.SLOTS, "埃及艳后"),


    // TABLE
    RED_BLACK_WAR(CoreConst.GameType.RED_BLACK_WAR, RoomType.BET_ROOM, "红黑大战"),
    LOONG_TIGER_WAR(CoreConst.GameType.LOONG_TIGER_WAR, RoomType.BET_ROOM, "龙虎斗"),
    CATCH_FISH(CoreConst.GameType.CATCH_FISH, RoomType.BET_ROOM, "捕鱼"),
    BIRDS_ANIMAL(CoreConst.GameType.BIRDS_ANIMAL, RoomType.BET_ROOM, "飞禽走兽"),
    LUXURY_CAR_CLUB(CoreConst.GameType.GOOD_CAR_CLUB, RoomType.BET_ROOM, "豪车俱乐部"),
    BACCARAT(CoreConst.GameType.BACCARAT, RoomType.BET_ROOM, "百家乐"),
    DICE_TREASURE(CoreConst.GameType.DICE_BABY, RoomType.BET_ROOM, "骰宝"),
    VIETNAM_DICE(CoreConst.GameType.VIETNAM_SEXY_DISK, RoomType.BET_ROOM, "越南色碟"),
    SIZE_DICE_TREASURE(CoreConst.GameType.SIZE_DICE_BABY, RoomType.BET_ROOM, "大小骰宝"),
    RIVER_ANIMALS(CoreConst.GameType.FISH_SHRIMP_CRAB, RoomType.BET_ROOM, "鱼虾蟹"),
    APPLE_FRUITS(CoreConst.GameType.APPLE_FRUITS, RoomType.BET_ROOM, "苹果机-水果"),
    APPLE_ANIMAL(CoreConst.GameType.APPLE_ANIMAL, RoomType.BET_ROOM, "苹果机-动物"),


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

    /**
     * 不要通过游戏类型获取房间类型，需要通过{@link RoomType#getRoomType(int)}获取
     */
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
}
