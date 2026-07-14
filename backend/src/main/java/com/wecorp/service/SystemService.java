package com.wecorp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wecorp.entity.*;

import java.util.List;
import java.util.Map;

public interface SystemService {

    // ========== 用户管理 ==========

    Page<User> getUserPage(int pageNum, int pageSize, String username, String nickname, Integer status);

    User getUserById(Long id);

    void createUser(User user);

    void updateUser(User user);

    void deleteUser(Long id);

    void resetPassword(Long id);

    // ========== 角色管理 ==========

    Page<Role> getRolePage(int pageNum, int pageSize, String roleName, Integer status);

    List<Role> getRoleList();

    void createRole(Role role);

    void updateRole(Role role);

    void deleteRole(Long id);

    List<Long> getRoleMenuIds(Long roleId);

    void saveRoleMenus(Long roleId, List<Long> menuIds);

    // ========== 权限管理 ==========

    List<Permission> getPermissionTree();

    void createPermission(Permission permission);

    void updatePermission(Permission permission);

    void deletePermission(Long id);

    // ========== 菜单管理 ==========

    List<Menu> getMenuTree();

    List<Map<String, Object>> getRouteTree();

    Menu getMenuById(Long id);

    void createMenu(Menu menu);

    void updateMenu(Menu menu);

    void deleteMenu(Long id);

    // ========== 内容库 ==========

    Page<Content> getContentPage(int pageNum, int pageSize, String type, String title, Integer status);

    Content getContentById(Long id);

    void createContent(Content content);

    void updateContent(Content content);

    void deleteContent(Long id);

    // ========== 图片管理 ==========

    Long uploadImage(byte[] data, String filename, String contentType);

    Image getImage(Long id);

    // ========== 视频管理 ==========

    Long uploadVideo(byte[] data, String filename, String contentType);

    Video getVideo(Long id);

    // ========== 字典管理 ==========

    Page<Dict> getDictPage(int pageNum, int pageSize, String dictName, Integer status);

    void createDict(Dict dict);

    void updateDict(Dict dict);

    void deleteDict(Long id);

    Page<DictData> getDictDataPage(int pageNum, int pageSize, String dictCode, Integer status);

    void createDictData(DictData dictData);

    void updateDictData(DictData dictData);

    void deleteDictData(Long id);

    Map<String, List<DictData>> getAllDictData();
}
