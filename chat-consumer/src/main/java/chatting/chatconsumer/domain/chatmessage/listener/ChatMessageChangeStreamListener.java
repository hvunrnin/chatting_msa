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

                    String id = fullDoc.getObjectId("_id").toHexString();

                    try {
                        chatMessageMongoRepository.findById(id).ifPresent(doc -> {
                            if (!"PENDING".equals(doc.getStatus())) {
                                return; // 이미 처리됨
                            }

                            ChatKafkaMessage msg = ChatKafkaMessage.builder()
                                    .roomId(doc.getRoomId())
                                    .sender(doc.getSender())
                                    .message(doc.getMessage())
                                    .timestamp(doc.getTimestamp())
                                    .messageType(doc.getMessageType())
                                    .build();

                            // Kafka 퍼블리시
                            kafkaSendMsgProducer.send(msg);

                            // 상태 SENT로 변경
                            doc.setStatus("SENT");
                            chatMessageMongoRepository.save(doc);

                            log.info("Change Stream 발행 완료: {}", doc.getId());
                        });

                    } catch (Exception e) {
                        log.error("Change Stream 발행 실패: {}", e.getMessage(), e);
                    }
                });
    }

}
