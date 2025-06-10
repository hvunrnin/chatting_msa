package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
}
