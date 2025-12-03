package com.jjg.game.gm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许跨域访问所有接口
        registry.addMapping("/**")
                // 允许所有来源进行跨域访问 (建议按需配置，例如只允许前端域名)
                .allowedOriginPatterns("*")

                // 允许所有请求方法 (GET, POST, PUT, DELETE等)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

                // 允许携带任何请求头
                .allowedHeaders("*")

                // 是否允许发送 Cookie（必须与前端的 withCredentials = true 配合）
                // 如果需要携带认证信息（如 session/cookie），必须设置为 true
                .allowCredentials(true)

                // 预检请求（OPTIONS）的缓存时间，单位：秒。
                // 在这段时间内，浏览器不需要发送新的预检请求
                .maxAge(3600);
    }
}
