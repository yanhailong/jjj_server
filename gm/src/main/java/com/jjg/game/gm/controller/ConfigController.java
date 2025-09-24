package com.jjg.game.gm.controller;

import com.jjg.game.core.config.AbstractExcelConfig;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.data.WebResult;
import com.jjg.game.gm.dto.config.DeleteConfigDto;
import com.jjg.game.gm.dto.config.ReplaceConfigDto;
import org.springframework.web.bind.annotation.GetMapping;
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

    public ConfigController(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 加载配置信息
     *
     * @param name 配置excel表名
     */
    @GetMapping(BackendGMCmd.Config.GET_CONFIG_LIST)
    public WebResult<List<AbstractExcelConfig>> getConfigList(String name) {
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
    @GetMapping(BackendGMCmd.Config.REPLACE_CONFIG)
    public WebResult<String> replace(ReplaceConfigDto dto) {
        WebResult<String> success = success("common.success");
        List<String> configStrList = dto.configs();
        if (dto.name() == null || dto.name().isEmpty()) {
            return fail("common.paramerror");
        }
        if (configStrList == null || configStrList.isEmpty()) {
            return fail("common.paramerror");
        }
        configManager.replaceConfigStrList(dto.name(), configStrList);
        return success;
    }

    /**
     * 删除配置
     */
    @GetMapping(BackendGMCmd.Config.DELETE_CONFIG)
    public WebResult<String> delete(DeleteConfigDto dto) {
        WebResult<String> success = success("common.success");
        if (dto.name() == null || dto.name().isEmpty()) {
            return fail("common.paramerror");
        }
        List<Integer> ids = dto.ids();
        if (ids == null || ids.isEmpty()) {
            return fail("common.paramerror");
        }
        configManager.deleteConfig(dto.name(), ids);
        return success;
    }

}
