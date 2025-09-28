package com.jjg.game.gm.controller;

import com.alibaba.fastjson.JSONAware;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.config.AbstractExcelConfig;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.data.WebResult;
import com.jjg.game.core.pb.NotifyConfigUpdate;
import com.jjg.game.gm.dto.config.DeleteConfigDto;
import com.jjg.game.gm.dto.config.ReplaceConfigDto;
import com.jjg.game.gm.dto.config.SyncConfigDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 配置相关操作
 */
@RestController
@RequestMapping(value = "gm/config")
public class ConfigController extends AbstractController {

    private final ConfigManager configManager;

    private final ClusterSystem clusterSystem;

    public ConfigController(ConfigManager configManager, ClusterSystem clusterSystem) {
        this.configManager = configManager;
        this.clusterSystem = clusterSystem;
    }

    /**
     * 加载配置信息
     *
     * @param name 配置excel表名
     */
    @PostMapping(BackendGMCmd.Config.GET_CONFIG_LIST)
    public WebResult<List<AbstractExcelConfig>> getConfigList(@RequestBody String name) {
        try {
            if (name == null || name.isEmpty()) {
                return fail("common.paramerror");
            }
            WebResult<List<AbstractExcelConfig>> success = success("common.success");
            List<AbstractExcelConfig> configs = configManager.getConfigs(name);
            if (configs != null) {
                success.setData(configs.stream().toList());
            }
            return success;
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 覆盖配置数据
     */
    @PostMapping(BackendGMCmd.Config.REPLACE_CONFIG)
    public WebResult<String> replace(@RequestBody ReplaceConfigDto dto) {
        WebResult<String> success = success("common.success");
        List<String> configStrList = dto.configs().stream().map(JSONAware::toJSONString).toList();
        if (dto.name() == null || dto.name().isEmpty()) {
            return fail("common.paramerror");
        }
        if (configStrList.isEmpty()) {
            return fail("common.paramerror");
        }
        configManager.replaceConfigStrList(dto.name(), configStrList);
        notifyAllNode(dto.name());
        return success;
    }

    /**
     * 删除配置
     */
    @PostMapping(BackendGMCmd.Config.DELETE_CONFIG)
    public WebResult<String> delete(@RequestBody DeleteConfigDto dto) {
        WebResult<String> success = success("common.success");
        if (dto.name() == null || dto.name().isEmpty()) {
            return fail("common.paramerror");
        }
        List<Integer> ids = dto.ids();
        if (ids == null || ids.isEmpty()) {
            return fail("common.paramerror");
        }
        configManager.deleteConfig(dto.name(), ids);
        notifyAllNode(dto.name());
        return success;
    }

    /**
     * 同步单个配置表信息
     */
    @PostMapping(BackendGMCmd.Config.SYNC_CONFIG)
    public WebResult<String> sync(@RequestBody SyncConfigDto dto) {
        WebResult<String> success = success("common.success");
        if (dto.name() == null || dto.name().isEmpty()) {
            return fail("common.paramerror");
        }
        List<String> configs = dto.configs().stream().map(JSONAware::toJSONString).toList();
        if (configs.isEmpty()) {
            return fail("common.paramerror");
        }
        configManager.syncStrConfigs(dto.name(), configs);
        notifyAllNode(dto.name());
        return success;
    }

    /**
     * 通知所有节点更新配置数据。
     */
    private void notifyAllNode(String name) {
        NotifyConfigUpdate notifyConfigUpdate = new NotifyConfigUpdate();
        notifyConfigUpdate.setName(name);
        PFMessage pfMessage = MessageUtil.getPFMessage(notifyConfigUpdate);
        clusterSystem.notifyAllNode(pfMessage);
    }

}
