package chatting.chatproducer.domain.user.controller;

import chatting.chatproducer.domain.user.entity.User;
import chatting.chatproducer.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public User login(@RequestParam String userId) {
        return userService.loginOrRegister(userId);
    }
}

