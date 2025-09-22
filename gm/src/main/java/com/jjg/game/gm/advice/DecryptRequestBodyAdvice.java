package com.jjg.game.gm.advice;

import com.jjg.game.gm.exception.DecryptException;
import com.jjg.game.gm.util.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@ControllerAdvice
public class DecryptRequestBodyAdvice implements RequestBodyAdvice {


    private static final Logger log = LoggerFactory.getLogger(DecryptRequestBodyAdvice.class);

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return false; // 全局启用，也可以判断注解开关
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        //获取请求体
        String encryptedBody = new BufferedReader(new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        String decryptedBody = null;
        try {
            decryptedBody = CryptoUtils.getDecryptRequest(encryptedBody);
        } catch (Exception e) {
            log.error("请求解密失败", e);
        }
        if (decryptedBody == null) {
            throw new DecryptException();
        }
        String finalDecryptedBody = decryptedBody;
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(finalDecryptedBody.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}
