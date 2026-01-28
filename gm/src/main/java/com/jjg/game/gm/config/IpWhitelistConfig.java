package com.jjg.game.gm.config;

import com.jjg.game.gm.interceptor.IpWhitelistInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class IpWhitelistConfig implements WebMvcConfigurer {

    @Autowired
    private IpWhitelistInterceptor ipWhitelistInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册IP白名单拦截器
        // 拦截所有请求，排除静态资源
        registry.addInterceptor(ipWhitelistInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/static/**",
                        "/resources/**",
                        "/webjars/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/fonts/**"
                );
    }
}
