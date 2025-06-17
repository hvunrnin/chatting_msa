package chatting.chatproducer.kafka.producer;

import chatting.chatproducer.domain.failedmessage.document.FailedChatMessageDocument;
import chatting.chatproducer.domain.failedmessage.repository.FailedChatMessageRepository;
import chatting.chatproducer.kafka.dto.ChatKafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatProducer {
    private final KafkaTemplate<String, ChatKafkaMessage> kafkaTemplate;
    private final FailedChatMessageRepository failedMessageRepository;

    private static final String TOPIC_NAME = "chat-message";

//    public void sendMessage(ChatKafkaMessage message) {
//        kafkaTemplate.send(TOPIC_NAME, message);
//    }
    public void sendMessage(ChatKafkaMessage message) {
        kafkaTemplate.send(TOPIC_NAME, message)
                .handle((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 전송 실패", ex);
                        FailedChatMessageDocument failedMessage = FailedChatMessageDocument.builder()
                                .roomId(message.getRoomId())
                                .sender(message.getSender())
                                .message(message.getMessage())
                                .timestamp(message.getTimestamp())
                                .status("FAILED")
                                .build();

                        try {
                            failedMessageRepository.save(failedMessage);
                            log.info("failedMsg MongoDB 저장 성공");
                        } catch (Exception mongoEx) {
                            log.error("failedMsg MongoDB 저장 실패", mongoEx);
                        }
                    } else {
                        log.info(" Kafka 전송 성공: {}", result.getRecordMetadata());
                    }
                    return null;
                });

    }

}
