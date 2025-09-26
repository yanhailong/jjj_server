package com.jjg.game.gm.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.data.WebResult;
import com.jjg.game.core.pb.ReqActivityInfos;
import com.jjg.game.core.utils.NodeCommunicationUtil;
import com.jjg.game.gm.vo.ActivityInfoVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jjg.game.core.constant.BackendGMCmd.GET_ALL_ACTIVITY_DATA;

/**
 * @author lm
 * @date 2025/9/19 17:03
 */
@RestController
@RequestMapping(value = "gm/activity")
public class ActivityController extends AbstractController {
    private final ClusterSystem clusterSystem;

    public ActivityController(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    /**
     * 开启,关闭活动
     */
    @GetMapping(GET_ALL_ACTIVITY_DATA)
    public WebResult<List<ActivityInfoVo>> getAllActivityData() {
        try {
            //通知其他服务器更新 带活动id
            ReqActivityInfos infos = new ReqActivityInfos();
            List<ClusterClient> clientList = clusterSystem.getNodesByType(NodeType.HALL);
            if (clientList.isEmpty()) {
                return fail("fail");
            }
            ClusterClient clusterClient = RandomUtil.randomEle(clientList);
            Object jsonData = NodeCommunicationUtil.sendAndGetResult(clusterClient, infos);
            if (!(jsonData instanceof String)) {
                return fail("fail");
            }
            List<ActivityInfoVo> activityDataList = JSONObject.parseArray((String) jsonData, ActivityInfoVo.class);
            WebResult<List<ActivityInfoVo>> success = success("common.success");
            success.setData(activityDataList);
            return success;
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

}

