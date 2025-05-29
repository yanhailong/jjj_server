package com.vegasnight.game.logserver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author 11
 * @date 2025/5/27 17:57
 */
@Component
@EnableScheduling
public class ClearManager {
    private Logger log = LoggerFactory.getLogger(getClass());

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

    @Autowired
    public LogCofig logConfig;

    /**
     * 每天凌晨4点半定时执行
     */
    @Scheduled(cron = "0 0 0/2 * * ? ")
    public void clearLogFile(){
        if(logConfig.getFilebeatPath() == null || logConfig.getFilebeatPath().length() < 1){
            return;
        }

        try{
            File file = new File(logConfig.getFilebeatPath());
            if(!file.exists()){
                return;
            }

            File newFile = new File("log/beatTempLog.json");

            newFile = FileUtil.copy(file,newFile,true);

            FileReader fileReader = new FileReader(newFile);
            List<String> list = fileReader.readLines();

            String currentLogFileName = logConfig.getGameLogPrefix() + ".log";

            LocalDateTime clearTime = LocalDateTime.now().plusHours(-logConfig.getClearExpireHours());

            int move = 0;
            for(String str : list){
                JSONObject json = JSONObject.parseObject(str);
                if(!json.containsKey("v")){
                    continue;
                }

                JSONObject vJson = json.getJSONObject("v");
                String source = vJson.getString("source");
                File logFile = new File(source);
                if(!logFile.exists()){
                    continue;
                }

                //查看日志文件是否消费完毕
                long length = logFile.length();
                long offset = vJson.getLongValue("offset");
                if(length < offset){
                    continue;
                }


                String fileName = logFile.getName();
                if(fileName.equals(currentLogFileName)){
                    continue;
                }

                //检查文件日期
                if(!fileName.startsWith(logConfig.getGameLogPrefix())){
                    continue;
                }

                //比较时间
                String logFileTime = fileName.replaceAll(logConfig.getGameLogPrefix() + ".","");
                logFileTime = logFileTime.replaceAll(".log","");

                LocalDateTime localDateTime = LocalDateTime.parse(logFileTime, format);
                if(clearTime.isBefore(localDateTime)){
                    continue;
                }

                File moveFile = new File("complete/" + fileName);
                FileUtil.move(logFile,moveFile,true);
                move++;
                log.info("{}已读取完成  转移到complete文件夹",logFile.getName());
            }
            log.info("日志文件检查结束 moveSize={}",move);
        }catch (Exception e){
            log.error("",e);
        }
    }
}
