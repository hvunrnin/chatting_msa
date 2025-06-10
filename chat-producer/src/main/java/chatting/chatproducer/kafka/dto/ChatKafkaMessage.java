package chatting.chatproducer.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatKafkaMessage {
    private String roomId;
    private String sender;
    private String message;
    private Instant timestamp;
    private String messageType; // ENTER / TALK / LEAVE

    public static ChatKafkaMessage from(chatting.chatproducer.domain.chat.dto.ChatMessage chatMessage) {
        return ChatKafkaMessage.builder()
                .roomId(chatMessage.getRoomId())
                .sender(chatMessage.getSender())
                .message(chatMessage.getMessage())
                .timestamp(chatMessage.getTimestamp())
                .messageType(chatMessage.getMessageType().name())
                .build();
    }
}
