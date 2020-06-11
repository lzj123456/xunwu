package com.lzj.search.repository;

import com.lzj.search.entity.Role;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @author lizijian
 */
public interface RoleRepository extends CrudRepository<Role, Long> {

    List<Role> findByUserId(Long userId);
}
