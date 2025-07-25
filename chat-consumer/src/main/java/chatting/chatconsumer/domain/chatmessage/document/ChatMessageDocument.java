package chatting.chatconsumer.domain.chatmessage.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_messages_ind")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageDocument {

    @Id
    private String id;

    private String roomId;
    private String sender;

    private Instant timestamp;

    private String message;
    private String messageType;

    private String status;

}

//@Document(collection = "chat_messages")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ChatMessageDocument {
//
//    @Id
//    private String roomId; // MongoDB에서 _id로 설정
//
//    // 날짜(yyyy-MM-dd) -> 메시지 리스트
//    private Map<String, List<Message>> messagesByDate = new HashMap<>();
//
//    @Getter
//    @Setter
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Builder
//    public static class Message {
//        private String sender;
//        private String message;
//        private Instant timestamp;
//    }
//}

