package com.postion.airlineorderbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // (推荐) 使用新的Lambda DSL配置CSRF (跨站请求伪造) ，更清晰
                .csrf(AbstractHttpConfigurer::disable)
                // 配置授权规则
                .authorizeHttpRequests(authz -> authz
                        // 明确放行所有公共路径
                        .requestMatchers(
                                // 明确放行 API 认证路径和 Swagger
                                new AntPathRequestMatcher("/api/auth/**"),
                                new AntPathRequestMatcher("/swagger-ui.html"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/webjars/**")
                        ).permitAll()

                        // 放行所有前端静态资源
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/index.html"),
                                new AntPathRequestMatcher("/*.js"),
                                new AntPathRequestMatcher("/*.css"),
                                new AntPathRequestMatcher("/*.ico"),
                                new AntPathRequestMatcher("/*.png"),
                                new AntPathRequestMatcher("/assets/**")
                        ).permitAll()

                        // 核心修复：放行所有看起来像前端路由的路径
                        // 让它们有机会被 WebConfig 转发
                        .requestMatchers(new RegexRequestMatcher("^/(?!api|assets|webjars|swagger-ui|v3/api-docs).*$", null)).permitAll()

                        // 剩下的所有请求（主要是 /api/ 路径下的其他接口）都需要认证
                        .anyRequest().authenticated()
                )
                // 配置会话管理为无状态，因为我们用JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 关联我们自定义的AuthenticationProvider
                .authenticationProvider(authenticationProvider())
                // 在UsernamePasswordAuthenticationFilter之前添加我们的JWT过滤器
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 注意：CORS(跨源资源共享)的配置在这里，但securityFilterChain中并没有显式调用.cors()
    // Spring Boot会自动寻找名为corsConfigurationSource的Bean并应用它
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // (重要修改) 允许任何来源，或您服务器的公网IP。用"*"在开发和测试中最方便
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}