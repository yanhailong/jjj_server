package com.jjg.game.gm.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.sharepromote.dao.SharePromoteDao;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.pb.NotifyKickout;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.*;
import com.jjg.game.core.dao.*;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.AmazonBucketManager;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.gm.*;
import com.jjg.game.core.rpc.GmToAllBridge;
import com.jjg.game.core.rpc.GmToHallBridge;
import com.jjg.game.core.rpc.GmToRechargeBridge;
import com.jjg.game.core.rpc.HallPointsAwardBridge;
import com.jjg.game.core.service.*;
import com.jjg.game.core.utils.CoreUtil;
import com.jjg.game.gm.dao.SlotsLibDao;
import com.jjg.game.gm.dto.*;
import com.jjg.game.gm.util.NetUtil;
import com.jjg.game.gm.vo.*;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.LoginConfigCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lm
 * @since 2025/7/10 09:15
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "gm")
public class GMController extends AbstractController {

    @Autowired
    private GameStatusService gameStatusService;
    @Autowired
    private MarqueeDao marqueeDao;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private MailService mailService;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private OnlinePlayerDao onlinePlayerDao;
    @Autowired
    private CarouselService carouselService;
    @Autowired
    private ShopProductDao shopProductDao;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private BlackListService blackListService;
    @Autowired
    private AmazonBucketManager amazonBucketManager;
    @Autowired
    private CoreSendMessageManager coreSendMessageManager;
    @Autowired
    private LoginConfigService loginConfigService;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @ClusterRpcReference
    private HallPointsAwardBridge hallPointsAwardBridge;
    @Autowired
    private SlotsLibDao slotsLibDao;
    @Autowired
    private NoticeDao noticeDao;
    @Autowired
    private SharePromoteDao sharePromoteDao;
    @ClusterRpcReference()
    private GmToRechargeBridge gmToRechargeBridge;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CommonDao commonDao;
    @ClusterRpcReference
    private GmToHallBridge gmToHallBridge;
    @Autowired
    private SmsService smsService;
    @ClusterRpcReference
    private GmToAllBridge gmToAllBridge;

    //邮件中的道具string，需要用正则匹配
    private final Pattern mailItemsPattern = Pattern.compile("\\[(\\d+),(\\d+)]");

    /**
     * 修改游戏状态
     */
    @RequestMapping(BackendGMCmd.CHANGE_GAME_STATUS)
    public WebResult<String> changeGameStatus(@RequestBody GameStatusListDto dtoList) {
        try {
            log.info("收到修改游戏状态请求 dtoList = {}", dtoList);
            if (dtoList == null || dtoList.gameStatusList() == null || dtoList.gameStatusList().isEmpty()) {
                log.debug("游戏列表为空，修改游戏状态失败 dtoList = {}", dtoList);
                return fail("common.paramerror");
            }

            //检查参数
            List<GameStatus> gameStatusList = new ArrayList<>();
            for (GameStatusDto dto : dtoList.gameStatusList()) {
                if (dto.number() < 1) {
                    log.debug("游戏id不能为负，修改游戏状态失败 dto = {}", dto);
                    return fail("common.paramerror");
                }

                if (dto.open() != 1 && dto.open() != 2) {
                    log.debug("开放状态错误，只能为1(开放)或2(不开放)  dto = {}", dto);
                    return fail("common.paramerror");
                }

                if (dto.status() != 1 && dto.status() != 2) {
                    log.debug("上下架状态错误，只能为1(上架)或2(下架)  dto = {}", dto);
                    return fail("common.paramerror");
                }

                if (dto.icon_category() != 0 && dto.icon_category() != 1) {
                    log.debug("图标大小错误，只能为0(大)或1(小)  dto = {}", dto);
                    return fail("common.paramerror");
                }

                gameStatusList.add(new GameStatus(dto.name(), dto.number(), dto.open(), dto.status(), dto.right_top_icon(), dto.icon_category(), dto.sort()));
            }

            boolean saved = gameStatusService.saveOrUpdateGameStatus(gameStatusList);

            if (!saved) {
                log.info("修改游戏状态失败,无法保存到Redis , dto = {}", dtoList);
                return fail("common.fail");
            }
            //获取大厅节点
            List<ClusterClient> nodesByType = ClusterSystem.system.getNodesByType(NodeType.HALL);
            //构建请求消息
            ReqRefreshGameStatus msg = new ReqRefreshGameStatus();

            msg.cmdParam = new ObjectMapper().writeValueAsString(dtoList);

            byte[] data = ProtostuffUtil.serialize(msg);
            PFMessage pfMessage = new PFMessage(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS, data);
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            for (ClusterClient clusterClient : nodesByType) {
                try {
                    //通知大厅节点修改游戏状态
                    clusterClient.write(clusterMessage);
                } catch (Exception e) {
                    log.error("请求改变游戏状态时发送失败", e);
                }
            }
            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 添加跑马灯
     */
    @RequestMapping(BackendGMCmd.SNED_MARQUEE)
    public WebResult<String> sendMarquee(@RequestBody MarqueeDto dto) {
        try {
            log.info("收到后台的跑马灯信息请求 {}", dto);
            if (dto.id() < 1) {
                log.debug("开启跑马灯时，从后台收到的跑马灯id不能小于1 id = {}", dto.id());
                return fail("common.paramerror");
            }

            if (StringUtils.isEmpty(dto.content()) || dto.showTime() < 0 || dto.interval_time() < 0 || dto.priority() < 0 ||
                    StringUtils.isEmpty(dto.start_time()) || StringUtils.isEmpty(dto.end_time())) {
                log.debug("从后台收到的跑马灯参数错误");
                return fail("common.paramerror");
            }

            //存储到redis
            Marquee marquee = new Marquee();
            marquee.setId(dto.id());

            LanguageData contentData = new LanguageData();
            contentData.setType(GameConstant.Language.TYPE_ORIGINAL);
            contentData.setContent(dto.content());
            marquee.setContent(contentData);

            marquee.setInterval(dto.interval_time());
            marquee.setNums(0);
            marquee.setShowTime(dto.showTime());
            marquee.setStartTime(TimeHelper.getSecondTime(dto.start_time()));
            marquee.setEndTime(TimeHelper.getSecondTime(dto.end_time()));
            marquee.setPriority(dto.priority());
            marquee.setType(dto.type() < 1 ? GameConstant.Marquee.SYSTEM_MSG : dto.type());
            marqueeDao.addMarquee(marquee);

            //构建请求消息,发送到其他节点
            NotifyAllNodesMarqueeServer notify = new NotifyAllNodesMarqueeServer();
            notify.marqueeInfo = marqueeManager.transMarqueeInfo(marquee);
            notify.type = marquee.getType();
            marqueeManager.notifyHallAndGameNodeStartMarquee(notify);
            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 停止跑马灯
     */
    @RequestMapping(BackendGMCmd.STOP_MARQUEE)
    public WebResult<String> stopMarquee(@RequestBody StopMarqueeDto dto) {
        try {
            log.info("收到后台的停止跑马灯信息请求 id = {}", dto.id());
            if (dto.id() < 1) {
                log.debug("停止跑马灯时，从后台收到的跑马灯id不能小于1 id = {}", dto.id());
                return fail("common.paramerror");
            }

            boolean exist = marqueeDao.exist(dto.id());
            if (!exist) {
                return fail("marquee.notfound");
            }

            //构建请求消息
            NotifyAllNodesStopMarqueeServer notify = new NotifyAllNodesStopMarqueeServer();
            notify.id = dto.id();
            marqueeManager.notifyHallAndGameNodeStopMarquee(notify);

            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 查询玩家信息
     */
    @RequestMapping(BackendGMCmd.QUERY_ACCOUNT)
    public WebResult<PlayerVo> queryAccount(@RequestBody QueryAccountDto dto) {
        try {
            log.info("收到后台查询玩家信息请求 dto = {}", dto);

            Player p = null;
            Account account = null;
            if (dto.playerId() > 0) {  //根据玩家id查询
                p = playerService.getFromAllDB(dto.playerId());
                account = accountDao.queryAccountByPlayerId(dto.playerId());
            } else if (StringUtils.isNotEmpty(dto.registerMac())) {  //根据注册mac
                account = accountDao.queryByRegisterMac(dto.registerMac());
                if (account == null) {
                    log.debug("未找到该玩家账号信息 registerMac = {}", dto.registerMac());
                    return fail("common.fail");
                }
                p = playerService.getFromAllDB(account.getPlayerId());
            } else if (StringUtils.isNotEmpty(dto.loginMac())) {  //根据登录mac
                account = accountDao.queryByLoginMac(dto.loginMac());
                if (account == null) {
                    log.debug("未找到该玩家账号信息 loginMac = {}", dto.loginMac());
                    return fail("common.fail");
                }
                p = playerService.getFromAllDB(account.getPlayerId());
            } else if (StringUtils.isNotEmpty(dto.nickName())) {   //根据昵称
                long playerId = playerService.queryPlayerIdByNick(dto.nickName());
                if (playerId < 1) {
                    log.debug("未找到该玩家账号信息 nick = {}", dto.nickName());
                    return fail("common.fail");
                }
                p = playerService.getFromAllDB(playerId);
                account = accountDao.queryAccountByPlayerId(playerId);
            } else if (StringUtils.isNotEmpty(dto.mobile())) {  //根据手机号
                account = accountDao.queryThirdAccount(LoginType.PHONE, dto.mobile());
                if (account == null) {
                    log.debug("未找到该玩家账号信息 mobile = {}", dto.mobile());
                    return fail("common.fail");
                }
                p = playerService.getFromAllDB(account.getPlayerId());
            }

            if (account == null || p == null) {
                if (account == null) {
                    log.debug("未找到该玩家账号信息 dto = {}", dto);
                    return fail("common.fail");
                }
                return fail("common.fail");
            }

            boolean check = checkPlayerInfo(dto, p, account);
            if (!check) {
                log.debug("获取后检验信息失败 dto = {},playerId = {}", dto, p.getId());
                return fail("common.fail");
            }

            //返回修改结果
            PlayerVo vo = new PlayerVo();
            vo.setPlayerId(p.getId());
            vo.setNickName(p.getNickName());
            vo.setGold(p.getGold());
            vo.setDiamond(p.getDiamond());
            vo.setVipLevel(p.getVipLevel());
            vo.setIp(p.getIp());
            vo.setCreateTime(account.getCreateTime());
            vo.setRegisterMac(account.getRegisterMac());
            vo.setIsBan(account.getStatus());
            vo.setIsOffline(playerSessionService.hasSession(p.getId()) ? 0 : 1);
            vo.setMobile(account.getThirdAccount(LoginType.PHONE));
            vo.setLevel(p.getLevel());
            vo.setGameType(p.getGameType());
            vo.setRoomCfgId(p.getRoomCfgId());

            SafeVo safeVo = new SafeVo();
            safeVo.setSafeGold(p.getSafeBoxGold());
            safeVo.setSafeDiamond(p.getSafeBoxDiamond());
            vo.setSafeInfo(safeVo);

            log.info("返回玩家信息 info = {}", JSON.toJSONString(vo));
            return success(vo);
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 邮件
     */
    @RequestMapping(BackendGMCmd.SEND_EMAIL)
    public WebResult<String> sendEmail(@RequestBody MailDto dto) {
        try {
            log.debug("收到后台的邮件请求 dto = {}", dto);
            if (StringUtils.isEmpty(dto.title())) {
                log.debug("发送邮件时，标题不能为空");
                return fail("mail.titlenull");
            }

            if (StringUtils.isEmpty(dto.content())) {
                log.debug("发送邮件时，内容不能为空 title = {}", dto.content());
                return fail("mail.contentnull");
            }

            List<Item> mailItems = null;
            //解析邮件中的道具
            if (StringUtils.isNotEmpty(dto.items())) {
                mailItems = mailItemsPattern.matcher(dto.items())
                        .results()
                        .map(match -> new Item(
                                Integer.parseInt(match.group(1)),
                                Long.parseLong(match.group(2))
                        ))
                        .collect(Collectors.toList());

                for (Item item : mailItems) {
                    ItemCfg itemCfg = GameDataManager.getItemCfg(item.getId());
                    if (itemCfg == null) {
                        log.warn("邮件中的道具，未在配置表中找到 id = {},count = {}", item.getId(), item.getItemCount());
                        return fail("mail.itemerror");
                    }
                }
            }

            if (StringUtils.isEmpty(dto.designated())) {  //为空表示全服邮件
                mailService.addAllServerMail(dto.title(), dto.content(), mailItems);
            } else {
                List<Long> playerIds = new ArrayList<>();
                String[] arr = dto.designated().split(",");
                for (String str : arr) {
                    playerIds.add(Long.parseLong(str));
                }
                mailService.addMails(playerIds, dto.title(), dto.content(), mailItems);
            }

            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 货币操作
     */
    @RequestMapping(BackendGMCmd.GOLD_OPERATOR)
    public WebResult<String> goldOperator(@RequestBody GoldOperatorDto dto) {
        try {
            log.debug("收到后台的修改玩家货币的请求 dto = {}", dto);
            if (dto.playerId() < 1) {
                log.debug("修改货币时，玩家id不能小于 1,playerId = {}", dto.playerId());
                return fail("common.paramerror");
            }

            if (dto.currency_id() != GameConstant.Item.TYPE_DIAMOND && dto.currency_id() != GameConstant.Item.TYPE_GOLD) {
                log.debug("修改货币时，货币类型错误 currency_type = {}", dto.currency_id());
                return fail("common.paramerror");
            }

            if (dto.type() != 1 && dto.type() != 2) {
                log.debug("修改货币时，操作类型错误 type = {}", dto.type());
                return fail("common.paramerror");
            }

            if (dto.operator_type() != 1 && dto.operator_type() != 2) {
                log.debug("修改货币时，增减资金流向错误 operator_type = {}", dto.operator_type());
                return fail("common.paramerror");
            }

            if (dto.quantity() < 1) {
                log.debug("修改货币时，增减数量错误 quantity = {}", dto.quantity());
                return fail("common.paramerror");
            }

            if (StringUtils.isEmpty(dto.playerName())) {
                log.debug("修改货币时，用户名不能为空");
                return fail("common.paramerror");
            }

            Player player = playerService.get(dto.playerId());
            if (player == null) {
                log.debug("修改货币时，未找到该用户 playerId = {}", dto.playerId());
                return fail("common.paramerror");
            }

            if (!dto.playerName().equals(player.getNickName())) {
                log.debug("修改货币时，用户名校验错误 playerId = {},paramNick = {},dbNick = {}", dto.playerId(), dto.playerName(), player.getNickName());
                return fail("common.paramerror");
            }

            boolean notifyNode = false;
            AddType addType = AddType.GM_OPERATOR;
            CommonResult<Player> result;
            if (dto.operator_type() == 1) {  //账户
                PlayerSessionInfo info = playerSessionService.getInfo(player.getId());
                if (info == null || info.getGameType() == CoreConst.GameMajorType.SLOTS) {
                    if (dto.type() == 1) { //增加
                        if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                            result = playerService.addGold(dto.playerId(), dto.quantity(), addType, dto.remark());
                        } else {  //钻石
                            result = playerService.addDiamond(dto.playerId(), dto.quantity(), addType, dto.remark());
                        }
                    } else {
                        if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                            result = playerService.deductGold(dto.playerId(), dto.quantity(), addType, dto.remark());
                        } else {  //钻石
                            result = playerService.deductDiamond(dto.playerId(), dto.quantity(), addType, dto.remark());
                        }
                    }
                } else {
                    //获取节点
                    String[] arr = info.getCurrentNode().split("/");
                    ClusterClient clusterClient = clusterSystem.getNodesByName(arr[arr.length - 1]);
                    if (clusterClient == null) {
                        log.debug("修改货币时，未找到玩家所在节点 playerId = {},nodeName = {}", dto.playerId(), info.getCurrentNode());
                        return fail("common.paramerror");
                    }
                    NotifyGoldOperator notify = new NotifyGoldOperator();
                    notify.playerId = dto.playerId();
                    notify.currency_id = dto.currency_id();
                    notify.type = dto.type();
                    notify.quantity = dto.quantity();
                    notify.addType = addType.getValue();
                    notify.remark = dto.remark();

                    PFMessage pfMessage = MessageUtil.getPFMessage(notify);
                    ClusterMessage msg = new ClusterMessage(pfMessage);
                    clusterClient.write(msg);
                    result = new CommonResult<>(Code.SUCCESS);
                    notifyNode = true;
                    log.debug("通知节点修改玩家账户 node = {},notify = {}", info.getCurrentNode(), JSON.toJSONString(notify));
                }
            } else {  //保险箱
                if (dto.type() == 1) { //增加
                    if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                        result = playerService.addSafeBoxGold(dto.playerId(), dto.quantity(), addType, dto.remark());
                    } else {  //钻石
                        result = playerService.addSafeBoxDiamond(dto.playerId(), dto.quantity(), addType, dto.remark());
                    }
                } else {
                    if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                        result = playerService.deductSafeBoxGold(dto.playerId(), dto.quantity(), addType, dto.remark());
                    } else {  //钻石
                        result = playerService.deductSafeBoxDiamond(dto.playerId(), dto.quantity(), addType, dto.remark());
                    }
                }
            }

            if (!result.success()) {
                log.debug("修改货币时错误 ,playerId = {},code = {}", dto.playerId(), result.code);
                return fail("common.fail");
            }

            if (!notifyNode && dto.operator_type() == 1) {  //如果是账户修改，则要进行通知
//                coreSendMessageManager.buildBaseInfoChangeMessage(result.data);
                if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                    coreSendMessageManager.buildGoldChangeMessage(result.data, dto.type() == 1 ? dto.quantity() : -dto.quantity());
                } else {
                    coreSendMessageManager.buildDiamondChangeMessage(result.data, dto.type() == 1 ? dto.quantity() : -dto.quantity());
                }
            }

            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 修改vip等级
     */
    @RequestMapping(BackendGMCmd.SET_VIPLEVEL)
    public WebResult<String> setVip(@RequestBody SetVipDto dto) {
        try {
            log.debug("收到后台的设置vip等级的请求 dto = {}", dto);
            if (dto.playerId() < 1) {
                log.debug("设置vip等级，玩家id不能小于 1,playerId = {}", dto.playerId());
                return fail("common.paramerror");
            }
            if (dto.vipLevel() < 1) {
                log.debug("设置vip等级，增减数量错误 vipLevel = {}", dto.vipLevel());
                return fail("common.paramerror");
            }
            Player player = playerService.get(dto.playerId());
            if (player == null) {
                log.debug("设置vip等级，未找到该用户 playerId = {}", dto.playerId());
                return fail("common.paramerror");
            }

            boolean notifyNode = false;
            CommonResult<Player> result;
            result = playerService.setVip(dto.playerId(), dto.vipLevel(), AddType.GM_OPERATOR, null);
            if (!result.success()) {
                log.debug("设置vip等级错误 ,playerId = {},code = {}", dto.playerId(), result.code);
                return fail("common.fail");
            }
            if (!notifyNode) {  //如果是账户修改，则要进行通知
                coreSendMessageManager.buildBaseInfoChangeMessage(result.data);
            }
            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 踢出玩家
     */
    @RequestMapping(BackendGMCmd.KICK_ACCOUNT)
    public WebResult<String> kickAccount(@RequestBody KickAccountDto dto) {
        try {
            log.info("收到后台踢人的请求 dto = {}", dto);
            if (dto.type() == 1) {  //指定id
                if (StringUtils.isEmpty(dto.ids())) {
                    log.debug("指定id踢人时，ids不能为空 dto = {}", dto);
                    return fail("common.paramerror");
                }
                String[] arr = dto.ids().trim().split(",");
                if (arr.length < 1) {
                    log.debug("踢人的玩家id不能为空 type = {}", dto.type());
                    return fail("common.paramerror");
                }

                NotifyKickout notifyKickout = new NotifyKickout();
                notifyKickout.langId = dto.tips();
                for (String str : arr) {
                    long playerId = Long.parseLong(str);
                    PFSession session = playerSessionService.getSession(playerId);
                    if (session == null) {
                        continue;
                    }
                    session.send(notifyKickout);
                }
            } else if (dto.type() == 2) {  //全服
                ReqAllKickout req = new ReqAllKickout();
                req.langId = dto.tips();
                PFMessage pfMessage = MessageUtil.getPFMessage(req);
                clusterSystem.notifyHallAndGameNode(pfMessage);
            } else {
                log.debug("踢人类型错误 type = {}", dto.type());
                return fail("common.paramerror");
            }
            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 封禁
     */
    @RequestMapping(BackendGMCmd.BAN_ACCOUNT)
    public WebResult<List<BanAccountVo>> banAccount(@RequestBody BanAccountDto dto) {
        try {
            log.info("收到后台封禁账号的请求 dto = {}", dto);
            AccountStatus accountStatus = AccountStatus.valueOf(dto.type());
            if (accountStatus == null || accountStatus == AccountStatus.DELETE) {
                log.debug("封禁类型错误 type = {}", dto.type());
                return fail("common.paramerror");
            }

            String[] arr = dto.playerIds().trim().split(",");
            if (arr.length < 1) {
                log.debug("封禁的玩家id不能为空 type = {}", dto.type());
                return fail("common.paramerror");
            }

            //先踢人
            NotifyKickout notifyKickout = new NotifyKickout();

            List<Long> delTokenList = new ArrayList<>();

            List<BanAccountVo> voList = new ArrayList<>();
            for (String str : arr) {
                long playerId = Long.parseLong(str);
                BanAccountVo vo = new BanAccountVo();
                vo.setPlayerId(playerId);
                voList.add(vo);

                if (accountStatus == AccountStatus.BAN) {  //封
                    vo.setChangeStatus(AccountStatus.BAN.getCode());
                    delTokenList.add(playerId);
                    Account resAccount = accountDao.checkAndSaveRes(playerId, new DataSaveCallback<>() {
                        @Override
                        public void updateData(Account dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(Account dataEntity) {
                            if (dataEntity.getStatus() == AccountStatus.NORMAL.getCode()) {
                                dataEntity.setStatus(AccountStatus.BAN.getCode());
                                return true;
                            }
                            log.warn("玩家当前状态不能被封禁 playerId = {},status = {},toStatus = {}", dataEntity.getPlayerId(), dataEntity.getStatus(), AccountStatus.BAN.getCode());
                            return false;
                        }
                    });
                    if (resAccount == null) {
                        log.warn("玩家封禁账号失败 playerId = {}", playerId);
                        vo.setSuccess(false);
                        continue;
                    }
                    vo.setSuccess(true);

                    PFSession session = playerSessionService.getSession(playerId);
                    if (session == null) {
                        continue;
                    }
                    session.send(notifyKickout);
                } else {  //解
                    vo.setChangeStatus(AccountStatus.NORMAL.getCode());
                    Account resAccount = accountDao.checkAndSaveRes(playerId, new DataSaveCallback<>() {
                        @Override
                        public void updateData(Account dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(Account dataEntity) {
                            if (dataEntity.getStatus() == AccountStatus.BAN.getCode()) {
                                dataEntity.setStatus(AccountStatus.NORMAL.getCode());
                                return true;
                            }
                            log.warn("玩家当前状态不能被解封 playerId = {},status = {},toStatus = {}", dataEntity.getPlayerId(), dataEntity.getStatus(), AccountStatus.NORMAL.getCode());
                            return true;
                        }
                    });

                    if (resAccount == null) {
                        log.warn("玩家解封账号失败 playerId = {}", playerId);
                        vo.setSuccess(false);
                    } else {
                        vo.setSuccess(true);
                    }
                }
            }

            //如果是封禁或者删除账号，则要删除当前token
            playerSessionTokenDao.delTokens(delTokenList);
            //返回修改结果
            return success("common.success", voList);
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 在线玩家
     */
    @RequestMapping(BackendGMCmd.PLAYING_INFO)
    public WebResult<PageVo<List<OnlinePlayerVo>>> onlinePlayer(@RequestBody OnlinePlayerDto dto) {
        try {
            log.info("收到后台查询在线玩家的请求 dto = {}", dto);
            if (dto.gameId() < 1 || dto.pageSize() < 1 || dto.page() < 1) {
                log.debug("参数错误 dto = {}", dto);
                return fail("common.paramerror");
            }

            List<String> subChannels = new ArrayList<>();
            if (dto.subChannels() != null) {
                for (String str : dto.subChannels()) {
                    if (!StringUtils.isBlank(str)) {
                        subChannels.add(str);
                    }
                }
            }

            List<OnlinePlayer> list = onlinePlayerDao.query(dto.gameId(), dto.registerChannel(), dto.playerId(), subChannels, dto.pageSize(), dto.page());
            if (list == null || list.isEmpty()) {
                return success("common.success");
            }

            List<Long> playerIds = new ArrayList<>();
            list.forEach(onlinePlayer -> playerIds.add(onlinePlayer.getPlayerId()));

            //从redis获取玩家最新信息
            Map<Long, Player> playerMap = playerService.multiGetPlayerMap(playerIds);

            PageVo<List<OnlinePlayerVo>> pageVo = new PageVo<>();
            List<OnlinePlayerVo> resultList = new ArrayList<>();
            for (OnlinePlayer olp : list) {
                OnlinePlayerVo vo = new OnlinePlayerVo();
                vo.setPlayerId(olp.getPlayerId());

                Player player = playerMap.get(vo.getPlayerId());
                if (player != null) {
                    vo.setPlayerName(player.getNickName());
                    vo.setCreateTime(player.getCreateTime());
                    vo.setGold(player.getGold());
                    vo.setDiamond(player.getDiamond());
                    vo.setGameType(player.getGameType());
                    vo.setRoomCfgId(player.getRoomCfgId());
                } else {
                    vo.setGameType(olp.getGameType());
                    vo.setRoomCfgId(olp.getRoomCfgId());
                }
                vo.setRegisterChannel(olp.getChannel());
                resultList.add(vo);
            }
            pageVo.setCount(onlinePlayerDao.countBy(dto.registerChannel(), dto.gameId()));
            pageVo.setData(resultList);

            //返回修改结果
            return success("common.success", pageVo);
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 添加或者更新轮播数据
     */
    @RequestMapping(BackendGMCmd.REPLACE_CAROUSEL)
    public WebResult<String> replaceCarousel(@RequestBody CarouselDto dto) {
        log.info("收到轮播数据更新dto={}", dto);
        try {
            if (dto == null || dto.id() < 0 || dto.activityImageType() < 0) {
                return fail("common.paramerror");
            }
            //构建变化的数据
            Carousel carousel = buildCarousel(dto);
            //构建通知变化数据对象
            CarouselUpdateInfo carouselUpdateInfo = new CarouselUpdateInfo();
            carouselUpdateInfo.setType(CarouselUpdateInfo.CarouselUpdateType.UPDATE);
            carouselUpdateInfo.setCarousel(carousel);
            carouselService.updateCarousel(carousel);
            carouselService.notifyHallCarouselUpdate(List.of(carouselUpdateInfo));
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 删除轮播数据
     */
    @RequestMapping(BackendGMCmd.DELETE_CAROUSEL)
    public WebResult<String> deleteCarousel(@RequestBody CarouselDeleteDto dto) {
        log.info("收到删除轮播数据请求dto={}", dto);
        try {
            if (dto == null || dto.id().isEmpty()) {
                return fail("common.paramerror");
            }
            List<CarouselUpdateInfo> updateInfoList = new ArrayList<>();
            dto.id().forEach(id -> {
                boolean deleted = carouselService.deleteCarouselById(id);
                //删除成功才通知
                if (deleted) {
                    //构建变化的数据
                    Carousel carousel = new Carousel();
                    carousel.setId(id);
                    //构建通知变化数据对象
                    CarouselUpdateInfo carouselUpdateInfo = new CarouselUpdateInfo();
                    carouselUpdateInfo.setType(CarouselUpdateInfo.CarouselUpdateType.DELETE);
                    carouselUpdateInfo.setCarousel(carousel);
                    updateInfoList.add(carouselUpdateInfo);
                }
            });
            carouselService.notifyHallCarouselUpdate(updateInfoList);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 同步轮播数据
     */
    @RequestMapping(BackendGMCmd.SYNC_CAROUSEL)
    public WebResult<String> syncCarousel(@RequestBody CarouselSyncDto param) {
        log.info("收到同步轮播数据请求carouselDtoList={}", param);
        try {
            if (param.list() != null && !param.list().isEmpty()) {
                carouselService.sync(param.list());
            }
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 生成结果库
     */
    @RequestMapping(BackendGMCmd.GENERATE_LIB)
    public WebResult<String> generateLib(@RequestBody GenerateLibDto param) {
        log.info("收到生成结果库的请求请求 param={}", param);
        try {
            ClusterClient clusterClient;
            if (StringUtils.isNotEmpty(param.nodeName())) {
                clusterClient = clusterSystem.getNodesByName(param.nodeName());
            } else {
                clusterClient = clusterSystem.randClientByType(NodeType.GAME, CoreConst.GameMajorType.SLOTS);
            }

            if (clusterClient == null) {
                log.debug("未找到对应的游戏节点");
                return fail("common.fail");
            }

            if (NodeType.GAME.name().equals(clusterClient.nodeConfig.getType()) &&
                    clusterClient.nodeConfig.getGameMajorTypes()[0] != CoreConst.GameMajorType.SLOTS) {
                log.debug("只能是slots的游戏节点才需要生成结果库 param = {}", param);
                return fail("common.fail");
            }

            NotifyGenrateLib notify = new NotifyGenrateLib();
            notify.gameType = param.gameType();
            notify.count = param.count();

            PFMessage pfMessage = MessageUtil.getPFMessage(notify);
            ClusterMessage msg = new ClusterMessage(pfMessage);
            clusterClient.write(msg);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 获取生成结果状态的请求
     */
    @RequestMapping(BackendGMCmd.GENERATE_LIB_STATUS)
    public WebResult<Set<Integer>> generateLibStatus() {
        log.info("收到查询生成结果状态的请求");
        try {
            return success(slotsLibDao.scanAllGenerateLocks());
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 保存商品
     */
    @RequestMapping(BackendGMCmd.SAVE_SHOP_PRODUCTS)
    public WebResult<String> saveShopProducts(@RequestBody SaveShopProductsDto dto) {
        log.info("收到保存商品请求 param={}", dto);
        try {
            if (dto.products() == null || dto.products().isEmpty()) {
                log.debug("保存的商品列表为空");
                return fail("common.fail");
            }

            boolean match = dto.products().stream().anyMatch(p -> p.id() < 1);
            if (match) {
                log.debug("商品的id不能小于1");
                return fail("common.fail");
            }

            List<ShopProduct> list = new ArrayList<>();
            dto.products().forEach(p -> {
                ShopProduct shopProduct = new ShopProduct();
                BeanUtils.copyProperties(p, shopProduct);
                list.add(shopProduct);
            });

            shopProductDao.deleteAll();
            shopProductDao.saveProducts(list);

            //通知大厅节点，商城商品变更
            PFMessage pfMessage = MessageUtil.getPFMessage(new NotifyShopProductChange());
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString(), NodeType.GAME.toString(), NodeType.RECHARGE.toString())::contains);

            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 删除商品
     */
    @RequestMapping(BackendGMCmd.DEL_SHOP_PRODUCTS)
    public WebResult<String> delShopProducts(@RequestBody DelShopProductsDto dto) {
        log.info("收到删除商品请求 param={}", dto);
        try {
            if (dto.productIds() == null || dto.productIds().isEmpty()) {
                log.debug("删除的商品列表为空");
                return fail("common.fail");
            }

            shopProductDao.delById(dto.productIds());

            //通知大厅节点，商城商品变更
            PFMessage pfMessage = MessageUtil.getPFMessage(new NotifyShopProductChange());
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString())::contains);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 添加黑名单
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANGE_BLACK_LIST)
    public WebResult<String> changeBlackList(@RequestBody BlackListDto dto) {
        log.info("收到修改黑名单信息 param={}", dto);
        try {
            boolean none = true;
            if (dto.ids() != null) {
                dto.ids().removeIf(Objects::isNull);
                if (!dto.ids().isEmpty()) {
                    if (dto.type() == 0) {
                        blackListService.addBlackIds(dto.ids());
                    } else {
                        blackListService.removeBlackIds(dto.ids());
                    }
                    none = false;
                }
            }

            if (dto.ips() != null) {
                dto.ips().replaceAll(String::trim);
                dto.ips().removeIf(StringUtils::isEmpty);
                if (!dto.ips().isEmpty()) {
                    boolean match = dto.ips().stream().allMatch(NetUtil::isValidIP);
                    if (!match) {
                        log.debug("ip格式错误");
                        return fail("common.fail");
                    }
                    if (dto.type() == 0) {
                        blackListService.addBlackIps(dto.ips());
                    } else {
                        blackListService.removeBlackIps(dto.ips());
                    }

                    none = false;
                }
            }

            if (none) {
                log.debug("黑名单为空...");
                return fail("common.fail");
            }

            NotifyLoadBlackList notify = new NotifyLoadBlackList();
            notify.loadId = dto.ids() != null;
            notify.loadIp = dto.ips() != null;

            PFMessage pfMessage = MessageUtil.getPFMessage(notify);
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.ACCOUNT.toString())::contains);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 获取服务器列表
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.QUERY_GAME_SERVER_NODE_LIST)
    public WebResult<List<GameNodeVo>> queryGameServerList() {
        log.info("收到获取服务器列表信息 ");
        List<GameNodeVo> nodeConfigList = new ArrayList<>();
//        addNodeConfig(nodeConfigList, NodeType.GATE);
        addNodeConfig(nodeConfigList, NodeType.ACCOUNT);
        addNodeConfig(nodeConfigList, NodeType.HALL);
        addNodeConfig(nodeConfigList, NodeType.GAME);
        addNodeConfig(nodeConfigList, NodeType.RECHARGE);
        return success("common.success", nodeConfigList);
    }

    /**
     * 修改服务器信息
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANG_GAME_NODE_INFO)
    public WebResult<String> changeGameServerInfo(@RequestBody ChangeNodeDto dto) {
        log.info("收到修改服务器列表信息 dto = {}", dto);

        try {
            if (dto.name() == null || dto.name().isEmpty()) {
                log.debug("修改服务器信息错误,节点名不能为空 dto = {}", dto);
                return fail("common.paramerror");
            }
            ClusterClient clusterClient = clusterSystem.getNodesByName(dto.name());
            if (clusterClient == null) {
                log.debug("修改服务器信息错误,未找到该节点 dto = {}", dto);
                return fail("common.paramerror");
            }

            NotifyGameNodeChange notify = new NotifyGameNodeChange();
            notify.weight = dto.weight();
            notify.ips = dto.whiteIpList();
            notify.ids = dto.whiteIdList();

            PFMessage pfMessage = MessageUtil.getPFMessage(notify);
            clusterClient.write(new ClusterMessage(pfMessage));
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }

    }

    /**
     * 更新excel配置
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.UPDATE_EXCEL_CONFIGS)
    public WebResult<String> updateExcelConfigs(@RequestBody UpdateExcelConfigsDto dto) {
        log.info("收到excel配置表更新请求 dto = {}", dto);

        try {
            if (dto.nameList() == null || dto.nameList().isEmpty()) {
                log.debug("更新excel配置表错误,名称列表不能为空 dto = {}", dto);
                return fail("common.paramerror");
            }

            //获取除 gate 之外的所有节点
            List<ClusterClient> clusterList = clusterSystem.getAllExcept(NodeType.GATE);
            if (clusterList == null || clusterList.isEmpty()) {
                log.debug("更新excel配置表错误,获取节点列表为空 dto = {}", dto);
                return fail("common.paramerror");
            }

            NotifyExcelChange notify = new NotifyExcelChange();
            notify.nameList = dto.nameList();

            PFMessage pfMessage = MessageUtil.getPFMessage(notify);
            clusterSystem.sendClusterMessage(pfMessage, clusterList);

            amazonBucketManager.dowmloadFiles(dto.nameList());
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 更新登录配置
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANGE_LOGIN_CONFIG)
    public WebResult<String> changeLoginConfig(@RequestBody ChangeLoginConfigDto dto) {
        log.info("收到更新登录配置的请求 dto = {}", dto);

        try {
            LoginConfigCfg loginConfigCfg = GameDataManager.getLoginConfigCfgList().stream().filter(c -> c.getType() == dto.loginType()).findFirst().orElse(null);
            if (loginConfigCfg == null) {
                log.debug("修改登录配置失败,未找到对应的配置文件 dto = {}", dto);
                return fail("common.paramerror");
            }

            loginConfigService.save(dto.loginType(), dto.open());

            PFMessage pfMessage = MessageUtil.getPFMessage(new NotifyLoadLoginConfig());
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.ACCOUNT.toString(), NodeType.HALL.toString())::contains);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }

    }

    /**
     * 修改积分大奖积分
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANGE_PLAYER_POINTS)
    public WebResult<String> changePoints(@RequestBody ChangePointsDto dto) {
        log.info("收到修改玩家积分大奖积分请求 dto = {}", dto);
        try {
            int points = dto.points();
            if (points <= 0 || dto.playerId() <= 0) {
                return fail("common.paramerror");
            }
            changePlayerPoints(dto.playerId(), points, dto.flag());
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }

    }

    /**
     * 保存公告
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SAVE_NOTICE)
    public WebResult<String> saveNotice(@RequestBody NoticeDto dto) {
        log.info("收到保存公告的请求 dto = {}", dto);
        try {
            Notice notice = new Notice();
            notice.setId(dto.id());
            notice.setName(dto.name());
            notice.setTitle(dto.title());
            notice.setContent(dto.content());
            notice.setType(dto.type());
            notice.setSort(dto.sort());
            notice.setOpen(dto.open() == 1);
            notice.setCornerMark(dto.corner_mark());
            notice.setBackdrop(dto.backdrop());
            notice.setButton(dto.button());
            notice.setStartTime(dto.start_time());
            notice.setEndTime(dto.end_time());
            notice.setScence(dto.scene());
            notice.setJumpUrl(dto.jump_url());
            noticeDao.save(notice);

            PFMessage pfMessage = MessageUtil.getPFMessage(new NotifyLoadNoticeConfig());
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString())::contains);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 删除公告
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.DEL_NOTICE)
    public WebResult<String> delNotice(@RequestBody DelNoticeDto dto) {
        log.info("收到删除公告的请求 dto = {}", dto);
        try {
            if (dto.ids() == null || dto.ids().isEmpty()) {
                return fail("common.paramerror");
            }

            long delCount = noticeDao.delNotice(dto.ids());
            if (delCount < 1) {
                log.debug("删除公告条数小于1，不通知hall节点 delCount = {}", delCount);
                return success("common.success");
            }

            PFMessage pfMessage = MessageUtil.getPFMessage(new NotifyLoadNoticeConfig());
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString())::contains);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 设置分享连接
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SHARE_URL_PREFIX)
    public WebResult<String> shareUrlPrefix(@RequestBody ShareUrlPrefixDto dto) {
        log.info("收到设置分享连接 dto = {}", dto);
        try {
            if (StringUtils.isBlank(dto.url())) {
                return fail("common.paramerror");
            }

            sharePromoteDao.setShareUrlPrefix(dto.url());
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 批量获取玩家信息
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.BATCH_GET_PLAYERS_INFO)
    public WebResult<List<PlayerAndAccountVo>> batchGetPlayersInfo(@RequestBody BatchGetPlayersInfoDto dto) {
        log.info("收到批量获取玩家信息 dto = {}", dto);
        try {
            if (dto.playerIds() == null || dto.playerIds().isEmpty()) {
                return fail("common.paramerror");
            }

            // 去重，避免重复查询
            List<Long> ids = dto.playerIds().stream()
                    .distinct()
                    .collect(Collectors.toList());

            if (ids.size() > 100) {
                log.warn("批量查询玩家数量不能超过100");
                return fail("common.paramerror");
            }
            List<Player> players = playerService.multiGetPlayer(ids);
            Map<Long, Account> accounts = accountDao.multiGetAccountMap(ids);

            List<PlayerAndAccountVo> list = new ArrayList<>();
            for (Player p : players) {
                PlayerAndAccountVo vo = new PlayerAndAccountVo();
                BeanUtils.copyProperties(p, vo);

                Account account = accounts.get(p.getId());
                if (account != null) {
                    BeanUtils.copyProperties(account, vo);
                }
                list.add(vo);
            }
            return success("common.success", list);
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 后台充值
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.BACKEND_RECHARGE)
    public WebResult<String> backendRecharge(@RequestBody BackendRechargeDto dto) {
        log.info("收到后台充值 dto = {}", dto);
        try {
            if (StringUtils.isBlank(dto.channelOrderId()) || dto.price() < 1 || dto.playerId() < 1 || dto.items() == null || dto.items().isEmpty()) {
                log.warn("参数错误 dto = {}", dto);
                return fail("common.paramerror");
            }

            RechargeType rechargeType = RechargeType.BACKEND;
            if (dto.rechargeType() > 0) {
                rechargeType = RechargeType.valueOf(dto.rechargeType());
                if (rechargeType == null) {
                    log.warn("参数错误,rechargeType无法识别 dto = {}", dto);
                    return fail("common.paramerror");
                }
            }

            List<Item> items = new ArrayList<>();
            for (ItemDto itemDto : dto.items()) {
                ItemCfg itemCfg = GameDataManager.getItemCfg(itemDto.id());
                if (itemCfg == null) {
                    log.warn("后台充值失败，未找到该道具 itemId = {}", itemDto.id());
                    return fail("common.paramerror");
                }

                if (itemDto.count() < 1) {
                    log.warn("后台充值失败，添加道具的数量不能小于1 itemId = {}", itemDto.id());
                    return fail("common.paramerror");
                }

                items.add(new Item(itemDto.id(), itemDto.count()));
            }

            //是否缓存充值节点
            ClusterClient client = clusterSystem.randClientByType(NodeType.RECHARGE);
            if (client == null) {
                log.warn("未找到充值节点 dto = {}", dto);
                return fail("common.paramerror");
            }

            GameRpcContext.getContext().withReqParameterBuilder(
                    RpcReqParameterBuilder.create()
                            .addClusterClient(client)
                            .setTryMillisPerClient(1000));

            Player player = playerService.get(dto.playerId());
            if (player == null) {
                log.warn("后台充值时，未找到玩家信息 dto = {}", dto);
                return fail("common.paramerror");
            }

            boolean add = orderService.putOrderId(dto.channelOrderId());
            if (!add) {
                log.warn("后台充值时，生成订单失败，该订单号已存在 dto = {}", dto);
                return fail("common.paramerror");
            }

            BigDecimal price = BigDecimal.valueOf(dto.price());
            Order order = orderService.generateOrder("htcz", player.getId(), price, rechargeType, items);
            if (order == null) {
                log.warn("后台充值时，生成订单失败 dto = {}", dto);
                return fail("common.paramerror");
            }

            //rpc调用充值
            int code = gmToRechargeBridge.recharge(order.getId(), dto.channelOrderId());
            if (code != Code.SUCCESS) {
                log.warn("后台充值失败 code = {}", code);
                return fail("common.fail");
            }
            log.info("后台充值成功 dto = {},orderId = {}", dto, order.getId());
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 设置链接
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SET_URL_PREFIX)
    public WebResult<String> setUrlPrefix(@RequestBody SetUrlPrefixDto dto) {
        log.info("收到设置链接 dto = {}", dto);
        try {
            if (StringUtils.isBlank(dto.url()) || dto.type() < 1) {
                return fail("common.paramerror");
            }
            commonDao.setValue(dto.type(), dto.url());
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    @RequestMapping(BackendGMCmd.EXPORT_SLOTS_LIB)
    public WebResult<String> exportSlotsLib(@RequestBody ExportSlotsLib dto) {
        log.info("收到导出结果库的请求 dto = {}", dto);
        try {
            if (dto.gameType() < 1) {
                return fail("common.paramerror");
            }

            Thread.ofVirtual().start(() -> {
                slotsLibDao.exportGameResultLib(dto.gameType());
            });

            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }


    /**
     * 绑定或解绑手机
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.PLAYER_BIND_PHONE)
    public WebResult<String> playerBindPhone(@RequestBody PlayerBindPhoneDto dto) {
        log.info("收到绑定手机或解绑消息 dto = {}", dto);
        try {
            if (dto.playerId() < 1 || (dto.type() != 1 && dto.type() != 2)) {
                log.warn("参数错误  dto = {}", dto);
                return fail("common.paramerror");
            }

            if (StringUtils.isNotBlank(dto.phone())) {
                String realPhone = CoreUtil.validPhoneNumber(dto.phone());
                if (StringUtils.isBlank(realPhone)) {
                    log.warn("手机号格式校验失败  dto = {}", dto);
                    return fail("common.paramerror");
                }
            }

            ClusterClient clusterClient;
            PlayerSessionInfo info = playerSessionService.getInfo(dto.playerId());
            if (info == null) {
                clusterClient = clusterSystem.randClientByType(NodeType.HALL);
            } else {
                clusterClient = clusterSystem.getClusterByPath(info.getCurrentNode());
            }

            if (clusterClient == null) {
                log.debug("绑定手机或解绑时，未找到对应的游戏节点");
                return fail("common.fail");
            }

            GameRpcContext.getContext().withReqParameterBuilder(
                    RpcReqParameterBuilder.create()
                            .addClusterClient(clusterClient)
                            .setTryMillisPerClient(1000));

            int bindCode = gmToHallBridge.playerBindPhone(dto.playerId(), dto.phone(), dto.type());
            if (bindCode != Code.SUCCESS) {
                log.warn("调用gmToHallBridge.playerBindPhone返回失败 client = {},errorCode = {}", clusterClient.marsNode.getNodePath(), bindCode);
                return fail("common.fail");
            }

            log.info("后台调用绑定或解绑手机成功 dto = {}", dto);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 获取sms配置
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.QUERY_SMS_CONFIG)
    public WebResult<List<SmsConfigInfo>> querySmsConfig() {
        log.info("收到查询sms配置的消息");
        try {
            return success("common.success", smsService.getAll());
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 保存sms配置
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.SAVE_SMS_CONFIG)
    public WebResult<String> saveSmsConfig(@RequestBody List<SmsConfigInfo> list) {
        log.info("收到保存sms配置的消息 list.size = {}", list == null ? null : list.size());
        try {
            smsService.save(list);
            List<ClusterClient> nodes = clusterSystem.getNodes(Set.of(NodeType.HALL.toString(), NodeType.ACCOUNT.toString()));
            if (nodes != null && !nodes.isEmpty()) {
                nodes.forEach(node -> {
                    GameRpcContext.getContext().withReqParameterBuilder(
                            RpcReqParameterBuilder.create()
                                    .addClusterClient(node)
                                    .setTryMillisPerClient(1000));

                    int code = gmToAllBridge.reload(ReloadType.SMS_CONFIG.getValue());
                    if (code == Code.SUCCESS) {
                        log.info("通知节点重新加载sms配置成功 nodePath = {}", node.marsNode.getNodePath());
                    } else {
                        log.info("通知节点重新加载sms配置失败 nodePath = {}", node.marsNode.getNodePath());
                    }
                });
            }
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 获取玩家最新的短信信息
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.PLAYER_LAST_SMS)
    public WebResult<VerCode> playerLastSms(@RequestBody PlayerLastSmsDto dto) {
        log.info("收到获取玩家最新的短信信息 dto = {}", dto);
        try {
            if (dto.playerId() < 1) {
                log.warn("参数错误 dto = {}", dto);
                return fail("common.fail");
            }

            return success("common.success", smsService.getSmsCodeByPlayerId(dto.playerId()));
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 给玩家发送短信
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.PLAYER_SMS)
    public WebResult<VerCode> playerSms(@RequestBody PlayerSmsDto dto) {
        log.info("收到后台给玩家发送短信 dto = {}", dto);
        try {
            if (dto.playerId() < 1 || StringUtils.isBlank(dto.phone())) {
                log.warn("参数错误 dto = {}", dto);
                return fail("common.fail");
            }

            VerCodeType vercodeType = VerCodeType.getType(dto.type());
            if (vercodeType == null) {
                log.warn("获取vercodeType失败 dto = {}", dto);
                return fail("common.fail");
            }

            //校验手机号格式
            String realPhone = CoreUtil.validPhoneNumber(dto.phone());
            if (StringUtils.isBlank(realPhone)) {
                log.warn("手机号格式校验失败  dto = {}", dto);
                return fail("common.paramerror");
            }

            Player player = playerService.get(dto.playerId());
            if (player == null) {
                log.warn("玩家不存在，发送短信失败  dto = {}", dto);
                return fail("common.paramerror");
            }

            VerCode vc = new VerCode();
            vc.setPlayerId(dto.playerId());
            vc.setVerCodeType(vercodeType);
            vc.setData(realPhone);
            CommonResult<VerCode> result = smsService.sendCode(vc);
            if (!result.success()) {
                log.warn("发送短信失败  dto = {}", dto);
                return fail("common.paramerror");
            }

            log.info("发送短信成功 dto = {},smsCode = {}", dto, result.data.getCode());
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 验证短信
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.VERIFY_PLAYER_SMS)
    public WebResult<VerCode> verifyPlayerSms(@RequestBody PlayerSmsDto dto) {
        log.info("收到后台验证短信 dto = {}", dto);
        try {
            if (dto.playerId() < 1 || StringUtils.isBlank(dto.phone()) || dto.smsCode() < 1) {
                log.warn("参数错误，验证短信失败 dto = {}", dto);
                return fail("common.fail");
            }

            VerCodeType vercodeType = VerCodeType.getType(dto.type());
            if (vercodeType == null) {
                log.warn("获取vercodeType失败，验证短信失败 dto = {}", dto);
                return fail("common.fail");
            }

            //校验手机号格式
            String realPhone = CoreUtil.validPhoneNumber(dto.phone());
            if (StringUtils.isBlank(realPhone)) {
                log.warn("手机号格式校验失败，验证短信失败  dto = {}", dto);
                return fail("common.paramerror");
            }

            VerCode vc = new VerCode();
            vc.setPlayerId(dto.playerId());
            vc.setVerCodeType(vercodeType);
            vc.setData(realPhone);
            vc.setCode(dto.smsCode());
            CommonResult<VerCode> result = smsService.verifySmsVerCode(vc);
            if (!result.success()) {
                log.warn("短信验证失败  dto = {}", dto);
                return fail("common.paramerror");
            }

            ClusterClient clusterClient;
            PlayerSessionInfo info = playerSessionService.getInfo(dto.playerId());
            if (info == null) {
                clusterClient = clusterSystem.randClientByType(NodeType.HALL);
            } else {
                clusterClient = clusterSystem.getClusterByPath(info.getCurrentNode());
            }

            if (clusterClient == null) {
                log.debug("后台验证短信成功后，未找到对应的游戏节点处理后续逻辑");
                return fail("common.fail");
            }

            GameRpcContext.getContext().withReqParameterBuilder(
                    RpcReqParameterBuilder.create()
                            .addClusterClient(clusterClient)
                            .setTryMillisPerClient(1000));

            int bindCode = gmToHallBridge.afterVerifySmsSuccess(dto.playerId(), dto.phone(), dto.type());
            if (bindCode != Code.SUCCESS) {
                log.debug("后台验证短信成功后，游戏节点处理后续逻辑失败 dto = {},errorCode = {}", dto, bindCode);
                return fail("common.fail");
            }
            log.info("短信验证成功 dto = {}", dto);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }


    //****************************************************************************************************************/

    /**
     * 修改玩家积分
     *
     * @param playerId 玩家id
     * @param value    变化值
     * @param flag     变化 true增加 false扣除
     */
    public void changePlayerPoints(long playerId, int value, boolean flag) {
        if (flag) {
            hallPointsAwardBridge.add(playerId, value, PointsAwardType.GM);

        } else {
            hallPointsAwardBridge.deduct(playerId, value, PointsAwardType.GM);
        }
    }

    /**
     * 检验玩家信息
     */
    private boolean checkPlayerInfo(QueryAccountDto dto, Player player, Account account) {
        //检查玩家id
        if (dto.playerId() > 0) {
            if (dto.playerId() != player.getId() || dto.playerId() != account.getPlayerId()) {
                return false;
            }
        }

        //检查注册的mac
        if (StringUtils.isNotEmpty(dto.registerMac())) {
            if (!dto.registerMac().equals(account.getRegisterMac())) {
                return false;
            }
        }

        //检查登录的mac
        if (StringUtils.isNotEmpty(dto.loginMac())) {
            if (!dto.loginMac().equals(account.getLastLoginMac())) {
                return false;
            }
        }

        //检查昵称
        if (StringUtils.isNotEmpty(dto.nickName())) {
            if (!dto.nickName().equals(player.getNickName())) {
                return false;
            }
        }

        //检查手机号
        if (StringUtils.isNotEmpty(dto.mobile())) {
            return dto.mobile().equals(account.getThirdAccount(LoginType.PHONE));
        }
        return true;
    }

    /**
     * 将请求对象转换为数据对象
     */
    public Carousel buildCarousel(CarouselDto dto) {
        Carousel carousel = new Carousel();
        carousel.setId(dto.id());
        carousel.setActivityImageType(dto.activityImageType());
        carousel.setSort(dto.sort());
        carousel.setShowType(dto.showType());
        carousel.setJumpType(dto.jumpType());
        carousel.setJumpValue(dto.jumpValue());
        carousel.setSourceName(dto.sourceName());
        return carousel;
    }

    private void addNodeConfig(List<GameNodeVo> nodeList, NodeType nodeType) {
        MarsNode marsNode = nodeManager.getMarNode(nodeType);
        if (marsNode == null) {
            return;
        }
        List<MarsNode> marsNodes = marsNode.getAllChildren();
        for (MarsNode node : marsNodes) {
            GameNodeVo vo = new GameNodeVo();
            vo.setType(node.getNodeConfig().getType());
            vo.setName(node.getNodeConfig().getName());
            vo.setTcpAddress(node.getNodeConfig().getTcpAddress());
            vo.setHttpAddress(node.getNodeConfig().getHttpAddress());
            vo.setWeight(node.getNodeConfig().getWeight());

            if (node.getNodeConfig().getWhiteIpList() != null && node.getNodeConfig().getWhiteIpList().length > 0) {
                vo.setWhiteIpList(Arrays.stream(node.getNodeConfig().getWhiteIpList()).toList());
            }
            if (node.getNodeConfig().getWhiteIdList() != null && node.getNodeConfig().getWhiteIdList().length > 0) {
                vo.setWhiteIdList(Arrays.stream(node.getNodeConfig().getWhiteIdList()).toList());
            }
            nodeList.add(vo);
        }
    }
}
