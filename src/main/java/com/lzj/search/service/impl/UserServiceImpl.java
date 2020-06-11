package com.lzj.search.service.impl;

import com.lzj.search.base.ServiceResult;
import com.lzj.search.dto.UserDTO;
import com.lzj.search.entity.Role;
import com.lzj.search.entity.User;
import com.lzj.search.repository.UserRepository;
import com.lzj.search.service.RoleService;
import com.lzj.search.service.UserService;
import com.lzj.search.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.naming.AuthenticationNotSupportedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author lizijian
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Override
    public User findByName(String name) {
        User user = userRepository.findByName(name);
        if (user == null) {
            return null;
        }

        List<Role> roles = roleService.findByUserId(user.getId());
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));
        user.setAuthList(authorities);
        return user;
    }

    @Override
    public ServiceResult<UserDTO> findById(Long userId) {
        User user = userRepository.findById(userId).get();
        UserDTO userDTO = CommonUtil.map(user, UserDTO.class);
        return ServiceResult.of(userDTO);
    }
}
