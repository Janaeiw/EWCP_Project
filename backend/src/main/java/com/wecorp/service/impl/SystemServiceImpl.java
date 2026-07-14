package com.wecorp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wecorp.entity.*;
import com.wecorp.mapper.*;
import com.wecorp.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final MenuMapper menuMapper;
    private final ContentMapper contentMapper;
    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    private final DictMapper dictMapper;
    private final DictDataMapper dictDataMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // ========== 用户管理 ==========

    @Override
    public Page<User> getUserPage(int pageNum, int pageSize, String username, String nickname, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), User::getUsername, username)
               .like(StringUtils.hasText(nickname), User::getNickname, nickname)
               .eq(status != null, User::getStatus, status)
               .orderByDesc(User::getCreateTime);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillRoleNames(page.getRecords());
        return page;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            fillRoleNames(Collections.singletonList(user));
        }
        return user;
    }

    @Override
    @Transactional
    public void createUser(User user) {
        String rawPassword = StringUtils.hasText(user.getPassword()) ? user.getPassword() : "Password123";
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.insert(user);

        // 写入用户-角色关联
        List<Long> roleIds = user.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            // 默认关联 admin 角色
            Role adminRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "admin"));
            if (adminRole != null) {
                roleIds = Collections.singletonList(adminRole.getId());
            }
        }
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        log.info("updateUser request: id={}, nickname={}, phone={}, email={}, avatar={}",
                user.getId(), user.getNickname(), user.getPhone(), user.getEmail(), user.getAvatar());
        // 空字符串转null，确保 updateById 能正确更新
        if (!StringUtils.hasText(user.getNickname())) user.setNickname(null);
        if (!StringUtils.hasText(user.getAvatar())) user.setAvatar(null);
        if (!StringUtils.hasText(user.getEmail())) user.setEmail(null);
        if (!StringUtils.hasText(user.getPhone())) user.setPhone(null);
        // 密码单独处理：有值才更新
        String rawPassword = user.getPassword();
        user.setPassword(null);
        int rows = userMapper.updateById(user);
        log.info("updateUser affected rows: {}", rows);
        // 如果提供了新密码，单独更新
        if (StringUtils.hasText(rawPassword)) {
            User pwdUser = new User();
            pwdUser.setId(user.getId());
            pwdUser.setPassword(passwordEncoder.encode(rawPassword));
            userMapper.updateById(pwdUser);
        }

        // 更新用户-角色关联
        List<Long> roleIds = user.getRoleIds();
        if (roleIds != null) {
            // 删除旧关联
            userRoleMapper.delete(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
            // 插入新关联
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
    }

    @Override
    public void resetPassword(Long id) {
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode("Password123"));
        userMapper.updateById(user);
    }

    // ========== 角色管理 ==========

    @Override
    public Page<Role> getRolePage(int pageNum, int pageSize, String roleName, Integer status) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(roleName), Role::getRoleName, roleName)
               .eq(status != null, Role::getStatus, status)
               .orderByAsc(Role::getSort);
        return roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Role> getRoleList() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getStatus, 1)
                        .orderByAsc(Role::getSort)
        );
    }

    @Override
    public void createRole(Role role) {
        roleMapper.insert(role);
    }

    @Override
    public void updateRole(Role role) {
        roleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
    }

    // ========== 权限管理 ==========

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort)
        );
        return buildTree(all, 0L);
    }

    @Override
    public void createPermission(Permission permission) {
        permissionMapper.insert(permission);
    }

    @Override
    public void updatePermission(Permission permission) {
        permissionMapper.updateById(permission);
    }

    @Override
    public void deletePermission(Long id) {
        // 同时删除子权限
        List<Long> ids = collectChildIds(id);
        ids.add(id);
        permissionMapper.deleteBatchIds(ids);
    }

    // ========== 菜单管理 ==========

    @Override
    public List<Menu> getMenuTree() {
        List<Menu> all = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getRank)
        );
        return buildMenuTree(all, 0L);
    }

    @Override
    public List<Map<String, Object>> getRouteTree() {
        List<Menu> all = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>()
                        .eq(Menu::getStatus, 1)
                        .eq(Menu::getShowLink, 1)
                        .orderByAsc(Menu::getRank)
        );
        return buildRouteTree(all, 0L);
    }

    @Override
    public Menu getMenuById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public void createMenu(Menu menu) {
        menuMapper.insert(menu);
    }

    @Override
    public void updateMenu(Menu menu) {
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        List<Long> ids = collectMenuChildIds(id);
        ids.add(id);
        menuMapper.deleteBatchIds(ids);
    }

    // ========== 私有方法 ==========

    /**
     * 批量填充用户的 roleNames 和 roleIds（从 t_user_role + t_role 关联表查询）
     */
    private void fillRoleNames(List<User> users) {
        if (users == null || users.isEmpty()) return;

        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        // 批量查 user_role
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        if (userRoles.isEmpty()) return;

        Set<Long> allRoleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        // 批量查 role
        Map<Long, Role> roleIdMap = roleMapper.selectBatchIds(allRoleIds).stream()
                .collect(Collectors.toMap(Role::getId, r -> r));

        // 按 userId 分组
        Map<Long, List<UserRole>> userRoleMap = userRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));

        for (User user : users) {
            List<UserRole> urList = userRoleMap.getOrDefault(user.getId(), Collections.emptyList());
            user.setRoleIds(urList.stream().map(UserRole::getRoleId).collect(Collectors.toList()));
            user.setRoleList(urList.stream()
                    .map(ur -> roleIdMap.getOrDefault(ur.getRoleId(), new Role()).getRoleKey())
                    .collect(Collectors.toList()));
            user.setRoleNames(urList.stream()
                    .map(ur -> roleIdMap.getOrDefault(ur.getRoleId(), new Role()).getRoleName())
                    .collect(Collectors.toList()));
        }
    }

    private List<Permission> buildTree(List<Permission> all, Long parentId) {
        return all.stream()
                .filter(p -> parentId.equals(p.getParentId()))
                .peek(p -> p.setChildren(buildTree(all, p.getId())))
                .collect(Collectors.toList());
    }

    private List<Long> collectChildIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Permission> children = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, parentId)
        );
        for (Permission child : children) {
            ids.add(child.getId());
            ids.addAll(collectChildIds(child.getId()));
        }
        return ids;
    }

    private List<Menu> buildMenuTree(List<Menu> all, Long parentId) {
        return all.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .peek(m -> m.setChildren(buildMenuTree(all, m.getId())))
                .collect(Collectors.toList());
    }

    private List<Long> collectMenuChildIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Menu> children = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, parentId)
        );
        for (Menu child : children) {
            ids.add(child.getId());
            ids.addAll(collectMenuChildIds(child.getId()));
        }
        return ids;
    }

    private List<Map<String, Object>> buildRouteTree(List<Menu> all, Long parentId) {
        return all.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    Map<String, Object> route = new LinkedHashMap<>();
                    route.put("path", m.getPath());
                    if (m.getName() != null) route.put("name", m.getName());
                    if (m.getComponent() != null) route.put("component", m.getComponent());

                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("title", m.getTitle());
                    if (m.getIcon() != null) meta.put("icon", m.getIcon());
                    if (m.getRank() != null) meta.put("rank", m.getRank());
                    if (m.getRoles() != null && !m.getRoles().isEmpty()) {
                        List<String> roleList = parseJsonArray(m.getRoles());
                        if (!roleList.isEmpty()) meta.put("roles", roleList);
                    }
                    if (m.getAuths() != null && !m.getAuths().isEmpty()) {
                        List<String> authList = parseJsonArray(m.getAuths());
                        if (!authList.isEmpty()) meta.put("auths", authList);
                    }
                    // pure-admin 的 filterTree 检查 meta.showLink !== false，必须放入 meta
                    if (m.getShowLink() != null) meta.put("showLink", m.getShowLink() == 1);
                    route.put("meta", meta);

                    List<Map<String, Object>> children = buildRouteTree(all, m.getId());
                    if (!children.isEmpty()) {
                        route.put("children", children);
                    }
                    return route;
                })
                .collect(Collectors.toList());
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析JSON数组失败: {}", json);
            return Collections.emptyList();
        }
    }

    // ========== 内容库 ==========

    @Override
    public Page<Content> getContentPage(int pageNum, int pageSize, String type, String title, Integer status) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(type), Content::getType, type)
               .like(StringUtils.hasText(title), Content::getTitle, title)
               .eq(status != null, Content::getStatus, status)
               .orderByDesc(Content::getCreateTime);
        return contentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Content getContentById(Long id) {
        return contentMapper.selectById(id);
    }

    @Override
    public void createContent(Content content) {
        contentMapper.insert(content);
    }

    @Override
    public void updateContent(Content content) {
        contentMapper.updateById(content);
    }

    @Override
    public void deleteContent(Long id) {
        contentMapper.deleteById(id);
    }

    // ========== 图片管理 ==========

    @Override
    public Long uploadImage(byte[] data, String filename, String contentType) {
        Image image = new Image();
        image.setName(filename);
        image.setType(contentType);
        image.setData(data);
        imageMapper.insert(image);
        return image.getId();
    }

    @Override
    public Image getImage(Long id) {
        return imageMapper.selectById(id);
    }

    // ========== 视频管理 ==========

    @Override
    public Long uploadVideo(byte[] data, String filename, String contentType) {
        Video video = new Video();
        video.setName(filename);
        video.setType(contentType);
        video.setData(data);
        videoMapper.insert(video);
        return video.getId();
    }

    @Override
    public Video getVideo(Long id) {
        return videoMapper.selectById(id);
    }

    // ========== 字典管理 ==========

    @Override
    public Page<Dict> getDictPage(int pageNum, int pageSize, String dictName, Integer status) {
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dictName), Dict::getDictName, dictName)
               .eq(status != null, Dict::getStatus, status)
               .orderByDesc(Dict::getCreateTime);
        return dictMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void createDict(Dict dict) {
        dictMapper.insert(dict);
    }

    @Override
    public void updateDict(Dict dict) {
        dictMapper.updateById(dict);
    }

    @Override
    @Transactional
    public void deleteDict(Long id) {
        Dict dict = dictMapper.selectById(id);
        dictMapper.deleteById(id);
        if (dict != null) {
            dictDataMapper.delete(
                    new LambdaQueryWrapper<DictData>().eq(DictData::getDictCode, dict.getDictCode()));
        }
    }

    @Override
    public Page<DictData> getDictDataPage(int pageNum, int pageSize, String dictCode, Integer status) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dictCode), DictData::getDictCode, dictCode)
               .eq(status != null, DictData::getStatus, status)
               .orderByAsc(DictData::getSort);
        return dictDataMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void createDictData(DictData dictData) {
        dictDataMapper.insert(dictData);
    }

    @Override
    public void updateDictData(DictData dictData) {
        dictDataMapper.updateById(dictData);
    }

    @Override
    public void deleteDictData(Long id) {
        dictDataMapper.deleteById(id);
    }

    @Override
    public Map<String, List<DictData>> getAllDictData() {
        // 先查启用的字典类型
        List<Dict> enabledDicts = dictMapper.selectList(
                new LambdaQueryWrapper<Dict>().eq(Dict::getStatus, 1));
        if (enabledDicts.isEmpty()) return Collections.emptyMap();
        List<String> enabledCodes = enabledDicts.stream()
                .map(Dict::getDictCode).collect(Collectors.toList());
        // 再查启用的字典数据
        List<DictData> all = dictDataMapper.selectList(
                new LambdaQueryWrapper<DictData>()
                        .in(DictData::getDictCode, enabledCodes)
                        .eq(DictData::getStatus, 1)
                        .orderByAsc(DictData::getSort));
        return all.stream().collect(Collectors.groupingBy(DictData::getDictCode));
    }
}
