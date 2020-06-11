package com.lzj.search.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author lizijian
 */
@Controller
public class UserController {

    @RequestMapping("/user/login")
    public String loginPage() {
        return "/user/login";
    }

    @RequestMapping("/user/center")
    public String centerPage() {
        return "/user/center";
    }
}
