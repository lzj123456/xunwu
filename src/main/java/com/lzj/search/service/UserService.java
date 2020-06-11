package com.lzj.search.service;

import com.lzj.search.base.ServiceResult;
import com.lzj.search.dto.UserDTO;
import com.lzj.search.entity.User;

/**
 * @author lizijian
 */
public interface UserService {

    User findByName(String name);

    ServiceResult<UserDTO> findById(Long userId);

}
