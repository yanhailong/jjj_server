package com.jjg.game.core.manager;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.core.constant.AwardCodeType;
import com.jjg.game.core.dao.AwardCodeDao;
import com.jjg.game.core.data.AwardCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.sqids.Sqids;

import java.util.List;
import java.util.Optional;

/**
 * 领奖码生成器
 * 基于雪花算法生成唯一ID，并使用Sqids进行编码混淆
 */
@Component
public class AwardCodeManager {
    private static final Logger log = LoggerFactory.getLogger(AwardCodeManager.class);

    /**
     * Sqids编码使用的字符表
     */
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 领奖码的最小长度
     */
    private static final int MIN_CODE_LENGTH = 10;

    /**
     * 雪花ID编解码器
     */
    private final Sqids sqids;

    /**
     * 雪花ID管理器
     */
    private final SnowflakeManager snowflakeManager;

    /**
     * 领奖码数据访问对象
     */
    private final AwardCodeDao awardCodeDao;

    public AwardCodeManager(AwardCodeDao awardCodeDao, @Lazy SnowflakeManager snowflakeManager) {
        this.awardCodeDao = awardCodeDao;
        this.snowflakeManager = snowflakeManager;
        this.sqids = createSqidsEncoder();
        log.info("领奖码管理器初始化成功");
    }

    /**
     * 创建Sqids编码器
     */
    private Sqids createSqidsEncoder() {
        return Sqids.builder()
                .alphabet(ALPHABET)
                .minLength(MIN_CODE_LENGTH)
                .build();
    }

    /**
     * 将雪花ID编码为混淆字符串
     *
     * @param id 雪花ID
     * @return 编码后的字符串
     */
    public String encode(long id) {
        return sqids.encode(List.of(id));
    }

    /**
     * 将混淆字符串解码为雪花ID
     *
     * @param str 编码后的字符串
     * @return 雪花ID
     */
    public long decode(String str) {
        return sqids.decode(str).getFirst();
    }

    /**
     * 生成唯一的领奖码
     * 生成后会异步保存到数据库
     *
     * @param playerId 玩家ID
     * @param type     领奖码类型
     * @return 编码后的领奖码字符串
     */
    public String generateCode(long playerId, AwardCodeType type) {
        long snowflakeId = snowflakeManager.nextId();
        String encodedCode = encode(snowflakeId);

        AwardCode awardCode = createAwardCode(playerId, type, snowflakeId, encodedCode);
        saveAwardCodeAsync(awardCode, playerId, encodedCode);

        return encodedCode;
    }

    /**
     * 创建领奖码数据对象
     *
     * @param playerId    玩家ID
     * @param type        领奖码类型
     * @param snowflakeId 雪花ID
     * @param encodedCode 编码后的领奖码
     * @return AwardCode对象
     */
    private AwardCode createAwardCode(long playerId, AwardCodeType type, long snowflakeId, String encodedCode) {
        AwardCode awardCode = new AwardCode();
        awardCode.setPlayerId(playerId);
        awardCode.setType(type);
        awardCode.setCreateTime(System.currentTimeMillis());
        awardCode.setSnowflakeId(snowflakeId);
        awardCode.setCode(encodedCode);
        return awardCode;
    }

    /**
     * 异步保存领奖码到数据库
     * 使用虚拟线程进行异步处理
     *
     * @param awardCode 领奖码对象
     * @param playerId  玩家ID
     * @param code      领奖码字符串
     */
    private void saveAwardCodeAsync(AwardCode awardCode, long playerId, String code) {
        PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(playerId, 0, new BaseHandler<String>() {
            @Override
            public void action() {
                try {
                    awardCodeDao.save(awardCode);
                } catch (Exception e) {
                    log.error("保存领奖码失败, playerId={}, code={}, data={}",
                            playerId, code, awardCode, e);
                }
            }
        }.setHandlerParamWithSelf("saveAwardCodeAsync"));
    }

    /**
     * 删除领奖码
     *
     * @param code 领奖码
     * @return true 删除成功
     */
    public boolean deleteCode(String code) {
        long id = decode(code);
        awardCodeDao.deleteById(id);
        return true;
    }

    /**
     * 使用领奖码
     *
     * @param code 领奖码
     * @return true 使用成功 false 使用失败
     */
    public boolean useCode(String code) {
        long id = decode(code);
        Optional<AwardCode> awardCodeOptional = awardCodeDao.findById(id);
        if (awardCodeOptional.isPresent()) {
            AwardCode awardCode = awardCodeOptional.get();
            awardCode.setUseTime(System.currentTimeMillis());
            awardCodeDao.save(awardCode);
            return true;
        }
        return false;
    }

}
