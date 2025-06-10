package chatting.chatproducer.domain.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginViewController {

    @GetMapping("/")
    public String loginPage() {
        return "login";
    }
}