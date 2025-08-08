package com.postion.airlineorderbackend.config; // 确保包名正确

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 这条规则负责处理单层的前端路由，如 /orders, /dashboard
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");

        // (关键新增) 这条规则负责处理所有深层的前端路由
        // "/**" 匹配多层路径
        // "{path:[^\\.]*}" 确保最后一节路径不含点号
        registry.addViewController("/**/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}