package com.jjg.game.gm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.activity.common.dao.ActivityDao;
import com.jjg.game.activity.common.dao.ActivityDetailDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.data.WebResult;
import com.jjg.game.core.pb.activity.NotifyActivityServerChange;
import com.jjg.game.gm.dto.activity.ActivityDetailInfoDto;
import com.jjg.game.gm.dto.activity.ActivityStatusChangeDto;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/19 17:03
 */
@RestController
@RequestMapping(value = "gm/activity")
public class ActivityController extends AbstractController {
    private final ActivityDao activityDao;
    private final ActivityDetailDao activityDetailDao;
    private final ClusterSystem clusterSystem;

    public ActivityController(ActivityDao activityDao, ActivityDetailDao activityDetailDao, ClusterSystem clusterSystem) {
        this.activityDao = activityDao;
        this.activityDetailDao = activityDetailDao;
        this.clusterSystem = clusterSystem;
    }

    /**
     * 获取全部活动列表
     */
    @GetMapping(BackendGMCmd.GET_ALL_ACTIVITY_DATA)
    public WebResult<List<ActivityData>> getAllActivityData() {
        try {
            WebResult<List<ActivityData>> success = success("common.success");
            success.setData(activityDao.getAllActivityInfos());
            return success;
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 根据ID获取单个活动
     */
    @GetMapping("/{id}")
    public WebResult<ActivityData> getActivityById(@PathVariable("id") long id) {
        try {
            ActivityData activity = activityDao.getActivityById(id);
            if (activity == null) {
                return fail("activity.not_found");
            }
            WebResult<ActivityData> success = success("common.success");
            success.setData(activity);
            return success;
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 新增活动
     */
    @PostMapping
    public WebResult<ActivityData> createActivity(@RequestBody ActivityData activityData) {
        try {
            if (activityData.getStatus() != 1) {
                return fail("activity.status");
            }
            //开发类型时间判断
            if (activityData.getOpenType() == ActivityConstant.Common.OPEN_SERVER_TYPE) {
                if (activityData.getDuration() <= 0 || activityData.getTimeEnd() > 0 || activityData.getTimeStart() > 0) {
                    return fail("activity.time_end");
                }
            }
            long currentTimeMillis = System.currentTimeMillis();
            //开发类型时间判断
            if (activityData.getOpenType() == ActivityConstant.Common.LIMIT_TYPE) {
                if (activityData.getDuration() > 0 || activityData.getTimeEnd() <= 0 || activityData.getTimeStart() <= 0) {
                    return fail("activity.time_end");
                }
                if (activityData.getTimeEnd() <= activityData.getTimeStart() || activityData.getTimeStart() <= currentTimeMillis || activityData.getTimeEnd() <= currentTimeMillis) {
                    return fail("activity.time_end");
                }
            }
            ActivityData data = activityDao.getActivityById(activityData.getId());
            if (data != null) {
                return fail("activity.exist");
            }
            activityDao.saveActivity(activityData);
            notifyActivityServerChange(activityData.getId(), 1);
            //通知其他服务器更新 带活动id
            WebResult<ActivityData> success = success("common.success");
            success.setData(activityData);
            return success;
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 更新活动
     */
    @PutMapping("/{id}")
    public WebResult<ActivityData> updateActivity(@PathVariable("id") long id,
                                                  @RequestBody ActivityData activityData) {
        try {
            ActivityData old = activityDao.getActivityById(id);
            if (old == null) {
                return fail("activity.not_found");
            }
            activityData.setId(id); // 保证id一致
            activityDao.saveActivity(activityData);
            //通知其他服务器更新 带活动id
            notifyActivityServerChange(id, 1);
            WebResult<ActivityData> success = success("common.success");
            success.setData(activityData);
            return success;
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 开启,关闭活动
     */
    @PutMapping("changeActivityStatus")
    public WebResult<Void> changeActivityStatus(@RequestBody ActivityStatusChangeDto changeDto) {
        try {

            ActivityData activity = activityDao.getActivityById(changeDto.activityId());
            if (activity == null) {
                return fail("activity.not_found");
            }
            if (activity.getStatus() == changeDto.status()) {
                return fail("activity.not_change");
            }
            activity.setStatus(changeDto.status());
            activityDao.saveActivity(activity);
            notifyActivityServerChange(activity.getId(), 1);
            //通知其他服务器更新 带活动id
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 删除活动
     */
    @DeleteMapping("/{id}")
    public WebResult<Void> deleteActivity(@PathVariable("id") long id) {
        try {
            boolean deleted = activityDao.deleteActivity(id);
            if (!deleted) {
                return fail("activity.not_found");
            }
            notifyActivityServerChange(id, 3);
            //通知其他服务器更新 带活动id
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 导入活动详情数据
     */
    @PutMapping("/importActivityDetail")
    public WebResult<Void> importActivityDetail(@RequestBody ActivityDetailInfoDto detailInfoDto) {
        try {
            ActivityType activityType = ActivityType.fromType(detailInfoDto.activityType());
            if (activityType == null) {
                return fail("activity.not_found_activity_type");
            }
            if (StringUtils.isEmpty(detailInfoDto.cfgInfos())) {
                return fail("activity.cfg_infos.not_found");
            }
            ActivityData activity = activityDao.getActivityById(detailInfoDto.activityId());
            if (activity == null) {
                return fail("activity.not_found");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            List<BaseCfgBean> readValue = objectMapper.readValue(detailInfoDto.cfgInfos(), objectMapper.getTypeFactory().constructCollectionType(
                    List.class,
                    activityType.getController().getDetailDataClass()
            ));
            Map<Integer, BaseCfgBean> detailMap = readValue.stream().collect(Collectors.toMap(BaseCfgBean::getId, d -> d));
            activityDetailDao.saveActivityDetails(activity.getId(), detailMap);
            //通知其他服务器更新 活动id
            notifyActivityServerChange(activity.getId(), 2);
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    private void notifyActivityServerChange(long activity, int type) {
        List<ClusterClient> nodesByType = clusterSystem.getNodesByType(NodeType.HALL);
        nodesByType.addAll(clusterSystem.getNodesByType(NodeType.GAME));
        if (nodesByType.isEmpty()) {
            return;
        }
        NotifyActivityServerChange notify = new NotifyActivityServerChange();
        notify.activityId = activity;
        notify.operationType = type;
        PFMessage pfMessage = MessageUtil.getPFMessage(notify);
        ClusterMessage msg = new ClusterMessage(pfMessage);
        for (ClusterClient clusterClient : nodesByType) {
            try {
                clusterClient.write(msg);
            } catch (Exception e) {
                log.error("通知其他节点活动更新数据失败 path:{} activityId:{} type:{}", clusterClient.nodeConfig.getName(), activity, type, e);
            }
        }
    }
}

