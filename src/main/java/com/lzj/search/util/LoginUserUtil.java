package com.lzj.search.util;

import com.lzj.search.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author lizijian
 */
public class LoginUserUtil {

    public static User load() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal != null && principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    public static Long getLoginUserId() {
        User load = load();
        if (load != null) {
            return load.getId();
        }
        return null;
    }
}
