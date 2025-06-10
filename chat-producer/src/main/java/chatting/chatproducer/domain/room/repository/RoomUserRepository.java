package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.RoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomUserRepository extends JpaRepository<RoomUser, RoomUser.RoomUserId> {
    List<RoomUser> findAllByUserId(String userId);
}

