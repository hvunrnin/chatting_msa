package chatting.chatproducer.domain.chat.repository;

import chatting.chatproducer.domain.room.entity.ChatRoom;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatRepository {
    // WebSocket 세션 기반의 인메모리 채팅방 관리 용도. DB와는 무관한 채팅방 세션 관리 용

    private final Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();

    public ChatRoom findById(String roomId) {
        return chatRooms.get(roomId);
    }

    public void save(ChatRoom room) {
        chatRooms.put(room.getRoomId(), room);
    }

    public Collection<ChatRoom> findAll() {
        return chatRooms.values();
    }

    public void delete(String roomId) {
        chatRooms.remove(roomId);
    }
}

