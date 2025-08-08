package com.postion.airlineorderbackend.config; // 确保包名正确

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 这个规则是解决 SPA 路由问题的“银弹”
        // 它会匹配所有不包含 "." 的路径
        // (例如 /orders, /users/1, 但不会匹配 /main.js 或 /styles.css)
        // 并将它们全部在服务器内部转发给 /index.html。
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}