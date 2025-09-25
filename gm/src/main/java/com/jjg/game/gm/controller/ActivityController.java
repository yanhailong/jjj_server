package com.jjg.game.gm.controller;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.core.data.WebResult;
import com.jjg.game.gm.dto.activity.ActivityOpenChangeDto;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lm
 * @date 2025/9/19 17:03
 */
@RestController
@RequestMapping(value = "gm/activity")
public class ActivityController extends AbstractController {
    private final ClusterSystem clusterSystem;

    public ActivityController( ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    /**
     * 开启,关闭活动
     */
    @PutMapping("changeActivityStatus")
    public WebResult<Void> changeActivityStatus(@RequestBody ActivityOpenChangeDto changeDto) {
        try {
//            ActivityData activity = activityDao.getActivityById(changeDto.activityId());
//            if (activity == null) {
//                return fail("activity.not_found");
//            }
//            if (activity.isOpen() == changeDto.open()) {
//                return fail("activity.not_change");
//            }
//            activity.setOpen(changeDto.open());
//            activityDao.saveActivity(activity);
//            notifyActivityServerChange(activity.getId(), ActivityChangeEvent.ChangeType.UPDATE_ACTIVITY_OPEN);
            //通知其他服务器更新 带活动id
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

}

