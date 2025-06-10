package chatting.chatproducer.domain.chat.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    public enum MessageType {
        ENTER, TALK, LEAVE
    }

    private MessageType messageType;
    private String roomId;
    private String sender;
    private String message;
    private Instant timestamp;

    public static ChatMessage leaveMessage(String roomId, String sender) {
        return ChatMessage.builder()
                .messageType(MessageType.LEAVE)
                .roomId(roomId)
                .sender(sender)
                .message(sender + "님이 퇴장하셨습니다.")
                .timestamp(Instant.now())
                .build();
    }
}
