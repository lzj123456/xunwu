package com.lzj.search.service;

import com.lzj.search.entity.Role;

import java.util.List;

/**
 * @author lizijian
 */
public interface RoleService {

    List<Role> findByUserId(Long userId);
}
