package com.jjg.game.gm.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.pb.NotifyKickout;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.*;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.gm.*;
import com.jjg.game.core.service.*;
import com.jjg.game.core.utils.MessageBuildUtil;
import com.jjg.game.gm.dto.*;
import com.jjg.game.gm.util.NetUtil;
import com.jjg.game.gm.vo.*;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
    private BlackListDao blackListDao;

    //邮件中的道具string，需要用正则匹配
    private final Pattern mailItemsPattern = Pattern.compile("\\[(\\d+),(\\d+)]");

    /**
     * 修改游戏状态
     */
    @RequestMapping(BackendGMCmd.CHANGE_GAME_STATUS)
    public WebResult<String> changeGameStatus(@RequestBody GameStatusDto dto) {
        try {
            log.info("收到修改游戏状态请求 dto = {}", dto);
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

            boolean saved = gameStatusService.saveOrUpdateGameStatus(new GameStatus(dto.name(),dto.number(),
                    dto.open(), dto.status(), dto.right_top_icon(), dto.icon_category(), dto.sort()));

            if (!saved) {
                log.info("修改游戏状态失败,无法保存到Redis , dto = {}", dto);
                return fail("common.fail");
            }
            //获取大厅节点
            List<ClusterClient> nodesByType = ClusterSystem.system.getNodesByType(NodeType.HALL);
            //构建请求消息
            ReqRefreshGameStatus msg = new ReqRefreshGameStatus();

            msg.cmdParam = new ObjectMapper().writeValueAsString(dto);

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
     *
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
     *
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
                account = accountDao.queryByPhone(dto.mobile());
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
            vo.setCreateTime(p.getCreateTime());
            vo.setRegisterMac(account.getRegisterMac());
            vo.setIsBan(account.getStatus());
            vo.setIsOffline(playerSessionService.hasSession(p.getId()) ? 0 : 1);
            vo.setMobile(account.getPhoneNumber());

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
     *
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
     *
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

            CommonResult<Player> result;
            if (dto.type() == 1) {  //增加
                if (dto.operator_type() == 1) { //账户
                    if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                        result = playerService.addGold(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    } else {  //钻石
                        result = playerService.addDiamond(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    }
                } else { //保险箱
                    if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                        result = playerService.addSafeBoxGold(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    } else {  //钻石
                        result = playerService.addSafeBoxDiamond(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    }
                }
            } else {  //减少
                if (dto.operator_type() == 1) { //账户
                    if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                        result = playerService.deductGold(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    } else {  //钻石
                        result = playerService.deductDiamond(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    }
                } else { //保险箱
                    if (dto.currency_id() == GameConstant.Item.TYPE_GOLD) { //金币
                        result = playerService.deductSafeBoxGold(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    } else {  //钻石
                        result = playerService.deductSafeBoxDiamond(dto.playerId(), dto.quantity(), "GM_GOLD_OPERATOR", dto.remark());
                    }
                }
            }

            if (!result.success()) {
                log.debug("修改货币时错误 ,playerId = {},code = {}", dto.playerId(), result.code);
                return fail("common.fail");
            }

            if (dto.operator_type() == 1) {  //如果是账户修改，则要进行通知
                PFSession session = playerSessionService.getSession(dto.playerId());
                if (session != null) {
                    NoticeBaseInfoChange notice = MessageBuildUtil.buildNoticeBaseInfoChange(result.data);
                    session.send(notice);
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
     * 踢出玩家
     *
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
     *
     */
    @RequestMapping(BackendGMCmd.BAN_ACCOUNT)
    public WebResult<String> banAccount(@RequestBody BanAccountDto dto) {
        try {
            log.info("收到后台封禁账号的请求 dto = {}", dto);
            if (dto.type() != 1 && dto.type() != 2) {
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
            for (String str : arr) {
                long playerId = Long.parseLong(str);

                if (dto.type() == 1) {  //封
                    accountDao.updateAccountStatus(playerId, GameConstant.AccountStatus.BAN);
                    PFSession session = playerSessionService.getSession(playerId);
                    if (session == null) {
                        continue;
                    }
                    session.send(notifyKickout);
                } else {  //解
                    accountDao.updateAccountStatus(playerId, GameConstant.AccountStatus.NORMAL);
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
     * 在线玩家
     *
     */
    @RequestMapping(BackendGMCmd.PLAYING_INFO)
    public WebResult<PageVo<List<OnlinePlayerVo>>> onlinePlayer(@RequestBody OnlinePlayerDto dto) {
        try {
            log.info("收到后台查询在线玩家的请求 dto = {}", dto);
            if (dto.gameId() < 1 || dto.registerChannel() < 0 || dto.pageSize() < 1 || dto.page() < 1) {
                log.debug("参数错误 dto = {}", dto);
                return fail("common.paramerror");
            }

            List<OnlinePlayer> list = onlinePlayerDao.query(dto.gameId(), dto.registerChannel(), dto.pageSize(), dto.page());
            if (list == null || list.isEmpty()) {
                return success("common.success");
            }

            List<Long> playerIds = new ArrayList<>();
            list.forEach(onlinePlayer -> {
                playerIds.add(onlinePlayer.getPlayerId());
            });

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
                }
                vo.setRegisterChannel(dto.registerChannel());
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
            ClusterClient clusterClient = null;
            if (StringUtils.isNotEmpty(param.nodeName())) {
                clusterClient = clusterSystem.getNodesByName(param.nodeName());
            } else {
                clusterClient = clusterSystem.randClientByType(NodeType.GAME, CoreConst.GameMajorType.SLOTS);
            }

            if (clusterClient == null) {
                log.debug("未找到对应的游戏节点");
                return fail("common.fail");
            }

            if(NodeType.GAME.name().equals(clusterClient.nodeConfig.getType()) &&
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

            shopProductDao.saveProducts(list);

            //通知大厅节点，商城商品变更
            PFMessage pfMessage = MessageUtil.getPFMessage(new NotifyShopProductChange());
            clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString(),NodeType.GAME.toString(),NodeType.RECHARGE.toString())::contains);

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
            if(dto.ids() != null) {
                dto.ids().removeIf(Objects::isNull);
                if(!dto.ids().isEmpty()){
                    if(dto.type() == 0){
                        blackListDao.addBlackIds(dto.ids());
                    }else {
                        blackListDao.removeBlackIds(dto.ids());
                    }
                    none = false;
                }
            }

            if(dto.ips() != null) {
                dto.ips().removeIf(ip -> {
                    ip = ip.trim();
                    return StringUtils.isEmpty(ip);
                });
                if(!dto.ips().isEmpty()){
                    boolean match = dto.ips().stream().allMatch(NetUtil::isValidIP);
                    if(!match){
                        log.debug("ip格式错误");
                        return fail("common.fail");
                    }
                    if(dto.type() == 0){
                        blackListDao.addBlackIps(dto.ips());
                    }else {
                        blackListDao.removeBlackIps(dto.ips());
                    }

                    none = false;
                }
            }

            if(none){
                log.debug("黑名单为空...");
                return fail("common.fail");
            }

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
        return success("common.success",nodeConfigList);
    }

    /**
     * 修改服务器信息
     *
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANG_GAME_NODE_INFO)
    public WebResult<String> changeGameServerInfo(@RequestBody ChangeNodeDto dto) {
        log.info("收到修改服务器列表信息 dto = {}",dto);

        try{
            if(dto.name() == null || dto.name().isEmpty()){
                log.debug("修改服务器信息错误,节点名不能为空 dto = {}",dto);
                return fail("common.paramerror");
            }
            ClusterClient clusterClient = clusterSystem.getNodesByName(dto.name());
            if(clusterClient == null){
                log.debug("修改服务器信息错误,未找到该节点 dto = {}",dto);
                return fail("common.paramerror");
            }

            NotifyGameNodeChange notify = new NotifyGameNodeChange();
            notify.weight = dto.weight();
            notify.ips = dto.whiteIpList();
            notify.ids = dto.whiteIdList();

            PFMessage pfMessage = MessageUtil.getPFMessage(notify);
            clusterClient.write(new ClusterMessage(pfMessage));
            return success("common.success");
        }catch (Exception e){
            log.error("", e);
            return fail("common.exception");
        }

    }


    //****************************************************************************************************************/

    /**
     *
     * 检验玩家信息
     *
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
            if (!dto.mobile().equals(account.getPhoneNumber())) {
                return false;
            }
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

            if(node.getNodeConfig().getWhiteIpList() != null && node.getNodeConfig().getWhiteIpList().length > 0){
                vo.setWhiteIpList(Arrays.stream(node.getNodeConfig().getWhiteIpList()).toList());
            }
            if(node.getNodeConfig().getWhiteIdList() != null && node.getNodeConfig().getWhiteIdList().length > 0){
                vo.setWhiteIdList(Arrays.stream(node.getNodeConfig().getWhiteIdList()).toList());
            }
            nodeList.add(vo);
        }
    }
}
