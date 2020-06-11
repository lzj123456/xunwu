package com.lzj.search;

import com.lzj.search.entity.User;
import com.lzj.search.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * @author lizijian
 */
public class UserRepositoryTest extends SearchApplicationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindOne() {
        Optional<User> user = userRepository.findById(1L);
        System.out.println(user.get());
    }
}
