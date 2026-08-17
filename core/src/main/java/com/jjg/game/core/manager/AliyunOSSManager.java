package com.jjg.game.core.manager;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.logger.CoreLogger;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
public class AliyunOSSManager {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Value("${aliyun.oss.region:us-east-1}")
    private String region;
    @Value("${aliyun.oss.endpoint:https://oss-us-east-1.aliyuncs.com}")
    private String endpoint;
    @Value("${aliyun.oss.bucket-name:}")
    private String bucketName;
    @Value("${aliyun.oss.id:}")
    private String keyId;
    @Value("${aliyun.oss.secret:}")
    private String keySecret;

    @Autowired
    private CoreLogger logger;

    private OSSClient ossClient;

    public void init(){
        if (region.isEmpty() || bucketName.isEmpty()
                || keyId.isEmpty() || keySecret.isEmpty()) {
            log.warn("OSS配置不完整，OSS上传下载功能将不可用");
            return;
        }

        OSSClientBuilder builder = OSSClient.newBuilder()
                .credentialsProvider(new StaticCredentialsProvider(keyId, keySecret))
                .region(region);

        if (!endpoint.isEmpty()) {
            builder.endpoint(endpoint);
        }

        this.ossClient = builder.build();
    }

    @PreDestroy
    public void close() {
        if (this.ossClient == null) {
            return;
        }

        try {
            this.ossClient.close();
        } catch (Exception e) {
            log.error("关闭oss客户端异常", e);
        }
    }

    /**
     * 下载并替换配置文件
     *
     * @param nameList
     */
    public void dowmloadFiles(List<String> nameList) {
        if (nameList != null && !nameList.isEmpty()) {
            for (String name : nameList) {
                String[] arr = name.split("#");

                String replaceFileName = "";
                if (arr.length < 2) {
                    replaceFileName = name;
                    log.warn("name = {} 分割失败，所以直接下载 fileName = {}", name, replaceFileName);
                } else {
                    replaceFileName = arr[0];
                }
                this.dowmload(name, replaceFileName);
            }
        }
    }

    /**
     * 下载并替换配置文件
     *
     * @param originFileName
     * @param replaceFileName
     */
    public synchronized void dowmload(String originFileName, String replaceFileName) {
        try {
            if (this.ossClient == null) {
                log.warn("ossClient 为空，下载文件失败,请检查是否添加相关配置 originFileName = {},replaceFileName = {}", originFileName, replaceFileName);
                return;
            }

            String localPath = CoreConst.Common.SAMPLE_ROOT_PATH + replaceFileName;
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                log.debug("本地之前不存在该文件，所以不需要下载 originFileName = {},replaceFileName = {}", originFileName, replaceFileName);
                return;
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.newBuilder()
                    .bucket(this.bucketName)
                    .key(originFileName)
                    .build();

            //先创建到临时目录
            String tmpPath = originFileName;
            File tmpFile = new File(tmpPath);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }

            //写入到临时目录的文件
            try (GetObjectResult result = this.ossClient.getObject(getObjectRequest);
                 FileOutputStream fos = new FileOutputStream(tmpFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = result.body().read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            Path tempPath = Paths.get(tmpPath);
            Path targetPath = Paths.get(localPath);
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.addExcelConfigUpdate(originFileName);

            log.debug("替换文件成功  originFileName = {},replaceFileName = {}", originFileName, replaceFileName);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 上传文件到OSS
     *
     * @param file 要上传的文件
     * @return 上传成功返回true，失败返回false
     */
    public synchronized boolean upload(File file) {
        if (this.ossClient == null) {
            log.warn("ossClient 为空，上传文件失败");
            return false;
        }

        if (file == null || !file.exists()) {
            log.warn("文件不存在或为空，上传失败");
            return false;
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.newBuilder()
                    .bucket(this.bucketName)
                    .key(file.getName())
//                    .contentType(getContentType(fileName))
                    .build();

            // 使用文件路径上传，避免内存占用过大
            this.ossClient.putObjectFromFile(putObjectRequest, file);

            log.info("上传文件成功 fileName = {}", file.getName());
            return true;
        } catch (Exception e) {
            log.error("上传文件失败 fileName = {}", file, e);
            return false;
        }
    }
}
