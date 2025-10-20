package com.jjg.game.core.manager;

import com.jjg.game.common.constant.CoreConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * @author 11
 * @date 2025/10/20 14:20
 */
@Component
public class AmazonBucketManager {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Autowired(required = false)
    private S3Client s3Client;

    /**
     * 下载并替换配置文件
     *
     * @param nameList
     */
    public void dowmloadFiles(List<String> nameList) {
        if (nameList != null && !nameList.isEmpty()) {
            nameList.forEach(this::dowmload);
        }
    }

    /**
     * 下载并替换配置文件
     *
     * @param fileName
     */
    public void dowmload(String fileName) {
        try {
            if (this.s3Client == null) {
                log.warn("s3Client 为空，下载文件失败,请检查是否添加相关配置 fileName = {}", fileName);
                return;
            }

            String localPath = CoreConst.Common.SAMPLE_ROOT_PATH + fileName;
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                log.debug("本地之前不存在该文件，所以不需要下载 fileName = {}", fileName);
                return;
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(fileName)
                    .build();

            ResponseInputStream<GetObjectResponse> resp = s3Client.getObject(getObjectRequest);

            //先创建到临时目录
            String tmpPath = fileName;
            File tmpFile = new File(tmpPath);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }

            //写入到临时目录的文件
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = resp.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            Path tempPath = Paths.get(tmpPath);
            Path targetPath = Paths.get(localPath);
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.debug("替换文件成功  fileName = {}", fileName);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
