package com.yao.musicplayer_server.service.user;

import com.yao.musicplayer_server.dto.user.UserLoginDto;
import com.yao.musicplayer_server.dto.user.UserRegisterDto;
import com.yao.musicplayer_server.entity.user.User;
import com.yao.musicplayer_server.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean register(UserRegisterDto registerDto) {
        if (userRepository.findByUsername(registerDto.getUsername()) != null) {
            return false; // 用户名已存在
        }
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setFullname(registerDto.getFullname());
        user.setPhone(registerDto.getPhone());
        user.setGender(registerDto.getGender());
        user.setBirthday(registerDto.getBirthday());
        userRepository.save(user);
        return true;
    }

    public User login(UserLoginDto loginDto) {
        User user = userRepository.findByUsername(loginDto.getUsername());
        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return user;
        }
        return null;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
} 