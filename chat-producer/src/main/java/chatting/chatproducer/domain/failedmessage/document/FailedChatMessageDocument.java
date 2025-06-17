package chatting.chatproducer.domain.failedmessage.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "failed_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedChatMessageDocument {

    @Id
    private String id;

    private String roomId;

    private String sender;

    private String message;

    private Instant timestamp;

    private String status; // FAILED
}
