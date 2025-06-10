package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.room.entity.ChatRoom;
import chatting.chatproducer.domain.room.entity.RoomUser;
import chatting.chatproducer.domain.room.repository.ChatRoomRepository;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomUserRepository roomUserRepository;

    @Transactional
    public ChatRoom joinRoom(String roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseGet(() -> {
                    // 방이 없으면 새로 생성
                    ChatRoom newRoom = ChatRoom.builder()
                            .roomId(roomId)
                            .name(roomId)  // 이름은 roomId로 일단 기본 세팅
                            .ownerId(userId)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        // 해당 유저가 방에 참여 중이 아니라면 등록
        RoomUser.RoomUserId id = new RoomUser.RoomUserId(roomId, userId);
        if (!roomUserRepository.existsById(id)) {
            roomUserRepository.save(RoomUser.of(roomId, userId));
        }

        return room;
    }


    public List<ChatRoom> getRoomsByUser(String userId) {
        List<RoomUser> entries = roomUserRepository.findAllByUserId(userId);

        return entries.stream()
                .map(e -> chatRoomRepository.findById(e.getRoomId()).orElse(null))
                .filter(r -> r != null)
                .toList();
    }
}

