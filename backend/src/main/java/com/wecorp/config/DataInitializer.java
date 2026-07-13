package com.wecorp.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wecorp.entity.Role;
import com.wecorp.entity.User;
import com.wecorp.entity.UserRole;
import com.wecorp.mapper.RoleMapper;
import com.wecorp.mapper.UserMapper;
import com.wecorp.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        // 1. 确保 admin 角色存在
        Role adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "admin")
        );
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setRoleName("超级管理员");
            adminRole.setRoleKey("admin");
            adminRole.setSort(1);
            adminRole.setStatus(1);
            roleMapper.insert(adminRole);
            log.info("默认管理员角色已创建: id={}", adminRole.getId());
        }

        // 2. 确保 admin 用户存在
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")
        );

        if (existing == null) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("Password123"));
            user.setNickname("管理员");
            user.setStatus(1);
            userMapper.insert(user);
            existing = user;
            log.info("默认管理员账号已创建: admin, id={}", user.getId());
        } else {
            log.info("管理员账号已存在，跳过创建");
        }

        // 3. 确保 admin 用户和 admin 角色的关联存在
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, existing.getId())
                        .eq(UserRole::getRoleId, adminRole.getId())
        );
        if (count == 0) {
            UserRole userRole = new UserRole();
            userRole.setUserId(existing.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleMapper.insert(userRole);
            log.info("已建立 admin 用户与管理员角色的关联");
        }
    }
}
