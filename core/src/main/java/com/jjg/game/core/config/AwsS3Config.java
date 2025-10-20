package com.jjg.game.core.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author 11
 * @date 2025/10/20 14:23
 */
@Configuration
public class AwsS3Config {
    @Value("${aws.credentials.access-key:}")
    private String accessKey;
    @Value("${aws.credentials.secret-key:}")
    private String secretKey;
    @Value("${aws.s3.region:}")
    private String region;
    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        if(StringUtils.isEmpty(this.accessKey) || StringUtils.isEmpty(this.secretKey) ||
                StringUtils.isEmpty(this.region) || StringUtils.isEmpty(this.bucketName)) {
            return null;
        }

        return S3Client.builder()
                .region(Region.of(this.region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(this.accessKey, this.secretKey)
                        )
                )
                .build();
    }
}
