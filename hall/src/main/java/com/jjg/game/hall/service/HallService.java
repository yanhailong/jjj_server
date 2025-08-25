package com.jjg.game.hall.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.PlayerAvatarDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.dao.BindDao;
import com.jjg.game.hall.dao.LikeGameDao;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.pb.res.NotifyGameList;
import com.jjg.game.hall.pb.struct.GameListConfig;
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
    private LikeGameDao likeGameDao;
    @Autowired
    private ClusterSystem clusterSystem;

    private Map<Integer, List<WareHouseConfigInfo>> wareHouseConfigMap = new HashMap<>();
    //游戏类型->游戏状态
    private Map<Integer, GameStatus> gameStatusesMap;
    //排序后的gameList
    private List<GameListConfig> sortGameList;

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

    public void init() {
        //缓存倍场的配置信息
        initWareHouseConfigData();
        //缓存每个游戏的状态
        loadGameStatuses(gameStatusService.getAllGameStatus());
    }

    public void loadGameStatuses(List<GameStatus> gameStatuses) {
        if(gameStatuses == null || gameStatuses.isEmpty()) {
            this.gameStatusesMap = new HashMap<>();
        }else {
            this.gameStatusesMap = gameStatuses.stream().collect(Collectors.toMap(GameStatus::gameId, gs -> gs));
        }
        this.sortGameList = sortGameList();
    }

    public void refreshGameStatuses() {
        loadGameStatuses(gameStatusService.getAllGameStatus());

        NotifyGameList notify = new NotifyGameList();
        notify.gameList = this.sortGameList;
        log.debug("推送游戏列表");
        clusterSystem.sessionMap().entrySet().forEach(en -> en.getValue().send(notify));
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
     * 用户修改信息
     * @param nick
     * @param gender
     * @return
     */
    public CommonResult<Player> changePlayerInfo(PlayerController playerController,String nick,byte gender){
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        try{
            boolean[] change = new boolean[2];
            //TODO 后面要敏感词检测，还要判断是否消费道具
            if(!nick.equals(playerController.getPlayer().getNickName())){  //修改昵称
                change[0] = true;
            }

            if(gender != playerController.getPlayer().getGender()){  //修改性别
                change[1] = true;
            }

            if(!change[0] && !change[1]){
                result.code = Code.PARAM_ERROR;
                log.debug("玩家请求修改的昵称和性别与原本一致，无需修改 playerId = {},nick = {},gender = {}", playerController.getPlayer().getId(),nick,gender);
                return result;
            }

            Player player = hallPlayerService.doSave(playerController.playerId(), p -> {
                if(change[0]){
                    p.setNickName(nick);
                }
                if(change[1]){
                    p.setGender(gender);
                }
            });

            if(player == null) {
                result.code = Code.NOT_FOUND;
                log.debug("修改信息失败 playerId = {}", playerController.playerId());
                return result;
            }

            playerController.setPlayer(player);
            result.data = player;

            if(change[0]){
                hallPlayerService.savePlayerNick(playerController.playerId(), nick);
            }
        }catch (Exception e){
            log.error("",e);
        }
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
    public CommonResult<Map<Integer,Long>> useItem(long playerId,int girdId,int itemId,long useItemCount){
        CommonResult<Map<Integer,Long>> result = new CommonResult<>(Code.SUCCESS);
        try{
            log.debug("玩家使用道具 playerId = {},girdId = {},itemId = {}",playerId,girdId,itemId);
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

            Map<Integer,Long> addItemsMap = new HashMap<>();
            CommonResult<PlayerPack> useResult = null;
            for(Map.Entry<Integer,Long> en : itemCfg.getGetItem().entrySet()){
                int addItemId = en.getKey();
                ItemCfg addItemCfg = GameDataManager.getItemCfg(addItemId);
                if(addItemCfg == null){
                    log.debug("未找到获得新道具的配置 playerId = {},itemId = {}",playerId,addItemId);
                    continue;
                }
                addItemsMap.merge(addItemId,en.getValue(),Long::sum);
            }

            Map<Integer,Long> tmpAddItemsMap = new HashMap<>(addItemsMap);

            useResult = playerPackService.useItem(playerId,girdId, itemId,useItemCount, addItemsMap, "packUseItem");

            if(useResult == null){
                log.debug("使用道具后获得新道具失败 playerId = {},itemId = {}",playerId,itemId);
                result.code = Code.FAIL;
                return result;
            }

            if(!useResult.success()){
                result.code = useResult.code;
                return result;
            }
            result.data = tmpAddItemsMap;
        }catch (Exception e){
            log.error("",e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 添加收藏游戏
     * @param playerId
     * @param gameTypes
     * @return
     */
    public List<Integer> addLikeGame(long playerId,List<Integer> gameTypes){
        TreeSet<Integer> set = likeGameDao.addLikeGame(playerId, gameTypes);
        return new ArrayList<>(set);
    }

    /**
     * 添加收藏游戏
     * @param playerId
     * @param gameTypes
     * @return
     */
    public List<Integer> cancelLikeGames(long playerId,List<Integer> gameTypes){
        TreeSet<Integer> set = likeGameDao.calcelLikeGame(playerId, gameTypes);
        if(set == null){
            return null;
        }
        return new ArrayList<>(set);
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
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GameConstant.GlobalConfig.DEFAULT_AVATAR_CFG_ID);
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

    /**
     * 游戏列表配置
     */
    public List<GameListConfig> sortGameList() {
        try {
            Map<Integer, GameStatus> gameStatusesMap = getGameStatusesMap();

            return GameDataManager.getGameListCfgList().stream()
                    .map(configCfg -> {
                        GameListConfig config = new GameListConfig();
                        int gameId = configCfg.getId();
                        int sortValue = Integer.MAX_VALUE;

                        if (Objects.nonNull(gameStatusesMap)) {
                            GameStatus gameStatus = gameStatusesMap.get(gameId);
                            if (Objects.nonNull(gameStatus)) {
                                config.sid = gameStatus.gameId();
                                config.name = configCfg.getName();
                                config.status = gameStatus.status();
                                config.iconType = gameStatus.icon_category();
                                config.rightTopIcon = gameStatus.right_top_icon();
                                sortValue = gameStatus.sort();
                            } else {
                                config.sid = gameId;
                                config.name = configCfg.getName();
                                config.status = configCfg.getStatus();
                                config.iconType = configCfg.getIconType();
                            }
                        } else {
                            config.sid = gameId;
                            config.name = configCfg.getName();
                            config.status = configCfg.getStatus();
                            config.iconType = configCfg.getIconType();
                        }

                        return new AbstractMap.SimpleEntry<>(config, sortValue);
                    })
                    .sorted((a, b) -> {
                        int sortCompare = Integer.compare(a.getValue(), b.getValue());
                        if (sortCompare != 0) {
                            return sortCompare;
                        }
                        return Integer.compare(a.getKey().sid, b.getKey().sid);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("", e);
        }
        return Collections.emptyList();
    }

    public List<GameListConfig> getSortGameList() {
        return sortGameList;
    }
}
