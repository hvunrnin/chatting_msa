package chatting.chatproducer.domain.user.repository;

import chatting.chatproducer.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
