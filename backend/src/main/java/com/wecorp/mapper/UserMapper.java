package com.wecorp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wecorp.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
