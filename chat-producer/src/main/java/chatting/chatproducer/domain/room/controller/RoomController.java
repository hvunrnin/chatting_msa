package chatting.chatproducer.domain.room.controller;


import chatting.chatproducer.domain.room.entity.ChatRoom;
import chatting.chatproducer.domain.room.entity.RoomUser;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
import chatting.chatproducer.domain.room.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final ChatRoomService chatRoomService;
    private final RoomUserRepository roomUserRepository;

    // 해당 roomId가 존재하면 채팅방 참여, 존재하지 않으면 채팅방 생성
    @PostMapping("/join-or-create")
    public void joinRoom(
            @RequestParam String roomId,
            @RequestParam String userId
    ) {
        //chatRoomService.joinRoom(roomId, userId);
    }

    // 참여 중인 채팅방 목록
    @GetMapping("/list")
    public List<ChatRoom> getMyRooms(@RequestParam String userId) {
        return chatRoomService.getRoomsByUser(userId);
    }

    @DeleteMapping("/leave")
    public ResponseEntity<Void> leaveRoom(@RequestParam String roomId, @RequestParam String userId) {
        roomUserRepository.deleteById(new RoomUser.RoomUserId(roomId, userId));
        return ResponseEntity.ok().build();
    }
}
