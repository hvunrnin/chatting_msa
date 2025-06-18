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
        try {
            var future = kafkaTemplate.send(TOPIC_NAME, message);
            var result = future.get(); // 동기 대기
            log.info("Kafka 전송 성공: offset={}, partition={}, key={}, roomId={}",
                    result.getRecordMetadata().offset(),
                    result.getRecordMetadata().partition(),
                    result.getProducerRecord().key(),
                    message.getRoomId());
        } catch (Exception e) {
            log.error("Kafka 전송 실패: roomId={}, msg={}", message.getRoomId(), message.getMessage(), e);
        }

    }


}
