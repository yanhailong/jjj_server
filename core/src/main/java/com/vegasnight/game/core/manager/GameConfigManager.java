package com.vegasnight.game.core.manager;

import com.alibaba.fastjson.JSONObject;
import com.vegasnight.game.common.monitor.FileLoader;
import com.vegasnight.game.common.monitor.FileMonitor;
import com.vegasnight.game.common.utils.CommonUtil;
import com.vegasnight.game.common.utils.FileHelper;
import com.vegasnight.game.core.constant.GameConstant;
import com.vegasnight.game.core.sample.Sample;
import com.vegasnight.game.core.sample.SampleConfig;
import com.vegasnight.game.core.sample.SampleFactory;
import com.vegasnight.game.core.sample.SampleReflectHelper;
import com.vegasnight.game.core.utils.ExcelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 游戏配置加载管理器
 * @author 11
 * @date 2025/5/27 10:14
 */
@Component
public class GameConfigManager implements FileLoader {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SampleConfig sampleConfig;

    @Autowired
    private FileMonitor fileMonitor;

    public void init() {
        log.debug("开始加载游戏配置...");

        String samplePath = sampleConfig.samplePath;
        if (samplePath == null) {
            log.warn("注意: 无法加载samplePath , samplePath 为空");
            return;
        }
        File sampleFile = new File(samplePath);
        loadFile(sampleFile);

        //监听文件变化
        fileMonitor.addDirectoryObserver(samplePath, this);
    }



    @Override
    public void load(File file, boolean isNew) {
        try {
            log.info("on file change filename = {},isNew={}", file.getName(), isNew);
            loadFile(file);
        } catch (Exception e) {
            log.warn("on file change err,filename={},isNew={}", file.getName(), isNew);
        }
    }

    /**
     * 加载所有的配置文件，包括xlsx和json
     * @param file
     */
    private void loadFile(File file) {
        if (file == null || !file.exists() || file.isHidden()
                || file.getName().endsWith(".svn")
                || file.getName().endsWith(".bak")) {
            return;
        }
        if (file.isDirectory()) {
            Arrays.asList(file.listFiles()).forEach(f -> loadFile(f));
            return;
        }
        String fileName = file.getName();
        if (fileName.endsWith(".xlsx")) {
            loadSampleByExcel(file);
        }else if(fileName.endsWith(".json")){
            loadJsonConfig(file);
        }
    }

    /**
     * 加载单个excel文件
     * @param file
     */
    public void loadSampleByExcel(File file) {
        try{
            Map<String, List<String[]>> sheetMap = ExcelUtil.readExcelFile(file);
            sheetMap.forEach((name, values) -> {
                if (values == null || values.size() <= 4) {
                    log.warn("cant use excel file {}, sheet {} 因为行数不够", file.getName(), name);
                } else {
                    String fileName = file.getName();
                    String pkg = fileName.substring(0, fileName.indexOf('.'));
                    int index = name.indexOf(".");
                    String className = name;
                    if (index != -1) {
                        className = name.substring(index + 1);
                    }
                    values.remove(0);// 第一行为说明，不要
                    values.remove(0);// 第2行为类型，不要
                    String[] names = values.remove(0); // 第3行为字段名称
                    String clazzName = sampleConfig.samplePackage + "." + className;
                    try {
                        Class<Sample> clazz = (Class<Sample>) Class.forName(clazzName);
                        List<Sample> samples = SampleReflectHelper.resolveSample(clazz, names,
                                values);
                        Field field = clazz.getField("factory");
                        SampleFactory factory = (SampleFactory) field.get(null);
                        factory.addSamples(samples);
                    } catch (Exception e) {
                        log.warn("parse file error, file is {}", fileName, e);
                    }
                }
            });
        }catch (Exception e){
            log.error("加载excel配置表失败 fileName = {}", file.getName(), e);
        }

    }

    /**
     * 加载config目录下的json配置，类似 HallConfig.json
     * @param file
     */
    private void loadJsonConfig(File file){
        try{
            String name = file.getName().replace(".json", "");
            Object bean = CommonUtil.getContext().getBean(name);

            String content = FileHelper.readFile(file, GameConstant.Common.ENCODING);
            JSONObject jsonObject = JSONObject.parseObject(content);

            BeanUtils.copyProperties(jsonObject.toJavaObject(bean.getClass()), bean);
        }catch (Exception e){
            log.error("加载微服务配置失败 fileName = {}", file.getName(), e);
        }
    }
}
