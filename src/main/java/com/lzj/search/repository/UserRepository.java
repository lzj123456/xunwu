package com.lzj.search.repository;

import com.lzj.search.entity.User;
import org.springframework.data.repository.CrudRepository;

/**
 * @author lizijian
 */
public interface UserRepository extends CrudRepository<User, Long> {

    User findByName(String name);
}
