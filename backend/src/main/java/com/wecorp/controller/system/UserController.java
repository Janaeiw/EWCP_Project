package com.wecorp.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wecorp.common.result.R;
import com.wecorp.entity.User;
import com.wecorp.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final SystemService systemService;

    @GetMapping("/page")
    public R<Page<User>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status
    ) {
        return R.ok(systemService.getUserPage(pageNum, pageSize, username, nickname, status));
    }

    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        User user = systemService.getUserById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        return R.ok(user);
    }

    @PostMapping
    public R<Void> create(@RequestBody User user) {
        systemService.createUser(user);
        return R.ok();
    }

    @PutMapping
    public R<Void> update(@RequestBody User user) {
        systemService.updateUser(user);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        systemService.deleteUser(id);
        return R.ok();
    }

    @PutMapping("/reset-password/{id}")
    public R<Void> resetPassword(@PathVariable Long id) {
        systemService.resetPassword(id);
        return R.ok();
    }
}
