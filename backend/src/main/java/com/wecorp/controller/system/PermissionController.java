package com.wecorp.controller.system;

import com.wecorp.common.result.R;
import com.wecorp.entity.Permission;
import com.wecorp.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final SystemService systemService;

    @GetMapping("/tree")
    public R<List<Permission>> tree() {
        return R.ok(systemService.getPermissionTree());
    }

    @PostMapping
    public R<Void> create(@RequestBody Permission permission) {
        systemService.createPermission(permission);
        return R.ok();
    }

    @PutMapping
    public R<Void> update(@RequestBody Permission permission) {
        systemService.updatePermission(permission);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        systemService.deletePermission(id);
        return R.ok();
    }
}
