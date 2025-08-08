package com.postion.airlineorderbackend.config; // 确保包名正确

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ErrorPageConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return container -> {
            // 创建一个针对 404 Not Found 错误的自定义错误页面
            ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/index.html");

            // 将这个错误页面注册到容器中
            container.addErrorPages(error404Page);
        };
    }
}