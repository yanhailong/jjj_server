package com.jjg.game.logic.robot.entity;

import com.jjg.game.core.robot.RobotThread;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author 2CL
 * @function 机器人玩家实例(数据)
 */
public class RobotPlayer {

    private RobotThread robot;

    private PlayerInfo playerInfo;

    /**
     * 登录时间
     */
    private long loginTime;
    /**
     * 操作的商店id
     */
    private int selectedShopId;
    /**
     * 操作的商品id
     */
    private int goodsId;
    /**
     * 刷新操作的商店
     */
    private int refreshShopId;

    /**
     * 看板娘
     */
    private int interfaceGirlId;
    /**
     * 执行精灵模块操作时 操作的精灵id(每一次流程,会选择一个精灵)
     */
    private int currentOperatedHeroId;
    /**
     * 精灵升级请求时的道具ID
     */
    private int heroLevelUpItemId = 0;
    /**
     * 精灵升级请求时的道具ID
     */
    private int heroLevelUpItemNum = 0;
    /**
     * 精灵时装切换
     */
    private int heroReqDressId = 0;
    /**
     * 精灵请求升级技能id
     */
    private int heroReqSkillUpId = 0;
    /**
     * 玩家指定的自定义阵容
     */
    private int specifiedDiyFormationIndex;
    /**
     * 执行精灵质点模块:当前操作的装备id
     */
    private String currentEquipId;
    /**
     * 执行精灵质点模块:当前操作的装备模板id
     */
    private int currentEquipTemplateId;
    /**
     * 强化所需的道具消耗
     */
    private Map<Integer, Integer> heroStrengthItem = new HashMap<>();
    /**
     * 分解操作的装备模板id
     */
    private int transformEquipTemplateId;

    /**
     * 白天还是黑夜 *
     */
    private int dayType;

    private boolean isLogin;

    //    private S2CPlayerMsg.FormationInfoList formationList;

    // 临时数据
    /**
     * 分解的道具
     */
    public int transformItem;
    /**
     * 下一次对话ID
     */
    private LinkedList<Integer> nextDialogIdList = new LinkedList<>();
    // 主线副本
    /**
     * 正在进行的关卡
     */
    public int mainPlotDungeon;

    /** ---------------抽卡相关--------------- */
    /**
     * 普通抽卡ID
     */
    private int normalSummonId;
    /**
     * 普通抽卡次数
     */
    private int normalSummonTimes;
    /**
     * 自选抽卡ID
     */
    private int selfSelectSummonId;
    /**
     * 先抽后付ID
     */
    private int payAfterSummonId;
    /**
     * 先抽后付次数
     */
    private int payAfterSummonTimes;

    //  public WorldSkillData worldSkillData = new WorldSkillData();

    public RobotPlayer(RobotThread robotThread) {
        this.robot = robotThread;
        this.playerInfo = new PlayerInfo();
    }

    public RobotThread getRobot() {
        return robot;
    }

    public boolean getIsLogin() {
        return this.isLogin;
    }

    public void setRobot(RobotThread robot) {
        this.robot = robot;
    }

    public void setLogin(boolean isLogin) {
        this.isLogin = isLogin;
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

    public void setPlayerInfo(PlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }
}
