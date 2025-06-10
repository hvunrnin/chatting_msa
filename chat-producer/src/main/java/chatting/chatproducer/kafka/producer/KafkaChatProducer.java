package chatting.chatproducer.kafka.producer;

import chatting.chatproducer.kafka.dto.ChatKafkaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaChatProducer {
    private final KafkaTemplate<String, ChatKafkaMessage> kafkaTemplate;
    private static final String TOPIC_NAME = "chat-message";

    public void sendMessage(ChatKafkaMessage message) {
        kafkaTemplate.send(TOPIC_NAME, message);
    }
}
