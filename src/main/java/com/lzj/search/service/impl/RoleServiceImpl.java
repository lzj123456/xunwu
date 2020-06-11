package com.lzj.search.service.impl;

import com.lzj.search.entity.Role;
import com.lzj.search.repository.RoleRepository;
import com.lzj.search.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author lizijian
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public List<Role> findByUserId(Long userId) {
        return roleRepository.findByUserId(userId);
    }
}
