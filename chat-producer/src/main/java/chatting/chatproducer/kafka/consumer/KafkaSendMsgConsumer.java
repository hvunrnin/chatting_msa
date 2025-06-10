package chatting.chatproducer.kafka.consumer;

import chatting.chatproducer.domain.chat.dto.ChatMessage;
import chatting.chatproducer.kafka.dto.ChatKafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSendMsgConsumer {
    // ‘메시지 수신’ 토픽에 consumer 역할
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "chat-message-sent",
            groupId = "#{T(java.util.UUID).randomUUID().toString()}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSent(ChatKafkaMessage message) {

        // WebSocket으로 전송
        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + message.getRoomId(),
                ChatMessage.builder()
                        .roomId(message.getRoomId())
                        .sender(message.getSender())
                        .message(message.getMessage())
                        .timestamp(message.getTimestamp())
                        .messageType(ChatMessage.MessageType.valueOf(message.getMessageType()))
                        .build()
        );
    }
}
