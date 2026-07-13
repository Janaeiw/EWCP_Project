package com.wecorp.service;

import com.wecorp.entity.User;

public interface UserService {
    User getByUsername(String username);
}
