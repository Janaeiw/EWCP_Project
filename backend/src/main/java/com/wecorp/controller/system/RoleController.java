package com.wecorp.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wecorp.common.result.R;
import com.wecorp.entity.Role;
import com.wecorp.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final SystemService systemService;

    @GetMapping("/page")
    public R<Page<Role>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Integer status
    ) {
        return R.ok(systemService.getRolePage(pageNum, pageSize, roleName, status));
    }

    @GetMapping("/list")
    public R<List<Role>> list() {
        return R.ok(systemService.getRoleList());
    }

    @PostMapping
    public R<Void> create(@RequestBody Role role) {
        systemService.createRole(role);
        return R.ok();
    }

    @PutMapping
    public R<Void> update(@RequestBody Role role) {
        systemService.updateRole(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        systemService.deleteRole(id);
        return R.ok();
    }
}
