package com.jjg.game.gm.dao;

import cn.hutool.core.io.file.FileWriter;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightResultLib;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneResultLib;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneResultLib;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitResultLib;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import com.jjg.game.slots.game.steamAge.data.SteamAgeResultLib;
import com.jjg.game.slots.game.superstar.data.SuperStarResultLib;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.game.wealthbank.data.WealthBankResultLib;
import com.jjg.game.slots.game.wealthgod.data.WealthGodResultLib;
import com.jjg.game.slots.utils.LZ4CompressionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author 11
 * @date 2025/11/4 10:24
 */
@Repository
public class SlotsLibDao extends AbstractResultLibDao<SlotsResultLib> {

    // 游戏类型到ResultLib子类的映射
    private static final Map<Integer, Class<? extends SlotsResultLib>> GAME_TYPE_TO_CLASS_MAP = new HashMap<>();

    static {
        // 初始化映射表
        // 美元快递
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.DOLLAR_EXPRESS, DollarExpressResultLib.class);
        // 超级明星
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.SUPER_STAR, SuperStarResultLib.class);
        // 麻将胡了
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.MAHJIONG_WIN, MahjiongWinResultLib.class);
        // 财神
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.WEALTH_GOD, WealthGodResultLib.class);
        // 埃及艳后
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.CLEOPATRA, CleopatraResultLib.class);
        // 圣诞狂欢夜
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.CHRISTMAS_PARTY, ChristmasBashNightResultLib.class);
        // 篮球巨星
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.BASKETBALL_STAR, BasketballSuperstarResultLib.class);
        // 杰克船长
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.CAPTAIN_JACK, CaptainJackResultLib.class);
        // 雷神
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.THOR, ThorResultLib.class);
        // 蒸汽时代
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.STEAM_AGE, SteamAgeResultLib.class);
        // 财富银行
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.WEALTH_BANK, WealthBankResultLib.class);
        // 寒冰王座
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.FROZEN_THRONE, FrozenThroneResultLib.class);
        // 神马飞扬
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.PEGASUS_UNBRIDLE, PegasusUnbridleResultLib.class);
        // 金蛇招财
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.GOLD_SNAKE_FORTUNE, GoldSnakeFortuneResultLib.class);
        // 金钱兔
        GAME_TYPE_TO_CLASS_MAP.put(CoreConst.GameType.MONEY_RABBIT, MoneyRabbitResultLib.class);

    }

    public SlotsLibDao() {
        super(SlotsResultLib.class);
    }

    /**
     * 根据游戏类型获取对应的ResultLib类
     */
    private Class<? extends SlotsResultLib> getResultLibClassByGameType(int gameType) {
        Class<? extends SlotsResultLib> clazz = GAME_TYPE_TO_CLASS_MAP.get(gameType);
        if (clazz == null) {
            log.warn("未找到游戏类型 {} 对应的ResultLib类，使用默认的SlotsResultLib.class", gameType);
            return SlotsResultLib.class;
        }
        return clazz;
    }

    /**
     * 使用RedisTemplate扫描所有generateLock
     */
    public Set<Integer> scanAllGenerateLocks() {
        Set<Integer> lockedGameTypes = new HashSet<>();

        // 使用scan命令避免阻塞
        ScanOptions options = ScanOptions.scanOptions()
                .match(generateLock + ":*")
                .count(100) // 每次扫描100个
                .build();

        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options);

        while (cursor.hasNext()) {
            String key = new String(cursor.next());
            String[] parts = key.split(":");
            if (parts.length >= 2) {
                try {
                    int gameType = Integer.parseInt(parts[1]);
                    lockedGameTypes.add(gameType);
                } catch (NumberFormatException e) {
                    log.warn("解析gameType失败，键名: {}", key);
                }
            }
        }

        return lockedGameTypes;
    }

    /**
     * 导出结果库
     *
     * @param gameType
     * @return
     */
    public int exportGameResultLib(int gameType) {
        // 获取该游戏类型对应的ResultLib类
        Class<? extends SlotsResultLib> resultLibClass = getResultLibClassByGameType(gameType);
        if (resultLibClass == null) {
            log.warn("获取 resultLibClass为空 gameType = {}", gameType);
            return Code.NOT_FOUND;
        }

        CommonResult<Set<String>> keysResult = getResultLibKeys(gameType);
        if (!keysResult.success()) {
            log.warn("获取 resultLibClass为空 gameType = {}", gameType);
            return keysResult.code;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String format = LocalDateTime.now().format(dtf);
        File file = new File("lib_" + gameType + "_" + format + ".xlsx");
        BigExcelWriter fileWriter = null;
        int totalLibs = 0;
        int resultCode = Code.FAIL;

        try {
            fileWriter = new BigExcelWriter(file);

            // 写入表头
            List<String> header = Arrays.asList("id", "游戏类型", "类型", "滚轴模式", "图标", "总倍数", "中奖线信息", "修改格子后的信息", "小游戏奖励信息", "其他", "命令");
            fileWriter.writeRow(header);

            for (String key : keysResult.data) {
                Cursor<byte[]> cursor = null;
                try {
                    // 分批获取
                    ScanOptions options = ScanOptions.scanOptions().count(1000).build();
                    cursor = (Cursor<byte[]>) redisTemplate.execute((RedisCallback<Object>) connection ->
                            connection.sScan(key.getBytes(), options)
                    );

                    while (cursor.hasNext()) {
                        byte[] next = cursor.next();

                        // 反序列化结果库对象
                        SlotsResultLib lib = selfDeserializeResultLib(next, resultLibClass);
                        if (lib != null) {
                            fileWriter.writeRow(getRowValue(lib, next));
                            totalLibs++;
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }

            // 所有数据写入完成
            resultCode = Code.SUCCESS;
        } catch (Exception e) {
            log.error("导出结果库失败", e);
            resultCode = Code.FAIL;
        } finally {
            // 确保文件写入器关闭
            if (fileWriter != null) {
                fileWriter.close();
                log.warn("已写入完毕 fileName = {},totalLibs = {}", file.getName(), totalLibs);
            }
        }
        return resultCode;
    }

    private List<Object> getRowValue(SlotsResultLib lib, byte[] data) {
        List<Object> list = new ArrayList<>();
        JSONObject json = JSON.parseObject(JSON.toJSONString(lib));
        list.add(json.remove("id"));
        list.add(json.remove("gameType"));
        list.add(json.remove("libTypeSet"));
        list.add(json.remove("rollerMode"));
        list.add(json.remove("iconArr"));
        list.add(json.remove("times"));
        list.add(json.remove("awardLineInfoList"));
        list.add(json.remove("specialGirdInfoList"));
        list.add(json.remove("specialAuxiliaryInfoList"));
        list.add(json.toJSONString());
        list.add(byteArrayToString(data));
        return list;
    }

    /**
     * 反序列化结果库数据
     */
    private SlotsResultLib selfDeserializeResultLib(byte[] compressedData, Class<? extends SlotsResultLib> cla) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(compressedData);
            int originalLength = buffer.getInt();  // 读取原始长度

            byte[] data = new byte[compressedData.length - 4];
            buffer.get(data);

            byte[] decompressedData = LZ4CompressionUtil.decompressFast(data, originalLength);
            // 使用指定的类进行反序列化
            return ProtostuffUtil.deserialize(decompressedData, cla);
        } catch (Exception e) {
            log.error("反序列化结果库数据失败, class: {}", cla.getName(), e);
            return null;
        }
    }

    /**
     * 获取该游戏类型结果库的所有key
     *
     * @param gameType
     * @return
     */
    private CommonResult<Set<String>> getResultLibKeys(int gameType) {
        CommonResult<Set<String>> result = new CommonResult<>(Code.SUCCESS);
        String libName = (String) this.redisTemplate.opsForHash().get(slotsCurrentRedisResultLib, gameType);
        if (StringUtils.isBlank(libName)) {
            log.warn("该游戏无可用的结果库 gameType = {}", gameType);
            result.code = Code.NOT_FOUND;
            return result;
        }

        // 扫描该游戏类型下的所有结果库键
        String pattern = libName + gameType + ":*";
        Set<String> keys = new HashSet<>();

        Cursor<String> cursor = null;
        try {
            cursor = redisTemplate.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)  // 每次扫描数量
                            .build()
            );

            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (keys.isEmpty()) {
            log.warn("未找到匹配的结果库键，pattern: {}", pattern);
            result.code = Code.NOT_FOUND;
            return result;
        }

        result.data = keys;
        return result;
    }

    private String byteArrayToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int last = data.length - 1;
        for (int i = 0; i < data.length; i++) {
            sb.append(data[i]);
            if (i != last) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
