package chatting.chatproducer.domain.chat.controller;

import chatting.chatproducer.domain.chat.service.ChatService;
import chatting.chatproducer.domain.room.entity.RoomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import chatting.chatproducer.domain.chat.dto.ChatMessage;
import chatting.chatproducer.kafka.dto.ChatKafkaMessage;
import chatting.chatproducer.kafka.producer.KafkaChatProducer;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
import chatting.chatproducer.domain.room.service.ChatRoomService;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomUserRepository roomUserRepository;
    private final ChatRoomService chatRoomService;
    private final KafkaChatProducer kafkaChatProducer;

    // 사용자별 현재 접속한 방을 저장하는 Map
    private static final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> userJoinedRooms = new ConcurrentHashMap<>();


    // 사용자가 입장했을 때
    @MessageMapping("/chat/addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId,
                        @Payload ChatMessage chatMessage,
                        SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();

        // 세션 저장
        headerAccessor.getSessionAttributes().put("username", username);

        // 최초 입장인지 확인
//        boolean isFirstEnter = userJoinedRooms
//                .computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet())
//                .add(username); // 이 때 최초 입장이면 true 반환됨
        boolean isFirstEnter = !roomUserRepository.existsById(
                new RoomUser.RoomUserId(roomId, username)
        );

        if (isFirstEnter) {
            ChatKafkaMessage kafkaMessage = ChatKafkaMessage.builder()
                    .roomId(roomId)
                    .sender(username)
                    .message(username + "님이 입장하셨습니다.")
                    .timestamp(Instant.now())
                    .messageType("ENTER")
                    .build();

            kafkaChatProducer.sendMessage(kafkaMessage);

            chatRoomService.joinRoom(roomId, username);
            messagingTemplate.convertAndSend("/sub/user/" + username + "/room-refresh", "refresh");
        }
    }


    // 사용자가 채팅 종료 버튼을 눌렀을 때
    @MessageMapping("/chat/leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
        String username = chatMessage.getSender();

        // 사용자가 현재 방에 있는지 확인
        if (userSessions.containsKey(username)) {
            userSessions.remove(username); // 사용자 제거
        }

        ChatKafkaMessage kafkaMessage = ChatKafkaMessage.builder()
                .roomId(roomId)
                .sender(username)
                .message(username + "님이 퇴장하셨습니다.")
                .timestamp(Instant.now())
                .messageType("LEAVE")
                .build();

        kafkaChatProducer.sendMessage(kafkaMessage);
    }

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        Instant now = Instant.now();
        if (message.getTimestamp() == null) {
            message.setTimestamp(now);
        }

        // Kafka 메시지로 변환
        ChatKafkaMessage kafkaMessage = ChatKafkaMessage.from(message);

        // Kafka로 전송
        kafkaChatProducer.sendMessage(kafkaMessage);
    }


}
