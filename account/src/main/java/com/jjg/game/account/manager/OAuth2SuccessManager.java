package com.jjg.game.account.manager;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author 11
 * @date 2025/10/11 17:00
 */
@Component
public class OAuth2SuccessManager extends SimpleUrlAuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        System.out.println(JSON.toJSONString(oauth2User));

//        // 生成JWT
//        String token = generateJwtToken(oauth2User);
//
//        // 构建重定向URL (通常是客户端自定义的deep link或universal link)
//        String redirectUrl = UriComponentsBuilder.fromUriString("yourgame://oauth2callback")
//                .queryParam("token", token)
//                .queryParam("name", oauth2User.getAttribute("name"))
//                .queryParam("email", oauth2User.getAttribute("email"))
//                .build().toUriString();
//
//        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
