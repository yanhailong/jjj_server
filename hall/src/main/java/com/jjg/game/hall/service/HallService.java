package com.jjg.game.hall.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.PlayerAvatarDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.dao.BindDao;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.utils.HallTool;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
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
    @Autowired
    private PlayerAvatarDao playerAvatarDao;
    @Autowired
    private HallPlayerService hallPlayerService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private MailService mailService;

    private Map<Integer, List<WareHouseConfigInfo>> wareHouseConfigMap = new HashMap<>();
    //游戏类型->游戏状态
    private Map<Integer, GameStatus> gameStatusesMap;

    public Map<Integer, GameStatus> getGameStatusesMap() {
        return gameStatusesMap;
    }

    //默认头像id
    private int defaultHeadImgId = 0;
    //默认头像框id
    private int defaultHeadFrameId = 0;
    //默认国旗id
    private int defaultNationalId = 0;
    //默认称号id
    private int defaultTitlelId = 0;

    public void loadGameStatuses(List<GameStatus> gameStatuses) {
        if (Objects.nonNull(gameStatuses)) {
            gameStatusesMap = gameStatuses.stream().collect(Collectors.toMap(GameStatus::gameId, gs -> gs));
        }
    }

    public void refreshGameStatuses() {
        loadGameStatuses(gameStatusService.getAllGameStatus());
    }

    public void init() {
        //缓存倍场的配置信息
        initWareHouseConfigData();
        //缓存每个游戏的状态
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

    /**
     * 确认验证码
     * @param playerId
     * @param verCodeType
     * @param verCode
     * @return
     */
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

    /**
     * 获取所有头像信息
     * @param playerId
     * @return
     */
    public PlayerAvatar allAvatar(long playerId){
        return playerAvatarDao.getPlayerAvatar(playerId);
    }

    /**
     * 切换头像
     * @param playerId
     * @param id
     * @return
     */
    public CommonResult<Player> selectAvatar(long playerId,int id){
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
        if(avatarCfg == null){
            log.debug("未在头像配置表中找到该配置 id = {}",id);
            result.code = Code.NOT_FOUND;
            return result;
        }

        //todo 要判断该id是不是默认的id
//        if(id == defauleId){
//
//        }

        //检查玩家是否拥有该id
        boolean has = false;
        if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_AVATAR){
            has = playerAvatarDao.hasAvatar(playerId,id);
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_FRAME){
            has = playerAvatarDao.hasFrame(playerId,id);
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_TITLE){
            has = playerAvatarDao.hasTitle(playerId,id);
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_NATIONAL){
            has = true;
        }

        if(!has){
           log.debug("玩家没有该头像id = {},type = {}",id,avatarCfg.getResourceType());
            result.code = Code.NOT_FOUND;
            return result;
        }

        Player player = null;
        if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_AVATAR){
            player = hallPlayerService.doSave(playerId, p -> {
                p.setHeadImgId(id);
            });
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_FRAME){
            player = hallPlayerService.doSave(playerId, p -> {
                p.setHeadFrameId(id);
            });
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_TITLE){
            player = hallPlayerService.doSave(playerId, p -> {
                p.setTitleId(id);
            });
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_NATIONAL){
            player = hallPlayerService.doSave(playerId, p -> {
                p.setNationalId(id);
            });
        }

        result.data = player;
        log.info("选择头像资源成功 playerId = {},id = {},type = {}", playerId,id,avatarCfg.getResourceType());
        return result;
    }

    /**
     * 添加头像等信息
     * @param playerId
     * @param id
     */
    public void addPlayerAvatar(long playerId,int id){
        AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
        if(avatarCfg == null){
            log.debug("未在头像配置表中找到该配置 id = {}",id);
            return;
        }

        boolean add = false;
        if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_AVATAR){
            add = playerAvatarDao.addAvatar(playerId,id);
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_FRAME){
            add = playerAvatarDao.addFrame(playerId,id);
        }else if(avatarCfg.getResourceType() == HallConstant.Avatar.TYPE_TITLE){
            add = playerAvatarDao.addTitle(playerId,id);
        }

        if(!add){
            log.debug("添加头像信息失败 playerId = {},cfgId = {}",playerId,avatarCfg.getId());
            return;
        }
        log.info("添加头像信息成功 playerId = {},cfgId = {}",playerId,avatarCfg.getId());
    }

    /**
     * 保存默认的头像信息
     * @param playerId
     */
    public void saveDefaultAvatar(long playerId){
        try{
            PlayerAvatar playerAvatar = new PlayerAvatar();
            playerAvatar.setPlayerId(playerId);
            playerAvatar.addAvatar(this.defaultHeadImgId);
            playerAvatar.addFrame(this.defaultHeadFrameId);
            playerAvatar.addTitle(this.defaultTitlelId);
            this.playerAvatarDao.save(playerAvatar);
            log.info("保存默认的头像信息成功  playerId = {}",playerId);
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 获取玩家背包
     * @param playerId
     * @return
     */
    public PlayerPack getPlayerPack(long playerId){
        return this.playerPackService.getFromAllDB(playerId);
    }

    /**
     * 使用道具
     * @param playerId
     * @param itemId
     */
    public CommonResult<Integer> useItem(long playerId,int itemId){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        try{
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if(itemCfg == null){
                result.code = Code.NOT_FOUND;
                log.debug("未找到该道具配置，使用道具失败 playerId = {},itemId = {}",playerId,itemId);
                return result;
            }

            //检查道具类型
            if(itemCfg.getType() != GameConstant.Item.TYPE_CAN_USE){
                result.code = Code.FORBID;
                log.debug("改道具不可被使用，使用道具失败 playerId = {},itemId = {}",playerId,itemId);
                return result;
            }

            if(itemCfg.getGetItem() == null || itemCfg.getGetItem().isEmpty()){
                result.code = Code.FORBID;
                log.debug("使用后获取道具配置为空，使用道具失败 playerId = {},itemId = {}",playerId,itemId);
                return result;
            }

            CommonResult<PlayerPack> useResult = null;
            for(Map.Entry<Integer,Long> en : itemCfg.getGetItem().entrySet()){
                int addItemId = en.getKey();
                ItemCfg addItemCfg = GameDataManager.getItemCfg(addItemId);
                if(addItemCfg == null){
                    log.debug("未找到获得新道具的配置 playerId = {},itemId = {}",playerId,addItemId);
                    continue;
                }
                useResult = playerPackService.useItem(playerId, itemId, itemCfg.getProp(), addItemId, en.getValue(), addItemCfg.getProp(), "packUseItem");
            }

            if(useResult == null){
                log.debug("使用道具后获得新道具失败 playerId = {},itemId = {}",playerId,itemId);
                result.code = Code.FAIL;
                return result;
            }

            if(!useResult.success()){
                result.code = useResult.code;
                return result;
            }
            result.data = useResult.data.getItemCount(itemId);
        }catch (Exception e){
            log.error("",e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 领取邮件内的道具
     * @param playerId
     * @param mailId
     * @return
     */
    public CommonResult<Integer> getMailItems(long playerId,int mailId){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        try{
            Mail mail = mailService.getMail(playerId, mailId);
            if(mail == null){
                result.code = Code.NOT_FOUND;
                log.debug("未找到玩家有邮件，获取道具失败 playerId = {},mailId = {}",playerId,mailId);
                return result;
            }

            if(mail.getStatus() == GameConstant.Mail.STAUTS_GET_ITEMS){
                result.code = Code.NOT_FOUND;
                log.debug("该道具已被领取，获取道具失败 playerId = {},mailId = {}",playerId,mailId);
                return result;
            }

            if(mail.getItems() == null || mail.getItems().isEmpty()){
                result.code = Code.NOT_FOUND;
                log.debug("该邮件内没有道具，获取道具失败 playerId = {},mailId = {}",playerId,mailId);
                return result;
            }

            List<int[]> list = new ArrayList<>();
            mail.getItems().forEach(mailItem -> {
                int id = mailItem.getId();
//                GameDataManager.getItemCfg()
//
//                int[] arr = new int[3];
//
//                arr[0] = mailItem.getId();
//                arr[1] = mailItem.getCount()

//                list.add(arr);
            });

            mailService.getMailItems(playerId, mailId);
        }catch (Exception e){
            log.error("",e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /***********************************************************************************************************/

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(WarehouseCfg.EXCEL_NAME, this::initWareHouseConfigData);
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initGlobalConfig);
    }

    /**
     * 缓存倍场配置
     */
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
     * 缓存global表的配置
     */
    private void initGlobalConfig() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(HallConstant.GlobalConfig.DEFAULT_AVATAR_CFG_ID);
        String[] arr = globalConfigCfg.getValue().split(":");
        this.defaultHeadImgId = Integer.parseInt(arr[0]);
        this.defaultHeadFrameId = Integer.parseInt(arr[1]);
        this.defaultNationalId = Integer.parseInt(arr[2]);
        this.defaultTitlelId = Integer.parseInt(arr[3]);
    }

    public int getDefaultHeadImgId() {
        return defaultHeadImgId;
    }

    public int getDefaultHeadFrameId() {
        return defaultHeadFrameId;
    }

    public int getDefaultNationalId() {
        return defaultNationalId;
    }

    public int getDefaultTitlelId() {
        return defaultTitlelId;
    }
}
