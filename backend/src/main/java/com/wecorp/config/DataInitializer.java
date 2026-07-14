package com.wecorp.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wecorp.entity.Menu;
import com.wecorp.entity.Role;
import com.wecorp.entity.User;
import com.wecorp.entity.UserRole;
import com.wecorp.mapper.MenuMapper;
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
    private final MenuMapper menuMapper;
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

        // 4. 确保默认菜单存在
        seedMenus();
    }

    private void seedMenus() {
        Menu systemMenu = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>().eq(Menu::getPath, "/system")
        );
        if (systemMenu == null) {
            systemMenu = new Menu();
            systemMenu.setParentId(0L);
            systemMenu.setPath("/system");
            systemMenu.setTitle("系统管理");
            systemMenu.setIcon("ep:setting");
            systemMenu.setRank(99);
            systemMenu.setShowLink(1);
            systemMenu.setStatus(1);
            menuMapper.insert(systemMenu);
            log.info("默认系统管理菜单已创建: id={}", systemMenu.getId());
        }

        seedChildMenu(systemMenu.getId(), "/system/user", "system/user/index", "SystemUser", "用户管理", 1);
        seedChildMenu(systemMenu.getId(), "/system/role", "system/role/index", "SystemRole", "角色管理", 2);
        seedChildMenu(systemMenu.getId(), "/system/permission", "system/permission/index", "SystemPermission", "权限管理", 3);
        seedChildMenu(systemMenu.getId(), "/system/organization", "system/organization/index", "SystemOrganization", "机构管理", 4);
        seedChildMenu(systemMenu.getId(), "/system/menu", "system/menu/index", "SystemMenu", "菜单管理", 5);
        seedChildMenu(systemMenu.getId(), "/system/log", "system/log/index", "SystemLog", "日志管理", 6);
        seedChildMenu(systemMenu.getId(), "/system/dict", "system/dict/index", "SystemDict", "字典管理", 7);
        seedChildMenu(systemMenu.getId(), "/system/config", "system/config/index", "SystemConfig", "系统参数管理", 8);
        seedChildMenu(systemMenu.getId(), "/system/job", "system/job/index", "SystemJob", "定时任务管理", 9);
    }

    private void seedChildMenu(Long parentId, String path, String component, String name, String title, int rank) {
        Menu existing = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>().eq(Menu::getPath, path)
        );
        if (existing == null) {
            Menu menu = new Menu();
            menu.setParentId(parentId);
            menu.setPath(path);
            menu.setComponent(component);
            menu.setName(name);
            menu.setTitle(title);
            menu.setRank(rank);
            menu.setShowLink(1);
            menu.setStatus(1);
            menuMapper.insert(menu);
            log.info("默认子菜单已创建: {} (id={})", title, menu.getId());
        }
    }
}
