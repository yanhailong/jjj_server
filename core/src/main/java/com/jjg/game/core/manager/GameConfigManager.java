package com.jjg.game.core.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.monitor.FileLoader;
import com.jjg.game.common.monitor.FileMonitor;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.FileHelper;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.sample.Sample;
import com.jjg.game.core.sample.SampleConfig;
import com.jjg.game.core.sample.SampleFactory;
import com.jjg.game.core.sample.SampleReflectHelper;
import com.jjg.game.core.utils.ExcelUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private NodeManager nodeManager;

    private final String configPath = "config";
    private final String nodeConfigName = "nodeConfig.json";

    public void init() {
        log.debug("开始加载游戏配置...");
        String samplePath = sampleConfig.getSamplePath();
        if (samplePath == null) {
            log.warn("注意: 无法加载samplePath , samplePath 为空");
            return;
        }
        File sampleFile = new File(samplePath);
        loadFile(sampleFile,false);
        //监听文件变化
        fileMonitor.addDirectoryObserver(samplePath, this);

        File configFile = new File(configPath);
        loadFile(configFile,false);
        //监听文件变化
        fileMonitor.addDirectoryObserver(configPath, this);
    }

    @Override
    public void load(File file, boolean isNew) {
        try {
            log.info("on file change filename = {},isNew={}", file.getName(), isNew);
            loadFile(file,true);
        } catch (Exception e) {
            log.warn("on file change err,filename={},isNew={}", file.getName(), isNew);
        }
    }

    /**
     * 加载所有的配置文件，包括xlsx和json
     * @param file
     */
    public void loadFile(File file,boolean change) {
        if (file == null || !file.exists() || file.isHidden()
                || file.getName().endsWith(".svn")
                || file.getName().endsWith(".bak")
                || file.getName().startsWith("~$")) {
            return;
        }
        if (file.isDirectory()) {
            Arrays.asList(file.listFiles()).forEach(f -> loadFile(f,change));
            return;
        }
        String fileName = file.getName();
        if (fileName.endsWith(".xlsx")) {
            String className = loadSampleByExcel(file);

            if(StringUtils.isNotEmpty(className) && change) {
                Map<String, ConfigExcelChangeListener> map = CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
                map.forEach((k,v) -> {
                    v.change(className);
                });
            }
        }else if(fileName.endsWith(".json")){
            loadJsonConfig(file);
        }
    }

    /**
     * 加载单个excel文件
     * @param file
     */
    public String loadSampleByExcel(File file) {
        try{
            Map<String, List<String[]>> sheetMap = ExcelUtil.readExcelFile(file);
            for(Map.Entry<String, List<String[]>> en : sheetMap.entrySet()){
                String name = en.getKey();
                List<String[]> values = en.getValue();
                if (values == null || values.size() <= 3) {
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
                    String clazzName = sampleConfig.getSamplePackage() + "." + className;
                    try {
                        Class<Sample> clazz = (Class<Sample>) Class.forName(clazzName);
                        List<Sample> samples = SampleReflectHelper.resolveSample(clazz, names,values);
                        if(samples.size() < 1){
                            log.warn("该配置表没有数据 configName = {},size = {}", name, samples.size());
                        }

                        Field field = clazz.getField("factory");
                        SampleFactory factory = (SampleFactory) field.get(null);
                        factory.addSamples(samples);
                        return className;
                    } catch (Exception e) {
                        log.warn("parse file error, file is {}", fileName, e);
                    }
                }
            }
        }catch (Exception e){
            log.error("加载excel配置表失败 fileName = {}", file.getName(), e);
        }
        return null;

    }

    /**
     * 加载config目录下的json配置，类似 HallConfig.json
     * @param file
     */
    private void loadJsonConfig(File file){
        try{
            String fileName = file.getName();
            String content = FileHelper.readFile(file, GameConstant.Common.ENCODING);
            JSONObject jsonObject = JSONObject.parseObject(content);

            if(nodeConfigName.equalsIgnoreCase(fileName)){
                readNodeConfigUpdate(jsonObject);
            }else {
                String name = fileName.replace(".json", "");
                Object bean = CommonUtil.getContext().getBean(name);
                BeanUtils.copyProperties(jsonObject.toJavaObject(bean.getClass()), bean);
            }
        }catch (NoSuchBeanDefinitionException e1){

        }catch (Exception e){
            log.error("加载微服务配置失败 fileName = {}", file.getName(), e);
        }
    }

    private void readNodeConfigUpdate(JSONObject jsonObject){
        Integer wight = jsonObject.getInteger("weight");
        if (wight != null) {
            nodeConfig.setWeight(wight);
        }

        Boolean open = jsonObject.getBoolean("open");
        if (open != null) {
            nodeConfig.setOpen(open);
        }

        Integer masterWeight = jsonObject.getInteger("masterWeight");
        if (masterWeight != null) {
            nodeConfig.setWeight(masterWeight);
        }

        JSONArray whiteIpArray = jsonObject.getJSONArray("whiteIpList");
        if (whiteIpArray != null) {
            nodeConfig.setWhiteIpList(whiteIpArray.toArray(new String[0]));
        }

        JSONArray whiteIdArray = jsonObject.getJSONArray("whiteIdList");
        if (whiteIdArray != null) {
            nodeConfig.setWhiteIdList(whiteIdArray.toArray(new String[0]));
        }

        nodeManager.update();
    }
}
