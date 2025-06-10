package chatting.chatconsumer.domain.chatmessage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private String sender;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
