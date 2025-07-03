package chatting.chatconsumer.domain.chatmessage.listener;

import chatting.chatconsumer.domain.chatmessage.document.ChatMessageDocument;
import chatting.chatconsumer.domain.chatmessage.repository.ChatMessageMongoRepository;
import chatting.chatconsumer.kafka.dto.ChatKafkaMessage;
import chatting.chatconsumer.kafka.producer.KafkaSendMsgProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageChangeStreamListener {

    private final MongoTemplate mongoTemplate;
    private final ChatMessageMongoRepository chatMessageMongoRepository;
    private final KafkaSendMsgProducer kafkaSendMsgProducer;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void listenChangeStream() {
        log.info("Change Stream Listener 시작");

        mongoTemplate.getCollection("chat_messages_ind")
                .watch()
                .forEach((ChangeStreamDocument<Document> change) -> {
                    Document fullDoc = change.getFullDocument();

                    if (fullDoc == null) return;

                    String status = fullDoc.getString("status");
                    if (!"PENDING".equals(status)) {
                        return; // SENT 상태면 무시
                    }

                    try {
                        String id = fullDoc.getObjectId("_id").toHexString();
                        String roomId = fullDoc.getString("roomId");
                        String sender = fullDoc.getString("sender");
                        String message = fullDoc.getString("message");
                        java.util.Date ts = fullDoc.getDate("timestamp");

                        ChatKafkaMessage msg = ChatKafkaMessage.builder()
                                .roomId(roomId)
                                .sender(sender)
                                .message(message)
                                .timestamp(ts.toInstant())
                                .build();

                        kafkaSendMsgProducer.send(msg);

                        // 상태 SENT로 변경
                        chatMessageMongoRepository.findById(id).ifPresent(doc -> {
                            doc.setStatus("SENT");
                            chatMessageMongoRepository.save(doc);
                        });

                        log.info("Change Stream 발행 완료: {}", id);

                    } catch (Exception e) {
                        log.error("Change Stream 발행 실패: {}", e.getMessage(), e);
                    }
                });
    }
}
