package chatting.chatproducer.domain.chat.service;

import chatting.chatproducer.domain.chat.repository.ChatRepository;
import chatting.chatproducer.domain.room.entity.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChatService {
    private final ChatRepository chatRepository;

    public ChatRoom findRoomById(String roomId) {
        return chatRepository.findById(roomId);
    }
}
