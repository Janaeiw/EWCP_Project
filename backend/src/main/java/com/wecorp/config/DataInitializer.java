package com.wecorp.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wecorp.entity.Menu;
import com.wecorp.entity.Role;
import com.wecorp.entity.RoleMenu;
import com.wecorp.entity.User;
import com.wecorp.entity.UserRole;
import com.wecorp.mapper.MenuMapper;
import com.wecorp.mapper.RoleMapper;
import com.wecorp.mapper.RoleMenuMapper;
import com.wecorp.mapper.UserMapper;
import com.wecorp.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
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
        // ===== 系统管理菜单（已存在逻辑） =====
        Menu systemMenu = seedParentMenu("/system", "系统管理", "ep:setting", 99);

        seedChildMenu(systemMenu.getId(), "/system/user", "system/user/index", "SystemUser", "用户管理", 1);
        seedChildMenu(systemMenu.getId(), "/system/role", "system/role/index", "SystemRole", "角色管理", 2);
        seedChildMenu(systemMenu.getId(), "/system/permission", "system/permission/index", "SystemPermission", "权限管理", 3);
        seedChildMenu(systemMenu.getId(), "/system/organization", "system/organization/index", "SystemOrganization", "机构管理", 4);
        seedChildMenu(systemMenu.getId(), "/system/menu", "system/menu/index", "SystemMenu", "菜单管理", 5);
        seedChildMenu(systemMenu.getId(), "/system/log", "system/log/index", "SystemLog", "日志管理", 6);
        seedChildMenu(systemMenu.getId(), "/system/dict", "system/dict/index", "SystemDict", "字典管理", 7);
        seedChildMenu(systemMenu.getId(), "/system/config", "system/config/index", "SystemConfig", "系统参数管理", 8);
        seedChildMenu(systemMenu.getId(), "/system/job", "system/job/index", "SystemJob", "定时任务管理", 9);

        // 给系统管理菜单补按钮类型子菜单
        seedButtonMenu(systemMenu.getId(), "permission:btn:add", "添加", 1);
        seedButtonMenu(systemMenu.getId(), "permission:btn:edit", "编辑", 2);
        seedButtonMenu(systemMenu.getId(), "permission:btn:delete", "删除", 3);

        // ===== 权限管理菜单 =====
        Menu permMenu = seedParentMenu("/permission", "权限管理", "ep:lollipop", 10);
        Long permId = permMenu.getId();

        seedChildMenu(permId, "/permission/page/index", "permission/page/index", "PermissionPage", "页面权限", 1);
        Menu btnMenu = seedChildMenu(permId, "/permission/button", null, null, "按钮权限", 2);
        seedChildMenu(btnMenu.getId(), "/permission/button/router", "permission/button/index", "PermissionButtonRouter", "路由返回按钮权限", 1);
        seedChildMenu(btnMenu.getId(), "/permission/button/login", "permission/button/perms", "PermissionButtonLogin", "登录接口返回按钮权限", 2);

        seedButtonMenu(permId, "permission:btn:add", "添加", 1);
        seedButtonMenu(permId, "permission:btn:edit", "编辑", 2);
        seedButtonMenu(permId, "permission:btn:delete", "删除", 3);

        // ===== 六库管理菜单 =====
        Menu libMenu = seedParentMenu("/library", "六库管理", "ep:collection", 12);
        Long libId = libMenu.getId();

        seedChildMenu(libId, "/library/product", "library/product/index", "LibraryProduct", "产品库", 1);
        seedChildMenu(libId, "/library/poster", "library/poster/index", "LibraryPoster", "海报库", 2);
        seedChildMenu(libId, "/library/content", "library/content/index", "LibraryContent", "内容库", 3);
        seedChildMenu(libId, "/library/script", "library/script/index", "LibraryScript", "话术库", 4);
        seedChildMenu(libId, "/library/activity", "library/activity/index", "LibraryActivity", "活动库", 5);
        seedChildMenu(libId, "/library/tool", "library/tool/index", "LibraryTool", "工具库", 6);

        seedButtonMenu(libId, "permission:btn:add", "添加", 1);
        seedButtonMenu(libId, "permission:btn:edit", "编辑", 2);
        seedButtonMenu(libId, "permission:btn:delete", "删除", 3);

        // ===== admin 角色绑定 systemRouter 所有菜单 =====
        Role adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "admin"));
        if (adminRole != null) {
            bindRoleMenus(adminRole.getId(), "/system");
        }
    }

    private Menu seedParentMenu(String path, String title, String icon, int rank) {
        Menu existing = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>().eq(Menu::getPath, path));
        if (existing != null) return existing;
        Menu menu = new Menu();
        menu.setParentId(0L);
        menu.setPath(path);
        menu.setTitle(title);
        menu.setIcon(icon);
        menu.setRank(rank);
        menu.setMenuType(0);
        menu.setShowLink(1);
        menu.setStatus(1);
        menuMapper.insert(menu);
        log.info("父级菜单已创建: {} (id={})", title, menu.getId());
        return menu;
    }

    private Menu seedChildMenu(Long parentId, String path, String component, String name, String title, int rank) {
        Menu existing = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>().eq(Menu::getPath, path));
        if (existing != null) return existing;
        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setName(name);
        menu.setTitle(title);
        menu.setRank(rank);
        menu.setMenuType(0);
        menu.setShowLink(1);
        menu.setStatus(1);
        menuMapper.insert(menu);
        log.info("默认子菜单已创建: {} (id={})", title, menu.getId());
        return menu;
    }

    private void seedButtonMenu(Long parentId, String permission, String title, int rank) {
        Menu existing = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>()
                        .eq(Menu::getParentId, parentId)
                        .eq(Menu::getPermission, permission));
        if (existing != null) return;
        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setPath(parentId + ":" + permission);
        menu.setMenuType(1);
        menu.setPermission(permission);
        menu.setTitle(title);
        menu.setRank(rank);
        menu.setShowLink(0);
        menu.setStatus(1);
        menuMapper.insert(menu);
        log.info("按钮权限已创建: {} -> {} (id={})", parentId, permission, menu.getId());
    }

    private void bindRoleMenus(Long roleId, String parentPath) {
        Menu parent = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>().eq(Menu::getPath, parentPath));
        if (parent == null) return;
        List<Long> allMenuIds = new ArrayList<>();
        allMenuIds.add(parent.getId());
        allMenuIds.addAll(collectAllMenuChildIds(parent.getId()));
        for (Long menuId : allMenuIds) {
            Long cnt = roleMenuMapper.selectCount(
                    new LambdaQueryWrapper<RoleMenu>()
                            .eq(RoleMenu::getRoleId, roleId)
                            .eq(RoleMenu::getMenuId, menuId));
            if (cnt == 0) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        log.info("角色 {} 已绑定 {} 下 {} 个菜单", roleId, parentPath, allMenuIds.size());
    }

    private List<Long> collectAllMenuChildIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Menu> children = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, parentId));
        for (Menu child : children) {
            ids.add(child.getId());
            ids.addAll(collectAllMenuChildIds(child.getId()));
        }
        return ids;
    }
}
