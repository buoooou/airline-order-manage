package com.postion.airlineorderbackend.service;

import com.postion.airlineorderbackend.service.impl.JwtServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        // 因为 @Value 在单元测试中无效，我们使用反射手动设置这些私有字段
        // 使用一个足够长的 Base64 编码密钥以满足 HMAC-SHA 算法的要求
        String testSecret = Base64.getEncoder().encodeToString("my-test-secret-key-that-is-long-enough-for-hs256".getBytes());
        long expirationMs = 3600000; // 1小时

        ReflectionTestUtils.setField(jwtService, "secretKeyString", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expirationMs);

        // 创建一个测试用的 UserDetails 对象
        userDetails = new User("testuser", "password", new ArrayList<>());
    }

    @Test
    @DisplayName("应能正确生成 Token 并从中提取用户名")
    void shouldGenerateTokenAndExtractUsername() {
        // Act
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals("testuser", extractedUsername);
    }

    @Test
    @DisplayName("对于有效的 Token 和正确的用户，isTokenValid 应返回 true")
    void shouldReturnTrueForValidTokenAndUser() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("对于有效的 Token 但错误的用户，isTokenValid 应返回 false")
    void shouldReturnFalseForValidTokenButWrongUser() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        UserDetails wrongUserDetails = new User("wronguser", "password", new ArrayList<>());

        // Act
        boolean isValid = jwtService.isTokenValid(token, wrongUserDetails);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("对于已过期的 Token，调用 isTokenValid 应抛出 ExpiredJwtException")
    void shouldThrowExceptionForExpiredToken() throws InterruptedException {
        // Arrange
        // 设置一个极短的过期时间
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);
        String expiredToken = jwtService.generateToken(userDetails);

        // 等待足够长的时间以确保 token 过期
        Thread.sleep(50);

        // Act & Assert
        // 断言当调用 isTokenValid 时，会抛出 ExpiredJwtException 异常
        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.isTokenValid(expiredToken, userDetails);
        });
    }
}