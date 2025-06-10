package chatting.chatconsumer.kafka.producer;

import chatting.chatconsumer.kafka.dto.ChatKafkaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaSendMsgProducer {
    // ‘메시지 수신’ 토픽에 producer 역할
    private final KafkaTemplate<String, ChatKafkaMessage> kafkaTemplate;
    private static final String TOPIC_NAME = "chat-message-sent";

    public void send(ChatKafkaMessage message) {
        kafkaTemplate.send(TOPIC_NAME, message);
    }
}
