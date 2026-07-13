package com.wecorp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.wecorp.common.exception.BusinessException;
import com.wecorp.common.result.R;
import com.wecorp.common.utils.JwtUtils;
import com.wecorp.entity.Role;
import com.wecorp.entity.User;
import com.wecorp.entity.UserRole;
import com.wecorp.mapper.RoleMapper;
import com.wecorp.mapper.UserRoleMapper;
import com.wecorp.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        long expires = System.currentTimeMillis() + jwtExpiration;

        // 从 t_user_role + t_role 关联表查真实角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<String> roles = List.of();
        if (!userRoles.isEmpty()) {
            Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
            roles = roleMapper.selectBatchIds(roleIds).stream()
                    .map(Role::getRoleKey)
                    .collect(Collectors.toList());
        }
        // TODO: permissions 后续从 t_role_permission 关联表查询
        List<String> permissions = List.of();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", token);
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        data.put("roles", roles);
        data.put("permissions", permissions);
        data.put("expires", new Date(expires));

        return R.ok(data);
    }

    @Data
    public static class LoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
