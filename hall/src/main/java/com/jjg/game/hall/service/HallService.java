package com.jjg.game.hall.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.GameStatus;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.dao.BindDao;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.WarehouseCfg;
import com.jjg.game.hall.sample.bean.GameListCfg;
import com.jjg.game.hall.utils.HallTool;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/18 14:56
 */
@Component
public class HallService implements ConfigExcelChangeListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GameStatusService gameStatusService;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private BindDao bindDao;

    private Map<Integer, List<WareHouseConfigInfo>> wareHouseConfigMap = new HashMap<>();
    //游戏类型->游戏状态
    private Map<Integer, GameStatus> gameStatusesMap;

    public Map<Integer, GameStatus> getGameStatusesMap() {
        return gameStatusesMap;
    }

    public void loadGameStatuses(List<GameStatus> gameStatuses) {
        if (Objects.nonNull(gameStatuses)) {
            gameStatusesMap = gameStatuses.stream().collect(Collectors.toMap(GameStatus::gameId, gs -> gs));
        }
    }

    public void refreshGameStatuses() {
        loadGameStatuses(gameStatusService.getAllGameStatus());
    }

    public void init() {
        initWareHouseConfigData();
        loadGameStatuses(gameStatusService.getAllGameStatus());
    }

    public List<WareHouseConfigInfo> getWareHouseConfigByGameType(int gameType) {
        return wareHouseConfigMap.get(gameType);
    }

    public boolean canJoinGame(int gameType) {
        GameStatus gameStatus = gameStatusesMap.get(gameType);
        if (Objects.nonNull(gameStatus)) {
            return gameStatus.open() == 1 && gameStatus.status() == 1;
        }
        GameListCfg gameListCfg = GameDataManager.getGameListCfg(gameType);
        if (Objects.nonNull(gameListCfg)) {
            return gameListCfg.getStatus() == HallCode.GAME_STATUS_OPEN;
        }
        return false;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(WarehouseCfg.EXCEL_NAME, this::initWareHouseConfigData);
    }

    private void initWareHouseConfigData() {
        Map<Integer, List<WareHouseConfigInfo>> tempwareHouseConfigMap = new HashMap<>();

        for (WarehouseCfg c : GameDataManager.getWarehouseCfgList()) {
            List<WareHouseConfigInfo> tempList = tempwareHouseConfigMap.computeIfAbsent(c.getGameID(),
                k -> new ArrayList<>());
            if(c.getRoomType() < 10){
                WareHouseConfigInfo info = new WareHouseConfigInfo();
                info.wareId = c.getId();
                info.limitGoldMin = c.getEnterLimit();
                info.limitVipMin = c.getVipLvLimit();
                info.betShow = c.getBetShow();
                tempList.add(info);
            }
        }

        //根据场次id，从小到大排序
        tempwareHouseConfigMap.replaceAll((key, list) ->
                list.stream()
                        .sorted(Comparator.comparingInt(wh -> wh.wareId))
                        .collect(Collectors.toList())
        );

        this.wareHouseConfigMap = tempwareHouseConfigMap;
    }

    /**
     * 绑定手机号
     * @param playerId
     * @param data
     * @return
     */
    public CommonResult<Integer> bindPhone(long playerId, String data){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        if(StringUtils.isEmpty(data)){
            result.code = Code.PARAM_ERROR;
            log.debug("参数为空,获取绑定手机验证码失败 playerId = {},phone = {}", playerId,data);
            return result;
        }

        int now = TimeHelper.nowInt();
        result = bindDao.verCodeIdleTime(playerId, HallConstant.VerCode.TYPE_BIND_PHONE);
        if(now <= result.data){
            log.debug("操作频繁，请稍后再试获取绑定手机验证码 playerId = {},data = {}", playerId,data);
            result.code = Code.REPEAT_OP;
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            result.code = Code.NOT_FOUND;
            log.debug("没有找到玩家账号信息，绑定手机号失败 playerId = {},phone = {}", playerId,data);
            return result;
        }

        if(data.equals(account.getPhoneNumber())) {
            result.code = Code.REPEAT_OP;
            log.debug("玩家当前绑定手机号与新手机号一致，绑定手机号失败 playerId = {},oldPhone = {},newPhone = {}", playerId,account.getPhoneNumber(),data);
            return result;
        }

        int verCode = RandomUtils.randomNum(HallConstant.VerCode.CODE_MIN,HallConstant.VerCode.CODE_MAX);
        bindDao.addPhoneVerCode(playerId, data,verCode);
        result.data = verCode;
        return result;
    }

    /**
     * 绑定手机号
     * @param playerId
     * @param data
     * @return
     */
    public CommonResult<Integer> bindEmail(long playerId,String data){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        if(StringUtils.isEmpty(data)){
            result.code = Code.PARAM_ERROR;
            log.debug("参数为空,获取绑定邮箱验证码失败 playerId = {},email = {}", playerId,data);
            return result;
        }

        if(!HallTool.checkEmail(data)) {
            result.code = Code.PARAM_ERROR;
            log.debug("邮箱格式不正确,获取绑定邮箱验证码失败 playerId = {},email = {}", playerId,data);
            return result;
        }

        int now = TimeHelper.nowInt();
        result = bindDao.verCodeIdleTime(playerId, HallConstant.VerCode.TYPE_BIND_EMAIL);
        if(now <= result.data){
            log.debug("操作频繁，请稍后再试获取绑定邮箱验证码 playerId = {},data = {}", playerId,data);
            result.code = Code.REPEAT_OP;
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            result.code = Code.NOT_FOUND;
            log.debug("没有找到玩家账号信息，获取绑定邮箱验证码失败 playerId = {},email = {}", playerId,data);
            return result;
        }

        if(data.equals(account.getEmail())) {
            result.code = Code.REPEAT_OP;
            log.debug("玩家当前绑定邮箱与新邮箱一致，获取绑定邮箱验证码失败 playerId = {},oldPhone = {},newPhone = {}", playerId,account.getPhoneNumber(),data);
            return result;
        }

        int verCode = RandomUtils.randomNum(HallConstant.VerCode.CODE_MIN,HallConstant.VerCode.CODE_MAX);
        bindDao.addEmailVerCode(playerId, data,verCode);
        result.data = verCode;
        return result;
    }

    public CommonResult<String> comfirmVerCode(long playerId,int verCodeType, int verCode){
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);
        if(verCode < HallConstant.VerCode.CODE_MIN || verCode > HallConstant.VerCode.CODE_MAX) {
            result.code = Code.PARAM_ERROR;
            log.debug("验证码不在范围内，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId,verCodeType,verCode);
            return result;
        }

        CommonResult<String> verResult = bindDao.verifyVerCode(playerId, verCodeType, verCode);
        if(!verResult.success()){
            result.code = verResult.code;
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            result.code = Code.NOT_FOUND;
            log.debug("没有找到玩家账号信息，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId,verCodeType,verCode);
            return result;
        }

        boolean update = false;
        if(verCodeType == HallConstant.VerCode.TYPE_BIND_PHONE){
            update = accountDao.updatePhoneNumber(playerId, verResult.data);
        }else if(verCodeType == HallConstant.VerCode.TYPE_BIND_EMAIL){
            update = accountDao.updateEmail(playerId, verResult.data);
        }

        if(!update){
            result.code = Code.FAIL;
            log.debug("更新到数据库失败，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId,verCodeType,verCode);
            return result;
        }

        //删除验证码
        bindDao.delVerCode(playerId, verCodeType);
        result.data = verResult.data;
        return result;
    }
}
