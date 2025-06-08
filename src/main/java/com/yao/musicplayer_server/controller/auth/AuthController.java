package com.yao.musicplayer_server.controller.auth;

import com.yao.musicplayer_server.dto.user.UserLoginDto;
import com.yao.musicplayer_server.dto.user.UserRegisterDto;
import com.yao.musicplayer_server.entity.user.User;
import com.yao.musicplayer_server.service.user.UserService;
import com.yao.musicplayer_server.util.JwtUtil;
import com.yao.musicplayer_server.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody UserRegisterDto registerDto) {
        boolean success = userService.register(registerDto);
        if (success) {
            return ApiResponse.success(null, "注册成功");
        } else {
            return ApiResponse.error("用户名已存在");
        }
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody UserLoginDto loginDto) {
        User user = userService.login(loginDto);
        if (user != null) {
            String token = jwtUtil.generateToken(user.getUsername());
            return ApiResponse.success(token, "登录成功");
        } else {
            return ApiResponse.error("用户名或密码错误");
        }
    }
} 