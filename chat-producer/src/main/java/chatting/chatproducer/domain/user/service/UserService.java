package chatting.chatproducer.domain.user.service;

import chatting.chatproducer.domain.user.entity.User;
import chatting.chatproducer.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User loginOrRegister(String userId) {
        return userRepository.findById(userId)
                .orElseGet(() -> userRepository.save(User.of(userId)));
    }
}

