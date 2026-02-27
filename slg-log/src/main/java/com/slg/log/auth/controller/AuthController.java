package com.slg.log.auth.controller;

import com.slg.log.auth.entity.LogUserEntity;
import com.slg.log.auth.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证与用户管理接口
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<LogUserEntity>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @PostMapping("/admin/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            LogUserEntity user = authService.createUser(request.getUsername(), request.getPassword(), request.getRole());
            return ResponseEntity.ok(Map.of("username", user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/admin/users/{username}/password")
    public ResponseEntity<?> changePassword(@PathVariable String username, @RequestBody Map<String, String> body) {
        authService.changePassword(username, body.get("password"));
        return ResponseEntity.ok(Map.of("message", "密码已修改"));
    }

    @PutMapping("/admin/users/{username}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable String username, @RequestBody Map<String, Boolean> body) {
        authService.setUserEnabled(username, body.get("enabled"));
        return ResponseEntity.ok(Map.of("message", "状态已更新"));
    }

    @DeleteMapping("/admin/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        authService.deleteUser(username);
        return ResponseEntity.ok(Map.of("message", "用户已删除"));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String role;
    }
}
